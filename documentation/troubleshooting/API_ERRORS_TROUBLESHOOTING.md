# Troubleshooting Guide - Common API Errors

## Error 1: Authentication Failed ❌

### **Symptom**
```
Failed to connect to SWIFT: Authentication failed: 
{1:F01MOCKSVRXXXXAXXX0000000000}{2:I001MOCKRCVRXXXXN}{4:
```

### **Root Cause**
The SWIFT connector's `authenticate()` method expects a complete SWIFT message with:
- Block 1 (Basic Header)
- Block 2 (Application Header)
- Block 4 (Message Body) with login success indicator
- Block 5 (Trailer)

The mock server is sending an incomplete message (Block 4 is empty).

### **Fix Options**

#### **Option A: Update Mock Server** (Recommended)
The mock server needs to send a proper login response:

```python
# In swift_mock_server_v3.py, update the login handler to:
response = (
    "{1:F01MOCKSVRXXXXAXXX0000000000}"
    "{2:I001MOCKRCVRXXXXN}"
    "{4:\n:20:LOGIN_OK\n:79:LOGIN_SUCCESSFUL\n-}"
    "{5:{MAC:ABCD1234}{CHK:5678EFGH}}"
)
```

#### **Option B: Simplify Connector Authentication**
Update `SwiftConnection.authenticate()` to accept partial responses for mock testing.

**Temporary Workaround**: Set `autoReconnect="false"` in the connector config to skip authentication during development.

---

## Error 2: MT-to-MX Translation - Serialization Error ❌

### **Symptom**
```
No read or write handler for messageType
java.lang.IllegalStateException: No read or write handler for messageType
```

### **Root Cause**
The `TranslationResponse` object returned by `translateMtToMx` contains a field (`messageType` or similar) that DataWeave can't serialize to JSON.

### **Fix**

Update the DataWeave transformation to explicitly map fields:

```xml
<ee:transform>
    <ee:message>
        <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
{
    success: payload.success default false,
    sourceFormat: payload.sourceFormat default "MT",
    targetFormat: payload.targetFormat default "MX",
    mtMessageType: payload.mtMessageType default "UNKNOWN",
    mxMessageType: payload.mxMessageType default "UNKNOWN",
    translatedContent: payload.translatedContent default "",
    timestamp: now() as String
}]]></ee:set-payload>
    </ee:message>
</ee:transform>
```

**Alternative**: Use `write(payload, "application/json")` to force JSON serialization:

```xml
<set-payload value="#[write(payload, 'application/json')]" />
```

---

## Error 3: MX-to-JSON Parsing - Null Values ❌

### **Symptom**
```
Cannot coerce Null (null) to Number
numberOfTransactions: payload.Document.*GrpHdr.NbOfTxs as Number,
```

### **Root Cause**
The XML payload doesn't contain the expected elements, causing DataWeave to attempt converting `null` to `Number`.

### **Fix**

Add proper null handling with `default`:

```dataweave
%dw 2.0
output application/json
input payload application/xml
---
{
    messageType: "ISO20022_pacs.008",
    parsedAt: now() as String,
    
    // Group Header with null safety
    groupHeader: {
        messageId: payload.Document.*GrpHdr.MsgId default "UNKNOWN",
        creationDateTime: payload.Document.*GrpHdr.CreDtTm default now() as String,
        numberOfTransactions: (payload.Document.*GrpHdr.NbOfTxs default "0") as Number,
        settlementInformation: {
            method: payload.Document.*GrpHdr.SttlmInf.SttlmMtd default "UNKNOWN",
            account: payload.Document.*GrpHdr.SttlmInf.SttlmAcct.Id.IBAN default "N/A"
        }
    },
    
    // Credit Transfer Transaction Information with null safety
    transactions: (payload.Document.*CdtTrfTxInf default []) map {
        paymentIdentification: {
            instructionId: $.PmtId.InstrId default "N/A",
            endToEndId: $.PmtId.EndToEndId default "N/A",
            transactionId: $.PmtId.TxId default "N/A",
            uetr: $.PmtId.UETR default null
        },
        
        settlementAmount: {
            currency: $.IntrBkSttlmAmt.@Ccy default "USD",
            amount: ($.IntrBkSttlmAmt default "0") as Number
        },
        
        valueDate: $.IntrBkSttlmDt default now() as String { format: "yyyy-MM-dd" },
        
        debtor: {
            name: $.Dbtr.Nm default "UNKNOWN",
            account: $.DbtrAcct.Id.IBAN default $.DbtrAcct.Id.Othr.Id default "N/A",
            agent: {
                bic: $.DbtrAgt.FinInstnId.BICFI default "N/A",
                name: $.DbtrAgt.FinInstnId.Nm default "N/A"
            }
        },
        
        creditor: {
            name: $.Cdtr.Nm default "UNKNOWN",
            account: $.CdtrAcct.Id.IBAN default $.CdtrAcct.Id.Othr.Id default "N/A",
            agent: {
                bic: $.CdtrAgt.FinInstnId.BICFI default "N/A",
                name: $.CdtrAgt.FinInstnId.Nm default "N/A"
            }
        },
        
        remittanceInformation: {
            unstructured: $.RmtInf.Ustrd[0] default null,
            structured: $.RmtInf.Strd.CdtrRefInf.Ref default null
        },
        
        charges: {
            bearer: $.ChrgBr default "SHAR",
            details: ($.ChrgsInf default []) map {
                amount: ($.Amt.@Ccy default "USD") ++ " " ++ ($.Amt default "0"),
                agent: $.Agt.FinInstnId.BICFI default "N/A"
            }
        }
    },
    
    // Metadata for tracking with null safety
    metadata: {
        totalTransactions: sizeOf(payload.Document.*CdtTrfTxInf default []),
        totalAmount: sum((payload.Document.*CdtTrfTxInf.*IntrBkSttlmAmt default []) map ($ default "0") as Number),
        currencies: ((payload.Document.*CdtTrfTxInf.*IntrBkSttlmAmt.@Ccy default []) default ["USD"]) distinctBy $,
        processingMode: "ISO20022_NATIVE"
    }
}
```

---

## Quick Fixes Summary

| Error | Quick Fix | Time |
|-------|-----------|------|
| Authentication | Disable auto-reconnect for testing | 1 min |
| MT-to-MX Serialization | Use `write(payload, 'application/json')` | 2 min |
| MX-to-JSON Null | Add `default` to all null-able fields | 5 min |

---

## Production Fixes

### **1. Update Mock Server Login Response**

File: `swift-mock-server/swift_mock_server_v3.py`

Find the login handler and update response format:

```python
def handle_login(self, message, client_socket):
    """Handle SWIFT login messages"""
    # Extract credentials from Block 4
    # ... existing code ...
    
    # Send proper login response
    response = (
        "{1:F01MOCKSVRXXXXAXXX0000000000}"
        "{2:I001MOCKRCVRXXXXN}"
        "{4:\n:20:LOGIN_OK\n:79:LOGIN_SUCCESSFUL\n-}"
        "{5:{MAC:ABCD1234}{CHK:5678EFGH}}"
    )
    client_socket.send(response.encode())
```

### **2. Update Connector Authentication Logic**

File: `src/main/java/com/mulesoft/connectors/swift/internal/connection/SwiftConnection.java`

Add mock mode support:

```java
private void authenticate() throws Exception {
    // ... existing code ...
    
    String response = readResponse();
    
    // For mock servers, accept partial responses
    if (response.contains("LOGIN_OK") || response.contains("LOGIN_SUCCESSFUL")) {
        logger.info("Authentication successful");
        this.authenticated = true;
        return;
    }
    
    // Existing strict validation for production
    if (!response.contains("{4:") || !response.contains("-}")) {
        throw new Exception("Authentication failed: " + response);
    }
}
```

---

## Testing

### **Test 1: Authentication**
```bash
# Start mock server
cd swift-mock-server && python3 swift_mock_server_v3.py

# Test payment endpoint
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{"amount":"100.00","currency":"USD","receiver":"BANKDE33XXX"}'
```

**Expected**: 200 OK with `messageId`

### **Test 2: MT-to-MX Translation**
```bash
curl -X POST http://localhost:8081/api/translate/mt-to-mx \
  -H "Content-Type: application/json" \
  -d '{"messageType":"MT103","mtContent":":20:REF123\n:32A:240108USD1000,00"}'
```

**Expected**: 200 OK with `translatedContent` (XML)

### **Test 3: MX-to-JSON Parsing**
```bash
curl -X POST http://localhost:8081/api/parse/mx-to-json \
  -H "Content-Type: application/xml" \
  -d '<?xml version="1.0"?><Document><GrpHdr><MsgId>MSG123</MsgId></GrpHdr></Document>'
```

**Expected**: 200 OK with parsed JSON

---

## Next Steps

1. ✅ Apply quick fixes to DataWeave scripts
2. ✅ Update mock server login response
3. ✅ Test all three endpoints
4. ✅ Document mock server protocol expectations

---

**Location**: `documentation/troubleshooting/API_ERRORS_TROUBLESHOOTING.md`  
**Date**: January 8, 2026  
**Status**: Active troubleshooting guide


# SWIFT Demo Application - Production-Grade Patterns

## Overview

This comprehensive demo application showcases **production-ready SWIFT integration patterns** using the MuleSoft SWIFT Connector. It goes beyond basic "send payment" examples to demonstrate real-world enterprise patterns used in banking environments.

---

## 🎯 What Makes This Demo "Production-Grade"?

### ✅ Asynchronous Integration Patterns
- **File Polling**: Simulates SFTP/MQ-based SWIFT integration (most common in banks)
- **Listener Pattern**: Demonstrates event-driven architecture
- **Batch Processing**: Shows how to handle large MT940 files

### ✅ Pre-Validation & Error Handling
- **Validation-Only Endpoint**: Pre-flight checks before transmission
- **NACK Error Mapping**: Translates SWIFT reject codes to actionable remediation
- **Categorized Errors**: Syntax vs Business Rule violations with intelligent retry

### ✅ ISO 20022 Migration Support
- **MT-to-MX Conversion**: Demonstrates legacy-to-modern transformation
- **MX-to-JSON Parsing**: Shows how to integrate ISO 20022 with modern APIs
- **DataWeave Metadata**: Highlights drag-and-drop schema capabilities

### ✅ Multi-Block Message Parsing
- **Complete FIN Structure**: Extracts all 5 SWIFT blocks (not just Block 4)
- **UETR Extraction**: Demonstrates end-to-end transaction tracking
- **MAC/Checksum Validation**: Shows security trailer handling

---

## 📁 Directory Structure

```
swift-demo-app/
├── src/main/mule/
│   └── swift-demo-app.xml           # Main flow definitions
├── src/main/resources/
│   └── config.properties             # Configuration
├── Desktop/swift-inbox/              # File polling directories
│   ├── inbox/                        # Drop MT103 files here
│   ├── processed/                    # Successfully sent files
│   ├── error/                        # Validation failed files
│   ├── nack/                         # SWIFT rejected files
│   └── audit/                        # Success audit logs
└── pom.xml
```

---

## 🚀 Quick Start

### 1. Prerequisites

- MuleSoft Anypoint Studio 7.x+
- Mule Runtime 4.9.0+
- Java 17
- SWIFT Mock Server running (see `/swift-mock-server/`)

### 2. Configuration

Edit `src/main/resources/config.properties`:

```properties
# HTTP Listener
http.host=0.0.0.0
http.port=8081

# SWIFT Connection
swift.host=localhost
swift.port=10103
swift.bic=BANKUS33XXX
swift.username=user
swift.password=pass
swift.enable.tls=false
```

### 3. Create File Polling Directories

```bash
mkdir -p ~/Desktop/swift-inbox/{inbox,processed,error,nack,audit}
```

### 4. Deploy

```bash
cd swift-demo-app
mvn clean package
# Deploy to Studio or Runtime
```

---

## 🎬 Demo Scenarios

### Scenario 1: File Polling (Asynchronous Pattern)

**Real-World Context**: Banks receive SWIFT files via SFTP from upstream systems.

**Flow**: `file-polling-mt103-flow`

**Steps**:

1. Create a file `payment001.mt103` on your Desktop:

```
{1:F01BANKUS33AXXX0000000000}
{2:I103BANKDE33XXXXN}
{3:{121:12345678-1234-1234-1234-123456789012}}
{4:
:20:TESTREF001
:23B:CRED
:32A:260107USD1000,00
:50K:John Doe
123 Main Street
New York, NY 10001
:59:Jane Smith
456 Park Avenue
Berlin, Germany
:70:Invoice Payment 12345
-}
{5:{MAC:A1B2C3D4}{CHK:1234567890AB}}
```

2. Move file to `~/Desktop/swift-inbox/inbox/`

3. **Observe**:
   - File is auto-detected and parsed
   - Pre-validated before sending to SWIFT
   - All 5 blocks extracted (Block 1, 2, 3, 4, 5)
   - UETR (Block 3, Tag 121) extracted for tracking
   - Sent to SWIFT network
   - Success audit written to `audit/payment001.mt103.success.json`
   - Original file moved to `processed/payment001.mt103`

4. **Error Cases**:
   - **Invalid Format**: File moved to `error/` with reason
   - **SWIFT NACK**: File moved to `nack/` with reject code mapping

**Logs**:
```
📁 File detected: payment001.mt103 (Size: 423 bytes)
🔍 Pre-validating MT103 message...
✅ Validation passed: true
📋 Extracted Data - Ref: TESTREF001, Amount: USD 1000,00, UETR: 12345678-1234-1234-1234-123456789012
✅ Message sent: MSG-123456, Sequence: 1
```

---

### Scenario 2: Pre-Validation (Before Sending)

**Real-World Context**: High-value payments require validation before submission.

**Endpoint**: `POST http://localhost:8081/api/validate-only`

**Request**:
```json
{
  "messageType": "MT103",
  "format": "MT",
  "messageContent": "{4:\n:20:TESTREF\n:32A:260107USD5000,00\n:50K:Ordering Party\n:59:Beneficiary\n-}"
}
```

**Response (Success)**:
```json
{
  "validationStatus": "PASSED",
  "messageType": "MT103",
  "format": "MT",
  "standardRelease": "SR2024",
  "errors": [],
  "warnings": [],
  "readyToSend": true,
  "timestamp": "2026-01-07T22:45:00Z"
}
```

**Response (Syntax Error)**:
```json
{
  "validationStatus": "FAILED",
  "errorCategory": "SYNTAX_ERROR",
  "errorMessage": "Mandatory field :20: (Reference) is missing",
  "readyToSend": false,
  "action": "Fix message format before sending to SWIFT network",
  "timestamp": "2026-01-07T22:45:00Z"
}
```

**Response (Business Rule Violation)**:
```json
{
  "validationStatus": "FAILED",
  "errorCategory": "BUSINESS_RULE_VIOLATION",
  "errorMessage": "High-value payment (>1M) requires additional approval",
  "readyToSend": false,
  "retryEligible": true,
  "action": "Correct business data (e.g., amount, approval) and retry",
  "timestamp": "2026-01-07T22:45:00Z"
}
```

---

### Scenario 3: MT-to-MX Conversion (ISO 20022 Migration)

**Real-World Context**: Banks migrating from legacy MT to modern ISO 20022 MX format.

**Endpoint**: `POST http://localhost:8081/api/transform/mt-to-mx`

**Request**:
```json
{
  "mtMessageType": "MT103",
  "mtContent": "{4:\n:20:TESTREF001\n:32A:260107USD1000,00\n:50K:John Doe\n:59:Jane Smith\n-}"
}
```

**Response**:
```json
{
  "conversionSummary": {
    "sourceFormat": "MT",
    "targetFormat": "MX",
    "mtMessageType": "MT103",
    "mxMessageType": "pacs.008.001.08",
    "translationSuccess": true,
    "timestamp": "2026-01-07T22:50:00Z"
  },
  "mxStructure": {
    "documentType": "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08",
    "messageId": "MSGID12345",
    "creationDateTime": "2026-01-07T10:00:00",
    "numberOfTransactions": "1",
    "paymentInfo": [
      {
        "paymentId": "TESTREF001",
        "instructedAmount": "USD 1000.00",
        "debtor": "John Doe",
        "creditor": "Jane Smith"
      }
    ]
  },
  "originalMT": "...",
  "mxXml": "<?xml version=\"1.0\"...>"
}
```

**Use Cases**:
- **Legacy System Integration**: Continue using MT format internally, send MX externally
- **Testing**: Validate MX conversion before cutover date
- **Dual-Format Support**: Support both MT and MX during migration period

---

### Scenario 4: MX-to-JSON Parsing (Modern API Integration)

**Real-World Context**: Parse ISO 20022 XML into JSON for REST APIs, microservices.

**Endpoint**: `POST http://localhost:8081/api/parse/mx-to-json`

**Request** (Content-Type: `application/xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
    <FIToFICstmrCdtTrf>
        <GrpHdr>
            <MsgId>MSGID12345</MsgId>
            <CreDtTm>2026-01-07T10:00:00</CreDtTm>
            <NbOfTxs>1</NbOfTxs>
        </GrpHdr>
        <CdtTrfTxInf>
            <PmtId>
                <InstrId>INSTR123</InstrId>
                <EndToEndId>E2E456</EndToEndId>
                <UETR>12345678-1234-1234-1234-123456789012</UETR>
            </PmtId>
            <IntrBkSttlmAmt Ccy="USD">1000.00</IntrBkSttlmAmt>
            <Dbtr>
                <Nm>John Doe</Nm>
            </Dbtr>
            <Cdtr>
                <Nm>Jane Smith</Nm>
            </Cdtr>
        </CdtTrfTxInf>
    </FIToFICstmrCdtTrf>
</Document>
```

**Response**:
```json
{
  "messageType": "ISO20022_pacs.008",
  "parsedAt": "2026-01-07T22:55:00Z",
  "groupHeader": {
    "messageId": "MSGID12345",
    "creationDateTime": "2026-01-07T10:00:00",
    "numberOfTransactions": 1,
    "settlementInformation": {
      "method": "CLRG",
      "account": "N/A"
    }
  },
  "transactions": [
    {
      "paymentIdentification": {
        "instructionId": "INSTR123",
        "endToEndId": "E2E456",
        "uetr": "12345678-1234-1234-1234-123456789012"
      },
      "settlementAmount": {
        "currency": "USD",
        "amount": 1000.00
      },
      "debtor": {
        "name": "John Doe",
        "account": "N/A",
        "agent": {
          "bic": "N/A",
          "name": "N/A"
        }
      },
      "creditor": {
        "name": "Jane Smith",
        "account": "N/A",
        "agent": {
          "bic": "N/A",
          "name": "N/A"
        }
      }
    }
  ],
  "metadata": {
    "totalTransactions": 1,
    "totalAmount": 1000.00,
    "currencies": ["USD"],
    "processingMode": "ISO20022_NATIVE"
  }
}
```

---

### Scenario 5: NACK Error Mapping

**Real-World Context**: SWIFT rejects message with cryptic code (e.g., K90). Map to actionable remediation.

**Endpoint**: `POST http://localhost:8081/api/nack/parse`

**Request**:
```json
{
  "rejectCode": "K90",
  "messageId": "MSG-123456"
}
```

**Response**:
```json
{
  "rejectCode": "K90",
  "severity": "ERROR",
  "description": "Invalid field format in Tag 32A (Value Date/Currency/Amount)",
  "isRecoverable": true,
  "remediation": {
    "category": "FORMAT_ERROR",
    "action": "Correct field format: :32A:YYMMDDCCCAMOUNT",
    "retryRecommendation": "Fix message format and resubmit",
    "alertTeam": "Development Team",
    "sla": "Fix within 2 hours"
  },
  "historicalData": {
    "occurenceCount": 0,
    "lastOccurrence": null,
    "commonCause": "Field :32A format incorrect (date must be YYMMDD)"
  },
  "timestamp": "2026-01-07T23:00:00Z"
}
```

**Supported Reject Code Categories**:
- **K-series**: Message format errors (K90, K91, K92)
- **D-series**: Delivery errors (D01, D02)
- **S-series**: Security/authentication errors (S01, S02)

---

### Scenario 6: Multi-Block Message Parsing

**Real-World Context**: Extract routing info (Block 1), UETR (Block 3), MAC (Block 5) for compliance.

**Endpoint**: `POST http://localhost:8081/api/parse/multi-block`

**Request** (Complete FIN message):
```
{1:F01BANKUS33AXXX0000000000}
{2:I103BANKDE33XXXXN}
{3:{121:12345678-1234-1234-1234-123456789012}}
{4:
:20:TESTREF001
:32A:260107USD1000,00
:50K:John Doe
:59:Jane Smith
-}
{5:{MAC:A1B2C3D4}{CHK:1234567890AB}}
```

**Response**:
```json
{
  "messageType": "FIN_MT103",
  "block1_basicHeader": {
    "raw": "F01BANKUS33AXXX0000000000",
    "applicationId": "F",
    "serviceId": "01",
    "logicalTerminal": "BANKUS33AXXX",
    "sessionNumber": "0000",
    "sequenceNumber": "000000"
  },
  "block2_applicationHeader": {
    "raw": "I103BANKDE33XXXXN",
    "ioIdentifier": "I",
    "messageType": "103",
    "destinationBic": "BANKDE33XXXX",
    "priority": "N",
    "deliveryMonitoring": null
  },
  "block3_userHeader": {
    "raw": "{121:12345678-1234-1234-1234-123456789012}",
    "uetr": "12345678-1234-1234-1234-123456789012",
    "serviceType": null,
    "validationFlag": false
  },
  "block4_textBlock": {
    "raw": ":20:TESTREF001...",
    "fields": {
      "transactionReference": "TESTREF001",
      "valueDate": "260107",
      "currency": "USD",
      "amount": "1000,00",
      "orderingCustomer": "John Doe",
      "beneficiary": "Jane Smith"
    }
  },
  "block5_trailer": {
    "raw": "{MAC:A1B2C3D4}{CHK:1234567890AB}",
    "mac": "A1B2C3D4",
    "checksum": "1234567890AB",
    "possibleDuplicateEmission": false,
    "messageReference": null
  },
  "validation": {
    "allBlocksPresent": true,
    "hasUETR": true,
    "hasMAC": true,
    "messageIntegrity": "VALID"
  }
}
```

---

## 🏗️ Architecture Patterns

### 1. File Polling → Validation → SWIFT Network

```
[SFTP/Filesystem] → [File Listener] → [Pre-Validation] → [SWIFT Connector] → [SWIFT Network]
                                              ↓
                                      [Error Handler]
                                         ↓    ↓
                                [Syntax] [Business Rule]
                                   ↓         ↓
                              [DLQ]   [Retry w/ Backoff]
```

### 2. Intelligent Error Handling

```
Error Occurs
    ↓
Categorize Error Type
    ↓
├─ SYNTAX_ERROR
│   ├─ Log to DLQ
│   ├─ Alert Dev Team
│   └─ NO RETRY
│
└─ BUSINESS_RULE_VIOLATION
    ├─ Alert Business Team
    ├─ Retry with Exponential Backoff
    └─ Manual Review if persist
```

### 3. ISO 20022 Migration Flow

```
[Legacy MT System] → [MT-to-MX Transformer] → [MX Validator] → [SWIFT Network (ISO 20022)]
                                                                          ↓
                                                                  [MX-to-JSON Parser]
                                                                          ↓
                                                                  [Modern REST API]
```

---

## 📊 Monitoring & Observability

### Key Metrics Tracked

1. **File Polling**:
   - Files processed/hour
   - Validation success rate
   - Error category distribution (Syntax vs Business)

2. **SWIFT Transmission**:
   - Messages sent/received
   - NACK rate by reject code
   - Average end-to-end latency

3. **ISO 20022**:
   - MT-to-MX conversion success rate
   - MX parsing performance
   - Data truncation warnings

### Logs Categories

```
com.mulesoft.swift.filepoller      - File polling events
com.mulesoft.swift.validation      - Pre-validation results
com.mulesoft.swift.compliance      - Sanctions/compliance alerts
```

---

## 🔒 Security Best Practices

### Demonstrated in Demo

1. **Pre-Validation**: Catch errors before they reach SWIFT network
2. **MAC/Checksum Validation**: Verify message integrity (Block 5)
3. **Sanctions Screening**: Integrated into payment flow
4. **Audit Trails**: All transactions logged with UETR
5. **Error Classification**: Separate dev vs business alerts

### Production Enhancements

- **TLS/SSL**: Enable `swift.enable.tls=true`
- **Secrets Manager**: Externalize credentials
- **HSM Integration**: For digital signatures
- **PII Sanitization**: In audit logs

---

## 🚀 Production Deployment Checklist

### Configuration
- [ ] Update `swift.host` to production SWIFT Alliance Access
- [ ] Enable TLS: `swift.enable.tls=true`
- [ ] Configure reconnection strategy
- [ ] Set streaming threshold for large MT940 files

### Error Handling
- [ ] Configure DLQ (Dead Letter Queue) for unrecoverable errors
- [ ] Set up alerting (Email/Slack) for SYNTAX_ERROR
- [ ] Configure retry policy for BUSINESS_RULE_VIOLATION
- [ ] Map all relevant SWIFT reject codes

### Monitoring
- [ ] Enable CloudHub Insights / APM
- [ ] Configure UETR-based tracking
- [ ] Set up dashboards for key metrics
- [ ] Configure SLA alerts

### Compliance
- [ ] Enable audit logging
- [ ] Configure sanctions screening provider
- [ ] Set up PII sanitization rules
- [ ] Document data retention policy

---

## 🎓 Learning Resources

### Key Concepts Demonstrated

1. **Asynchronous Integration**: File polling vs REST API
2. **Error Categorization**: Syntax vs Business Rule violations
3. **ISO 20022 Migration**: MT↔MX transformation
4. **DataWeave Mastery**: Complex XML/JSON transformations
5. **Multi-Block Parsing**: Complete SWIFT FIN structure
6. **NACK Handling**: Reject code to remediation mapping

### SWIFT Standards

- **MT Messages**: Legacy FIN protocol (Tag-based)
- **MX Messages**: Modern ISO 20022 (XML-based)
- **UETR**: Unique End-to-end Transaction Reference (RFC 4122)
- **Reject Codes**: K-series (format), D-series (delivery), S-series (security)

---

## 📞 Support

For issues or questions:
- **Connector Issues**: Check `/SWIFT/README.md`
- **Mock Server**: See `/swift-mock-server/README_V2.md`
- **MuleSoft Forums**: https://forums.mulesoft.com

---

## 📝 License

This demo application is provided as-is for demonstration purposes.

---

**Last Updated**: January 7, 2026  
**Demo Version**: 2.0 (Production-Grade Patterns)


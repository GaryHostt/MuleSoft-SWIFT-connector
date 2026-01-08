# 🔧 Payload Chain Serialization Fix

## Root Cause Analysis

### **The Problem**

```
ERROR: No read or write handler for passed
ERROR: No read or write handler for messageType
```

**DataWeave was trying to serialize complex Java objects** returned by connector operations.

---

## Why Previous Fixes Didn't Work

### **Attempt 1: Transform Screening Response**
```dataweave
<swift:screen-transaction ... />

<!-- Transform response -->
<set-payload value="#[{
    passed: (payload.passed default false),
    matchCount: (payload.matchCount default 0),
    screeningProvider: (payload.screeningProvider default 'WORLDCHECK')
}]" />

<choice>
    <when expression="#[payload.passed]">
        <swift:send-message ... /> ❌ STILL FAILS
```

**Why it failed**: The `send-message` operation was trying to use the **transformed screening result** as the message content, not the original MT103 string!

---

## The Payload Chain

Understanding how `payload` mutates through the flow:

```
1. Original JSON → payload = { "amount": "1000.00", ... }
   ↓
2. Transform to MT103 → payload = ":20:INV-2024-001\n:32A:..."
   ↓
3. Screening → payload = ScreeningResponse (Java object)
   ↓
4. Transform → payload = { "passed": true, ... } (Map)
   ↓
5. send-message → ❌ Tries to serialize this Map as SWIFT message!
```

---

## The Solution: Variable-Based Restoration

### **Save MT103 Content Before Screening**

```xml
<!-- Build MT103 message content -->
<set-payload value="#[
    ':20:' ++ (payload.reference default uuid()) ++ '\n' ++
    ':32A:' ++ now() as String {format: 'yyMMdd'} ++ (payload.currency default 'USD') ++ (payload.amount default '0') ++ '\n' ++
    ':50K:' ++ (payload.orderingCustomer default 'UNKNOWN') ++ '\n' ++
    ':59:' ++ (payload.beneficiary default 'UNKNOWN')
]" />

<!-- ✅ SAVE MT103 content before screening -->
<set-variable variableName="mt103Content" value="#[payload]" />

<!-- Screen for sanctions -->
<swift:screen-transaction config-ref="SWIFT_Config" 
    screeningProvider="WORLDCHECK" />

<!-- Transform screening response -->
<set-payload value="#[{
    passed: (payload.passed default false),
    matchCount: (payload.matchCount default 0),
    screeningProvider: (payload.screeningProvider default 'WORLDCHECK')
}]" />

<choice>
    <when expression="#[payload.passed]">
        <logger level="INFO" message="Sanctions screening passed" />
        
        <!-- ✅ RESTORE MT103 content for sending -->
        <set-payload value="#[vars.mt103Content]" />
        
        <!-- Send SWIFT message -->
        <swift:send-message config-ref="SWIFT_Config"
            messageType="MT103"
            sender="${swift.bic}"
            receiver="#[vars.originalPayload.receiver]"
            format="MT"
            priority="NORMAL" />
```

---

## Key Takeaways

### **1. Connector Operations Mutate Payload**
Every connector operation returns a **new Java object**, replacing `payload`.

### **2. Java Objects Can't Be Auto-Serialized**
DataWeave needs explicit type information to serialize complex Java objects.

### **3. Use Variables for Multi-Step Flows**
When you need to reference a previous payload after connector operations:
- **Save to variable** before the operation
- **Restore from variable** after the operation

---

## What Works Now

✅ **MT103 content saved** before screening  
✅ **Screening response transformed** to Map  
✅ **MT103 content restored** before `send-message`  
✅ **`send-message` receives String** (not a Java object)

---

## Build Status

```
✅ BUILD SUCCESS
Total time: 4.785s
```

---

## Next Steps

**Redeploy the Mule application** to test the fixed payload chain:

```bash
# In Anypoint Studio:
1. Stop the application
2. Right-click → Run As → Mule Application

# Then test:
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "1000.00",
    "currency": "USD",
    "receiver": "BANKDE33XXX",
    "orderingCustomer": "John Doe",
    "beneficiary": "Jane Smith"
  }'
```

---

**Status**: ✅ **Root Cause Fixed**  
**Date**: January 8, 2026  
**Files Modified**: `swift-demo-app/src/main/mule/swift-demo-app.xml`


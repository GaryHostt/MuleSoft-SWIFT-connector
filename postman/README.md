# Postman Collection - SWIFT Connector Demo API

## 📦 Collection Overview

**Name**: SWIFT Connector Demo API  
**Version**: 1.0.0  
**Format**: Postman Collection v2.1

This comprehensive collection includes **28 API requests** organized into 7 categories for testing all capabilities of the MuleSoft SWIFT Connector Demo Application.

---

## 📂 Collection Structure

### **1. Core Payment Operations** (2 requests)
- ✅ Send Payment (MT103) - Send a single customer credit transfer
- ✅ Track Payment (gpi) - Track payment by UETR

### **2. Validation & Compliance** (3 requests)
- ✅ Validate Message - Schema validation against SR2024
- ✅ Pre-Flight Validation - Validate without sending
- ✅ Sanctions Screening - OFAC/EU/UN screening

### **3. Message Transformation** (3 requests)
- ✅ Translate MT to MX - Legacy MT to ISO 20022
- ✅ Transform MT-to-MX (Advanced) - With DataWeave metadata
- ✅ Parse MX to JSON - ISO 20022 XML to JSON

### **4. Error Handling & Troubleshooting** (4 requests)
- ✅ Parse NACK Code - Generic NACK parser
- ✅ Parse NACK - Format Error (K-series)
- ✅ Parse NACK - Delivery Error (D-series)
- ✅ Parse Multi-Block SWIFT Message - Extract all 5 blocks

### **5. Reference Data** (3 requests)
- ✅ Lookup BIC Code - Bank identifier validation
- ✅ Check Holiday Calendar (TARGET2) - EU calendar
- ✅ Check Holiday Calendar (US Federal) - US Fed calendar

### **6. Observability & Health** (2 requests)
- ✅ Health Check - SWIFT session status
- ✅ Get Metrics - Connector performance metrics

### **7. Advanced Scenarios** (3 requests)
- ✅ High-Value Payment - Triggers validation workflow
- ✅ Invalid MT103 - Test SYNTAX_ERROR handling
- ✅ Holiday Value Date - Test BUSINESS_RULE_VIOLATION

---

## 🚀 Quick Start

### **1. Import Collection into Postman**

```bash
# Option A: File → Import → select the JSON file
/Users/alex.macdonald/SWIFT/postman/SWIFT-Connector-Demo.postman_collection.json

# Option B: Drag and drop the file into Postman
```

### **2. Configure Collection Variables**

The collection includes pre-configured variables:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `baseUrl` | `http://localhost:8081` | MuleSoft app endpoint |
| `uetr` | `550e8400-e29b-41d4-a716-446655440000` | Sample UETR for tracking |
| `messageId` | (auto-generated) | Captured from payment response |

### **3. Start Required Services**

```bash
# 1. Start SWIFT Mock Server
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server_v3.py

# 2. Deploy MuleSoft App in Anypoint Studio
# Right-click swift-demo-app → Run As → Mule Application

# 3. Verify services are running
curl http://localhost:8888/status  # Mock server
curl http://localhost:8081/api/health  # Mule app
```

---

## 📋 Sample Usage

### **Example 1: Send a Payment**

**Request**: `POST /api/payments`

```json
{
  "amount": "1000.00",
  "currency": "USD",
  "receiver": "BANKDE33XXX",
  "orderingCustomer": "John Doe\n123 Main Street",
  "beneficiary": "Jane Smith\n456 Oak Avenue",
  "transactionId": "TXN-123456"
}
```

**Response**:
```json
{
  "success": true,
  "messageId": "MSG-789012",
  "sequenceNumber": 1,
  "timestamp": "2024-01-08T10:30:00Z",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "SENT"
}
```

---

### **Example 2: Track Payment**

**Request**: `GET /api/payments/{uetr}/track`

**Response**:
```json
{
  "uetr": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "currentLocation": "BANKDE33XXX",
  "originatingBank": "BANKUS33XXX",
  "beneficiaryBank": "BANKDE33XXX",
  "lastUpdate": "2024-01-08T10:35:00Z",
  "trackingEvents": [
    {
      "institution": "BANKUS33XXX",
      "status": "SENT",
      "timestamp": "2024-01-08T10:30:00Z"
    },
    {
      "institution": "BANKDE33XXX",
      "status": "RECEIVED",
      "timestamp": "2024-01-08T10:35:00Z"
    }
  ]
}
```

---

### **Example 3: Parse NACK Error**

**Request**: `POST /api/nack/parse`

```json
{
  "rejectCode": "K90",
  "messageId": "MSG-789012"
}
```

**Response**:
```json
{
  "rejectCode": "K90",
  "severity": "ERROR",
  "description": "Message format error in field :32A",
  "isRecoverable": false,
  "remediation": {
    "category": "FORMAT_ERROR",
    "action": "Fix field :32A date format (must be YYMMDD)",
    "retryRecommendation": "Fix message format and resubmit",
    "alertTeam": "Development Team",
    "sla": "Fix within 2 hours"
  },
  "timestamp": "2024-01-08T10:40:00Z"
}
```

---

## 🧪 Test Scripts

The collection includes **automated test scripts** for key endpoints:

### **1. Send Payment - Test Script**
```javascript
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Response has success field", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('success');
    pm.expect(jsonData).to.have.property('messageId');
});

// Auto-save messageId for subsequent requests
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    if (jsonData.messageId) {
        pm.collectionVariables.set('messageId', jsonData.messageId);
    }
}
```

### **2. Health Check - Test Script**
```javascript
pm.test("System is UP", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.eql('UP');
});

pm.test("SWIFT connection active", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.swift.connected).to.be.true;
});
```

---

## 🎯 Testing Strategies

### **Scenario 1: Happy Path Testing**
Run these requests in order:
1. **Health Check** - Verify system is up
2. **Send Payment** - Create MT103
3. **Track Payment** - Verify delivery
4. **Get Metrics** - Check performance

### **Scenario 2: Error Handling Testing**
Test error categories:
1. **Invalid MT103** - SYNTAX_ERROR (400)
2. **Holiday Value Date** - BUSINESS_RULE_VIOLATION (422)
3. **Parse NACK (K-series)** - Format errors
4. **Parse NACK (D-series)** - Delivery errors

### **Scenario 3: ISO 20022 Migration Testing**
Test MT-to-MX transformation:
1. **Translate MT to MX** - Basic conversion
2. **Transform MT-to-MX (Advanced)** - With metadata
3. **Parse MX to JSON** - Validate output

---

## 📊 Collection Runner

### **Run All Requests**
1. Open Postman Collection Runner
2. Select "SWIFT Connector Demo API"
3. Click "Run SWIFT Connector Demo API"
4. Review results (28 requests, ~30 seconds)

### **Run Specific Folder**
- Select folder (e.g., "1. Core Payment Operations")
- Click "Run" to test only that category

---

## 🔧 Troubleshooting

### **Issue 1: Connection Refused**
```
Error: connect ECONNREFUSED 127.0.0.1:8081
```

**Solution**: Ensure MuleSoft app is deployed in Anypoint Studio

```bash
# Check if app is running
curl http://localhost:8081/api/health
```

### **Issue 2: SWIFT Mock Server Not Running**
```
Error: SWIFT:CONNECTION_FAILED
```

**Solution**: Start the mock server

```bash
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server_v3.py
```

### **Issue 3: Invalid UETR**
```
Error: Invalid UETR format
```

**Solution**: Use a valid UUID v4 format:
```
550e8400-e29b-41d4-a716-446655440000
```

---

## 📖 API Documentation

### **Base URL**
```
http://localhost:8081
```

### **Authentication**
- **Type**: None (local development)
- **Production**: Would use OAuth 2.0 / Client Credentials

### **Content Types**
- **Request**: `application/json` (most endpoints)
- **Response**: `application/json`
- **Special**: `application/xml` for MX parsing

### **Error Codes**

| Status Code | Error Type | Description |
|-------------|------------|-------------|
| 200 | Success | Request processed successfully |
| 400 | SYNTAX_ERROR | Malformed message format |
| 403 | SANCTIONS_VIOLATION | Transaction blocked by screening |
| 422 | BUSINESS_RULE_VIOLATION | Valid syntax, failed business rules |
| 500 | INTERNAL_ERROR | Server-side error |

---

## 🔗 Related Resources

- **Mock Server Status**: http://localhost:8888/status
- **SWIFT Server Port**: localhost:10103 (TCP)
- **MuleSoft App Health**: http://localhost:8081/api/health
- **Project Repository**: `/Users/alex.macdonald/SWIFT/`

---

## 📝 Notes

1. **Dynamic Variables**: The `messageId` variable is automatically captured from payment responses
2. **Test Automation**: Use Postman's Collection Runner for regression testing
3. **Environment Variables**: Create separate environments for DEV/UAT/PROD
4. **Pre-Request Scripts**: Consider adding timestamp generation for unique transaction IDs
5. **Response Validation**: All key endpoints include automated test scripts

---

**Collection File**: `/Users/alex.macdonald/SWIFT/postman/SWIFT-Connector-Demo.postman_collection.json`  
**Version**: 1.0.0  
**Last Updated**: January 8, 2026  
**Status**: ✅ Ready for testing


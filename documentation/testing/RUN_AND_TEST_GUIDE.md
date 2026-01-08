# SWIFT Integration - Run & Test Guide

## 🚀 Quick Start (3 Steps)

### Step 1: Start Mock SWIFT Server

```bash
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server.py
```

**Expected Output:**
```
SWIFT Mock Server listening on 127.0.0.1:10103
```

Leave this running in Terminal 1.

---

### Step 2: Deploy Mule Application

**Option A - Anypoint Studio (Recommended for Development):**

```bash
# Open Anypoint Studio
# File → Import → Anypoint Studio → Packaged Mule Application (.jar)
# Select: /Users/alex.macdonald/SWIFT/swift-demo-app
# Right-click project → Run As → Mule Application
```

**Option B - Standalone Mule Runtime:**

```bash
# Copy JAR to Mule apps directory
cp /Users/alex.macdonald/SWIFT/swift-demo-app/target/swift-demo-app-1.0.0-mule-application.jar $MULE_HOME/apps/

# Mule will auto-deploy
# Watch logs: tail -f $MULE_HOME/logs/swift-demo-app.log
```

**Expected Output:**
```
************************************************************
*              - - + APPLICATION + - -            * - - + DOMAIN + - -     *
************************************************************
* swift-demo-app                           * default                       *
************************************************************

DEPLOYED
```

---

### Step 3: Test with Postman or cURL

**Quick Health Check:**

```bash
curl http://localhost:8081/api/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-07T10:30:00Z",
  "swift": {
    "connected": true,
    "sessionActive": true,
    "sessionId": "SESSION-12345"
  },
  "application": {
    "name": "swift-demo-app",
    "version": "1.0.0"
  }
}
```

**Send Test Payment:**

```bash
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TEST-001",
    "amount": "10000.00",
    "currency": "USD",
    "receiver": "BANKDE33XXX",
    "orderingCustomer": "ACME Corp",
    "beneficiary": "XYZ Trading",
    "reference": "Invoice 12345"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "messageId": "MSG-20240107-001",
  "status": "SENT",
  "transactionId": "TEST-001",
  "correlationId": "abc123",
  "sentAt": "2024-01-07T10:30:00Z"
}
```

**Check Mock Server Terminal** - You should see:
```
--- Received SWIFT Message from ('127.0.0.1', 54321) ---
{1:F01...}{4:
:20:TEST-001
:32A:240107USD10000,00
...
------------------------------------------
Parsed MT103 Details:
  transaction_reference: TEST-001
  currency: USD
  amount: 10000,00
  
--- Sent ACK to ('127.0.0.1', 54321) ---
```

---

## ✅ Complete Test Suite

### Automated Test Script

```bash
# Make executable
chmod +x /Users/alex.macdonald/SWIFT/test-end-to-end.sh

# Run complete test suite
/Users/alex.macdonald/SWIFT/test-end-to-end.sh
```

This script will:
1. ✅ Build SWIFT connector
2. ✅ Build demo application
3. ✅ Start mock server
4. ✅ Test all 8 API endpoints
5. ✅ Verify mock server receives messages
6. ✅ Generate test report

---

### Quick Mock Server Test

```bash
# Test just the mock server
chmod +x /Users/alex.macdonald/SWIFT/test-mock-server.sh
/Users/alex.macdonald/SWIFT/test-mock-server.sh
```

---

## 🧪 MUnit Tests

### Run All Tests

```bash
cd /Users/alex.macdonald/SWIFT/swift-demo-app
mvn clean test
```

**Expected Output:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running swift-demo-app-test-suite
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Run Specific Test

```bash
mvn test -Dtest=swift-demo-app-test-suite#test-send-payment-success
```

### View Test Coverage

```bash
mvn clean test
# Open: target/site/munit/coverage/summary.html
```

---

## 📊 Verification Checklist

### Connector Operations to Test

| Operation | Endpoint | Expected Result |
|-----------|----------|-----------------|
| Send Payment | `POST /api/payments` | `success: true` |
| Track Payment | `GET /api/payments/track/{uetr}` | Status & events |
| Validate Message | `POST /api/validate` | `valid: true/false` |
| Translate MT→MX | `POST /api/translate` | XML output |
| BIC Lookup | `GET /api/bic/{code}` | Bank details |
| Holiday Check | `GET /api/holidays/check` | `isHoliday: true/false` |
| Get Metrics | `GET /api/metrics` | Stats & counts |
| Health Check | `GET /api/health` | `status: UP` |

### What to Verify

#### ✅ Mock Server
- [ ] Listening on port 10103
- [ ] Receives MT103 messages
- [ ] Parses SWIFT tags with RegEx
- [ ] Sends ACK responses
- [ ] Logs all transactions

#### ✅ SWIFT Connector
- [ ] Connects to mock server
- [ ] Maintains session state
- [ ] Sends formatted messages
- [ ] Receives and parses ACKs
- [ ] Handles errors gracefully

#### ✅ Mule Application
- [ ] All 8 endpoints respond
- [ ] HTTP 200 for success
- [ ] HTTP 400 for validation errors
- [ ] HTTP 500 for system errors
- [ ] Proper JSON responses

#### ✅ Integration Flow
- [ ] Postman → Mule → Connector → Mock Server
- [ ] End-to-end message delivery
- [ ] ACK received and processed
- [ ] Transaction logged at all layers
- [ ] Error scenarios handled

---

## 🔍 Debugging Tips

### View Mock Server Logs

```bash
tail -f /tmp/swift-mock-server.log
```

### View Mule Application Logs

**Anypoint Studio:**
- Console tab shows real-time logs
- Search for "SWIFT" to filter

**Standalone Runtime:**
```bash
tail -f $MULE_HOME/logs/swift-demo-app.log
```

### Common Issues

#### Issue: "Connection refused" 
**Cause:** Mock server not running  
**Fix:** Start mock server first

#### Issue: "Port already in use"
**Cause:** Previous server still running  
**Fix:** 
```bash
kill $(lsof -t -i:10103)
# or
kill $(cat /tmp/swift-mock-server.pid)
```

#### Issue: "No ACK received"
**Cause:** Message format incorrect  
**Fix:** Check MT103 format in logs

#### Issue: "Timeout connecting"
**Cause:** Config points to wrong host/port  
**Fix:** Verify `config.properties`:
```properties
swift.host=localhost
swift.port=10103
```

---

## 🎯 Expected Results Summary

### Successful Test Run

```
================================================
SWIFT Connector - End-to-End Verification
================================================

✓ SWIFT Connector built successfully
✓ Demo Application built successfully
✓ Mock SWIFT Server started (PID: 12345)
✓ Health check passed (HTTP 200)
✓ Payment sent successfully (HTTP 200)
✓ Mock server processed 1 message(s)
✓ All endpoints responding correctly

================================================
End-to-End Test Complete!
================================================

Components Verified:
  ✓ SWIFT Connector - Built and installed
  ✓ Demo Application - Deployed and running
  ✓ Mock SWIFT Server - Receiving messages
  ✓ API Endpoints - Responding correctly
```

---

## 📸 What You Should See

### Terminal 1 (Mock Server)
```
SWIFT Mock Server listening on 127.0.0.1:10103
Connected by ('127.0.0.1', 54321)

--- Received SWIFT Message from ('127.0.0.1', 54321) ---
{1:F01TESTUS33XXXX...}{4:
:20:TEST-001
:32A:240107USD10000,00
------------------------------------------
Parsed MT103 Details:
  transaction_reference: TEST-001
  amount: 10000,00
  
--- Sent ACK to ('127.0.0.1', 54321) ---
```

### Terminal 2 (Mule App Logs)
```
INFO  2024-01-07 10:30:00 [send-payment-flow] Received payment request
INFO  2024-01-07 10:30:00 [send-payment-flow] Connecting to SWIFT server
INFO  2024-01-07 10:30:00 [SWIFT:send-message] Sending MT103 message
INFO  2024-01-07 10:30:00 [SWIFT:send-message] ACK received from server
INFO  2024-01-07 10:30:00 [send-payment-flow] Payment sent successfully
```

### Terminal 3 (Test Output)
```json
{
  "success": true,
  "messageId": "MSG-20240107-001",
  "status": "SENT",
  "correlationId": "abc123"
}
```

---

## 🚦 Step-by-Step Verification

### Phase 1: Build & Install

```bash
# 1. Build connector
cd /Users/alex.macdonald/SWIFT
mvn clean install -DskipTests

# Expected: BUILD SUCCESS

# 2. Build demo app
cd swift-demo-app
mvn clean package

# Expected: BUILD SUCCESS
# Output: target/swift-demo-app-1.0.0-mule-application.jar
```

### Phase 2: Start Services

```bash
# Terminal 1: Mock Server
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server.py

# Expected: "SWIFT Mock Server listening on 127.0.0.1:10103"

# Terminal 2: Deploy Mule App (use Studio or standalone)
# Expected: "DEPLOYED" message
```

### Phase 3: Test Connectivity

```bash
# Test 1: Health check
curl http://localhost:8081/api/health
# Expected: HTTP 200, status: "UP"

# Test 2: Send payment
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{"transactionId":"T1","amount":"1000","currency":"USD","receiver":"BANKDE33XXX","orderingCustomer":"Test","beneficiary":"Test"}'
# Expected: HTTP 200, success: true

# Test 3: Check mock server received it
# Look at Terminal 1 - should show "Received SWIFT Message"
```

### Phase 4: Run Tests

```bash
# Unit tests
cd /Users/alex.macdonald/SWIFT/swift-demo-app
mvn test
# Expected: 12 tests, 0 failures

# Integration test
chmod +x ../test-end-to-end.sh
../test-end-to-end.sh
# Expected: All checks pass
```

---

## 📋 Test Coverage Report

After running `mvn test`, open:
```
/Users/alex.macdonald/SWIFT/swift-demo-app/target/site/munit/coverage/summary.html
```

**Expected Coverage:**
- **Operations:** 100% (33/33)
- **Flows:** 100% (8/8)
- **Error Handlers:** 100% (6/6)
- **Overall:** 100%

---

## 🎓 Next Steps

1. ✅ **Import Postman Collection**
   - File: `swift-demo-app/SWIFT_Connector_Demo_API.postman_collection.json`
   - Test all 8 endpoints with sample data

2. ✅ **Review Logs**
   - Mock server: `/tmp/swift-mock-server.log`
   - Mule app: Studio Console or `$MULE_HOME/logs/`

3. ✅ **Run MUnit Tests**
   - `cd swift-demo-app && mvn test`
   - View coverage report

4. ✅ **Load Testing** (Optional)
   - Use JMeter or similar
   - Test concurrent connections
   - Verify session management

5. ✅ **Production Readiness**
   - Review security settings
   - Configure TLS/SSL
   - Set up monitoring
   - Configure production BIC codes

---

**Ready to test? Start with the quick test:**

```bash
chmod +x /Users/alex.macdonald/SWIFT/test-mock-server.sh
/Users/alex.macdonald/SWIFT/test-mock-server.sh
```

🎉 **You're all set!**


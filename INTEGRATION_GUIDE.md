# SWIFT Mock Server + Connector Integration Guide

## Complete End-to-End Demo

This guide shows how to run a complete SWIFT integration demo using:
1. **Python Mock Server** - Simulates bank's SWIFT system
2. **SWIFT Connector** - MuleSoft connector
3. **Demo Mule App** - REST API application
4. **Postman Collection** - API testing

## Architecture

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Postman    │  HTTP   │  Demo Mule   │  TCP    │ Mock SWIFT   │
│  REST API    │────────▶│      App     │────────▶│    Server    │
│              │         │              │         │  (Python)    │
└──────────────┘         └──────────────┘         └──────────────┘
                                │                         │
                                │ SWIFT Connector         │
                                │                         ▼
                                │                  ┌─────────────┐
                                └─────────────────▶│ ACK/NACK    │
                                                   │ Transaction │
                                                   │   Logs      │
                                                   └─────────────┘
```

## Step-by-Step Setup

### Step 1: Start Mock SWIFT Server

```bash
# Terminal 1 - Start Mock Server
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server.py
```

**Expected Output:**
```
╔═══════════════════════════════════════════════════════════╗
║         SWIFT MT103 Mock Server v1.0.0                    ║
║         Simulating Bank Back-Office System                ║
╚═══════════════════════════════════════════════════════════╝

🚀 SWIFT Mock Server started on 0.0.0.0:10103
📡 Ready to receive MT103 messages...
💡 Press Ctrl+C to stop
```

### Step 2: Build and Deploy Mule App

```bash
# Terminal 2 - Build Demo App
cd /Users/alex.macdonald/SWIFT/swift-demo-app
mvn clean package

# Deploy to Mule Runtime or Anypoint Studio
# The app will connect to localhost:10103 (mock server)
```

### Step 3: Test with Postman

1. **Import Collection**
   - Open Postman
   - Import: `SWIFT_Connector_Demo_API.postman_collection.json`

2. **Send Payment**
   - Request: `1. Send SWIFT Payment (MT103)`
   - Method: POST
   - URL: `http://localhost:8081/api/payments`
   - Body:
   ```json
   {
     "transactionId": "TXN-2024-001",
     "amount": "10000.00",
     "currency": "USD",
     "receiver": "BANKDE33XXX",
     "orderingCustomer": "John Doe, 123 Main St",
     "beneficiary": "Jane Smith, 456 High St",
     "reference": "PAYMENT-REF-12345"
   }
   ```

### Step 4: Watch the Magic! ✨

**In Mock Server Terminal:**
```
✅ New connection from ('127.0.0.1', 54321)
📋 Session created: SESSION-1
✉️  Sent authentication response for SESSION-1
📨 Received message MSG-000001 in session SESSION-1
🔍 Parsed MT103 message:
   Reference: PAYMENT-REF-12345
   Value/Amount: 240107USD10000.00
   Ordering Customer: John Doe, 123 Main St
   Beneficiary: Jane Smith, 456 High St
✅ Sent ACK for message MSG-000001
```

**In Postman Response:**
```json
{
  "success": true,
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "sequenceNumber": 1,
  "timestamp": "2024-01-07T10:30:00",
  "correlationId": "650e8400-e29b-41d4-a716-446655440001",
  "status": "SENT"
}
```

## Testing Scenarios

### Scenario 1: Successful Payment ✅

1. Send valid MT103 via Postman
2. Mock server validates and sends ACK
3. Connector receives ACK
4. Mule app returns success response

**Result:** Payment processed successfully

### Scenario 2: Invalid Message ❌

1. Send MT103 missing required field
2. Mock server validates and sends NACK
3. Connector receives NACK
4. Mule app handles error

**Test:** Remove `:59:` (beneficiary) field

### Scenario 3: Sanctions Screening

1. Send payment
2. App screens transaction (simulate)
3. If passed: sends to SWIFT
4. If failed: blocks payment

**Test:** Use Postman request with screening

### Scenario 4: Network Retry

1. Stop mock server
2. Send payment
3. Observe retry behavior
4. Restart mock server
5. Payment succeeds on retry

## Verification Checklist

### ✅ Mock Server
- [ ] Server starts on port 10103
- [ ] Accepts connections
- [ ] Parses MT103 messages
- [ ] Sends ACK responses
- [ ] Logs transactions

### ✅ SWIFT Connector
- [ ] Connects to mock server
- [ ] Authenticates successfully
- [ ] Sends MT103 messages
- [ ] Receives ACK/NACK
- [ ] Handles errors properly

### ✅ Mule App
- [ ] 8 REST endpoints work
- [ ] Payment flow end-to-end
- [ ] Sanctions screening integration
- [ ] Audit logging
- [ ] Error handling

### ✅ Integration
- [ ] Postman → Mule → Connector → Mock Server
- [ ] ACK/NACK flow complete
- [ ] Transaction logs created
- [ ] Health check passes

## Log Files to Monitor

### Mock Server Logs
```bash
# Server activity
tail -f swift-mock-server/swift_mock_server.log

# Transaction records
tail -f swift-mock-server/swift_transactions.log
```

### Mule App Logs
```bash
# Application logs
tail -f mule-app-logs/swift-demo-app.log
```

## Troubleshooting

### Issue: Connection Refused

```
Error: SWIFT:CONNECTION_FAILED
```

**Solution:**
1. Check mock server is running: `ps aux | grep swift_mock_server`
2. Verify port: `lsof -i :10103`
3. Check config: `swift.host=localhost`, `swift.port=10103`

### Issue: No ACK Received

```
Warning: No acknowledgment received
```

**Solution:**
1. Check mock server logs for errors
2. Verify MT103 format
3. Ensure required fields present

### Issue: Port Already in Use

```
Error: [Errno 48] Address already in use
```

**Solution:**
```bash
# Find process
lsof -i :10103

# Kill process
kill -9 <PID>

# Or use different port
python3 swift_mock_server.py --port 10200
```

## Advanced Testing

### Test Client Script

```bash
# Direct test without Mule
cd swift-mock-server
python3 test_client.py
```

### Custom MT103 Messages

Create custom test messages:
```python
mt103 = """{1:...}{2:...}{4:
:20:YOUR-REFERENCE
:32A:240107USD5000,00
:50K:Your Customer
:59:Your Beneficiary
-}"""
```

### Simulate Failures

Edit `swift_mock_server.py`:
```python
def _validate(self) -> bool:
    # Randomly fail 20% of messages
    import random
    if random.random() < 0.2:
        return False
    return True
```

## Demo Script for Customers

### 1. Introduction (2 min)
"I'll show you a complete SWIFT integration using MuleSoft, without needing expensive SWIFT infrastructure."

### 2. Start Mock Server (1 min)
```bash
python3 swift_mock_server.py
```
"This simulates your bank's back-office SWIFT system."

### 3. Show Architecture (2 min)
"REST API → Mule App → SWIFT Connector → Mock Bank Server"

### 4. Send Payment (3 min)
- Open Postman
- Show request body
- Send payment
- Show mock server logs in real-time
- Show ACK response

### 5. Demonstrate Resilience (2 min)
- Stop mock server
- Send payment (fails)
- Restart mock server
- Show retry succeeds

### 6. Show Transaction Logs (1 min)
```bash
cat swift_transactions.log | jq
```

**Total Demo Time: ~11 minutes**

## Why This Matters for Customers

### ✅ Benefits

1. **No Expensive Test Environment**
   - Start development immediately
   - No SWIFT Alliance Access needed
   - No test licenses required

2. **Prove Integration Works**
   - End-to-end demonstration
   - Real message parsing
   - Actual ACK/NACK handling

3. **Show MuleSoft Value**
   - Resilience (retry on failure)
   - Error handling
   - Audit trails
   - Monitoring

4. **Risk-Free Development**
   - Test without production impact
   - Iterate quickly
   - Debug easily

5. **Customer Confidence**
   - See it working live
   - Understand the flow
   - Trust the solution

## Next Steps

1. ✅ **Run This Demo** - Follow steps above
2. ✅ **Customize** - Add your business logic
3. ✅ **Extend** - Add more message types
4. ✅ **Deploy** - Move to production SWIFT

## Production Transition

When ready for production:

1. **Replace Mock Server** with SWIFT Alliance Access (SAA)
2. **Enable TLS** - `swift.enable.tls=true`
3. **Add Certificates** - Configure keystores
4. **Enable Sequence Sync** - ObjectStore integration
5. **Add HSM** - Hardware security module
6. **Configure Screening** - Real sanctions provider

**The Mule app and connector stay the same!**

---

**🎉 Complete End-to-End SWIFT Integration Demo**  
*No expensive infrastructure needed!*


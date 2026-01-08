# SWIFT Mock Server

A Python-based mock server that simulates a bank's back-office SWIFT message processing system.

## Features

✅ **TCP Socket Listener** - Listens on port 10103 for SWIFT messages  
✅ **MT103 Parsing** - Uses RegEx to extract SWIFT fields  
✅ **ACK/NACK Responses** - Sends proper acknowledgments  
✅ **Session Management** - Tracks multiple client connections  
✅ **Transaction Logging** - Logs all messages to file  
✅ **Validation** - Checks required MT103 fields  

## Quick Start

### 1. Install Python 3.8+

```bash
python3 --version  # Verify Python 3.8+
```

### 2. Run Mock Server

```bash
cd swift-mock-server
python3 swift_mock_server.py
```

**Output:**
```
╔═══════════════════════════════════════════════════════════╗
║         SWIFT MT103 Mock Server v1.0.0                    ║
║         Simulating Bank Back-Office System                ║
╚═══════════════════════════════════════════════════════════╝

🚀 SWIFT Mock Server started on 0.0.0.0:10103
📡 Ready to receive MT103 messages...
💡 Press Ctrl+C to stop
```

### 3. Update Mule App Configuration

Edit `swift-demo-app/src/main/resources/config.properties`:

```properties
# Connect to mock server
swift.host=localhost
swift.port=10103
swift.bic=TESTUS33XXX
swift.username=testuser
swift.password=testpass
swift.enable.tls=false
```

### 4. Test with Postman

Send payment via POST `/api/payments`

**Mock server will log:**
```
✅ New connection from ('127.0.0.1', 54321)
📋 Session created: SESSION-1
✉️  Sent authentication response for SESSION-1
📨 Received message MSG-000001 in session SESSION-1
🔍 Parsed MT103 message:
   Reference: PAYMENT-REF-12345
   Value/Amount: 240107USD10000.00
   Ordering Customer: John Doe
   Beneficiary: Jane Smith
✅ Sent ACK for message MSG-000001
```

## Complete End-to-End Demo

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Postman    │───▶│  Demo Mule   │───▶│    SWIFT     │───▶│ Mock SWIFT   │
│  REST Client │    │      App     │    │  Connector   │    │    Server    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                                                                     │
                                                                     ▼
                                                              [ACK Response]
                                                                     │
                    ┌────────────────────────────────────────────────┘
                    ▼
              ✅ Payment Successful
```

## See Full Documentation

For complete documentation, usage examples, and advanced features:

📖 **[Swift Mock Server Complete Documentation](SWIFT_MOCK_SERVER_DOCS.md)**

## Quick Commands

```bash
# Start on default port
python3 swift_mock_server.py

# Custom port
python3 swift_mock_server.py --port 10200

# Debug mode
python3 swift_mock_server.py --debug

# Specific host
python3 swift_mock_server.py --host 127.0.0.1 --port 10103
```

## Log Files

- `swift_mock_server.log` - Server activity
- `swift_transactions.log` - Transaction records (JSON)

## Example MT103 Message

```
{1:F01TESTUS33AXXX0000000000}
{2:O1031234240107TESTDE33XXXX00000000002401071234N}
{4:
:20:PAYMENT-REF-12345
:32A:240107USD10000,00
:50K:John Doe, 123 Main St
:59:Jane Smith, 456 High St
-}
```

## Why This Mock Server?

### For Customers
- ✅ **No Expensive Test Environment** - Start development immediately
- ✅ **Demonstrate MuleSoft Resilience** - Show retry on NACK
- ✅ **Prove Integration** - End-to-end working system
- ✅ **Risk-Free Testing** - No real SWIFT network needed

### For Development
- ✅ **Instant Feedback** - See messages in real-time
- ✅ **Controlled Testing** - Simulate ACK/NACK scenarios
- ✅ **Transaction Logging** - Audit trail for debugging
- ✅ **Easy Setup** - Single Python script

## Next Steps

1. ✅ **Start Mock Server** - Run this script
2. ✅ **Update Mule Config** - Point to localhost:10103
3. ✅ **Deploy Demo App** - Start Mule application
4. ✅ **Test with Postman** - Send payments
5. ✅ **Monitor Logs** - Watch real-time processing

---

**Built for MuleSoft SWIFT Connector Demo**  
*Simulates bank back-office SWIFT processing*


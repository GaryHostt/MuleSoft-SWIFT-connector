# ✅ SWIFT Mock Server v3 - Running

## Server Status: ONLINE ✅

```
================================================================================
✅ SWIFT Mock Server v3 - Production-Grade
================================================================================
SWIFT Protocol: localhost:10103
Control API: http://localhost:8888
================================================================================
```

---

## Connection Endpoints

### **1. SWIFT Protocol Port (TCP)**
- **Host**: `localhost`
- **Port**: `10103`
- **Protocol**: TCP/Socket
- **Status**: ✅ **LISTENING**
- **Usage**: MuleSoft SWIFT Connector connects here

### **2. Control API (REST)**
- **URL**: `http://localhost:8888`
- **Status**: ✅ **RUNNING**
- **Usage**: Error injection, simulation control

---

## Server Capabilities

### **Production-Grade Features**
✅ Stateful session management  
✅ Sequence number tracking  
✅ ACK/NACK simulation  
✅ Real MAC (HMAC-SHA256) validation  
✅ Checksum (SHA-256) verification  
✅ Persistent state to `/tmp/swift_mock_state.json`  
✅ Audit logging (last 1000 messages)  
✅ Login/Logout handshake support  
✅ Heartbeat/IDLE packet handling  

### **Adversarial Testing**
✅ NACK injection via REST API  
✅ Sequence gap simulation  
✅ Socket drop simulation  
✅ Latency injection  
✅ Invalid MAC/Checksum simulation  

---

## Quick Status Check

```bash
# Check server status
curl http://localhost:8888/status

# Response:
{
  "status": "running",
  "sessions": 0,
  "messages": 0,
  "message_count": 0,
  "latency_ms": 0,
  "simulation_mode": null
}
```

---

## Control API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/status` | GET | Server status and statistics |
| `/simulate-error` | POST | Inject NACK/error for next message |
| `/drop-socket` | POST | Simulate dirty disconnect |
| `/set-latency` | POST | Add artificial latency (ms) |
| `/reset` | POST | Reset server state |
| `/messages` | GET | View recent messages |

---

## Test Connection

```bash
# Test SWIFT port (should connect)
nc -zv localhost 10103

# Test Control API
curl http://localhost:8888/status | jq .
```

---

## Next Steps

### **1. Deploy MuleSoft Application**
In Anypoint Studio:
- Right-click `swift-demo-app`
- Run As → **Mule Application**

### **2. Test Payment Flow**
```bash
# Send a test MT103 payment
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "amount": "1000.00",
    "currency": "USD",
    "receiver": "BANKDE33XXX",
    "orderingCustomer": "John Doe",
    "beneficiary": "Jane Smith",
    "transactionId": "TXN001"
  }'
```

### **3. Monitor Server Logs**
```bash
# Watch server activity in real-time
tail -f /Users/alex.macdonald/.cursor/projects/Users-alex-macdonald-SWIFT/terminals/2.txt
```

---

## Server Management

### **Check Status**
```bash
curl http://localhost:8888/status
```

### **Stop Server**
```bash
# Press CTRL+C in the terminal, or:
pkill -f swift_mock_server_v3.py
```

### **Restart Server**
```bash
cd /Users/alex.macdonald/SWIFT/swift-mock-server
python3 swift_mock_server_v3.py
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MuleSoft App                         │
│                  (swift-demo-app)                       │
│               Port 8081 (HTTP API)                      │
└─────────────────┬───────────────────────────────────────┘
                  │
                  │ SWIFT FIN Protocol (TCP)
                  │
┌─────────────────▼───────────────────────────────────────┐
│          SWIFT Mock Server v3                           │
│                                                          │
│  ┌──────────────────┐     ┌──────────────────┐         │
│  │  SWIFT Protocol  │     │   Control API    │         │
│  │   Port: 10103    │     │   Port: 8888     │         │
│  │   (TCP/Socket)   │     │   (HTTP/REST)    │         │
│  └──────────────────┘     └──────────────────┘         │
│                                                          │
│  ┌────────────────────────────────────────────┐        │
│  │  State: /tmp/swift_mock_state.json         │        │
│  │  - Sequence Numbers                        │        │
│  │  - Session State                           │        │
│  │  - Audit Trail                             │        │
│  └────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

---

**Status**: ✅ **SERVER RUNNING**  
**Date**: January 8, 2026  
**Terminal**: `/Users/alex.macdonald/.cursor/projects/Users-alex-macdonald-SWIFT/terminals/2.txt`  
**Next Step**: Deploy `swift-demo-app` in Anypoint Studio


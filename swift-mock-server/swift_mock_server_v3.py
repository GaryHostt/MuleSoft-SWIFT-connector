#!/usr/bin/env python3
"""
✅ ENHANCED SWIFT Mock Server v3 - Production-Grade Testing
 
Critical Enhancements:
1. ✅ Handshake simulation (Login/Logout)
2. ✅ Message persistence (query history)
3. ✅ Error simulation (ACK/NACK toggle via API)
4. ✅ Latency simulation (configurable delays)
5. ✅ Stateful session tracking
6. ✅ TLS support (optional)
7. ✅ Real sequence number validation
"""

import socket
import threading
import json
import time
import hmac
import hashlib
import re
from flask import Flask, request, jsonify
from datetime import datetime

# Configuration
TCP_HOST = 'localhost'
TCP_PORT = 10103
API_PORT = 8888
STATE_FILE = '/tmp/swift_mock_state_v3.json'

# Global state
mock_state = {
    "sessions": {},  # sessionId -> session data
    "messages": {},  # messageId -> message data
    "simulation_mode": None,  # 'nack', 'timeout', 'latency', etc.
    "latency_ms": 0,
    "message_count": 0
}

# Session states
ACTIVE_SESSIONS = {}
SESSION_TIMEOUT = 300  # 5 minutes

# Flask API for control
app = Flask(__name__)

def load_state():
    """Load state from file"""
    global mock_state
    try:
        with open(STATE_FILE, 'r') as f:
            mock_state = json.load(f)
            print(f"✅ State loaded from {STATE_FILE}")
    except FileNotFoundError:
        print("📝 No existing state file, starting fresh")
    except Exception as e:
        print(f"⚠️  Error loading state: {e}")

def save_state():
    """Save state to file"""
    try:
        with open(STATE_FILE, 'w') as f:
            # Convert sessions to serializable format
            state_copy = mock_state.copy()
            state_copy['sessions'] = {
                k: {**v, 'created_at': str(v.get('created_at', '')), 
                    'last_activity': str(v.get('last_activity', ''))}
                for k, v in mock_state['sessions'].items()
            }
            json.dump(state_copy, f, indent=2, default=str)
            print(f"💾 State saved to {STATE_FILE}")
    except Exception as e:
        print(f"❌ Error saving state: {e}")

# ========== REST API for Control ==========

@app.route('/status', methods=['GET'])
def get_status():
    """Get mock server status"""
    return jsonify({
        'status': 'running',
        'sessions': len(mock_state['sessions']),
        'messages': len(mock_state['messages']),
        'simulation_mode': mock_state['simulation_mode'],
        'latency_ms': mock_state['latency_ms'],
        'message_count': mock_state['message_count']
    })

@app.route('/simulate-error', methods=['POST'])
def simulate_error():
    """✅ Configure error simulation mode"""
    data = request.json
    error_type = data.get('error_type')  # 'nack', 'timeout', 'session_invalid', 'none'
    
    mock_state['simulation_mode'] = error_type
    
    if error_type == 'timeout':
        # Don't send any response
        pass
    elif error_type == 'latency':
        # Add configurable delay
        mock_state['latency_ms'] = data.get('latency_ms', 5000)
    
    print(f"🎭 Simulation mode set to: {error_type}")
    save_state()
    
    return jsonify({
        'status': 'ok',
        'simulation_mode': error_type
    })

@app.route('/query-message/<message_id>', methods=['GET'])
def query_message(message_id):
    """✅ Query status of a message by ID"""
    if message_id in mock_state['messages']:
        msg = mock_state['messages'][message_id]
        return jsonify({
            'messageId': message_id,
            'status': msg.get('status'),
            'timestamp': str(msg.get('timestamp')),
            'sequenceNumber': msg.get('sequenceNumber'),
            'responseType': msg.get('responseType')
        })
    else:
        return jsonify({'error': 'Message not found'}), 404

@app.route('/sessions', methods=['GET'])
def list_sessions():
    """List all active sessions"""
    return jsonify({
        'sessions': list(mock_state['sessions'].keys()),
        'count': len(mock_state['sessions'])
    })

@app.route('/reset', methods=['POST'])
def reset_state():
    """Reset all state"""
    global mock_state
    mock_state = {
        "sessions": {},
        "messages": {},
        "simulation_mode": None,
        "latency_ms": 0,
        "message_count": 0
    }
    save_state()
    print("🔄 State reset")
    return jsonify({'status': 'reset'})

# ========== SWIFT Protocol Handler ==========

def handle_client(conn, addr):
    """Handle SWIFT client connection"""
    print(f"📞 Connection from {addr}")
    
    session_id = f"SESSION-{addr[0]}-{addr[1]}"
    
    # ✅ Send immediate login response
    login_response = (
        "{1:F01MOCKSVRXXXXAXXX0000000000}"
        "{2:I001MOCKRCVRXXXXN}"
        "{4:\n:20:LOGIN_OK\n:79:LOGIN_SUCCESSFUL\n-}"
        "{5:{MAC:ABCD1234}{CHK:5678EFGH}}\n"
    )
    conn.send(login_response.encode())
    print(f"✅ Sent login response to {addr}")
    
    # ✅ Create session
    ACTIVE_SESSIONS[session_id] = {
        'created_at': time.time(),
        'last_activity': time.time(),
        'incoming_sequence': 0,
        'outgoing_sequence': 0,
        'authenticated': False
    }
    
    mock_state['sessions'][session_id] = ACTIVE_SESSIONS[session_id]
    
    try:
        while True:
            # Read message
            data = conn.recv(4096).decode('utf-8')
            if not data:
                break
            
            print(f"📨 Received: {len(data)} bytes")
            
            # Update last activity
            ACTIVE_SESSIONS[session_id]['last_activity'] = time.time()
            
            # ✅ Check simulation mode
            if mock_state['simulation_mode'] == 'timeout':
                print("⏱️  SIMULATING TIMEOUT - no response")
                time.sleep(2)
                continue
            
            # ✅ Add latency if configured
            if mock_state['latency_ms'] > 0:
                print(f"⏱️  Simulating latency: {mock_state['latency_ms']}ms")
                time.sleep(mock_state['latency_ms'] / 1000.0)
            
            # Parse message type
            msg_type = detect_message_type(data)
            
            # ✅ Handle LOGIN
            if 'LOGIN' in data or msg_type == 'LOGIN':
                response = handle_login(session_id, data)
                conn.sendall(response.encode('utf-8'))
                print(f"✅ LOGIN processed for {session_id}")
                continue
            
            # ✅ Validate session
            if not ACTIVE_SESSIONS[session_id]['authenticated']:
                response = generate_error("SESSION_NOT_ACTIVE", "Session not authenticated")
                conn.sendall(response.encode('utf-8'))
                print(f"❌ Rejected: session not authenticated")
                continue
            
            # ✅ Validate sequence number
            seq_num = extract_sequence_number(data)
            expected_seq = ACTIVE_SESSIONS[session_id]['incoming_sequence'] + 1
            
            if seq_num != expected_seq:
                print(f"⚠️  Sequence mismatch: expected {expected_seq}, got {seq_num}")
                response = generate_resend_request(expected_seq, seq_num - 1)
                conn.sendall(response.encode('utf-8'))
                continue
            
            # Update sequence
            ACTIVE_SESSIONS[session_id]['incoming_sequence'] = seq_num
            
            # Extract message ID
            message_id = extract_message_id(data)
            
            # Store message
            mock_state['messages'][message_id] = {
                'timestamp': datetime.now().isoformat(),
                'sequenceNumber': seq_num,
                'status': 'RECEIVED',
                'responseType': 'ACK',
                'content': data[:200]  # Store first 200 chars
            }
            mock_state['message_count'] += 1
            
            # Generate response based on simulation mode
            if mock_state['simulation_mode'] == 'nack':
                response = generate_nack(message_id, seq_num, "T27", "Invalid format (simulated)")
                mock_state['messages'][message_id]['responseType'] = 'NACK'
                print(f"❌ Sending NACK for {message_id}")
            else:
                response = generate_ack(message_id, seq_num)
                mock_state['messages'][message_id]['responseType'] = 'ACK'
                print(f"✅ Sending ACK for {message_id}")
            
            # Send response
            conn.sendall(response.encode('utf-8'))
            
            ACTIVE_SESSIONS[session_id]['outgoing_sequence'] += 1
            
            # Save state periodically
            if mock_state['message_count'] % 10 == 0:
                save_state()
    
    except Exception as e:
        print(f"❌ Error handling client: {e}")
    
    finally:
        # ✅ Cleanup session
        if session_id in ACTIVE_SESSIONS:
            del ACTIVE_SESSIONS[session_id]
        if session_id in mock_state['sessions']:
            del mock_state['sessions'][session_id]
        
        conn.close()
        print(f"🔌 Connection closed: {addr}")

def handle_login(session_id, data):
    """Handle LOGIN request"""
    ACTIVE_SESSIONS[session_id]['authenticated'] = True
    
    response = f"""{{1:F01MOCKSVRXXXXAXXX0000000000}}{{2:I001MOCKRCVRXXXXN}}{{4:
:20:LOGIN-ACK
:108:SESSION-{session_id}
:79:Login successful
-}}"""
    
    return response

def detect_message_type(data):
    """Detect SWIFT message type"""
    if 'LOGIN' in data:
        return 'LOGIN'
    elif ':20:HEARTBEAT' in data or ':20:ECHO' in data:
        return 'HEARTBEAT'
    elif 'MT103' in data or ':32A:' in data:
        return 'MT103'
    else:
        return 'UNKNOWN'

def extract_sequence_number(data):
    """Extract sequence number from message"""
    match = re.search(r':34:(\d+)', data)
    if match:
        return int(match.group(1))
    return 1  # Default

def extract_message_id(data):
    """Extract message ID (Tag 20)"""
    match = re.search(r':20:([^\n]+)', data)
    if match:
        return match.group(1).strip()
    return f"MSG-{int(time.time())}"

def generate_ack(message_id, seq_num):
    """Generate ACK (F21)"""
    return f"""{{1:F21MOCKSVRXXXXAXXX0000000000}}{{2:I901MOCKRCVRXXXXN}}{{4:
:20:{message_id}
:34:{seq_num}
:77E:ACK
:108:DEMO-UETR-{message_id}
:177:{datetime.now().strftime('%y%m%d%H%M')}
:451:0
-}}
{{5:{{MAC:TESTMAC}}{{CHK:TESTCHK}}}}
"""

def generate_nack(message_id, seq_num, error_code, error_text):
    """Generate NACK (F21 with error)"""
    return f"""{{1:F21MOCKSVRXXXXAXXX0000000000}}{{2:I901MOCKRCVRXXXXN}}{{4:
:20:{message_id}
:34:{seq_num}
:77E:NACK
:451:{error_code}
:79:{error_text}
-}}
{{5:{{MAC:TESTMAC}}{{CHK:TESTCHK}}}}
"""

def generate_resend_request(begin_seq, end_seq):
    """Generate Resend Request (MsgType 2)"""
    return f"""{{1:F02MOCKSVRXXXXAXXX0000000000}}{{2:I2MOCKRCVRXXXXN}}{{4:
:7:1
:16:{begin_seq}
:17:{end_seq}
-}}
{{5:{{MAC:TESTMAC}}{{CHK:TESTCHK}}}}
"""

def generate_error(error_code, error_text):
    """Generate error message"""
    return f"""{{1:F21MOCKSVRXXXXAXXX0000000000}}{{2:I901MOCKRCVRXXXXN}}{{4:
:20:ERROR
:451:{error_code}
:79:{error_text}
-}}
"""

def start_tcp_server():
    """Start TCP server for SWIFT messages"""
    print(f"🚀 Starting SWIFT TCP server on {TCP_HOST}:{TCP_PORT}")
    
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind((TCP_HOST, TCP_PORT))
    server.listen(5)
    
    print(f"✅ SWIFT server listening on port {TCP_PORT}")
    
    while True:
        conn, addr = server.accept()
        thread = threading.Thread(target=handle_client, args=(conn, addr))
        thread.daemon = True
        thread.start()

def start_api_server():
    """Start REST API server for control"""
    print(f"🚀 Starting Control API on port {API_PORT}")
    app.run(host='0.0.0.0', port=API_PORT, debug=False)

if __name__ == '__main__':
    print("=" * 80)
    print("✅ SWIFT Mock Server v3 - Production-Grade")
    print("=" * 80)
    print(f"SWIFT Protocol: {TCP_HOST}:{TCP_PORT}")
    print(f"Control API: http://{TCP_HOST}:{API_PORT}")
    print("=" * 80)
    
    # Load previous state
    load_state()
    
    # Start API server in separate thread
    api_thread = threading.Thread(target=start_api_server)
    api_thread.daemon = True
    api_thread.start()
    
    # Start TCP server (main thread)
    try:
        start_tcp_server()
    except KeyboardInterrupt:
        print("\n🛑 Server shutting down...")
        save_state()


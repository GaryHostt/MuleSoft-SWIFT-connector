#!/usr/bin/env python3
"""
SWIFT Production-Grade Adversarial Mock Server
Level 2: Stateful, NACK-capable, MAC-validating test environment

Features:
- ACK/NACK simulation (configurable error injection)
- Sequence number persistence (survive restarts)
- MAC/Checksum validation (detect tampered messages)
- Gap detection and Resend Requests
- REST API for adversarial testing control
- Multi-client session management
"""

import socket
import threading
import re
import datetime
import time
import json
import hashlib
import os
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Dict, Optional, List

# Configuration
HOST = '127.0.0.1'
PORT = 10103
CONTROL_API_PORT = 8888
STATE_FILE = '/tmp/swift_mock_state.json'

# Global state
class MockServerState:
    def __init__(self):
        self.sessions: Dict[str, Dict] = {}
        self.error_mode: Optional[str] = None  # 'nack_next', 'drop_connection', 'ignore_sequence', 'invalid_mac'
        self.ignored_sequences: List[int] = []
        self.message_log: List[Dict] = []
        self.lock = threading.Lock()
        self.load_state()
    
    def load_state(self):
        """Load persisted state from file"""
        if os.path.exists(STATE_FILE):
            try:
                with open(STATE_FILE, 'r') as f:
                    data = json.load(f)
                    self.sessions = data.get('sessions', {})
                    self.message_log = data.get('message_log', [])
                    print(f"✓ Loaded state: {len(self.sessions)} sessions, {len(self.message_log)} messages")
            except Exception as e:
                print(f"⚠ Failed to load state: {e}")
    
    def save_state(self):
        """Persist state to file for crash recovery"""
        try:
            with open(STATE_FILE, 'w') as f:
                json.dump({
                    'sessions': self.sessions,
                    'message_log': self.message_log[-1000:]  # Keep last 1000 messages
                }, f, indent=2, default=str)
        except Exception as e:
            print(f"⚠ Failed to save state: {e}")
    
    def get_session(self, session_id: str) -> Dict:
        """Get or create session"""
        with self.lock:
            if session_id not in self.sessions:
                self.sessions[session_id] = {
                    'session_id': session_id,
                    'input_seq': 0,
                    'output_seq': 0,
                    'connected': True,
                    'last_heartbeat': datetime.datetime.now(),
                    'created_at': datetime.datetime.now()
                }
                self.save_state()
            return self.sessions[session_id]
    
    def log_message(self, session_id: str, direction: str, message: str, details: Dict):
        """Log message for audit trail"""
        with self.lock:
            entry = {
                'timestamp': datetime.datetime.now().isoformat(),
                'session_id': session_id,
                'direction': direction,  # 'INBOUND' or 'OUTBOUND'
                'message_preview': message[:200] if message else None,
                'details': details
            }
            self.message_log.append(entry)
            # Keep only last 1000 messages in memory
            if len(self.message_log) > 1000:
                self.message_log = self.message_log[-1000:]
            self.save_state()

state = MockServerState()


def calculate_swift_checksum(message: str) -> str:
    """
    Calculate SWIFT checksum (simplified version for demonstration)
    Real SWIFT uses a complex LAU (Logical Authentication Unit) algorithm
    This is a mock implementation using SHA-256
    """
    # Remove existing trailer for calculation
    message_without_trailer = re.sub(r'\{5:.*?\}\}$', '', message, flags=re.DOTALL)
    checksum = hashlib.sha256(message_without_trailer.encode('utf-8')).hexdigest()[:12]
    return checksum.upper()


def calculate_mac(message: str, key: str = "MOCK_SECRET_KEY") -> str:
    """
    Calculate Message Authentication Code
    Real SWIFT uses bilateral keys and HMAC-SHA256
    This is a simplified implementation for testing
    """
    hmac = hashlib.sha256(f"{message}{key}".encode('utf-8')).hexdigest()[:16]
    return hmac.upper()


def validate_trailer(message: str) -> tuple[bool, str]:
    """
    Validate Block 5 trailer (CHK and MAC)
    Returns (is_valid, reason)
    """
    # Extract Block 5
    block5_match = re.search(r'\{5:\{MAC:([A-F0-9]+)\}\{CHK:([A-F0-9]+)\}\}', message, re.DOTALL)
    
    if not block5_match:
        return False, "Missing Block 5 trailer"
    
    provided_mac = block5_match.group(1)
    provided_chk = block5_match.group(2)
    
    # Calculate expected values
    expected_chk = calculate_swift_checksum(message)
    expected_mac = calculate_mac(message)
    
    if provided_chk != expected_chk:
        return False, f"Checksum mismatch: expected {expected_chk}, got {provided_chk}"
    
    if provided_mac != expected_mac:
        return False, f"MAC mismatch: expected {expected_mac}, got {provided_mac}"
    
    return True, "Valid"


def parse_mt103(message: str) -> Dict:
    """Enhanced parser with Block 3 (UETR) and Block 5 (trailer) support"""
    parsed_data = {
        "raw_message": message
    }
    
    # Block 1: Basic Header
    block1_match = re.search(r'\{1:([^\}]+)\}', message)
    if block1_match:
        parsed_data["block1"] = block1_match.group(1)
    
    # Block 2: Application Header
    block2_match = re.search(r'\{2:([^\}]+)\}', message)
    if block2_match:
        parsed_data["block2"] = block2_match.group(1)
    
    # Block 3: User Header (contains UETR for gpi)
    block3_match = re.search(r'\{3:\{108:([^\}]+)\}\}', message)
    if block3_match:
        parsed_data["uetr"] = block3_match.group(1)
    
    # Block 4: Text Block
    block4_match = re.search(r'\{4:(.*?)-\}', message, re.DOTALL)
    if block4_match:
        block4_content = block4_match.group(1)
        
        # Tag 20: Transaction Reference
        ref_match = re.search(r':20:(\S+)', block4_content)
        if ref_match:
            parsed_data["transaction_reference"] = ref_match.group(1)
        
        # Tag 32A: Value Date, Currency, Amount
        value_match = re.search(r':32A:(\d{6})(\w{3})([\d,\.]+)', block4_content)
        if value_match:
            parsed_data["value_date"] = value_match.group(1)
            parsed_data["currency"] = value_match.group(2)
            parsed_data["amount"] = value_match.group(3)
        
        # Tag 34: Sequence Number
        seq_match = re.search(r':34:(\d+)', block4_content)
        if seq_match:
            parsed_data["sequence_number"] = int(seq_match.group(1))
        
        # Tag 50K: Ordering Customer
        ordering_match = re.search(r':50K:(.+?)(?=:|$)', block4_content, re.DOTALL)
        if ordering_match:
            parsed_data["ordering_customer"] = ordering_match.group(1).strip()
        
        # Tag 59: Beneficiary
        beneficiary_match = re.search(r':59:(.+?)(?=:|$)', block4_content, re.DOTALL)
        if beneficiary_match:
            parsed_data["beneficiary_customer"] = beneficiary_match.group(1).strip()
    
    # Block 5: Trailer
    block5_match = re.search(r'\{5:(.+?)\}\}$', message, re.DOTALL)
    if block5_match:
        block5_content = block5_match.group(1)
        mac_match = re.search(r'\{MAC:([A-F0-9]+)\}', block5_content)
        chk_match = re.search(r'\{CHK:([A-F0-9]+)\}', block5_content)
        
        if mac_match:
            parsed_data["mac"] = mac_match.group(1)
        if chk_match:
            parsed_data["checksum"] = chk_match.group(1)
    
    return parsed_data


def generate_ack(message_data: Dict, session: Dict) -> str:
    """Generate ACK (F21 acknowledgment)"""
    now = datetime.datetime.now()
    ack_time = now.strftime("%H%M")
    ack_date = now.strftime("%y%m%d")
    
    uetr = message_data.get('uetr', f"ACK-{now.strftime('%Y%m%d%H%M%S')}")
    transaction_ref = message_data.get('transaction_reference', 'UNKNOWN')
    
    # Increment output sequence
    session['output_seq'] += 1
    
    ack_message = (
        f"{{1:F21MOCKSVRXXXXAXXX0000000000}}"
        f"{{2:I901MOCKRCVRXXXXN}}"
        f"{{4:\n"
        f":20:{transaction_ref}\n"
        f":34:{session['output_seq']}\n"
        f":77E:ACK\n"
        f":108:{uetr}\n"
        f":177:{ack_date}{ack_time}\n"
        f":451:0\n"
        f"-}}\n"
    )
    
    # Calculate and append Block 5
    checksum = calculate_swift_checksum(ack_message)
    mac = calculate_mac(ack_message)
    ack_message += f"{{5:{{MAC:{mac}}}{{CHK:{checksum}}}}}"
    
    return ack_message


def generate_nack(message_data: Dict, session: Dict, error_code: str = "5", reason: str = "VALIDATION_ERROR") -> str:
    """Generate NACK (negative acknowledgment)"""
    now = datetime.datetime.now()
    nack_time = now.strftime("%H%M")
    nack_date = now.strftime("%y%m%d")
    
    transaction_ref = message_data.get('transaction_reference', 'UNKNOWN')
    
    # Increment output sequence
    session['output_seq'] += 1
    
    # NACK uses MsgType 21 with non-zero error code in tag 451
    nack_message = (
        f"{{1:F21MOCKSVRXXXXAXXX0000000000}}"
        f"{{2:I901MOCKRCVRXXXXN}}"
        f"{{4:\n"
        f":20:{transaction_ref}\n"
        f":34:{session['output_seq']}\n"
        f":77E:NACK\n"
        f":177:{nack_date}{nack_time}\n"
        f":451:{error_code}\n"
        f":79:{reason}\n"
        f"-}}\n"
    )
    
    checksum = calculate_swift_checksum(nack_message)
    mac = calculate_mac(nack_message)
    nack_message += f"{{5:{{MAC:{mac}}}{{CHK:{checksum}}}}}"
    
    return nack_message


def generate_resend_request(session: Dict, from_seq: int, to_seq: int) -> str:
    """Generate Resend Request (MsgType 2)"""
    now = datetime.datetime.now()
    
    session['output_seq'] += 1
    
    resend_message = (
        f"{{1:F02MOCKSVRXXXXAXXX0000000000}}"
        f"{{2:I2MOCKRCVRXXXXN}}"
        f"{{4:\n"
        f":34:{session['output_seq']}\n"
        f":7:{from_seq}\n"
        f":16:{to_seq}\n"
        f"-}}\n"
    )
    
    checksum = calculate_swift_checksum(resend_message)
    mac = calculate_mac(resend_message)
    resend_message += f"{{5:{{MAC:{mac}}}{{CHK:{checksum}}}}}"
    
    return resend_message


def check_sequence_gap(session: Dict, received_seq: int) -> Optional[tuple[int, int]]:
    """
    Check for sequence gap
    Returns (from_seq, to_seq) tuple if gap detected, None otherwise
    """
    expected_seq = session['input_seq'] + 1
    
    if received_seq > expected_seq:
        # Gap detected!
        return (expected_seq, received_seq - 1)
    
    return None


def handle_client(conn, addr):
    """Enhanced client handler with adversarial capabilities"""
    session_id = f"SESSION-{addr[0]}-{addr[1]}"
    session = state.get_session(session_id)
    
    print(f"\n{'='*60}")
    print(f"Connected by {addr}")
    print(f"Session ID: {session_id}")
    print(f"Current Input Seq: {session['input_seq']}, Output Seq: {session['output_seq']}")
    print(f"{'='*60}\n")
    
    try:
        while True:
            data = conn.recv(8192)
            if not data:
                print(f"Client {addr} disconnected.")
                break
            
            message = data.decode('utf-8').strip()
            print(f"\n--- Received SWIFT Message from {addr} ---")
            print(message[:500])  # Print first 500 chars
            print("------------------------------------------")
            
            # Parse message
            parsed_data = parse_mt103(message)
            
            # Extract sequence number
            received_seq = parsed_data.get('sequence_number', 0)
            
            print(f"\nParsed Details:")
            print(f"  Transaction Reference: {parsed_data.get('transaction_reference', 'N/A')}")
            print(f"  Sequence Number: {received_seq}")
            print(f"  Currency: {parsed_data.get('currency', 'N/A')}")
            print(f"  Amount: {parsed_data.get('amount', 'N/A')}")
            print(f"  UETR: {parsed_data.get('uetr', 'N/A')}")
            
            # Log message
            state.log_message(session_id, 'INBOUND', message, parsed_data)
            
            # ====== ADVERSARIAL TESTING LOGIC ======
            
            # 1. Check if we should drop connection
            if state.error_mode == 'drop_connection':
                print(f"\n⚠️  ADVERSARIAL MODE: Dropping connection without response")
                state.error_mode = None  # Reset
                break
            
            # 2. Validate MAC/Checksum (if not in ignore mode)
            if parsed_data.get('mac') and parsed_data.get('checksum'):
                is_valid, validation_reason = validate_trailer(message)
                print(f"\n🔒 Trailer Validation: {validation_reason}")
                
                if not is_valid:
                    print(f"❌ INVALID TRAILER - Sending NACK")
                    nack_response = generate_nack(parsed_data, session, "5", validation_reason)
                    conn.sendall(nack_response.encode('utf-8'))
                    print(f"\n--- Sent NACK to {addr} ---")
                    print(nack_response[:300])
                    state.log_message(session_id, 'OUTBOUND', nack_response, {'type': 'NACK', 'reason': validation_reason})
                    continue
            
            # 3. Check sequence gap
            gap = check_sequence_gap(session, received_seq)
            if gap and received_seq not in state.ignored_sequences:
                from_seq, to_seq = gap
                print(f"\n⚠️  SEQUENCE GAP DETECTED: Expected {session['input_seq'] + 1}, got {received_seq}")
                print(f"   Missing sequences: {from_seq} to {to_seq}")
                
                # Send Resend Request
                resend_request = generate_resend_request(session, from_seq, to_seq)
                conn.sendall(resend_request.encode('utf-8'))
                print(f"\n--- Sent RESEND REQUEST to {addr} ---")
                print(resend_request)
                state.log_message(session_id, 'OUTBOUND', resend_request, {
                    'type': 'RESEND_REQUEST',
                    'from_seq': from_seq,
                    'to_seq': to_seq
                })
                
                # Don't update input_seq yet - wait for missing messages
                continue
            
            # 4. Check if this sequence should be ignored (simulate gap)
            if received_seq in state.ignored_sequences:
                print(f"\n⚠️  ADVERSARIAL MODE: Ignoring sequence {received_seq}")
                state.ignored_sequences.remove(received_seq)
                # Don't respond, don't update sequence
                continue
            
            # 5. Check if we should NACK this message
            if state.error_mode == 'nack_next':
                print(f"\n⚠️  ADVERSARIAL MODE: Sending NACK")
                state.error_mode = None  # Reset
                nack_response = generate_nack(parsed_data, session, "7", "ADVERSARIAL_TEST")
                conn.sendall(nack_response.encode('utf-8'))
                print(f"\n--- Sent NACK to {addr} ---")
                print(nack_response[:300])
                state.log_message(session_id, 'OUTBOUND', nack_response, {'type': 'NACK', 'reason': 'ADVERSARIAL_TEST'})
                # Update input sequence even for NACK
                session['input_seq'] = received_seq
                state.save_state()
                continue
            
            # 6. Normal flow - Send ACK
            ack_response = generate_ack(parsed_data, session)
            conn.sendall(ack_response.encode('utf-8'))
            
            print(f"\n--- Sent ACK to {addr} ---")
            print(ack_response[:300])
            
            # Update input sequence
            session['input_seq'] = received_seq
            session['last_heartbeat'] = datetime.datetime.now()
            state.save_state()
            
            state.log_message(session_id, 'OUTBOUND', ack_response, {'type': 'ACK'})
            
            print(f"\n✓ Session updated: Input Seq={session['input_seq']}, Output Seq={session['output_seq']}")
    
    except Exception as e:
        print(f"❌ Error handling client {addr}: {e}")
        import traceback
        traceback.print_exc()
    finally:
        conn.close()
        print(f"\nConnection closed: {addr}")


# ====== CONTROL API FOR ADVERSARIAL TESTING ======

class ControlAPIHandler(BaseHTTPRequestHandler):
    """REST API to control mock server behavior"""
    
    def log_message(self, format, *args):
        """Suppress default logging"""
        pass
    
    def do_GET(self):
        """Handle GET requests"""
        if self.path == '/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            response = {
                'status': 'running',
                'sessions': len(state.sessions),
                'error_mode': state.error_mode,
                'ignored_sequences': state.ignored_sequences,
                'message_count': len(state.message_log),
                'session_details': {
                    sid: {
                        'input_seq': s['input_seq'],
                        'output_seq': s['output_seq'],
                        'connected': s['connected']
                    }
                    for sid, s in state.sessions.items()
                }
            }
            self.wfile.write(json.dumps(response, indent=2).encode('utf-8'))
        
        elif self.path == '/messages':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            # Return last 50 messages
            response = {
                'messages': state.message_log[-50:],
                'total_count': len(state.message_log)
            }
            self.wfile.write(json.dumps(response, indent=2, default=str).encode('utf-8'))
        
        else:
            self.send_response(404)
            self.end_headers()
    
    def do_POST(self):
        """Handle POST requests for error injection"""
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        try:
            data = json.loads(post_data.decode('utf-8'))
        except:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b'Invalid JSON')
            return
        
        if self.path == '/inject-error':
            # Set error mode
            error_type = data.get('error_type')  # 'nack_next', 'drop_connection', 'ignore_sequence'
            
            if error_type == 'ignore_sequence':
                sequences = data.get('sequences', [])
                state.ignored_sequences.extend(sequences)
                response_msg = f"Will ignore sequences: {sequences}"
            elif error_type in ['nack_next', 'drop_connection']:
                state.error_mode = error_type
                response_msg = f"Error mode set to: {error_type}"
            else:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b'Invalid error_type')
                return
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'status': 'ok', 'message': response_msg}).encode('utf-8'))
            print(f"\n⚠️  Control API: {response_msg}")
        
        elif self.path == '/reset':
            # Reset state
            state.sessions.clear()
            state.message_log.clear()
            state.error_mode = None
            state.ignored_sequences.clear()
            state.save_state()
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({'status': 'ok', 'message': 'State reset'}).encode('utf-8'))
            print(f"\n🔄 Control API: State reset")
        
        else:
            self.send_response(404)
            self.end_headers()


def start_control_api():
    """Start REST API server for control"""
    server = HTTPServer(('localhost', CONTROL_API_PORT), ControlAPIHandler)
    print(f"Control API listening on http://localhost:{CONTROL_API_PORT}")
    print(f"  GET  /status          - View current state")
    print(f"  GET  /messages        - View message log")
    print(f"  POST /inject-error    - Inject errors")
    print(f"  POST /reset           - Reset state")
    server.serve_forever()


def start_swift_server():
    """Start SWIFT TCP server"""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        s.bind((HOST, PORT))
        s.listen()
        print(f"SWIFT Mock Server (Production-Grade) listening on {HOST}:{PORT}")
        print(f"State persistence: {STATE_FILE}")
        print(f"\nAdversarial Features:")
        print(f"  ✓ ACK/NACK simulation")
        print(f"  ✓ Sequence gap detection & Resend Requests")
        print(f"  ✓ MAC/Checksum validation")
        print(f"  ✓ State persistence (crash recovery)")
        print(f"  ✓ Control API for error injection")
        print(f"\nWaiting for connections...\n")
        
        while True:
            conn, addr = s.accept()
            client_handler = threading.Thread(target=handle_client, args=(conn, addr), daemon=True)
            client_handler.start()


if __name__ == "__main__":
    print("="*80)
    print("SWIFT PRODUCTION-GRADE ADVERSARIAL MOCK SERVER v2.0")
    print("="*80)
    print()
    
    # Start Control API in separate thread
    api_thread = threading.Thread(target=start_control_api, daemon=True)
    api_thread.start()
    
    time.sleep(1)  # Give API time to start
    
    # Start main SWIFT server
    try:
        start_swift_server()
    except KeyboardInterrupt:
        print("\n\nShutting down...")
        state.save_state()
        print("✓ State saved")


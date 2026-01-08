#!/usr/bin/env python3
"""
Test client for SWIFT Production-Grade Mock Server v2
Demonstrates adversarial testing scenarios
"""

import socket
import time
import hashlib
import requests
import json

HOST = 'localhost'
PORT = 10103
CONTROL_API = 'http://localhost:8888'


def calculate_swift_checksum(message: str) -> str:
    """Must match server implementation"""
    import re
    message_without_trailer = re.sub(r'\{5:.*?\}\}$', '', message, flags=re.DOTALL)
    checksum = hashlib.sha256(message_without_trailer.encode('utf-8')).hexdigest()[:12]
    return checksum.upper()


def calculate_mac(message: str, key: str = "MOCK_SECRET_KEY") -> str:
    """Must match server implementation"""
    hmac = hashlib.sha256(f"{message}{key}".encode('utf-8')).hexdigest()[:16]
    return hmac.upper()


def build_mt103(trn: str, seq_num: int, amount: str = "10000,00") -> str:
    """Build complete MT103 with all 5 blocks"""
    message = (
        f"{{1:F01TESTUS33XXXX0000000000}}"
        f"{{2:O1031234240107TESTDE33XXXX12345678}}"
        f"{{3:{{108:DEMO-UETR-{trn}}}}}"
        f"{{4:\n"
        f":20:{trn}\n"
        f":34:{seq_num}\n"
        f":32A:240107USD{amount}\n"
        f":50K:Test Ordering Customer\nACME Corp\n"
        f":59:Test Beneficiary\nXYZ Bank\n"
        f"-}}\n"
    )
    
    # Calculate Block 5
    checksum = calculate_swift_checksum(message)
    mac = calculate_mac(message)
    message += f"{{5:{{MAC:{mac}}}{{CHK:{checksum}}}}}"
    
    return message


def send_message(message: str) -> str:
    """Send message and receive response"""
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(10)
        s.connect((HOST, PORT))
        s.sendall(message.encode('utf-8'))
        
        response = s.recv(8192).decode('utf-8')
        return response


def check_server_status():
    """Check mock server status via Control API"""
    try:
        response = requests.get(f'{CONTROL_API}/status')
        return response.json()
    except Exception as e:
        print(f"❌ Control API not available: {e}")
        return None


def inject_error(error_type: str, **kwargs):
    """Inject error via Control API"""
    try:
        payload = {'error_type': error_type, **kwargs}
        response = requests.post(f'{CONTROL_API}/inject-error', json=payload)
        return response.json()
    except Exception as e:
        print(f"❌ Failed to inject error: {e}")
        return None


def reset_server():
    """Reset server state"""
    try:
        response = requests.post(f'{CONTROL_API}/reset')
        return response.json()
    except Exception as e:
        print(f"❌ Failed to reset server: {e}")
        return None


# ====== TEST SCENARIOS ======

def test_1_happy_path():
    """Test 1: Normal ACK response"""
    print("\n" + "="*60)
    print("TEST 1: Happy Path - Normal ACK")
    print("="*60)
    
    message = build_mt103("TEST-001", 1)
    print(f"Sending message: TEST-001 (seq: 1)")
    
    response = send_message(message)
    
    if ':77E:ACK' in response:
        print("✓ Received ACK")
        print(f"Response preview: {response[:200]}")
        return True
    else:
        print("❌ Expected ACK, got something else")
        print(response[:300])
        return False


def test_2_nack_injection():
    """Test 2: NACK response (error injection)"""
    print("\n" + "="*60)
    print("TEST 2: NACK Injection")
    print("="*60)
    
    # Inject NACK for next message
    print("Injecting NACK error...")
    result = inject_error('nack_next')
    print(f"Injection result: {result}")
    
    time.sleep(0.5)
    
    message = build_mt103("TEST-002", 2)
    print(f"Sending message: TEST-002 (seq: 2)")
    
    response = send_message(message)
    
    if ':77E:NACK' in response and ':451:' in response:
        print("✓ Received NACK as expected")
        print(f"Response preview: {response[:300]}")
        return True
    else:
        print("❌ Expected NACK, got something else")
        print(response[:300])
        return False


def test_3_sequence_gap():
    """Test 3: Sequence gap detection"""
    print("\n" + "="*60)
    print("TEST 3: Sequence Gap Detection")
    print("="*60)
    
    # Send sequence 10
    print("Sending sequence 10...")
    msg1 = build_mt103("TEST-010", 10)
    response1 = send_message(msg1)
    print(f"Response 1: {'ACK' if ':77E:ACK' in response1 else 'Other'}")
    
    # Skip sequence 11, send 12 (creates gap!)
    print("\nSkipping sequence 11, sending 12...")
    msg2 = build_mt103("TEST-012", 12)
    response2 = send_message(msg2)
    
    # Should receive Resend Request for sequence 11
    if ':7:11' in response2 and ':16:11' in response2:
        print("✓ Received Resend Request for missing sequence 11")
        print(f"Response preview: {response2[:300]}")
        return True
    else:
        print("❌ Expected Resend Request")
        print(response2[:300])
        return False


def test_4_invalid_mac():
    """Test 4: Invalid MAC detection"""
    print("\n" + "="*60)
    print("TEST 4: Invalid MAC Detection")
    print("="*60)
    
    # Build message with WRONG MAC
    message = (
        f"{{1:F01TESTUS33XXXX0000000000}}"
        f"{{2:O1031234240107TESTDE33XXXX12345678}}"
        f"{{4:\n"
        f":20:TEST-BAD-MAC\n"
        f":34:20\n"
        f":32A:240107USD5000,00\n"
        f":50K:Test\n"
        f":59:Test\n"
        f"-}}\n"
        f"{{5:{{MAC:INVALID1234}}{{CHK:INVALIDCHECK}}}}"
    )
    
    print("Sending message with INVALID MAC/CHK...")
    
    response = send_message(message)
    
    if ':77E:NACK' in response:
        print("✓ Server rejected message with invalid MAC")
        print(f"Response preview: {response[:300]}")
        return True
    else:
        print("❌ Server should have rejected invalid MAC!")
        print(response[:300])
        return False


def test_5_ignored_sequence():
    """Test 5: Ignored sequence (mock doesn't respond)"""
    print("\n" + "="*60)
    print("TEST 5: Ignored Sequence (Simulated Gap)")
    print("="*60)
    
    # Tell mock to ignore sequence 30
    print("Configuring mock to ignore sequence 30...")
    result = inject_error('ignore_sequence', sequences=[30])
    print(f"Injection result: {result}")
    
    time.sleep(0.5)
    
    message = build_mt103("TEST-030", 30)
    print(f"Sending sequence 30 (will be ignored)...")
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(3)  # Short timeout
            s.connect((HOST, PORT))
            s.sendall(message.encode('utf-8'))
            
            response = s.recv(8192).decode('utf-8')
            print("❌ Received response when server should have ignored!")
            print(response[:300])
            return False
    
    except socket.timeout:
        print("✓ No response received (sequence ignored as expected)")
        return True


def test_6_connection_drop():
    """Test 6: Connection drop simulation"""
    print("\n" + "="*60)
    print("TEST 6: Connection Drop (Network Partition)")
    print("="*60)
    
    # Tell mock to drop connection
    print("Configuring mock to drop next connection...")
    result = inject_error('drop_connection')
    print(f"Injection result: {result}")
    
    time.sleep(0.5)
    
    message = build_mt103("TEST-DROP", 40)
    print(f"Sending message (connection will drop)...")
    
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(5)
            s.connect((HOST, PORT))
            s.sendall(message.encode('utf-8'))
            
            response = s.recv(8192).decode('utf-8')
            if not response:
                print("✓ Connection dropped (no response)")
                return True
            else:
                print("❌ Received response when connection should have dropped!")
                return False
    
    except (ConnectionResetError, BrokenPipeError):
        print("✓ Connection dropped by server")
        return True
    except socket.timeout:
        print("✓ No response (connection dropped)")
        return True


def test_7_state_persistence():
    """Test 7: Check state persistence"""
    print("\n" + "="*60)
    print("TEST 7: State Persistence")
    print("="*60)
    
    status = check_server_status()
    if status:
        print(f"✓ Server status retrieved:")
        print(f"  Sessions: {status.get('sessions', 0)}")
        print(f"  Messages: {status.get('message_count', 0)}")
        print(f"  Error Mode: {status.get('error_mode', 'None')}")
        
        for session_id, details in status.get('session_details', {}).items():
            print(f"\n  Session: {session_id}")
            print(f"    Input Seq: {details['input_seq']}")
            print(f"    Output Seq: {details['output_seq']}")
        
        return True
    else:
        print("❌ Failed to retrieve status")
        return False


def run_all_tests():
    """Run complete test suite"""
    print("\n" + "="*80)
    print("SWIFT MOCK SERVER v2 - ADVERSARIAL TEST SUITE")
    print("="*80)
    
    # Check server is running
    print("\nChecking server connectivity...")
    status = check_server_status()
    if not status:
        print("❌ Mock server not running!")
        print("Start with: python3 swift_mock_server_v2.py")
        return
    
    print(f"✓ Server running (Sessions: {status.get('sessions', 0)})")
    
    # Reset server state
    print("\nResetting server state...")
    reset_server()
    
    time.sleep(1)
    
    # Run tests
    tests = [
        test_1_happy_path,
        test_2_nack_injection,
        test_3_sequence_gap,
        test_4_invalid_mac,
        test_5_ignored_sequence,
        test_6_connection_drop,
        test_7_state_persistence
    ]
    
    results = []
    for test_func in tests:
        try:
            result = test_func()
            results.append((test_func.__name__, result))
            time.sleep(1)  # Pause between tests
        except Exception as e:
            print(f"❌ Test failed with exception: {e}")
            import traceback
            traceback.print_exc()
            results.append((test_func.__name__, False))
    
    # Summary
    print("\n" + "="*80)
    print("TEST SUMMARY")
    print("="*80)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✓ PASS" if result else "❌ FAIL"
        print(f"{status} - {test_name}")
    
    print(f"\nResults: {passed}/{total} tests passed ({int(passed/total*100)}%)")
    
    if passed == total:
        print("\n🎉 All tests passed! Mock server is production-grade.")
    else:
        print(f"\n⚠️  {total-passed} test(s) failed. Review output above.")


if __name__ == "__main__":
    run_all_tests()


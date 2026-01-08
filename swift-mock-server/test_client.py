#!/usr/bin/env python3
"""
Test Client for SWIFT Mock Server
Sends sample MT103 messages to test the mock server

Usage:
    python test_client.py [--host localhost] [--port 10103]
"""

import socket
import argparse
import time

def send_mt103(host: str, port: int):
    """Send sample MT103 message"""
    
    # Sample MT103 message
    mt103 = """{1:F01TESTUS33AXXX0000000000}{2:O1031234240107TESTDE33XXXX00000000002401071234N}{4:
:20:TEST-REF-123
:32A:240107USD10000,00
:50K:John Doe
123 Main Street
New York, NY 10001
:59:Jane Smith
456 High Street
Berlin, Germany
-}"""
    
    print(f"🔌 Connecting to SWIFT Mock Server at {host}:{port}...")
    
    try:
        # Create socket connection
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((host, port))
        print("✅ Connected successfully!")
        
        # Receive authentication response
        auth = sock.recv(1024).decode('utf-8')
        print(f"🔐 Auth response: {auth.strip()}")
        
        # Send MT103 message
        print("\n📤 Sending MT103 message...")
        print(f"{mt103}")
        sock.send(mt103.encode('utf-8'))
        
        # Wait a bit for processing
        time.sleep(0.5)
        
        # Receive ACK/NACK
        response = sock.recv(4096).decode('utf-8')
        print("\n📥 Received response:")
        print(response)
        
        if ':77E:ACK' in response:
            print("\n✅ Payment ACCEPTED - ACK received")
        elif ':77E:NACK' in response:
            print("\n❌ Payment REJECTED - NACK received")
        else:
            print("\n⚠️  Unknown response format")
        
        # Close connection
        sock.close()
        print("\n👋 Connection closed")
        
    except ConnectionRefusedError:
        print(f"❌ Connection refused. Is the mock server running on {host}:{port}?")
        print("   Start it with: python3 swift_mock_server.py")
    except Exception as e:
        print(f"❌ Error: {e}")

def main():
    parser = argparse.ArgumentParser(description='Test client for SWIFT Mock Server')
    parser.add_argument('--host', default='localhost', help='Server host')
    parser.add_argument('--port', type=int, default=10103, help='Server port')
    
    args = parser.parse_args()
    
    print("""
╔═══════════════════════════════════════════════════════════╗
║         SWIFT Mock Server Test Client                    ║
╚═══════════════════════════════════════════════════════════╝
    """)
    
    send_mt103(args.host, args.port)

if __name__ == '__main__':
    main()


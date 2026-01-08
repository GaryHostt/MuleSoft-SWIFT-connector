#!/bin/bash

##############################################
# Quick Test Script - SWIFT Mock Server Only
##############################################

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

MOCK_SERVER_DIR="/Users/alex.macdonald/SWIFT/swift-mock-server"
MOCK_SERVER_PORT=10103

echo "================================================"
echo "Testing SWIFT Mock Server"
echo "================================================"
echo ""

# Check if server is running
echo -e "${YELLOW}Checking if mock server is running on port $MOCK_SERVER_PORT...${NC}"
if lsof -Pi :$MOCK_SERVER_PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo -e "${GREEN}✓ Mock server is already running${NC}"
    SERVER_PID=$(lsof -t -i:$MOCK_SERVER_PORT)
    echo "  PID: $SERVER_PID"
else
    echo -e "${YELLOW}Starting mock server...${NC}"
    cd "$MOCK_SERVER_DIR"
    python3 swift_mock_server.py > /tmp/swift-mock-server.log 2>&1 &
    SERVER_PID=$!
    echo $SERVER_PID > /tmp/swift-mock-server.pid
    
    sleep 3
    
    if ps -p $SERVER_PID > /dev/null; then
        echo -e "${GREEN}✓ Mock server started (PID: $SERVER_PID)${NC}"
    else
        echo -e "${RED}✗ Failed to start mock server${NC}"
        exit 1
    fi
fi
echo ""

# Send test message
echo -e "${YELLOW}Sending test MT103 message...${NC}"

MT103_MESSAGE="{1:F01TESTUS33XXXX0000000000}{2:O1031234240107TESTDE33XXXX12345678}{4:
:20:TEST-QUICK-001
:32A:240107USD25000,00
:50K:Quick Test Customer
Test Corp
:59:Quick Test Beneficiary
Test Bank
-}"

# Use Python to send and receive
python3 -c "
import socket
import time

HOST = 'localhost'
PORT = $MOCK_SERVER_PORT

try:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((HOST, PORT))
        s.settimeout(5)
        
        message = '''$MT103_MESSAGE'''
        s.sendall(message.encode('utf-8'))
        print('✓ Message sent')
        
        time.sleep(1)
        
        response = s.recv(4096).decode('utf-8')
        if response:
            print('✓ Received ACK response')
            print('\nResponse preview:')
            print(response[:200])
        else:
            print('⚠ No response received')
            
except Exception as e:
    print(f'✗ Error: {e}')
"

echo ""
echo -e "${GREEN}Test Complete!${NC}"
echo ""
echo "View logs: tail -f /tmp/swift-mock-server.log"
echo "Stop server: kill \$(cat /tmp/swift-mock-server.pid)"
echo ""


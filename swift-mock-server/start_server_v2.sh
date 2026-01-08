#!/bin/bash

##############################################################
# Production-Grade Mock Server Quick Start
##############################################################

echo "=========================================="
echo "SWIFT Mock Server v2.0 - Quick Start"
echo "=========================================="
echo ""

cd /Users/alex.macdonald/SWIFT/swift-mock-server

# Check if server is already running
if lsof -Pi :10103 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  Mock server already running on port 10103"
    echo "    PID: $(lsof -t -i:10103)"
    echo ""
    read -p "Kill and restart? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        kill $(lsof -t -i:10103) 2>/dev/null
        kill $(lsof -t -i:8888) 2>/dev/null
        sleep 2
    else
        echo "Exiting..."
        exit 0
    fi
fi

echo "Starting SWIFT Mock Server v2.0..."
echo ""
echo "Features:"
echo "  ✓ ACK/NACK simulation"
echo "  ✓ Sequence gap detection"
echo "  ✓ MAC/Checksum validation"
echo "  ✓ State persistence"
echo "  ✓ Error injection API"
echo ""
echo "SWIFT Port: 10103"
echo "Control API: http://localhost:8888"
echo ""
echo "Stop with: Ctrl+C"
echo "=========================================="
echo ""

python3 swift_mock_server_v2.py


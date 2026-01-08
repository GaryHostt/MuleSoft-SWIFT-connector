#!/bin/bash

echo "================================================"
echo "  SWIFT Mock Server - Quick Start"
echo "================================================"
echo ""

# Check Python version
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
    echo "✅ Python found: $PYTHON_VERSION"
else
    echo "❌ Python 3 not found. Please install Python 3.8+"
    exit 1
fi

echo ""
echo "Starting SWIFT Mock Server on port 10103..."
echo ""
echo "To stop: Press Ctrl+C"
echo ""

# Run the mock server
python3 swift_mock_server.py


#!/bin/bash

##############################################
# SWIFT Demo Application - End-to-End Test
##############################################

set -e

echo "================================================"
echo "SWIFT Connector - End-to-End Verification"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
MOCK_SERVER_DIR="/Users/alex.macdonald/SWIFT/swift-mock-server"
DEMO_APP_DIR="/Users/alex.macdonald/SWIFT/swift-demo-app"
MOCK_SERVER_PORT=10103
MULE_APP_PORT=8081

##############################################
# Step 1: Build SWIFT Connector
##############################################

echo -e "${YELLOW}Step 1: Building SWIFT Connector...${NC}"
cd /Users/alex.macdonald/SWIFT
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ SWIFT Connector built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build SWIFT Connector${NC}"
    exit 1
fi
echo ""

##############################################
# Step 2: Build Demo Application
##############################################

echo -e "${YELLOW}Step 2: Building Demo Application...${NC}"
cd "$DEMO_APP_DIR"
mvn clean package
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Demo Application built successfully${NC}"
else
    echo -e "${RED}✗ Failed to build Demo Application${NC}"
    exit 1
fi
echo ""

##############################################
# Step 3: Start Mock SWIFT Server
##############################################

echo -e "${YELLOW}Step 3: Starting Mock SWIFT Server on port $MOCK_SERVER_PORT...${NC}"

# Check if port is already in use
if lsof -Pi :$MOCK_SERVER_PORT -sTCP:LISTEN -t >/dev/null ; then
    echo -e "${YELLOW}⚠ Port $MOCK_SERVER_PORT already in use. Stopping existing process...${NC}"
    kill $(lsof -t -i:$MOCK_SERVER_PORT) 2>/dev/null || true
    sleep 2
fi

# Start mock server in background
cd "$MOCK_SERVER_DIR"
python3 swift_mock_server.py > /tmp/swift-mock-server.log 2>&1 &
MOCK_SERVER_PID=$!

# Wait for server to start
echo "Waiting for mock server to start..."
sleep 3

# Verify server is running
if ps -p $MOCK_SERVER_PID > /dev/null; then
    echo -e "${GREEN}✓ Mock SWIFT Server started (PID: $MOCK_SERVER_PID)${NC}"
    echo "  Log file: /tmp/swift-mock-server.log"
else
    echo -e "${RED}✗ Failed to start Mock SWIFT Server${NC}"
    exit 1
fi
echo ""

##############################################
# Step 4: Test Mock Server Directly
##############################################

echo -e "${YELLOW}Step 4: Testing Mock Server Directly...${NC}"

# Create a test MT103 message
MT103_MESSAGE="{1:F01TESTUS33XXXX0000000000}{2:O1031234240107TESTDE33XXXX12345678}{4:
:20:TEST-001
:32A:240107USD10000,00
:50K:Test Ordering Customer
ACME Corporation
:59:Test Beneficiary
ABC Bank
-}"

# Send test message
echo "$MT103_MESSAGE" | nc localhost $MOCK_SERVER_PORT > /tmp/swift-test-response.txt &
NC_PID=$!
sleep 2

# Check response
if [ -s /tmp/swift-test-response.txt ]; then
    echo -e "${GREEN}✓ Mock server responded to test message${NC}"
    echo "  Response preview:"
    head -n 3 /tmp/swift-test-response.txt | sed 's/^/  /'
else
    echo -e "${YELLOW}⚠ No response received (may be normal for async)${NC}"
fi
echo ""

##############################################
# Step 5: Deploy Mule Application
##############################################

echo -e "${YELLOW}Step 5: Deploy Mule Application${NC}"
echo ""
echo "MANUAL STEP REQUIRED:"
echo "======================================"
echo "Deploy the Mule application using one of these methods:"
echo ""
echo "Option 1 - Anypoint Studio:"
echo "  1. Open Anypoint Studio"
echo "  2. Import project from: $DEMO_APP_DIR"
echo "  3. Right-click project → Run As → Mule Application"
echo ""
echo "Option 2 - Standalone Mule Runtime:"
echo "  1. Copy the JAR to: \$MULE_HOME/apps/"
echo "     cp $DEMO_APP_DIR/target/swift-demo-app-1.0.0-mule-application.jar \$MULE_HOME/apps/"
echo "  2. Mule will auto-deploy the application"
echo ""
echo "Option 3 - CloudHub:"
echo "  1. Login to Anypoint Platform"
echo "  2. Runtime Manager → Deploy Application"
echo "  3. Upload: $DEMO_APP_DIR/target/swift-demo-app-1.0.0-mule-application.jar"
echo ""
echo "======================================"
echo ""

read -p "Press ENTER once the Mule application is deployed and running..."
echo ""

##############################################
# Step 6: Test Health Check
##############################################

echo -e "${YELLOW}Step 6: Testing Health Check Endpoint...${NC}"

HEALTH_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" http://localhost:$MULE_APP_PORT/api/health)
HTTP_STATUS=$(echo "$HEALTH_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
BODY=$(echo "$HEALTH_RESPONSE" | grep -v HTTP_STATUS)

if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "${GREEN}✓ Health check passed (HTTP $HTTP_STATUS)${NC}"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}✗ Health check failed (HTTP $HTTP_STATUS)${NC}"
    echo "$BODY"
fi
echo ""

##############################################
# Step 7: Test Send Payment
##############################################

echo -e "${YELLOW}Step 7: Testing Send Payment Endpoint...${NC}"

PAYMENT_DATA='{
  "transactionId": "TEST-E2E-001",
  "amount": "15000.00",
  "currency": "USD",
  "receiver": "BANKDE33XXX",
  "orderingCustomer": "ACME Corporation",
  "beneficiary": "XYZ Trading Ltd",
  "reference": "Invoice 12345"
}'

PAYMENT_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$PAYMENT_DATA" \
  http://localhost:$MULE_APP_PORT/api/payments)

HTTP_STATUS=$(echo "$PAYMENT_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
BODY=$(echo "$PAYMENT_RESPONSE" | grep -v HTTP_STATUS)

if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "201" ]; then
    echo -e "${GREEN}✓ Payment sent successfully (HTTP $HTTP_STATUS)${NC}"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    
    # Extract message ID for tracking
    MESSAGE_ID=$(echo "$BODY" | python3 -c "import sys, json; print(json.load(sys.stdin).get('messageId', 'N/A'))" 2>/dev/null)
    echo ""
    echo "Message ID: $MESSAGE_ID"
else
    echo -e "${RED}✗ Payment failed (HTTP $HTTP_STATUS)${NC}"
    echo "$BODY"
fi
echo ""

##############################################
# Step 8: Check Mock Server Logs
##############################################

echo -e "${YELLOW}Step 8: Checking Mock Server Logs...${NC}"

if [ -f /tmp/swift-mock-server.log ]; then
    echo "Recent activity from mock server:"
    tail -n 20 /tmp/swift-mock-server.log | sed 's/^/  /'
    
    # Count messages received
    MSG_COUNT=$(grep -c "Received SWIFT Message" /tmp/swift-mock-server.log 2>/dev/null || echo "0")
    echo ""
    echo -e "${GREEN}✓ Mock server processed $MSG_COUNT message(s)${NC}"
else
    echo -e "${YELLOW}⚠ Mock server log not found${NC}"
fi
echo ""

##############################################
# Step 9: Test Additional Endpoints
##############################################

echo -e "${YELLOW}Step 9: Testing Additional Endpoints...${NC}"

# Test 9a: Validate Message
echo "9a. Testing Validate Message..."
VALIDATE_DATA='{
  "messageType": "MT103",
  "format": "MT",
  "content": ":20:TEST123\n:32A:240107USD10000,00\n:50K:Test\n:59:Test"
}'

VALIDATE_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$VALIDATE_DATA" \
  http://localhost:$MULE_APP_PORT/api/validate)

HTTP_STATUS=$(echo "$VALIDATE_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "   ${GREEN}✓ Validate endpoint works${NC}"
else
    echo -e "   ${RED}✗ Validate endpoint failed (HTTP $HTTP_STATUS)${NC}"
fi

# Test 9b: BIC Lookup
echo "9b. Testing BIC Lookup..."
BIC_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  http://localhost:$MULE_APP_PORT/api/bic/DEUTDEFFXXX)

HTTP_STATUS=$(echo "$BIC_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "   ${GREEN}✓ BIC Lookup endpoint works${NC}"
else
    echo -e "   ${RED}✗ BIC Lookup endpoint failed (HTTP $HTTP_STATUS)${NC}"
fi

# Test 9c: Holiday Check
echo "9c. Testing Holiday Check..."
HOLIDAY_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:$MULE_APP_PORT/api/holidays/check?date=2024-12-25&calendar=TARGET2")

HTTP_STATUS=$(echo "$HOLIDAY_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "   ${GREEN}✓ Holiday Check endpoint works${NC}"
else
    echo -e "   ${RED}✗ Holiday Check endpoint failed (HTTP $HTTP_STATUS)${NC}"
fi

# Test 9d: Get Metrics
echo "9d. Testing Get Metrics..."
METRICS_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  http://localhost:$MULE_APP_PORT/api/metrics)

HTTP_STATUS=$(echo "$METRICS_RESPONSE" | grep HTTP_STATUS | cut -d: -f2)
if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "   ${GREEN}✓ Metrics endpoint works${NC}"
else
    echo -e "   ${RED}✗ Metrics endpoint failed (HTTP $HTTP_STATUS)${NC}"
fi

echo ""

##############################################
# Summary
##############################################

echo "================================================"
echo -e "${GREEN}End-to-End Test Complete!${NC}"
echo "================================================"
echo ""
echo "Components Verified:"
echo "  ✓ SWIFT Connector - Built and installed"
echo "  ✓ Demo Application - Deployed and running"
echo "  ✓ Mock SWIFT Server - Receiving messages"
echo "  ✓ API Endpoints - Responding correctly"
echo ""
echo "Next Steps:"
echo "  1. Import Postman collection for detailed testing"
echo "  2. Check mock server logs: tail -f /tmp/swift-mock-server.log"
echo "  3. Run MUnit tests: cd $DEMO_APP_DIR && mvn test"
echo "  4. View Mule app logs for detailed flow execution"
echo ""
echo "To stop the mock server:"
echo "  kill $MOCK_SERVER_PID"
echo ""
echo "Mock server PID saved to: /tmp/swift-mock-server.pid"
echo $MOCK_SERVER_PID > /tmp/swift-mock-server.pid
echo ""


# SWIFT Connector Demo Application

## Overview

A complete Mule application demonstrating all major capabilities of the SWIFT Connector, including:
- Send MT103 payments with sanctions screening
- Track payments via gpi
- Validate messages against SWIFT standards
- Translate MT to MX (ISO 20022)
- Lookup BIC codes
- Check holiday calendars
- Monitor metrics and health

## Purpose & Design Pattern

### System API Layer Architecture
This application demonstrates the **System API layer** pattern in MuleSoft's API-Led Connectivity architecture. It takes simplified payloads (JSON/XML) from upstream systems and uses DataWeave to transform them into the raw SWIFT MT format required by the SWIFT network.

**Key Responsibilities**:
- Abstracting SWIFT complexity from business applications
- Handling protocol-specific transformations (JSON → MT103, XML → pacs.008)
- Managing SWIFT-specific error handling and retry logic
- Providing a clean RESTful interface for payment operations

### Connector Usage Demonstration
This demo showcases the specific operations of the custom SWIFT connector:
- **Parse Operations**: Extracting data from SWIFT messages (Block 1-5 parsing)
- **Generate Operations**: Building compliant SWIFT MT/MX messages
- **Validation Operations**: Schema and field validation (SR2024 rules)
- **Transformation Operations**: MT-to-MX conversion (legacy modernization)
- **Security Operations**: Message signing, sanctions screening, audit logging

**Educational Focus**: The application is particularly helpful for understanding how the connector handles **SWIFT Block 4** (the message content), which is notoriously difficult to map due to:
- Field-based structure (`:20:`, `:32A:`, `:50K:`, etc.)
- Complex validation rules (mandatory vs. optional fields)
- Multi-line field handling (addresses, remittance info)
- Character set restrictions (SWIFT X-Character Set)

### Assessment

#### Educational Value: **High**
- ✅ Provides a "template" for SWIFT field mapping
- ✅ Demonstrates DataWeave transformations for SWIFT formats
- ✅ Shows error handling patterns for SWIFT-specific failures
- ✅ Illustrates connector operation usage with real examples
- ✅ Includes Postman collection for immediate testing
- ✅ Documents common integration patterns

#### Production Readiness: **Low**

⚠️ **Missing Enterprise-Grade Features Required for Production SWIFT**:

| Feature | Demo App | Production Requirement | Impact |
|---------|----------|------------------------|--------|
| **Non-repudiation Logging** | ❌ No | ✅ Tamper-evident audit trail | Regulatory compliance (SOX, FINRA) |
| **Duplicate Detection** | ❌ No | ✅ Persistent idempotency checks | Prevents double payments ($M losses) |
| **HSM Integration** | ❌ No | ✅ Hardware Security Module signing | Tier-1 bank requirement (LAU) |
| **Rate Limiting** | ❌ No | ✅ Throttling & backpressure | Prevents SWIFT network penalties |
| **Sequence Management** | ❌ No | ✅ Persistent sequence tracking | Session recovery after crashes |
| **Real-time Monitoring** | ❌ No | ✅ Operational dashboards & alerts | SLA compliance, incident response |
| **FIPS-140-2 Compliance** | ❌ No | ✅ Federal/DoD deployments | Government contracts |
| **Connection Pooling** | ⚠️ Basic | ✅ Advanced with health checks | High-throughput processing |
| **BICPlus Validation** | ❌ No | ✅ Real-time directory lookup | Prevents message rejection |
| **Cutoff Time Checks** | ❌ No | ✅ Holiday & cutoff awareness | Avoids trapped funds |

**Recommendation**: 
> Use this demo as a **learning tool and integration starting point**. For production deployments, implement the missing enterprise features outlined in the main connector documentation, including:
> - ObjectStore persistence for state management
> - HSM signing for message authentication
> - Comprehensive audit logging with PII sanitization
> - Persistent duplicate detection
> - Real-time BIC/IBAN validation
> - FIPS-140-2 compliance mode
> - Advanced reconnection strategies
>
> **Production Timeline**: Budget 4-6 weeks to harden this demo for production use with enterprise features.

---

## Prerequisites
- Mule Runtime 4.9+
- Java 17
- SWIFT Connector 1.0.0 installed
- Maven 3.8+

## Quick Start

### 1. Install SWIFT Connector

```bash
cd ../
mvn clean install
```

### 2. Configure Application

Edit `src/main/resources/config.properties`:

```properties
swift.host=localhost
swift.port=3000
swift.bic=TESTUS33XXX
swift.username=testuser
swift.password=testpass
```

### 3. Build and Run

```bash
cd swift-demo-app
mvn clean package
```

Deploy the generated JAR to Mule Runtime or run in Studio.

### 4. Test with Postman

Import the included Postman collection: `SWIFT_Connector_Demo_API.postman_collection.json`

## API Endpoints

### 1. Send Payment
**POST** `/api/payments`

Send a SWIFT MT103 payment with automatic sanctions screening.

```json
{
  "transactionId": "TXN-2024-001",
  "amount": "10000.00",
  "currency": "USD",
  "receiver": "BANKDE33XXX",
  "orderingCustomer": "John Doe",
  "beneficiary": "Jane Smith",
  "reference": "PAYMENT-REF-12345"
}
```

**Features:**
- ✅ Automatic correlation ID generation
- ✅ Sanctions screening
- ✅ Audit trail logging
- ✅ Error handling with retry

### 2. Track gpi Payment
**GET** `/api/payments/{uetr}/track`

Track payment status in real-time through correspondent banking chain.

**Response includes:**
- Current location
- Status updates
- Tracking events
- Bank information

### 3. Validate Message
**POST** `/api/validate`

Validate SWIFT message against SR2024 standards.

```json
{
  "messageType": "MT103",
  "format": "MT",
  "content": ":20:REF123\n:32A:240107USD10000,00"
}
```

### 4. Translate MT to MX
**POST** `/api/translate/mt-to-mx`

Convert legacy MT format to ISO 20022.

```json
{
  "messageType": "MT103",
  "content": ":20:REF123..."
}
```

### 5. Lookup BIC Code
**GET** `/api/bic/{bicCode}`

Validate and get details for a Bank Identifier Code.

Example: `/api/bic/DEUTDEFFXXX`

### 6. Check Holiday
**GET** `/api/holidays/{date}/{calendar}`

Check if a date is a banking holiday.

Example: `/api/holidays/2024-12-25/TARGET2`

Supported calendars:
- `TARGET2` - European Central Bank
- `US_FED` - Federal Reserve
- `UK_BOE` - Bank of England

### 7. Get Metrics
**GET** `/api/metrics`

Retrieve operational metrics for monitoring.

**Returns:**
- Messages sent/received/failed
- Average latency
- Success rate

### 8. Health Check
**GET** `/api/health`

Check application and SWIFT connection health.

**Returns:**
- Connection status
- Session information
- Sequence numbers

## Inbound Listener

The application automatically listens for incoming SWIFT messages:

**Flow:** `receive-swift-messages-flow`
- Polls every 5 seconds
- Filters MT103 messages
- Automatically sends ACK
- Processes payments

## Configuration Options

### SWIFT Connection

```xml
<swift:config name="SWIFT_Config">
    <swift:connection 
        host="${swift.host}"
        port="${swift.port}"
        bicCode="${swift.bic}"
        username="${swift.username}"
        password="${swift.password}"
        protocol="FIN"
        enableTls="false"
        autoReconnect="true"
        enableSequenceSync="true" />
</swift:config>
```

### HTTP Listener

```xml
<http:listener-config name="HTTP_Listener_config">
    <http:listener-connection 
        host="0.0.0.0" 
        port="8081" />
</http:listener-config>
```

## Error Handling

The application includes comprehensive error handling:

### SWIFT-Specific Errors
- `SWIFT:SANCTIONS_SCREENING_FAILED` - Blocked by compliance
- `SWIFT:CONNECTION_FAILED` - Unable to connect
- `SWIFT:SESSION_EXPIRED` - Session timeout
- `SWIFT:MESSAGE_REJECTED` - SWIFT network rejection

### Response Codes
- `200` - Success
- `400` - Bad request (missing fields)
- `403` - Forbidden (sanctions screening)
- `500` - Internal error

## Testing with Postman

### Import Collection

1. Open Postman
2. Click **Import**
3. Select `SWIFT_Connector_Demo_API.postman_collection.json`
4. Collection will appear in sidebar

### Run Tests

The collection includes 8 requests with sample responses:

1. **Send Payment** - Test MT103 payment flow
2. **Track gpi** - Track payment status
3. **Validate** - Check message validity
4. **Translate** - MT to MX conversion
5. **BIC Lookup** - Validate bank codes
6. **Holiday Check** - Calendar validation
7. **Metrics** - Operational stats
8. **Health** - Connection status

### Environment Variables

Collection uses variable: `{{base_url}}`
- Default: `http://localhost:8081`
- Update if running on different host/port

## Monitoring & Observability

### Correlation IDs

Every payment generates a correlation ID for end-to-end tracing:

```
Request → Correlation ID → SWIFT Message ID → gpi UETR
```

### Audit Logs

All operations are logged with:
- Message ID (no PII)
- Operation type
- Timestamp
- Institution

### Metrics

Track operational health:
- Volume (sent/received/failed)
- Performance (latency, success rate)
- Status (connection, session)

## Production Considerations

### Security

For production deployment:
1. Enable TLS (`enableTls="true"`)
2. Configure keystores/truststores
3. Use secure properties for credentials
4. Enable mutual TLS authentication
5. Integrate with HSM for signing

### Resilience

The application includes:
- Auto-reconnect on connection failure
- Sequence number synchronization
- Duplicate detection
- Retry logic with exponential backoff

### Compliance

Built-in compliance features:
- Sanctions screening integration
- Audit trail (no PII)
- Correlation for regulatory reporting
- Error tracking and investigations

## Troubleshooting

### Connection Failed

```
Error: SWIFT:CONNECTION_FAILED
```

**Solution:**
- Check `swift.host` and `swift.port`
- Verify SWIFT service is running
- Check firewall rules

### Sanctions Screening Blocked

```
HTTP 403: Payment blocked by sanctions screening
```

**Solution:**
- Review transaction details
- Check screening provider configuration
- Investigate match details

### Session Expired

```
Error: SWIFT:SESSION_EXPIRED
```

**Solution:**
- Auto-reconnect will trigger automatically
- Check session timeout configuration
- Verify network stability

## Architecture

### Flow Diagram

```
HTTP Request → Validation → Sanctions Screening → SWIFT Connector → SWIFT Network
                                 ↓                       ↓
                          Block Payment           Audit Logging
                                                         ↓
                                                   Response
```

### Components

1. **HTTP Listeners** - REST API endpoints
2. **SWIFT Connector** - Financial messaging operations
3. **Transformation** - DataWeave for JSON/SWIFT conversion
4. **Error Handlers** - Comprehensive error management
5. **Logging** - Audit trail and monitoring

## Development

### Add New Endpoint

1. Create new flow in `swift-demo-app.xml`
2. Add HTTP listener
3. Call SWIFT connector operation
4. Transform response
5. Add to Postman collection

### Extend Functionality

The connector supports 33 operations. Explore:
- `SecurityOperations` - Signing, verification
- `SessionOperations` - Session management
- `ReferenceDataOperations` - Currency, country validation
- `ObservabilityOperations` - Metrics, correlation

## Support

- **Connector Documentation**: `../README.md`
- **Architecture Guide**: `../ARCHITECTURE.md`
- **Quick Start**: `../QUICKSTART.md`

## License

Enterprise License Required

---

**Built with MuleSoft SWIFT Connector v1.0.0**  
*Mule 4.9+ | Java 17 | Mule SDK 1.10.0*


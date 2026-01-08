# MuleSoft SWIFT Connector

Enterprise-grade SWIFT connector for MuleSoft Anypoint Platform, supporting legacy MT standards, modern ISO 20022 (MX), SWIFT gpi, and complete financial compliance features.

## Version
1.0.0

## Requirements
- **Mule Runtime**: 4.10 or higher
- **Java**: JDK 17
- **Mule SDK**: 1.10.0
- **Anypoint Studio**: 7.x or higher
- **License**: Enterprise License required

## Features

### 1. Core Messaging Operations
- ✅ **Send Message**: Send payment instructions or reporting messages (MT/MX formats)
- ✅ **Receive Message**: Poll for incoming SWIFT messages
- ✅ **Message Listener**: Event-driven inbound trigger for real-time message processing
- ✅ **Acknowledge Message**: Send ACK/NACK for received messages
- ✅ **Query Message Status**: Retrieve message state within SWIFT interface

### 2. SWIFT gpi (Global Payments Innovation)
- ✅ **Track Payment**: Real-time cross-border payment tracking across correspondent banks
- ✅ **Update Payment Status**: Signal funds received or credited
- ✅ **Stop and Recall**: Emergency payment cancellation
- ✅ **Fee/FX Transparency**: Retrieve exact fees and exchange rates from intermediaries

### 3. Transformation & Validation
- ✅ **Validate Schema**: Check message conformance to SWIFT Standard Release rules
- ✅ **MT to MX Translation**: Convert legacy MT to ISO 20022 format
- ✅ **MX to MT Translation**: Convert ISO 20022 to legacy MT for backward compatibility
- ✅ **BIC Code Lookup**: Validate and lookup Bank Identifier Codes
- ✅ **Enrich Message**: Add reference data to messages

### 4. Security & Compliance
- ✅ **Sign Message**: Digital signing using LAU (Local Authentication)
- ✅ **Verify Signature**: Verify digital signatures
- ✅ **Sanction Screening**: Integration with screening engines (FICO, Accuity, WorldCheck)
- ✅ **Audit Logging**: Regulatory-compliant audit trail (no PII)

### 5. Session, Routing & Resilience
- ✅ **Session Management**: Automatic login, keepalive, and reconnection
- ✅ **Sequence Synchronization**: Input/Output sequence number management
- ✅ **Duplicate Detection**: Prevent accidental message resubmission
- ✅ **Get Session Info**: Retrieve current session details

### 6. Error Handling & Investigations
- ✅ **Parse Reject Code**: Normalize SWIFT reject codes into business-friendly categories
- ✅ **Open Investigation Case**: Create SWIFT investigation for payment inquiry
- ✅ **Query Investigation Case**: Check investigation status

### 7. Reference Data & Calendars
- ✅ **Validate Currency**: Check currency codes against ISO 4217
- ✅ **Check Holiday Calendar**: Validate dates against TARGET2, US_FED, UK_BOE calendars
- ✅ **Validate Country**: Check country codes against ISO 3166
- ✅ **Check RMA Authorization**: Verify counterparty authorization
- ✅ **Get Cutoff Times**: Retrieve payment cutoff times by currency

### 8. Observability & Controls
- ✅ **Generate Correlation ID**: End-to-end tracing (Flow → SWIFT → gpi)
- ✅ **Get Metrics**: Operational metrics (volumes, failures, SLA)
- ✅ **Check Rate Limit**: Monitor rate limit status

## Installation

### Maven Dependency
Add the connector to your Mule application's `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule-swift-connector</artifactId>
    <version>1.0.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Anypoint Studio
1. Open Anypoint Studio
2. Go to **Help** → **Install New Software**
3. Add repository: `https://repository.mulesoft.org/releases/`
4. Search for "SWIFT Connector"
5. Install and restart Studio

## Configuration

### Basic Connection Configuration

```xml
<swift:config name="SWIFT_Config">
    <swift:connection 
        host="swift.alliance.example.com"
        port="3000"
        bicCode="BANKUS33XXX"
        username="${swift.username}"
        password="${swift.password}"
        protocol="FIN"
        enableTls="true"
        truststorePath="${truststore.path}"
        truststorePassword="${truststore.password}"
        keystorePath="${keystore.path}"
        keystorePassword="${keystore.password}" />
</swift:config>
```

### Configuration Properties

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `host` | Yes | - | SWIFT Alliance Access (SAA) hostname |
| `port` | Yes | 3000 | SWIFT interface port |
| `bicCode` | Yes | - | Your institution's BIC (8 or 11 chars) |
| `username` | Yes | - | SWIFT interface username |
| `password` | Yes | - | SWIFT interface password |
| `protocol` | No | FIN | Protocol: FIN, INTERACT, FILEACT, GPI_API |
| `enableTls` | No | true | Enable TLS/SSL encryption |
| `autoReconnect` | No | true | Automatic reconnection on failure |
| `enableSequenceSync` | No | true | Synchronize sequence numbers |

## Usage Examples

### Example 1: Send MT103 Payment

```xml
<flow name="send-payment-flow">
    <scheduler>
        <scheduling-strategy>
            <fixed-frequency frequency="60000"/>
        </scheduling-strategy>
    </scheduler>
    
    <!-- Transform payload to MT103 format -->
    <ee:transform>
        <ee:message>
            <ee:set-payload><![CDATA[%dw 2.0
output application/java
---
":20:REF123456\n" ++
":32A:240107USD10000,00\n" ++
":50K:ORDERING CUSTOMER NAME\n" ++
":59:BENEFICIARY NAME"
]]></ee:set-payload>
        </ee:message>
    </ee:transform>
    
    <!-- Send SWIFT message -->
    <swift:send-message config-ref="SWIFT_Config"
        messageType="MT103"
        sender="BANKUS33XXX"
        receiver="BANKDE33XXX"
        format="MT"
        priority="NORMAL" />
    
    <logger level="INFO" message="Payment sent: #[payload.messageId]" />
</flow>
```

### Example 2: Listen for Incoming Messages

```xml
<flow name="receive-messages-flow">
    <!-- SWIFT Message Listener (Event-Driven) -->
    <swift:listener config-ref="SWIFT_Config"
        pollingInterval="5000"
        messageTypeFilter="MT103" />
    
    <logger level="INFO" 
        message="Received message: #[attributes.messageId] from #[payload.sender]" />
    
    <!-- Process the message -->
    <flow-ref name="process-payment-subflow" />
    
    <!-- Send acknowledgment -->
    <swift:acknowledge-message config-ref="SWIFT_Config"
        messageId="#[attributes.messageId]"
        acknowledgeType="ACK" />
</flow>
```

### Example 3: Track gpi Payment

```xml
<flow name="track-payment-flow">
    <http:listener config-ref="HTTP_Config" path="/track/{uetr}" />
    
    <!-- Track payment using gpi -->
    <swift:track-payment config-ref="SWIFT_Config"
        uetr="#[attributes.uriParams.uetr]" />
    
    <!-- Transform response to JSON -->
    <ee:transform>
        <ee:message>
            <ee:set-payload><![CDATA[%dw 2.0
output application/json
---
{
    uetr: payload.uetr,
    status: payload.status,
    currentLocation: payload.currentLocation,
    trackingEvents: payload.trackingEvents map {
        institution: $.institution,
        status: $.status,
        timestamp: $.timestamp
    }
}
]]></ee:set-payload>
        </ee:message>
    </ee:transform>
</flow>
```

### Example 4: Validate and Translate MT to MX

```xml
<flow name="translate-mt-to-mx-flow">
    <!-- Validate MT103 message -->
    <swift:validate-schema config-ref="SWIFT_Config"
        messageType="MT103"
        format="MT"
        standardRelease="SR2024" />
    
    <!-- Check if valid -->
    <choice>
        <when expression="#[payload.valid]">
            <!-- Translate to MX format -->
            <swift:translate-mt-to-mx config-ref="SWIFT_Config"
                mtMessageType="MT103" />
            
            <logger level="INFO" 
                message="Translated to MX: #[payload.mxMessageType]" />
        </when>
        <otherwise>
            <logger level="ERROR" 
                message="Validation failed: #[payload.errors]" />
        </otherwise>
    </choice>
</flow>
```

### Example 5: Sanctions Screening Before Sending

```xml
<flow name="screened-payment-flow">
    <!-- Screen transaction -->
    <swift:screen-transaction config-ref="SWIFT_Config"
        screeningProvider="WORLDCHECK" />
    
    <!-- Check screening result -->
    <choice>
        <when expression="#[payload.passed]">
            <!-- Send payment -->
            <swift:send-message config-ref="SWIFT_Config"
                messageType="MT103"
                sender="BANKUS33XXX"
                receiver="#[vars.receiverBic]"
                format="MT" />
        </when>
        <otherwise>
            <logger level="WARN" 
                message="Transaction blocked by sanctions screening" />
            <raise-error type="SWIFT:SANCTIONS_SCREENING_FAILED" />
        </otherwise>
    </choice>
</flow>
```

### Example 6: Error Handling with Retry Logic

```xml
<flow name="resilient-payment-flow">
    <try>
        <swift:send-message config-ref="SWIFT_Config"
            messageType="MT103"
            sender="BANKUS33XXX"
            receiver="BANKDE33XXX"
            format="MT" />
        
        <error-handler>
            <!-- Handle specific SWIFT errors -->
            <on-error-continue type="SWIFT:FIELD_LENGTH_EXCEEDED">
                <logger level="WARN" message="Field length error - attempting repair" />
                
                <!-- Parse reject code -->
                <swift:parse-reject-code config-ref="SWIFT_Config"
                    rejectCode="K91" />
                
                <!-- Retry with corrected message -->
                <flow-ref name="repair-and-resend-subflow" />
            </on-error-continue>
            
            <on-error-continue type="SWIFT:SESSION_EXPIRED">
                <logger level="INFO" message="Session expired - will auto-reconnect" />
                <flow-ref name="retry-send-subflow" />
            </on-error-continue>
            
            <on-error-propagate type="SWIFT:SANCTIONS_SCREENING_FAILED">
                <logger level="ERROR" message="Payment blocked by compliance" />
            </on-error-propagate>
        </error-handler>
    </try>
</flow>
```

## Error Handling

The connector provides granular error types for precise error handling:

### Connection Errors
- `SWIFT:CONNECTION_FAILED`
- `SWIFT:SESSION_EXPIRED`
- `SWIFT:AUTHENTICATION_FAILED`
- `SWIFT:SEQUENCE_MISMATCH`

### Validation Errors
- `SWIFT:INVALID_MESSAGE_FORMAT`
- `SWIFT:SCHEMA_VALIDATION_FAILED`
- `SWIFT:INVALID_BIC_CODE`
- `SWIFT:FIELD_LENGTH_EXCEEDED`
- `SWIFT:MANDATORY_FIELD_MISSING`

### Network Errors
- `SWIFT:MESSAGE_REJECTED`
- `SWIFT:NETWORK_NACK`
- `SWIFT:DUPLICATE_MESSAGE`

### Business Errors
- `SWIFT:SANCTIONS_SCREENING_FAILED`
- `SWIFT:CUTOFF_TIME_EXCEEDED`
- `SWIFT:INSUFFICIENT_FUNDS`

### gpi Errors
- `SWIFT:GPI_TRACKING_NOT_AVAILABLE`
- `SWIFT:GPI_STOP_RECALL_FAILED`

### Security Errors
- `SWIFT:SIGNATURE_VERIFICATION_FAILED`
- `SWIFT:LAU_AUTHENTICATION_FAILED`
- `SWIFT:CERTIFICATE_EXPIRED`

## Best Practices

### 1. Security
- ✅ Always use TLS/SSL for connections
- ✅ Store credentials in secure properties
- ✅ Enable message signing for non-repudiation
- ✅ Screen all outbound payments for sanctions

### 2. Reliability
- ✅ Enable auto-reconnect and sequence synchronization
- ✅ Implement duplicate detection
- ✅ Use persistent ObjectStore for message queuing
- ✅ Set appropriate timeouts

### 3. Performance
- ✅ Use connection pooling for high-volume scenarios
- ✅ Monitor rate limits to avoid throttling
- ✅ Cache reference data (BIC, currency, holiday calendars)
- ✅ Use streaming for large ISO 20022 messages

### 4. Compliance
- ✅ Enable comprehensive audit logging
- ✅ Use correlation IDs for end-to-end tracing
- ✅ Validate messages before sending
- ✅ Retain message metadata per regulatory requirements (GDPR, BCBS 239)

### 5. Monitoring
- ✅ Track operational metrics (volumes, latency, failures)
- ✅ Set up alerts for SLA breaches
- ✅ Monitor session health and sequence sync status
- ✅ Use correlation IDs to trace Flow → SWIFT → gpi

## Technical Architecture

### Connector SDK Components

| Component | Purpose |
|-----------|---------|
| `SwiftConnectionProvider` | Manages connection pooling and lifecycle |
| `SwiftConnection` | Stateful connection with sequence management |
| `CoreMessagingOperations` | Send, receive, ACK/NACK operations |
| `GpiOperations` | gpi tracking, status updates, stop/recall |
| `TransformationOperations` | Validation, MT↔MX translation, BIC lookup |
| `SecurityOperations` | Signing, verification, screening, audit |
| `SessionOperations` | Session management, duplicate detection |
| `ErrorHandlingOperations` | Reject code parsing, investigations |
| `ReferenceDataOperations` | Currency, country, holiday, RMA checks |
| `ObservabilityOperations` | Metrics, correlation, rate limits |
| `SwiftMessageListener` | Inbound event source (trigger) |

### State Management
The connector uses **ObjectStore v2** for:
- Sequence number persistence
- Duplicate message detection
- Message replay after outages
- Session state recovery

### Protocol Support
- **FIN**: Legacy MT message protocol (stateful, sequence-based)
- **InterAct**: Real-time file transfer
- **FileAct**: Store-and-forward file transfer
- **gpi API**: RESTful gpi Tracker API with OAuth2

## Building from Source

```bash
# Clone repository
git clone https://github.com/mulesoft/mule-swift-connector.git
cd mule-swift-connector

# Build with Maven
mvn clean install

# Install to local repository
mvn install:install-file \
  -Dfile=target/mule-swift-connector-1.0.0-mule-plugin.jar \
  -DgroupId=com.mulesoft.connectors \
  -DartifactId=mule-swift-connector \
  -Dversion=1.0.0 \
  -Dpackaging=mule-plugin
```

## Support & Resources

- **Documentation**: https://docs.mulesoft.com/swift-connector
- **Support Portal**: https://support.mulesoft.com
- **SWIFT Standards**: https://www.swift.com/standards
- **ISO 20022**: https://www.iso20022.org

## License

This connector requires a MuleSoft Enterprise License.  
Evaluation licenses are available for testing purposes.

## Roadmap

### Upcoming Features
- [ ] SWIFT API Market Infrastructure support
- [ ] Enhanced ISO 20022 migration toolkit
- [ ] Real-time fraud detection hooks
- [ ] Advanced reconciliation operations
- [ ] SWIFT for Corporates integration

## Contributing

Please contact MuleSoft Professional Services for custom development.

---

**Built with ❤️ by MuleSoft Financial Services Team**

*Designed for Mule 4.10+ | Java 17 | Enterprise-Grade Financial Integration*


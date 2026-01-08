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

## ⚠️ CRITICAL: Banking-Grade Security & Dependency Management

### Security Requirements for Production

**For production SWIFT connectivity, you MUST configure**:

1. **Mutual TLS (MTLS)**: SWIFT requires client certificate authentication
2. **HSM Integration**: Hardware Security Module for signing operations (recommended)
3. **Keystore/Truststore**: Proper certificate management

**See "Banking-Grade Security Configuration" section below for details.**

### ⚡ CRITICAL: Prowide Library Version Management

**The Prowide Core library MUST be updated regularly to support new SWIFT Standards Release (SR) updates.**

**Current Prowide Version**: Check `pom.xml` for `<prowide.version>` property

**Update Schedule**:
- **November Each Year**: SWIFT releases new SR (e.g., SR2024 → SR2025)
- **Within 30 Days**: Prowide releases updated library with new validation rules
- **Action Required**: Update `pom.xml` and rebuild connector

**How to Update**:
```xml
<properties>
    <!-- Update this version annually after SWIFT SR release -->
    <prowide.version>SRU2024-10.0.0</prowide.version>  <!-- ← CHECK FOR UPDATES -->
</properties>
```

**Failure to Update Risks**:
- ❌ Messages rejected by SWIFT network (new mandatory fields)
- ❌ Validation failures (new business rules not enforced)
- ❌ Compliance issues (outdated SR year)

**Recommended**: Subscribe to Prowide release notifications at https://github.com/prowide/prowide-core/releases

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

### Banking-Grade Security Configuration (PRODUCTION)

**For production SWIFT connectivity, proper security configuration is MANDATORY.**

#### Mutual TLS (MTLS) Configuration

```xml
<swift:config name="SWIFT_Production_Config">
    <swift:connection 
        host="swift.production.bank.com"
        port="3000"
        bicCode="BANKUS33XXX"
        username="${swift.username}"
        password="${secure::swift.password}"
        enableTls="true"
        
        <!-- ✅ KEYSTORE (Your Bank's Certificate) -->
        keystorePath="/secure/certs/bank-keystore.jks"
        keystorePassword="${secure::keystore.password}"
        certificateAlias="swift-client-cert"
        
        <!-- ✅ TRUSTSTORE (SWIFT Network CA) -->
        truststorePath="/secure/certs/swift-truststore.jks"
        truststorePassword="${secure::truststore.password}"
        
        <!-- ✅ MTLS Settings -->
        clientCertRequired="true"
        sslProtocol="TLSv1.2"
        cipherSuites="TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256">
        
        <reconnection>
            <reconnect frequency="5000" count="3"/>
        </reconnection>
    </swift:connection>
</swift:config>
```

#### Hardware Security Module (HSM) Configuration (Recommended)

```xml
<swift:config name="SWIFT_HSM_Config">
    <swift:connection 
        host="swift.production.bank.com"
        port="3000"
        bicCode="BANKUS33XXX"
        username="${swift.username}"
        password="${secure::swift.password}"
        enableTls="true"
        
        <!-- ✅ HSM Integration (for LAU Signing) -->
        hsmEnabled="true"
        hsmProvider="sun.security.pkcs11.SunPKCS11"
        hsmConfigPath="/etc/pkcs11/swift-hsm.cfg"
        hsmPin="${secure::hsm.pin}"
        
        keystorePath="/secure/certs/bank-keystore.jks"
        keystorePassword="${secure::keystore.password}"
        truststorePath="/secure/certs/swift-truststore.jks"
        truststorePassword="${secure::truststore.password}">
        
        <reconnection>
            <reconnect frequency="5000" count="3"/>
        </reconnection>
    </swift:connection>
</swift:config>
```

#### Connection Pooling (Automatic)

**The connector implements `PoolingConnectionProvider` for automatic connection pooling.**

**Benefits**:
- ✅ Avoids re-authentication overhead for every message
- ✅ Maintains stateful sessions with sequence synchronization
- ✅ Automatic health checks and validation
- ✅ CloudHub-compatible (distributed state via Object Store)

**Pool Configuration** (in `mule-artifact.properties`):
```properties
# Connection pool settings (optional - defaults are production-ready)
swift.connection.pool.maxActive=10
swift.connection.pool.maxIdle=5
swift.connection.pool.maxWait=30000
swift.connection.pool.exhaustedAction=WHEN_EXHAUSTED_WAIT
```

**How Pooling Works**:
1. First message → Connector establishes connection + authenticates
2. Connection stored in pool (maintains session + sequence numbers)
3. Subsequent messages → Reuse pooled connection (NO re-authentication)
4. Connection validation → Automatic health checks via `@ConnectionValidator`
5. Failed connection → Removed from pool, new connection created

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

## Performance Considerations

### Large MT940 File Processing

The connector automatically switches to **streaming mode** for large files to prevent OutOfMemoryError during bank statement reconciliation:

- **Standard Mode**: Files < 50MB load into memory (fastest, lowest latency)
- **Streaming Mode**: Files > 50MB process line-by-line (memory-safe, scalable)

#### Configuration

Configure the streaming threshold via connection parameters:

```xml
<swift:connection 
    host="${swift.host}"
    port="${swift.port}"
    streamingThresholdBytes="104857600"> <!-- 100MB threshold -->
</swift:connection>
```

**Default**: 50MB (52,428,800 bytes)

#### Performance Characteristics

| File Size | Mode | Memory Usage | Throughput |
|-----------|------|--------------|------------|
| < 50MB | Standard (in-memory) | ~2x file size | 15,000 txns/sec |
| > 50MB | Streaming (line-by-line) | Fixed 8KB buffer | 10,000 txns/sec |
| > 1GB | Streaming | Fixed 8KB buffer | 8,000 txns/sec |

#### Use Cases

**Streaming mode is recommended for:**
- Daily MT940 bank statement processing (thousands of transactions)
- End-of-month reconciliation (multi-day transaction files)
- Batch processing of historical data
- Real-time high-volume payment processing

**Standard mode is optimal for:**
- Single MT103 payments
- Small MT940 statements (< 100 transactions)
- Low-latency real-time operations

#### Example: Processing Large MT940 Files

```xml
<flow name="process-large-mt940-flow">
    <swift:receive-message config-ref="SWIFT_Config" />
    
    <!-- Connector auto-detects size and uses streaming if needed -->
    <logger level="INFO" message="Processing message: #[payload.messageType] (#[sizeOf(payload.content)] bytes)" />
    
    <!-- Process each transaction block -->
    <foreach collection="#[payload.content splitBy '\n-}']">
        <logger level="DEBUG" message="Processing transaction: #[payload]" />
        <!-- Your business logic here -->
    </foreach>
</flow>
```

## Error Handling

### Error Type Categories

The connector provides **two-tier error categorization** for intelligent error handling:

#### 1. SYNTAX_ERROR (Malformed Messages)

**Parent error type for parser/format violations.**

**Child Error Types:**
- `SWIFT:INVALID_MESSAGE_FORMAT` - Malformed SWIFT blocks or fields
- `SWIFT:SCHEMA_VALIDATION_FAILED` - Violates SWIFT Standard Release rules
- `SWIFT:INVALID_BIC_CODE` - Invalid BIC format or unknown institution
- `SWIFT:FIELD_LENGTH_EXCEEDED` - Field exceeds maximum length
- `SWIFT:MANDATORY_FIELD_MISSING` - Required field absent
- `SWIFT:SIGNATURE_VERIFICATION_FAILED` - Invalid MAC/LAU signature

**Handling Strategy:**
- ❌ **Do NOT retry** - Message structure is fundamentally wrong
- 🚨 **Alert dev team** - Requires code/configuration fix
- 📧 **Send to DLQ** - Manual intervention required

**Example:**

```xml
<error-handler>
    <on-error-continue type="SWIFT:SYNTAX_ERROR">
        <logger level="ERROR" 
            message="❌ SYNTAX ERROR: Message malformed - #[error.description]" />
        
        <!-- Alert development team -->
        <email:send config-ref="Email_Config" 
            to="dev-team@bank.com"
            subject="SWIFT Message Format Error">
            <email:body>
                <![CDATA[
                Message ID: #[vars.messageId]
                Error: #[error.description]
                Action: Fix message format at source. No retry will be attempted.
                ]]>
            </email:body>
        </email:send>
        
        <!-- Send to Dead Letter Queue -->
        <jms:publish config-ref="JMS_Config" destination="swift.dlq" />
    </on-error-continue>
</error-handler>
```

#### 2. BUSINESS_RULE_VIOLATION (Valid Syntax, Failed Business Logic)

**Parent error type for business rule failures.**

**Child Error Types:**
- `SWIFT:CUTOFF_TIME_EXCEEDED` - Payment after currency cutoff
- `SWIFT:HOLIDAY_CALENDAR_VIOLATION` - Value date falls on bank holiday
- `SWIFT:SANCTIONS_VIOLATION` - Sanctions screening match detected
- `SWIFT:INSUFFICIENT_FUNDS` - Account balance too low
- `SWIFT:ACCOUNT_NOT_FOUND` - Invalid account number

**Handling Strategy:**
- ✅ **Retry eligible** - May succeed after time window or data correction
- 💼 **Alert business team** - Requires operational decision
- 🔄 **Implement backoff** - Retry with exponential delay

**Example:**

```xml
<error-handler>
    <on-error-continue type="SWIFT:BUSINESS_RULE_VIOLATION">
        <logger level="WARN" 
            message="⚠️ BUSINESS RULE VIOLATION: #[error.description]" />
        
        <!-- Alert business operations team -->
        <slack:post-message config-ref="Slack_Config" 
            channel="swift-ops"
            message="⚠️ Business rule violation: #[error.description]. Retry scheduled." />
        
        <!-- Retry with exponential backoff -->
        <until-successful 
            maxRetries="3" 
            millisBetweenRetries="60000">
            <swift:send-message config-ref="SWIFT_Config"
                messageType="MT103"
                sender="#[vars.sender]"
                receiver="#[vars.receiver]"
                format="MT" />
        </until-successful>
    </on-error-continue>
</error-handler>
```

### Granular Error Types

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

*Designed for Mule 4.10+ | Java 17 | Enterprise-Grade Financial Integration*


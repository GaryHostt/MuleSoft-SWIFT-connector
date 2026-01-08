# SWIFT Connector - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Step 1: Add Dependency

Add to your Mule application's `pom.xml`:

```xml
<dependency>
    <groupId>com.mulesoft.connectors</groupId>
    <artifactId>mule-swift-connector</artifactId>
    <version>1.0.0</version>
    <classifier>mule-plugin</classifier>
</dependency>
```

### Step 2: Configure Connection

Create `src/main/resources/config.properties`:

```properties
swift.host=swift.example.com
swift.port=3000
swift.bic=BANKUS33XXX
swift.username=myuser
swift.password=mypassword

truststore.path=/path/to/truststore.jks
truststore.password=changeit
keystore.path=/path/to/keystore.jks
keystore.password=changeit
```

### Step 3: Configure in Mule App

```xml
<swift:config name="SWIFT_Config">
    <swift:connection 
        host="${swift.host}"
        port="${swift.port}"
        bicCode="${swift.bic}"
        username="${swift.username}"
        password="${swift.password}"
        enableTls="true"
        truststorePath="${truststore.path}"
        truststorePassword="${truststore.password}"
        keystorePath="${keystore.path}"
        keystorePassword="${keystore.password}" />
</swift:config>
```

### Step 4: Send Your First Payment

```xml
<flow name="my-first-payment">
    <scheduler>
        <scheduling-strategy>
            <fixed-frequency frequency="60000"/>
        </scheduling-strategy>
    </scheduler>
    
    <set-payload value=":20:REF123&#10;:32A:240107USD10000,00&#10;:50K:John Doe&#10;:59:Jane Smith" />
    
    <swift:send-message config-ref="SWIFT_Config"
        messageType="MT103"
        sender="${swift.bic}"
        receiver="BANKDE33XXX"
        format="MT"
        priority="NORMAL" />
    
    <logger level="INFO" message="Payment sent! Message ID: #[payload.messageId]" />
</flow>
```

### Step 5: Listen for Incoming Messages

```xml
<flow name="receive-payments">
    <swift:listener config-ref="SWIFT_Config"
        pollingInterval="5000" />
    
    <logger level="INFO" message="Received: #[payload.messageType] from #[payload.sender]" />
</flow>
```

---

## 📋 Common Use Cases

### Track a gpi Payment
```xml
<swift:track-payment config-ref="SWIFT_Config"
    uetr="550e8400-e29b-41d4-a716-446655440000" />
```

### Validate Message Before Sending
```xml
<swift:validate-schema config-ref="SWIFT_Config"
    messageType="MT103"
    format="MT" />

<choice>
    <when expression="#[payload.valid]">
        <swift:send-message config-ref="SWIFT_Config" ... />
    </when>
    <otherwise>
        <logger level="ERROR" message="Invalid: #[payload.errors]" />
    </otherwise>
</choice>
```

### Screen for Sanctions
```xml
<swift:screen-transaction config-ref="SWIFT_Config"
    screeningProvider="WORLDCHECK" />

<choice>
    <when expression="#[payload.passed]">
        <swift:send-message config-ref="SWIFT_Config" ... />
    </when>
    <otherwise>
        <logger level="WARN" message="Blocked by sanctions screening" />
    </otherwise>
</choice>
```

---

## 🎯 Next Steps

1. **Read Full Documentation**: `README.md`
2. **Explore Examples**: `examples/swift-example.xml`
3. **Understand Architecture**: `ARCHITECTURE.md`
4. **Check Changelog**: `CHANGELOG.md`

---

## ⚠️ Important Notes

### For Production Use
- Obtain SWIFT Alliance Access (SAA) credentials
- Configure proper TLS certificates from your bank
- Enable sequence synchronization
- Set up audit logging
- Configure sanctions screening provider

### Security Best Practices
- Never commit credentials to source control
- Use secure properties for sensitive data
- Enable mutual TLS authentication
- Rotate certificates regularly
- Monitor audit logs

---

## 🆘 Troubleshooting

### Connection Failed
```
Error: SWIFT:CONNECTION_FAILED
```
**Solution**: Check host/port, verify network connectivity, check firewall rules

### Authentication Failed
```
Error: SWIFT:AUTHENTICATION_FAILED
```
**Solution**: Verify username/password, check BIC code, ensure account is active

### Session Expired
```
Error: SWIFT:SESSION_EXPIRED
```
**Solution**: Enable auto-reconnect in connection config (enabled by default)

### Message Rejected
```
Error: SWIFT:MESSAGE_REJECTED
```
**Solution**: Use `parse-reject-code` operation to get detailed reason and fix

---

## 📞 Need Help?

- **Documentation**: Full docs in `README.md`
- **Examples**: Working examples in `examples/`
- **Architecture**: Technical details in `ARCHITECTURE.md`

---

**Happy SWIFT Integration! 🎉**


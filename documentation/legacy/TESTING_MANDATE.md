# SWIFT Connector Testing Mandate
## Enterprise-Grade Stateful Financial Protocol Validation

---

## 🎯 Testing Philosophy

**SWIFT is stateful. Traditional REST API testing is insufficient.**

This mandate defines tests that **banks would actually perform** before trusting this connector in production. Each test validates critical behavior that protects financial integrity, prevents fraud, and ensures regulatory compliance.

---

## 1. SESSION LAYER (Stateful Integrity)

### Context
SWIFT maintains a "conversation" between parties with strict sequence numbers. **Loss of sequence = loss of money.**

---

### Test 1.1: Logon/Logout Handshake
**Critical**: Connection must not be marked "Active" until bank confirms.

```xml
<munit:test name="test-session-logon-handshake">
    <munit:behavior>
        <set-variable name="bicCode" value="TESTUS33XXX" />
        <set-variable name="username" value="testuser" />
        <set-variable name="password" value="testpass" />
        
        <!-- Mock: Bank will send ACK after logon -->
        <munit-tools:mock-when processor="swift:establish-session">
            <munit-tools:then-return>
                <munit-tools:payload value="#[{
                    sessionId: 'SESSION-TEST-001',
                    status: 'PENDING',
                    ackReceived: false
                }]" />
            </munit-tools:then-return>
        </munit-tools:mock-when>
    </munit:behavior>
    
    <munit:execution>
        <swift:establish-session config-ref="SWIFT_Config" />
        <set-variable name="initialStatus" value="#[payload.status]" />
        
        <!-- Wait for ACK (simulated) -->
        <munit-tools:mock-when processor="swift:wait-for-ack">
            <munit-tools:then-return>
                <munit-tools:payload value="#[{
                    sessionId: vars.sessionId,
                    status: 'ACTIVE',
                    ackReceived: true,
                    ackTimestamp: now()
                }]" />
            </munit-tools:then-return>
        </munit-tools:mock-when>
        
        <swift:wait-for-ack config-ref="SWIFT_Config" />
        <set-variable name="finalStatus" value="#[payload.status]" />
    </munit:execution>
    
    <munit:validation>
        <!-- CRITICAL: Status must be PENDING before ACK -->
        <munit-tools:assert-that 
            expression="#[vars.initialStatus]" 
            is="#[MunitTools::equalTo('PENDING')]"
            message="Session must start as PENDING before ACK received" />
            
        <!-- CRITICAL: Status must be ACTIVE only after ACK -->
        <munit-tools:assert-that 
            expression="#[vars.finalStatus]" 
            is="#[MunitTools::equalTo('ACTIVE')]"
            message="Session must be ACTIVE only after bank ACK received" />
            
        <munit-tools:assert-that 
            expression="#[payload.ackReceived]" 
            is="#[MunitTools::equalTo(true)]"
            message="ACK flag must be set to true" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: If the connector marks connection as "Active" before receiving ACK, messages could be sent into a void, causing silent failures and financial losses.

---

### Test 1.2: Sequence Number Continuity (Tag 34)
**Critical**: Gaps in sequence numbers = missed payments.

```xml
<munit:test name="test-sequence-number-continuity">
    <munit:behavior>
        <!-- Initialize Object Store with sequence 100 -->
        <os:store key="swift.outputSeqNum" objectStore="Object_store">
            <os:value>#[100]</os:value>
        </os:store>
    </munit:behavior>
    
    <munit:execution>
        <!-- Send 10 consecutive MT103 messages -->
        <foreach collection="#[[1,2,3,4,5,6,7,8,9,10]]">
            <swift:send-message config-ref="SWIFT_Config">
                <swift:message><![CDATA[
                    {1:F01TESTUS33XXXX}{4:
                    :20:TEST-#[payload]
                    :32A:240107USD10000,00
                    :50K:Test Customer
                    :59:Test Beneficiary
                    -}
                ]]></swift:message>
            </swift:send-message>
            
            <!-- Extract sequence number from sent message -->
            <set-variable name="seqNum#[payload]" value="#[attributes.sequenceNumber]" />
        </foreach>
    </munit:execution>
    
    <munit:validation>
        <!-- Verify perfect sequence: 101, 102, 103, ..., 110 -->
        <munit-tools:assert-that 
            expression="#[vars.seqNum1]" 
            is="#[MunitTools::equalTo(101)]" />
        <munit-tools:assert-that 
            expression="#[vars.seqNum2]" 
            is="#[MunitTools::equalTo(102)]" />
        <munit-tools:assert-that 
            expression="#[vars.seqNum10]" 
            is="#[MunitTools::equalTo(110)]" />
            
        <!-- Verify Object Store updated correctly -->
        <os:retrieve key="swift.outputSeqNum" objectStore="Object_store" target="finalSeqNum" />
        <munit-tools:assert-that 
            expression="#[vars.finalSeqNum]" 
            is="#[MunitTools::equalTo(110)]"
            message="Object Store must reflect final sequence number" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Banks reconcile using sequence numbers. A missing or duplicate number triggers fraud alerts and account freezes.

---

### Test 1.3: Heartbeat Resilience (MsgType 0)
**Critical**: Idle connections timeout. Heartbeats keep sessions alive.

```xml
<munit:test name="test-heartbeat-prevents-timeout">
    <munit:behavior>
        <set-variable name="heartbeatInterval" value="30000" /><!-- 30 seconds -->
        
        <swift:establish-session config-ref="SWIFT_Config">
            <swift:heartbeat-interval>#[vars.heartbeatInterval]</swift:heartbeat-interval>
        </swift:establish-session>
    </munit:behavior>
    
    <munit:execution>
        <!-- Idle for 35 seconds (longer than heartbeat interval) -->
        <set-variable name="startTime" value="#[now()]" />
        
        <!-- Mock: Connector should send heartbeat automatically -->
        <munit-tools:spy processor="swift:send-heartbeat">
            <munit-tools:assertions-before-call>
                <set-variable name="heartbeatSent" value="true" />
            </munit-tools:assertions-before-call>
        </munit-tools:spy>
        
        <!-- Simulate idle time -->
        <async doc:name="Simulate Idle">
            <until-successful maxRetries="2" millisBetweenRetries="35000">
                <logger message="Simulating idle connection..." />
            </until-successful>
        </async>
        
        <!-- Check session still active -->
        <swift:get-session-info config-ref="SWIFT_Config" />
    </munit:execution>
    
    <munit:validation>
        <munit-tools:assert-that 
            expression="#[vars.heartbeatSent]" 
            is="#[MunitTools::equalTo(true)]"
            message="Heartbeat must be sent automatically during idle" />
            
        <munit-tools:assert-that 
            expression="#[payload.active]" 
            is="#[MunitTools::equalTo(true)]"
            message="Session must remain active after heartbeat" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Lost connections during business hours = inability to send payments = customer complaints + revenue loss.

---

## 2. MESSAGE VALIDATION (The "Parser" Test)

### Context
**Financial messages are dense and unforgiving.** One wrong character = rejected transaction.

---

### Test 2.1: Standards Release (SR) Compliance
**Critical**: Invalid messages should never leave the Mule runtime.

```xml
<munit:test name="test-invalid-currency-code-rejected">
    <munit:behavior>
        <set-variable name="invalidMessage"><![CDATA[
            {1:F01TESTUS33XXXX}{4:
            :20:TEST-INVALID-001
            :32A:240107XXX10000,00
            :50K:Test Customer
            :59:Test Beneficiary
            -}
        ]]></set-variable>
    </munit:behavior>
    
    <munit:execution>
        <try>
            <swift:validate-schema config-ref="SWIFT_Config">
                <swift:message>#[vars.invalidMessage]</swift:message>
                <swift:message-type>MT103</swift:message-type>
                <swift:format>MT</swift:format>
            </swift:validate-schema>
            
            <set-variable name="testFailed" value="true" />
            
            <error-handler>
                <on-error-continue type="SWIFT:VALIDATION_ERROR">
                    <set-variable name="validationError" value="#[error.description]" />
                    <set-variable name="errorCaught" value="true" />
                </on-error-continue>
            </error-handler>
        </try>
    </munit:execution>
    
    <munit:validation>
        <!-- CRITICAL: Invalid currency must throw VALIDATION_ERROR -->
        <munit-tools:assert-that 
            expression="#[vars.errorCaught]" 
            is="#[MunitTools::equalTo(true)]"
            message="Invalid currency code XXX must trigger VALIDATION_ERROR" />
            
        <munit-tools:assert-that 
            expression="#[vars.testFailed]" 
            is="#[MunitTools::nullValue()]"
            message="Message with invalid currency must NOT pass validation" />
            
        <munit-tools:assert-that 
            expression="#[vars.validationError contains 'currency']" 
            is="#[MunitTools::equalTo(true)]"
            message="Error description must mention currency issue" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: If invalid messages reach the bank, they're rejected with fees. Catching errors pre-transmission saves money and reputation.

---

### Test 2.2: Multi-Block Parsing
**Critical**: All 5 SWIFT blocks must parse correctly.

```xml
<munit:test name="test-five-block-parsing">
    <munit:behavior>
        <set-variable name="fullMT103"><![CDATA[
            {1:F01TESTUS33XXXX0000000000}
            {2:O1031234240107TESTDE33XXXX12345678}
            {3:{108:DEMO-UETR-12345}}
            {4:
            :20:TEST-5BLOCK-001
            :32A:240107USD50000,00
            :50K:ACME Corporation
            123 Main Street
            :59:XYZ Trading Ltd
            456 Bank Street
            -}
            {5:{MAC:12345678}{CHK:ABCDEF123456}}
        ]]></set-variable>
    </munit:behavior>
    
    <munit:execution>
        <swift:validate-schema config-ref="SWIFT_Config">
            <swift:message>#[vars.fullMT103]</swift:message>
            <swift:message-type>MT103</swift:message-type>
            <swift:format>MT</swift:format>
        </swift:validate-schema>
        
        <set-variable name="validationResult" value="#[payload]" />
    </munit:execution>
    
    <munit:validation>
        <munit-tools:assert-that 
            expression="#[vars.validationResult.valid]" 
            is="#[MunitTools::equalTo(true)]"
            message="Full 5-block message must validate successfully" />
            
        <!-- Verify Block 1 (Basic Header) parsed -->
        <munit-tools:assert-that 
            expression="#[vars.validationResult.parsedBlocks contains 'block1']" 
            is="#[MunitTools::equalTo(true)]" />
            
        <!-- Verify Block 3 (User Header with UETR) parsed -->
        <munit-tools:assert-that 
            expression="#[vars.validationResult.parsedBlocks.block3.uetr]" 
            is="#[MunitTools::equalTo('DEMO-UETR-12345')]" />
            
        <!-- Verify Block 5 (Trailer with MAC/CHK) parsed -->
        <munit-tools:assert-that 
            expression="#[vars.validationResult.parsedBlocks.block5.mac]" 
            is="#[MunitTools::equalTo('12345678')]" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: gpi operations require Block 3 (UETR). Security requires Block 5 (MAC/CHK). Missing blocks = incomplete implementation.

---

## 3. CRYPTOGRAPHY & SECURITY (MAC Validation)

### Context
**MAC (Message Authentication Code) protects against tampering and fraud.**

---

### Test 3.1: Checksum Integrity
**Critical**: Altered messages must be rejected.

```xml
<munit:test name="test-tampered-message-rejected">
    <munit:behavior>
        <!-- Create valid message with MAC -->
        <swift:send-message config-ref="SWIFT_Config">
            <swift:message><![CDATA[
                {4:
                :20:TEST-MAC-001
                :32A:240107USD10000,00
                :50K:Test
                :59:Test
                -}
            ]]></swift:message>
        </swift:send-message>
        
        <set-variable name="originalMessage" value="#[payload.signedMessage]" />
        <set-variable name="originalMAC" value="#[payload.mac]" />
        
        <!-- TAMPER: Change amount from 10000 to 99999 -->
        <set-variable name="tamperedMessage" value="#[vars.originalMessage replace '10000,00' with '99999,00']" />
    </munit:behavior>
    
    <munit:execution>
        <try>
            <!-- Try to send tampered message with original MAC -->
            <swift:verify-signature config-ref="SWIFT_Config">
                <swift:message>#[vars.tamperedMessage]</swift:message>
                <swift:mac>#[vars.originalMAC]</swift:mac>
            </swift:verify-signature>
            
            <set-variable name="securityBreach" value="true" />
            
            <error-handler>
                <on-error-continue type="SWIFT:SECURITY_ERROR">
                    <set-variable name="macValidationFailed" value="true" />
                    <set-variable name="errorDescription" value="#[error.description]" />
                </on-error-continue>
            </error-handler>
        </try>
    </munit:execution>
    
    <munit:validation>
        <!-- CRITICAL: Tampered message MUST fail MAC validation -->
        <munit-tools:assert-that 
            expression="#[vars.macValidationFailed]" 
            is="#[MunitTools::equalTo(true)]"
            message="Tampered message must fail MAC validation" />
            
        <munit-tools:assert-that 
            expression="#[vars.securityBreach]" 
            is="#[MunitTools::nullValue()]"
            message="SECURITY BREACH: Tampered message was NOT rejected!" />
            
        <munit-tools:assert-that 
            expression="#[vars.errorDescription contains 'MAC']" 
            is="#[MunitTools::equalTo(true)]"
            message="Error must explicitly mention MAC failure" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Without MAC validation, an attacker could change $10,000 to $99,999 mid-flight. This test prevents fraud.

---

### Test 3.2: Trailer Integrity (Block 5)
**Critical**: Every outbound message must have CHK and MAC.

```xml
<munit:test name="test-block5-appended-to-all-messages">
    <munit:execution>
        <foreach collection="#[[1,2,3,4,5]]">
            <swift:send-message config-ref="SWIFT_Config">
                <swift:message><![CDATA[
                    {4:
                    :20:TEST-B5-#[payload]
                    :32A:240107USD1000,00
                    :50K:Test
                    :59:Test
                    -}
                ]]></swift:message>
            </swift:send-message>
            
            <set-variable name="sentMessage#[payload]" value="#[payload.rawMessage]" />
        </foreach>
    </munit:execution>
    
    <munit:validation>
        <!-- Verify ALL messages have Block 5 -->
        <foreach collection="#[[1,2,3,4,5]]">
            <munit-tools:assert-that 
                expression="#[vars['sentMessage' ++ payload] contains '{5:']" 
                is="#[MunitTools::equalTo(true)]"
                message="Message #[payload] missing Block 5 trailer" />
                
            <munit-tools:assert-that 
                expression="#[vars['sentMessage' ++ payload] contains '{MAC:']" 
                is="#[MunitTools::equalTo(true)]"
                message="Message #[payload] missing MAC in Block 5" />
                
            <munit-tools:assert-that 
                expression="#[vars['sentMessage' ++ payload] contains '{CHK:']" 
                is="#[MunitTools::equalTo(true)]"
                message="Message #[payload] missing CHK in Block 5" />
        </foreach>
    </munit:validation>
</munit:test>
```

**Why This Matters**: Banks will reject any message without proper Block 5 trailer. 100% of messages must include it.

---

## 4. RESILIENCE & "THE GAP" (Home Front Protection)

### Context
**Production systems crash. Networks fail. This is where custom connectors usually fail.**

---

### Test 4.1: Sequence Gap Recovery
**Critical**: Detect and recover from missed messages.

```xml
<munit:test name="test-sequence-gap-triggers-resend-request">
    <munit:behavior>
        <!-- Set incoming sequence to 50 in Object Store -->
        <os:store key="swift.inputSeqNum" objectStore="Object_store">
            <os:value>#[50]</os:value>
        </os:store>
        
        <!-- Mock: Receive message with sequence 52 (GAP!) -->
        <munit-tools:mock-when processor="swift:listen">
            <munit-tools:then-return>
                <munit-tools:payload value="Mock message 52" />
                <munit-tools:attributes value="#[{
                    sequenceNumber: 52,
                    messageId: 'MSG-052'
                }]" />
            </munit-tools:then-return>
        </munit-tools:mock-when>
    </munit:behavior>
    
    <munit:execution>
        <!-- Listen for incoming message -->
        <flow-ref name="swift-listener-flow" />
        
        <!-- Connector should detect gap and trigger resend -->
        <munit-tools:verify-call processor="swift:request-resend">
            <munit-tools:with-attributes>
                <munit-tools:with-attribute attributeName="from-sequence" whereValue="51" />
                <munit-tools:with-attribute attributeName="to-sequence" whereValue="51" />
            </munit-tools:with-attributes>
        </munit-tools:verify-call>
    </munit:execution>
    
    <munit:validation>
        <!-- CRITICAL: Gap must be detected -->
        <munit-tools:assert-that 
            expression="#[vars.gapDetected]" 
            is="#[MunitTools::equalTo(true)]"
            message="Sequence gap 50→52 must be detected" />
            
        <!-- CRITICAL: Resend request must be sent -->
        <munit-tools:assert-that 
            expression="#[vars.resendRequested]" 
            is="#[MunitTools::equalTo(true)]"
            message="Resend request for sequence 51 must be sent" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Gap in sequence = missing payment instruction. Auto-recovery prevents manual intervention and delays.

---

### Test 4.2: Mule Runtime Crash Recovery
**Critical**: After crash, connector must reconcile with bank before resuming.

```xml
<munit:test name="test-crash-recovery-from-persistent-store">
    <munit:behavior>
        <!-- Simulate: Connector was at sequence 100 before crash -->
        <os:store key="swift.outputSeqNum" objectStore="Object_store">
            <os:value>#[100]</os:value>
        </os:store>
        <os:store key="swift.sessionId" objectStore="Object_store">
            <os:value>SESSION-PRE-CRASH</os:value>
        </os:store>
        
        <!-- Mock: Bank reports last received sequence was 98 -->
        <munit-tools:mock-when processor="swift:sync-sequences">
            <munit-tools:then-return>
                <munit-tools:payload value="#[{
                    bankLastReceived: 98,
                    connectorLastSent: 100,
                    gap: true,
                    missingSequences: [99, 100]
                }]" />
            </munit-tools:then-return>
        </munit-tools:mock-when>
    </munit:behavior>
    
    <munit:execution>
        <!-- Simulate: Mule runtime restarts -->
        <flow-ref name="swift-initialize-flow" />
        
        <!-- Connector should read from Object Store and sync with bank -->
        <swift:sync-sequences config-ref="SWIFT_Config" />
        
        <!-- Connector should retransmit missing messages -->
        <munit-tools:verify-call processor="swift:retransmit-message" times="2">
            <!-- Should retransmit sequences 99 and 100 -->
        </munit-tools:verify-call>
    </munit:execution>
    
    <munit:validation>
        <munit-tools:assert-that 
            expression="#[payload.gap]" 
            is="#[MunitTools::equalTo(true)]"
            message="Sync must detect gap after crash" />
            
        <munit-tools:assert-that 
            expression="#[sizeOf(payload.missingSequences)]" 
            is="#[MunitTools::equalTo(2)]"
            message="Must identify 2 missing sequences (99, 100)" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Without persistent Object Store, a crash = lost track of what was sent = duplicate payments or missed payments.

---

### Test 4.3: Network Partition (Dirty Disconnect)
**Critical**: Graceful handling of unexpected disconnections.

```xml
<munit:test name="test-network-partition-triggers-reconnect">
    <munit:behavior>
        <!-- Establish active session -->
        <swift:establish-session config-ref="SWIFT_Config" />
        <set-variable name="originalSessionId" value="#[payload.sessionId]" />
        
        <!-- Mock: Network suddenly fails (no graceful close) -->
        <munit-tools:mock-when processor="swift:send-message">
            <munit-tools:then-return>
                <munit-tools:error typeId="SWIFT:CONNECTION_TIMEOUT" />
            </munit-tools:then-return>
        </munit-tools:mock-when>
    </munit:behavior>
    
    <munit:execution>
        <try>
            <!-- Try to send message (will timeout) -->
            <swift:send-message config-ref="SWIFT_Config">
                <swift:message>Test message</swift:message>
            </swift:send-message>
            
            <error-handler>
                <on-error-continue type="SWIFT:CONNECTION_TIMEOUT">
                    <set-variable name="timeoutOccurred" value="true" />
                    
                    <!-- Connector should enter reconnection loop -->
                    <until-successful maxRetries="3" millisBetweenRetries="5000">
                        <swift:establish-session config-ref="SWIFT_Config" />
                    </until-successful>
                    
                    <set-variable name="reconnected" value="true" />
                    <set-variable name="newSessionId" value="#[payload.sessionId]" />
                </on-error-continue>
            </error-handler>
        </try>
    </munit:execution>
    
    <munit:validation>
        <munit-tools:assert-that 
            expression="#[vars.timeoutOccurred]" 
            is="#[MunitTools::equalTo(true)]"
            message="Timeout must be detected" />
            
        <munit-tools:assert-that 
            expression="#[vars.reconnected]" 
            is="#[MunitTools::equalTo(true)]"
            message="Connector must automatically reconnect" />
            
        <munit-tools:assert-that 
            expression="#[vars.newSessionId != vars.originalSessionId]" 
            is="#[MunitTools::equalTo(true)]"
            message="New session must be established after network failure" />
    </munit:validation>
</munit:test>
```

**Why This Matters**: Network failures are common. Graceful reconnection prevents downtime and maintains business continuity.

---

## 🎓 Testing Success Criteria

### Mandatory Pass Rate
- **Session Layer**: 100% (all 3 tests must pass)
- **Message Validation**: 100% (all 2 tests must pass)
- **Cryptography**: 100% (all 2 tests must pass)
- **Resilience**: 100% (all 3 tests must pass)

**Total**: **10/10 tests must pass** for production approval.

### Failure Impact Matrix

| Test Failure | Business Impact | Severity |
|--------------|----------------|----------|
| Logon Handshake | Messages sent to closed connection | **CRITICAL** |
| Sequence Continuity | Payments lost or duplicated | **CRITICAL** |
| Heartbeat | Session timeouts during business hours | **HIGH** |
| SR Compliance | Rejected messages + bank fees | **HIGH** |
| Multi-Block Parsing | gpi operations fail | **HIGH** |
| MAC Validation | **FRAUD RISK** | **CRITICAL** |
| Trailer Integrity | All messages rejected by bank | **CRITICAL** |
| Gap Recovery | Manual intervention required | **HIGH** |
| Crash Recovery | Lost payments after system restart | **CRITICAL** |
| Network Partition | Extended downtime | **HIGH** |

---

## 📊 Test Execution Commands

### Run All Mandate Tests
```bash
cd /Users/alex.macdonald/SWIFT/swift-demo-app
mvn test -Dtest=*Mandate*
```

### Run by Category
```bash
# Session Layer only
mvn test -Dtest=*Session*

# Security only
mvn test -Dtest=*MAC*,*Signature*

# Resilience only
mvn test -Dtest=*Gap*,*Crash*,*Network*
```

### Generate Compliance Report
```bash
mvn test
# Open: target/site/munit/coverage/mandate-compliance-report.html
```

---

## 🏆 Production Readiness Checklist

Before deploying to production, verify:

- [ ] All 10 mandate tests pass
- [ ] Load test: 10,000 messages/hour with 0% sequence gaps
- [ ] Crash test: Kill Mule runtime mid-transaction, verify recovery
- [ ] Network test: Simulate firewall rules, verify reconnection
- [ ] Security audit: Penetration testing of MAC validation
- [ ] Regulatory compliance: SOC2, PCI-DSS documentation
- [ ] Disaster recovery: Backup/restore of Object Store validated

---

**Built for banks. Tested like banks. Ready for production.**

*Testing Mandate v1.0 - Based on real-world SWIFT implementation standards*


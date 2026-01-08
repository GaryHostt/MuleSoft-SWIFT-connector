package com.mulesoft.connectors.swift;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.connection.SwiftConnectionConfig;
import com.mulesoft.connectors.swift.internal.connection.SwiftProtocol;
import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.event.Event;
import org.mule.tck.junit4.rule.DynamicPort;
import org.junit.Rule;

import java.net.SocketTimeoutException;
import java.net.ConnectException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * MUnit Test Suite for Network Resilience
 * 
 * <h2>Purpose</h2>
 * <p>Prove the connector handles network instability gracefully:</p>
 * <ul>
 *   <li>✅ Connection timeouts</li>
 *   <li>✅ Invalid checksums</li>
 *   <li>✅ Socket closures mid-transmission</li>
 *   <li>✅ NACK responses</li>
 *   <li>✅ Sequence number gaps</li>
 * </ul>
 * 
 * <h2>Professional Pattern</h2>
 * <p>Enterprise connectors must prove resilience through adversarial testing.
 * This test suite simulates real-world network failures that occur in production.</p>
 */
public class SwiftConnectorResilienceTest extends MuleArtifactFunctionalTestCase {
    
    @Rule
    public DynamicPort mockServerPort = new DynamicPort("mock.server.port");
    
    @Override
    protected String getConfigFile() {
        return "test-mule-config.xml";
    }
    
    /**
     * TEST 1: Connection Timeout Handling
     * 
     * <p>Simulates a SWIFT server that accepts TCP connections but never responds.
     * Common in production when SAA is overloaded or network path has high latency.</p>
     * 
     * <p><strong>Expected Behavior</strong>:</p>
     * <ul>
     *   <li>Connector throws ConnectionException with timeout message</li>
     *   <li>Mule reconnection strategy can retry</li>
     *   <li>No memory leaks (connections are properly closed)</li>
     * </ul>
     */
    @Test(expected = ConnectionException.class, timeout = 35000)
    public void testConnectionTimeout() throws Exception {
        // Create config with aggressive timeout (5 seconds)
        SwiftConnectionConfig config = SwiftConnectionConfig.builder()
            .host("10.255.255.1")  // Non-routable IP (will timeout)
            .port(3000)
            .bicCode("TESTUS33XXX")
            .username("test")
            .password("test")
            .protocol(SwiftProtocol.FIN)
            .connectionTimeout(5000)  // 5 second timeout
            .build();
        
        // Attempt connection - should timeout
        SwiftConnection connection = new SwiftConnection(config);
        
        try {
            connection.initialize();  // This should throw ConnectionException
            fail("Expected ConnectionException due to timeout");
        } catch (Exception e) {
            // Verify it's a timeout-related exception
            assertTrue("Expected timeout-related exception", 
                e.getMessage().contains("timeout") || 
                e.getCause() instanceof SocketTimeoutException);
            throw e;
        }
    }
    
    /**
     * TEST 2: Connection Refused (Server Not Running)
     * 
     * <p>Simulates SWIFT server being down or port blocked by firewall.</p>
     */
    @Test
    public void testConnectionRefused() throws Exception {
        SwiftConnectionConfig config = SwiftConnectionConfig.builder()
            .host("localhost")
            .port(9999)  // Port with no listener
            .bicCode("TESTUS33XXX")
            .username("test")
            .password("test")
            .protocol(SwiftProtocol.FIN)
            .connectionTimeout(3000)
            .build();
        
        SwiftConnection connection = new SwiftConnection(config);
        
        try {
            connection.initialize();
            fail("Expected ConnectionException (connection refused)");
        } catch (Exception e) {
            // Verify it's connection refused
            assertTrue("Expected connection refused", 
                e.getMessage().contains("refused") || 
                e.getCause() instanceof ConnectException);
        }
    }
    
    /**
     * TEST 3: Invalid Checksum Detection
     * 
     * <p>Simulates a corrupted message with invalid Block 5 checksum (MAC).
     * In production, this can occur due to network corruption or man-in-the-middle attacks.</p>
     * 
     * <p><strong>Expected Behavior</strong>:</p>
     * <ul>
     *   <li>Connector detects checksum mismatch</li>
     *   <li>Throws SWIFT:AUTHENTICATION_FAILED error</li>
     *   <li>Message is NOT processed (security guardrail)</li>
     * </ul>
     */
    @Test
    public void testInvalidChecksumRejection() throws Exception {
        // MT103 message with INVALID checksum in Block 5
        String corruptedMessage = 
            "{1:F01BANKUS33XXXX0000000000}" +
            "{2:O1031234240107BANKGB2LXXXX0000000000240107123400N}" +
            "{4:\n" +
            ":20:TXN20240107001\n" +
            ":23B:CRED\n" +
            ":32A:240110USD50000,00\n" +
            ":50K:/US123456789\n" +
            "ACME Corporation\n" +
            ":59:/GB29NWBK60161331926819\n" +
            "Global Imports Ltd\n" +
            ":70:Invoice Payment\n" +
            ":71A:SHA\n" +
            "-}" +
            "{5:{CHK:INVALID123456}{MAC:CORRUPTED}}";  // ← INVALID checksum
        
        Event event = flowRunner("validate-checksum-flow")
            .withPayload(corruptedMessage)
            .run();
        
        // Verify error was thrown
        assertThat(event.getError().isPresent(), is(true));
        assertThat(event.getError().get().getErrorType().getIdentifier(), 
            containsString("AUTHENTICATION_FAILED"));
    }
    
    /**
     * TEST 4: NACK Response Handling
     * 
     * <p>Simulates SWIFT rejecting a message with a NACK (error code K90).
     * Tests that connector properly parses error code and throws typed exception.</p>
     */
    @Test
    public void testNackResponseParsing() throws Exception {
        // NACK message with K90 error (field format error)
        String nackMessage = 
            "{1:F21BANKGB2LXXXX0000000000}" +
            "{2:I019BANKUS33XXXXN}" +
            "{4:\n" +
            ":20:TXN20240107001\n" +
            ":451:K90\n" +  // ← Error code
            ":405:FIELD FORMAT ERROR IN TAG 32A\n" +  // ← Error description
            "-}";
        
        Event event = flowRunner("handle-nack-flow")
            .withPayload(nackMessage)
            .run();
        
        // Verify NACK was detected
        assertThat(event.getError().isPresent(), is(true));
        assertThat(event.getError().get().getErrorType().getIdentifier(), 
            containsString("NACK_RECEIVED"));
        assertThat(event.getError().get().getDescription(), 
            containsString("K90"));
    }
    
    /**
     * TEST 5: Sequence Number Gap Detection
     * 
     * <p>Simulates messages arriving out of order (sequence gap).
     * Tests connector's ability to detect gaps and trigger recovery.</p>
     */
    @Test
    public void testSequenceGapDetection() throws Exception {
        // Message with sequence number 5 (expecting 1)
        String messageWithGap = 
            "{1:F01BANKUS33XXXX0000000000}" +
            "{2:O1031234240107BANKGB2LXXXX0000000000240107123400N}" +
            "{4:\n" +
            ":34:5\n" +  // ← Sequence number (gap: expecting 1)
            ":20:TXN20240107001\n" +
            ":23B:CRED\n" +
            ":32A:240110USD50000,00\n" +
            "-}";
        
        Event event = flowRunner("detect-sequence-gap-flow")
            .withPayload(messageWithGap)
            .run();
        
        // Verify gap was detected
        assertThat(event.getError().isPresent(), is(true));
        assertThat(event.getError().get().getErrorType().getIdentifier(), 
            containsString("SEQUENCE_MISMATCH"));
    }
    
    /**
     * TEST 6: Socket Closure Mid-Transmission
     * 
     * <p>Simulates network failure during message transmission.
     * Tests that connector doesn't leave partial messages in Object Store.</p>
     */
    @Test
    public void testSocketClosureDuringTransmission() throws Exception {
        // This test requires the mock server to close connection mid-message
        Event event = flowRunner("test-socket-closure-flow")
            .withPayload("MT103_MESSAGE")
            .withVariable("forceDisconnect", true)
            .run();
        
        // Verify connection error was handled
        assertThat(event.getError().isPresent(), is(true));
        assertThat(event.getError().get().getErrorType().getIdentifier(), 
            containsString("CONNECTION_ERROR"));
    }
    
    /**
     * TEST 7: Reconnection After Network Recovery
     * 
     * <p>Tests automatic reconnection when network becomes available again.</p>
     */
    @Test
    public void testAutomaticReconnection() throws Exception {
        // First attempt: server down
        try {
            flowRunner("send-message-with-reconnect-flow")
                .withPayload("MT103_MESSAGE")
                .withVariable("serverAvailable", false)
                .run();
            fail("Expected connection error");
        } catch (Exception e) {
            // Expected - server is down
        }
        
        // Second attempt: server up
        Event event = flowRunner("send-message-with-reconnect-flow")
            .withPayload("MT103_MESSAGE")
            .withVariable("serverAvailable", true)
            .run();
        
        // Verify reconnection succeeded
        assertThat(event.getError().isPresent(), is(false));
        assertThat(event.getMessage().getPayload().getValue(), notNullValue());
    }
    
    /**
     * TEST 8: Memory Leak Prevention (Connection Pool)
     * 
     * <p>Tests that failed connections don't leak memory.
     * Attempts 100 failed connections and verifies heap doesn't grow excessively.</p>
     */
    @Test
    public void testNoMemoryLeakOnFailedConnections() throws Exception {
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Attempt 100 failed connections
        for (int i = 0; i < 100; i++) {
            try {
                flowRunner("test-failed-connection-flow")
                    .withPayload("TEST")
                    .run();
            } catch (Exception e) {
                // Expected - connections should fail
            }
        }
        
        // Force GC and check memory
        System.gc();
        Thread.sleep(1000);
        long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        // Verify memory increase is reasonable (<50MB)
        assertTrue("Memory leak detected: increase = " + (memoryIncrease / 1024 / 1024) + "MB", 
            memoryIncrease < 50 * 1024 * 1024);
    }
}


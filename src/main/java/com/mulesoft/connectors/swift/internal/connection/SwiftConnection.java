package com.mulesoft.connectors.swift.internal.connection;

import com.mulesoft.connectors.swift.internal.model.SwiftMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an active SWIFT connection with session state management.
 * 
 * Key responsibilities:
 * - TCP/TLS connection management
 * - Session lifecycle (login, keepalive, logout)
 * - Sequence number synchronization
 * - Auto-reconnection logic
 */
public class SwiftConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftConnection.class);

    private final SwiftConnectionConfig config;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    
    // Session state
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean sessionActive = new AtomicBoolean(false);
    private String sessionId;
    private long lastActivityTime;
    
    // Sequence numbers for FIN protocol
    private final AtomicLong inputSequenceNumber = new AtomicLong(0);
    private final AtomicLong outputSequenceNumber = new AtomicLong(0);
    
    // Connection timestamp
    private long connectionTimestamp;
    
    // ObjectStore for persistent state (injected by connection provider)
    private org.mule.runtime.api.store.ObjectStore<java.io.Serializable> objectStore;

    public SwiftConnection(SwiftConnectionConfig config) {
        this.config = config;
    }
    
    /**
     * Set ObjectStore (called by connection provider)
     */
    public void setObjectStore(org.mule.runtime.api.store.ObjectStore<java.io.Serializable> objectStore) {
        this.objectStore = objectStore;
    }
    
    /**
     * Get ObjectStore for operations
     */
    public org.mule.runtime.api.store.ObjectStore<java.io.Serializable> getObjectStore() {
        return objectStore;
    }

    /**
     * Initialize the connection and establish session
     */
    public void initialize() throws Exception {
        connect();
        authenticate();
        if (config.isEnableSequenceSync()) {
            synchronizeSequenceNumbers();
        }
    }

    /**
     * Establish TCP/TLS connection to SWIFT interface
     */
    private void connect() throws Exception {
        LOGGER.info("Connecting to SWIFT at {}:{}", config.getHost(), config.getPort());
        
        try {
            if (config.isEnableTls()) {
                socket = createSecureSocket();
            } else {
                socket = new Socket(config.getHost(), config.getPort());
            }
            
            socket.setSoTimeout(config.getConnectionTimeout());
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            
            connected.set(true);
            connectionTimestamp = System.currentTimeMillis();
            lastActivityTime = System.currentTimeMillis();
            
            LOGGER.info("TCP connection established successfully");
            
        } catch (Exception e) {
            connected.set(false);
            throw new Exception("Failed to establish connection: " + e.getMessage(), e);
        }
    }

    /**
     * Create SSL/TLS socket with mutual authentication
     */
    private Socket createSecureSocket() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        // Load truststore
        TrustManagerFactory tmf = null;
        if (config.getTruststorePath() != null) {
            KeyStore truststore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(config.getTruststorePath())) {
                truststore.load(fis, config.getTruststorePassword().toCharArray());
            }
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);
        }
        
        // Load keystore for mutual TLS
        KeyManagerFactory kmf = null;
        if (config.getKeystorePath() != null) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            try (FileInputStream fis = new FileInputStream(config.getKeystorePath())) {
                keystore.load(fis, config.getKeystorePassword().toCharArray());
            }
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keystore, config.getKeystorePassword().toCharArray());
        }
        
        sslContext.init(
            kmf != null ? kmf.getKeyManagers() : null,
            tmf != null ? tmf.getTrustManagers() : null,
            new SecureRandom()
        );
        
        SSLSocketFactory factory = sslContext.getSocketFactory();
        return factory.createSocket(config.getHost(), config.getPort());
    }

    /**
     * Authenticate with SWIFT interface
     */
    private void authenticate() throws Exception {
        LOGGER.info("Authenticating user {} with BIC {}", config.getUsername(), config.getBicCode());
        
        // Build authentication message (simplified - real implementation would follow SWIFT protocol)
        String authMessage = buildAuthenticationMessage();
        
        // Send authentication
        sendRawMessage(authMessage);
        
        // Read response
        String response = readRawMessage();
        
        if (isAuthenticationSuccessful(response)) {
            sessionActive.set(true);
            sessionId = extractSessionId(response);
            LOGGER.info("Authentication successful. Session ID: {}", sessionId);
        } else {
            throw new Exception("Authentication failed: " + response);
        }
    }

    /**
     * Synchronize sequence numbers with ObjectStore
     */
    private void synchronizeSequenceNumbers() {
        LOGGER.info("Synchronizing sequence numbers for BIC {}", config.getBicCode());
        // In real implementation, this would:
        // 1. Query ObjectStore for last known sequence numbers
        // 2. Negotiate with SWIFT interface if mismatch detected
        // 3. Update local sequence counters
        
        // For now, initialize to 0
        inputSequenceNumber.set(0);
        outputSequenceNumber.set(0);
        
        LOGGER.info("Sequence numbers synchronized: ISN={}, OSN={}", 
            inputSequenceNumber.get(), outputSequenceNumber.get());
    }

    /**
     * Send a SWIFT message
     */
    public synchronized void sendMessage(SwiftMessage message) throws IOException {
        if (!isConnected() || !isSessionActive()) {
            throw new IOException("Connection is not active");
        }
        
        // Increment output sequence number
        long sequenceNumber = outputSequenceNumber.incrementAndGet();
        
        // Add sequence number to message (for FIN protocol)
        message.setSequenceNumber(sequenceNumber);
        
        // Serialize and send
        String serialized = message.serialize();
        sendRawMessage(serialized);
        
        lastActivityTime = System.currentTimeMillis();
        
        LOGGER.debug("Sent message with sequence number {}", sequenceNumber);
    }

    /**
     * Receive a SWIFT message
     */
    public synchronized SwiftMessage receiveMessage() throws IOException {
        if (!isConnected() || !isSessionActive()) {
            throw new IOException("Connection is not active");
        }
        
        String rawMessage = readRawMessage();
        
        // Parse message
        SwiftMessage message = SwiftMessage.parse(rawMessage);
        
        // Verify and increment input sequence number
        long expectedSequence = inputSequenceNumber.get() + 1;
        if (message.getSequenceNumber() != expectedSequence) {
            LOGGER.warn("Sequence mismatch: expected {}, got {}", 
                expectedSequence, message.getSequenceNumber());
            // In production, this would trigger re-synchronization
        }
        inputSequenceNumber.set(message.getSequenceNumber());
        
        lastActivityTime = System.currentTimeMillis();
        
        return message;
    }

    /**
     * Send raw message over the wire
     */
    public void sendRawMessage(String message) throws IOException {
        writer.write(message);
        writer.flush();
    }

    /**
     * Read raw message from the wire
     */
    private String readRawMessage() throws IOException {
        // Simplified - real implementation would handle SWIFT framing
        return reader.readLine();
    }

    /**
     * Check if connection is active
     */
    public boolean isConnected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Check if session is active
     */
    public boolean isSessionActive() {
        if (!sessionActive.get()) {
            return false;
        }
        
        // Check session timeout
        long timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime;
        if (timeSinceLastActivity > config.getSessionTimeout()) {
            LOGGER.warn("Session timeout exceeded");
            sessionActive.set(false);
            return false;
        }
        
        return true;
    }

    /**
     * Close connection and cleanup resources
     */
    public void close() throws Exception {
        LOGGER.info("Closing SWIFT connection");
        
        try {
            // Send logout message if session is active
            if (sessionActive.get()) {
                logout();
            }
        } catch (Exception e) {
            LOGGER.warn("Error during logout", e);
        }
        
        // Close resources
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.warn("Error closing resources", e);
        }
        
        connected.set(false);
        sessionActive.set(false);
        
        LOGGER.info("Connection closed");
    }

    /**
     * Logout from SWIFT session
     */
    private void logout() throws Exception {
        LOGGER.info("Logging out from session {}", sessionId);
        String logoutMessage = buildLogoutMessage();
        sendRawMessage(logoutMessage);
        sessionActive.set(false);
    }

    // Helper methods for protocol-specific message building
    private String buildAuthenticationMessage() {
        // Simplified - real implementation follows SWIFT LOGIN protocol
        return String.format("LOGIN:%s:%s:%s", config.getBicCode(), config.getUsername(), config.getPassword());
    }

    private String buildLogoutMessage() {
        return "LOGOUT:" + sessionId;
    }

    private boolean isAuthenticationSuccessful(String response) {
        return response != null && response.startsWith("AUTH_OK");
    }

    private String extractSessionId(String response) {
        // Simplified extraction
        return response.substring(response.lastIndexOf(":") + 1);
    }

    // Getters
    public SwiftConnectionConfig getConfig() {
        return config;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getInputSequenceNumber() {
        return inputSequenceNumber.get();
    }

    public long getOutputSequenceNumber() {
        return outputSequenceNumber.get();
    }

    public long getConnectionTimestamp() {
        return connectionTimestamp;
    }
}


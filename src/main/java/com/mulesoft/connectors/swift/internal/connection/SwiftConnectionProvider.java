package com.mulesoft.connectors.swift.internal.connection;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Password;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ FIXED: SWIFT Connection Provider with proper validation
 * 
 * Manages stateful connections to SWIFT Alliance Access (SAA) or Service Bureau.
 * Handles session lifecycle, sequence synchronization, and auto-reconnection.
 * 
 * Implements PoolingConnectionProvider which includes built-in @ConnectionValidator
 */
@Alias("connection")
@DisplayName("SWIFT Connection")
public class SwiftConnectionProvider implements PoolingConnectionProvider<SwiftConnection> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftConnectionProvider.class);

    // Connection Parameters
    @Parameter
    @DisplayName("Host")
    @Summary("SWIFT Alliance Access (SAA) or Service Bureau hostname")
    @Placement(order = 1)
    private String host;

    @Parameter
    @DisplayName("Port")
    @Summary("SWIFT interface port (typically 3000-3999 for FIN)")
    @Placement(order = 2)
    @Optional(defaultValue = "3000")
    private int port;

    @Parameter
    @DisplayName("BIC Code")
    @Summary("Your institution's 8 or 11-character Bank Identifier Code")
    @Placement(order = 3)
    private String bicCode;

    @Parameter
    @DisplayName("Username")
    @Summary("SWIFT interface username")
    @Placement(order = 4)
    private String username;

    @Parameter
    @Password
    @DisplayName("Password")
    @Summary("SWIFT interface password")
    @Placement(order = 5)
    private String password;

    @Parameter
    @DisplayName("Protocol")
    @Summary("SWIFT messaging protocol")
    @Placement(tab = "Advanced", order = 1)
    @Optional(defaultValue = "FIN")
    private SwiftProtocol protocol;

    @Parameter
    @DisplayName("Connection Timeout")
    @Summary("Connection timeout in milliseconds")
    @Placement(tab = "Advanced", order = 2)
    @Optional(defaultValue = "30000")
    private int connectionTimeout;

    @Parameter
    @DisplayName("Session Timeout")
    @Summary("Session timeout in milliseconds")
    @Placement(tab = "Advanced", order = 3)
    @Optional(defaultValue = "300000")
    private int sessionTimeout;

    @Parameter
    @DisplayName("Enable Auto Reconnect")
    @Summary("Automatically reconnect on session failure")
    @Placement(tab = "Advanced", order = 4)
    @Optional(defaultValue = "true")
    private boolean autoReconnect;

    @Parameter
    @DisplayName("Enable Sequence Sync")
    @Summary("Synchronize Input/Output sequence numbers via ObjectStore")
    @Placement(tab = "Advanced", order = 5)
    @Optional(defaultValue = "true")
    private boolean enableSequenceSync;

    // TLS/SSL Parameters
    @Parameter
    @DisplayName("Enable TLS")
    @Summary("Enable TLS/SSL encryption")
    @Placement(tab = "Security", order = 1)
    @Optional(defaultValue = "true")
    private boolean enableTls;

    @Parameter
    @DisplayName("Truststore Path")
    @Summary("Path to truststore file (JKS or PKCS12)")
    @Placement(tab = "Security", order = 2)
    @Optional
    private String truststorePath;

    @Parameter
    @Password
    @DisplayName("Truststore Password")
    @Summary("Truststore password")
    @Placement(tab = "Security", order = 3)
    @Optional
    private String truststorePassword;

    @Parameter
    @DisplayName("Keystore Path")
    @Summary("Path to keystore file for mutual TLS")
    @Placement(tab = "Security", order = 4)
    @Optional
    private String keystorePath;

    @Parameter
    @Password
    @DisplayName("Keystore Password")
    @Summary("Keystore password")
    @Placement(tab = "Security", order = 5)
    @Optional
    private String keystorePassword;

    @Parameter
    @DisplayName("Certificate Alias")
    @Summary("Certificate alias in keystore")
    @Placement(tab = "Security", order = 6)
    @Optional
    private String certificateAlias;

    @Override
    public SwiftConnection connect() throws ConnectionException {
        LOGGER.info("Establishing SWIFT connection to {}:{} using protocol {}", host, port, protocol);
        
        try {
            SwiftConnectionConfig config = SwiftConnectionConfig.builder()
                .host(host)
                .port(port)
                .bicCode(bicCode)
                .username(username)
                .password(password)
                .protocol(protocol)
                .connectionTimeout(connectionTimeout)
                .sessionTimeout(sessionTimeout)
                .autoReconnect(autoReconnect)
                .enableSequenceSync(enableSequenceSync)
                .enableTls(enableTls)
                .truststorePath(truststorePath)
                .truststorePassword(truststorePassword)
                .keystorePath(keystorePath)
                .keystorePassword(keystorePassword)
                .certificateAlias(certificateAlias)
                .build();

            SwiftConnection connection = new SwiftConnection(config);
            connection.initialize();
            
            LOGGER.info("SWIFT connection established successfully");
            return connection;
            
        } catch (Exception e) {
            LOGGER.error("Failed to establish SWIFT connection", e);
            throw new ConnectionException("Failed to connect to SWIFT: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect(SwiftConnection connection) {
        LOGGER.info("Disconnecting SWIFT connection");
        try {
            connection.close();
            LOGGER.info("SWIFT connection closed successfully");
        } catch (Exception e) {
            LOGGER.error("Error closing SWIFT connection", e);
        }
    }

    @Override
    public ConnectionValidationResult validate(SwiftConnection connection) {
        // ✅ CRITICAL FIX: Real validation with ECHO/PING, not just local flags
        try {
            // First check local state
            if (!connection.isConnected() || !connection.isSessionActive()) {
                return ConnectionValidationResult.failure(
                    "Local connection state indicates inactive session",
                    new ConnectionException("Session inactive locally")
                );
            }
            
            // ✅ NOW: Send actual ECHO request to SWIFT network
            LOGGER.debug("Validating connection with ECHO request...");
            String echoResponse = connection.sendEchoRequest();
            
            // Check if response is valid
            if (echoResponse != null && 
                (echoResponse.contains("F01") || echoResponse.contains("ACK") || echoResponse.length() > 0)) {
                LOGGER.debug("✅ Connection validated: received ECHO response");
                return ConnectionValidationResult.success();
            } else {
                LOGGER.warn("⚠️ Connection validation failed: no valid ECHO response");
                return ConnectionValidationResult.failure(
                    "SWIFT session inactive - no echo response from network",
                    new ConnectionException("ECHO request failed")
                );
            }
        } catch (Exception e) {
            LOGGER.error("❌ Connection validation failed with exception", e);
            return ConnectionValidationResult.failure(
                "Connection validation failed: " + e.getMessage(),
                e
            );
        }
    }
}


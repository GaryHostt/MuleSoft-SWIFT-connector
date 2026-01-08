package com.mulesoft.connectors.swift.internal.connection;

/**
 * Configuration object for SWIFT connections.
 * Built using the Builder pattern for immutability and clarity.
 */
public class SwiftConnectionConfig {
    
    private final String host;
    private final int port;
    private final String bicCode;
    private final String username;
    private final String password;
    private final SwiftProtocol protocol;
    private final int connectionTimeout;
    private final int sessionTimeout;
    private final boolean autoReconnect;
    private final boolean enableSequenceSync;
    private final boolean enableTls;
    private final String truststorePath;
    private final String truststorePassword;
    private final String keystorePath;
    private final String keystorePassword;
    private final String certificateAlias;
    private final int streamingThresholdBytes;
    private final int heartbeatInterval;
    private final String messageEncoding; // ← BATTLE-SCARRED: Support legacy EBCDIC/ISO-8859-1

    private SwiftConnectionConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.bicCode = builder.bicCode;
        this.username = builder.username;
        this.password = builder.password;
        this.protocol = builder.protocol;
        this.connectionTimeout = builder.connectionTimeout;
        this.sessionTimeout = builder.sessionTimeout;
        this.autoReconnect = builder.autoReconnect;
        this.enableSequenceSync = builder.enableSequenceSync;
        this.enableTls = builder.enableTls;
        this.truststorePath = builder.truststorePath;
        this.truststorePassword = builder.truststorePassword;
        this.keystorePath = builder.keystorePath;
        this.keystorePassword = builder.keystorePassword;
        this.certificateAlias = builder.certificateAlias;
        this.streamingThresholdBytes = builder.streamingThresholdBytes;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.messageEncoding = builder.messageEncoding;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getBicCode() { return bicCode; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public SwiftProtocol getProtocol() { return protocol; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getSessionTimeout() { return sessionTimeout; }
    public boolean isAutoReconnect() { return autoReconnect; }
    public boolean isEnableSequenceSync() { return enableSequenceSync; }
    public boolean isEnableTls() { return enableTls; }
    public String getTruststorePath() { return truststorePath; }
    public String getTruststorePassword() { return truststorePassword; }
    public String getKeystorePath() { return keystorePath; }
    public String getKeystorePassword() { return keystorePassword; }
    public String getCertificateAlias() { return certificateAlias; }
    public int getStreamingThresholdBytes() { return streamingThresholdBytes; }
    public int getHeartbeatInterval() { return heartbeatInterval; }
    public String getMessageEncoding() { return messageEncoding; }

    public static class Builder {
        private String host;
        private int port;
        private String bicCode;
        private String username;
        private String password;
        private SwiftProtocol protocol;
        private int connectionTimeout;
        private int sessionTimeout;
        private boolean autoReconnect;
        private boolean enableSequenceSync;
        private boolean enableTls;
        private String truststorePath;
        private String truststorePassword;
        private String keystorePath;
        private String keystorePassword;
        private String certificateAlias;
        private int streamingThresholdBytes = 50 * 1024 * 1024; // 50MB default
        private int heartbeatInterval = 60000; // 60 seconds default
        private String messageEncoding = "UTF-8"; // ← DEFAULT: Modern SWIFT uses UTF-8

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder bicCode(String bicCode) {
            this.bicCode = bicCode;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder protocol(SwiftProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder sessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder enableSequenceSync(boolean enableSequenceSync) {
            this.enableSequenceSync = enableSequenceSync;
            return this;
        }

        public Builder enableTls(boolean enableTls) {
            this.enableTls = enableTls;
            return this;
        }

        public Builder truststorePath(String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public Builder truststorePassword(String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

        public Builder keystorePath(String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public Builder keystorePassword(String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public Builder certificateAlias(String certificateAlias) {
            this.certificateAlias = certificateAlias;
            return this;
        }

        public Builder streamingThresholdBytes(int streamingThresholdBytes) {
            this.streamingThresholdBytes = streamingThresholdBytes;
            return this;
        }

        public Builder heartbeatInterval(int heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
            return this;
        }

        public Builder messageEncoding(String messageEncoding) {
            this.messageEncoding = messageEncoding;
            return this;
        }

        public SwiftConnectionConfig build() {
            return new SwiftConnectionConfig(this);
        }
    }
}


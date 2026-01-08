package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Operational metrics response
 */
public class MetricsResponse {
    
    private Long messagesSent;
    private Long messagesReceived;
    private Long messagesFailed;
    private Long averageLatencyMs;
    private Double successRate;
    private LocalDateTime collectionTimestamp;

    public Long getMessagesSent() {
        return messagesSent;
    }

    public void setMessagesSent(Long messagesSent) {
        this.messagesSent = messagesSent;
    }

    public Long getMessagesReceived() {
        return messagesReceived;
    }

    public void setMessagesReceived(Long messagesReceived) {
        this.messagesReceived = messagesReceived;
    }

    public Long getMessagesFailed() {
        return messagesFailed;
    }

    public void setMessagesFailed(Long messagesFailed) {
        this.messagesFailed = messagesFailed;
    }

    public Long getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(Long averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public LocalDateTime getCollectionTimestamp() {
        return collectionTimestamp;
    }

    public void setCollectionTimestamp(LocalDateTime collectionTimestamp) {
        this.collectionTimestamp = collectionTimestamp;
    }
}


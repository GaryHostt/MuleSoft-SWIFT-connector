package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response object for message status queries
 */
public class MessageStatusResponse {
    
    private String messageId;
    private SwiftMessageStatus status;
    private LocalDateTime sentTimestamp;
    private LocalDateTime receivedTimestamp;
    private String lastKnownLocation;
    private String errorDetails;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public SwiftMessageStatus getStatus() {
        return status;
    }

    public void setStatus(SwiftMessageStatus status) {
        this.status = status;
    }

    public LocalDateTime getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(LocalDateTime sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    public LocalDateTime getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(LocalDateTime receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public String getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(String lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}


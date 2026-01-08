package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response object for acknowledgment operations
 */
public class AcknowledgmentResponse {
    
    private String messageId;
    private AcknowledgmentType acknowledgmentType;
    private String reasonCode;
    private LocalDateTime timestamp;
    private boolean success;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public AcknowledgmentType getAcknowledgmentType() {
        return acknowledgmentType;
    }

    public void setAcknowledgmentType(AcknowledgmentType acknowledgmentType) {
        this.acknowledgmentType = acknowledgmentType;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}


package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Attributes attached to message results
 */
public class MessageAttributes {
    
    private String messageId;
    private long sequenceNumber;
    private LocalDateTime timestamp;
    private SwiftMessageStatus status;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public SwiftMessageStatus getStatus() {
        return status;
    }

    public void setStatus(SwiftMessageStatus status) {
        this.status = status;
    }
}


package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a SWIFT message (MT or MX format).
 * 
 * This is the core domain object that flows through the connector.
 */
public class SwiftMessage {

    private String messageId;
    private String messageType; // e.g., "MT103", "pacs.008"
    private MessageFormat format; // MT or MX
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;
    private long sequenceNumber;
    private MessagePriority priority;
    private Map<String, String> headers;
    private SwiftMessageStatus status;

    public SwiftMessage() {
        this.headers = new HashMap<>();
        this.timestamp = LocalDateTime.now();
        this.priority = MessagePriority.NORMAL;
        this.status = SwiftMessageStatus.CREATED;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public MessageFormat getFormat() {
        return format;
    }

    public void setFormat(MessageFormat format) {
        this.format = format;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public MessagePriority getPriority() {
        return priority;
    }

    public void setPriority(MessagePriority priority) {
        this.priority = priority;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public SwiftMessageStatus getStatus() {
        return status;
    }

    public void setStatus(SwiftMessageStatus status) {
        this.status = status;
    }

    /**
     * Serialize message to wire format
     */
    public String serialize() {
        // Simplified serialization - real implementation would follow SWIFT protocol
        StringBuilder sb = new StringBuilder();
        sb.append("{1:").append(sender).append("}");
        sb.append("{2:").append(messageType).append(receiver).append("}");
        sb.append("{3:").append(sequenceNumber).append("}");
        sb.append("{4:\n").append(content).append("\n}");
        return sb.toString();
    }

    /**
     * Parse message from wire format
     */
    public static SwiftMessage parse(String raw) {
        SwiftMessage message = new SwiftMessage();
        // Simplified parsing - real implementation would follow SWIFT protocol
        message.setContent(raw);
        message.setStatus(SwiftMessageStatus.RECEIVED);
        return message;
    }

    @Override
    public String toString() {
        return "SwiftMessage{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", format=" + format +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", timestamp=" + timestamp +
                ", sequenceNumber=" + sequenceNumber +
                ", priority=" + priority +
                ", status=" + status +
                '}';
    }
}


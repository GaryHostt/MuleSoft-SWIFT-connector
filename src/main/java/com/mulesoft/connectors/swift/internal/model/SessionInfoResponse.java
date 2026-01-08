package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Session information response
 */
public class SessionInfoResponse {
    
    private String sessionId;
    private boolean connected;
    private boolean active;
    private long inputSequenceNumber;
    private long outputSequenceNumber;
    private long connectionTimestamp;
    private LocalDateTime lastGapDetectedTimestamp;
    private LocalDateTime lastResendRequestTimestamp;
    private int totalGapCount;
    private int totalResendCount;
    private int totalDuplicateCount;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getInputSequenceNumber() {
        return inputSequenceNumber;
    }

    public void setInputSequenceNumber(long inputSequenceNumber) {
        this.inputSequenceNumber = inputSequenceNumber;
    }

    public long getOutputSequenceNumber() {
        return outputSequenceNumber;
    }

    public void setOutputSequenceNumber(long outputSequenceNumber) {
        this.outputSequenceNumber = outputSequenceNumber;
    }

    public long getConnectionTimestamp() {
        return connectionTimestamp;
    }

    public void setConnectionTimestamp(long connectionTimestamp) {
        this.connectionTimestamp = connectionTimestamp;
    }

    public LocalDateTime getLastGapDetectedTimestamp() {
        return lastGapDetectedTimestamp;
    }

    public void setLastGapDetectedTimestamp(LocalDateTime lastGapDetectedTimestamp) {
        this.lastGapDetectedTimestamp = lastGapDetectedTimestamp;
    }

    public LocalDateTime getLastResendRequestTimestamp() {
        return lastResendRequestTimestamp;
    }

    public void setLastResendRequestTimestamp(LocalDateTime lastResendRequestTimestamp) {
        this.lastResendRequestTimestamp = lastResendRequestTimestamp;
    }

    public int getTotalGapCount() {
        return totalGapCount;
    }

    public void setTotalGapCount(int totalGapCount) {
        this.totalGapCount = totalGapCount;
    }

    public int getTotalResendCount() {
        return totalResendCount;
    }

    public void setTotalResendCount(int totalResendCount) {
        this.totalResendCount = totalResendCount;
    }

    public int getTotalDuplicateCount() {
        return totalDuplicateCount;
    }

    public void setTotalDuplicateCount(int totalDuplicateCount) {
        this.totalDuplicateCount = totalDuplicateCount;
    }
}


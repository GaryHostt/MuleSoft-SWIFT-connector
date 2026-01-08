package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from sequence synchronization
 */
public class SequenceSyncResponse {
    
    private long inputSequenceNumber;
    private long outputSequenceNumber;
    private boolean synced;
    private LocalDateTime syncTimestamp;
    private boolean synchronizedStatus;
    private boolean gapDetected;
    private List<Long> missingSequenceNumbers;
    private String recoveryAction;

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

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public LocalDateTime getSyncTimestamp() {
        return syncTimestamp;
    }

    public void setSyncTimestamp(LocalDateTime syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    public boolean isSynchronizedStatus() {
        return synchronizedStatus;
    }

    public void setSynchronizedStatus(boolean synchronizedStatus) {
        this.synchronizedStatus = synchronizedStatus;
    }

    public boolean isGapDetected() {
        return gapDetected;
    }

    public void setGapDetected(boolean gapDetected) {
        this.gapDetected = gapDetected;
    }

    public List<Long> getMissingSequenceNumbers() {
        return missingSequenceNumbers;
    }

    public void setMissingSequenceNumbers(List<Long> missingSequenceNumbers) {
        this.missingSequenceNumbers = missingSequenceNumbers;
    }

    public String getRecoveryAction() {
        return recoveryAction;
    }

    public void setRecoveryAction(String recoveryAction) {
        this.recoveryAction = recoveryAction;
    }
}


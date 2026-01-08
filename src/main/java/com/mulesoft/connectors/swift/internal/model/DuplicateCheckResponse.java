package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from duplicate check
 */
public class DuplicateCheckResponse {
    
    private String messageReference;
    private boolean duplicate;
    private LocalDateTime checkTimestamp;
    private LocalDateTime previousSubmissionTimestamp;
    private LocalDateTime firstSeenTimestamp;
    private int duplicateCount;

    public String getMessageReference() {
        return messageReference;
    }

    public void setMessageReference(String messageReference) {
        this.messageReference = messageReference;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public LocalDateTime getCheckTimestamp() {
        return checkTimestamp;
    }

    public void setCheckTimestamp(LocalDateTime checkTimestamp) {
        this.checkTimestamp = checkTimestamp;
    }

    public LocalDateTime getPreviousSubmissionTimestamp() {
        return previousSubmissionTimestamp;
    }

    public void setPreviousSubmissionTimestamp(LocalDateTime previousSubmissionTimestamp) {
        this.previousSubmissionTimestamp = previousSubmissionTimestamp;
    }

    public LocalDateTime getFirstSeenTimestamp() {
        return firstSeenTimestamp;
    }

    public void setFirstSeenTimestamp(LocalDateTime firstSeenTimestamp) {
        this.firstSeenTimestamp = firstSeenTimestamp;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }
}


package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from gpi status update
 */
public class GpiStatusUpdateResponse {
    
    private String uetr;
    private boolean success;
    private GpiPaymentStatus updatedStatus;
    private LocalDateTime updateTimestamp;
    private String errorMessage;

    public String getUetr() {
        return uetr;
    }

    public void setUetr(String uetr) {
        this.uetr = uetr;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public GpiPaymentStatus getUpdatedStatus() {
        return updatedStatus;
    }

    public void setUpdatedStatus(GpiPaymentStatus updatedStatus) {
        this.updatedStatus = updatedStatus;
    }

    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}


package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from payment recall request
 */
public class GpiRecallResponse {
    
    private String uetr;
    private boolean canBeRecalled;
    private String recallStatus;
    private LocalDateTime recallInitiatedTime;
    private LocalDateTime estimatedResponseTime;
    private String declineReason;

    public String getUetr() {
        return uetr;
    }

    public void setUetr(String uetr) {
        this.uetr = uetr;
    }

    public boolean isCanBeRecalled() {
        return canBeRecalled;
    }

    public void setCanBeRecalled(boolean canBeRecalled) {
        this.canBeRecalled = canBeRecalled;
    }

    public String getRecallStatus() {
        return recallStatus;
    }

    public void setRecallStatus(String recallStatus) {
        this.recallStatus = recallStatus;
    }

    public LocalDateTime getRecallInitiatedTime() {
        return recallInitiatedTime;
    }

    public void setRecallInitiatedTime(LocalDateTime recallInitiatedTime) {
        this.recallInitiatedTime = recallInitiatedTime;
    }

    public LocalDateTime getEstimatedResponseTime() {
        return estimatedResponseTime;
    }

    public void setEstimatedResponseTime(LocalDateTime estimatedResponseTime) {
        this.estimatedResponseTime = estimatedResponseTime;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }
}


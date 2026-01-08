package com.mulesoft.connectors.swift.internal.model.gpi;

public class TrackingResponse {
    private String uetr;
    private String status;
    private String transactionStatus;
    private String lastUpdateTime;
    
    public String getUetr() { return uetr; }
    public void setUetr(String uetr) { this.uetr = uetr; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }
    
    public String getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(String lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
}


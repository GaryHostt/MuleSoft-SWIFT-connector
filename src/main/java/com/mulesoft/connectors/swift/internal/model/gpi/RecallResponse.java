package com.mulesoft.connectors.swift.internal.model.gpi;

public class RecallResponse {
    private String uetr;
    private String status;
    private boolean success;
    private String message;
    
    public String getUetr() { return uetr; }
    public void setUetr(String uetr) { this.uetr = uetr; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}


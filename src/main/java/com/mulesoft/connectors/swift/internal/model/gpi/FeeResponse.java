package com.mulesoft.connectors.swift.internal.model.gpi;

public class FeeResponse {
    private String uetr;
    private String totalFees;
    private String currency;
    private String feeBreakdown;
    
    public String getUetr() { return uetr; }
    public void setUetr(String uetr) { this.uetr = uetr; }
    
    public String getTotalFees() { return totalFees; }
    public void setTotalFees(String totalFees) { this.totalFees = totalFees; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getFeeBreakdown() { return feeBreakdown; }
    public void setFeeBreakdown(String feeBreakdown) { this.feeBreakdown = feeBreakdown; }
}


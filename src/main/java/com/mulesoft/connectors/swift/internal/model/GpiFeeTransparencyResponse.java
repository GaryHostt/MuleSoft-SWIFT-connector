package com.mulesoft.connectors.swift.internal.model;

import java.util.List;

/**
 * Response containing fee and FX transparency details
 */
public class GpiFeeTransparencyResponse {
    
    private String uetr;
    private Double originalAmount;
    private String originalCurrency;
    private Double finalAmount;
    private String finalCurrency;
    private Double totalFeesAmount;
    private String totalFeesCurrency;
    private Double exchangeRate;
    private List<GpiFeeDetail> feeDetails;

    public String getUetr() {
        return uetr;
    }

    public void setUetr(String uetr) {
        this.uetr = uetr;
    }

    public Double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(Double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public String getFinalCurrency() {
        return finalCurrency;
    }

    public void setFinalCurrency(String finalCurrency) {
        this.finalCurrency = finalCurrency;
    }

    public Double getTotalFeesAmount() {
        return totalFeesAmount;
    }

    public void setTotalFeesAmount(Double totalFeesAmount) {
        this.totalFeesAmount = totalFeesAmount;
    }

    public String getTotalFeesCurrency() {
        return totalFeesCurrency;
    }

    public void setTotalFeesCurrency(String totalFeesCurrency) {
        this.totalFeesCurrency = totalFeesCurrency;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public List<GpiFeeDetail> getFeeDetails() {
        return feeDetails;
    }

    public void setFeeDetails(List<GpiFeeDetail> feeDetails) {
        this.feeDetails = feeDetails;
    }
}


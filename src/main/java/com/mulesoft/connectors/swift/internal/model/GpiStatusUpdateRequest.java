package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Request to update gpi payment status
 */
public class GpiStatusUpdateRequest {
    
    private String uetr;
    private GpiPaymentStatus status;
    private Double creditedAmount;
    private String creditedCurrency;
    private LocalDateTime updateTimestamp;
    private String updatingInstitution;

    public String getUetr() {
        return uetr;
    }

    public void setUetr(String uetr) {
        this.uetr = uetr;
    }

    public GpiPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(GpiPaymentStatus status) {
        this.status = status;
    }

    public Double getCreditedAmount() {
        return creditedAmount;
    }

    public void setCreditedAmount(Double creditedAmount) {
        this.creditedAmount = creditedAmount;
    }

    public String getCreditedCurrency() {
        return creditedCurrency;
    }

    public void setCreditedCurrency(String creditedCurrency) {
        this.creditedCurrency = creditedCurrency;
    }

    public LocalDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(LocalDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public String getUpdatingInstitution() {
        return updatingInstitution;
    }

    public void setUpdatingInstitution(String updatingInstitution) {
        this.updatingInstitution = updatingInstitution;
    }
}


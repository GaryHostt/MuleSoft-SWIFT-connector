package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Correlation ID response
 */
public class CorrelationIdResponse {
    
    private String correlationId;
    private String businessTransactionId;
    private LocalDateTime generatedTimestamp;
    private String institution;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getBusinessTransactionId() {
        return businessTransactionId;
    }

    public void setBusinessTransactionId(String businessTransactionId) {
        this.businessTransactionId = businessTransactionId;
    }

    public LocalDateTime getGeneratedTimestamp() {
        return generatedTimestamp;
    }

    public void setGeneratedTimestamp(LocalDateTime generatedTimestamp) {
        this.generatedTimestamp = generatedTimestamp;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }
}


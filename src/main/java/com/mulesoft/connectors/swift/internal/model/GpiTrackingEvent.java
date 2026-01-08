package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Individual tracking event in payment chain
 */
public class GpiTrackingEvent {
    
    private String institution;
    private String status;
    private LocalDateTime timestamp;
    private String remarks;

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}


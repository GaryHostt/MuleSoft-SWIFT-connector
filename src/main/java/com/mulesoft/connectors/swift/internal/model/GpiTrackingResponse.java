package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from gpi payment tracking
 */
public class GpiTrackingResponse {
    
    private String uetr;
    private GpiPaymentStatus status;
    private String currentLocation;
    private String originatingBank;
    private String beneficiaryBank;
    private List<GpiTrackingEvent> trackingEvents;
    private LocalDateTime lastUpdateTime;

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

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public String getOriginatingBank() {
        return originatingBank;
    }

    public void setOriginatingBank(String originatingBank) {
        this.originatingBank = originatingBank;
    }

    public String getBeneficiaryBank() {
        return beneficiaryBank;
    }

    public void setBeneficiaryBank(String beneficiaryBank) {
        this.beneficiaryBank = beneficiaryBank;
    }

    public List<GpiTrackingEvent> getTrackingEvents() {
        return trackingEvents;
    }

    public void setTrackingEvents(List<GpiTrackingEvent> trackingEvents) {
        this.trackingEvents = trackingEvents;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}


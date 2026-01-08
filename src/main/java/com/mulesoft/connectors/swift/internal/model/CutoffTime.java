package com.mulesoft.connectors.swift.internal.model;

/**
 * Cutoff time detail
 */
public class CutoffTime {
    
    private String settlementSystem;
    private String cutoffTime;
    private String timeZone;

    public String getSettlementSystem() {
        return settlementSystem;
    }

    public void setSettlementSystem(String settlementSystem) {
        this.settlementSystem = settlementSystem;
    }

    public String getCutoffTime() {
        return cutoffTime;
    }

    public void setCutoffTime(String cutoffTime) {
        this.cutoffTime = cutoffTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}


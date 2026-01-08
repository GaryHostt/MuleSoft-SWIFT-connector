package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from sanctions screening
 */
public class ScreeningResponse {
    
    private boolean passed;
    private int matchCount;
    private String screeningProvider;
    private LocalDateTime screeningTimestamp;
    private Double riskScore;

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public String getScreeningProvider() {
        return screeningProvider;
    }

    public void setScreeningProvider(String screeningProvider) {
        this.screeningProvider = screeningProvider;
    }

    public LocalDateTime getScreeningTimestamp() {
        return screeningTimestamp;
    }

    public void setScreeningTimestamp(LocalDateTime screeningTimestamp) {
        this.screeningTimestamp = screeningTimestamp;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }
}


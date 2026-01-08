package com.mulesoft.connectors.swift.internal.model;

import java.util.List;

/**
 * Cutoff times response
 */
public class CutoffTimesResponse {
    
    private String currencyCode;
    private List<CutoffTime> cutoffTimes;

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public List<CutoffTime> getCutoffTimes() {
        return cutoffTimes;
    }

    public void setCutoffTimes(List<CutoffTime> cutoffTimes) {
        this.cutoffTimes = cutoffTimes;
    }
}


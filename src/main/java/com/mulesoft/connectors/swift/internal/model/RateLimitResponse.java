package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Rate limit response
 */
public class RateLimitResponse {
    
    private int currentRate;
    private int maxRate;
    private int remainingCapacity;
    private LocalDateTime resetTime;
    private boolean throttled;

    public int getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(int currentRate) {
        this.currentRate = currentRate;
    }

    public int getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(int maxRate) {
        this.maxRate = maxRate;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public LocalDateTime getResetTime() {
        return resetTime;
    }

    public void setResetTime(LocalDateTime resetTime) {
        this.resetTime = resetTime;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public void setThrottled(boolean throttled) {
        this.throttled = throttled;
    }
}


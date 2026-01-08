package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * RMA authorization check response
 */
public class RmaCheckResponse {
    
    private String counterpartyBic;
    private String messageType;
    private boolean authorized;
    private LocalDateTime checkTimestamp;
    private String declineReason;

    public String getCounterpartyBic() {
        return counterpartyBic;
    }

    public void setCounterpartyBic(String counterpartyBic) {
        this.counterpartyBic = counterpartyBic;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public LocalDateTime getCheckTimestamp() {
        return checkTimestamp;
    }

    public void setCheckTimestamp(LocalDateTime checkTimestamp) {
        this.checkTimestamp = checkTimestamp;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }
}


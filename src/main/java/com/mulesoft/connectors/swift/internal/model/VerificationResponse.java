package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from signature verification
 */
public class VerificationResponse {
    
    private boolean valid;
    private LocalDateTime verificationTimestamp;
    private String signatureAlgorithm;
    private String errorMessage;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public LocalDateTime getVerificationTimestamp() {
        return verificationTimestamp;
    }

    public void setVerificationTimestamp(LocalDateTime verificationTimestamp) {
        this.verificationTimestamp = verificationTimestamp;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}


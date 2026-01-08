package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Response from message signing
 */
public class SignatureResponse {
    
    private String originalContent;
    private String signature;
    private String signatureAlgorithm;
    private LocalDateTime signingTimestamp;
    private String signerIdentity;

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public LocalDateTime getSigningTimestamp() {
        return signingTimestamp;
    }

    public void setSigningTimestamp(LocalDateTime signingTimestamp) {
        this.signingTimestamp = signingTimestamp;
    }

    public String getSignerIdentity() {
        return signerIdentity;
    }

    public void setSignerIdentity(String signerIdentity) {
        this.signerIdentity = signerIdentity;
    }
}


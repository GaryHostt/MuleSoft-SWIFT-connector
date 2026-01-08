package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Validation response
 */
public class ValidationResponse {
    
    private String messageType;
    private MessageFormat format;
    private String standardRelease;
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private LocalDateTime validationTimestamp;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public MessageFormat getFormat() {
        return format;
    }

    public void setFormat(MessageFormat format) {
        this.format = format;
    }

    public String getStandardRelease() {
        return standardRelease;
    }

    public void setStandardRelease(String standardRelease) {
        this.standardRelease = standardRelease;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public List<ValidationWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ValidationWarning> warnings) {
        this.warnings = warnings;
    }

    public LocalDateTime getValidationTimestamp() {
        return validationTimestamp;
    }

    public void setValidationTimestamp(LocalDateTime validationTimestamp) {
        this.validationTimestamp = validationTimestamp;
    }
}


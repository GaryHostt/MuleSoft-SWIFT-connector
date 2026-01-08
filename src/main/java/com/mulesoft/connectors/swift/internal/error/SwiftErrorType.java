package com.mulesoft.connectors.swift.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * SWIFT-specific error types for granular error handling in Mule flows.
 * These enable precise "On Error Continue" and "On Error Propagate" patterns.
 */
public enum SwiftErrorType implements ErrorTypeDefinition<SwiftErrorType> {
    
    // Connection & Session Errors
    CONNECTION_FAILED(MuleErrors.CONNECTIVITY),
    CONNECTION_ERROR(MuleErrors.CONNECTIVITY),
    SESSION_EXPIRED(MuleErrors.CONNECTIVITY),
    SESSION_ERROR(MuleErrors.CONNECTIVITY),
    AUTHENTICATION_FAILED(MuleErrors.CONNECTIVITY),
    SEQUENCE_MISMATCH(MuleErrors.CONNECTIVITY),
    
    // Message Validation Errors
    INVALID_MESSAGE_FORMAT,
    SCHEMA_VALIDATION_FAILED,
    INVALID_BIC_CODE,
    INVALID_CURRENCY_CODE,
    FIELD_LENGTH_EXCEEDED,
    MANDATORY_FIELD_MISSING,
    
    // Network Acknowledgment Errors
    MESSAGE_REJECTED,
    NETWORK_NACK,
    NACK_RECEIVED,          // Distinct error for SWIFT NACK (Tag 451 non-zero)
    ACK_TIMEOUT,            // No ACK/NACK received within timeout period
    DUPLICATE_MESSAGE,
    
    // Business Errors
    INSUFFICIENT_FUNDS,
    ACCOUNT_NOT_FOUND,
    CUTOFF_TIME_EXCEEDED,
    HOLIDAY_CALENDAR_VIOLATION,
    SANCTIONS_SCREENING_FAILED,
    
    // gpi Specific Errors
    GPI_TRACKING_NOT_AVAILABLE,
    GPI_PAYMENT_NOT_FOUND,
    GPI_STOP_RECALL_FAILED,
    
    // Transformation Errors
    MT_TO_MX_CONVERSION_FAILED,
    MX_TO_MT_CONVERSION_FAILED,
    UNSUPPORTED_MESSAGE_TYPE,
    
    // Security & Compliance Errors
    SIGNATURE_VERIFICATION_FAILED,
    LAU_AUTHENTICATION_FAILED,
    SANCTIONS_VIOLATION,
    ENCRYPTION_FAILED,
    CERTIFICATE_EXPIRED,
    HSM_UNAVAILABLE,
    
    // Operational Errors
    MESSAGE_QUEUE_FULL,
    REPLAY_FAILED,
    TIMEOUT(MuleErrors.CONNECTIVITY),
    RATE_LIMIT_EXCEEDED,
    
    // Investigation Errors
    CASE_CREATION_FAILED,
    CASE_NOT_FOUND,
    
    // Reference Data Errors
    RMA_CHECK_FAILED,
    COUNTERPARTY_NOT_AUTHORIZED,
    CONFIGURATION_ERROR;
    
    private ErrorTypeDefinition<? extends Enum<?>> parent;
    
    SwiftErrorType(ErrorTypeDefinition<? extends Enum<?>> parent) {
        this.parent = parent;
    }
    
    SwiftErrorType() {
        this(null);
    }
    
    @Override
    public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
        return Optional.ofNullable(parent);
    }
}


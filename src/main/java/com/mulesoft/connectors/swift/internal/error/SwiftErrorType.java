package com.mulesoft.connectors.swift.internal.error;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.error.MuleErrors;

import java.util.Optional;

/**
 * SWIFT-specific error types for granular error handling in Mule flows.
 * These enable precise "On Error Continue" and "On Error Propagate" patterns.
 */
public enum SwiftErrorType implements ErrorTypeDefinition<SwiftErrorType> {
    
    // === ERROR CATEGORIES (NEW) ===
    // Parser/Format Errors (Malformed Messages)
    // Use in Mule flows: <on-error-continue type="SWIFT:SYNTAX_ERROR"> → Alert dev team, no retry
    SYNTAX_ERROR,
    
    // Business Logic Errors (Valid Message, Invalid Business Rules)
    // Use in Mule flows: <on-error-continue type="SWIFT:BUSINESS_RULE_VIOLATION"> → Retry with backoff
    BUSINESS_RULE_VIOLATION,
    
    // Connection & Session Errors
    CONNECTION_FAILED(MuleErrors.CONNECTIVITY),
    CONNECTION_ERROR(MuleErrors.CONNECTIVITY),
    SESSION_EXPIRED(MuleErrors.CONNECTIVITY),
    SESSION_ERROR(MuleErrors.CONNECTIVITY),
    AUTHENTICATION_FAILED(MuleErrors.CONNECTIVITY),
    SEQUENCE_MISMATCH(MuleErrors.CONNECTIVITY),
    
    // Message Validation Errors → SYNTAX_ERROR
    INVALID_MESSAGE_FORMAT(SYNTAX_ERROR),
    SCHEMA_VALIDATION_FAILED(SYNTAX_ERROR),
    INVALID_BIC_CODE(SYNTAX_ERROR),
    INVALID_CURRENCY_CODE(SYNTAX_ERROR),
    FIELD_LENGTH_EXCEEDED(SYNTAX_ERROR),
    MANDATORY_FIELD_MISSING(SYNTAX_ERROR),
    
    // Network Acknowledgment Errors
    MESSAGE_REJECTED,
    NETWORK_NACK,
    NACK_RECEIVED,          // Distinct error for SWIFT NACK (Tag 451 non-zero)
    ACK_TIMEOUT,            // No ACK/NACK received within timeout period
    DUPLICATE_MESSAGE,
    
    // Business Errors → BUSINESS_RULE_VIOLATION
    INSUFFICIENT_FUNDS(BUSINESS_RULE_VIOLATION),
    ACCOUNT_NOT_FOUND(BUSINESS_RULE_VIOLATION),
    CUTOFF_TIME_EXCEEDED(BUSINESS_RULE_VIOLATION),
    HOLIDAY_CALENDAR_VIOLATION(BUSINESS_RULE_VIOLATION),
    SANCTIONS_SCREENING_FAILED(BUSINESS_RULE_VIOLATION),
    
    // gpi Specific Errors
    GPI_TRACKING_NOT_AVAILABLE,
    GPI_PAYMENT_NOT_FOUND,
    GPI_STOP_RECALL_FAILED,
    
    // Transformation Errors
    MT_TO_MX_CONVERSION_FAILED,
    MX_TO_MT_CONVERSION_FAILED,
    UNSUPPORTED_MESSAGE_TYPE,
    
    // Security & Compliance Errors
    SIGNATURE_VERIFICATION_FAILED(SYNTAX_ERROR),
    LAU_AUTHENTICATION_FAILED(SYNTAX_ERROR),
    SANCTIONS_VIOLATION(BUSINESS_RULE_VIOLATION),
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


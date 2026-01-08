package com.mulesoft.connectors.swift.internal.operation;

import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides SWIFT-specific error types for operations.
 */
public class SwiftErrorProvider implements ErrorTypeProvider {

    @Override
    public Set<ErrorTypeDefinition> getErrorTypes() {
        Set<ErrorTypeDefinition> errors = new HashSet<>();
        
        // ✅ NEW: Error categories for granular error handling
        errors.add(SwiftErrorType.SYNTAX_ERROR);
        errors.add(SwiftErrorType.BUSINESS_RULE_VIOLATION);
        
        // Connection errors
        errors.add(SwiftErrorType.CONNECTION_FAILED);
        errors.add(SwiftErrorType.SESSION_EXPIRED);
        errors.add(SwiftErrorType.AUTHENTICATION_FAILED);
        
        // Validation errors (child of SYNTAX_ERROR)
        errors.add(SwiftErrorType.INVALID_MESSAGE_FORMAT);
        errors.add(SwiftErrorType.SCHEMA_VALIDATION_FAILED);
        errors.add(SwiftErrorType.INVALID_BIC_CODE);
        errors.add(SwiftErrorType.FIELD_LENGTH_EXCEEDED);
        errors.add(SwiftErrorType.MANDATORY_FIELD_MISSING);
        
        // Network errors
        errors.add(SwiftErrorType.MESSAGE_REJECTED);
        errors.add(SwiftErrorType.NETWORK_NACK);
        errors.add(SwiftErrorType.NACK_RECEIVED);
        errors.add(SwiftErrorType.ACK_TIMEOUT);
        errors.add(SwiftErrorType.DUPLICATE_MESSAGE);
        
        // Business errors (child of BUSINESS_RULE_VIOLATION)
        errors.add(SwiftErrorType.CUTOFF_TIME_EXCEEDED);
        errors.add(SwiftErrorType.HOLIDAY_CALENDAR_VIOLATION);
        errors.add(SwiftErrorType.SANCTIONS_VIOLATION);
        errors.add(SwiftErrorType.INSUFFICIENT_FUNDS);
        
        // Operational errors
        errors.add(SwiftErrorType.TIMEOUT);
        errors.add(SwiftErrorType.MESSAGE_QUEUE_FULL);
        
        return errors;
    }
}


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
        
        // Connection errors
        errors.add(SwiftErrorType.CONNECTION_FAILED);
        errors.add(SwiftErrorType.SESSION_EXPIRED);
        errors.add(SwiftErrorType.AUTHENTICATION_FAILED);
        
        // Validation errors
        errors.add(SwiftErrorType.INVALID_MESSAGE_FORMAT);
        errors.add(SwiftErrorType.SCHEMA_VALIDATION_FAILED);
        errors.add(SwiftErrorType.INVALID_BIC_CODE);
        
        // Network errors
        errors.add(SwiftErrorType.MESSAGE_REJECTED);
        errors.add(SwiftErrorType.NETWORK_NACK);
        errors.add(SwiftErrorType.DUPLICATE_MESSAGE);
        
        // Operational errors
        errors.add(SwiftErrorType.TIMEOUT);
        errors.add(SwiftErrorType.MESSAGE_QUEUE_FULL);
        
        return errors;
    }
}


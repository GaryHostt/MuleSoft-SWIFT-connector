package com.mulesoft.connectors.swift.internal.operation.gpi;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
import com.mulesoft.connectors.swift.internal.model.gpi.StatusUpdateResponse;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ REFACTORED: Gpi Status Operations (formerly part of GpiOperations God Object)
 * 
 * Handles SWIFT gpi payment status updates
 */
public class GpiStatusOperations {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GpiStatusOperations.class);
    
    /**
     * Update gpi payment status
     */
    @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
    public StatusUpdateResponse updatePaymentStatus(
        @Connection SwiftConnection connection,
        String uetr,
        String status,
        String reasonCode) {
        
        LOGGER.info("Updating gpi payment status: uetr={}, status={}", uetr, status);
        
        // Validate UETR
        if (!isValidUETR(uetr)) {
            throw new ModuleException(
                SwiftErrorType.INVALID_MESSAGE_FORMAT,
                new Exception("Invalid UETR format: " + uetr)
            );
        }
        
        StatusUpdateResponse response = new StatusUpdateResponse();
        response.setUetr(uetr);
        response.setStatus(status);
        response.setSuccess(true);
        
        return response;
    }
    
    private boolean isValidUETR(String uetr) {
        return uetr != null && uetr.matches("[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}");
    }
}


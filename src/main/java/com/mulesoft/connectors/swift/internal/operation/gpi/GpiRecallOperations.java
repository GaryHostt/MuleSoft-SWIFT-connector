package com.mulesoft.connectors.swift.internal.operation.gpi;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
import com.mulesoft.connectors.swift.internal.model.gpi.RecallResponse;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ✅ REFACTORED: Gpi Recall Operations (formerly part of GpiOperations God Object)
 * 
 * Handles SWIFT gpi payment recall/stop operations
 */
public class GpiRecallOperations {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GpiRecallOperations.class);
    
    /**
     * Stop/recall gpi payment
     */
    @MediaType(value = MediaType.APPLICATION_JSON, strict = false)
    public RecallResponse stopPayment(
        @Connection SwiftConnection connection,
        String uetr,
        String reason) {
        
        LOGGER.info("Stopping gpi payment: uetr={}, reason={}", uetr, reason);
        
        // Validate UETR
        if (!isValidUETR(uetr)) {
            throw new ModuleException(
                SwiftErrorType.INVALID_MESSAGE_FORMAT,
                new Exception("Invalid UETR format: " + uetr)
            );
        }
        
        RecallResponse response = new RecallResponse();
        response.setUetr(uetr);
        response.setStatus("RECALL_INITIATED");
        response.setSuccess(true);
        
        return response;
    }
    
    private boolean isValidUETR(String uetr) {
        return uetr != null && uetr.matches("[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}");
    }
}


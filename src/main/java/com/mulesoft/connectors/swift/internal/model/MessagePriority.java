package com.mulesoft.connectors.swift.internal.model;

/**
 * Message priority levels
 */
public enum MessagePriority {
    /**
     * Urgent priority
     */
    URGENT,
    
    /**
     * Normal priority (default)
     */
    NORMAL,
    
    /**
     * System priority (for network messages)
     */
    SYSTEM
}


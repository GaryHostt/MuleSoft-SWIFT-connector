package com.mulesoft.connectors.swift.internal.connection;

/**
 * SWIFT messaging protocols
 */
public enum SwiftProtocol {
    /**
     * FIN (Financial) - Legacy protocol for MT messages
     */
    FIN,
    
    /**
     * InterAct - Real-time file transfer protocol
     */
    INTERACT,
    
    /**
     * FileAct - Store-and-forward file transfer protocol
     */
    FILEACT,
    
    /**
     * gpi - Global Payments Innovation REST API
     */
    GPI_API
}


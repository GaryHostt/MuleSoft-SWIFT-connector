package com.mulesoft.connectors.swift.internal.model;

/**
 * Status of a SWIFT message through its lifecycle
 */
public enum SwiftMessageStatus {
    CREATED,
    VALIDATED,
    QUEUED,
    SENT,
    ACKNOWLEDGED,
    RECEIVED,
    REJECTED,
    FAILED,
    COMPLETED
}


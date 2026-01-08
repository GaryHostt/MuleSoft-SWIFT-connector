package com.mulesoft.connectors.swift.internal.model;

/**
 * Validation error with category classification.
 * 
 * <p>Categories distinguish between:
 * <ul>
 *   <li><strong>SYNTAX</strong>: Malformed messages, missing fields, invalid formats</li>
 *   <li><strong>BUSINESS</strong>: Valid syntax but failed business rules (cutoff, sanctions, holidays)</li>
 * </ul>
 * </p>
 */
public class ValidationError {
    
    private String code;
    private String message;
    private String field;
    private String category; // "SYNTAX" or "BUSINESS"

    public ValidationError(String code, String message, String field) {
        this(code, message, field, "SYNTAX"); // Default to SYNTAX for backward compatibility
    }

    public ValidationError(String code, String message, String field, String category) {
        this.code = code;
        this.message = message;
        this.field = field;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}


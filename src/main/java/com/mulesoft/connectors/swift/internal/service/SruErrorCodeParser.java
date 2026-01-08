package com.mulesoft.connectors.swift.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SWIFT SRU (Standards Release User) Error Code Parser
 * 
 * <p>Parses SWIFT rejection messages and extracts error codes from ACK/NACK responses.
 * SWIFT uses specific error code formats defined in the Standards Release Updates (SRU).</p>
 * 
 * <h2>Error Code Categories</h2>
 * <ul>
 *   <li><strong>T-codes</strong>: Text validation errors (T01-T99)</li>
 *   <li><strong>K-codes</strong>: Network validation errors (K01-K99)</li>
 *   <li><strong>D-codes</strong>: Delivery errors (D01-D99)</li>
 *   <li><strong>S-codes</strong>: Security/signature errors (S01-S99)</li>
 *   <li><strong>E-codes</strong>: System errors (E01-E99)</li>
 * </ul>
 * 
 * <h2>Common Error Codes</h2>
 * <table>
 *   <tr><th>Code</th><th>Description</th><th>Action</th></tr>
 *   <tr><td>T01</td><td>Invalid BIC</td><td>Correct receiver BIC</td></tr>
 *   <tr><td>T13</td><td>Unknown message type</td><td>Verify message type (e.g., 103)</td></tr>
 *   <tr><td>T26</td><td>Invalid date</td><td>Correct date format (YYMMDD)</td></tr>
 *   <tr><td>K90</td><td>Field format error</td><td>Check field :32A: format</td></tr>
 *   <tr><td>K91</td><td>Field length exceeded</td><td>Truncate field to max length</td></tr>
 *   <tr><td>D01</td><td>Delivery timeout</td><td>Retry after network recovery</td></tr>
 *   <tr><td>S01</td><td>Invalid signature</td><td>Verify MAC/LAU credentials</td></tr>
 * </table>
 * 
 * @see <a href="https://www2.swift.com/uhbonline/">SWIFT User Handbook</a>
 */
public class SruErrorCodeParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SruErrorCodeParser.class);
    
    // Regex patterns for extracting error codes from SWIFT messages
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile(":451:([TKDSE]\\d{2})");
    private static final Pattern ERROR_TEXT_PATTERN = Pattern.compile(":405:(.*?)(?=:|$)", Pattern.DOTALL);
    private static final Pattern FIELD_TAG_PATTERN = Pattern.compile(":([0-9]{2}[A-Z]?):");
    
    // Error code dictionary (subset - full dictionary would be externalized)
    private static final Map<String, ErrorDefinition> ERROR_DICTIONARY = new HashMap<>();
    
    static {
        // T-series: Text validation errors
        ERROR_DICTIONARY.put("T01", new ErrorDefinition("T01", "Invalid BIC code", 
            "Verify receiver BIC in Tag :57A: or :58A:", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T12", new ErrorDefinition("T12", "Invalid MT number",
            "Message type in Block 2 is not supported", Severity.ERROR, false));
        ERROR_DICTIONARY.put("T13", new ErrorDefinition("T13", "Unknown message type",
            "Message type does not exist in current SR", Severity.ERROR, false));
        ERROR_DICTIONARY.put("T26", new ErrorDefinition("T26", "Invalid date",
            "Date format must be YYMMDD", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T27", new ErrorDefinition("T27", "Invalid currency code",
            "Currency code must be valid ISO 4217", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T50", new ErrorDefinition("T50", "Mandatory field missing",
            "Required field is absent from message", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T51", new ErrorDefinition("T51", "Field repetition error",
            "Field appears more than allowed occurrences", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T52", new ErrorDefinition("T52", "Invalid field sequence",
            "Fields appear in wrong order", Severity.ERROR, true));
        ERROR_DICTIONARY.put("T70", new ErrorDefinition("T70", "Invalid field format",
            "Field content does not match required format", Severity.ERROR, true));
        
        // K-series: Network validation errors
        ERROR_DICTIONARY.put("K90", new ErrorDefinition("K90", "Field format error in Tag :32A:",
            "Value Date/Currency/Amount format is incorrect", Severity.ERROR, true));
        ERROR_DICTIONARY.put("K91", new ErrorDefinition("K91", "Field length exceeded",
            "Field content exceeds maximum allowed length", Severity.ERROR, true));
        ERROR_DICTIONARY.put("K92", new ErrorDefinition("K92", "Invalid character in field",
            "Field contains character(s) not in SWIFT X-Character Set", Severity.ERROR, true));
        
        // D-series: Delivery errors
        ERROR_DICTIONARY.put("D01", new ErrorDefinition("D01", "Delivery timeout",
            "Message could not be delivered within timeout period", Severity.WARNING, true));
        ERROR_DICTIONARY.put("D02", new ErrorDefinition("D02", "Receiver unavailable",
            "Destination institution is not reachable", Severity.WARNING, true));
        ERROR_DICTIONARY.put("D03", new ErrorDefinition("D03", "RMA authorization missing",
            "Sender not authorized to send this message type to receiver", Severity.ERROR, false));
        
        // S-series: Security/signature errors
        ERROR_DICTIONARY.put("S01", new ErrorDefinition("S01", "Invalid MAC",
            "Message Authentication Code verification failed", Severity.ERROR, false));
        ERROR_DICTIONARY.put("S02", new ErrorDefinition("S02", "Invalid digital signature",
            "LAU (Local Authentication) signature is invalid", Severity.ERROR, false));
        ERROR_DICTIONARY.put("S03", new ErrorDefinition("S03", "Certificate expired",
            "PKI certificate used for signing has expired", Severity.ERROR, false));
        
        // E-series: System errors
        ERROR_DICTIONARY.put("E01", new ErrorDefinition("E01", "System error",
            "SWIFT system encountered internal error", Severity.ERROR, true));
        ERROR_DICTIONARY.put("E02", new ErrorDefinition("E02", "Duplicate emission",
            "Message with same MOR has been sent previously", Severity.WARNING, false));
    }
    
    /**
     * Parse SWIFT ACK/NACK message and extract error information.
     * 
     * @param ackNackMessage Raw ACK/NACK message from SWIFT
     * @return Parsed error result
     */
    public static SruErrorResult parse(String ackNackMessage) {
        if (ackNackMessage == null || ackNackMessage.isEmpty()) {
            return SruErrorResult.noError();
        }
        
        LOGGER.debug("Parsing SWIFT ACK/NACK message: {}", 
            ackNackMessage.substring(0, Math.min(100, ackNackMessage.length())));
        
        // Extract error code from Tag :451:
        Matcher errorCodeMatcher = ERROR_CODE_PATTERN.matcher(ackNackMessage);
        String errorCode = null;
        if (errorCodeMatcher.find()) {
            errorCode = errorCodeMatcher.group(1);
            LOGGER.info("Extracted SWIFT error code: {}", errorCode);
        }
        
        // Extract error text from Tag :405:
        Matcher errorTextMatcher = ERROR_TEXT_PATTERN.matcher(ackNackMessage);
        String errorText = null;
        if (errorTextMatcher.find()) {
            errorText = errorTextMatcher.group(1).trim();
        }
        
        // Extract affected field tag (if present)
        String affectedField = extractAffectedField(ackNackMessage, errorText);
        
        // Check if this is an ACK (no error code) or NACK (error code present)
        boolean isAck = errorCode == null;
        
        if (isAck) {
            LOGGER.info("Message acknowledged (ACK) - no errors");
            return SruErrorResult.acknowledged();
        }
        
        // Lookup error definition
        ErrorDefinition definition = ERROR_DICTIONARY.getOrDefault(errorCode,
            new ErrorDefinition(errorCode, "Unknown error code", 
                "Consult SWIFT User Handbook for error code " + errorCode, 
                Severity.ERROR, false));
        
        return new SruErrorResult(
            false,
            true,
            errorCode,
            definition.getDescription(),
            errorText,
            affectedField,
            definition.getSeverity(),
            definition.isRecoverable(),
            definition.getRemediationAction()
        );
    }
    
    /**
     * Extract the affected field tag from error message.
     * 
     * <p>SWIFT error messages often include the field tag that caused the error.</p>
     * 
     * @param message Full error message
     * @param errorText Error description text
     * @return Field tag (e.g., "32A") or null if not found
     */
    private static String extractAffectedField(String message, String errorText) {
        // Try to find field tag in error text first
        if (errorText != null) {
            Matcher fieldMatcher = FIELD_TAG_PATTERN.matcher(errorText);
            if (fieldMatcher.find()) {
                return fieldMatcher.group(1);
            }
        }
        
        // Try to find in full message
        Matcher fieldMatcher = FIELD_TAG_PATTERN.matcher(message);
        if (fieldMatcher.find()) {
            return fieldMatcher.group(1);
        }
        
        return null;
    }
    
    /**
     * Get error category from error code.
     * 
     * @param errorCode SWIFT error code (e.g., "T01", "K90")
     * @return Error category (TEXT_VALIDATION, NETWORK_VALIDATION, DELIVERY, SECURITY, SYSTEM)
     */
    public static ErrorCategory getErrorCategory(String errorCode) {
        if (errorCode == null || errorCode.length() < 1) {
            return ErrorCategory.UNKNOWN;
        }
        
        char categoryChar = errorCode.charAt(0);
        switch (categoryChar) {
            case 'T': return ErrorCategory.TEXT_VALIDATION;
            case 'K': return ErrorCategory.NETWORK_VALIDATION;
            case 'D': return ErrorCategory.DELIVERY;
            case 'S': return ErrorCategory.SECURITY;
            case 'E': return ErrorCategory.SYSTEM;
            default: return ErrorCategory.UNKNOWN;
        }
    }
    
    /**
     * SRU Error Result
     */
    public static class SruErrorResult {
        private final boolean isAck;
        private final boolean isNack;
        private final String errorCode;
        private final String errorDescription;
        private final String errorText;
        private final String affectedField;
        private final Severity severity;
        private final boolean recoverable;
        private final String remediationAction;
        
        public SruErrorResult(boolean isAck, boolean isNack, String errorCode, 
                             String errorDescription, String errorText, String affectedField,
                             Severity severity, boolean recoverable, String remediationAction) {
            this.isAck = isAck;
            this.isNack = isNack;
            this.errorCode = errorCode;
            this.errorDescription = errorDescription;
            this.errorText = errorText;
            this.affectedField = affectedField;
            this.severity = severity;
            this.recoverable = recoverable;
            this.remediationAction = remediationAction;
        }
        
        public static SruErrorResult noError() {
            return new SruErrorResult(false, false, null, null, null, null, null, false, null);
        }
        
        public static SruErrorResult acknowledged() {
            return new SruErrorResult(true, false, null, "Message acknowledged", 
                null, null, Severity.INFO, true, "None - message accepted");
        }
        
        // Getters
        public boolean isAck() { return isAck; }
        public boolean isNack() { return isNack; }
        public String getErrorCode() { return errorCode; }
        public String getErrorDescription() { return errorDescription; }
        public String getErrorText() { return errorText; }
        public String getAffectedField() { return affectedField; }
        public Severity getSeverity() { return severity; }
        public boolean isRecoverable() { return recoverable; }
        public String getRemediationAction() { return remediationAction; }
        public ErrorCategory getCategory() { return getErrorCategory(errorCode); }
    }
    
    /**
     * Error Definition (from SWIFT SRU dictionary)
     */
    private static class ErrorDefinition {
        private final String code;
        private final String description;
        private final String remediationAction;
        private final Severity severity;
        private final boolean recoverable;
        
        public ErrorDefinition(String code, String description, String remediationAction, 
                              Severity severity, boolean recoverable) {
            this.code = code;
            this.description = description;
            this.remediationAction = remediationAction;
            this.severity = severity;
            this.recoverable = recoverable;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getRemediationAction() { return remediationAction; }
        public Severity getSeverity() { return severity; }
        public boolean isRecoverable() { return recoverable; }
    }
    
    /**
     * Error Category
     */
    public enum ErrorCategory {
        TEXT_VALIDATION("T", "Text Validation Error", "Development Team"),
        NETWORK_VALIDATION("K", "Network Validation Error", "Development Team"),
        DELIVERY("D", "Delivery Error", "Operations Team"),
        SECURITY("S", "Security/Signature Error", "Security Team"),
        SYSTEM("E", "System Error", "SWIFT Operations"),
        UNKNOWN("?", "Unknown Error", "SWIFT Support");
        
        private final String prefix;
        private final String displayName;
        private final String responsibleTeam;
        
        ErrorCategory(String prefix, String displayName, String responsibleTeam) {
            this.prefix = prefix;
            this.displayName = displayName;
            this.responsibleTeam = responsibleTeam;
        }
        
        public String getPrefix() { return prefix; }
        public String getDisplayName() { return displayName; }
        public String getResponsibleTeam() { return responsibleTeam; }
    }
    
    /**
     * Error Severity
     */
    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}


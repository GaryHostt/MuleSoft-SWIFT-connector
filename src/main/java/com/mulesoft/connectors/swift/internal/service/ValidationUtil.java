package com.mulesoft.connectors.swift.internal.service;

import com.mulesoft.connectors.swift.internal.model.ValidationError;
import com.mulesoft.connectors.swift.internal.model.ValidationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Shared Validation Utility - DRY Principle
 * 
 * <h2>Problem: Redundancy in MT and MX Validation</h2>
 * <pre>{@code
 * // BAD: Repeated in validateMtMessage() and validateMxMessage()
 * if (content == null || content.trim().isEmpty()) {
 *     errors.add(new ValidationError("E001", "Content is empty", "content", "SYNTAX"));
 * }
 * 
 * if (content.length() < 50) {
 *     errors.add(new ValidationError("E002", "Content too short", "content", "SYNTAX"));
 * }
 * }</pre>
 * 
 * <h2>Solution: Shared Utility (DRY)</h2>
 * <pre>{@code
 * // GOOD: Single source of truth
 * ValidationUtil.validateNotEmpty(content, errors, "content");
 * ValidationUtil.validateMinLength(content, 50, errors, "content");
 * }</pre>
 * 
 * <p>This utility eliminates redundancy between MT and MX validation methods while
 * maintaining type safety and clear error messages.</p>
 */
public class ValidationUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtil.class);
    
    // Common regex patterns (compiled once, reused)
    private static final Pattern BIC_PATTERN = Pattern.compile("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d{1,15}(,\\d{1,2})?$");
    private static final Pattern DATE_YYMMDD_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("^[A-Z0-9]{1,16}$");
    
    /**
     * Validate content is not null or empty.
     * 
     * @param content Content to validate
     * @param errors Error list to append to
     * @param fieldName Field name for error message
     * @return true if valid
     */
    public static boolean validateNotEmpty(String content, List<ValidationError> errors, String fieldName) {
        if (content == null || content.trim().isEmpty()) {
            errors.add(new ValidationError(
                "E001",
                fieldName + " is empty",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: {} is empty", fieldName);
            return false;
        }
        return true;
    }
    
    /**
     * Validate minimum length.
     * 
     * @param content Content to validate
     * @param minLength Minimum length
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateMinLength(String content, int minLength, 
                                           List<ValidationError> errors, String fieldName) {
        if (content == null || content.length() < minLength) {
            errors.add(new ValidationError(
                "E002",
                fieldName + " too short (minimum " + minLength + " characters)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: {} length {} < {}", fieldName, 
                content != null ? content.length() : 0, minLength);
            return false;
        }
        return true;
    }
    
    /**
     * Validate maximum length.
     * 
     * @param content Content to validate
     * @param maxLength Maximum length
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateMaxLength(String content, int maxLength,
                                           List<ValidationError> errors, String fieldName) {
        if (content != null && content.length() > maxLength) {
            errors.add(new ValidationError(
                "E003",
                fieldName + " too long (maximum " + maxLength + " characters)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: {} length {} > {}", fieldName, content.length(), maxLength);
            return false;
        }
        return true;
    }
    
    /**
     * Validate BIC code format (8 or 11 characters).
     * 
     * @param bic BIC code
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateBicCode(String bic, List<ValidationError> errors, String fieldName) {
        if (bic == null || !BIC_PATTERN.matcher(bic).matches()) {
            errors.add(new ValidationError(
                "E004",
                "Invalid BIC code format (expected: XXXXXX99XXX)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid BIC code: {}", bic);
            return false;
        }
        return true;
    }
    
    /**
     * Validate ISO 4217 currency code.
     * 
     * @param currency Currency code
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateCurrency(String currency, List<ValidationError> errors, String fieldName) {
        if (currency == null || !CURRENCY_PATTERN.matcher(currency).matches()) {
            errors.add(new ValidationError(
                "E005",
                "Invalid currency code (expected: 3 uppercase letters, e.g., USD)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid currency: {}", currency);
            return false;
        }
        return true;
    }
    
    /**
     * Validate amount format.
     * 
     * @param amount Amount string
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateAmount(String amount, List<ValidationError> errors, String fieldName) {
        if (amount == null || !AMOUNT_PATTERN.matcher(amount).matches()) {
            errors.add(new ValidationError(
                "E006",
                "Invalid amount format (expected: digits with optional comma for decimals)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid amount: {}", amount);
            return false;
        }
        return true;
    }
    
    /**
     * Validate date in YYMMDD format.
     * 
     * @param date Date string
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateDateYYMMDD(String date, List<ValidationError> errors, String fieldName) {
        if (date == null || !DATE_YYMMDD_PATTERN.matcher(date).matches()) {
            errors.add(new ValidationError(
                "E007",
                "Invalid date format (expected: YYMMDD)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid date: {}", date);
            return false;
        }
        return true;
    }
    
    /**
     * Validate transaction reference format.
     * 
     * @param reference Reference string
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateReference(String reference, List<ValidationError> errors, String fieldName) {
        if (reference == null || !REFERENCE_PATTERN.matcher(reference).matches()) {
            errors.add(new ValidationError(
                "E008",
                "Invalid reference format (expected: 1-16 alphanumeric characters)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid reference: {}", reference);
            return false;
        }
        return true;
    }
    
    /**
     * Validate mandatory field presence.
     * 
     * @param content Message content
     * @param fieldTag SWIFT field tag (e.g., ":20:", ":32A:")
     * @param errors Error list
     * @param fieldName Field name
     * @return true if present
     */
    public static boolean validateMandatoryField(String content, String fieldTag,
                                                 List<ValidationError> errors, String fieldName) {
        if (content == null || !content.contains(fieldTag)) {
            errors.add(new ValidationError(
                "E009",
                "Mandatory field " + fieldTag + " is missing",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Mandatory field {} missing", fieldTag);
            return false;
        }
        return true;
    }
    
    /**
     * Validate SWIFT character set (X-Character Set).
     * 
     * @param content Content to validate
     * @param warnings Warning list (not error - can be sanitized)
     * @param fieldName Field name
     * @return true if valid (warnings don't fail validation)
     */
    public static boolean validateSwiftCharacterSet(String content, List<ValidationWarning> warnings, 
                                                    String fieldName) {
        if (content == null) {
            return true;
        }
        
        // Check for invalid characters (anything not in SWIFT X-Character Set)
        boolean hasInvalidChars = false;
        for (char c : content.toCharArray()) {
            if (!isSwiftValidChar(c)) {
                hasInvalidChars = true;
                break;
            }
        }
        
        if (hasInvalidChars) {
            warnings.add(new ValidationWarning(
                "W001",
                fieldName + " contains invalid SWIFT characters (will be sanitized)",
                fieldName
            ));
            LOGGER.info("Character set warning: {} contains invalid characters", fieldName);
        }
        
        return true; // Always return true - warnings don't fail validation
    }
    
    /**
     * Check if character is valid in SWIFT X-Character Set.
     * 
     * @param c Character to check
     * @return true if valid
     */
    private static boolean isSwiftValidChar(char c) {
        // SWIFT X-Character Set: A-Z, 0-9, space, and specific punctuation
        return (c >= 'A' && c <= 'Z') ||
               (c >= '0' && c <= '9') ||
               c == ' ' || c == '/' || c == '-' || c == '?' || c == ':' ||
               c == '(' || c == ')' || c == '.' || c == ',' || c == '\'' ||
               c == '+' || c == '{' || c == '}' || c == '\r' || c == '\n';
    }
    
    /**
     * Validate XML well-formedness (for MX messages).
     * 
     * @param content XML content
     * @param errors Error list
     * @param fieldName Field name
     * @return true if valid
     */
    public static boolean validateXmlWellFormed(String content, List<ValidationError> errors, 
                                                String fieldName) {
        if (content == null) {
            return validateNotEmpty(content, errors, fieldName);
        }
        
        // Basic XML validation
        if (!content.trim().startsWith("<?xml") && !content.trim().startsWith("<")) {
            errors.add(new ValidationError(
                "E010",
                "Content is not valid XML (missing XML declaration or root element)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Invalid XML structure");
            return false;
        }
        
        // Check for matching opening/closing tags (basic check)
        int openBrackets = 0;
        int closeBrackets = 0;
        for (char c : content.toCharArray()) {
            if (c == '<') openBrackets++;
            if (c == '>') closeBrackets++;
        }
        
        if (openBrackets != closeBrackets) {
            errors.add(new ValidationError(
                "E011",
                "XML is malformed (mismatched brackets)",
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Malformed XML (brackets: open={}, close={})", 
                openBrackets, closeBrackets);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate namespace presence (for MX messages).
     * 
     * @param content XML content
     * @param namespace Expected namespace
     * @param errors Error list
     * @param fieldName Field name
     * @return true if present
     */
    public static boolean validateNamespace(String content, String namespace,
                                           List<ValidationError> errors, String fieldName) {
        if (content == null || !content.contains(namespace)) {
            errors.add(new ValidationError(
                "E012",
                "Missing required namespace: " + namespace,
                fieldName,
                "SYNTAX"
            ));
            LOGGER.warn("Validation failed: Missing namespace: {}", namespace);
            return false;
        }
        return true;
    }
}


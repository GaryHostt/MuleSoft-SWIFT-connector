package com.mulesoft.connectors.swift.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SWIFT X-Character Set Utility
 * 
 * <p>SWIFT FIN messages use a restricted character set defined by ISO 9735.
 * This utility ensures all message content conforms to SWIFT standards by
 * sanitizing or validating input strings.</p>
 * 
 * <h2>SWIFT X-Character Set (Printable Characters)</h2>
 * <ul>
 *   <li>Uppercase letters: A-Z</li>
 *   <li>Digits: 0-9</li>
 *   <li>Space: ' '</li>
 *   <li>Allowed special characters: / - ? : ( ) . , ' + { }</li>
 *   <li>Line breaks: CR, LF (in specific contexts)</li>
 * </ul>
 * 
 * <h2>Common Issues</h2>
 * <ul>
 *   <li>Lowercase letters → Convert to uppercase</li>
 *   <li>Accented characters (é, ñ, ü) → Replace with closest ASCII equivalent</li>
 *   <li>Special characters (&, @, #, $, %, etc.) → Replace or remove</li>
 *   <li>Emoji, Unicode → Remove</li>
 * </ul>
 * 
 * @see <a href="https://www2.swift.com/uhbonline/books/public/en_uk/us9m_20210723/ch04.htm">SWIFT Character Set</a>
 */
public class SwiftCharacterSetUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftCharacterSetUtil.class);
    
    // SWIFT X-Character Set: A-Z, 0-9, space, and specific punctuation
    private static final Pattern SWIFT_VALID_CHARS = Pattern.compile("[A-Z0-9 /\\-?:().,'\\+\\{\\}\r\n]");
    
    // Characters that need replacement (accented → ASCII)
    private static final Map<Character, Character> ACCENT_MAP = new HashMap<>();
    
    static {
        // Latin accents to ASCII (using Unicode escapes for portability)
        ACCENT_MAP.put('\u00C0', 'A'); ACCENT_MAP.put('\u00C1', 'A'); ACCENT_MAP.put('\u00C2', 'A'); ACCENT_MAP.put('\u00C3', 'A');
        ACCENT_MAP.put('\u00C4', 'A'); ACCENT_MAP.put('\u00C5', 'A'); ACCENT_MAP.put('\u00C6', 'A');
        ACCENT_MAP.put('\u00E0', 'a'); ACCENT_MAP.put('\u00E1', 'a'); ACCENT_MAP.put('\u00E2', 'a'); ACCENT_MAP.put('\u00E3', 'a');
        ACCENT_MAP.put('\u00E4', 'a'); ACCENT_MAP.put('\u00E5', 'a'); ACCENT_MAP.put('\u00E6', 'a');
        
        ACCENT_MAP.put('\u00C8', 'E'); ACCENT_MAP.put('\u00C9', 'E'); ACCENT_MAP.put('\u00CA', 'E'); ACCENT_MAP.put('\u00CB', 'E');
        ACCENT_MAP.put('\u00E8', 'e'); ACCENT_MAP.put('\u00E9', 'e'); ACCENT_MAP.put('\u00EA', 'e'); ACCENT_MAP.put('\u00EB', 'e');
        
        ACCENT_MAP.put('\u00CC', 'I'); ACCENT_MAP.put('\u00CD', 'I'); ACCENT_MAP.put('\u00CE', 'I'); ACCENT_MAP.put('\u00CF', 'I');
        ACCENT_MAP.put('\u00EC', 'i'); ACCENT_MAP.put('\u00ED', 'i'); ACCENT_MAP.put('\u00EE', 'i'); ACCENT_MAP.put('\u00EF', 'i');
        
        ACCENT_MAP.put('\u00D2', 'O'); ACCENT_MAP.put('\u00D3', 'O'); ACCENT_MAP.put('\u00D4', 'O'); ACCENT_MAP.put('\u00D5', 'O');
        ACCENT_MAP.put('\u00D6', 'O'); ACCENT_MAP.put('\u00D8', 'O');
        ACCENT_MAP.put('\u00F2', 'o'); ACCENT_MAP.put('\u00F3', 'o'); ACCENT_MAP.put('\u00F4', 'o'); ACCENT_MAP.put('\u00F5', 'o');
        ACCENT_MAP.put('\u00F6', 'o'); ACCENT_MAP.put('\u00F8', 'o');
        
        ACCENT_MAP.put('\u00D9', 'U'); ACCENT_MAP.put('\u00DA', 'U'); ACCENT_MAP.put('\u00DB', 'U'); ACCENT_MAP.put('\u00DC', 'U');
        ACCENT_MAP.put('\u00F9', 'u'); ACCENT_MAP.put('\u00FA', 'u'); ACCENT_MAP.put('\u00FB', 'u'); ACCENT_MAP.put('\u00FC', 'u');
        
        ACCENT_MAP.put('\u00DD', 'Y'); ACCENT_MAP.put('\u00FD', 'y'); ACCENT_MAP.put('\u00FF', 'y');
        
        ACCENT_MAP.put('\u00D1', 'N'); ACCENT_MAP.put('\u00F1', 'n');
        ACCENT_MAP.put('\u00C7', 'C'); ACCENT_MAP.put('\u00E7', 'c');
        ACCENT_MAP.put('\u00DF', 's');
        
        // Common problematic characters → replacement
        ACCENT_MAP.put('&', '+'); // Ampersand → Plus
        ACCENT_MAP.put('@', 'A'); // At → A
        ACCENT_MAP.put('#', ' '); // Hash → Space
        ACCENT_MAP.put('$', 'S'); // Dollar → S
        ACCENT_MAP.put('%', ' '); // Percent → Space
        ACCENT_MAP.put('*', ' '); // Asterisk → Space
        ACCENT_MAP.put('_', ' '); // Underscore → Space
        ACCENT_MAP.put('=', ' '); // Equals → Space
        ACCENT_MAP.put('|', '/'); // Pipe → Slash
        ACCENT_MAP.put('\\', '/'); // Backslash → Slash
    }
    
    /**
     * Sanitize input string to SWIFT X-Character Set.
     * 
     * <p>Performs the following transformations:</p>
     * <ol>
     *   <li>Converts lowercase to uppercase</li>
     *   <li>Replaces accented characters with ASCII equivalents</li>
     *   <li>Replaces invalid special characters with safe alternatives</li>
     *   <li>Removes emoji and non-printable characters</li>
     *   <li>Trims leading/trailing whitespace</li>
     *   <li>Collapses multiple spaces to single space</li>
     * </ol>
     * 
     * @param input Raw input string (may contain invalid characters)
     * @return SWIFT-safe string conforming to X-Character Set
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder(input.length());
        int invalidCharCount = 0;
        
        for (char c : input.toCharArray()) {
            // Step 1: Convert lowercase to uppercase
            if (Character.isLowerCase(c)) {
                c = Character.toUpperCase(c);
            }
            
            // Step 2: Replace accented/special characters
            if (ACCENT_MAP.containsKey(c)) {
                char replacement = ACCENT_MAP.get(c);
                result.append(Character.toUpperCase(replacement));
                invalidCharCount++;
                continue;
            }
            
            // Step 3: Validate against SWIFT character set
            String charStr = String.valueOf(c);
            if (SWIFT_VALID_CHARS.matcher(charStr).matches()) {
                result.append(c);
            } else {
                // Step 4: Invalid character → replace with space
                result.append(' ');
                invalidCharCount++;
                LOGGER.debug("Replaced invalid SWIFT character: '{}' (U+{}) with space",
                    c, String.format("%04X", (int) c));
            }
        }
        
        // Step 5: Normalize whitespace (collapse multiple spaces)
        String sanitized = result.toString().replaceAll("\\s+", " ").trim();
        
        if (invalidCharCount > 0) {
            LOGGER.warn("Sanitized SWIFT string: {} invalid character(s) replaced. " +
                "Original length: {}, Final length: {}",
                invalidCharCount, input.length(), sanitized.length());
        }
        
        return sanitized;
    }
    
    /**
     * Validate if string conforms to SWIFT X-Character Set.
     * 
     * <p>Strict validation - returns false if ANY invalid character is present.
     * Use this for pre-flight validation before sending to SWIFT network.</p>
     * 
     * @param input String to validate
     * @return true if all characters are SWIFT-valid, false otherwise
     */
    public static boolean isValid(String input) {
        if (input == null || input.isEmpty()) {
            return true; // Empty strings are valid
        }
        
        for (char c : input.toCharArray()) {
            String charStr = String.valueOf(c);
            if (!SWIFT_VALID_CHARS.matcher(charStr).matches()) {
                LOGGER.debug("Invalid SWIFT character detected: '{}' (U+{}) at position {}",
                    c, String.format("%04X", (int) c), input.indexOf(c));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get list of invalid characters in string.
     * 
     * <p>Useful for validation error messages to show user exactly what needs fixing.</p>
     * 
     * @param input String to analyze
     * @return Map of invalid character → position(s) in string
     */
    public static Map<Character, Integer> getInvalidCharacters(String input) {
        Map<Character, Integer> invalidChars = new HashMap<>();
        
        if (input == null || input.isEmpty()) {
            return invalidChars;
        }
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String charStr = String.valueOf(c);
            if (!SWIFT_VALID_CHARS.matcher(charStr).matches()) {
                invalidChars.put(c, i);
            }
        }
        
        return invalidChars;
    }
    
    /**
     * Sanitize specific SWIFT field with field-specific rules.
     * 
     * <p>Some SWIFT fields have additional restrictions beyond the X-Character Set:</p>
     * <ul>
     *   <li>Tag 20 (Reference): Max 16 alphanumeric</li>
     *   <li>Tag 50K (Ordering Customer): Max 4 lines x 35 chars</li>
     *   <li>Tag 59 (Beneficiary): Max 4 lines x 35 chars</li>
     * </ul>
     * 
     * @param input Raw field value
     * @param fieldTag SWIFT field tag (e.g., "20", "50K", "59")
     * @return Sanitized and truncated field value
     */
    public static String sanitizeField(String input, String fieldTag) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        // First apply general sanitization
        String sanitized = sanitize(input);
        
        // Apply field-specific rules
        switch (fieldTag) {
            case "20": // Transaction Reference
                // Max 16 alphanumeric characters
                sanitized = sanitized.replaceAll("[^A-Z0-9]", "");
                if (sanitized.length() > 16) {
                    LOGGER.warn("Field :20: truncated from {} to 16 characters", sanitized.length());
                    sanitized = sanitized.substring(0, 16);
                }
                break;
                
            case "50K": // Ordering Customer
            case "59":  // Beneficiary
                // Max 4 lines x 35 characters per line
                String[] lines = sanitized.split("\n");
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < Math.min(lines.length, 4); i++) {
                    String line = lines[i];
                    if (line.length() > 35) {
                        LOGGER.warn("Field :{}: line {} truncated from {} to 35 characters",
                            fieldTag, i + 1, line.length());
                        line = line.substring(0, 35);
                    }
                    result.append(line);
                    if (i < Math.min(lines.length, 4) - 1) {
                        result.append("\n");
                    }
                }
                sanitized = result.toString();
                break;
                
            case "70": // Remittance Information
                // Max 4 lines x 35 characters per line
                sanitized = truncateMultilineField(sanitized, 4, 35);
                break;
                
            default:
                // No additional field-specific rules
                break;
        }
        
        return sanitized;
    }
    
    /**
     * Truncate multi-line field to SWIFT line limits.
     * 
     * @param input Multi-line text
     * @param maxLines Maximum number of lines
     * @param maxCharsPerLine Maximum characters per line
     * @return Truncated text
     */
    private static String truncateMultilineField(String input, int maxLines, int maxCharsPerLine) {
        String[] lines = input.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
            String line = lines[i];
            if (line.length() > maxCharsPerLine) {
                line = line.substring(0, maxCharsPerLine);
            }
            result.append(line);
            if (i < Math.min(lines.length, maxLines) - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
}


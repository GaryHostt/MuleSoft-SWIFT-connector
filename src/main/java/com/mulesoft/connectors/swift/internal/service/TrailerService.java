package com.mulesoft.connectors.swift.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * TrailerService - Generates and validates SWIFT Block 5 (Trailer)
 * 
 * Responsible for:
 * - Calculating CHK (Checksum) using SHA-256
 * - Calculating MAC (Message Authentication Code) using HMAC-SHA256
 * - Appending Block 5 to message content
 * - Validating incoming Block 5 trailers
 */
public class TrailerService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TrailerService.class);
    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final Pattern BLOCK5_PATTERN = Pattern.compile("\\{5:(.+?)\\}\\}$", Pattern.DOTALL);
    
    private final String bilateralKey;
    
    public TrailerService(String bilateralKey) {
        this.bilateralKey = bilateralKey;
    }
    
    /**
     * Append Block 5 trailer to message
     * 
     * @param messageContent Original message (Blocks 1-4)
     * @return Message with Block 5 appended
     */
    public String appendTrailer(String messageContent) {
        LOGGER.debug("Appending Block 5 trailer to message");
        
        // Remove any existing Block 5
        String messageWithoutTrailer = removeTrailer(messageContent);
        
        // Calculate CHK (Checksum)
        String checksum = calculateChecksum(messageWithoutTrailer);
        
        // Calculate MAC (Message Authentication Code)
        String mac = calculateMAC(messageWithoutTrailer);
        
        // Build Block 5
        String block5 = String.format("{5:{MAC:%s}{CHK:%s}}", mac, checksum);
        
        // Append to message
        String completeMessage = messageWithoutTrailer + block5;
        
        LOGGER.debug("Block 5 appended - CHK: {}, MAC: {}", checksum, mac);
        
        return completeMessage;
    }
    
    /**
     * Validate Block 5 trailer in incoming message
     * 
     * @param messageContent Message with Block 5
     * @return ValidationResult with status and details
     */
    public ValidationResult validateTrailer(String messageContent) {
        LOGGER.debug("Validating Block 5 trailer");
        
        // Extract Block 5
        Matcher matcher = BLOCK5_PATTERN.matcher(messageContent);
        if (!matcher.find()) {
            return new ValidationResult(false, "Missing Block 5 trailer");
        }
        
        String block5Content = matcher.group(1);
        
        // Extract MAC and CHK
        Pattern macPattern = Pattern.compile("\\{MAC:([A-F0-9]+)\\}");
        Pattern chkPattern = Pattern.compile("\\{CHK:([A-F0-9]+)\\}");
        
        Matcher macMatcher = macPattern.matcher(block5Content);
        Matcher chkMatcher = chkPattern.matcher(block5Content);
        
        if (!macMatcher.find() || !chkMatcher.find()) {
            return new ValidationResult(false, "Invalid Block 5 format");
        }
        
        String providedMAC = macMatcher.group(1);
        String providedCHK = chkMatcher.group(1);
        
        // Remove Block 5 and recalculate
        String messageWithoutTrailer = removeTrailer(messageContent);
        String expectedCHK = calculateChecksum(messageWithoutTrailer);
        String expectedMAC = calculateMAC(messageWithoutTrailer);
        
        // Validate CHK
        if (!expectedCHK.equals(providedCHK)) {
            return new ValidationResult(false, 
                String.format("Checksum mismatch - Expected: %s, Got: %s", expectedCHK, providedCHK));
        }
        
        // Validate MAC
        if (!expectedMAC.equals(providedMAC)) {
            return new ValidationResult(false, 
                String.format("MAC mismatch - Expected: %s, Got: %s", expectedMAC, providedMAC));
        }
        
        LOGGER.debug("Block 5 validation successful");
        return new ValidationResult(true, "Valid");
    }
    
    /**
     * Calculate SHA-256 checksum
     */
    private String calculateChecksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            
            // Take first 12 characters in hex
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(6, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }
    
    /**
     * Calculate HMAC-SHA256 MAC
     */
    private String calculateMAC(String content) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(bilateralKey.getBytes(), MAC_ALGORITHM);
            mac.init(secretKey);
            
            byte[] hmac = mac.doFinal(content.getBytes());
            
            // Take first 16 characters in hex
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hmac.length); i++) {
                String hex = Integer.toHexString(0xff & hmac[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            LOGGER.error("Failed to calculate MAC", e);
            throw new RuntimeException("Failed to calculate MAC", e);
        }
    }
    
    /**
     * Remove Block 5 from message
     */
    private String removeTrailer(String messageContent) {
        return BLOCK5_PATTERN.matcher(messageContent).replaceAll("");
    }
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}


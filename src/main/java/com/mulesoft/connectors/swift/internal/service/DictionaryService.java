package com.mulesoft.connectors.swift.internal.service;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DictionaryService - External configuration for SWIFT reject codes and mappings
 * 
 * Maintains reject code mappings that can be updated without recompiling.
 * Supports loading from Object Store, external files, or database.
 */
public class DictionaryService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryService.class);
    private static final String REJECT_CODES_KEY = "swift.dictionary.reject.codes";
    
    private final ObjectStore<Serializable> objectStore;
    private final Map<String, RejectCodeDefinition> rejectCodes;
    
    public DictionaryService(ObjectStore<Serializable> objectStore) {
        this.objectStore = objectStore;
        this.rejectCodes = new ConcurrentHashMap<>();
        
        // Load reject codes from Object Store or defaults
        loadRejectCodes();
    }
    
    /**
     * Load reject codes from Object Store or initialize with defaults
     */
    @SuppressWarnings("unchecked")
    private void loadRejectCodes() {
        try {
            // Try to load from Object Store first
            Map<String, RejectCodeDefinition> stored = 
                (Map<String, RejectCodeDefinition>) objectStore.retrieve(REJECT_CODES_KEY);
            
            if (stored != null) {
                rejectCodes.putAll(stored);
                LOGGER.info("Loaded {} reject codes from Object Store", stored.size());
                return;
            }
        } catch (ObjectStoreException e) {
            LOGGER.debug("No reject codes in Object Store, loading defaults");
        }
        
        // Load default reject codes (SWIFT standards)
        initializeDefaultRejectCodes();
        
        // Persist defaults to Object Store
        persistRejectCodes();
    }
    
    /**
     * Initialize default SWIFT reject codes per standards
     */
    private void initializeDefaultRejectCodes() {
        // K-series errors (network/system errors) - TERMINAL
        addRejectCode("K90", "Message incomplete", Severity.TERMINAL, true, 
            "Message format incomplete or corrupted");
        addRejectCode("K08", "Incorrect delivery monitoring", Severity.TERMINAL, true,
            "Delivery monitoring field incorrect");
        addRejectCode("K10", "Message too long", Severity.TERMINAL, true,
            "Message exceeds maximum length");
        
        // D-series errors (field validation) - TERMINAL
        addRejectCode("D01", "Invalid field", Severity.TERMINAL, true,
            "Field content validation failed");
        addRejectCode("D02", "Field sequence error", Severity.TERMINAL, true,
            "Fields in incorrect sequence");
        addRejectCode("D03", "Conditional field missing", Severity.TERMINAL, true,
            "Required conditional field not present");
        
        // T-series errors (test/training) - RETRYABLE
        addRejectCode("T12", "Test code word missing", Severity.RETRYABLE, false,
            "Test/training message missing required code word");
        
        // C-series errors (cutoff/business) - BUSINESS
        addRejectCode("C05", "Cutoff time exceeded", Severity.BUSINESS, false,
            "Payment submitted after cutoff time");
        
        // S-series errors (security) - SECURITY
        addRejectCode("S01", "Authentication failed", Severity.SECURITY, true,
            "LAU authentication verification failed");
        addRejectCode("S02", "Incorrect MAC", Severity.SECURITY, true,
            "Message Authentication Code validation failed");
        
        // N-series errors (network acknowledgment) - NETWORK
        addRejectCode("N01", "Duplicate message", Severity.NETWORK, false,
            "Message already received (duplicate detection)");
        addRejectCode("N02", "Sequence number error", Severity.NETWORK, true,
            "Message sequence number out of order");
        
        LOGGER.info("Initialized {} default reject codes", rejectCodes.size());
    }
    
    /**
     * Add or update reject code definition
     */
    public void addRejectCode(String code, String description, Severity severity, 
                               boolean terminal, String remediationGuidance) {
        RejectCodeDefinition definition = new RejectCodeDefinition(
            code, description, severity, terminal, remediationGuidance
        );
        rejectCodes.put(code, definition);
    }
    
    /**
     * Get reject code definition
     */
    public Optional<RejectCodeDefinition> getRejectCode(String code) {
        return Optional.ofNullable(rejectCodes.get(code));
    }
    
    /**
     * Check if reject code is terminal (should fail the flow)
     */
    public boolean isTerminal(String code) {
        return getRejectCode(code)
            .map(RejectCodeDefinition::isTerminal)
            .orElse(true); // Unknown codes are treated as terminal for safety
    }
    
    /**
     * Get all reject codes
     */
    public Map<String, RejectCodeDefinition> getAllRejectCodes() {
        return Collections.unmodifiableMap(rejectCodes);
    }
    
    /**
     * Update reject code definition (hot-reload without recompile)
     */
    public void updateRejectCode(String code, RejectCodeDefinition definition) {
        rejectCodes.put(code, definition);
        persistRejectCodes();
        LOGGER.info("Updated reject code: {} - {}", code, definition.getDescription());
    }
    
    /**
     * Persist reject codes to Object Store
     */
    private void persistRejectCodes() {
        try {
            objectStore.store(REJECT_CODES_KEY, (Serializable) new HashMap<>(rejectCodes));
            LOGGER.debug("Persisted {} reject codes to Object Store", rejectCodes.size());
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to persist reject codes", e);
        }
    }
    
    /**
     * Reload reject codes from external source (e.g., after standards update)
     */
    public void reload() {
        rejectCodes.clear();
        loadRejectCodes();
        LOGGER.info("Reloaded reject codes");
    }
    
    /**
     * Reject code definition
     */
    public static class RejectCodeDefinition implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String code;
        private final String description;
        private final Severity severity;
        private final boolean terminal;
        private final String remediationGuidance;
        
        public RejectCodeDefinition(String code, String description, Severity severity, 
                                   boolean terminal, String remediationGuidance) {
            this.code = code;
            this.description = description;
            this.severity = severity;
            this.terminal = terminal;
            this.remediationGuidance = remediationGuidance;
        }
        
        // Getters
        public String getCode() { return code; }
        public String getDescription() { return description; }
        public Severity getSeverity() { return severity; }
        public boolean isTerminal() { return terminal; }
        public String getRemediationGuidance() { return remediationGuidance; }
    }
    
    /**
     * Reject code severity levels
     */
    public enum Severity {
        TERMINAL,      // Fatal error, cannot be retried
        RETRYABLE,     // Temporary error, retry possible
        BUSINESS,      // Business rule violation
        SECURITY,      // Security/authentication failure
        NETWORK        // Network/protocol issue
    }
}


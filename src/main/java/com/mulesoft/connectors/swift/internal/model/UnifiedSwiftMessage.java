package com.mulesoft.connectors.swift.internal.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified SWIFT Message Model
 * 
 * <p>Provides a format-agnostic representation of SWIFT messages, abstracting
 * the differences between MT (FIN) and MX (ISO 20022) formats.</p>
 * 
 * <h2>Purpose</h2>
 * <p>As SWIFT migrates from MT to MX (ISO 20022), integration developers need
 * to support BOTH formats simultaneously. This unified model allows a single
 * Mule flow to handle both MT103 and pacs.008 (the MX equivalent) without
 * conditional logic.</p>
 * 
 * <h2>Field Mapping</h2>
 * <table>
 *   <tr><th>Unified Field</th><th>MT Source</th><th>MX Source</th></tr>
 *   <tr><td>messageType</td><td>MT103</td><td>pacs.008.001.08</td></tr>
 *   <tr><td>format</td><td>MT</td><td>MX</td></tr>
 *   <tr><td>sender</td><td>:50A/K:</td><td>&lt;DbtrAgt&gt;&lt;BIC&gt;</td></tr>
 *   <tr><td>receiver</td><td>:59:</td><td>&lt;CdtrAgt&gt;&lt;BIC&gt;</td></tr>
 *   <tr><td>amount</td><td>:32A: (numeric part)</td><td>&lt;IntrBkSttlmAmt&gt;</td></tr>
 *   <tr><td>currency</td><td>:32A: (CCY part)</td><td>&lt;IntrBkSttlmAmt Ccy="..."&gt;</td></tr>
 *   <tr><td>reference</td><td>:20:</td><td>&lt;EndToEndId&gt;</td></tr>
 *   <tr><td>uetr</td><td>Block 3 :121:</td><td>&lt;UETR&gt;</td></tr>
 *   <tr><td>valueDate</td><td>:32A: (date part)</td><td>&lt;IntrBkSttlmDt&gt;</td></tr>
 * </table>
 * 
 * <h2>DataWeave Usage</h2>
 * <pre>{@code
 * %dw 2.0
 * output application/json
 * ---
 * {
 *   // These fields work REGARDLESS of whether input was MT or MX
 *   transactionId: payload.reference,
 *   fromBic: payload.sender,
 *   toBic: payload.receiver,
 *   paymentAmount: payload.amount as Number,
 *   paymentCurrency: payload.currency,
 *   trackingId: payload.uetr,
 *   
 *   // Conditional logic based on format (if needed)
 *   originalFormat: payload.format,
 *   isLegacyMT: payload.format == "MT"
 * }
 * }</pre>
 * 
 * @see com.mulesoft.connectors.swift.internal.operation.UnifiedParsingOperations
 */
public class UnifiedSwiftMessage {
    
    // Core identification
    private String messageType;      // MT103, pacs.008.001.08, etc.
    private MessageFormat format;    // MT or MX
    private String reference;        // Transaction reference (:20: or <EndToEndId>)
    private String uetr;            // Unique End-to-End Transaction Reference (gpi)
    
    // Party information
    private String sender;          // Ordering customer BIC
    private String receiver;        // Beneficiary BIC
    
    // Payment details
    private String amount;          // Settlement amount (as string to preserve precision)
    private String currency;        // ISO 4217 currency code (USD, EUR, etc.)
    private String valueDate;       // Value date (YYMMDD format)
    
    // Technical metadata
    private String rawContent;      // Original message content (for audit)
    private long parseTimestamp;    // When message was parsed (epoch millis)
    
    // Additional fields (extensible)
    private Map<String, String> additionalFields;
    
    public UnifiedSwiftMessage() {
        this.parseTimestamp = System.currentTimeMillis();
        this.additionalFields = new HashMap<>();
    }
    
    // ========== Getters and Setters ==========
    
    public String getMessageType() {
        return messageType;
    }
    
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    public MessageFormat getFormat() {
        return format;
    }
    
    public void setFormat(MessageFormat format) {
        this.format = format;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public String getUetr() {
        return uetr;
    }
    
    public void setUetr(String uetr) {
        this.uetr = uetr;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getReceiver() {
        return receiver;
    }
    
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public void setAmount(String amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getValueDate() {
        return valueDate;
    }
    
    public void setValueDate(String valueDate) {
        this.valueDate = valueDate;
    }
    
    public String getRawContent() {
        return rawContent;
    }
    
    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }
    
    public long getParseTimestamp() {
        return parseTimestamp;
    }
    
    public void setParseTimestamp(long parseTimestamp) {
        this.parseTimestamp = parseTimestamp;
    }
    
    public Map<String, String> getAdditionalFields() {
        return additionalFields;
    }
    
    public void setAdditionalFields(Map<String, String> additionalFields) {
        this.additionalFields = additionalFields;
    }
    
    /**
     * Add an additional field (for message-type-specific data).
     * 
     * @param key Field key
     * @param value Field value
     */
    public void addAdditionalField(String key, String value) {
        this.additionalFields.put(key, value);
    }
    
    /**
     * Get an additional field value.
     * 
     * @param key Field key
     * @return Field value or null if not found
     */
    public String getAdditionalField(String key) {
        return this.additionalFields.get(key);
    }
    
    /**
     * Check if this is an MT (FIN) message.
     * 
     * @return true if format is MT
     */
    public boolean isMtMessage() {
        return format == MessageFormat.MT;
    }
    
    /**
     * Check if this is an MX (ISO 20022) message.
     * 
     * @return true if format is MX
     */
    public boolean isMxMessage() {
        return format == MessageFormat.MX;
    }
    
    /**
     * Get a human-readable summary of the message.
     * 
     * @return Summary string
     */
    public String getSummary() {
        return String.format("%s %s: %s %s from %s to %s (ref: %s)",
            format, messageType, amount, currency, sender, receiver, reference);
    }
    
    @Override
    public String toString() {
        return "UnifiedSwiftMessage{" +
                "messageType='" + messageType + '\'' +
                ", format=" + format +
                ", reference='" + reference + '\'' +
                ", uetr='" + uetr + '\'' +
                ", sender='" + sender + '\'' +
                ", receiver='" + receiver + '\'' +
                ", amount='" + amount + '\'' +
                ", currency='" + currency + '\'' +
                ", valueDate='" + valueDate + '\'' +
                ", parseTimestamp=" + parseTimestamp +
                ", additionalFields=" + additionalFields.size() +
                '}';
    }
}


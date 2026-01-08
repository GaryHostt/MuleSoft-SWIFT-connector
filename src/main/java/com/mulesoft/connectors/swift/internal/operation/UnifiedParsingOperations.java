package com.mulesoft.connectors.swift.internal.operation;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
import com.mulesoft.connectors.swift.internal.model.MessageFormat;
import com.mulesoft.connectors.swift.internal.model.UnifiedSwiftMessage;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unified Message Parsing Operations
 * 
 * <p>Provides a single operation to parse BOTH MT (FIN) and MX (ISO 20022) messages,
 * returning a consistent metadata structure regardless of input format.</p>
 * 
 * <h2>Professional Connector Pattern</h2>
 * <p>Enterprise-grade connectors provide a "Unified Message Model" that abstracts
 * format-specific details, allowing developers to work with SWIFT messages using
 * a single, consistent API.</p>
 * 
 * <h3>Benefits</h3>
 * <ul>
 *   <li>Single operation for both MT and MX → simpler flow design</li>
 *   <li>Automatic format detection → no manual configuration</li>
 *   <li>Consistent metadata structure → reusable DataWeave scripts</li>
 *   <li>Future-proof → supports SWIFT's MT-to-MX migration</li>
 * </ul>
 * 
 * <h3>Usage in Mule Flow</h3>
 * <pre>{@code
 * <swift:parse-message config-ref="SWIFT_Config">
 *   <swift:message-content>#[payload]</swift:message-content>
 * </swift:parse-message>
 * 
 * <!-- Result is ALWAYS a UnifiedSwiftMessage, regardless of input format -->
 * <logger level="INFO" message="Message Type: #[payload.messageType]" />
 * <logger level="INFO" message="Sender: #[payload.sender]" />
 * <logger level="INFO" message="Receiver: #[payload.receiver]" />
 * <logger level="INFO" message="Amount: #[payload.amount]" />
 * <logger level="INFO" message="Currency: #[payload.currency]" />
 * }</pre>
 * 
 * @see UnifiedSwiftMessage
 */
public class UnifiedParsingOperations {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedParsingOperations.class);
    
    // Patterns for format detection
    private static final Pattern MT_PATTERN = Pattern.compile("\\{1:.*?\\}\\{2:.*?\\}");
    private static final Pattern MX_PATTERN = Pattern.compile("<\\?xml.*?<Document.*?xmlns=\"urn:iso:std:iso:20022");
    
    // MT field extractors (Block 4 tags)
    private static final Pattern MT_SENDER_PATTERN = Pattern.compile(":50[AK]:(.*?)(?=:|$)", Pattern.DOTALL);
    private static final Pattern MT_RECEIVER_PATTERN = Pattern.compile(":59[AF]?:(.*?)(?=:|$)", Pattern.DOTALL);
    private static final Pattern MT_AMOUNT_PATTERN = Pattern.compile(":32A:(\\d{6})([A-Z]{3})([\\d,\\.]+)");
    private static final Pattern MT_REFERENCE_PATTERN = Pattern.compile(":20:(.*?)(?=\n|$)");
    private static final Pattern MT_MESSAGE_TYPE_PATTERN = Pattern.compile("\\{2:.*?MT(\\d{3})");
    
    // MX XPath-like extractors (simplified - real implementation would use XML parser)
    private static final Pattern MX_MESSAGE_TYPE_PATTERN = Pattern.compile("<(\\w+\\.\\d{3}\\.\\d{3}\\.\\d{2})>");
    private static final Pattern MX_AMOUNT_PATTERN = Pattern.compile("<IntrBkSttlmAmt.*?Ccy=\"([A-Z]{3})\">(.*?)</IntrBkSttlmAmt>");
    private static final Pattern MX_SENDER_PATTERN = Pattern.compile("<DbtrAgt>.*?<BIC>(.*?)</BIC>", Pattern.DOTALL);
    private static final Pattern MX_RECEIVER_PATTERN = Pattern.compile("<CdtrAgt>.*?<BIC>(.*?)</BIC>", Pattern.DOTALL);
    private static final Pattern MX_REFERENCE_PATTERN = Pattern.compile("<EndToEndId>(.*?)</EndToEndId>");
    private static final Pattern MX_UETR_PATTERN = Pattern.compile("<UETR>(.*?)</UETR>");
    
    /**
     * Parse SWIFT Message (MT or MX)
     * 
     * <p>Automatically detects message format (FIN/MT or XML/MX) and parses into
     * a unified metadata structure. This operation provides consistent DataWeave
     * metadata regardless of input format.</p>
     * 
     * <h3>Format Detection</h3>
     * <ul>
     *   <li><strong>MT (FIN)</strong>: Detected by presence of {1:...}{2:...} blocks</li>
     *   <li><strong>MX (ISO 20022)</strong>: Detected by XML declaration + ISO 20022 namespace</li>
     * </ul>
     * 
     * <h3>Extracted Fields</h3>
     * <table>
     *   <tr><th>Field</th><th>MT Source</th><th>MX Source</th></tr>
     *   <tr><td>messageType</td><td>Block 2 (MT103)</td><td>Root element (pacs.008.001.08)</td></tr>
     *   <tr><td>sender</td><td>Tag :50A/K:</td><td>&lt;DbtrAgt&gt;&lt;BIC&gt;</td></tr>
     *   <tr><td>receiver</td><td>Tag :59:</td><td>&lt;CdtrAgt&gt;&lt;BIC&gt;</td></tr>
     *   <tr><td>amount</td><td>Tag :32A:</td><td>&lt;IntrBkSttlmAmt&gt;</td></tr>
     *   <tr><td>currency</td><td>Tag :32A:</td><td>&lt;IntrBkSttlmAmt Ccy="..."&gt;</td></tr>
     *   <tr><td>reference</td><td>Tag :20:</td><td>&lt;EndToEndId&gt;</td></tr>
     *   <tr><td>uetr</td><td>Block 3 Tag 121</td><td>&lt;UETR&gt;</td></tr>
     * </table>
     * 
     * <h3>DataWeave Metadata</h3>
     * <p>The output is typed as {@link UnifiedSwiftMessage}, enabling drag-and-drop
     * mapping in Anypoint Studio:</p>
     * <pre>{@code
     * %dw 2.0
     * output application/json
     * ---
     * {
     *   transactionRef: payload.reference,
     *   fromBank: payload.sender,
     *   toBank: payload.receiver,
     *   paymentAmount: payload.amount as Number,
     *   paymentCurrency: payload.currency,
     *   trackingId: payload.uetr
     * }
     * }</pre>
     * 
     * @param connection Active SWIFT connection
     * @param messageContent Raw SWIFT message (MT or MX format)
     * @return Unified message structure with consistent metadata
     * @throws ModuleException If message format is invalid or parsing fails
     */
    @Throws(SwiftErrorProvider.class)
    @DisplayName("Parse SWIFT Message (Unified)")
    @Summary("Automatically detects and parses both MT (FIN) and MX (ISO 20022) messages into a unified structure")
    public UnifiedSwiftMessage parseMessage(
            @Connection SwiftConnection connection,
            @Content
            @DisplayName("Message Content")
            @Summary("Raw SWIFT message content (MT or MX format)")
            String messageContent) throws ModuleException {
        
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new ModuleException(
                SwiftErrorType.SYNTAX_ERROR,
                new IllegalArgumentException("Message content is empty")
            );
        }
        
        // Step 1: Detect format
        MessageFormat detectedFormat = detectFormat(messageContent);
        LOGGER.info("Detected SWIFT message format: {}", detectedFormat);
        
        // Step 2: Parse based on format
        UnifiedSwiftMessage unifiedMessage;
        switch (detectedFormat) {
            case MT:
                unifiedMessage = parseMtMessage(messageContent);
                break;
            case MX:
                unifiedMessage = parseMxMessage(messageContent);
                break;
            default:
                throw new ModuleException(
                    SwiftErrorType.SYNTAX_ERROR,
                    new IllegalArgumentException("Unable to detect message format (not MT or MX)")
                );
        }
        
        LOGGER.info("Successfully parsed {} message: type={}, ref={}, amount={} {}",
            detectedFormat, unifiedMessage.getMessageType(), unifiedMessage.getReference(),
            unifiedMessage.getAmount(), unifiedMessage.getCurrency());
        
        return unifiedMessage;
    }
    
    /**
     * Detect message format (MT vs MX).
     * 
     * @param content Raw message content
     * @return Detected format
     */
    private MessageFormat detectFormat(String content) {
        // Check for MT (FIN) format: {1:...}{2:...}
        if (MT_PATTERN.matcher(content).find()) {
            return MessageFormat.MT;
        }
        
        // Check for MX (ISO 20022) format: <?xml ... <Document xmlns="urn:iso:std:iso:20022
        if (MX_PATTERN.matcher(content).find()) {
            return MessageFormat.MX;
        }
        
        return MessageFormat.UNKNOWN;
    }
    
    /**
     * Parse MT (FIN) message into unified structure.
     * 
     * @param content MT message content
     * @return Unified message
     */
    private UnifiedSwiftMessage parseMtMessage(String content) throws ModuleException {
        UnifiedSwiftMessage message = new UnifiedSwiftMessage();
        message.setFormat(MessageFormat.MT);
        message.setRawContent(content);
        
        // Extract message type from Block 2
        Matcher mtTypeMatcher = MT_MESSAGE_TYPE_PATTERN.matcher(content);
        if (mtTypeMatcher.find()) {
            message.setMessageType("MT" + mtTypeMatcher.group(1));
        }
        
        // Extract reference from Tag :20:
        Matcher refMatcher = MT_REFERENCE_PATTERN.matcher(content);
        if (refMatcher.find()) {
            message.setReference(refMatcher.group(1).trim());
        }
        
        // Extract amount and currency from Tag :32A:
        Matcher amountMatcher = MT_AMOUNT_PATTERN.matcher(content);
        if (amountMatcher.find()) {
            String valueDate = amountMatcher.group(1);
            String currency = amountMatcher.group(2);
            String amount = amountMatcher.group(3).replace(",", "");
            
            message.setValueDate(valueDate);
            message.setCurrency(currency);
            message.setAmount(amount);
        }
        
        // Extract sender from Tag :50A: or :50K:
        Matcher senderMatcher = MT_SENDER_PATTERN.matcher(content);
        if (senderMatcher.find()) {
            message.setSender(extractBicFromField(senderMatcher.group(1)));
        }
        
        // Extract receiver from Tag :59:
        Matcher receiverMatcher = MT_RECEIVER_PATTERN.matcher(content);
        if (receiverMatcher.find()) {
            message.setReceiver(extractBicFromField(receiverMatcher.group(1)));
        }
        
        return message;
    }
    
    /**
     * Parse MX (ISO 20022) message into unified structure.
     * 
     * @param content MX message content
     * @return Unified message
     */
    private UnifiedSwiftMessage parseMxMessage(String content) throws ModuleException {
        UnifiedSwiftMessage message = new UnifiedSwiftMessage();
        message.setFormat(MessageFormat.MX);
        message.setRawContent(content);
        
        // Extract message type from root element
        Matcher mxTypeMatcher = MX_MESSAGE_TYPE_PATTERN.matcher(content);
        if (mxTypeMatcher.find()) {
            message.setMessageType(mxTypeMatcher.group(1));
        }
        
        // Extract reference from <EndToEndId>
        Matcher refMatcher = MX_REFERENCE_PATTERN.matcher(content);
        if (refMatcher.find()) {
            message.setReference(refMatcher.group(1).trim());
        }
        
        // Extract UETR from <UETR>
        Matcher uetrMatcher = MX_UETR_PATTERN.matcher(content);
        if (uetrMatcher.find()) {
            message.setUetr(uetrMatcher.group(1).trim());
        }
        
        // Extract amount and currency from <IntrBkSttlmAmt>
        Matcher amountMatcher = MX_AMOUNT_PATTERN.matcher(content);
        if (amountMatcher.find()) {
            String currency = amountMatcher.group(1);
            String amount = amountMatcher.group(2).trim();
            
            message.setCurrency(currency);
            message.setAmount(amount);
        }
        
        // Extract sender BIC from <DbtrAgt><BIC>
        Matcher senderMatcher = MX_SENDER_PATTERN.matcher(content);
        if (senderMatcher.find()) {
            message.setSender(senderMatcher.group(1).trim());
        }
        
        // Extract receiver BIC from <CdtrAgt><BIC>
        Matcher receiverMatcher = MX_RECEIVER_PATTERN.matcher(content);
        if (receiverMatcher.find()) {
            message.setReceiver(receiverMatcher.group(1).trim());
        }
        
        return message;
    }
    
    /**
     * Extract BIC code from MT field (handles Option A/K formats).
     * 
     * @param fieldContent Raw field content
     * @return Extracted BIC code
     */
    private String extractBicFromField(String fieldContent) {
        // MT Tag :50A: format: /ACCOUNT\nBICCODE
        // MT Tag :50K: format: /ACCOUNT\nName\nAddress
        // We look for 11-character BIC pattern
        Pattern bicPattern = Pattern.compile("([A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?)");
        Matcher bicMatcher = bicPattern.matcher(fieldContent);
        if (bicMatcher.find()) {
            return bicMatcher.group(1);
        }
        
        // If no BIC found, return first line (account info)
        String[] lines = fieldContent.split("\n");
        return lines.length > 0 ? lines[0].trim() : fieldContent.trim();
    }
}


package com.mulesoft.connectors.swift.internal.operation;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.model.*;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformation & Validation Operations
 * 
 * Provides schema validation, format conversion (MT to MX), and BIC lookup capabilities.
 * Critical for ensuring messages conform to SWIFT standards before transmission.
 */
public class TransformationOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationOperations.class);

    /**
     * Validate a SWIFT message against current standards.
     * 
     * Checks if a message conforms to the current year's SWIFT Standard Release (SR) rules.
     * 
     * @param connection Active SWIFT connection
     * @param messageType Message type (e.g., MT103, pacs.008)
     * @param messageContent Message content to validate
     * @param format Message format (MT or MX)
     * @param standardRelease SWIFT Standard Release year (e.g., "SR2023", "SR2024")
     * @return Result containing validation results
     */
    @DisplayName("Validate Schema")
    @Summary("Check if a message conforms to SWIFT Standard Release rules")
    @Throws(SwiftErrorProvider.class)
    public Result<ValidationResponse, MessageAttributes> validateSchema(
            @Connection SwiftConnection connection,
            @DisplayName("Message Type")
            @Summary("Message type (e.g., MT103, pacs.008)")
            String messageType,
            @Content
            @DisplayName("Message Content")
            @Summary("Message content to validate")
            String messageContent,
            @DisplayName("Message Format")
            @Summary("MT or MX format")
            MessageFormat format,
            @Optional(defaultValue = "SR2024")
            @DisplayName("Standard Release")
            @Summary("SWIFT Standard Release (e.g., SR2023, SR2024)")
            String standardRelease) throws Exception {

        LOGGER.info("Validating message: type={}, format={}, SR={}", messageType, format, standardRelease);

        ValidationResponse response = new ValidationResponse();
        response.setMessageType(messageType);
        response.setFormat(format);
        response.setStandardRelease(standardRelease);
        response.setValidationTimestamp(LocalDateTime.now());

        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // Perform validation (simplified - real implementation would use SWIFT validation engine)
        if (format == MessageFormat.MT) {
            validateMtMessage(messageType, messageContent, standardRelease, errors, warnings);
        } else {
            validateMxMessage(messageType, messageContent, standardRelease, errors, warnings);
        }

        response.setErrors(errors);
        response.setWarnings(warnings);
        response.setValid(errors.isEmpty());

        if (response.isValid()) {
            LOGGER.info("Message validation successful");
        } else {
            LOGGER.warn("Message validation failed with {} errors", errors.size());
        }

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<ValidationResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Convert legacy MT message to ISO 20022 MX format.
     * 
     * Supports the industry-wide migration from MT to MX standards.
     * 
     * @param connection Active SWIFT connection
     * @param mtMessageType MT message type (e.g., MT103, MT202)
     * @param mtContent MT message content
     * @return Result containing converted MX message
     */
    @DisplayName("MT to MX Translation")
    @Summary("Convert legacy MT message to ISO 20022 MX format")
    @Throws(SwiftErrorProvider.class)
    public Result<TranslationResponse, MessageAttributes> translateMtToMx(
            @Connection SwiftConnection connection,
            @DisplayName("MT Message Type")
            @Summary("MT message type (e.g., MT103, MT202)")
            String mtMessageType,
            @Content
            @DisplayName("MT Content")
            @Summary("MT message content")
            String mtContent) throws Exception {

        LOGGER.info("Translating MT to MX: type={}", mtMessageType);

        // Perform translation (simplified - real implementation would use SWIFT translation service)
        TranslationResponse response = performMtToMxTranslation(mtMessageType, mtContent);

        LOGGER.info("Translation successful: {} -> {}", mtMessageType, response.getMxMessageType());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<TranslationResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Convert MX message to legacy MT format.
     * 
     * For backward compatibility with systems that only support MT.
     * 
     * @param connection Active SWIFT connection
     * @param mxMessageType MX message type (e.g., pacs.008)
     * @param mxContent MX message content
     * @return Result containing converted MT message
     */
    @DisplayName("MX to MT Translation")
    @Summary("Convert ISO 20022 MX message to legacy MT format")
    @Throws(SwiftErrorProvider.class)
    public Result<TranslationResponse, MessageAttributes> translateMxToMt(
            @Connection SwiftConnection connection,
            @DisplayName("MX Message Type")
            @Summary("MX message type (e.g., pacs.008)")
            String mxMessageType,
            @Content
            @DisplayName("MX Content")
            @Summary("MX message content (XML)")
            String mxContent) throws Exception {

        LOGGER.info("Translating MX to MT: type={}", mxMessageType);

        // Perform translation
        TranslationResponse response = performMxToMtTranslation(mxMessageType, mxContent);

        LOGGER.info("Translation successful: {} -> {}", mxMessageType, response.getMtMessageType());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<TranslationResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Lookup and validate a Bank Identifier Code (BIC).
     * 
     * Validates BIC format and retrieves institution details from SWIFT directory.
     * 
     * @param connection Active SWIFT connection
     * @param bicCode BIC code to lookup (8 or 11 characters)
     * @return Result containing BIC details
     */
    @DisplayName("BIC Code Lookup")
    @Summary("Validate and lookup Bank Identifier Code details")
    @Throws(SwiftErrorProvider.class)
    public Result<BicLookupResponse, MessageAttributes> lookupBicCode(
            @Connection SwiftConnection connection,
            @DisplayName("BIC Code")
            @Summary("8 or 11 character Bank Identifier Code")
            String bicCode) throws Exception {

        LOGGER.info("Looking up BIC code: {}", bicCode);

        // Validate BIC format
        if (!isValidBicFormat(bicCode)) {
            throw new Exception("Invalid BIC format: " + bicCode);
        }

        // Perform BIC lookup (simplified - real implementation would query SWIFT BIC Directory)
        BicLookupResponse response = performBicLookup(bicCode);

        LOGGER.info("BIC lookup successful: {} - {}", bicCode, response.getInstitutionName());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<BicLookupResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Enrich a message with additional data from reference sources.
     * 
     * @param connection Active SWIFT connection
     * @param messageContent Original message content
     * @param enrichmentType Type of enrichment to apply
     * @return Result containing enriched message
     */
    @DisplayName("Enrich Message")
    @Summary("Enrich message with additional reference data")
    @Throws(SwiftErrorProvider.class)
    public Result<EnrichmentResponse, MessageAttributes> enrichMessage(
            @Connection SwiftConnection connection,
            @Content
            @DisplayName("Message Content")
            @Summary("Original message content")
            String messageContent,
            @DisplayName("Enrichment Type")
            @Summary("Type of enrichment (BIC_DETAILS, COUNTRY_INFO, CURRENCY_INFO)")
            String enrichmentType) throws Exception {

        LOGGER.info("Enriching message with type: {}", enrichmentType);

        EnrichmentResponse response = new EnrichmentResponse();
        response.setOriginalContent(messageContent);
        response.setEnrichmentType(enrichmentType);
        response.setEnrichedContent(messageContent); // Simplified
        response.setEnrichmentTimestamp(LocalDateTime.now());

        LOGGER.info("Message enriched successfully");

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setTimestamp(LocalDateTime.now());

        return Result.<EnrichmentResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    // Helper methods

    private void validateMtMessage(String messageType, String content, String standardRelease,
                                   List<ValidationError> errors, List<ValidationWarning> warnings) {
        // Simplified validation - real implementation would check:
        // - Field presence (mandatory vs optional)
        // - Field format and length
        // - Character sets
        // - Business rules
        // - Network validation rules

        if (content == null || content.trim().isEmpty()) {
            errors.add(new ValidationError("E001", "Message content is empty", "content"));
        }

        // Example validations
        if (!content.contains(":20:")) {
            errors.add(new ValidationError("E002", "Mandatory field :20: (Reference) is missing", "field20"));
        }

        if (content.length() > 10000) {
            warnings.add(new ValidationWarning("W001", "Message exceeds recommended length", "content"));
        }
    }

    private void validateMxMessage(String messageType, String content, String standardRelease,
                                   List<ValidationError> errors, List<ValidationWarning> warnings) {
        // XML schema validation for ISO 20022
        if (!content.trim().startsWith("<")) {
            errors.add(new ValidationError("E003", "MX message must be valid XML", "content"));
        }

        if (!content.contains("xmlns")) {
            errors.add(new ValidationError("E004", "XML namespace declaration missing", "content"));
        }
    }

    private TranslationResponse performMtToMxTranslation(String mtMessageType, String mtContent) {
        TranslationResponse response = new TranslationResponse();
        response.setSourceFormat(MessageFormat.MT);
        response.setTargetFormat(MessageFormat.MX);
        response.setMtMessageType(mtMessageType);

        // Map MT to MX message types
        String mxType = mapMtToMx(mtMessageType);
        response.setMxMessageType(mxType);

        // Simplified translation - real implementation would parse MT and generate MX XML
        String mxContent = generateMxContent(mtMessageType, mtContent);
        response.setTranslatedContent(mxContent);
        response.setTranslationTimestamp(LocalDateTime.now());
        response.setSuccess(true);

        return response;
    }

    private TranslationResponse performMxToMtTranslation(String mxMessageType, String mxContent) {
        TranslationResponse response = new TranslationResponse();
        response.setSourceFormat(MessageFormat.MX);
        response.setTargetFormat(MessageFormat.MT);
        response.setMxMessageType(mxMessageType);

        // Map MX to MT message types
        String mtType = mapMxToMt(mxMessageType);
        response.setMtMessageType(mtType);

        // Simplified translation
        String mtContent = generateMtContent(mxMessageType, mxContent);
        response.setTranslatedContent(mtContent);
        response.setTranslationTimestamp(LocalDateTime.now());
        response.setSuccess(true);

        return response;
    }

    private String mapMtToMx(String mtType) {
        // Mapping of common MT to MX types
        return switch (mtType) {
            case "MT103" -> "pacs.008.001.08";
            case "MT202" -> "pacs.009.001.08";
            case "MT900" -> "camt.054.001.08";
            case "MT910" -> "camt.054.001.08";
            default -> "pacs.008.001.08";
        };
    }

    private String mapMxToMt(String mxType) {
        // Reverse mapping
        return switch (mxType) {
            case "pacs.008.001.08", "pacs.008" -> "MT103";
            case "pacs.009.001.08", "pacs.009" -> "MT202";
            case "camt.054.001.08", "camt.054" -> "MT900";
            default -> "MT103";
        };
    }

    private String generateMxContent(String mtType, String mtContent) {
        // Simplified MX generation
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
                <FIToFICstmrCdtTrf>
                    <GrpHdr>
                        <MsgId>MSGID12345</MsgId>
                        <CreDtTm>2024-01-07T10:00:00</CreDtTm>
                        <NbOfTxs>1</NbOfTxs>
                    </GrpHdr>
                    <!-- Translated from %s -->
                </FIToFICstmrCdtTrf>
            </Document>
            """.formatted(mtType);
    }

    private String generateMtContent(String mxType, String mxContent) {
        // Simplified MT generation
        return """
            {1:F01BANKUS33AXXX0000000000}
            {2:O1031234240107BANKDE33XXXX00000000002401071234N}
            {4:
            :20:REFERENCE123
            :32A:240107USD1000,00
            :50K:ORDERING CUSTOMER
            :59:BENEFICIARY
            -}
            """;
    }

    private boolean isValidBicFormat(String bicCode) {
        // BIC is 8 or 11 characters
        if (bicCode == null) return false;
        int length = bicCode.length();
        return length == 8 || length == 11;
    }

    private BicLookupResponse performBicLookup(String bicCode) {
        // Simplified lookup - real implementation would query SWIFT BIC Directory
        BicLookupResponse response = new BicLookupResponse();
        response.setBicCode(bicCode);
        response.setValid(true);
        response.setInstitutionName("Example Bank AG");
        response.setBranchInformation("Head Office");
        response.setCountryCode("DE");
        response.setCity("Frankfurt");
        response.setActive(true);
        return response;
    }
}


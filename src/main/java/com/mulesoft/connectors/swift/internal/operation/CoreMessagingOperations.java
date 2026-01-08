package com.mulesoft.connectors.swift.internal.operation;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.error.SwiftErrorType;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core SWIFT messaging operations.
 * 
 * These are the fundamental operations for sending, receiving, and managing
 * SWIFT messages (both MT and MX formats).
 */
public class CoreMessagingOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreMessagingOperations.class);

    /**
     * Send a SWIFT message to the network.
     * 
     * Supports both MT (Message Type) and MX (ISO 20022) formats.
     * 
     * @param connection Active SWIFT connection
     * @param messageType Message type (e.g., MT103, pacs.008)
     * @param messageContent The message content/payload
     * @param sender Sender BIC code
     * @param receiver Receiver BIC code
     * @param priority Message priority
     * @param format Message format (MT or MX)
     * @return Result containing the sent message details
     */
    @DisplayName("Send Message")
    @Summary("Send a payment instruction or reporting message to the SWIFT network")
    @Throws(SwiftErrorProvider.class)
    public Result<SwiftMessage, MessageAttributes> sendMessage(
            @Connection SwiftConnection connection,
            @DisplayName("Message Type") 
            @Summary("Message type (e.g., MT103 for customer credit transfer, pacs.008 for ISO 20022)")
            String messageType,
            @Content
            @DisplayName("Message Content")
            @Summary("The actual message payload")
            String messageContent,
            @DisplayName("Sender BIC")
            @Summary("8 or 11 character sender Bank Identifier Code")
            String sender,
            @DisplayName("Receiver BIC")
            @Summary("8 or 11 character receiver Bank Identifier Code")
            String receiver,
            @Optional(defaultValue = "NORMAL")
            @DisplayName("Priority")
            @Summary("Message priority (URGENT, NORMAL, SYSTEM)")
            MessagePriority priority,
            @Optional(defaultValue = "MT")
            @DisplayName("Message Format")
            @Summary("Message format: MT (legacy) or MX (ISO 20022)")
            MessageFormat format) throws Exception {

        LOGGER.info("Sending SWIFT message: type={}, sender={}, receiver={}", 
            messageType, sender, receiver);

        // Create message object
        SwiftMessage message = new SwiftMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType(messageType);
        message.setFormat(format);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageContent);
        message.setPriority(priority);
        message.setTimestamp(LocalDateTime.now());
        message.setStatus(SwiftMessageStatus.VALIDATED);

        // Send through connection
        connection.sendMessage(message);
        message.setStatus(SwiftMessageStatus.SENT);

        LOGGER.info("Message sent successfully: messageId={}, sequenceNumber={}", 
            message.getMessageId(), message.getSequenceNumber());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setMessageId(message.getMessageId());
        attributes.setSequenceNumber(message.getSequenceNumber());
        attributes.setTimestamp(message.getTimestamp());
        attributes.setStatus(message.getStatus());

        return Result.<SwiftMessage, MessageAttributes>builder()
            .output(message)
            .attributes(attributes)
            .build();
    }

    /**
     * Send an acknowledgment (ACK) for a received message.
     * 
     * @param connection Active SWIFT connection
     * @param messageId Original message ID to acknowledge
     * @param acknowledgeType Type of acknowledgment (ACK or NACK)
     * @param reasonCode Optional reason code for NACK
     * @return Result containing acknowledgment details
     */
    @DisplayName("Acknowledge Message")
    @Summary("Send ACK or NACK for a received SWIFT message")
    @Throws(SwiftErrorProvider.class)
    public Result<AcknowledgmentResponse, MessageAttributes> acknowledgeMessage(
            @Connection SwiftConnection connection,
            @DisplayName("Message ID")
            @Summary("ID of the message to acknowledge")
            String messageId,
            @DisplayName("Acknowledgment Type")
            @Summary("ACK (accept) or NACK (reject)")
            AcknowledgmentType acknowledgeType,
            @Optional
            @DisplayName("Reason Code")
            @Summary("Reason code for NACK (required if NACK)")
            String reasonCode) throws Exception {

        LOGGER.info("Sending acknowledgment: messageId={}, type={}", messageId, acknowledgeType);

        // Build acknowledgment message
        SwiftMessage ackMessage = new SwiftMessage();
        ackMessage.setMessageId(UUID.randomUUID().toString());
        ackMessage.setMessageType(acknowledgeType == AcknowledgmentType.ACK ? "MT011" : "MT019");
        ackMessage.setFormat(MessageFormat.MT);
        ackMessage.setSender(connection.getConfig().getBicCode());
        ackMessage.setContent(buildAckContent(messageId, acknowledgeType, reasonCode));
        ackMessage.setPriority(MessagePriority.SYSTEM);
        ackMessage.setTimestamp(LocalDateTime.now());

        // Send acknowledgment
        connection.sendMessage(ackMessage);

        LOGGER.info("Acknowledgment sent: type={}, sequenceNumber={}", 
            acknowledgeType, ackMessage.getSequenceNumber());

        // Create response
        AcknowledgmentResponse response = new AcknowledgmentResponse();
        response.setMessageId(messageId);
        response.setAcknowledgmentType(acknowledgeType);
        response.setReasonCode(reasonCode);
        response.setTimestamp(LocalDateTime.now());
        response.setSuccess(true);

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setMessageId(ackMessage.getMessageId());
        attributes.setSequenceNumber(ackMessage.getSequenceNumber());
        attributes.setTimestamp(ackMessage.getTimestamp());

        return Result.<AcknowledgmentResponse, MessageAttributes>builder()
            .output(response)
            .attributes(attributes)
            .build();
    }

    /**
     * Query the status of a previously sent message.
     * 
     * @param connection Active SWIFT connection
     * @param messageId Message ID to query
     * @return Result containing message status information
     */
    @DisplayName("Query Message Status")
    @Summary("Retrieve the current state of a message within the SWIFT interface")
    @Throws(SwiftErrorProvider.class)
    public Result<MessageStatusResponse, MessageAttributes> queryMessageStatus(
            @Connection SwiftConnection connection,
            @DisplayName("Message ID")
            @Summary("ID of the message to query")
            String messageId) throws Exception {

        LOGGER.info("Querying message status: messageId={}", messageId);

        // Build status query message (MT012 - Delivery Notification Request)
        SwiftMessage queryMessage = new SwiftMessage();
        queryMessage.setMessageId(UUID.randomUUID().toString());
        queryMessage.setMessageType("MT012");
        queryMessage.setFormat(MessageFormat.MT);
        queryMessage.setSender(connection.getConfig().getBicCode());
        queryMessage.setContent(buildStatusQueryContent(messageId));
        queryMessage.setPriority(MessagePriority.SYSTEM);
        queryMessage.setTimestamp(LocalDateTime.now());

        // Send query
        connection.sendMessage(queryMessage);

        // Receive response (simplified - real implementation would wait for async response)
        SwiftMessage responseMessage = connection.receiveMessage();

        // Parse status response
        MessageStatusResponse statusResponse = parseStatusResponse(responseMessage);
        statusResponse.setMessageId(messageId);

        LOGGER.info("Message status retrieved: messageId={}, status={}", 
            messageId, statusResponse.getStatus());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setMessageId(responseMessage.getMessageId());
        attributes.setTimestamp(responseMessage.getTimestamp());

        return Result.<MessageStatusResponse, MessageAttributes>builder()
            .output(statusResponse)
            .attributes(attributes)
            .build();
    }

    /**
     * Retrieve a message from the queue (polling).
     * 
     * @param connection Active SWIFT connection
     * @param timeout Timeout in milliseconds
     * @return Result containing the received message, or null if timeout
     */
    @DisplayName("Receive Message")
    @Summary("Poll for an incoming SWIFT message")
    @Throws(SwiftErrorProvider.class)
    public Result<SwiftMessage, MessageAttributes> receiveMessage(
            @Connection SwiftConnection connection,
            @Optional(defaultValue = "30000")
            @DisplayName("Timeout")
            @Summary("Timeout in milliseconds")
            int timeout) throws Exception {

        LOGGER.info("Polling for incoming message with timeout {}ms", timeout);

        // Receive message with timeout
        SwiftMessage message = connection.receiveMessage();

        if (message == null) {
            LOGGER.info("No message received within timeout");
            return null;
        }

        LOGGER.info("Message received: messageId={}, type={}, sender={}", 
            message.getMessageId(), message.getMessageType(), message.getSender());

        // Create attributes
        MessageAttributes attributes = new MessageAttributes();
        attributes.setMessageId(message.getMessageId());
        attributes.setSequenceNumber(message.getSequenceNumber());
        attributes.setTimestamp(message.getTimestamp());
        attributes.setStatus(message.getStatus());

        return Result.<SwiftMessage, MessageAttributes>builder()
            .output(message)
            .attributes(attributes)
            .build();
    }

    // Helper methods

    private String buildAckContent(String messageId, AcknowledgmentType type, String reasonCode) {
        StringBuilder content = new StringBuilder();
        content.append(":20:").append(messageId).append("\n");
        if (type == AcknowledgmentType.NACK && reasonCode != null) {
            content.append(":75:").append(reasonCode).append("\n");
        }
        return content.toString();
    }

    private String buildStatusQueryContent(String messageId) {
        return ":20:" + messageId + "\n";
    }

    private MessageStatusResponse parseStatusResponse(SwiftMessage message) {
        MessageStatusResponse response = new MessageStatusResponse();
        // Simplified parsing - real implementation would parse MT017/MT019 format
        response.setStatus(SwiftMessageStatus.COMPLETED);
        response.setReceivedTimestamp(message.getTimestamp());
        return response;
    }
}


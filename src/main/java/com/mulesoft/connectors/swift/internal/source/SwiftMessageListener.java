package com.mulesoft.connectors.swift.internal.source;

import com.mulesoft.connectors.swift.internal.connection.SwiftConnection;
import com.mulesoft.connectors.swift.internal.model.MessageAttributes;
import com.mulesoft.connectors.swift.internal.model.SwiftMessage;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * SWIFT Message Listener (Source)
 * 
 * Listens for incoming SWIFT messages and automatically triggers Mule flows.
 * This is the inbound trigger that makes the connector event-driven.
 */
@Alias("listener")
@DisplayName("Message Listener")
public class SwiftMessageListener extends Source<SwiftMessage, MessageAttributes> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwiftMessageListener.class);

    @Connection
    private ConnectionProvider<SwiftConnection> connectionProvider;

    @Parameter
    @Optional(defaultValue = "5000")
    @DisplayName("Polling Interval")
    @Summary("Interval in milliseconds to poll for new messages")
    private int pollingInterval;

    @Parameter
    @Optional
    @DisplayName("Message Type Filter")
    @Summary("Filter messages by type (e.g., MT103). Leave empty for all messages.")
    private String messageTypeFilter;

    private volatile boolean running = false;
    private Thread listenerThread;
    private SwiftConnection connection;

    @Override
    public void onStart(SourceCallback<SwiftMessage, MessageAttributes> sourceCallback) throws MuleException {
        LOGGER.info("Starting SWIFT Message Listener");
        
        try {
            connection = connectionProvider.connect();
            running = true;
            
            // Start listener thread
            listenerThread = new Thread(() -> {
                LOGGER.info("Listener thread started with polling interval: {}ms", pollingInterval);
                
                while (running) {
                    try {
                        // Poll for incoming messages
                        SwiftMessage message = connection.receiveMessage();
                        
                        if (message != null) {
                            // Apply message type filter if configured
                            if (messageTypeFilter == null || messageTypeFilter.isEmpty() ||
                                message.getMessageType().equals(messageTypeFilter)) {
                                
                                LOGGER.info("Received message: id={}, type={}, sender={}", 
                                    message.getMessageId(), message.getMessageType(), message.getSender());
                                
                                // Create attributes
                                MessageAttributes attributes = new MessageAttributes();
                                attributes.setMessageId(message.getMessageId());
                                attributes.setSequenceNumber(message.getSequenceNumber());
                                attributes.setTimestamp(message.getTimestamp());
                                attributes.setStatus(message.getStatus());
                                
                                // Trigger flow
                                Result<SwiftMessage, MessageAttributes> result = 
                                    Result.<SwiftMessage, MessageAttributes>builder()
                                        .output(message)
                                        .attributes(attributes)
                                        .build();
                                
                                sourceCallback.handle(result);
                                
                            } else {
                                LOGGER.debug("Message filtered out: type={} does not match filter={}", 
                                    message.getMessageType(), messageTypeFilter);
                            }
                        }
                        
                        // Sleep before next poll
                        Thread.sleep(pollingInterval);
                        
                    } catch (InterruptedException e) {
                        LOGGER.info("Listener thread interrupted");
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        LOGGER.error("Error receiving message", e);
                        sourceCallback.onConnectionException(
                            new org.mule.runtime.api.connection.ConnectionException("Error receiving message: " + e.getMessage(), e)
                        );
                    }
                }
                
                LOGGER.info("Listener thread stopped");
            });
            
            listenerThread.setDaemon(true);
            listenerThread.setName("swift-message-listener");
            listenerThread.start();
            
            LOGGER.info("SWIFT Message Listener started successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to start SWIFT Message Listener", e);
            throw new MuleException() {
                @Override
                public Throwable getCause() {
                    return e;
                }
            };
        }
    }

    @Override
    public void onStop() {
        LOGGER.info("Stopping SWIFT Message Listener");
        
        running = false;
        
        if (listenerThread != null && listenerThread.isAlive()) {
            listenerThread.interrupt();
            try {
                listenerThread.join(5000);
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for listener thread to stop");
                Thread.currentThread().interrupt();
            }
        }
        
        if (connection != null) {
            try {
                connectionProvider.disconnect(connection);
            } catch (Exception e) {
                LOGGER.error("Error disconnecting", e);
            }
        }
        
        LOGGER.info("SWIFT Message Listener stopped");
    }
}


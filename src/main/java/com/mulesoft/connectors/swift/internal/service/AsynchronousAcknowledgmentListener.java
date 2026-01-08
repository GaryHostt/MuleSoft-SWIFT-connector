package com.mulesoft.connectors.swift.internal.service;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * AsynchronousAcknowledgmentListener
 * 
 * Non-blocking ACK/NACK listener that:
 * - Registers pending messages in Object Store
 * - Listens for MT019 (ACK) / MT011 (NACK) responses
 * - Matches responses to original messageId
 * - Provides CompletableFuture for async processing
 * - Handles timeouts and retries
 */
public class AsynchronousAcknowledgmentListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousAcknowledgmentListener.class);
    private static final String PENDING_ACKS_KEY = "swift.pending.acks";
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    private final ObjectStore<Serializable> objectStore;
    private final Map<String, CompletableFuture<AcknowledgmentResult>> pendingAcks;
    
    public AsynchronousAcknowledgmentListener(ObjectStore<Serializable> objectStore) {
        this.objectStore = objectStore;
        this.pendingAcks = new ConcurrentHashMap<>();
        
        // Start cleanup thread for expired acknowledgments
        startCleanupThread();
    }
    
    /**
     * Register a message awaiting acknowledgment
     * 
     * @param messageId Unique message identifier
     * @param sequenceNumber Message sequence number
     * @param timeoutMs Timeout in milliseconds
     * @return CompletableFuture that completes when ACK/NACK received
     */
    public CompletableFuture<AcknowledgmentResult> registerPendingAck(
            String messageId, 
            long sequenceNumber, 
            long timeoutMs) {
        
        LOGGER.debug("Registering pending ACK for messageId: {}, sequence: {}", messageId, sequenceNumber);
        
        CompletableFuture<AcknowledgmentResult> future = new CompletableFuture<>();
        
        // Store in memory
        pendingAcks.put(messageId, future);
        
        // Persist to Object Store
        try {
            PendingAckInfo info = new PendingAckInfo(
                messageId, 
                sequenceNumber, 
                System.currentTimeMillis(),
                timeoutMs
            );
            
            objectStore.store(getPendingAckKey(messageId), info);
            LOGGER.debug("Stored pending ACK info in Object Store");
            
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to store pending ACK in Object Store", e);
        }
        
        // Set timeout
        CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (!future.isDone()) {
                    LOGGER.warn("ACK timeout for messageId: {} after {}ms", messageId, timeoutMs);
                    future.complete(AcknowledgmentResult.timeout(messageId));
                    cleanup(messageId);
                }
            });
        
        return future;
    }
    
    /**
     * Process incoming acknowledgment (MT019 ACK or MT011 NACK)
     * 
     * @param incomingMessage The ACK/NACK message
     * @param messageId The messageId from Tag 20
     * @param isNack True if NACK, false if ACK
     * @param errorCode Error code from Tag 451 (for NACK)
     * @param errorText Error text from Tag 79 (for NACK)
     */
    public void processAcknowledgment(
            String incomingMessage,
            String messageId, 
            boolean isNack,
            String errorCode,
            String errorText) {
        
        LOGGER.debug("Processing {} for messageId: {}", isNack ? "NACK" : "ACK", messageId);
        
        CompletableFuture<AcknowledgmentResult> future = pendingAcks.get(messageId);
        
        if (future == null) {
            LOGGER.warn("Received {} for unknown messageId: {}", isNack ? "NACK" : "ACK", messageId);
            return;
        }
        
        if (future.isDone()) {
            LOGGER.warn("Received duplicate {} for messageId: {}", isNack ? "NACK" : "ACK", messageId);
            return;
        }
        
        // Complete the future
        if (isNack) {
            future.complete(AcknowledgmentResult.nack(messageId, errorCode, errorText));
            LOGGER.info("NACK received for messageId: {} - Code: {}, Text: {}", 
                messageId, errorCode, errorText);
        } else {
            future.complete(AcknowledgmentResult.ack(messageId));
            LOGGER.info("ACK received for messageId: {}", messageId);
        }
        
        // Cleanup
        cleanup(messageId);
    }
    
    /**
     * Check for sequence gaps using Object Store
     * 
     * @param expectedSequence Expected sequence number
     * @param receivedSequence Received sequence number
     * @return Gap information if gap detected
     */
    public SequenceGap detectGap(long expectedSequence, long receivedSequence) {
        if (receivedSequence > expectedSequence) {
            LOGGER.warn("Sequence gap detected - Expected: {}, Received: {}", 
                expectedSequence, receivedSequence);
            
            return new SequenceGap(
                expectedSequence,
                receivedSequence - 1,
                receivedSequence - expectedSequence
            );
        }
        
        return null;
    }
    
    /**
     * Retrieve pending acknowledgment from Object Store
     * Used for crash recovery
     */
    public PendingAckInfo retrievePendingAck(String messageId) {
        try {
            return (PendingAckInfo) objectStore.retrieve(getPendingAckKey(messageId));
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to retrieve pending ACK from Object Store", e);
            return null;
        }
    }
    
    /**
     * Get all pending acknowledgments from Object Store
     * Used for system restart recovery
     */
    public Map<String, PendingAckInfo> getAllPendingAcks() {
        Map<String, PendingAckInfo> pending = new ConcurrentHashMap<>();
        
        try {
            for (Serializable key : objectStore.allKeys()) {
                String keyStr = (String) key;
                if (keyStr.startsWith(PENDING_ACKS_KEY)) {
                    String messageId = keyStr.substring(PENDING_ACKS_KEY.length() + 1);
                    PendingAckInfo info = (PendingAckInfo) objectStore.retrieve(keyStr);
                    pending.put(messageId, info);
                }
            }
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to retrieve all pending ACKs", e);
        }
        
        LOGGER.info("Retrieved {} pending acknowledgments from Object Store", pending.size());
        return pending;
    }
    
    /**
     * Cleanup after acknowledgment received or timeout
     */
    private void cleanup(String messageId) {
        pendingAcks.remove(messageId);
        
        try {
            objectStore.remove(getPendingAckKey(messageId));
            LOGGER.debug("Cleaned up pending ACK for messageId: {}", messageId);
        } catch (ObjectStoreException e) {
            LOGGER.error("Failed to remove pending ACK from Object Store", e);
        }
    }
    
    /**
     * Background thread to cleanup expired acknowledgments
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // Check every minute
                    
                    long now = System.currentTimeMillis();
                    Map<String, PendingAckInfo> allPending = getAllPendingAcks();
                    
                    for (Map.Entry<String, PendingAckInfo> entry : allPending.entrySet()) {
                        PendingAckInfo info = entry.getValue();
                        long age = now - info.getRegisteredAt();
                        
                        if (age > info.getTimeoutMs()) {
                            LOGGER.warn("Cleaning up expired ACK for messageId: {} (age: {}ms)", 
                                entry.getKey(), age);
                            cleanup(entry.getKey());
                        }
                    }
                    
                } catch (InterruptedException e) {
                    LOGGER.info("Cleanup thread interrupted");
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in cleanup thread", e);
                }
            }
        }, "SWIFT-ACK-Cleanup");
        
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    private String getPendingAckKey(String messageId) {
        return PENDING_ACKS_KEY + "." + messageId;
    }
    
    /**
     * Acknowledgment result
     */
    public static class AcknowledgmentResult {
        private final String messageId;
        private final AckStatus status;
        private final String errorCode;
        private final String errorText;
        private final long timestamp;
        
        private AcknowledgmentResult(String messageId, AckStatus status, String errorCode, String errorText) {
            this.messageId = messageId;
            this.status = status;
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.timestamp = System.currentTimeMillis();
        }
        
        public static AcknowledgmentResult ack(String messageId) {
            return new AcknowledgmentResult(messageId, AckStatus.ACK, null, null);
        }
        
        public static AcknowledgmentResult nack(String messageId, String errorCode, String errorText) {
            return new AcknowledgmentResult(messageId, AckStatus.NACK, errorCode, errorText);
        }
        
        public static AcknowledgmentResult timeout(String messageId) {
            return new AcknowledgmentResult(messageId, AckStatus.TIMEOUT, null, "Timeout waiting for ACK");
        }
        
        // Getters
        public String getMessageId() { return messageId; }
        public AckStatus getStatus() { return status; }
        public String getErrorCode() { return errorCode; }
        public String getErrorText() { return errorText; }
        public long getTimestamp() { return timestamp; }
        
        public boolean isAck() { return status == AckStatus.ACK; }
        public boolean isNack() { return status == AckStatus.NACK; }
        public boolean isTimeout() { return status == AckStatus.TIMEOUT; }
    }
    
    public enum AckStatus {
        ACK,
        NACK,
        TIMEOUT
    }
    
    /**
     * Pending acknowledgment info (stored in Object Store)
     */
    public static class PendingAckInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String messageId;
        private final long sequenceNumber;
        private final long registeredAt;
        private final long timeoutMs;
        
        public PendingAckInfo(String messageId, long sequenceNumber, long registeredAt, long timeoutMs) {
            this.messageId = messageId;
            this.sequenceNumber = sequenceNumber;
            this.registeredAt = registeredAt;
            this.timeoutMs = timeoutMs;
        }
        
        // Getters
        public String getMessageId() { return messageId; }
        public long getSequenceNumber() { return sequenceNumber; }
        public long getRegisteredAt() { return registeredAt; }
        public long getTimeoutMs() { return timeoutMs; }
    }
    
    /**
     * Sequence gap information
     */
    public static class SequenceGap {
        private final long fromSequence;
        private final long toSequence;
        private final long gapSize;
        
        public SequenceGap(long fromSequence, long toSequence, long gapSize) {
            this.fromSequence = fromSequence;
            this.toSequence = toSequence;
            this.gapSize = gapSize;
        }
        
        // Getters
        public long getFromSequence() { return fromSequence; }
        public long getToSequence() { return toSequence; }
        public long getGapSize() { return gapSize; }
    }
}


package com.mulesoft.connectors.swift.internal.service;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.*;

/**
 * CloudHub-Compatible AsynchronousAcknowledgmentListener
 * 
 * ✅ CRITICAL FIX: Replaced ConcurrentHashMap with Object Store V2
 * 
 * This version is CloudHub-safe:
 * - Worker 1 registers pending ACK in Object Store
 * - Worker 2 can process ACK by reading from Object Store
 * - No in-memory state shared between workers
 * - Distributed polling mechanism for timeout detection
 * 
 * Key Changes:
 * 1. Removed: ConcurrentHashMap<String, CompletableFuture>
 * 2. Added: Object Store-only state management
 * 3. Added: Polling-based ACK retrieval (CloudHub compatible)
 * 4. Added: Distributed timeout detection
 */
public class AsynchronousAcknowledgmentListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousAcknowledgmentListener.class);
    
    // Object Store key prefixes
    private static final String OS_PREFIX_PENDING = "swift.pending.ack.";
    private static final String OS_PREFIX_RESULT = "swift.ack.result.";
    
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    private static final long POLL_INTERVAL_MS = 500; // Poll every 500ms
    
    private final ObjectStore<Serializable> objectStore;
    private final ScheduledExecutorService timeoutChecker;
    
    public AsynchronousAcknowledgmentListener(ObjectStore<Serializable> objectStore) {
        this.objectStore = objectStore;
        
        // Timeout checker runs on all workers (safe for CloudHub)
        this.timeoutChecker = Executors.newScheduledThreadPool(1);
        startTimeoutChecker();
        
        LOGGER.info("✅ CloudHub-compatible ACK listener initialized (Object Store-based)");
    }
    
    /**
     * ✅ CLOUDH UB-SAFE: Register pending ACK in Object Store (not memory)
     * 
     * Any worker can later process the ACK by querying Object Store
     */
    public CompletableFuture<AcknowledgmentResult> registerPendingAck(
            String messageId, 
            long sequenceNumber) {
        
        LOGGER.debug("Registering pending ACK in Object Store: messageId={}", messageId);
        
        CompletableFuture<AcknowledgmentResult> future = new CompletableFuture<>();
        
        try {
            // Store pending state in Object Store (not memory!)
            PendingAckRecord record = new PendingAckRecord(
                messageId,
                sequenceNumber,
                LocalDateTime.now(),
                DEFAULT_TIMEOUT_MS
            );
            
            objectStore.store(OS_PREFIX_PENDING + messageId, record);
            LOGGER.info("✅ Pending ACK registered in Object Store: {}", messageId);
            
            // Start polling for result (CloudHub-safe async)
            pollForAcknowledgment(messageId, future);
            
        } catch (ObjectStoreException e) {
            LOGGER.error("❌ Failed to register pending ACK in Object Store", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * ✅ CLOUDHUB-SAFE: Process ACK on ANY worker
     * 
     * Worker 1 sends message, Worker 2 receives ACK → Works correctly!
     * ACK result stored in Object Store, polling thread picks it up
     */
    public void processAcknowledgment(
            String messageId, 
            boolean isNack,
            String errorCode,
            String errorText) {
        
        LOGGER.debug("Processing {} for messageId: {}", isNack ? "NACK" : "ACK", messageId);
        
        try {
            // Check if pending ACK exists in Object Store
            PendingAckRecord pending = (PendingAckRecord) objectStore.retrieve(OS_PREFIX_PENDING + messageId);
            
            if (pending == null) {
                LOGGER.warn("⚠️ Received {} for unknown messageId: {}", 
                    isNack ? "NACK" : "ACK", messageId);
                return;
            }
            
            // Create result
            AcknowledgmentResult result = isNack
                ? AcknowledgmentResult.nack(messageId, errorCode, errorText)
                : AcknowledgmentResult.ack(messageId);
            
            // Store result in Object Store (polling thread will pick it up)
            objectStore.store(OS_PREFIX_RESULT + messageId, result);
            
            // Remove pending record
            objectStore.remove(OS_PREFIX_PENDING + messageId);
            
            LOGGER.info("✅ {} processed and stored in Object Store: {}", 
                isNack ? "NACK" : "ACK", messageId);
            
        } catch (ObjectStoreException e) {
            LOGGER.error("❌ Failed to process acknowledgment", e);
        }
    }
    
    /**
     * ✅ CLOUDHUB-SAFE: Poll Object Store for ACK result
     * 
     * This replaces CompletableFuture callback mechanism with polling
     * Works across workers because Object Store is distributed
     */
    private void pollForAcknowledgment(String messageId, CompletableFuture<AcknowledgmentResult> future) {
        
        ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
        
        final long startTime = System.currentTimeMillis();
        
        poller.scheduleAtFixedRate(() -> {
            try {
                // Check if result is available in Object Store
                AcknowledgmentResult result = (AcknowledgmentResult) objectStore.retrieve(OS_PREFIX_RESULT + messageId);
                
                if (result != null) {
                    // ACK/NACK received!
                    future.complete(result);
                    
                    // Cleanup
                    objectStore.remove(OS_PREFIX_RESULT + messageId);
                    poller.shutdown();
                    
                    LOGGER.info("✅ ACK result retrieved from Object Store: {}", messageId);
                    return;
                }
                
                // Check timeout
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > DEFAULT_TIMEOUT_MS) {
                    LOGGER.warn("⏱️ ACK timeout for messageId: {} ({}ms)", messageId, elapsed);
                    
                    AcknowledgmentResult timeoutResult = AcknowledgmentResult.timeout(messageId);
                    future.complete(timeoutResult);
                    
                    // Cleanup
                    try {
                        objectStore.remove(OS_PREFIX_PENDING + messageId);
                    } catch (Exception e) {
                        LOGGER.error("Failed to remove timed out ACK", e);
                    }
                    
                    poller.shutdown();
                }
                
            } catch (ObjectStoreException e) {
                LOGGER.error("❌ Error polling for ACK", e);
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * ✅ CLOUDHUB-SAFE: Distributed timeout checker
     * 
     * Runs on all workers, but Object Store ensures idempotency
     * Only one worker will successfully remove expired ACKs
     */
    private void startTimeoutChecker() {
        timeoutChecker.scheduleAtFixedRate(() -> {
            try {
                checkExpiredAcks();
            } catch (Exception e) {
                LOGGER.error("Error in timeout checker", e);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }
    
    /**
     * Check for expired pending ACKs and clean them up
     */
    private void checkExpiredAcks() {
        try {
            for (Serializable key : objectStore.allKeys()) {
                String keyStr = (String) key;
                
                if (keyStr.startsWith(OS_PREFIX_PENDING)) {
                    PendingAckRecord record = (PendingAckRecord) objectStore.retrieve(keyStr);
                    
                    if (record != null) {
                        long elapsedMs = java.time.Duration.between(
                            record.getRegisteredAt(), 
                            LocalDateTime.now()
                        ).toMillis();
                        
                        if (elapsedMs > record.getTimeoutMs() + 60000) { // 1 min grace period
                            String messageId = keyStr.substring(OS_PREFIX_PENDING.length());
                            LOGGER.warn("🧹 Cleaning up expired ACK: {} (age: {}ms)", 
                                messageId, elapsedMs);
                            
                            objectStore.remove(keyStr);
                        }
                    }
                }
            }
        } catch (ObjectStoreException e) {
            LOGGER.error("Error checking expired ACKs", e);
        }
    }
    
    /**
     * Get all pending acknowledgments (for monitoring)
     */
    public int getPendingCount() {
        try {
            int count = 0;
            for (Serializable key : objectStore.allKeys()) {
                if (((String) key).startsWith(OS_PREFIX_PENDING)) {
                    count++;
                }
            }
            return count;
        } catch (ObjectStoreException e) {
            LOGGER.error("Error getting pending count", e);
            return -1;
        }
    }
    
    /**
     * Shutdown (cleanup)
     */
    public void shutdown() {
        timeoutChecker.shutdown();
        try {
            if (!timeoutChecker.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutChecker.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutChecker.shutdownNow();
        }
    }
    
    // ========== DATA CLASSES ==========
    
    /**
     * Pending ACK record (stored in Object Store)
     */
    public static class PendingAckRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String messageId;
        private final long sequenceNumber;
        private final LocalDateTime registeredAt;
        private final long timeoutMs;
        
        public PendingAckRecord(String messageId, long sequenceNumber, 
                               LocalDateTime registeredAt, long timeoutMs) {
            this.messageId = messageId;
            this.sequenceNumber = sequenceNumber;
            this.registeredAt = registeredAt;
            this.timeoutMs = timeoutMs;
        }
        
        public String getMessageId() { return messageId; }
        public long getSequenceNumber() { return sequenceNumber; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public long getTimeoutMs() { return timeoutMs; }
    }
    
    /**
     * Acknowledgment result
     */
    public static class AcknowledgmentResult implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String messageId;
        private final AckStatus status;
        private final String errorCode;
        private final String errorText;
        private final long timestamp;
        
        private AcknowledgmentResult(String messageId, AckStatus status, 
                                    String errorCode, String errorText) {
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
            return new AcknowledgmentResult(messageId, AckStatus.TIMEOUT, null, "ACK timeout");
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
}

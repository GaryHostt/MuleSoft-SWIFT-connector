package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Message enrichment response
 */
public class EnrichmentResponse {
    
    private String originalContent;
    private String enrichedContent;
    private String enrichmentType;
    private LocalDateTime enrichmentTimestamp;

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getEnrichedContent() {
        return enrichedContent;
    }

    public void setEnrichedContent(String enrichedContent) {
        this.enrichedContent = enrichedContent;
    }

    public String getEnrichmentType() {
        return enrichmentType;
    }

    public void setEnrichmentType(String enrichmentType) {
        this.enrichmentType = enrichmentType;
    }

    public LocalDateTime getEnrichmentTimestamp() {
        return enrichmentTimestamp;
    }

    public void setEnrichmentTimestamp(LocalDateTime enrichmentTimestamp) {
        this.enrichmentTimestamp = enrichmentTimestamp;
    }
}


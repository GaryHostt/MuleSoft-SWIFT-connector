package com.mulesoft.connectors.swift.internal.model;

import java.time.LocalDateTime;

/**
 * Translation response for MT/MX conversion
 */
public class TranslationResponse {
    
    private MessageFormat sourceFormat;
    private MessageFormat targetFormat;
    private String mtMessageType;
    private String mxMessageType;
    private String translatedContent;
    private LocalDateTime translationTimestamp;
    private boolean success;
    private String errorMessage;

    public MessageFormat getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(MessageFormat sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public MessageFormat getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(MessageFormat targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getMtMessageType() {
        return mtMessageType;
    }

    public void setMtMessageType(String mtMessageType) {
        this.mtMessageType = mtMessageType;
    }

    public String getMxMessageType() {
        return mxMessageType;
    }

    public void setMxMessageType(String mxMessageType) {
        this.mxMessageType = mxMessageType;
    }

    public String getTranslatedContent() {
        return translatedContent;
    }

    public void setTranslatedContent(String translatedContent) {
        this.translatedContent = translatedContent;
    }

    public LocalDateTime getTranslationTimestamp() {
        return translationTimestamp;
    }

    public void setTranslationTimestamp(LocalDateTime translationTimestamp) {
        this.translationTimestamp = translationTimestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}


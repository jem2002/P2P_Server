package com.universidad.messaging.server.shared.schema.commentSchema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Comment {
    private Long id;
    private Long documentId;
    private Long userId;
    private String content;
    private Sentiment sentiment;      // Enum: POSITIVO, NEGATIVO
    private BigDecimal confidence;    // Mapea perfectamente con DECIMAL(7,4)
    private LocalDateTime createdAt;

    // Enum interno (o en archivo separado) para el sentimiento
    public enum Sentiment {
        POSITIVO,
        NEGATIVO
    }


    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Sentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(Sentiment sentiment) {
        this.sentiment = sentiment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
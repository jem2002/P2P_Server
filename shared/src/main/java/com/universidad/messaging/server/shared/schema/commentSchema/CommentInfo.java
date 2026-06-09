package com.universidad.messaging.server.shared.schema.commentSchema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommentInfo {
    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private String content;
    private Comment.Sentiment sentiment; // Reutiliza el Enum de tu clase Comment
    private BigDecimal confidence;
    private LocalDateTime createdAt;

    // Constructor vacío
    public CommentInfo() {
    }

    // Constructor completo (opcional, por si quieres instanciarlo directamente)
    public CommentInfo(Long id, Long documentId, Long userId, String username, String content,
                       Comment.Sentiment sentiment, BigDecimal confidence, LocalDateTime createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.userId = userId;
        this.username = username;
        this.content = content;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Comment.Sentiment getSentiment() { return sentiment; }
    public void setSentiment(Comment.Sentiment sentiment) { this.sentiment = sentiment; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
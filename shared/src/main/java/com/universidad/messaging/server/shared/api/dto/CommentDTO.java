package com.universidad.messaging.server.shared.api.dto;

public record CommentDTO(
        String documentName,
        String username,
        String content,
        String sentiment, // Reutiliza el Enum de tu clase Comment
        String confidence,
        String createdAt
)
{}

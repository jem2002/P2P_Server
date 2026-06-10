package com.universidad.messaging.server.shared.api.dto;

public record DocumentDTO(
        String name,
        String extension,
        long sizeBytes,
        String owner,
        String createdAt
)
{}

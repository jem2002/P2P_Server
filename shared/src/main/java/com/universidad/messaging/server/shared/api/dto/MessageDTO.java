package com.universidad.messaging.server.shared.api.dto;

public record MessageDTO(
        String content,
        String owner,
        String target,
        String createdAt
)
{}

package com.universidad.messaging.server.shared.api.dto;

public record ConnectionDTO(
        String  username,
        String  ipAddress,
        String  nodeId,
        int     port,
        String  protocol,
        String  connectedAt,
        String  disconnectedAt,
        boolean isActive
) {}

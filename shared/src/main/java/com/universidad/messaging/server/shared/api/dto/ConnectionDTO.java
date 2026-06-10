package com.universidad.messaging.server.shared.api.dto;

public record ConnectionDTO(
        String  username,
        String  ipAddress,
        String  nodeId,
        int     port,
        String  connectedAt,
        String  disconnectedAt,
        String  protocol,
        boolean isActive
) {}

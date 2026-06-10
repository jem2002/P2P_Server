package com.universidad.messaging.server.shared.api.dto;

public record NodeInfoDTO(
        // Info del Servidor Local
        String host,
        int port,
        int maxConnections,

        // Info del Clúster P2P
        String nodeId,
        int clusterPort,
        int totalRegisteredUsers,
        int totalMessages,
        int totalDocuments,
        int totalComments
) {}
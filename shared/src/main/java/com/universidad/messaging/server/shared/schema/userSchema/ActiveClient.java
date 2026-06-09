package com.universidad.messaging.server.shared.schema.userSchema;

/**
 * DTO inmutable que representa un cliente activo conectado al servidor.
 * Reemplaza Map<String, String> con tipado fuerte.
 */
public final class ActiveClient {

    private final String username;
    private final String ip;
    private final String connectedAt;
    private final String nodeId; // Identificador del nodo/servidor al que se conecta

    public ActiveClient(String username, String ip, String connectedAt, String nodeId) {
        this.username = username;
        this.ip = ip;
        this.connectedAt = connectedAt;
        this.nodeId = nodeId;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getConnectedAt() {
        return connectedAt;
    }

    public String getNodeId() {
        return nodeId;
    }
}
package identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidad inmutable de un nodo en la red P2P.
 * Cada servidor tiene una identidad única compuesta por un UUID,
 * su host, el puerto para clientes y el puerto para comunicación inter-servidor.
 *
 * Principio aplicado: Value Object (DDD) — identidad por nodeId, inmutable.
 */
public final class NodeIdentity {

    private final String nodeId;
    private final String host;
    private final int clientPort;
    private final int clusterPort;

    /**
     * Crea una identidad de nodo con un ID auto-generado si se pasa "auto".
     */
    public NodeIdentity(String nodeId, String host, int clientPort, int clusterPort) {
        this.nodeId = "auto".equalsIgnoreCase(nodeId)
                ? UUID.randomUUID().toString().substring(0, 8)
                : nodeId;
        this.host = Objects.requireNonNull(host, "host no puede ser null");
        this.clientPort = clientPort;
        this.clusterPort = clusterPort;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getClientPort() {
        return clientPort;
    }

    public int getClusterPort() {
        return clusterPort;
    }

    /**
     * Dirección completa del cluster en formato host:clusterPort.
     */
    public String getClusterAddress() {
        return host + ":" + clusterPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeIdentity that = (NodeIdentity) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return "Node[" + nodeId + "@" + host + ":" + clusterPort + "]";
    }
}

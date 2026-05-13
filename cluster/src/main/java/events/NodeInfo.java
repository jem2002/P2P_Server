package events;

import java.util.Objects;

/**
 * DTO inmutable con la información pública de un nodo remoto.
 * Se usa como payload en los eventos del cluster.
 *
 * Principio aplicado: Value Object (DDD) — igualdad por nodeId.
 */
public final class NodeInfo {

    private final String nodeId;
    private final String host;
    private final int clusterPort;

    public NodeInfo(String nodeId, String host, int clusterPort) {
        this.nodeId = Objects.requireNonNull(nodeId);
        this.host = Objects.requireNonNull(host);
        this.clusterPort = clusterPort;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getClusterPort() {
        return clusterPort;
    }

    public String getAddress() {
        return host + ":" + clusterPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeInfo that = (NodeInfo) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }

    @Override
    public String toString() {
        return "NodeInfo[" + nodeId + "@" + host + ":" + clusterPort + "]";
    }
}

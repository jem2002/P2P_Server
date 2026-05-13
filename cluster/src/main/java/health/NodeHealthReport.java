package health;

import discovery.NodeState;

/**
 * DTO inmutable con el reporte de salud de un nodo individual.
 */
public final class NodeHealthReport {

    private final String nodeId;
    private final String host;
    private final int clusterPort;
    private final NodeState state;
    private final long lastHeartbeatMs;
    private final boolean isLocal;

    public NodeHealthReport(String nodeId, String host, int clusterPort,
                            NodeState state, long lastHeartbeatMs, boolean isLocal) {
        this.nodeId = nodeId;
        this.host = host;
        this.clusterPort = clusterPort;
        this.state = state;
        this.lastHeartbeatMs = lastHeartbeatMs;
        this.isLocal = isLocal;
    }

    public String getNodeId() { return nodeId; }
    public String getHost() { return host; }
    public int getClusterPort() { return clusterPort; }
    public NodeState getState() { return state; }
    public long getLastHeartbeatMs() { return lastHeartbeatMs; }
    public boolean isLocal() { return isLocal; }

    @Override
    public String toString() {
        return String.format("%-12s | %-20s | %-10s | %s",
                nodeId, host + ":" + clusterPort, state, isLocal ? "LOCAL" : "REMOTE");
    }
}

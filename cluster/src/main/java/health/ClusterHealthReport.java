package health;

import java.util.Collections;
import java.util.List;

/**
 * DTO inmutable con el reporte de salud de todo el cluster.
 */
public final class ClusterHealthReport {

    private final String localNodeId;
    private final int totalNodes;
    private final int aliveNodes;
    private final int suspectedNodes;
    private final int downNodes;
    private final List<NodeHealthReport> nodeReports;

    public ClusterHealthReport(String localNodeId, int totalNodes, int aliveNodes,
                                int suspectedNodes, int downNodes,
                                List<NodeHealthReport> nodeReports) {
        this.localNodeId = localNodeId;
        this.totalNodes = totalNodes;
        this.aliveNodes = aliveNodes;
        this.suspectedNodes = suspectedNodes;
        this.downNodes = downNodes;
        this.nodeReports = Collections.unmodifiableList(nodeReports);
    }

    public String getLocalNodeId() { return localNodeId; }
    public int getTotalNodes() { return totalNodes; }
    public int getAliveNodes() { return aliveNodes; }
    public int getSuspectedNodes() { return suspectedNodes; }
    public int getDownNodes() { return downNodes; }
    public List<NodeHealthReport> getNodeReports() { return nodeReports; }
}

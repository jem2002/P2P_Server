package health;

import communication.PeerConnectionPool;
import discovery.MemberEntry;
import discovery.MembershipList;
import discovery.NodeState;
import identity.NodeIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de monitoreo distribuido que agrega el estado de salud
 * de todos los nodos del cluster.
 *
 * Principio aplicado: Facade (GoF) — provee una interfaz simplificada
 * para consultar el estado de la red desde la consola admin o endpoints.
 */
public class ClusterHealthService {

    private static final Logger logger = LoggerFactory.getLogger(ClusterHealthService.class);

    private final NodeIdentity localNode;
    private final MembershipList membership;
    private final PeerConnectionPool peerPool;

    public ClusterHealthService(NodeIdentity localNode, MembershipList membership,
                                 PeerConnectionPool peerPool) {
        this.localNode = localNode;
        this.membership = membership;
        this.peerPool = peerPool;
    }

    /**
     * Genera un reporte completo del estado del cluster.
     */
    public ClusterHealthReport getClusterHealth() {
        List<NodeHealthReport> reports = new ArrayList<>();
        int alive = 0, suspected = 0, down = 0;

        // Añadir este nodo (siempre ALIVE)
        reports.add(new NodeHealthReport(
                localNode.getNodeId(), localNode.getHost(), localNode.getClusterPort(),
                NodeState.ALIVE, 0, true));
        alive++;

        // Añadir todos los nodos conocidos
        for (MemberEntry entry : membership.getAllEntries()) {
            NodeState state = entry.getState();
            reports.add(new NodeHealthReport(
                    entry.getNodeInfo().getNodeId(),
                    entry.getNodeInfo().getHost(),
                    entry.getNodeInfo().getClusterPort(),
                    state,
                    entry.getTimeSinceLastHeartbeatMs(),
                    false));

            switch (state) {
                case ALIVE: alive++; break;
                case SUSPECTED: suspected++; break;
                case DOWN: down++; break;
                default: break;
            }
        }

        return new ClusterHealthReport(
                localNode.getNodeId(),
                reports.size(), alive, suspected, down,
                reports);
    }

    /**
     * Imprime el estado del cluster en la consola de administración.
     */
    public void printClusterStatus() {
        ClusterHealthReport report = getClusterHealth();

        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           ESTADO DEL CLUSTER P2P                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║  Nodo Local: %-43s ║%n", localNode.getNodeId());
        System.out.printf("║  Total Nodos: %-3d  |  Vivos: %-3d  |  Sospechosos: %-3d   ║%n",
                report.getTotalNodes(), report.getAliveNodes(), report.getSuspectedNodes());
        System.out.printf("║  Conexiones Peer Activas: %-30d ║%n", peerPool.activeCount());
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-12s | %-20s | %-10s | %-7s ║%n",
                "NODE ID", "DIRECCIÓN", "ESTADO", "TIPO");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        for (NodeHealthReport nodeReport : report.getNodeReports()) {
            System.out.printf("║ %-12s | %-20s | %-10s | %-7s ║%n",
                    nodeReport.getNodeId(),
                    nodeReport.getHost() + ":" + nodeReport.getClusterPort(),
                    nodeReport.getState(),
                    nodeReport.isLocal() ? "LOCAL" : "REMOTO");
        }

        System.out.println("╚══════════════════════════════════════════════════════════╝");
    }
}

package discovery;

import events.ClusterEvent;
import events.NetworkEventBus;
import events.NodeInfo;
import identity.NodeIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gossip Protocol para descubrimiento de miembros y detección de fallos en la red P2P.
 *
 * Protocolo de heartbeat ENRIQUECIDO con membresía:
 *
 *   Formato del mensaje UDP:
 *     HEARTBEAT|nodeId|host|clusterPort|member1Id:member1Host:member1Port|member2Id:...
 *
 *   Esto permite "gossip real": cuando node-2 recibe el heartbeat de node-1
 *   que incluye a node-3 en su lista de miembros, node-2 descubre a node-3
 *   sin necesidad de conectarse directamente a él como seed.
 *
 *   Sin esto, en una topología en estrella (todos apuntan al mismo seed),
 *   los nodos secundarios (node-2, node-3) nunca se conocen entre sí.
 *
 * Funcionamiento:
 *   1. Cada heartbeatIntervalMs envía un heartbeat UDP a seeds + miembros conocidos.
 *   2. El heartbeat incluye la lista de miembros ALIVE propios (propagación de membresía).
 *   3. El receptor añade cualquier miembro nuevo que aparezca en el heartbeat.
 *   4. Un hilo de detección verifica periódicamente si algún nodo dejó de responder.
 *   5. Si un nodo no responde en suspectTimeoutMs → SUSPECTED.
 *   6. Si no responde en failureTimeoutMs → DOWN.
 */
public class GossipProtocol implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GossipProtocol.class);
    private static final int HEARTBEAT_BUFFER_SIZE = 2048; // ampliado para la lista de miembros

    private final NodeIdentity self;
    private final MembershipList membershipList;
    private final NetworkEventBus eventBus;
    private final List<String> seedNodes;
    private final long heartbeatIntervalMs;
    private final long suspectTimeoutMs;
    private final long failureTimeoutMs;

    private volatile boolean running = true;
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;

    public GossipProtocol(NodeIdentity self, MembershipList membershipList,
                          NetworkEventBus eventBus, List<String> seedNodes,
                          long heartbeatIntervalMs, long failureTimeoutMs) {
        this.self = self;
        this.membershipList = membershipList;
        this.eventBus = eventBus;
        this.seedNodes = seedNodes;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.suspectTimeoutMs = failureTimeoutMs / 2;
        this.failureTimeoutMs = failureTimeoutMs;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(self.getClusterPort());
            logger.info("GossipProtocol escuchando en UDP:{} — NodeId: {}",
                    self.getClusterPort(), self.getNodeId());

            scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(this::sendHeartbeats,
                    0, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::checkForFailures,
                    heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);

            byte[] buffer = new byte[HEARTBEAT_BUFFER_SIZE];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                processIncomingHeartbeat(packet);
            }

        } catch (Exception e) {
            if (running) {
                logger.error("Error en GossipProtocol", e);
            } else {
                logger.info("GossipProtocol detenido.");
            }
        }
    }

    // ── Envío ──────────────────────────────────────────────────────────────

    /**
     * Construye el heartbeat incluyendo la lista de miembros conocidos.
     *
     * Formato:
     *   HEARTBEAT|selfId|selfHost|selfPort[|memberId:memberHost:memberPort]*
     *
     * El sufijo de miembros permite que el receptor descubra nodos que no
     * conoce aún (propagación de membresía transitiva).
     */
    private String buildHeartbeatMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("HEARTBEAT|")
          .append(self.getNodeId()).append("|")
          .append(self.getHost()).append("|")
          .append(self.getClusterPort());

        // Añadir lista de miembros ALIVE conocidos (excluirse a sí mismo)
        for (NodeInfo node : membershipList.getAliveNodes()) {
            sb.append("|")
              .append(node.getNodeId()).append(":")
              .append(node.getHost()).append(":")
              .append(node.getClusterPort());
        }
        return sb.toString();
    }

    /**
     * Envía el heartbeat a seeds + miembros conocidos.
     */
    private void sendHeartbeats() {
        String heartbeat = buildHeartbeatMessage();
        byte[] data = heartbeat.getBytes(StandardCharsets.UTF_8);

        // Bootstrap: seeds siempre reciben heartbeat
        for (String seedAddress : seedNodes) {
            sendUdpPacket(data, seedAddress);
        }

        // Miembros conocidos
        for (NodeInfo node : membershipList.getAliveNodes()) {
            sendUdpPacket(data, node.getAddress());
        }
    }

    private void sendUdpPacket(byte[] data, String address) {
        try {
            String[] parts = address.split(":");
            if (parts.length != 2) return;

            InetAddress inetAddress = InetAddress.getByName(parts[0]);
            int port = Integer.parseInt(parts[1]);

            // No enviarse a sí mismo
            if (self.getHost().equals(parts[0]) && self.getClusterPort() == port) {
                return;
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, inetAddress, port);
            socket.send(packet);
        } catch (Exception e) {
            logger.trace("No se pudo enviar heartbeat a {}: {}", address, e.getMessage());
        }
    }

    // ── Recepción ──────────────────────────────────────────────────────────

    /**
     * Procesa un heartbeat entrante.
     *
     * Parsea el remitente directo (partes 1-3) y la lista de miembros
     * incluida en el mensaje (partes 4+). Cualquier miembro desconocido
     * se agrega a la MembershipList y dispara onNodeJoined en el EventBus.
     */
    private void processIncomingHeartbeat(DatagramPacket packet) {
        try {
            String message = new String(
                    packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
            String[] parts = message.split("\\|");

            if (parts.length < 4 || !"HEARTBEAT".equals(parts[0])) {
                return;
            }

            // ── 1. Procesar el remitente directo ──────────────────────────
            String nodeId     = parts[1];
            String host       = parts[2];
            int clusterPort   = Integer.parseInt(parts[3]);

            if (!nodeId.equals(self.getNodeId())) {
                NodeInfo sender = new NodeInfo(nodeId, host, clusterPort);
                boolean isNew = membershipList.addOrUpdate(sender);
                if (isNew) {
                    logger.info("Nodo descubierto (directo): {}", sender);
                    eventBus.publish(ClusterEvent.NODE_JOINED, sender);
                }
            }

            // ── 2. Procesar miembros incluidos en el heartbeat ────────────
            //    Formato de cada miembro: "memberId:memberHost:memberPort"
            for (int i = 4; i < parts.length; i++) {
                String[] m = parts[i].split(":");
                if (m.length < 3) continue;

                String mId   = m[0];
                String mHost = m[1];
                int    mPort;
                try { mPort = Integer.parseInt(m[2]); } catch (NumberFormatException e) { continue; }

                // Ignorarse a sí mismo
                if (mId.equals(self.getNodeId())) continue;

                NodeInfo memberNode = new NodeInfo(mId, mHost, mPort);
                boolean isNewMember = membershipList.addOrUpdate(memberNode);
                if (isNewMember) {
                    logger.info("Nodo descubierto (via gossip de '{}'): {}", nodeId, memberNode);
                    eventBus.publish(ClusterEvent.NODE_JOINED, memberNode);
                }
            }

        } catch (Exception e) {
            logger.warn("Error procesando heartbeat entrante", e);
        }
    }

    // ── Detección de fallos ────────────────────────────────────────────────

    private void checkForFailures() {
        for (MemberEntry entry : membershipList.getAllEntries()) {
            long elapsed = entry.getTimeSinceLastHeartbeatMs();
            NodeInfo nodeInfo = entry.getNodeInfo();

            if (elapsed > failureTimeoutMs && entry.getState() != NodeState.DOWN) {
                if (membershipList.markDown(nodeInfo.getNodeId())) {
                    eventBus.publish(ClusterEvent.NODE_LEFT, nodeInfo);
                }
            } else if (elapsed > suspectTimeoutMs && entry.getState() == NodeState.ALIVE) {
                if (membershipList.markSuspected(nodeInfo.getNodeId())) {
                    eventBus.publish(ClusterEvent.NODE_SUSPECTED, nodeInfo);
                }
            }
        }
    }

    public void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdownNow();
        if (socket != null && !socket.isClosed()) socket.close();
    }
}

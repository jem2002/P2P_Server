package discovery;

import models.enums.ClusterEvent;
import events.NetworkEventBus;
import models.RemoteNodeInfo;
import models.LocalNodeInfo;
import models.enums.NodeState;
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
    private static final int HEARTBEAT_BUFFER_SIZE = 4096; // ampliado para la lista de miembros + URLs

    private final LocalNodeInfo self;
    private final MembershipList membershipList;
    private final NetworkEventBus eventBus;
    private final List<String> seedNodes;
    private final long heartbeatIntervalMs;
    private final long suspectTimeoutMs;
    private final long failureTimeoutMs;

    private volatile boolean running = true;
    private DatagramSocket socket;
    private ScheduledExecutorService scheduler;

    public GossipProtocol(LocalNodeInfo self, MembershipList membershipList,
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
     * Construye el heartbeat incluyendo la URL del gateway propio y la lista de miembros conocidos.
     *
     * Formato:
     *   HEARTBEAT|selfId|selfHost|selfPort|selfGatewayUrl[|memberId:memberHost:memberPort:memberGateway]*
     *
     * La posición 4 transporta la URL del API Gateway del remitente.
     * Cada miembro usa 4 campos separados por ":" (split con límite 4 para preservar "http://").
     */

    private String buildHeartbeatMessage() {
        StringBuilder sb = new StringBuilder();
        // 4 campos base en la cabecera: HEARTBEAT | nodeId | host | clusterPort
        sb.append("HEARTBEAT|")
                .append(self.getNodeId()).append("|")
                .append(self.getHost()).append("|")
                .append(self.getClusterPort());

        // El 5to campo en adelante representa la lista de miembros ALIVE conocidos (excluirse a sí mismo)
        for (RemoteNodeInfo node : membershipList.getAliveNodes()) {
            sb.append("|")
                    .append(node.getNodeId()).append(":")
                    .append(node.getHost()).append(":")
                    .append(node.getClusterPort()).append(":")
                    .append(node.getGatewayUrl()); // gateway del miembro propagado
        }
        return sb.toString();
    }

    /**
     * Envía el heartbeat a seeds + todos los miembros no-DOWN.
     * Se sondean también nodos JOINING y SUSPECTED para que puedan
     * responder con su propio heartbeat directo y así transitar a ALIVE.
     */
    private void sendHeartbeats() {
        String heartbeat = buildHeartbeatMessage();
        byte[] data = heartbeat.getBytes(StandardCharsets.UTF_8);

        // Bootstrap: seeds siempre reciben heartbeat
        for (String seedAddress : seedNodes) {
            sendUdpPacket(data, seedAddress);
        }

        // Todos los miembros conocidos que no están caídos
        for (RemoteNodeInfo node : membershipList.getNonDownNodes()) {
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
     * Formato esperado de 4 campos base + N miembros:
     * HEARTBEAT|nodeId|host|clusterPort[|memberId:memberHost:memberPort:memberGateway]*
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

            // Al tener 4 campos obligatorios iniciales (índices 0, 1, 2, 3), la longitud mínima debe ser 4
            if (parts.length < 4 || !"HEARTBEAT".equals(parts[0])) {
                return;
            }

            // ── 1. Procesar el remitente directo (Campos 0 a 3) ───────────────────────
            String nodeId     = parts[1];
            String host       = parts[2];
            int clusterPort   = Integer.parseInt(parts[3]);

            // Nota: Si el remitente es directo, extraemos el gateway que tengamos en su registro o asumimos vacío
            // si aún no se ha recibido mediante gossip, ya que no viaja en la cabecera pura de 4 campos.
            if (!nodeId.equals(self.getNodeId())) {
                // Inicialmente se asume vacío o se busca en una resolución previa si aplica
                RemoteNodeInfo sender = new RemoteNodeInfo(nodeId, host, clusterPort, "");
                boolean isNew = membershipList.addOrUpdate(sender);
                if (isNew) {
                    logger.info("Nodo descubierto (directo): {}", sender);
                    eventBus.publish(ClusterEvent.NODE_JOINED, sender);
                }
            }

            // ── 2. Procesar miembros incluidos en el heartbeat (A partir del índice 4) ──
            //    IMPORTANTE: usar addIfAbsent (NO addOrUpdate) para los miembros
            //    propagados via gossip. Solo el heartbeat DIRECTO de un nodo
            //    renueva su timer; un nodo muerto no debe quedar "vivo" porque
            //    otro nodo lo mencione en su lista de miembros.
            for (int i = 4; i < parts.length; i++) {
                // Formato de cada miembro: "nodeId:host:port:gatewayUrl"
                // Se usa split con límite 4 para preservar el formato "http://" si viniera en la URL del gateway
                String[] m = parts[i].split(":", 4);
                if (m.length < 3) continue; // Al menos requiere nodeId, host y port

                String mId      = m[0];
                String mHost    = m[1];
                int    mPort;
                try { mPort = Integer.parseInt(m[2]); } catch (NumberFormatException e) { continue; }
                String mGateway = m.length == 4 ? m[3] : "";

                // Ignorarse a sí mismo
                if (mId.equals(self.getNodeId())) continue;

                RemoteNodeInfo memberNode = new RemoteNodeInfo(mId, mHost, mPort, mGateway);
                boolean isNewMember = membershipList.addIfAbsent(memberNode);
                if (isNewMember) {
                    logger.info("Nodo descubierto (via gossip de '{}'): {} gateway={}",
                            nodeId, memberNode, mGateway.isEmpty() ? "(ninguno)" : mGateway);
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
            RemoteNodeInfo remoteNodeInfo = entry.getNodeInfo();
            NodeState state = entry.getState();

            if (elapsed > failureTimeoutMs && state != NodeState.DOWN) {
                // ALIVE / JOINING / SUSPECTED  → DOWN
                if (membershipList.markDown(remoteNodeInfo.getNodeId())) {
                    eventBus.publish(ClusterEvent.NODE_LEFT, remoteNodeInfo);
                }
            } else if (elapsed > suspectTimeoutMs
                    && (state == NodeState.ALIVE || state == NodeState.JOINING)) {
                // ALIVE / JOINING  → SUSPECTED
                if (membershipList.markSuspected(remoteNodeInfo.getNodeId())) {
                    eventBus.publish(ClusterEvent.NODE_SUSPECTED, remoteNodeInfo);
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

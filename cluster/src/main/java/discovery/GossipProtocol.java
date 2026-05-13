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
 * Implementación simplificada del Gossip Protocol para descubrimiento
 * y detección de fallos en la red P2P.
 *
 * Funcionamiento:
 *   1. Cada {@code heartbeatIntervalMs} envía un heartbeat UDP a los seed nodes
 *      y a todos los nodos conocidos.
 *   2. Un hilo de detección de fallos verifica periódicamente si algún nodo
 *      ha dejado de enviar heartbeats.
 *   3. Si un nodo no responde en {@code suspectTimeoutMs} → SUSPECTED.
 *   4. Si no responde en {@code failureTimeoutMs} → DOWN.
 *
 * Principios aplicados:
 *   - SRP: solo maneja heartbeats y detección. La reacción es del EventBus.
 *   - Observer (indirecto): publica eventos vía NetworkEventBus.
 */
public class GossipProtocol implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(GossipProtocol.class);
    private static final int HEARTBEAT_BUFFER_SIZE = 512;

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

    /**
     * @param self               Identidad de este nodo
     * @param membershipList     Lista compartida de membresía
     * @param eventBus           Bus de eventos para notificar cambios
     * @param seedNodes          Lista de direcciones semilla ("host:port")
     * @param heartbeatIntervalMs Intervalo entre heartbeats (ej. 2000ms)
     * @param failureTimeoutMs   Timeout para declarar nodo DOWN (ej. 10000ms)
     */
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

            // Programar envío periódico de heartbeats
            scheduler = Executors.newScheduledThreadPool(2);
            scheduler.scheduleAtFixedRate(this::sendHeartbeats,
                    0, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
            scheduler.scheduleAtFixedRate(this::checkForFailures,
                    heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);

            // Bucle principal: escuchar heartbeats entrantes
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

    /**
     * Envía un heartbeat UDP a todos los seed nodes y nodos conocidos.
     * Formato del heartbeat: "HEARTBEAT|nodeId|host|clusterPort"
     */
    private void sendHeartbeats() {
        String heartbeat = String.format("HEARTBEAT|%s|%s|%d",
                self.getNodeId(), self.getHost(), self.getClusterPort());
        byte[] data = heartbeat.getBytes(StandardCharsets.UTF_8);

        // Enviar a seed nodes (bootstrap)
        for (String seedAddress : seedNodes) {
            sendUdpPacket(data, seedAddress);
        }

        // Enviar a nodos conocidos vivos
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

            // No enviar heartbeat a sí mismo
            if (self.getHost().equals(parts[0]) && self.getClusterPort() == port) {
                return;
            }

            DatagramPacket packet = new DatagramPacket(data, data.length, inetAddress, port);
            socket.send(packet);
        } catch (Exception e) {
            logger.trace("No se pudo enviar heartbeat a {}: {}", address, e.getMessage());
        }
    }

    /**
     * Procesa un heartbeat entrante de otro nodo.
     */
    private void processIncomingHeartbeat(DatagramPacket packet) {
        try {
            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            String[] parts = message.split("\\|");

            if (parts.length < 4 || !"HEARTBEAT".equals(parts[0])) {
                return;
            }

            String nodeId = parts[1];
            String host = parts[2];
            int clusterPort = Integer.parseInt(parts[3]);

            // Ignorar heartbeats propios
            if (nodeId.equals(self.getNodeId())) {
                return;
            }

            NodeInfo nodeInfo = new NodeInfo(nodeId, host, clusterPort);
            boolean isNew = membershipList.addOrUpdate(nodeInfo);

            if (isNew) {
                eventBus.publish(ClusterEvent.NODE_JOINED, nodeInfo);
            }

        } catch (Exception e) {
            logger.warn("Error procesando heartbeat entrante", e);
        }
    }

    /**
     * Verifica periódicamente si algún nodo ha dejado de enviar heartbeats.
     */
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

    /**
     * Detiene el protocolo Gossip de forma segura.
     */
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}

package communication;

import events.NetworkEventListener;
import events.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Pool de conexiones TCP a los servidores peer.
 * Mantiene una conexión persistente a cada nodo vivo.
 *
 * Implementa NetworkEventListener para abrir/cerrar conexiones
 * automáticamente cuando los nodos se unen o abandonan la red.
 *
 * Principio aplicado: Observer (GoF) — reacciona a eventos del cluster.
 */
public class PeerConnectionPool implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(PeerConnectionPool.class);
    private final ConcurrentHashMap<String, PeerConnection> connections = new ConcurrentHashMap<>();

    /**
     * Envía un mensaje a un peer específico.
     * Si la conexión no existe o está rota, intenta reconectar.
     */
    public void sendToPeer(String nodeId, String jsonMessage) throws Exception {
        PeerConnection conn = connections.get(nodeId);
        if (conn == null) {
            throw new IllegalStateException("No hay conexión al peer: " + nodeId);
        }

        try {
            conn.send(jsonMessage);
        } catch (Exception e) {
            logger.warn("Fallo enviando a peer {}, intentando reconectar...", nodeId);
            try {
                conn.close();
                conn.connect();
                conn.send(jsonMessage);
            } catch (Exception retryEx) {
                logger.error("Reconexión fallida a peer {}", nodeId);
                throw retryEx;
            }
        }
    }

    /**
     * Envía un mensaje a todos los peers conectados.
     */
    public void broadcastToPeers(String jsonMessage) {
        for (PeerConnection conn : connections.values()) {
            try {
                conn.send(jsonMessage);
            } catch (Exception e) {
                logger.warn("Fallo broadcast a peer {}", conn.getTargetNodeId());
            }
        }
    }

    /**
     * Obtiene la conexión a un peer específico.
     */
    public PeerConnection getConnection(String nodeId) {
        return connections.get(nodeId);
    }

    /**
     * Número de conexiones activas.
     */
    public int activeCount() {
        int count = 0;
        for (PeerConnection conn : connections.values()) {
            if (conn.isConnected()) count++;
        }
        return count;
    }

    // ============ NetworkEventListener ============

    @Override
    public void onNodeJoined(NodeInfo node) {
        String nodeId = node.getNodeId();
        if (connections.containsKey(nodeId)) {
            return; // Ya existe conexión
        }

        PeerConnection conn = new PeerConnection(nodeId, node.getHost(), node.getClusterPort());
        connections.put(nodeId, conn);

        // Conectar en un hilo separado para no bloquear el EventBus
        new Thread(() -> {
            try {
                conn.connect();
                logger.info("Conexión TCP establecida con nuevo peer: {}", node);
            } catch (Exception e) {
                logger.warn("No se pudo conectar al peer recién detectado: {}", node);
            }
        }, "PeerConnect-" + nodeId).start();
    }

    @Override
    public void onNodeLeft(NodeInfo node) {
        PeerConnection conn = connections.remove(node.getNodeId());
        if (conn != null) {
            conn.close();
            logger.info("Conexión cerrada con peer caído: {}", node);
        }
    }

    /**
     * Cierra todas las conexiones (para shutdown).
     */
    public void closeAll() {
        for (PeerConnection conn : connections.values()) {
            conn.close();
        }
        connections.clear();
    }
}

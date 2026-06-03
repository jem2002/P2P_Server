package communication;

import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Pool de conexiones TCP a los servidores peer.
 * Mantiene una conexión persistente a cada nodo vivo.
 */
public class PeerConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(PeerConnectionPool.class);
    private final ConcurrentHashMap<String, PeerConnection> connections = new ConcurrentHashMap<>();

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

    public void broadcastToPeers(String jsonMessage) {
        for (PeerConnection conn : connections.values()) {
            try {
                conn.send(jsonMessage);
            } catch (Exception e) {
                logger.warn("Fallo broadcast a peer {}", conn.getTargetNodeId());
            }
        }
    }

    public PeerConnection getConnection(String nodeId) {
        return connections.get(nodeId);
    }

    public int activeCount() {
        int count = 0;
        for (PeerConnection conn : connections.values()) {
            if (conn.isConnected()) count++;
        }
        return count;
    }

    // ============ MÉTODOS DE CONEXIÓN / DESCONEXIÓN ============

    /**
     * Agrega un nuevo peer al pool y lanza la conexión.
     */
    public boolean connectToPeer(RemoteNodeInfo node) {
        String nodeId = node.getNodeId();
        if (connections.containsKey(nodeId)) {
            return false; // Ya existe conexión
        }

        PeerConnection conn = new PeerConnection(nodeId, node.getHost(), node.getClusterPort());
        connections.put(nodeId, conn);

        // Conectar en un hilo separado para no bloquear
        new Thread(() -> {
            try {
                conn.connect();
                logger.info("Conexión TCP establecida con nuevo peer: {}", node);
            } catch (Exception e) {
                logger.warn("No se pudo conectar al peer recién detectado: {}", node);
            }
        }, "PeerConnect-" + nodeId).start();

        return true;
    }

    /**
     * Remueve un peer del pool y cierra su conexión.
     */
    public boolean disconnectFromPeer(RemoteNodeInfo node) {
        PeerConnection conn = connections.remove(node.getNodeId());
        if (conn != null) {
            conn.close();
            logger.info("Conexión cerrada con peer caído: {}", node);
            return true;
        }
        return false;
    }

    public void closeAll() {
        for (PeerConnection conn : connections.values()) {
            conn.close();
        }
        connections.clear();
    }
}
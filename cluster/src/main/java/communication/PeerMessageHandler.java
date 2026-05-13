package communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handler para mensajes entrantes de servidores peer.
 * Procesa las acciones del protocolo inter-servidor:
 *   - PEER_REPLICATE: aplicar evento de replicación
 *   - PEER_SYNC: sincronizar tabla de enrutamiento
 *   - PEER_HEALTH: responder con estado de salud
 *   - PEER_ROUTE: reenviar mensaje a un cliente local
 *
 * Principio aplicado: Controller (GRASP) — punto de entrada coordinador
 * para los mensajes inter-servidor.
 */
public class PeerMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(PeerMessageHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReplicationManager replicationManager;
    private final RoutingTable routingTable;

    public PeerMessageHandler(ReplicationManager replicationManager, RoutingTable routingTable) {
        this.replicationManager = replicationManager;
        this.routingTable = routingTable;
    }

    /**
     * Gestiona una conexión peer entrante. Lee mensajes JSON línea por línea
     * hasta que la conexión se cierre.
     */
    public void handlePeerConnection(Socket peerSocket) {
        String peerAddress = peerSocket.getRemoteSocketAddress().toString();
        try (InputStream in = peerSocket.getInputStream()) {
            logger.info("Procesando conexión peer desde {}", peerAddress);

            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    String line = lineBuffer.toString(StandardCharsets.UTF_8.name()).trim();
                    lineBuffer.reset();
                    if (!line.isEmpty()) {
                        processMessage(line, peerAddress);
                    }
                } else if (c != '\r') {
                    lineBuffer.write(c);
                }
            }
        } catch (Exception e) {
            logger.debug("Conexión peer cerrada desde {}: {}", peerAddress, e.getMessage());
        } finally {
            try {
                if (!peerSocket.isClosed()) peerSocket.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Despacha un mensaje JSON al handler correspondiente según su acción.
     */
    private void processMessage(String jsonMessage, String peerAddress) {
        try {
            JsonNode root = mapper.readTree(jsonMessage);
            String action = root.has("action") ? root.get("action").asText() : "";
            JsonNode payload = root.has("payload") ? root.get("payload") : null;

            switch (action) {
                case "PEER_REPLICATE":
                    handleReplicate(payload);
                    break;
                case "PEER_SYNC":
                    handleSync(payload);
                    break;
                case "PEER_HEALTH":
                    logger.debug("Health check recibido de {}", peerAddress);
                    break;
                default:
                    logger.warn("Acción peer desconocida: {} desde {}", action, peerAddress);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje peer desde {}", peerAddress, e);
        }
    }

    private void handleReplicate(JsonNode payload) {
        if (payload == null) return;
        try {
            ReplicationEvent event = ReplicationEvent.fromJson(payload.toString());
            replicationManager.handleIncoming(event);
        } catch (Exception e) {
            logger.error("Error procesando evento de replicación", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSync(JsonNode payload) {
        if (payload == null) return;
        try {
            String sourceNodeId = payload.has("sourceNodeId")
                    ? payload.get("sourceNodeId").asText() : "";
            JsonNode tableNode = payload.has("routingTable")
                    ? payload.get("routingTable") : null;

            if (tableNode != null && !sourceNodeId.isEmpty()) {
                Map<String, String> peerTable = mapper.convertValue(tableNode, Map.class);
                routingTable.syncFromPeer(sourceNodeId, peerTable);
            }
        } catch (Exception e) {
            logger.error("Error sincronizando tabla de enrutamiento", e);
        }
    }
}

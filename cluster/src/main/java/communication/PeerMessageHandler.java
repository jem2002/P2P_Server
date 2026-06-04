package communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.ReplicationEvent;
import replication.ReplicationManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Handler para mensajes entrantes de servidores peer.
 *
 * Procesa las acciones del protocolo inter-servidor:
 *   - PEER_REPLICATE:       Aplica evento de replicación (clientes conectados/desconectados, mensajes, docs)
 *   - PEER_SYNC:            Sincroniza tabla de enrutamiento con datos del peer
 *   - PEER_HEALTH:          Responde con estado de salud (no-op, el heartbeat ya lo maneja)
 *   - PEER_ROUTE:           Reenvía un mensaje JSON directamente al socket de un cliente local
 *   - PEER_BROADCAST:       Retransmite un mensaje a todos los clientes locales (BroadcastManager)
 *   - PEER_LOGS_REQUEST:    Devuelve los logs locales al peer solicitante
 *   - PEER_LOGS_RESPONSE:   Almacena los logs recibidos para que LIST_PEER_LOGS los lea
 *
 * Principio aplicado: Controller (GRASP) — punto de entrada coordinador
 * para los mensajes inter-servidor.
 */
public class PeerMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(PeerMessageHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReplicationManager replicationManager;


    public interface RouteDeliveryListener {
        void onDelivered(String targetUser, String fromUser, String rawContent, String clientIp);
    }

    /**
     * Callback que se invoca cuando un PEER_ROUTE es entregado exitosamente al cliente local.
     */
    private volatile RouteDeliveryListener onRouteDelivered;

    public PeerMessageHandler(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
    }



    /**
     * Gestiona una conexión peer entrante. Lee mensajes JSON línea por línea
     * hasta que la conexión se cierre.
     */
    public void handlePeerConnection(Socket peerSocket) {
        String peerAddress = peerSocket.getRemoteSocketAddress().toString();
        OutputStream peerOut = null;
        try {
            peerOut = peerSocket.getOutputStream();
            InputStream in = peerSocket.getInputStream();
            logger.info("Procesando conexión peer desde {}", peerAddress);

            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    String line = lineBuffer.toString(StandardCharsets.UTF_8.name()).trim();
                    lineBuffer.reset();
                    if (!line.isEmpty()) {
                        processMessage(line, peerAddress, peerOut);
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
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Despacha un mensaje JSON al handler correspondiente según su acción.
     */
    private void processMessage(String jsonMessage, String peerAddress, OutputStream peerOut) {
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

    }


}

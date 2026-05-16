package communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import registry.LocalClientRegistry;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;

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
    private final RoutingTable routingTable;
    private final LocalClientRegistry localClientRegistry;

    /** Proveedor de logs locales para responder a PEER_LOGS_REQUEST. */
    private volatile Supplier<String> localLogsSupplier;

    /** Callback para hacer broadcast a clientes locales (PEER_BROADCAST). */
    private volatile java.util.function.Consumer<String> localBroadcast;

    /** Callback que se invoca al recibir PEER_LOGS_RESPONSE, con (nodeId, logsJson). */
    private volatile java.util.function.BiConsumer<String, String> peerLogsReceiver;

    public interface RouteDeliveryListener {
        void onDelivered(String targetUser, String fromUser, String rawContent);
    }
    
    /**
     * Callback que se invoca cuando un PEER_ROUTE es entregado exitosamente al cliente local.
     */
    private volatile RouteDeliveryListener onRouteDelivered;

    public PeerMessageHandler(ReplicationManager replicationManager, RoutingTable routingTable,
                               LocalClientRegistry localClientRegistry) {
        this.replicationManager = replicationManager;
        this.routingTable = routingTable;
        this.localClientRegistry = localClientRegistry;
    }

    /** Inyecta el proveedor de logs locales (para responder a peticiones PEER_LOGS_REQUEST). */
    public void setLocalLogsSupplier(Supplier<String> supplier) {
        this.localLogsSupplier = supplier;
    }

    /** Inyecta el callback de broadcast local (para PEER_BROADCAST). */
    public void setLocalBroadcast(java.util.function.Consumer<String> broadcast) {
        this.localBroadcast = broadcast;
    }

    /** Inyecta el receptor de logs remotos (para LIST_PEER_LOGS). */
    public void setPeerLogsReceiver(java.util.function.BiConsumer<String, String> receiver) {
        this.peerLogsReceiver = receiver;
    }

    /** Inyecta el callback de persistencia para mensajes enrutados (PEER_ROUTE). */
    public void setOnRouteDelivered(RouteDeliveryListener callback) {
        this.onRouteDelivered = callback;
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
            } catch (Exception ignored) {}
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
                case "PEER_HEALTH":
                    logger.debug("Health check recibido de {}", peerAddress);
                    break;
                case "PEER_ROUTE":
                    handleRoute(payload);
                    break;
                case "PEER_BROADCAST":
                    handleBroadcast(payload);
                    break;
                case "PEER_LOGS_REQUEST":
                    handleLogsRequest(payload, peerOut);
                    break;
                case "PEER_LOGS_RESPONSE":
                    handleLogsResponse(payload);
                    break;
                default:
                    logger.warn("Acción peer desconocida: {} desde {}", action, peerAddress);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje peer desde {}", peerAddress, e);
        }
    }

    // ============ Handlers individuales ============

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

    /**
     * PEER_ROUTE: entrega un mensaje directamente al socket del cliente local destino.
     * Si el cliente no está en este servidor, descarta (ya no debería llegar aquí).
     * Si la entrega es exitosa, invoca onRouteDelivered para persistir copia local.
     */
    private void handleRoute(JsonNode payload) {
        if (payload == null) return;
        try {
            String targetUsername  = payload.get("targetUsername").asText();
            String originalMessage = payload.get("originalMessage").asText();
            String fromUser = payload.has("fromUser") ? payload.get("fromUser").asText() : "unknown";
            String rawContent = payload.has("rawContent") ? payload.get("rawContent").asText() : "";

            boolean delivered = localClientRegistry.deliver(targetUsername, originalMessage);
            if (!delivered) {
                logger.warn("PEER_ROUTE: cliente '{}' no encontrado localmente", targetUsername);
                return;
            }

            // Persistir copia local en el servidor receptor para que aparezca en LIST_MESSAGES
            if (onRouteDelivered != null) {
                try {
                    if (!rawContent.isBlank()) {
                        onRouteDelivered.onDelivered(targetUsername, fromUser, rawContent);
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo persistir copia local del mensaje enrutado: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando PEER_ROUTE", e);
        }
    }

    /**
     * PEER_BROADCAST: retransmite el mensaje a todos los clientes locales.
     * Esto es lo que reciben los peers cuando se hace un broadcast federado.
     */
    private void handleBroadcast(JsonNode payload) {
        if (payload == null) return;
        try {
            String message = payload.get("message").asText();
            if (localBroadcast != null) {
                localBroadcast.accept(message);
            }
        } catch (Exception e) {
            logger.error("Error procesando PEER_BROADCAST", e);
        }
    }

    /**
     * PEER_LOGS_REQUEST: responde con los logs locales serializados en JSON.
     */
    private void handleLogsRequest(JsonNode payload, OutputStream peerOut) {
        if (peerOut == null || localLogsSupplier == null) return;
        try {
            String requestingNodeId = payload != null && payload.has("requestingNodeId")
                    ? payload.get("requestingNodeId").asText() : "unknown";

            String logsJson = localLogsSupplier.get();
            // Construir respuesta y enviarla por el mismo socket
            String response = InterServerProtocol.buildLogsResponse("local", logsJson);
            byte[] bytes = (response + "\n").getBytes(StandardCharsets.UTF_8);
            peerOut.write(bytes);
            peerOut.flush();
            logger.info("Logs enviados al peer '{}' (PEER_LOGS_REQUEST)", requestingNodeId);
        } catch (Exception e) {
            logger.error("Error respondiendo PEER_LOGS_REQUEST", e);
        }
    }

    /**
     * PEER_LOGS_RESPONSE: notifica al receptor (LIST_PEER_LOGS) con los logs recibidos.
     */
    private void handleLogsResponse(JsonNode payload) {
        if (payload == null || peerLogsReceiver == null) return;
        try {
            String sourceNodeId = payload.has("sourceNodeId")
                    ? payload.get("sourceNodeId").asText() : "unknown";
            String logsJson = payload.has("logsJson")
                    ? payload.get("logsJson").asText() : "{}";
            peerLogsReceiver.accept(sourceNodeId, logsJson);
        } catch (Exception e) {
            logger.error("Error procesando PEER_LOGS_RESPONSE", e);
        }
    }
}

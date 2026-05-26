package orchestrators;

import DocumentService.DocumentManager;
import LogService.LogManager;
import UserService.UserManager;
import MessageParser.BroadcastManager;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Aplica eventos de replicación recibidos de otros nodos al dominio local.
 * Implementado en 'main' para tener acceso a todos los módulos y no violar dependencias.
 */
public class ReplicationEventApplier implements ReplicationManager.ReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationEventApplier.class);

    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final BroadcastManager broadcastManager;
    private final RoutingTable routingTable;
    private final String localNodeId;
    // Supplier para obtener listas actualizadas sin acoplamiento circular
    private final Supplier<String> clientsListSupplier;
    private final Supplier<String> documentsListSupplier;

    public ReplicationEventApplier(UserManager userManager,
                                   DocumentManager documentManager,
                                   BroadcastManager broadcastManager,
                                   RoutingTable routingTable,
                                   String localNodeId,
                                   Supplier<String> clientsListSupplier,
                                   Supplier<String> documentsListSupplier) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.routingTable = routingTable;
        this.localNodeId = localNodeId;
        this.clientsListSupplier = clientsListSupplier;
        this.documentsListSupplier = documentsListSupplier;
    }

    @Override
    public void apply(ReplicationEvent event) throws Exception {
        String type = event.getEventType();
        switch (type) {
            case "CLIENT_CONNECTED":
                handleClientConnected(event);
                break;
            case "CLIENT_DISCONNECTED":
                handleClientDisconnected(event);
                break;
            case "NEW_MESSAGE":
                handleNewMessage(event);
                break;
            case "DOCUMENT_UPLOADED":
                handleDocumentUploaded(event);
                break;
            default:
                logger.debug("Evento de replicación no manejado: {}", type);
        }
    }

    private void handleClientConnected(ReplicationEvent event) {
        // Un cliente se conectó a otro servidor → registrar en RoutingTable y crear en BD local si no existe
        String username = event.getPayload().get("username").asText();
        String sourceNode = event.getSourceNodeId();
        String clientIp = event.getPayload().has("ip") ? event.getPayload().get("ip").asText() : "unknown";

        try {
            userManager.obtenerORegistrarUsuario(username, clientIp);
        } catch (Exception e) {
            logger.error("Error registrando usuario conectado {}", username, e);
        }

        routingTable.registerRemoteClient(username, sourceNode);
        // Enviar lista actualizada SOLO a clientes locales (no federar de vuelta)
        try {
            broadcastManager.broadcastLocalOnly(clientsListSupplier.get());
        } catch (Exception ignored) {}
    }

    private void handleClientDisconnected(ReplicationEvent event) {
        // Un cliente se desconectó de otro servidor
        String username = event.getPayload().get("username").asText();
        routingTable.unregisterClient(username);
        try {
            broadcastManager.broadcastLocalOnly(clientsListSupplier.get());
        } catch (Exception ignored) {}
    }

    private void handleNewMessage(ReplicationEvent event) {
        // Un mensaje fue enviado en otro servidor → retransmitir a clientes locales
        String fromUser = event.getPayload().get("username").asText();
        String content  = event.getPayload().get("content").asText();

        try {
            long userId = userManager.obtenerIdUsuario(fromUser);
            java.io.InputStream textStream = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String nombreArchivo = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";
            documentManager.procesarRecepcionDocumento(textStream, nombreArchivo, content.length(), ".txt", "text/plain", userId, "replicado", "MESSAGE");
        } catch (Exception e) {
            logger.error("Error guardando mensaje replicado de {}", fromUser, e);
        }

        String msgJson  = "{\"action\":\"NEW_MESSAGE\",\"payload\":{\"message\":\"["
                          + event.getSourceNodeId() + "] De " + fromUser + ": " + content + "\"}}";
        broadcastManager.broadcastLocalOnly(msgJson);
    }

    private void handleDocumentUploaded(ReplicationEvent event) {
        // Un documento fue subido en otro servidor → registrar metadatos (Proxy) y actualizar lista local
        try {
            com.fasterxml.jackson.databind.JsonNode p = event.getPayload();
            long docId = p.get("documentId").asLong();
            String filename = p.get("filename").asText();
            long sizeBytes = p.get("sizeBytes").asLong();
            String extension = p.get("extension").asText();
            String mimeType = p.get("mimeType").asText();
            String docType = p.get("docType").asText();
            String ownerUsername = p.get("ownerUsername").asText();
            String ownerIp = p.get("ownerIp").asText();
            String host = p.get("host").asText();
            int clientPort = p.get("clientPort").asInt();

            long localUserId = userManager.obtenerIdUsuario(ownerUsername);
            documentManager.registrarDocumentoReplicado(filename, sizeBytes, extension, mimeType, docType, localUserId, ownerIp, host, clientPort, docId);
            broadcastManager.broadcastLocalOnly(documentsListSupplier.get());
        } catch (Exception ignored) {}
    }
}

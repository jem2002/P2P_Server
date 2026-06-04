package orchestrators;

import DocumentService.DocumentManager;
import ports.api.ClientActionHandler;
import UserService.UserManager;
import MessageParser.BroadcastManager;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Aplica eventos de replicación recibidos de otros nodos al dominio local.
 * Implementado en 'main' para tener acceso a todos los módulos y no violar dependencias.
 */
public class ReplicationEventApplier implements ReplicationManager.ReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationEventApplier.class);

    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listMessagesHandler;
    private final ClientActionHandler listClientsHandler;
    private final ClientActionHandler listDocumentsHandler;

    public ReplicationEventApplier(UserManager userManager,
                                   DocumentManager documentManager,
                                   BroadcastManager broadcastManager,
                                   ClientActionHandler listMessagesHandler,
                                   ClientActionHandler listClientsHandler,
                                   ClientActionHandler listDocumentsHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.listMessagesHandler = listMessagesHandler;
        this.listClientsHandler = listClientsHandler;
        this.listDocumentsHandler = listDocumentsHandler;
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
        int clientPort =  event.getPayload().has("port") ? event.getPayload().get("port").asInt() : 0;

        try {
            userManager.conectarUsuario(username, clientIp, clientPort, sourceNode);
        } catch (Exception e) {
            logger.error("Error registrando usuario conectado {}", username, e);
        }

        try {
            broadcastManager.broadcast(listClientsHandler.handle(null, null));
        } catch (Exception ignored) {}
    }

    private void handleClientDisconnected(ReplicationEvent event) {
        // Un cliente se desconectó de otro servidor
        String username = event.getPayload().get("username").asText();

        userManager.cerrarSesionPorUsername(username);

        try {
            broadcastManager.broadcast(listClientsHandler.handle(null, null));
        } catch (Exception ignored) {}
    }

    private void handleNewMessage(ReplicationEvent event) {
        // Un mensaje fue enviado en otro servidor → retransmitir a clientes locales
        String fromUser = event.getPayload().get("username").asText();
        String targetUsername = event.getPayload().get("targetUsername").asText();
        String content  = event.getPayload().get("content").asText();


        try {
            long userId = userManager.obtenerIdUsuario(fromUser);
            java.io.InputStream textStream = new java.io.ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String nombreArchivo = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";

            String docType = targetUsername.equals("ALL") ? "MESSAGE" : "PRIVATE_TO:" + targetUsername;

            documentManager.procesarRecepcionDocumento(textStream, nombreArchivo, content.length(), ".txt", "text/plain", userId, "replicado", docType);

            } catch (Exception e) {
            logger.error("Error guardando mensaje replicado de {}", fromUser, e);
        }
        try {
        broadcastManager.broadcast(listMessagesHandler.handle(null, null));
        } catch (Exception ignored) {}

    }

    private void handleDocumentUploaded(ReplicationEvent event) {
        // Un documento fue subido en otro servidor → descargar físicamente el archivo en background
        new Thread(() -> {
            try {
                com.fasterxml.jackson.databind.JsonNode p = event.getPayload();
                long docId = p.get("documentId").asLong();
                String filename = p.get("filename").asText();
                long sizeBytes = p.get("sizeBytes").asLong();
                String extension = p.get("extension").asText();
                String mimeType = p.get("mimeType").asText();
                String docType = p.get("docType").asText();
                String ownerUsername = p.get("ownerUsername").asText();
                String host = p.get("host").asText();
                int hostPort = p.get("hostPort").asInt();

                long localUserId = userManager.obtenerIdUsuario(ownerUsername);

                logger.info("Iniciando descarga física en background del documento remoto {} desde {}:{}", docId, host, hostPort);

                try (java.net.Socket controlSocket = new java.net.Socket(host, hostPort)) {
                    String req = "{\"action\":\"DOWNLOAD_INIT\", \"payload\":{\"document_id\":" + docId + ", \"format\":\"ORG\", \"username\":\"replicador\"}}\n";
                    controlSocket.getOutputStream().write(req.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    controlSocket.getOutputStream().flush();
                    
                    String remoteToken = null;
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(controlSocket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                    String res;
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    
                    while ((res = in.readLine()) != null) {
                        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(res);
                        String action = root.has("action") ? root.get("action").asText() : "";
                        
                        if ("DOWNLOAD_INIT_ACK".equals(action)) {
                            if (root.has("payload") && root.get("payload").has("message")) {
                                remoteToken = root.get("payload").get("message").asText();
                            }
                            break;
                        } else if ("ERROR_ACK".equals(action)) {
                            logger.error("Error devuelto por peer remoto al intentar replicar: {}", root.path("payload").path("reason").asText());
                            break;
                        }
                    }
                    
                    if (remoteToken != null) {
                        try (java.net.Socket dataSocket = new java.net.Socket(host, hostPort)) {
                            dataSocket.getOutputStream().write((remoteToken + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            dataSocket.getOutputStream().flush();
                            
                            java.io.InputStream peerIn = dataSocket.getInputStream();
                            documentManager.procesarRecepcionDocumento(peerIn, filename, sizeBytes, extension, mimeType, localUserId, "replicado", docType);
                            logger.info("Descarga física de replicación completada con éxito para {}", filename);
                            broadcastManager.broadcast(listDocumentsHandler.handle(null, null));
                        }
                    } else {
                        logger.error("Error en proxy P2P de replicación: No se obtuvo token del peer");
                    }
                }

            } catch (Exception e) {
                logger.error("Fallo durante la descarga física de replicación", e);
            }
        }, "ReplicationDownloader").start();
    }
}

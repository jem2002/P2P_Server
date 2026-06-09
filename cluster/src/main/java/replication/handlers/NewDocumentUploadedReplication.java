package replication.handlers;

import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

public class NewDocumentUploadedReplication implements IReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewDocumentUploadedReplication.class);
    private final IUserManager userManager;
    private final IDocumentManager documentManager;
    private final IBroadcastManager broadcastManager;
    private final ClientActionHandler listDocumentsHandler;


    public NewDocumentUploadedReplication(IUserManager userManager, IDocumentManager documentManager, IBroadcastManager broadcastManager, ClientActionHandler listDocumentsHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.listDocumentsHandler = listDocumentsHandler;
    }

    @Override
    public void apply(ReplicationEvent event) throws Exception {
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

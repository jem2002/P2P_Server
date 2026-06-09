package replication.handlers;

import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

public class NewMessageReplication implements IReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewMessageReplication.class);
    private final IUserManager userManager;
    private final IDocumentManager documentManager;
    private final IBroadcastManager broadcastManager;
    private final ClientActionHandler listMessagesHandler;

    public NewMessageReplication(IUserManager userManager, IDocumentManager documentManager, IBroadcastManager broadcastManager, ClientActionHandler listMessagesHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.listMessagesHandler = listMessagesHandler;
    }

    @Override
    public void apply(ReplicationEvent event) throws Exception {
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
}

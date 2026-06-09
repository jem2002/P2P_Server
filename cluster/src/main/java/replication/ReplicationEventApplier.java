package replication;

import com.universidad.messaging.server.shared.schema.ReplicationSchema;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.servicios.api.ICommentManager;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.handlers.*;
import service.EventDeduplicator;

import java.util.HashMap;
import java.util.Map;


/**
 * Aplica eventos de replicación recibidos de otros nodos al dominio local.
 * Implementado en 'main' para tener acceso a todos los módulos y no violar dependencias.
 */
public class ReplicationEventApplier {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationEventApplier.class);
    private final Map<String, IReplicationEventHandler> replicationHandlers = new HashMap<>();
    private final EventDeduplicator deduplicator;


    public ReplicationEventApplier(IUserManager userManager,
                                   IDocumentManager documentManager,
                                   ICommentManager commentManager,
                                   IBroadcastManager broadcastManager,
                                   ClientActionHandler listMessagesHandler,
                                   ClientActionHandler listClientsHandler,
                                   ClientActionHandler listDocumentsHandler,
                                   EventDeduplicator deduplicator
                                   ) {

        this.deduplicator = deduplicator;


        replicationHandlers.put(ReplicationSchema.NEW_CLIENT_CONNECTED,
                new NewClientConnectedReplication(userManager, broadcastManager, listClientsHandler));

        replicationHandlers.put(ReplicationSchema.NEW_CLIENT_DISCONNECTED,
                new NewClientDisconnectedReplication(userManager, broadcastManager, listClientsHandler));

        replicationHandlers.put(ReplicationSchema.NEW_COMMENT,
               new NewCommentReplication(commentManager));

        replicationHandlers.put(ReplicationSchema.NEW_MESSAGE,
                new NewMessageReplication(userManager, documentManager, broadcastManager, listMessagesHandler));

        replicationHandlers.put(ReplicationSchema.NEW_DOCUMENT_UPLOADED,
                new NewDocumentUploadedReplication(userManager, documentManager, broadcastManager, listDocumentsHandler));

    }


    public void route(ReplicationEvent event) throws Exception {

        if (!deduplicator.tryAccept(event.getEventId())) {
            logger.info("Evento duplicado ignorado: {}", event.getEventId());
            return;
        }

        logger.info("Evento de replicación recibido: {} desde {}",
                event.getEventType(), event.getSourceNodeId());


        String type = event.getEventType();

        IReplicationEventHandler handler = replicationHandlers.get(type);

        if (handler == null) {
            logger.error("Acción de replicacion no soportada.");
            return;
        }

        try {
            handler.apply(event);
        } catch (Exception e) {
            logger.error("Error en replicacion procesando acción: {}", event.getEventType(), e);
        }


    }


}

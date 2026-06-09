package replication.handlers;

import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

public class NewClientConnectedReplication implements IReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewClientConnectedReplication.class);
    private final IUserManager userManager;
    private final IBroadcastManager broadcastManager;
    private final ClientActionHandler listClientsHandler;

    public NewClientConnectedReplication(IUserManager userManager, IBroadcastManager broadcastManager, ClientActionHandler listClientsHandler)  {
        this.userManager = userManager;
        this.broadcastManager = broadcastManager;
        this.listClientsHandler = listClientsHandler;
    }


    @Override
    public void apply(ReplicationEvent event) throws Exception {
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
}

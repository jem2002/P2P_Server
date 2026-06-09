package replication.handlers;

import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

public class NewClientDisconnectedReplication implements IReplicationEventHandler {


    private static final Logger logger = LoggerFactory.getLogger(NewClientDisconnectedReplication.class);
    private final IUserManager userManager;
    private final IBroadcastManager broadcastManager;
    private final ClientActionHandler listClientsHandler;

    public NewClientDisconnectedReplication(IUserManager userManager, IBroadcastManager broadcastManager, ClientActionHandler listClientsHandler)  {
        this.userManager = userManager;
        this.broadcastManager = broadcastManager;
        this.listClientsHandler = listClientsHandler;
    }

    @Override
    public void apply(ReplicationEvent event) throws Exception {
        String username = event.getPayload().get("username").asText();

        userManager.cerrarSesionPorUsername(username);

        try {
            broadcastManager.broadcast(listClientsHandler.handle(null, null));
        } catch (Exception ignored) {}
    }


}

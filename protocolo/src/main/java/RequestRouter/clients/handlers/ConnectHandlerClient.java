package RequestRouter.clients.handlers;

import com.universidad.messaging.server.shared.schema.userSchema.ClientAddress;
import com.universidad.messaging.server.shared.schema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.gestion.cluster.api.IReplicationManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.universidad.messaging.server.servicios.api.ILogManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import com.universidad.messaging.server.shared.events.ReplicationEvent;



import java.io.OutputStream;

public class ConnectHandlerClient implements ClientActionHandler {

    private final IUserManager userManager;
    private final ILogManager logManager;
    private final ResponseBuilder serializer;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listClientsHandler;

    // Dependencias de clúster obligatorias e inmutables
    private final IReplicationManager replicationManager;
    private final String localNodeId;

    // Mantiene el stream del cliente actual mapeado al hilo de ejecución de la solicitud
    private final ThreadLocal<OutputStream> clientOut = new ThreadLocal<>();

    public ConnectHandlerClient(IUserManager userManager, ILogManager logManager,
                                ResponseBuilder serializer, BroadcastManager broadcastManager,
                                ClientActionHandler listClientsHandler,
                                IReplicationManager replicationManager,
                                String localNodeId) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.serializer = serializer;
        this.broadcastManager = broadcastManager;
        this.listClientsHandler = listClientsHandler;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;
    }


    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        if (payload == null || !payload.has(JsonSchema.PAYLOAD_USERNAME)) {
            return serializer.buildErrorResponse("Falta el username.");
        }

        String username = payload.get(JsonSchema.PAYLOAD_USERNAME).asText();
        ClientAddress address = ClientAddress.parse(clientIp);

        // 1. Persistencia y login en la base de datos local
        long userId = userManager.conectarUsuario(username, address.getIp(), address.getPort(), localNodeId);

        // 2. Registro en la bitácora interna de auditoría
        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS",
                "Usuario " + username + " conectado desde " + address);

        // Propagar de forma inmediata el evento de conexión a todos los peers del clúster
        ReplicationEvent event = ReplicationEvent.clientConnected(localNodeId, username, address.getIp(), address.getPort());
        replicationManager.propagate(event);

        // Broadcast global a la red para refrescar las listas de clientes en las UI
        broadcastManager.broadcast(listClientsHandler.handle(null, null));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
}

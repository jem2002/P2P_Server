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

public class DisconnectHandlerClient implements ClientActionHandler {

    private final IUserManager userManager;
    private final ILogManager logManager;
    private final ResponseBuilder serializer;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listClientsHandler;

    private final IReplicationManager replicationManager;
    private final String localNodeId;

    public DisconnectHandlerClient(IUserManager userManager, ILogManager logManager,
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

        // 1. Cerrar sesión en la base de datos local
        userManager.cerrarSesionPorUsername(username);
        long userId = userManager.obtenerIdUsuario(username);

        // 2. Registro en la bitácora interna de auditoría
        logManager.registrarAccion(null, userId > 0 ? userId : -1, "DISCONNECT", "SUCCESS",
                "Usuario " + username + " desconectado explícitamente desde " + address);

        // Propagar evento de desconexión a todos los peers del clúster
        ReplicationEvent event = ReplicationEvent.clientDisconnected(localNodeId, username);
        replicationManager.propagate(event);

        // Broadcast global a la red para refrescar las listas de clientes en las UI locales
        broadcastManager.broadcast(listClientsHandler.handle(null, null));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_DISCONNECT, "Usuario desconectado exitosamente.");
    }
}

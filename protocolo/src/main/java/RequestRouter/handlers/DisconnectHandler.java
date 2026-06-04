package RequestRouter.handlers;

import JsonSchema.ClientAddress;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import ports.api.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;

public class DisconnectHandler implements ActionHandler {

    private final UserManager userManager;
    private final LogManager logManager;
    private final ResponseBuilder serializer;
    private final BroadcastManager broadcastManager;
    private final ActionHandler listClientsHandler;

    private final RoutingTable routingTable;
    private final ReplicationManager replicationManager;
    private final String localNodeId;

    public DisconnectHandler(UserManager userManager, LogManager logManager,
                             ResponseBuilder serializer, BroadcastManager broadcastManager,
                             ActionHandler listClientsHandler, RoutingTable routingTable,
                             ReplicationManager replicationManager,
                             String localNodeId) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.serializer = serializer;
        this.broadcastManager = broadcastManager;
        this.listClientsHandler = listClientsHandler;
        this.routingTable = routingTable;
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

        // ── 3. INTEGRACIÓN CON CLÚSTER P2P ───────────────────
        
        // Desregistrar de la tabla de enrutamiento local
        routingTable.unregisterClient(username);

        // Propagar evento de desconexión a todos los peers del clúster
        ReplicationEvent event = ReplicationEvent.clientDisconnected(localNodeId, username);
        replicationManager.propagate(event);

        // Broadcast global a la red para refrescar las listas de clientes en las UI locales
        broadcastManager.broadcast(listClientsHandler.handle(null, null));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_DISCONNECT, "Usuario desconectado exitosamente.");
    }
}

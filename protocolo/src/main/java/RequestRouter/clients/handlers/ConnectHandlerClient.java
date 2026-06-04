package RequestRouter.clients.handlers;

import JsonSchema.ClientAddress;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import ports.api.ClientActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;

import java.io.OutputStream;

public class ConnectHandlerClient implements ClientActionHandler {

    private final UserManager userManager;
    private final LogManager logManager;
    private final ResponseBuilder serializer;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listClientsHandler;

    // Dependencias de clúster obligatorias e inmutables
    private final RoutingTable routingTable;
    private final ReplicationManager replicationManager;
    private final String localNodeId;

    // Mantiene el stream del cliente actual mapeado al hilo de ejecución de la solicitud
    private final ThreadLocal<OutputStream> clientOut = new ThreadLocal<>();

    public ConnectHandlerClient(UserManager userManager, LogManager logManager,
                                ResponseBuilder serializer, BroadcastManager broadcastManager,
                                ClientActionHandler listClientsHandler, RoutingTable routingTable,
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

        // 1. Persistencia y login en la base de datos local
        long userId = userManager.conectarUsuario(username, address.getIp(), address.getPort());

        // 2. Registro en la bitácora interna de auditoría
        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS",
                "Usuario " + username + " conectado desde " + address);

        // ── 3. INTEGRACIÓN CON CLÚSTER P2P (Siempre activo) ───────────────────

        // Registrar como cliente local en la tabla de enrutamiento en memoria
        routingTable.registerLocalClient(username);


        // Propagar de forma inmediata el evento de conexión a todos los peers del clúster
        ReplicationEvent event = ReplicationEvent.clientConnected(localNodeId, username, address.getIp(), address.getPort());
        replicationManager.propagate(event);

        // Broadcast global a la red para refrescar las listas de clientes en las UI
        broadcastManager.broadcast(listClientsHandler.handle(null, null));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
}

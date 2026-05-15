package RequestRouter.handlers;

import JsonSchema.ClientAddress;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import RequestRouter.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import registry.LocalClientRegistry;
import topology.RoutingTable;

import java.io.OutputStream;

/**
 * Maneja la acción CONNECT: registra o recupera un usuario, crea su sesión activa
 * y, en modo cluster, registra al cliente en la RoutingTable y propaga el evento
 * de conexión a todos los peers (para que actualicen su vista de clientes).
 *
 * Requerimiento cumplido: "Cada servidor deberá actualizar la información de los
 * clientes disponibles, deben incluir los clientes y los clientes de otros servidores."
 */
public class ConnectHandler implements ActionHandler {

    private final UserManager userManager;
    private final LogManager logManager;
    private final ResponseBuilder serializer;
    private final BroadcastManager broadcastManager;
    private final ListClientsHandler listClientsHandler;

    // Componentes de cluster (null si cluster deshabilitado)
    private RoutingTable routingTable;
    private ReplicationManager replicationManager;
    private LocalClientRegistry localClientRegistry;
    private String localNodeId;
    private OutputStream clientOut;   // Se inyecta por setter antes de cada handle()

    public ConnectHandler(UserManager userManager, LogManager logManager,
                          ResponseBuilder serializer, BroadcastManager broadcastManager,
                          ListClientsHandler listClientsHandler) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.serializer = serializer;
        this.broadcastManager = broadcastManager;
        this.listClientsHandler = listClientsHandler;
    }

    /** Inyecta dependencias de cluster (llamado desde ServerApplication si cluster habilitado). */
    public void enableCluster(RoutingTable routingTable, ReplicationManager replicationManager,
                               LocalClientRegistry localClientRegistry, String localNodeId) {
        this.routingTable = routingTable;
        this.replicationManager = replicationManager;
        this.localClientRegistry = localClientRegistry;
        this.localNodeId = localNodeId;
    }

    /**
     * Se debe llamar desde el ClientHandler/TCPServer antes de handle()
     * para que ConnectHandler pueda registrar el stream del cliente.
     */
    public void setClientOutputStream(OutputStream out) {
        this.clientOut = out;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        if (payload == null || !payload.has(JsonSchema.PAYLOAD_USERNAME)) {
            return serializer.buildErrorResponse("Falta el username.");
        }

        String username = payload.get(JsonSchema.PAYLOAD_USERNAME).asText();
        ClientAddress address = ClientAddress.parse(clientIp);

        long userId = userManager.conectarUsuario(username, address.getIp(), address.getPort());

        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS",
                "Usuario " + username + " conectado desde " + address);

        // --- Integración con cluster P2P ---
        if (routingTable != null) {
            // 1. Registrar como cliente local en la tabla de enrutamiento
            routingTable.registerLocalClient(username);

            // 2. Registrar el OutputStream para entrega directa de mensajes
            if (localClientRegistry != null && clientOut != null) {
                localClientRegistry.register(username, clientOut);
            }

            // 3. Propagar evento de conexión a todos los peers
            if (replicationManager != null && localNodeId != null) {
                ReplicationEvent event = ReplicationEvent.clientConnected(localNodeId, username);
                replicationManager.propagate(event);
            }

            // 4. Broadcast de lista actualizada (incluye cliente recién conectado)
            broadcastManager.broadcast(listClientsHandler.handle(null, null));
        }

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
}

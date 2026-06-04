package RequestRouter.clients.handlers;

import JsonSchema.ActiveClient;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import ports.api.ClientActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/**
 * Maneja la acción LIST_CLIENTS: retorna la lista de clientes activos.
 *
 * Se utiliza exclusivamente la base de datos (UserManager) como fuente de la verdad,
 * eliminando el uso del snapshot en memoria (RoutingTable).
 */
public class ListClientsHandlerClient implements ClientActionHandler {

    private final UserManager userManager;
    private final ResponseBuilder serializer;
    private final String localNodeId;

    public ListClientsHandlerClient(UserManager userManager, ResponseBuilder serializer, String localNodeId) {
        this.userManager = userManager;
        this.serializer = serializer;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();
        List<ActiveClient> dbClients = new ArrayList<>();

        // 1. Obtener la lista consolidada directamente desde la BD
        try {
            dbClients = userManager.obtenerClientesActivos();
        } catch (Exception e) {
            // Log de error opcional, continuará con la lista vacía
            System.err.println("Error obteniendo clientes de la BD: " + e.getMessage());
        }

        // 2. Clasificar cada cliente
        for (ActiveClient c : dbClients) {
            String username = c.getUsername();

            String clientNodeId = c.getNodeId();

            // Si el nodo es nulo o coincide con el local, lo marcamos como LOCAL
            if (clientNodeId == null || localNodeId.equals(clientNodeId)) {
                String effectiveNodeId = (clientNodeId != null) ? clientNodeId : localNodeId;
                result.add(buildLocal(username, c, effectiveNodeId));
            } else {
                // Si tiene un nodo distinto asignado, es REMOTO
                result.add(buildRemote(username, clientNodeId, c));
            }
        }

        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, result, "clientes");
    }

    // ── Helpers de Procesamiento ─────────────────────────────────────────────

    private Map<String, String> buildLocal(String username, ActiveClient c, String nodeId) {
        Map<String, String> item = new HashMap<>();
        item.put("username",     username);
        // Usamos valores por defecto "—" si el campo viene nulo desde la BD
        item.put("ip",           (c != null && c.getIp() != null) ? c.getIp() : "—");
        item.put("fecha_inicio", (c != null && c.getConnectedAt() != null) ? c.getConnectedAt() : "—");
        item.put("servidor",     nodeId);
        item.put("tipo",         "LOCAL");
        return item;
    }

    private Map<String, String> buildRemote(String username, String nodeId, ActiveClient c) {
        Map<String, String> item = new HashMap<>();
        item.put("username",     username);

        // Mantenemos "N/A" para emular tu lógica original en clientes remotos.
        // (Opcional: podrías cambiarlo a c.getIp() si ahora deseas mostrar la IP de la BD)
        item.put("ip",           "N/A");
        item.put("fecha_inicio", "N/A");
        item.put("servidor",     nodeId);
        item.put("tipo",         "REMOTO");
        return item;
    }
}
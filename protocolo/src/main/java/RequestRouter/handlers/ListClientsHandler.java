package RequestRouter.handlers;

import JsonSchema.ActiveClient;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import ports.api.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import topology.RoutingTable;

import java.util.*;

/**
 * Maneja la acción LIST_CLIENTS: retorna la lista consolidada de clientes activos.
 *
 * Problema con BD compartida:
 *   Cuando varios servidores comparten el mismo MySQL, obtenerClientesActivos()
 *   devuelve TODOS los usuarios activos de la red, sin distinguir a cuál nodo
 *   están conectados. Esto causaba que todos aparecieran como LOCAL en cada nodo.
 *
 * Solución — RoutingTable como fuente de verdad:
 *   Si el cluster está habilitado, la RoutingTable (en memoria, por nodo) sabe
 *   exactamente qué usuarios están conectados a ESTE nodo y cuáles a otros.
 *   Se usa como filtro autoritativo:
 *     - LOCAL  → RoutingTable.isLocal(username) == true
 *     - REMOTO → RoutingTable dice que pertenece a otro nodeId
 *   Para los clientes LOCALES se enriquece con datos de la BD (IP, timestamp).
 *
 * Modo standalone (sin cluster):
 *   Se comporta igual que antes: todos los registros activos de la BD = LOCAL.
 *
 * Requerimiento cumplido: "Cada servidor deberá actualizar la información de los
 * clientes disponibles, deben incluir los clientes y los clientes de otros servidores."
 */
public class ListClientsHandler implements ActionHandler {

    private final UserManager userManager;
    private final ResponseBuilder serializer;

    // Dependencias de clúster obligatorias e inmutables
    private final RoutingTable routingTable;
    private final String localNodeId;

    public ListClientsHandler(UserManager userManager, ResponseBuilder serializer,
                              RoutingTable routingTable, String localNodeId) {
        this.userManager = userManager;
        this.serializer = serializer;
        this.routingTable = routingTable;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        // ── Modo clúster: RoutingTable es la fuente de verdad absoluta ─────────────
        // 1. Snapshot de la tabla en memoria: username → nodeId
        Map<String, String> snapshot = routingTable.getSnapshot();

        // 2. Índice de metadatos desde la BD (para enriquecer únicamente a los locales)
        //    Construimos un Map<username, ActiveClient> para acceso O(1)
        Map<String, ActiveClient> dbIndex = buildDbIndex();

        // 3. Clasificar secuencialmente cada entrada de la RoutingTable
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            String username = entry.getKey();
            String nodeId   = entry.getValue();

            if (localNodeId.equals(nodeId)) {
                // CLIENTE LOCAL — buscar metadatos extendidos en la BD
                ActiveClient dbRecord = dbIndex.get(username);
                if (dbRecord != null) {
                    result.add(buildLocal(dbRecord, localNodeId));
                } else {
                    // Registrado en RoutingTable pero sin registro físico en BD aún
                    // (Sincronización en la fracción de segundo del CONNECT)
                    result.add(buildLocalStub(username, localNodeId));
                }
            } else {
                // CLIENTE REMOTO — no consultamos su BD local, usamos los datos del clúster
                result.add(buildRemote(username, nodeId));
            }
        }

        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, result, "clientes");
    }

    // ── Helpers de Procesamiento ─────────────────────────────────────────────

    private Map<String, ActiveClient> buildDbIndex() {
        Map<String, ActiveClient> index = new HashMap<>();
        try {
            for (ActiveClient c : userManager.obtenerClientesActivos()) {
                index.put(c.getUsername(), c);
            }
        } catch (Exception ignored) {}
        return index;
    }

    private Map<String, String> buildLocal(ActiveClient c, String nodeId) {
        Map<String, String> item = new HashMap<>();
        item.put("username",     c.getUsername());
        item.put("ip",           c.getIp());
        item.put("fecha_inicio", c.getConnectedAt());
        item.put("servidor",     nodeId);
        item.put("tipo",         "LOCAL");
        return item;
    }

    private Map<String, String> buildLocalStub(String username, String nodeId) {
        Map<String, String> item = new HashMap<>();
        item.put("username",     username);
        item.put("ip",           "—");
        item.put("fecha_inicio", "—");
        item.put("servidor",     nodeId);
        item.put("tipo",         "LOCAL");
        return item;
    }

    private Map<String, String> buildRemote(String username, String nodeId) {
        Map<String, String> item = new HashMap<>();
        item.put("username",     username);
        item.put("ip",           "N/A");
        item.put("fecha_inicio", "N/A");
        item.put("servidor",     nodeId);
        item.put("tipo",         "REMOTO");
        return item;
    }
}

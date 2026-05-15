package RequestRouter.handlers;

import JsonSchema.ActiveClient;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import RequestRouter.ActionHandler;
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

    // Nullable — solo disponible si cluster está habilitado
    private RoutingTable routingTable;
    private String localNodeId;

    public ListClientsHandler(UserManager userManager, ResponseBuilder serializer) {
        this.userManager = userManager;
        this.serializer = serializer;
    }

    /** Inyecta la tabla de enrutamiento para incluir clientes remotos. */
    public void enableFederatedList(RoutingTable routingTable, String localNodeId) {
        this.routingTable = routingTable;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, String>> result = new ArrayList<>();

        if (routingTable == null) {
            // ── Modo standalone: todos los activos de la BD son locales ──────
            for (ActiveClient c : userManager.obtenerClientesActivos()) {
                result.add(buildLocal(c, "local"));
            }
        } else {
            // ── Modo cluster: RoutingTable es la fuente de verdad ─────────────
            //
            // La BD compartida puede tener usuarios de TODOS los nodos, por lo
            // que NO podemos confiar en ella para saber si un cliente es LOCAL.
            // Usamos la RoutingTable que cada nodo mantiene en memoria.

            // 1. Snapshot de la tabla: username → nodeId
            Map<String, String> snapshot = routingTable.getSnapshot();

            // 2. Índice de metadatos desde BD (para enriquecer a los locales)
            //    Construimos Map<username, ActiveClient> para acceso O(1)
            Map<String, ActiveClient> dbIndex = buildDbIndex();

            // 3. Clasificar cada entrada de la RoutingTable
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                String username = entry.getKey();
                String nodeId   = entry.getValue();

                if (localNodeId.equals(nodeId)) {
                    // LOCAL — buscar metadatos en la BD
                    ActiveClient dbRecord = dbIndex.get(username);
                    if (dbRecord != null) {
                        result.add(buildLocal(dbRecord, localNodeId));
                    } else {
                        // Registrado en RoutingTable pero sin registro de BD aún
                        // (puede ocurrir en la fracción de segundo del CONNECT)
                        result.add(buildLocalStub(username, localNodeId));
                    }
                } else {
                    // REMOTO — no tenemos metadatos de BD; usamos lo que sabemos
                    result.add(buildRemote(username, nodeId));
                }
            }
        }

        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, result, "clientes");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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

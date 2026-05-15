package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import RequestRouter.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import discovery.MemberEntry;
import discovery.MembershipList;
import discovery.NodeState;
import health.ClusterHealthService;
import identity.NodeIdentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maneja la acción LIST_PEER_INFO: retorna el estado de todos los servidores
 * conocidos en la red (conectados y desconectados).
 *
 * Requerimiento cumplido:
 *   - "Detección de servidores amigos: listar servidores conectados y desconectados."
 *   - "Adicionar servicios para mostrar la información y los logs de otros servidores."
 *
 * Responde con la lista completa de nodos: ALIVE, SUSPECTED y DOWN.
 */
public class ListPeerInfoHandler implements ActionHandler {

    private final ResponseBuilder serializer;
    private final ClusterHealthService healthService;
    private final NodeIdentity localIdentity;
    private final MembershipList membershipList;

    public ListPeerInfoHandler(ResponseBuilder serializer,
                                ClusterHealthService healthService,
                                NodeIdentity localIdentity,
                                MembershipList membershipList) {
        this.serializer = serializer;
        this.healthService = healthService;
        this.localIdentity = localIdentity;
        this.membershipList = membershipList;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, String>> servidores = new ArrayList<>();

        // Agregar este servidor (siempre ALIVE)
        Map<String, String> local = new HashMap<>();
        local.put("nodeId", localIdentity.getNodeId());
        local.put("host", localIdentity.getHost());
        local.put("clusterPort", String.valueOf(localIdentity.getClusterPort()));
        local.put("clientPort", String.valueOf(localIdentity.getClientPort()));
        local.put("estado", "ALIVE");
        local.put("tipo", "LOCAL");
        servidores.add(local);

        // Agregar todos los nodos conocidos (ALIVE, SUSPECTED, DOWN)
        for (MemberEntry entry : membershipList.getAllEntries()) {
            Map<String, String> peer = new HashMap<>();
            peer.put("nodeId", entry.getNodeInfo().getNodeId());
            peer.put("host", entry.getNodeInfo().getHost());
            peer.put("clusterPort", String.valueOf(entry.getNodeInfo().getClusterPort()));
            peer.put("clientPort", "N/A");
            peer.put("estado", entry.getState().name());
            peer.put("ultimoHeartbeatMs", String.valueOf(entry.getTimeSinceLastHeartbeatMs()));
            peer.put("tipo", "REMOTO");
            servidores.add(peer);
        }

        return serializer.buildListResponse(JsonSchema.ACTION_LIST_PEER_INFO, servidores, "servidores");
    }
}

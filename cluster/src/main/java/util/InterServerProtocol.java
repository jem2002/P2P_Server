package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import topology.RoutingTable;

import java.util.Map;

/**
 * Utilidad para construir los mensajes JSON del protocolo inter-servidor.
 *
 * Principio aplicado: Pure Fabrication (GRASP) — centraliza la construcción
 * de mensajes para evitar duplicación y garantizar consistencia en el formato.
 */
public final class InterServerProtocol {

    private static final ObjectMapper mapper = new ObjectMapper();

    private InterServerProtocol() {}

    /**
     * Construye un mensaje de sincronización de tabla de enrutamiento.
     */
    public static String buildSyncMessage(String sourceNodeId, RoutingTable table) {
        ObjectNode root = mapper.createObjectNode();
        root.put("action", "PEER_SYNC");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("sourceNodeId", sourceNodeId);

        ObjectNode routingTableNode = mapper.createObjectNode();
        for (Map.Entry<String, String> entry : table.getSnapshot().entrySet()) {
            routingTableNode.put(entry.getKey(), entry.getValue());
        }
        payload.set("routingTable", routingTableNode);

        root.set("payload", payload);
        return root.toString();
    }



    /**
     * Construye una solicitud de logs dirigida a un peer.
     * El peer responderá con PEER_LOGS_RESPONSE.
     */
    public static String buildLogsRequest(String requestingNodeId) {
        ObjectNode root = mapper.createObjectNode();
        root.put("action", "PEER_LOGS_REQUEST");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("requestingNodeId", requestingNodeId);
        root.set("payload", payload);
        return root.toString();
    }


}

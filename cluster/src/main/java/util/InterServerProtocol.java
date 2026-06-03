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
     * Construye un mensaje de enrutamiento (reenviar mensaje a cliente remoto).
     */
    public static String buildRouteMessage(String targetUsername, String originalJson, String fromUser, String rawContent, String clientIp) {
        ObjectNode root = mapper.createObjectNode();
        root.put("action", "PEER_ROUTE");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("targetUsername", targetUsername);
        payload.put("originalMessage", originalJson);
        payload.put("fromUser", fromUser);
        payload.put("rawContent", rawContent);
        payload.put("clientIp", clientIp);

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

    /**
     * Construye una respuesta con los logs de este servidor.
     *
     * @param sourceNodeId NodeId del servidor que responde
     * @param logsJson     String JSON de la lista de logs (formato LIST_LOGS)
     */
    public static String buildLogsResponse(String sourceNodeId, String logsJson) {
        ObjectNode root = mapper.createObjectNode();
        root.put("action", "PEER_LOGS_RESPONSE");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("sourceNodeId", sourceNodeId);
        payload.put("logsJson", logsJson);
        root.set("payload", payload);
        return root.toString();
    }

}

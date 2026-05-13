package JsonSerializer;
import JsonSchema.JsonSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;


public class ResponseBuilder {
    private final ObjectMapper mapper;

    public ResponseBuilder() {
        this.mapper = new ObjectMapper();
    }

    public String buildSuccessResponse(String action, String message) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, action + "_ACK");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "SUCCESS");
        payload.put("message", message);

        root.set(JsonSchema.KEY_PAYLOAD, payload);

        return root.toString();
    }

    public String buildDownloadInitResponse(String token, long size, long id) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, "DOWNLOAD_INIT_ACK");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "SUCCESS");
        payload.put("message", token);
        payload.put("size_bytes", size);
        payload.put("document_id", id);

        root.set(JsonSchema.KEY_PAYLOAD, payload);

        return root.toString();
    }

    public String buildErrorResponse(String reason) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, JsonSchema.ACTION_ERROR);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "ERROR");
        payload.put(JsonSchema.PAYLOAD_REASON, reason);

        root.set(JsonSchema.KEY_PAYLOAD, payload);

        return root.toString();
    }
    public String buildListResponse(String action, List<Map<String, String>> items, String listName) {
        ObjectNode root = mapper.createObjectNode();
        root.put(JsonSchema.KEY_ACTION, action + "_ACK");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "SUCCESS");

        // Crear un Array JSON dinámico
        ArrayNode arrayNode = payload.putArray(listName);
        for (Map<String, String> item : items) {
            ObjectNode itemNode = mapper.createObjectNode();
            item.forEach(itemNode::put); // Transfiere el mapa al nodo JSON
            arrayNode.add(itemNode);
        }

        root.set(JsonSchema.KEY_PAYLOAD, payload);
        return root.toString();
    }
}
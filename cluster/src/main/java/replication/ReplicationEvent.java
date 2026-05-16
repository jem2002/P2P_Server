package replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.UUID;

/**
 * Comando de replicación que encapsula un cambio de datos a propagar
 * a todos los nodos de la red.
 *
 * Principio aplicado: Command (GoF) — cada evento de mutación se serializa
 * como un comando inmutable que puede enviarse, deserializarse y aplicarse
 * en cualquier nodo.
 *
 * Deduplicación: cada evento tiene un UUID único. Los nodos usan el
 * {@link EventDeduplicator} para evitar procesar el mismo evento dos veces.
 */
public class ReplicationEvent {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String eventId;
    private final String sourceNodeId;
    private final String eventType;
    private final long timestamp;
    private final JsonNode payload;

    public ReplicationEvent(String eventId, String sourceNodeId,
                            String eventType, long timestamp, JsonNode payload) {
        this.eventId = eventId;
        this.sourceNodeId = sourceNodeId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    // ============ Factory Methods ============

    public static ReplicationEvent newMessage(String sourceNodeId, String username, String content, String ip) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("content", content);
        payload.put("ip", ip);
        return create(sourceNodeId, "NEW_MESSAGE", payload);
    }

    public static ReplicationEvent clientConnected(String sourceNodeId, String username, String ip) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("ip", ip);
        return create(sourceNodeId, "CLIENT_CONNECTED", payload);
    }

    public static ReplicationEvent clientDisconnected(String sourceNodeId, String username) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        return create(sourceNodeId, "CLIENT_DISCONNECTED", payload);
    }

    public static ReplicationEvent documentUploaded(String sourceNodeId, long docId,
                                                     String filename, long sizeBytes,
                                                     String extension, String mimeType, String docType,
                                                     String ownerUsername, String ownerIp,
                                                     String host, int clientPort) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("documentId", docId);
        payload.put("filename", filename);
        payload.put("sizeBytes", sizeBytes);
        payload.put("extension", extension);
        payload.put("mimeType", mimeType);
        payload.put("docType", docType);
        payload.put("ownerUsername", ownerUsername);
        payload.put("ownerIp", ownerIp);
        payload.put("host", host);
        payload.put("clientPort", clientPort);
        return create(sourceNodeId, "DOCUMENT_UPLOADED", payload);
    }

    private static ReplicationEvent create(String sourceNodeId, String eventType, JsonNode payload) {
        return new ReplicationEvent(
                UUID.randomUUID().toString(),
                sourceNodeId,
                eventType,
                System.currentTimeMillis(),
                payload
        );
    }

    // ============ Serialización JSON ============

    public String toJson() {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("eventId", eventId);
            root.put("sourceNodeId", sourceNodeId);
            root.put("eventType", eventType);
            root.put("timestamp", timestamp);
            root.set("payload", payload);
            return root.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error serializando ReplicationEvent", e);
        }
    }

    public static ReplicationEvent fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            return new ReplicationEvent(
                    root.get("eventId").asText(),
                    root.get("sourceNodeId").asText(),
                    root.get("eventType").asText(),
                    root.get("timestamp").asLong(),
                    root.get("payload")
            );
        } catch (Exception e) {
            throw new RuntimeException("Error deserializando ReplicationEvent", e);
        }
    }

    // ============ Getters ============

    public String getEventId() { return eventId; }
    public String getSourceNodeId() { return sourceNodeId; }
    public String getEventType() { return eventType; }
    public long getTimestamp() { return timestamp; }
    public JsonNode getPayload() { return payload; }

    @Override
    public String toString() {
        return "ReplicationEvent[" + eventType + " from " + sourceNodeId + " id=" + eventId + "]";
    }
}

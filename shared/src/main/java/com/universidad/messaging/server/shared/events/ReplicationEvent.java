package com.universidad.messaging.server.shared.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.universidad.messaging.server.shared.schema.ReplicationSchema;

import java.math.BigDecimal;
import java.util.UUID;


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

    public static ReplicationEvent newMessage(String sourceNodeId, String username, String targetUsername, String content, String ip) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("content", content);
        payload.put("targetUsername", targetUsername);
        payload.put("ip", ip);
        return create(sourceNodeId, ReplicationSchema.NEW_MESSAGE, payload);
    }

    public static ReplicationEvent newComment(String sourceNodeId, Long id, Long documentId, String username, String content, String sentiment, BigDecimal confidence) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", id);
        payload.put("documentId", documentId);
        payload.put("username", username);
        payload.put("content", content);
        payload.put("sentiment", sentiment);
        payload.put("confidence", confidence);
        return create(sourceNodeId, ReplicationSchema.NEW_COMMENT, payload);
    }

    public static ReplicationEvent clientConnected(String sourceNodeId, String username, String ip, int port) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        payload.put("ip", ip);
        payload.put("port", port);
        return create(sourceNodeId, ReplicationSchema.NEW_CLIENT_CONNECTED, payload);
    }

    public static ReplicationEvent clientDisconnected(String sourceNodeId, String username) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("username", username);
        return create(sourceNodeId, ReplicationSchema.NEW_CLIENT_DISCONNECTED, payload);
    }

    public static ReplicationEvent documentUploaded(String sourceNodeId, long docId,
                                                     String filename, long sizeBytes,
                                                     String extension, String mimeType, String docType,
                                                     String ownerUsername,
                                                     String host, int hostPort) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("documentId", docId);
        payload.put("filename", filename);
        payload.put("sizeBytes", sizeBytes);
        payload.put("extension", extension);
        payload.put("mimeType", mimeType);
        payload.put("docType", docType);
        payload.put("ownerUsername", ownerUsername);
        payload.put("host", host);
        payload.put("hostPort", hostPort);
        return create(sourceNodeId, ReplicationSchema.NEW_DOCUMENT_UPLOADED, payload);
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

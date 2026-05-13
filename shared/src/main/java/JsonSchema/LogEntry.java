package JsonSchema;

/**
 * DTO inmutable para entradas de la bitácora de auditoría.
 * Reemplaza Map<String, String> con tipado fuerte.
 */
public final class LogEntry {

    private final long id;
    private final String documentId;
    private final String sender;
    private final String action;
    private final String protocol;
    private final String status;
    private final String details;
    private final String timestamp;

    public LogEntry(long id, String documentId, String sender, String action,
                    String protocol, String status, String details, String timestamp) {
        this.id = id;
        this.documentId = documentId;
        this.sender = sender;
        this.action = action;
        this.protocol = protocol;
        this.status = status;
        this.details = details;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getSender() {
        return sender;
    }

    public String getAction() {
        return action;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    public String getTimestamp() {
        return timestamp;
    }
}

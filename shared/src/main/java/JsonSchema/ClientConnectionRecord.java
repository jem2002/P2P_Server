package JsonSchema;

public class ClientConnectionRecord {
    private final long id;
    private final long userId;
    private final String ipAddress;
    private final int port;
    private final String connectedAt;
    private final String disconnectedAt;
    private final String protocol;
    private final boolean isActive;

    public ClientConnectionRecord(long id, long userId, String ipAddress, int port,
                                  String connectedAt, String disconnectedAt, String protocol, boolean isActive) {
        this.id = id;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.connectedAt = connectedAt;
        this.disconnectedAt = disconnectedAt;
        this.protocol = protocol;
        this.isActive = isActive;
    }

    // Getters...
}
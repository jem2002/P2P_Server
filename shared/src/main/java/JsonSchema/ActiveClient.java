package JsonSchema;

/**
 * DTO inmutable que representa un cliente activo conectado al servidor.
 * Reemplaza Map<String, String> con tipado fuerte.
 */
public final class ActiveClient {

    private final String username;
    private final String ip;
    private final String connectedAt;

    public ActiveClient(String username, String ip, String connectedAt) {
        this.username = username;
        this.ip = ip;
        this.connectedAt = connectedAt;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getConnectedAt() {
        return connectedAt;
    }
}

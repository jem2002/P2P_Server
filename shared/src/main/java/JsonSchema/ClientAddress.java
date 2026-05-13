package JsonSchema;

/**
 * Value Object inmutable que representa una dirección de cliente (IP + Puerto).
 * Centraliza la lógica de parsing que antes estaba duplicada en MainRouter.
 *
 * Principios aplicados:
 *   - Information Expert (GRASP): el parsing vive donde se usan los datos.
 *   - DRY: elimina duplicación de lógica de parsing de IP.
 */
public final class ClientAddress {

    private final String ip;
    private final int port;

    public ClientAddress(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * Parsea una dirección cruda del socket (ej: "/192.168.1.5:54321") a un ClientAddress limpio.
     */
    public static ClientAddress parse(String rawAddress) {
        String clean = rawAddress.replace("/", "");
        if (clean.contains(":")) {
            String[] parts = clean.split(":");
            return new ClientAddress(parts[0], Integer.parseInt(parts[1]));
        }
        return new ClientAddress(clean, 0);
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}

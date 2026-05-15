package JsonSchema;

/**
 * DTO que representa un cliente conectado a cualquier servidor de la red P2P.
 *
 * Para clientes locales: el serverNodeId es el nodeId de este servidor.
 * Para clientes remotos: el serverNodeId identifica el servidor que los hospeda.
 * Los clientes remotos se almacenan solo en memoria (RoutingTable), NO en la BD local.
 */
public class FederatedClientInfo {

    private final String username;
    private final String ip;
    private final String connectedAt;
    private final String serverNodeId;
    private final boolean isLocal;

    public FederatedClientInfo(String username, String ip, String connectedAt,
                                String serverNodeId, boolean isLocal) {
        this.username = username;
        this.ip = ip;
        this.connectedAt = connectedAt;
        this.serverNodeId = serverNodeId;
        this.isLocal = isLocal;
    }

    /** Constructor para clientes locales (con información completa de BD). */
    public static FederatedClientInfo local(String username, String ip,
                                             String connectedAt, String localNodeId) {
        return new FederatedClientInfo(username, ip, connectedAt, localNodeId, true);
    }

    /** Constructor para clientes remotos (sin información de BD, solo en memoria). */
    public static FederatedClientInfo remote(String username, String remoteNodeId) {
        return new FederatedClientInfo(username, "N/A", "N/A", remoteNodeId, false);
    }

    public String getUsername()    { return username; }
    public String getIp()          { return ip; }
    public String getConnectedAt() { return connectedAt; }
    public String getServerNodeId(){ return serverNodeId; }
    public boolean isLocal()       { return isLocal; }

    @Override
    public String toString() {
        return "FederatedClientInfo[" + username + "@" + serverNodeId
                + (isLocal ? "(local)" : "(remoto)") + "]";
    }
}

package JsonSchema;

/**
 * DTO inmutable para registros de usuarios almacenados en la base de datos.
 * Usado por la API administrativa para listar todos los usuarios registrados.
 */
public final class UserRecord {

    private final long id;
    private final String username;
    private final String ip;
    private final String createdAt;

    public UserRecord(long id, String username, String ip, String createdAt) {
        this.id = id;
        this.username = username;
        this.ip = ip;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}

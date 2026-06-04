package MySqlRepository;

import JsonSchema.ActiveClient;
import JsonSchema.UserRecord;
import ports.spi.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL del repositorio de usuarios.
 * Extraída de MySqlDao para cumplir SRP — cada DAO implementa una sola interfaz.
 *
 * Principios aplicados:
 *   - SRP: solo operaciones de usuarios.
 *   - DIP: DatabaseConnectionManager inyectado por constructor.
 *   - ISP: implementa únicamente IUserRepository.
 */
public class MySqlUserDao implements IUserRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlUserDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlUserDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public long obtenerORegistrarUsuario(String username, String ipAddress) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, ip_address) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, username);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, ipAddress);
                insertStmt.executeUpdate();

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Fallo al obtener el ID del usuario insertado.");
                    }
                }
            }
        }
    }

    @Override
    public String obtenerNombreUsuario(long userId) throws Exception {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return "UsuarioDesconocido";
    }

    @Override
    public long obtenerIdUsuarioPorUsername(String username) throws Exception {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new Exception("El usuario " + username + " no existe en la base de datos.");
                }
            }
        }
    }

    @Override
    public List<ActiveClient> listarClientesActivos() throws Exception {
        List<ActiveClient> activos = new ArrayList<>();
        String sql = "SELECT u.username, c.ip_address, c.connected_at " +
                "FROM users u " +
                "JOIN client_connections c ON u.id = c.user_id " +
                "WHERE c.is_active = TRUE";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                activos.add(new ActiveClient(
                        rs.getString("username"),
                        rs.getString("ip_address"),
                        rs.getString("connected_at")
                ));
            }
        }

        return activos;
    }

    @Override
    public List<UserRecord> listarUsuariosRegistrados() throws SQLException {
        List<UserRecord> usuarios = new ArrayList<>();
        String sql = "SELECT id, username, ip_address, created_at FROM users ORDER BY id ASC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("ip_address"),
                        rs.getString("created_at")
                ));
            }
        }
        return usuarios;
    }
}

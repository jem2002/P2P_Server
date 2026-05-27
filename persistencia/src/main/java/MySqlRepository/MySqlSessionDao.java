package MySqlRepository;

import ports.spi.ISessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Implementación MySQL del repositorio de sesiones de conexión.
 * Extraída de MySqlDao para cumplir SRP — cada DAO implementa una sola interfaz.
 *
 * Principios aplicados:
 *   - SRP: solo operaciones de sesiones de conexión.
 *   - DIP: DatabaseConnectionManager inyectado por constructor.
 *   - ISP: implementa únicamente ISessionRepository.
 */
public class MySqlSessionDao implements ISessionRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlSessionDao.class);
    private final DatabaseConnectionManager dbManager;

    public MySqlSessionDao(DatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public long registrarSesionActiva(long userId, String ipAddress, int port, String protocol) throws SQLException {
        String sql = "INSERT INTO client_connections (user_id, ip_address, port, protocol, is_active, connected_at) VALUES (?, ?, ?, ?, TRUE, NOW())";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, userId);
            stmt.setString(2, ipAddress);
            stmt.setInt(3, port);
            stmt.setString(4, protocol);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    @Override
    public void cerrarSesionActiva(long sessionId) throws SQLException {
        String sql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sessionId);
            stmt.executeUpdate();
        }
    }

    @Override
    public long cerrarSesionPorIpYPuerto(String ipAddress, int port) throws Exception {
        String selectSql = "SELECT user_id FROM client_connections WHERE ip_address = ? AND port = ? AND is_active = TRUE";
        String updateSql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE ip_address = ? AND port = ? AND is_active = TRUE";

        long userId = 0;
        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, ipAddress);
                selectStmt.setInt(2, port);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getLong("user_id");
                    }
                }
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, ipAddress);
                updateStmt.setInt(2, port);
                updateStmt.executeUpdate();
            }
        }
        return userId;
    }

    @Override
    public void cerrarSesionPorUsername(String username) throws Exception {
        String sql = "UPDATE client_connections c JOIN users u ON c.user_id = u.id " +
                     "SET c.is_active = FALSE, c.disconnected_at = NOW() " +
                     "WHERE u.username = ? AND c.is_active = TRUE";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public void limpiarConexionesMuertas() {
        String sql = "UPDATE client_connections SET is_active = FALSE, disconnected_at = NOW() WHERE is_active = TRUE";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            int limpiados = stmt.executeUpdate();
            if (limpiados > 0) {
                logger.info("Limpieza de inicio: {} conexiones 'muertas' fueron cerradas.", limpiados);
            }
        } catch (SQLException e) {
            logger.error("Error limpiando conexiones muertas", e);
        }
    }
}

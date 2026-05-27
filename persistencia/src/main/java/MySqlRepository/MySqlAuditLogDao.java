package MySqlRepository;

import JsonSchema.LogEntry;
import ports.spi.IAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL del repositorio de auditoría (logs).
 * Extraída de MySqlDao para cumplir SRP — cada DAO implementa una sola interfaz.
 *
 * Principios aplicados:
 *   - SRP: solo operaciones de logs de auditoría.
 *   - DIP: DatabaseConnectionManager inyectado por constructor.
 *   - ISP: implementa únicamente IAuditLogRepository.
 */
public class MySqlAuditLogDao implements IAuditLogRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlAuditLogDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlAuditLogDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public void registrarLog(Long documentId, long senderId, Long receiverId, String action, String protocol,
            String status, String details) {
        String sql = "INSERT INTO logs (document_id, sender_user_id, receiver_user_id, action, protocol, status, details, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (documentId != null)
                stmt.setLong(1, documentId);
            else
                stmt.setNull(1, Types.BIGINT);
            stmt.setLong(2, senderId);
            if (receiverId != null)
                stmt.setLong(3, receiverId);
            else
                stmt.setNull(3, Types.BIGINT);
            stmt.setString(4, action);
            stmt.setString(5, protocol);
            stmt.setString(6, status);
            stmt.setString(7, details);

            // Tiempo de Colombia (UTC-5)
            java.time.ZonedDateTime colombiaTime = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Bogota"));
            stmt.setTimestamp(8, java.sql.Timestamp.valueOf(colombiaTime.toLocalDateTime()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al registrar log en auditoría: {} - Detalles: {}", action, details, e);
        }
    }

    @Override
    public List<LogEntry> listarLogs() throws Exception {
        List<LogEntry> logs = new ArrayList<>();
        String sql = "SELECT l.id, l.document_id, u1.username as sender, " +
                "l.action, l.protocol, l.status, l.details, l.timestamp " +
                "FROM logs l " +
                "LEFT JOIN users u1 ON l.sender_user_id = u1.id " +
                "ORDER BY l.id DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(new LogEntry(
                        rs.getLong("id"),
                        rs.getString("document_id") != null ? rs.getString("document_id") : "",
                        rs.getString("sender") != null ? rs.getString("sender") : "",
                        rs.getString("action"),
                        rs.getString("protocol"),
                        rs.getString("status"),
                        rs.getString("details"),
                        rs.getString("timestamp")
                ));
            }
        }
        return logs;
    }
}

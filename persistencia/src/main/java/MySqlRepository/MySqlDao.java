package MySqlRepository;

import JsonSchema.ActiveClient;
import JsonSchema.DocumentInfo;
import JsonSchema.DownloadDetails;
import JsonSchema.LogEntry;
import JsonSchema.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación unificada del acceso a datos en MySQL.
 * Implementa las 4 interfaces segregadas (ISP) para que cada servicio
 * dependa solo del contrato que necesita.
 *
 * Principios aplicados:
 *   - ISP: 4 interfaces segregadas por dominio.
 *   - SRP: aunque la clase es grande, cada interfaz puede extraerse
 *          a su propia implementación cuando sea necesario.
 */
public class MySqlDao implements IUserRepository, IDocumentRepository, ISessionRepository, IAuditLogRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlDao.class);
    private final DatabaseConnectionManager dbManager;

    public MySqlDao() {
        this.dbManager = DatabaseConnectionManager.getInstance();
    }

    // ======================== IUserRepository ========================

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

    // ======================== IDocumentRepository ========================

    @Override
    public long registrarDocumento(String name, long sizeBytes, String extension, String mimeType,
            String docType, String originalPath, long ownerUserId, String ownerIp) throws Exception {
        String sql = "INSERT INTO documents (name, size_bytes, extension, mime_type, doc_type, original_path, owner_user_id, owner_ip) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setLong(2, sizeBytes);
            stmt.setString(3, extension);
            stmt.setString(4, mimeType);
            stmt.setString(5, docType);
            stmt.setString(6, originalPath);
            stmt.setLong(7, ownerUserId);
            stmt.setString(8, ownerIp);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new Exception("No se pudo obtener el ID del documento generado.");
                }
            }
        }
    }

    @Override
    public void registrarHashDocumento(long documentId, String algorithm, String hashValue) throws Exception {
        String sql = "INSERT INTO document_hashes (document_id, algorithm, hash_value) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            stmt.setString(2, algorithm);
            stmt.setString(3, hashValue);
            stmt.executeUpdate();
        }
    }

    @Override
    public void registrarCifradoDocumento(long documentId, String algorithm, String encryptedPath, String keyReference)
            throws Exception {
        String sql = "INSERT INTO encrypted_documents (document_id, algorithm, encrypted_path, key_reference) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            stmt.setString(2, algorithm);
            stmt.setString(3, encryptedPath);
            stmt.setString(4, keyReference);
            stmt.executeUpdate();
        }
    }

    @Override
    public DownloadDetails obtenerDetallesDescarga(long documentId) throws Exception {
        String sql = "SELECT d.name, d.size_bytes, e.encrypted_path " +
                "FROM documents d " +
                "JOIN encrypted_documents e ON d.id = e.document_id " +
                "WHERE d.id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DownloadDetails(
                            rs.getString("name"),
                            rs.getLong("size_bytes"),
                            rs.getString("encrypted_path")
                    );
                } else {
                    throw new Exception("Documento no encontrado o no tiene archivo físico.");
                }
            }
        }
    }

    @Override
    public String obtenerRutaOriginal(long documentId) throws Exception {
        String sql = "SELECT original_path FROM documents WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("original_path");
                else
                    throw new Exception("Documento no encontrado.");
            }
        }
    }

    @Override
    public String obtenerHashValue(long documentId) throws Exception {
        String sql = "SELECT hash_value FROM document_hashes WHERE document_id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, documentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("hash_value");
                else
                    throw new Exception("Hash no encontrado.");
            }
        }
    }

    @Override
    public List<DocumentInfo> listarArchivosDisponibles() throws Exception {
        return listarDocumentosFiltrados("FILE");
    }

    @Override
    public List<DocumentInfo> listarMensajesDisponibles() throws Exception {
        return listarMensajesDisponibles(null);
    }

    @Override
    public List<DocumentInfo> listarMensajesDisponibles(String requestingUsername) throws Exception {
        List<DocumentInfo> documentos = new ArrayList<>();

        String sql;
        boolean filtered = requestingUsername != null && !requestingUsername.isBlank();

        if (filtered) {
            // Muestra:
            //  1. Todos los broadcasts (doc_type = 'MESSAGE')
            //  2. Mensajes privados donde este usuario ES el destinatario
            //  3. Mensajes privados que este usuario envió (es el owner)
            sql = "SELECT d.id, d.name, d.size_bytes, d.extension, d.original_path, u.username, u.ip_address " +
                  "FROM documents d " +
                  "JOIN users u ON d.owner_user_id = u.id " +
                  "WHERE d.doc_type = 'MESSAGE' " +
                  "   OR d.doc_type = CONCAT('PRIVATE_TO:', ?) " +
                  "   OR (d.doc_type LIKE 'PRIVATE_TO:%' AND u.username = ?) " +
                  "ORDER BY d.id DESC";
        } else {
            sql = "SELECT d.id, d.name, d.size_bytes, d.extension, d.original_path, u.username, u.ip_address " +
                  "FROM documents d " +
                  "JOIN users u ON d.owner_user_id = u.id " +
                  "WHERE d.doc_type = 'MESSAGE' OR d.doc_type LIKE 'PRIVATE_TO:%' " +
                  "ORDER BY d.id DESC";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filtered) {
                stmt.setString(1, requestingUsername);
                stmt.setString(2, requestingUsername);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propietario = rs.getString("username") + " (" + rs.getString("ip_address") + ")";
                    documentos.add(new DocumentInfo(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("size_bytes"),
                            rs.getString("extension"),
                            rs.getString("original_path"),
                            propietario
                    ));
                }
            }
        }
        return documentos;
    }

    private List<DocumentInfo> listarDocumentosFiltrados(String type) throws Exception {
        List<DocumentInfo> documentos = new ArrayList<>();
        String sql = "SELECT d.id, d.name, d.size_bytes, d.extension, d.original_path, u.username, u.ip_address " +
                "FROM documents d " +
                "JOIN users u ON d.owner_user_id = u.id " +
                "WHERE d.doc_type = ? " +
                "ORDER BY d.id DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String propietario = rs.getString("username") + " (" + rs.getString("ip_address") + ")";
                    documentos.add(new DocumentInfo(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getLong("size_bytes"),
                            rs.getString("extension"),
                            rs.getString("original_path"),
                            propietario
                    ));
                }
            }
        }
        return documentos;
    }

    @Override
    public List<DocumentInfo> listarDocumentosDisponibles() throws Exception {
        List<DocumentInfo> documentos = new ArrayList<>();
        String sql = "SELECT d.id, d.name, d.size_bytes, d.extension, u.username, u.ip_address " +
                "FROM documents d " +
                "JOIN users u ON d.owner_user_id = u.id " +
                "ORDER BY d.id DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String propietario = rs.getString("username") + " (" + rs.getString("ip_address") + ")";
                documentos.add(new DocumentInfo(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getLong("size_bytes"),
                        rs.getString("extension"),
                        null,
                        propietario
                ));
            }
        }
        return documentos;
    }

    // ======================== ISessionRepository ========================

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

    // ======================== IAuditLogRepository ========================

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
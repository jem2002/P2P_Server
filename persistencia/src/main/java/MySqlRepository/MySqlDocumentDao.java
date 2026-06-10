package MySqlRepository;

import com.universidad.messaging.server.shared.api.dto.DocumentDTO;
import com.universidad.messaging.server.shared.api.dto.MessageDTO;
import com.universidad.messaging.server.shared.schema.documentSchema.DocumentInfo;
import com.universidad.messaging.server.shared.schema.documentSchema.DownloadDetails;
import MySqlRepository.db.IDatabaseConnectionManager;
import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL del repositorio de documentos.
 * Extraída de MySqlDao para cumplir SRP — cada DAO implementa una sola interfaz.
 *
 * Principios aplicados:
 *   - SRP: solo operaciones de documentos.
 *   - DIP: DatabaseConnectionManager inyectado por constructor.
 *   - ISP: implementa únicamente IDocumentRepository.
 */
public class MySqlDocumentDao implements IDocumentRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlDocumentDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlDocumentDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public long registrarDocumento(String name, long sizeBytes, String extension, String mimeType,
                                   String docType, String originalPath, long ownerUserId, String ownerIp) throws Exception {

        // 1. Definir las columnas y asegurar que coincida el número de '?' (9 en total)
        String sql = "INSERT INTO documents (name, size_bytes, extension, mime_type, doc_type, original_path, owner_user_id, owner_ip, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // 2. Obtener la fecha y hora actual específicamente para la zona horaria de Colombia
            ZoneId zonaColombia = ZoneId.of("America/Bogota");
            ZonedDateTime horaColombia = ZonedDateTime.now(zonaColombia);
            // Convertir a Timestamp para que el driver de la base de datos lo entienda correctamente
            Timestamp createdAt = Timestamp.from(horaColombia.toInstant());

            // 3. Setear los parámetros en el PreparedStatement
            stmt.setString(1, name);
            stmt.setLong(2, sizeBytes);
            stmt.setString(3, extension);
            stmt.setString(4, mimeType);
            stmt.setString(5, docType);
            stmt.setString(6, originalPath);
            stmt.setLong(7, ownerUserId);
            stmt.setString(8, ownerIp);
            stmt.setTimestamp(9, createdAt); // <-- Noveno parámetro añadido

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
                  "ORDER BY d.created_at ASC";
        } else {
            sql = "SELECT d.id, d.name, d.size_bytes, d.extension, d.original_path, u.username, u.ip_address " +
                  "FROM documents d " +
                  "JOIN users u ON d.owner_user_id = u.id " +
                  "WHERE d.doc_type = 'MESSAGE' OR d.doc_type LIKE 'PRIVATE_TO:%' " +
                  "ORDER BY d.created_at ASC";
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
        return listarDocumentosDisponibles(null);
    }

    @Override
    public List<DocumentInfo> listarDocumentosDisponibles(String requestingUsername) throws Exception {
        List<DocumentInfo> documentos = new ArrayList<>();
        String sql;
        boolean filterByUser = (requestingUsername != null && !requestingUsername.trim().isEmpty());

        if (filterByUser) {
            sql = "SELECT d.id, d.name, d.size_bytes, d.extension, u.username, u.ip_address " +
                  "FROM documents d " +
                  "JOIN users u ON d.owner_user_id = u.id " +
                  "WHERE d.doc_type = 'FILE' " +
                  "   OR d.doc_type = ? " +
                  "   OR (d.doc_type LIKE 'PRIVATE_FILE_TO:%' AND d.owner_user_id = (SELECT id FROM users WHERE username = ?)) " +
                  "ORDER BY d.id DESC";
        } else {
            sql = "SELECT d.id, d.name, d.size_bytes, d.extension, u.username, u.ip_address " +
                  "FROM documents d " +
                  "JOIN users u ON d.owner_user_id = u.id " +
                  "WHERE d.doc_type = 'FILE' " +
                  "ORDER BY d.id DESC";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            if (filterByUser) {
                stmt.setString(1, "PRIVATE_FILE_TO:" + requestingUsername);
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
                            null,
                            propietario
                    ));
                }
            }
        }
        return documentos;
    }


    @Override
    public List<String> obtenerTodasRutasArchivosOriginales() throws Exception {
        List<String> rutasOriginales = new ArrayList<>();
        String sql = "SELECT original_path FROM documents WHERE original_path IS NOT NULL";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rutasOriginales.add(rs.getString("original_path"));
            }
        }
        return rutasOriginales;
    }

    @Override
    public List<String> obtenerTodasRutasArchivosEncriptados() throws Exception {
        List<String> rutasEncriptadas = new ArrayList<>();
        String sql = "SELECT encrypted_path FROM encrypted_documents WHERE encrypted_path IS NOT NULL";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                rutasEncriptadas.add(rs.getString("encrypted_path"));
            }
        }
        return rutasEncriptadas;
    }

    @Override
    public List<MessageDTO> buscarMensajes(
            String owner,
            String target,
            String type,
            String keyword,
            String fromDate,
            String toDate,
            String sortBy,
            String sortDir
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT d.name,
                   d.original_path,
                   d.doc_type,
                   d.created_at,
                   u.username AS owner
            FROM documents d
            INNER JOIN users u ON u.id = d.owner_user_id
            WHERE (d.doc_type = 'MESSAGE' OR d.doc_type LIKE 'PRIVATE\\_TO:%')
            """);

        List<Object> params = new ArrayList<>();

        if (owner != null && !owner.isBlank()) {
            sql.append(" AND LOWER(u.username) = LOWER(?) ");
            params.add(owner.trim());
        }

        if (target != null && !target.isBlank()) {
            sql.append(" AND d.doc_type = ? ");
            params.add("PRIVATE_TO:" + target.trim());
        }

        if (type != null && !type.isBlank()) {
            switch (type.toLowerCase()) {
                case "public"  -> sql.append(" AND d.doc_type = 'MESSAGE' ");
                case "private" -> sql.append(" AND d.doc_type LIKE 'PRIVATE\\_TO:%' ");
                default        -> throw new IllegalArgumentException(
                        "type inválido: usa 'public' o 'private'");
            }
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND d.created_at >= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(fromDate).atStartOfDay()));
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND d.created_at <= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(toDate).atTime(23, 59, 59)));
        }

        String column = switch (sortBy != null ? sortBy : "created_at") {
            case "owner",     "u.username"  -> "u.username";
            case "createdAt", "created_at"  -> "d.created_at";
            case "name"                     -> "d.name";
            default                         -> "d.created_at";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(column).append(" ").append(direction);

        List<MessageDTO> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    String docType = rs.getString("doc_type");
                    String path    = rs.getString("original_path");
                    String content = leerContenidoMensaje(path);

                    if (keyword != null && !keyword.isBlank()) {
                        if (!content.toLowerCase().contains(keyword.toLowerCase())) {
                            continue;
                        }
                    }

                    String targetOut = docType.startsWith("PRIVATE_TO:")
                            ? docType.substring("PRIVATE_TO:".length())
                            : "Todos";

                    result.add(new MessageDTO(
                            content,
                            rs.getString("owner"),
                            targetOut,
                            rs.getString("created_at")
                    ));
                }
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /** Lee el contenido del mensaje desde el filesystem */
    private String leerContenidoMensaje(String pathStr) {
        try {
            return Files.readString(Paths.get(pathStr), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "ERROR AL LEER CONTENIDO";
        }
    }

    /** Bind genérico para los tipos usados en los filtros */
    private void setParam(PreparedStatement stmt, int index, Object value)
            throws SQLException {
        switch (value) {
            case String s    -> stmt.setString(index, s);
            case Integer n   -> stmt.setInt(index, n);
            case Timestamp t -> stmt.setTimestamp(index, t);
            default          -> stmt.setObject(index, value);
        }
    }


    public List<DocumentDTO> buscarDocumentos(
            String owner,
            String extension,
            String keyword,
            String fromDate,
            String toDate,
            String sortBy,
            String sortDir
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT d.name,
                   d.extension,
                   d.size_bytes,
                   d.created_at,
                   u.username AS owner
            FROM documents d
            INNER JOIN users u ON u.id = d.owner_user_id
            WHERE d.doc_type NOT LIKE 'PRIVATE\\_TO:%'
              AND d.doc_type != 'MESSAGE'
            """);

        List<Object> params = new ArrayList<>();

        if (owner != null && !owner.isBlank()) {
            sql.append(" AND LOWER(u.username) = LOWER(?) ");
            params.add(owner.trim());
        }

        if (extension != null && !extension.isBlank()) {
            sql.append(" AND LOWER(d.extension) = LOWER(?) ");
            params.add(extension.trim());
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND LOWER(d.name) LIKE LOWER(?) ");
            params.add("%" + keyword.trim() + "%");
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND d.created_at >= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(fromDate).atStartOfDay()));
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND d.created_at <= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(toDate).atTime(23, 59, 59)));
        }

        String column = switch (sortBy != null ? sortBy : "created_at") {
            case "owner",     "u.username"  -> "u.username";
            case "createdAt", "created_at"  -> "d.created_at";
            case "name"                     -> "d.name";
            case "size",      "size_bytes"  -> "d.size_bytes";
            case "extension"                -> "d.extension";
            default                         -> "d.created_at";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(column).append(" ").append(direction);

        List<DocumentDTO> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new DocumentDTO(
                            rs.getString("name"),
                            rs.getString("extension"),
                            rs.getLong("size_bytes"),
                            rs.getString("owner"),
                            rs.getString("created_at")
                    ));
                }
            }
        }

        return result;
    }


    @Override
    public int contarMensajesRegistrados() throws SQLException {
        String sql = "SELECT COUNT(*) FROM documents WHERE doc_type = 'MESSAGE' OR doc_type LIKE 'PRIVATE_TO:%'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public int contarDocumentosRegistrados() throws SQLException {
        // Excluye los mensajes utilizando la misma lógica invertida de tus filtros de búsqueda
        String sql = "SELECT COUNT(*) FROM documents WHERE doc_type NOT LIKE 'PRIVATE_TO:%' AND doc_type != 'MESSAGE'";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }



}

package MySqlRepository;

import JsonSchema.DocumentInfo;
import JsonSchema.DownloadDetails;
import ports.spi.IDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
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
}

package MySqlRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.universidad.messaging.server.shared.api.dto.CommentDTO;
import com.universidad.messaging.server.shared.schema.commentSchema.Comment;
import com.universidad.messaging.server.shared.schema.commentSchema.CommentInfo;
import MySqlRepository.db.IDatabaseConnectionManager;
import com.universidad.messaging.server.persistencia.api.ICommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySqlCommentDao implements ICommentRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlCommentDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlCommentDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Comment registrarComentario(Comment comment) {
        // 1. Añadimos 'created_at' y el '?' correspondiente
        String sql = "INSERT INTO comments (document_id, user_id, content, sentiment, confidence, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, comment.getDocumentId());
            pstmt.setLong(2, comment.getUserId());
            pstmt.setString(3, comment.getContent());
            pstmt.setString(4, comment.getSentiment().name());
            pstmt.setBigDecimal(5, comment.getConfidence());
            // 2. Pasamos el LocalDateTime que ya viene en el objeto
            pstmt.setObject(6, comment.getCreatedAt());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        comment.setId(generatedKeys.getLong(1));
                    }
                }
            } else {
                logger.warn("El insert no afectó ninguna fila. Comentario no registrado.");
            }

        } catch (SQLException e) {
            logger.error("Error al registrar comentario para el documento {}: ", comment.getDocumentId(), e);
            throw new RuntimeException("Error en base de datos al guardar el comentario", e);
        }

        return comment;
    }

    @Override
    public Comment replicarComentario(Comment comment) {
        // 1. Añadimos 'created_at' y el '?' correspondiente
        String sql = "INSERT INTO comments (id, document_id, user_id, content, sentiment, confidence, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (comment.getId() == null) {
                throw new IllegalArgumentException("El ID del comentario no puede ser nulo para una réplica.");
            }

            pstmt.setLong(1, comment.getId());
            pstmt.setLong(2, comment.getDocumentId());
            pstmt.setLong(3, comment.getUserId());
            pstmt.setString(4, comment.getContent());
            pstmt.setString(5, comment.getSentiment().name());
            pstmt.setBigDecimal(6, comment.getConfidence());
            // 2. Pasamos el LocalDateTime que ya viene en el objeto
            pstmt.setObject(7, comment.getCreatedAt());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                logger.warn("No se pudo replicar el comentario con ID {}. Ninguna fila afectada.", comment.getId());
            }

        } catch (SQLException e) {
            logger.error("Error al replicar el comentario con ID {} para el documento {}: ", comment.getId(), comment.getDocumentId(), e);
            throw new RuntimeException("Error en base de datos al replicar el comentario", e);
        }

        return comment;
    }
    @Override
    public List<CommentInfo> listarComentariosPorDocumento(Long documentId) {
        List<CommentInfo> comentarios = new ArrayList<>();
        String sql = "SELECT c.id, c.document_id, c.user_id, u.username, c.content, c.sentiment, c.confidence, c.created_at " +
                "FROM comments c " +
                "INNER JOIN users u ON c.user_id = u.id " +
                "WHERE c.document_id = ? " +
                "ORDER BY c.created_at ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, documentId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CommentInfo info = new CommentInfo();
                    info.setId(rs.getLong("id"));
                    info.setDocumentId(rs.getLong("document_id"));
                    info.setUserId(rs.getLong("user_id"));
                    info.setUsername(rs.getString("username"));
                    info.setContent(rs.getString("content"));

                    // Convertimos el String de la BD al Enum definido en Comment
                    info.setSentiment(Comment.Sentiment.valueOf(rs.getString("sentiment")));

                    // Recuperamos el DECIMAL exacto usando BigDecimal
                    info.setConfidence(rs.getBigDecimal("confidence"));

                    // Convertimos el Timestamp de SQL a LocalDateTime de Java
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        info.setCreatedAt(ts.toLocalDateTime());
                    }

                    comentarios.add(info);
                }
            }

        } catch (SQLException e) {
            logger.error("Error al listar comentarios para el documento con ID {}: ", documentId, e);
            throw new RuntimeException("Error en base de datos al listar los comentarios", e);
        }

        return comentarios;
    }


    public List<CommentDTO> buscarComentarios(
            String username,
            String documentName,
            String sentiment,
            String fromDate,
            String toDate,
            String sortBy,
            String sortDir
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT d.name      AS document_name,
                   u.username,
                   c.content,
                   c.sentiment,
                   c.confidence,
                   c.created_at
            FROM comments c
            INNER JOIN users     u ON u.id = c.user_id
            INNER JOIN documents d ON d.id = c.document_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(u.username) = LOWER(?) ");
            params.add(username.trim());
        }

        if (documentName != null && !documentName.isBlank()) {
            sql.append(" AND LOWER(d.name) LIKE LOWER(?) ");
            params.add("%" + documentName.trim() + "%");
        }

        if (sentiment != null && !sentiment.isBlank()) {
            switch (sentiment.toUpperCase()) {
                case "POSITIVO", "NEGATIVO", "NO_CALIFICABLE"
                        -> sql.append(" AND c.sentiment = ? ");
                default -> throw new IllegalArgumentException(
                        "sentiment inválido: usa 'POSITIVO', 'NEGATIVO' o 'NO_CALIFICABLE'");
            }
            params.add(sentiment.toUpperCase().trim());
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND c.created_at >= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(fromDate).atStartOfDay()));
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND c.created_at <= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(toDate).atTime(23, 59, 59)));
        }

        String column = switch (sortBy != null ? sortBy : "created_at") {
            case "username"                 -> "u.username";
            case "documentName",
                 "document_name"           -> "d.name";
            case "sentiment"               -> "c.sentiment";
            case "confidence"              -> "c.confidence";
            case "createdAt", "created_at" -> "c.created_at";
            default                        -> "c.created_at";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(column).append(" ").append(direction);

        List<CommentDTO> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new CommentDTO(
                            rs.getString("document_name"),
                            rs.getString("username"),
                            rs.getString("content"),
                            rs.getString("sentiment"),
                            rs.getString("confidence"),
                            rs.getString("created_at")
                    ));
                }
            }
        }

        return result;
    }

    private void setParam(PreparedStatement stmt, int index, Object value)
            throws SQLException {
        switch (value) {
            case String s    -> stmt.setString(index, s);
            case Integer n   -> stmt.setInt(index, n);
            case Timestamp t -> stmt.setTimestamp(index, t);
            default          -> stmt.setObject(index, value);
        }
    }

}

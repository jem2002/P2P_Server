package MySqlRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import JsonSchema.Comment;
import JsonSchema.CommentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ports.spi.ICommentRepository;

public class MySqlCommentDao implements ICommentRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlCommentDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlCommentDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Comment registrarComentario(Comment comment) {
        String sql = "INSERT INTO comments (document_id, user_id, content, sentiment, confidence) VALUES (?, ?, ?, ?, ?)";

        // El try-with-resources asegura que el Connection y el PreparedStatement se cierren solos
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, comment.getDocumentId());
            pstmt.setLong(2, comment.getUserId());
            pstmt.setString(3, comment.getContent());
            pstmt.setString(4, comment.getSentiment().name()); // Guarda el Enum como String ('POSITIVO' o 'NEGATIVO')
            pstmt.setBigDecimal(5, comment.getConfidence());

            int affectedRows = pstmt.executeUpdate();

            // Si se insertó correctamente, recuperamos el ID autogenerado
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
}

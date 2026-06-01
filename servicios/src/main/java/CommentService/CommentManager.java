package CommentService;

import JsonSchema.Comment;
import JsonSchema.ApiResponse;
import APIService.SentimentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ports.spi.ICommentRepository;

import java.math.BigDecimal;
import java.util.List;

public class CommentManager {
    private static final Logger logger = LoggerFactory.getLogger(CommentManager.class);

    private final ICommentRepository commentRepository;
    private final SentimentService sentimentService;

    // Se inyecta tanto el repositorio como el servicio de sentimiento
    public CommentManager(ICommentRepository commentRepository, SentimentService sentimentService){
        this.commentRepository = commentRepository;
        this.sentimentService = sentimentService;
    }

    /**
     * Valida, analiza el sentimiento y registra un nuevo comentario.
     */
    public Comment registrarComentario(Long documentId, Long userId, String content) {
        logger.info("Iniciando flujo de negocio para registrar comentario del documento {} por el usuario {}", documentId, userId);

        // 1. Validaciones iniciales de los datos de entrada
        if (documentId == null || userId == null || content == null || content.trim().isEmpty()) {
            logger.error("Error de validación: Faltan campos obligatorios para procesar el comentario.");
            throw new IllegalArgumentException("Todos los campos (documentId, userId, content) son obligatorios.");
        }

        // 2. Consumir el SentimentService para analizar el texto
        ApiResponse sentimentAnalysis;
        try {
            sentimentAnalysis = sentimentService.process(content);
        } catch (Exception e) {
            logger.error("Error crítico al comunicarse con SentimentService para el contenido: {}", content, e);
            throw new RuntimeException("El servicio de análisis de sentimiento no está disponible en este momento.", e);
        }

        if (sentimentAnalysis == null) {
            logger.error("El SentimentService devolvió una respuesta nula para el comentario.");
            throw new RuntimeException("No se pudo analizar el sentimiento del comentario.");
        }

        // 3. Mapear y validar los resultados del análisis
        Comment.Sentiment sentimentEnum;
        try {
            sentimentEnum = Comment.Sentiment.valueOf(sentimentAnalysis.getSentiment().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Sentimiento desconocido recibido del servicio: {}", sentimentAnalysis.getSentiment());
            throw new RuntimeException("El análisis devolvió un tipo de sentimiento inválido.");
        }

        BigDecimal confidence = BigDecimal.valueOf(sentimentAnalysis.getConfidence());

        // Validar rango de confianza (regla de negocio alineada con la BD)
        if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(new BigDecimal("100.0000")) > 0) {
            logger.error("La confianza devuelta ({}) está fuera del rango permitido (0-100).", confidence);
            throw new IllegalArgumentException("La confianza del análisis debe estar entre 0 y 100.");
        }

        // 4. Construcción de la entidad Comment
        Comment nuevoComentario = new Comment();
        nuevoComentario.setDocumentId(documentId);
        nuevoComentario.setUserId(userId);
        nuevoComentario.setContent(content);
        nuevoComentario.setSentiment(sentimentEnum);
        nuevoComentario.setConfidence(confidence);

        // 5. Delegar la persistencia al repositorio
        try {
            Comment comentarioRegistrado = commentRepository.registrarComentario(nuevoComentario);
            logger.info("Comentario guardado exitosamente en BD con ID: {}", comentarioRegistrado.getId());
            return comentarioRegistrado;
        } catch (Exception e) {
            logger.error("Excepción al intentar persistir el comentario en la base de datos.", e);
            throw new RuntimeException("No se pudo completar el almacenamiento del comentario.", e);
        }
    }

    /**
     * Obtiene la lista de comentarios para un documento validando su ID.
     */
    public List<Comment> listarComentariosPorDocumento(Long documentId) {
        logger.info("Solicitando lista de comentarios para el documento con ID: {}", documentId);

        if (documentId == null) {
            logger.error("Error de validación: ID de documento nulo.");
            throw new IllegalArgumentException("El ID del documento es obligatorio para consultar su historial de comentarios.");
        }

        try {
            return commentRepository.listarComentariosPorDocumento(documentId);
        } catch (Exception e) {
            logger.error("Excepción al recuperar los comentarios del documento {}.", documentId, e);
            throw new RuntimeException("No se pudieron obtener los comentarios del sistema", e);
        }
    }
}
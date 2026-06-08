package CommentService;

import JsonSchema.Comment;
import JsonSchema.ApiResponse;
import APIService.SentimentService;
import JsonSchema.CommentInfo;
import UserService.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ports.spi.ICommentRepository;

import java.math.BigDecimal;
import java.util.List;

public class CommentManager {
    private static final Logger logger = LoggerFactory.getLogger(CommentManager.class);

    private final ICommentRepository commentRepository;
    private final SentimentService sentimentService;
    private final UserManager userManager;

    // Se inyecta tanto el repositorio como el servicio de sentimiento
    public CommentManager(ICommentRepository commentRepository, SentimentService sentimentService, UserManager userManager){
        this.commentRepository = commentRepository;
        this.sentimentService = sentimentService;
        this.userManager = userManager;
    }

    /**
     * Valida, analiza el sentimiento y registra un nuevo comentario.
     */
    public Comment registrarComentario(Long documentId, String username, String content) {
        logger.info("Iniciando flujo de negocio para registrar comentario del documento {} por el usuario {}", documentId, username);

        // 1. Validaciones iniciales de los datos de entrada
        if (documentId == null || username == null || content == null || content.trim().isEmpty()) {
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


        try{
        long userId = userManager.obtenerIdUsuario(username);

        // 4. Construcción de la entidad Comment
        Comment nuevoComentario = new Comment();
        nuevoComentario.setDocumentId(documentId);
        nuevoComentario.setUserId(userId);
        nuevoComentario.setContent(content);
        nuevoComentario.setSentiment(sentimentEnum);
        nuevoComentario.setConfidence(confidence);

        // 5. Delegar la persistencia al repositorio

            Comment comentarioRegistrado = commentRepository.registrarComentario(nuevoComentario);
            logger.info("Comentario guardado exitosamente en BD con ID: {}", comentarioRegistrado.getId());
            return comentarioRegistrado;
        } catch (Exception e) {
            logger.error("Excepción al intentar persistir el comentario en la base de datos.", e);
            throw new RuntimeException("No se pudo completar el almacenamiento del comentario.", e);
        }
    }


    public Comment replicarComentario(Long id, Long documentId, String username, String content, String sentimentStr, BigDecimal confidence) {
        logger.info("Procesando evento de réplica para el comentario ID: {}", id);

        // 1. Validaciones estructurales de los datos del payload
        if (id == null || documentId == null || username == null || content == null || sentimentStr == null || confidence == null) {
            logger.error("Error de validación: El payload del evento contiene campos nulos.");
            throw new IllegalArgumentException("Todos los campos del payload son obligatorios para la réplica.");
        }

        // 2. Parsear el String del evento al Enum interno de forma segura
        Comment.Sentiment sentimentEnum;
        try {
            sentimentEnum = Comment.Sentiment.valueOf(sentimentStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            logger.error("Sentimiento inválido recibido en el evento: '{}'", sentimentStr);
            throw new RuntimeException("No se puede replicar: Tipo de sentimiento desconocido en el payload.");
        }

        try {
            // 3. Obtener el ID del usuario local basado en el username del evento
            long userId = userManager.obtenerIdUsuario(username);

            // 4. Construcción de la entidad con el ID histórico
            Comment comentarioReplica = new Comment();
            comentarioReplica.setId(id); // Forzamos el ID original
            comentarioReplica.setDocumentId(documentId);
            comentarioReplica.setUserId(userId);
            comentarioReplica.setContent(content);
            comentarioReplica.setSentiment(sentimentEnum);
            comentarioReplica.setConfidence(confidence);

            // 5. Persistir usando el método exclusivo de réplicas en el repositorio
            Comment guardado = commentRepository.replicarComentario(comentarioReplica);
            logger.debug("Comentario histórico {} guardado exitosamente.", guardado.getId());

            return guardado;

        } catch (Exception e) {
            logger.error("Error crítico al intentar persistir la réplica del comentario ID: {}", id, e);
            throw new RuntimeException("Error en el almacenamiento de la réplica en base de datos.", e);
        }
    }

    /**
     * Obtiene la lista de comentarios para un documento validando su ID.
     */
    public List<CommentInfo>  listarComentariosPorDocumento(Long documentId) {
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
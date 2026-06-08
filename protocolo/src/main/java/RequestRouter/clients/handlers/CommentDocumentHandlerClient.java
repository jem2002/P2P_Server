package RequestRouter.clients.handlers;

import CommentService.CommentManager;
import JsonSchema.Comment;
import JsonSerializer.ResponseBuilder;
import ports.api.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.ReplicationEvent;
import replication.ReplicationManager;

public class CommentDocumentHandlerClient implements ClientActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommentDocumentHandlerClient.class);

    private final CommentManager commentManager;
    private final ResponseBuilder serializer;
    private final ReplicationManager replicationManager;
    private final String localNodeId;

    // Ya no se inyecta el SentimentService aquí, se limpia el constructor
    public CommentDocumentHandlerClient(CommentManager commentManager, ResponseBuilder serializer, ReplicationManager replicationManager,  String localNodeId) {
        this.commentManager = commentManager;
        this.serializer = serializer;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) {
        logger.info("Handler recibió una solicitud para registrar un comentario.");

        // 1. Validar la estructura del payload JSON
        if (!payload.hasNonNull("documentId") || !payload.hasNonNull("username") || !payload.hasNonNull("content")) {
            logger.error("Error de formato: El payload no contiene los campos requeridos.");
            return serializer.buildErrorResponse("Faltan campos obligatorios en el JSON: documentId, userId, content");
        }

        try {
            // 2. Extraer datos del JSON
            Long documentId = payload.get("documentId").asLong();
            String username = payload.get("username").asText();
            String content = payload.get("content").asText();

            // 3. Delegar todo el flujo de negocio e integración al Manager
            Comment savedComment = commentManager.registrarComentario(documentId, username, content);

            // 4. Formatear la respuesta de éxito
            String mensajeExito = String.format("Comentario registrado correctamente con ID: %d, Sentimiento: %s, Confianza: %s",
                    savedComment.getId(), savedComment.getSentiment().name(), savedComment.getConfidence().toString());

            replicationManager.propagate(ReplicationEvent.newComment(localNodeId, savedComment.getId(), documentId, username, content, savedComment.getSentiment().name(), savedComment.getConfidence()));

            return serializer.buildSuccessResponse("REGISTER_COMMENT", mensajeExito);

        } catch (IllegalArgumentException e) {
            // Captura errores de validación controlados (campos vacíos, rangos incorrectos)
            logger.warn("Petición rechazada por reglas de validación: {}", e.getMessage());
            return serializer.buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            // Captura cualquier otro fallo general (caída del microservicio de sentimientos, error de SQL, etc.)
            logger.error("Fallo inesperado al procesar la acción del comentario.", e);
            return serializer.buildErrorResponse("Error interno del servidor: " + e.getMessage());
        }
    }
}
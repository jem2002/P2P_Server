package RequestRouter.clients.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.shared.schema.JsonSchema;
import com.universidad.messaging.server.shared.schema.commentSchema.Comment;
import JsonSerializer.ResponseBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.universidad.messaging.server.gestion.cluster.api.IReplicationManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.servicios.api.ICommentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.universidad.messaging.server.shared.events.ReplicationEvent;

public class CommentDocumentHandlerClient implements ClientActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommentDocumentHandlerClient.class);

    private final ICommentManager commentManager;
    private final ResponseBuilder serializer;
    private final IReplicationManager replicationManager;
    private final String localNodeId;
    private final IBroadcastManager broadcastManager;
    private final ClientActionHandler listCommentsHandler;

    // Ya no se inyecta el SentimentService aquí, se limpia el constructor
    public CommentDocumentHandlerClient(ICommentManager commentManager, ResponseBuilder serializer, IReplicationManager replicationManager,  String localNodeId,
                                        IBroadcastManager broadcastManager, ClientActionHandler listCommentsHandler
                                        ) {
        this.commentManager = commentManager;
        this.serializer = serializer;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;
        this.broadcastManager = broadcastManager;
        this.listCommentsHandler = listCommentsHandler;

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

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.createObjectNode();
            node.put("document_id", documentId);

            broadcastManager.broadcast(listCommentsHandler.handle(node, null));


            return serializer.buildSuccessResponse(JsonSchema.ACTION_COMMENT_DOCUMENT, mensajeExito);

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
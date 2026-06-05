package RequestRouter.clients.handlers;

import CommentService.CommentManager;
import JsonSchema.CommentInfo;
import JsonSerializer.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ports.api.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCommentsHandlerClient implements ClientActionHandler {


    private static final Logger logger = LoggerFactory.getLogger(ListCommentsHandlerClient.class);

    private final CommentManager commentManager;
    private final ResponseBuilder serializer;

    // Ya no se inyecta el SentimentService aquí, se limpia el constructor
    public ListCommentsHandlerClient(CommentManager commentManager, ResponseBuilder serializer) {
        this.commentManager = commentManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        logger.info("Procesando solicitud de listar comentarios desde la IP: {}", clientIp);

        // 1. Validación de la existencia del payload
        if (payload == null || !payload.has("document_id")) {
            logger.error("Payload inválido o carece del campo 'document_id'.");
            return serializer.buildErrorResponse("Falta el campo obligatorio 'document_id'.");
        }

        try {
            // 2. Extraer el document_id de manera segura
            long documentId = payload.get("document_id").asLong();

            // 3. Invocar la lógica de negocio en el CommentManager
            List<CommentInfo> comentarios = commentManager.listarComentariosPorDocumento(documentId);

            // 4. Transformar la lista de CommentInfo a la estructura esperada por ResponseBuilder (List<Map<String, Object>>)
            List<Map<String, Object>> items = new ArrayList<>();
            for (CommentInfo info : comentarios) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", info.getId());
                itemMap.put("document_id", info.getDocumentId());
                itemMap.put("user_id", info.getUserId());
                itemMap.put("username", info.getUsername());
                itemMap.put("content", info.getContent());

                // Convertimos el Enum y LocalDateTime a String para una representación JSON limpia
                itemMap.put("sentiment", info.getSentiment() != null ? info.getSentiment().name() : null);
                itemMap.put("confidence", info.getConfidence());
                itemMap.put("created_at", info.getCreatedAt() != null ? info.getCreatedAt().toString() : null);

                items.add(itemMap);
            }

            // 5. Construir y retornar la respuesta estructurada de éxito empleando buildObjectListResponse
            return serializer.buildObjectListResponse("LIST_COMMENTS", items, "comments");

        } catch (IllegalArgumentException e) {
            logger.warn("Error de validación de negocio al listar comentarios: {}", e.getMessage());
            return serializer.buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado en el manejador ListCommentsHandlerClient", e);
            return serializer.buildErrorResponse("Error interno del servidor al recuperar el historial de comentarios.");
        }
    }



}

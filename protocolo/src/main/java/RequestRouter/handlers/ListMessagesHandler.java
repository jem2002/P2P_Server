package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import DocumentService.DocumentManager;
import RequestRouter.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Maneja la acción LIST_MESSAGES: retorna la lista de mensajes disponibles.
 */
public class ListMessagesHandler implements ActionHandler {

    private final DocumentManager documentManager;
    private final ResponseBuilder serializer;

    public ListMessagesHandler(DocumentManager documentManager, ResponseBuilder serializer) {
        this.documentManager = documentManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, String>> msgs = documentManager.obtenerMensajesDisponibles();
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_MESSAGES, msgs, "mensajes");
    }
}

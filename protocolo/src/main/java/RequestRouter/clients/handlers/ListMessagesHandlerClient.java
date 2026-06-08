package RequestRouter.clients.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import DocumentService.DocumentManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Maneja la acción LIST_MESSAGES: retorna la lista de mensajes disponibles.
 */
public class ListMessagesHandlerClient implements ClientActionHandler {

    private final DocumentManager documentManager;
    private final ResponseBuilder serializer;

    public ListMessagesHandlerClient(DocumentManager documentManager, ResponseBuilder serializer) {
        this.documentManager = documentManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        // El cliente envía su username para que el servidor filtre solo sus mensajes
        String requestingUser = (payload != null && payload.has("username"))
                ? payload.get("username").asText() : null;
        List<Map<String, String>> msgs = documentManager.obtenerMensajesDisponibles(requestingUser);
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_MESSAGES, msgs, "mensajes");
    }
}

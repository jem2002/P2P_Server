package RequestRouter.clients.handlers;

import JsonSchema.DocumentInfo;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import DocumentService.DocumentManager;
import ports.api.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Maneja la acción LIST_DOCUMENTS: retorna la lista de archivos disponibles.
 */
public class ListDocumentsHandlerClient implements ClientActionHandler {

    private final DocumentManager documentManager;
    private final ResponseBuilder serializer;

    public ListDocumentsHandlerClient(DocumentManager documentManager, ResponseBuilder serializer) {
        this.documentManager = documentManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        String username = null;
        if (payload != null && payload.has("username") && !payload.get("username").isNull()) {
            username = payload.get("username").asText();
        }

        List<DocumentInfo> docs = documentManager.obtenerArchivosDisponibles(username);
        List<Map<String, String>> mapped = new ArrayList<>();
        for (DocumentInfo d : docs) {
            Map<String, String> item = new HashMap<>();
            item.put("id", String.valueOf(d.getId()));
            item.put("nombre", d.getNombre());
            item.put("tamano_bytes", String.valueOf(d.getSizeBytes()));
            item.put("extension", d.getExtension());
            item.put("propietario", d.getPropietario());
            mapped.add(item);
        }
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_DOCUMENTS, mapped, "documentos");
    }
}

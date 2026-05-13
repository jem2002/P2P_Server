package RequestRouter.handlers;

import JsonSchema.DocumentInfo;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import DocumentService.DocumentManager;
import RequestRouter.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Maneja la acción LIST_DOCUMENTS: retorna la lista de archivos disponibles.
 */
public class ListDocumentsHandler implements ActionHandler {

    private final DocumentManager documentManager;
    private final ResponseBuilder serializer;

    public ListDocumentsHandler(DocumentManager documentManager, ResponseBuilder serializer) {
        this.documentManager = documentManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<DocumentInfo> docs = documentManager.obtenerArchivosDisponibles();
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

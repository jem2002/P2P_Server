package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSchema.LogEntry;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import RequestRouter.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Maneja la acción LIST_LOGS: retorna la bitácora de auditoría.
 * También es reutilizado por otros handlers para broadcast de logs actualizado.
 */
public class ListLogsHandler implements ActionHandler {

    private final LogManager logManager;
    private final ResponseBuilder serializer;

    public ListLogsHandler(LogManager logManager, ResponseBuilder serializer) {
        this.logManager = logManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<LogEntry> logs = logManager.listarLogs();
        List<Map<String, String>> mapped = new ArrayList<>();
        for (LogEntry l : logs) {
            Map<String, String> item = new HashMap<>();
            item.put("id", String.valueOf(l.getId()));
            item.put("document_id", l.getDocumentId());
            item.put("sender", l.getSender());
            item.put("action", l.getAction());
            item.put("protocol", l.getProtocol());
            item.put("status", l.getStatus());
            item.put("details", l.getDetails());
            item.put("timestamp", l.getTimestamp());
            mapped.add(item);
        }
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_LOGS, mapped, "logs");
    }
}

package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import RequestRouter.ActionHandler;
import RequestRouter.TransferManager;
import RequestRouter.TransferTicket;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maneja la acción UPLOAD_INIT: genera un ticket de transferencia para subida de archivos.
 */
public class UploadInitHandler implements ActionHandler {

    private final UserManager userManager;
    private final TransferManager transferManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ListLogsHandler listLogsHandler;

    public UploadInitHandler(UserManager userManager, TransferManager transferManager,
                             LogManager logManager, BroadcastManager broadcastManager,
                             ResponseBuilder serializer, ListLogsHandler listLogsHandler) {
        this.userManager = userManager;
        this.transferManager = transferManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.serializer = serializer;
        this.listLogsHandler = listLogsHandler;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        String filename = payload.get("filename").asText();
        long size = payload.get("size").asLong();
        String extension = payload.get("extension").asText();
        String mimeType = payload.get("mimeType").asText();
        String username = payload.get("username").asText();

        long userId = userManager.obtenerIdUsuario(username);

        String token = java.util.UUID.randomUUID().toString();
        TransferTicket ticket = new TransferTicket(token, filename, size, extension, mimeType, userId, clientIp);
        transferManager.registrarTicket(ticket);

        logManager.registrarAccion(null, userId, "UPLOAD_INIT", "SUCCESS",
                "Ticket de subida generado para " + username + " (Archivo: " + filename + ")");
        broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_UPLOAD_INIT, token);
    }
}

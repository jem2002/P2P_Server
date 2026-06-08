package RequestRouter.clients.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import RequestRouter.files.TransferManager;
import ports.api.TransferTicket;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maneja la acción UPLOAD_INIT: genera un ticket de transferencia para subida de archivos.
 */
public class UploadInitHandlerClient implements ClientActionHandler {

    private final UserManager userManager;
    private final TransferManager transferManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ClientActionHandler listLogsHandler;

    public UploadInitHandlerClient(UserManager userManager, TransferManager transferManager,
                                   LogManager logManager, BroadcastManager broadcastManager,
                                   ResponseBuilder serializer, ClientActionHandler listLogsHandler) {
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
        
        String targetUsername = payload.get("targetUsername").asText().trim();

        long userId = userManager.obtenerIdUsuario(username);

        String token = "UPL-" + java.util.UUID.randomUUID();
        TransferTicket ticket = new TransferTicket(token, filename, size, extension, mimeType, userId, clientIp, targetUsername, username);
        transferManager.registrarTicket(ticket);

        logManager.registrarAccion(null, userId, "UPLOAD_INIT", "SUCCESS",
                "Ticket de subida generado para " + username + " (Archivo: " + filename + ")");
        broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_UPLOAD_INIT, token);
    }
}

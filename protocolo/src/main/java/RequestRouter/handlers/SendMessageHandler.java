package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import DocumentService.DocumentManager;
import RequestRouter.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Maneja la acción SEND_MESSAGE: persiste el mensaje como documento
 * y lo retransmite en tiempo real a todos los clientes conectados.
 */
public class SendMessageHandler implements ActionHandler {

    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ListLogsHandler listLogsHandler;

    public SendMessageHandler(UserManager userManager, DocumentManager documentManager,
                              LogManager logManager, BroadcastManager broadcastManager,
                              ResponseBuilder serializer, ListLogsHandler listLogsHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.serializer = serializer;
        this.listLogsHandler = listLogsHandler;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        String fromUser = payload.get("username").asText();
        String content = payload.get("message").asText();

        long userId = userManager.obtenerIdUsuario(fromUser);

        InputStream textStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String nombreArchivo = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";

        documentManager.procesarRecepcionDocumento(
                textStream, nombreArchivo, content.length(), ".txt", "text/plain", userId, clientIp, "MESSAGE");

        String mensajeRealTime = serializer.buildSuccessResponse(
                JsonSchema.ACTION_NEW_MESSAGE,
                "De " + fromUser + ": " + content);
        broadcastManager.broadcast(mensajeRealTime);

        logManager.registrarAccion(null, userId, "SEND_MESSAGE", "SUCCESS",
                "Mensaje enviado por " + fromUser);
        broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

        return serializer.buildSuccessResponse(JsonSchema.ACTION_SEND_MESSAGE,
                "Mensaje procesado, encriptado y entregado.");
    }
}

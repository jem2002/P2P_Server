package RequestRouter.handlers;

import JsonSchema.DownloadDetails;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import DocumentService.DocumentManager;
import RequestRouter.ActionHandler;
import RequestRouter.TransferManager;
import RequestRouter.TransferTicket;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maneja la acción DOWNLOAD_INIT: genera un ticket de transferencia para descarga de archivos.
 * Soporta los modos: ORIGINAL (ORG), ENCRIPTADO (ENC), HASH (HSH), DESCIFRADO (default).
 */
public class DownloadInitHandler implements ActionHandler {

    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final TransferManager transferManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ListLogsHandler listLogsHandler;

    public DownloadInitHandler(UserManager userManager, DocumentManager documentManager,
                               TransferManager transferManager, LogManager logManager,
                               BroadcastManager broadcastManager, ResponseBuilder serializer,
                               ListLogsHandler listLogsHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.transferManager = transferManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.serializer = serializer;
        this.listLogsHandler = listLogsHandler;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        long docId = payload.get("document_id").asLong();
        String username = payload.has("username") ? payload.get("username").asText() : "UsuarioDesconocido";
        long userId = resolverUserId(username);

        DownloadDetails detalles = documentManager.obtenerDetallesDescarga(docId);
        long size = detalles.getSizeBytes();
        String encryptedPath = detalles.getRutaCifrada();

        String format = payload.has("format") ? payload.get("format").asText().toUpperCase() : "";
        String prefix = "DWN-";
        String ticketInfo = encryptedPath;

        switch (format) {
            case "ORG":
                prefix = "DWN-ORG-";
                ticketInfo = String.valueOf(docId);
                break;
            case "HSH":
                prefix = "DWN-HSH-";
                ticketInfo = String.valueOf(docId);
                if (encryptedPath == null || !encryptedPath.startsWith("PEER:")) {
                    size = documentManager.obtenerTamanoHash(docId);
                }
                break;
            case "ENC":
                prefix = "DWN-ENC-";
                ticketInfo = String.valueOf(docId);
                if (encryptedPath == null || !encryptedPath.startsWith("PEER:")) {
                    size = documentManager.obtenerTamanoEncriptado(docId);
                }
                break;
            default:
                break;
        }

        if (encryptedPath != null && encryptedPath.startsWith("PEER:")) {
            ticketInfo = encryptedPath;
        }

        String token = prefix + java.util.UUID.randomUUID().toString();
        TransferTicket ticket = new TransferTicket(token, detalles.getNombre(), size, "", ticketInfo, userId, clientIp);
        transferManager.registrarTicket(ticket);

        logManager.registrarAccion(docId, userId, "DOWNLOAD_INIT", "SUCCESS",
                "Ticket de descarga (" + format + ") generado para " + username + " (ID doc: " + docId + ")");
        broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

        return serializer.buildDownloadInitResponse(token, size, docId);
    }

    private long resolverUserId(String username) {
        try {
            return userManager.obtenerIdUsuario(username);
        } catch (Exception e) {
            return 0;
        }
    }
}

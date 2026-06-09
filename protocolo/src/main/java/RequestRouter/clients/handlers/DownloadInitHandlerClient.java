package RequestRouter.clients.handlers;

import com.universidad.messaging.server.shared.schema.documentSchema.DownloadDetails;
import JsonSerializer.ResponseBuilder;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import RequestRouter.files.TransferManager;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.ILogManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maneja la acción DOWNLOAD_INIT: genera un ticket de transferencia para descarga de archivos.
 * Soporta los modos: ORIGINAL (ORG), ENCRIPTADO (ENC), HASH (HSH), DESCIFRADO (default).
 */
public class DownloadInitHandlerClient implements ClientActionHandler {

    private final IUserManager userManager;
    private final IDocumentManager documentManager;
    private final TransferManager transferManager;
    private final ILogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ClientActionHandler listLogsHandler;

    public DownloadInitHandlerClient(IUserManager userManager, IDocumentManager documentManager,
                                     TransferManager transferManager, ILogManager logManager,
                                     BroadcastManager broadcastManager, ResponseBuilder serializer,
                                     ClientActionHandler listLogsHandler) {
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

        String token = prefix + java.util.UUID.randomUUID();
        TransferTicket ticket = new TransferTicket(token, detalles.getNombre(), size, "", ticketInfo, userId, clientIp, username);
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

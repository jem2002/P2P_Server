package RequestRouter.files.handlers;

import DocumentService.DocumentManager;
import JsonSchema.DownloadMode;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.FileActionHandler;
import ports.api.TransferTicket;

import java.io.InputStream;
import java.io.OutputStream;

public class DownloadFileHandler implements FileActionHandler {

    private final DocumentManager documentManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listLogsHandler;

    public DownloadFileHandler(DocumentManager documentManager,
                                     LogManager logManager, BroadcastManager broadcastManager,
                               ClientActionHandler listLogsHandler) {
        this.documentManager = documentManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.listLogsHandler = listLogsHandler;
    }

    @Override
    public void process(TransferTicket ticket, InputStream in, OutputStream out) throws Exception {
        DownloadMode mode = DownloadMode.fromTicket(ticket);
        long docIdToLog = 0;

        switch (mode) {
            case ORIGINAL:
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoOriginal(docIdToLog, out);
                break;

            case ENCRYPTED:
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoEncriptado(docIdToLog, out);
                break;

            case HASH:
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoHash(docIdToLog, out);
                break;

            case DECRYPTED:
                String encryptedPath = ticket.getMimeType();
                documentManager.enviarDocumentoAlCliente(encryptedPath, out);
                break;
        }

        logManager.registrarAccion(docIdToLog > 0 ? docIdToLog : null, ticket.getOwnerUserId(),
                "DOWNLOAD_COMPLETE", "SUCCESS", "Descarga finalizada en modo: " + mode.name());
        broadcastManager.broadcast(listLogsHandler.handle(null, null));
    }
}

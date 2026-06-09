package RequestRouter.files.handlers;

import com.universidad.messaging.server.shared.schema.documentSchema.DownloadMode;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.FileActionHandler;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.ILogManager;
import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;

import java.io.InputStream;
import java.io.OutputStream;

public class DownloadFileHandler implements FileActionHandler {

    private final IDocumentManager documentManager;
    private final ILogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ClientActionHandler listLogsHandler;

    public DownloadFileHandler(IDocumentManager documentManager,
                                     ILogManager logManager, BroadcastManager broadcastManager,
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

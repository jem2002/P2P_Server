package RequestRouter.files;

import DocumentService.DocumentManager;
import JsonSchema.FileAction;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import RequestRouter.files.handlers.DownloadFileHandler;
import RequestRouter.files.handlers.UploadFileHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.FileActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.IFileRequestDispatcher;
import models.LocalNodeInfo;

import ports.api.TransferTicket;
import replication.ReplicationManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileRouter implements IFileRequestDispatcher {

    private final Map<String, FileActionHandler> fileHandlers = new HashMap<>();;

    /**
     * Inyectamos los handlers ya construidos.
     */
    public FileRouter(DocumentManager documentManager,
                      LogManager logManager, BroadcastManager broadcastManager,
                      ClientActionHandler listLogsHandler, ClientActionHandler listDocumentsHandler, ReplicationManager replicationManager,
                      LocalNodeInfo localNodeInfo) {


        fileHandlers.put(FileAction.DWN.name(), new DownloadFileHandler(documentManager, logManager, broadcastManager, listLogsHandler));
        fileHandlers.put(FileAction.UPL.name(), new UploadFileHandler(documentManager, broadcastManager, listDocumentsHandler, listLogsHandler, replicationManager, localNodeInfo));
    }

    @Override
    public void routeAndProcess(TransferTicket ticket, InputStream in, OutputStream out) throws Exception {
        if (ticket == null || ticket.getFileAction() == null) {
            throw new IllegalArgumentException("El ticket o su acción de archivo (FileAction) no pueden ser nulos.");
        }

        // 1. Extraemos la acción directamente como un Enum fuerte (UPL o DWN)
        FileAction action = ticket.getFileAction();

        // 2. Buscamos el handler correspondiente en el mapa que inicializaste
        FileActionHandler handler = fileHandlers.get(action.name());

        // 3. Validación defensiva por si en el futuro se agrega una acción al enum pero no al mapa
        if (handler == null) {
            throw new IllegalArgumentException("No se encontró un manejador registrado para la acción: " + action);
        }

        // 4. Delegamos la ejecución al handler correspondiente de forma polimórfica
        handler.process(ticket, in, out);
    }
}
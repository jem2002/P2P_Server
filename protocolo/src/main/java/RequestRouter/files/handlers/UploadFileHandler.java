package RequestRouter.files.handlers;

import DocumentService.DocumentManager;
import JsonSerializer.ResponseBuilder;
import MessageParser.BroadcastManager;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.FileActionHandler;
import models.LocalNodeInfo;


import ports.api.TransferTicket;
import replication.ReplicationEvent;
import replication.ReplicationManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UploadFileHandler implements FileActionHandler {

    private final ReplicationManager replicationManager;
    private final LocalNodeInfo localNodeInfo;

    private final DocumentManager documentManager;
    private final BroadcastManager broadcastManager;

    private final ClientActionHandler listDocumentsHandler;
    private final ClientActionHandler listLogsHandler;

    public UploadFileHandler(DocumentManager documentManager,
                                   BroadcastManager broadcastManager,
                                   ClientActionHandler listDocumentsHandler,
                                   ClientActionHandler listLogsHandler,
                                   ReplicationManager replicationManager,
                                   LocalNodeInfo localNodeInfo
                                   ) {
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.listDocumentsHandler = listDocumentsHandler;
        this.listLogsHandler = listLogsHandler;
        this.replicationManager = replicationManager;
        this. localNodeInfo = localNodeInfo;
    }

    @Override
    public void process(TransferTicket ticket, InputStream in, OutputStream out) throws Exception {

        String docType = "FILE";

        if (ticket.getTargetUsername() != null) {
            String target = ticket.getTargetUsername().trim();

            // Si no está vacío y NO es "ALL", entonces es un archivo privado
            if (!target.isEmpty() && !target.equalsIgnoreCase("ALL")) {
                docType = "PRIVATE_FILE_TO:" + target;
            }
        }
        Long docId = documentManager.procesarRecepcionDocumento(
                in, ticket.getFilename(), ticket.getSizeBytes(), ticket.getExtension(),
                ticket.getMimeType(), ticket.getOwnerUserId(), ticket.getOwnerIp(), docType);

        if (docId != null) {
            out.write("SUCCESS\n".getBytes(StandardCharsets.UTF_8));
            out.flush();


            replicationManager.propagate(
                    ReplicationEvent.documentUploaded(
                            localNodeInfo.getNodeId(),
                            docId, // docId (0 if not known)
                            ticket.getFilename(),
                            ticket.getSizeBytes(),
                            ticket.getExtension(),
                            ticket.getMimeType(),
                            docType, // default docType
                            ticket.getOwnerUsername(),
                            localNodeInfo.getHost(), // host
                            localNodeInfo.getClientPort() // clientPort
                    )
            );


            broadcastManager.broadcast(listDocumentsHandler.handle(null, null));
            broadcastManager.broadcast(listLogsHandler.handle(null, null));
        } else {
            out.write("ERROR\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

    }

}

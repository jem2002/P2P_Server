package RequestRouter.files.handlers;

import MessageParser.BroadcastManager;
import com.universidad.messaging.server.gestion.cluster.api.IReplicationManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.FileActionHandler;
import com.universidad.messaging.server.servicios.api.IDocumentManager;


import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;
import com.universidad.messaging.server.shared.events.ReplicationEvent;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UploadFileHandler implements FileActionHandler {

    private final IReplicationManager replicationManager;

    private final IDocumentManager documentManager;
    private final BroadcastManager broadcastManager;

    private final ClientActionHandler listDocumentsHandler;
    private final ClientActionHandler listLogsHandler;

    private final String localNodeId;
    private final String localHost;
    private final int localPort;


    public UploadFileHandler(IDocumentManager documentManager,
                                   BroadcastManager broadcastManager,
                                   ClientActionHandler listDocumentsHandler,
                                   ClientActionHandler listLogsHandler,
                                   IReplicationManager replicationManager,
                                   String localNodeId,
                                   String localHost,
                                   int localPort
                                   ) {
        this.documentManager = documentManager;
        this.broadcastManager = broadcastManager;
        this.listDocumentsHandler = listDocumentsHandler;
        this.listLogsHandler = listLogsHandler;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;
        this.localHost = localHost;
        this.localPort = localPort;

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
                            localNodeId,
                            docId, // docId (0 if not known)
                            ticket.getFilename(),
                            ticket.getSizeBytes(),
                            ticket.getExtension(),
                            ticket.getMimeType(),
                            docType, // default docType
                            ticket.getOwnerUsername(),
                            localHost, // host
                            localPort // clientPort
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

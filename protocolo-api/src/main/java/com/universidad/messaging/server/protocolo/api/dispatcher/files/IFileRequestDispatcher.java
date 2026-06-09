package com.universidad.messaging.server.protocolo.api.dispatcher.files;

import java.io.InputStream;
import java.io.OutputStream;
import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;

public interface IFileRequestDispatcher {
    void routeAndProcess(TransferTicket ticket, InputStream in, OutputStream out) throws Exception;
}

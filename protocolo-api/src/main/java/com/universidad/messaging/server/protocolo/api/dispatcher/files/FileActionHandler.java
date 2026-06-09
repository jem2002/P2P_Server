package com.universidad.messaging.server.protocolo.api.dispatcher.files;

import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileActionHandler {
    void process(TransferTicket ticket, InputStream in, OutputStream out) throws Exception;
}

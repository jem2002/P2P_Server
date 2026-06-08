package com.universidad.messaging.server.gestion.de.conexiones.api.handler;

import com.universidad.messaging.server.protocolo.api.dispatcher.files.IFileRequestDispatcher;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.ITransferDispatcher;

import java.net.Socket;

public interface IFileHandlerFactory {

    Runnable create(Socket clientSocket, String token, ITransferDispatcher transferDispatcher, IFileRequestDispatcher requestDispatcher);
}

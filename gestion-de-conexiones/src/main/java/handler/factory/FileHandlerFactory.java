package handler.factory;

import com.universidad.messaging.server.gestion.de.conexiones.api.handler.IFileHandlerFactory;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.IFileRequestDispatcher;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.ITransferDispatcher;
import handler.FileTransferHandler;

import java.net.Socket;

public class FileHandlerFactory implements IFileHandlerFactory {

    @Override
    public Runnable create(Socket clientSocket, String token, ITransferDispatcher transferDispatcher, IFileRequestDispatcher requestDispatcher) {
        return new FileTransferHandler(clientSocket, token, transferDispatcher, requestDispatcher);
    }
}

package com.universidad.messaging.server.gestion.de.conexiones.api.pool;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public interface IPooledClientConnection {

    void setSocket(Socket socket) throws Exception;

    Socket getSocket();

    InputStream getInputStream();

    OutputStream getOutputStream();

    void reset();

}

package com.universidad.messaging.server.network;

import com.universidad.messaging.server.business.MessageProcessor;
import com.universidad.messaging.server.pool.ClientConnectionPool;
import com.universidad.messaging.server.pool.PooledClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final PooledClientConnection connection;
    private final ClientConnectionPool pool;
    private final MessageProcessor messageProcessor;

    public ClientHandler(PooledClientConnection connection, ClientConnectionPool pool, MessageProcessor messageProcessor) {
        this.connection = connection;
        this.pool = pool;
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void run() {
        try {
            if (connection.getSocket() == null || connection.getSocket().isClosed()) {
                logger.warn("ClientHandler ejecutado sin un socket válido asignado.");
                return;
            }

            String clientAddress = connection.getSocket().getRemoteSocketAddress().toString();
            logger.info("Procesando conexión del cliente: {}", clientAddress);

            InputStream in = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            if (in == null || out == null) {
                logger.error("Streams no inicializados en la conexión pooleable.");
                return;
            }

            byte[] buffer = new byte[4096];
            int bytesRead = in.read(buffer);

            if (bytesRead > 0) {
                String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                
                // Se delega al procesador de mensajes
                String response = messageProcessor.processMessage(clientAddress, received);
                
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
                logger.debug("Respuesta enviada al cliente {}.", clientAddress);
            } else {
                logger.debug("Cliente {} cerró la conexión sin enviar datos.", clientAddress);
            }

            logger.info("Procesamiento finalizado para el cliente: {}", clientAddress);

        } catch (IOException e) {
            logger.error("Error de I/O procesando al cliente.", e);
        } catch (Exception e) {
            logger.error("Error inesperado en ClientHandler.", e);
        } finally {
            pool.release(connection);
            logger.debug("Conexión devuelta al pool de conexiones.");
        }
    }
}

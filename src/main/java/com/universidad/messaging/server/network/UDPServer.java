package com.universidad.messaging.server.network;

import com.universidad.messaging.server.business.MessageProcessor;
import com.universidad.messaging.server.pool.ClientConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UDPServer.class);
    private final int port;
    private final ClientConnectionPool connectionPool;
    private DatagramSocket datagramSocket;
    private volatile boolean running = false;
    
    private static final int MAX_BUFFER_SIZE = 65507;
    private final MessageProcessor messageProcessor;

    public UDPServer(int port, ClientConnectionPool connectionPool, MessageProcessor messageProcessor) {
        this.port = port;
        this.connectionPool = connectionPool;
        this.messageProcessor = messageProcessor;
    }

    public void start() {
        this.running = true;
        new Thread(this, "UDPServerThread").start();
    }

    public void stop() {
        this.running = false;
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(port);
            logger.info("UDP Server iniciado en el puerto {}", port);

            while (running) {
                try {
                    byte[] buffer = new byte[MAX_BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    datagramSocket.receive(packet);
                    
                    connectionPool.dispatchDatagram(packet, datagramSocket, messageProcessor);
                    
                } catch (IOException e) {
                    if (running) {
                        logger.error("Error recibiendo paquete UDP", e);
                    } else {
                        logger.info("DatagramSocket UDP fue cerrado, finalizando bucle de recepción.");
                    }
                }
            }
        } catch (SocketException e) {
            logger.error("Fallo al iniciar el servidor UDP en el puerto {}", port, e);
        } finally {
            logger.info("UDP Server Thread finalizado.");
        }
    }
}

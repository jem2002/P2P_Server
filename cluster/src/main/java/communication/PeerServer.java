package communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP que acepta conexiones entrantes de otros servidores peer.
 * Cada conexión entrante se despacha a un {@link PeerMessageHandler}.
 *
 * Principio aplicado: SRP — solo acepta conexiones. El procesamiento
 * de mensajes se delega al handler.
 */
public class PeerServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PeerServer.class);

    private final int port;
    private final PeerMessageHandler messageHandler;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public PeerServer(int port, PeerMessageHandler messageHandler) {
        this.port = port;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("PeerServer escuchando conexiones de peers en TCP:{}", port);

            while (running) {
                Socket peerSocket = serverSocket.accept();
                logger.info("Conexión peer entrante desde {}", peerSocket.getRemoteSocketAddress());

                // Cada peer en su propio hilo
                new Thread(() -> messageHandler.handlePeerConnection(peerSocket),
                        "PeerHandler-" + peerSocket.getRemoteSocketAddress()).start();
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en PeerServer", e);
            } else {
                logger.info("PeerServer detenido.");
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error cerrando PeerServer", e);
        }
    }
}

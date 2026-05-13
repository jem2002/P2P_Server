package communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Conexión TCP persistente a un servidor peer.
 * Encapsula el socket y provee métodos de envío/recepción de mensajes JSON.
 *
 * Principio aplicado: Low Coupling (GRASP) — encapsula los detalles
 * de la conexión TCP para que el resto del cluster trabaje con una
 * abstracción de alto nivel.
 */
public class PeerConnection implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PeerConnection.class);

    private final String targetNodeId;
    private final String host;
    private final int port;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private volatile boolean connected;

    public PeerConnection(String targetNodeId, String host, int port) {
        this.targetNodeId = targetNodeId;
        this.host = host;
        this.port = port;
        this.connected = false;
    }

    /**
     * Establece la conexión TCP al peer.
     */
    public synchronized void connect() throws IOException {
        if (connected) return;

        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        socket.setSoTimeout(5000);
        outputStream = socket.getOutputStream();
        inputStream = socket.getInputStream();
        connected = true;

        logger.info("Conexión establecida con peer {} en {}:{}", targetNodeId, host, port);
    }

    /**
     * Envía un mensaje JSON al peer (terminado en newline).
     */
    public synchronized void send(String jsonMessage) throws IOException {
        if (!connected) {
            connect();
        }

        try {
            outputStream.write((jsonMessage + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (IOException e) {
            connected = false;
            throw e;
        }
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public String getTargetNodeId() {
        return targetNodeId;
    }

    @Override
    public synchronized void close() {
        connected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.debug("Error cerrando PeerConnection a {}", targetNodeId, e);
        } finally {
            socket = null;
            inputStream = null;
            outputStream = null;
        }
    }
}

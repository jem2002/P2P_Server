package MessageParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Gestiona la retransmisión de mensajes a todos los clientes conectados.
 *
 * Refactorizado: ahora remueve streams inactivos tras fallo de escritura
 * para evitar acumulación de referencias muertas.
 */
public class BroadcastManager {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);
    private final Set<OutputStream> activeStreams = new CopyOnWriteArraySet<>();

    public void addStream(OutputStream out) {
        activeStreams.add(out);
    }

    public void removeStream(OutputStream out) {
        activeStreams.remove(out);
    }

    public void broadcast(String jsonMessage) {
        logger.debug("Haciendo broadcast a {} clientes", activeStreams.size());
        byte[] messageBytes = (jsonMessage + "\n").getBytes(StandardCharsets.UTF_8);

        for (OutputStream out : activeStreams) {
            try {
                out.write(messageBytes);
                out.flush();
            } catch (Exception e) {
                logger.error("Fallo al enviar broadcast a un stream inactivo. Removiendo del pool.");
                activeStreams.remove(out);
            }
        }
    }
}
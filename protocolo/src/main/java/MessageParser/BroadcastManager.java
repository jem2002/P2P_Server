package MessageParser;

import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;




public class BroadcastManager implements IBroadcastManager {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);
    private final Set<OutputStream> activeStreams = new CopyOnWriteArraySet<>();

    @Override
    public void addStream(OutputStream out) {
        activeStreams.add(out);
    }

    @Override
    public void removeStream(OutputStream out) {
        activeStreams.remove(out);
    }

    @Override
    public void broadcast(String jsonMessage) {
        logger.debug("Encolando broadcast LOCAL-ONLY a {} clientes", activeStreams.size());

        byte[] messageBytes = (jsonMessage + "\n").getBytes(StandardCharsets.UTF_8);

        for (OutputStream out : activeStreams) {
            try {
                synchronized (out) {
                    out.write(messageBytes);
                    out.flush();
                }
            } catch (Exception e) {
                logger.error("Fallo al enviar broadcast a un stream inactivo. Removiendo del pool.");
                activeStreams.remove(out);
            }
        }
    }
}
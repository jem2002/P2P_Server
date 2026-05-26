package MessageParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import ports.api.IBroadcastManager;

/**
 * Gestiona la retransmisión de mensajes a todos los clientes conectados.
 *
 * Refactorizado: ahora remueve streams inactivos tras fallo de escritura
 * para evitar acumulación de referencias muertas.
 *
 * Extensión P2P: soporta un {@link FederatedBroadcastHook} opcional que,
 * cuando está presente, reenvía los broadcasts a los servidores peer.
 * Principio aplicado: OCP — el comportamiento original no se modifica.
 */
public class BroadcastManager implements IBroadcastManager {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);
    private final Set<OutputStream> activeStreams = new CopyOnWriteArraySet<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Hook opcional para broadcast federado P2P. Null si cluster deshabilitado. */
    private volatile Consumer<String> federatedHook;

    public void addStream(OutputStream out) {
        activeStreams.add(out);
    }

    public void removeStream(OutputStream out) {
        activeStreams.remove(out);
    }

    /**
     * Inyecta el hook de broadcast federado (solo cuando cluster P2P está activo).
     */
    @Override
    public void setFederatedHook(Consumer<String> hook) {
        this.federatedHook = hook;
        logger.info("FederatedBroadcastHook configurado — broadcasts se propagarán a peers.");
    }

    @Override
    public void broadcast(String jsonMessage) {
        logger.debug("Encolando broadcast a {} clientes locales", activeStreams.size());
        executor.submit(() -> doBroadcast(jsonMessage, true));
    }

    @Override
    public void broadcastLocalOnly(String jsonMessage) {
        logger.debug("Encolando broadcast LOCAL-ONLY a {} clientes", activeStreams.size());
        executor.submit(() -> doBroadcast(jsonMessage, false));
    }

    private void doBroadcast(String jsonMessage, boolean propagateToPeers) {
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

        // Extensión P2P: propagar a servidores peer si el hook está configurado
        if (propagateToPeers && federatedHook != null) {
            try {
                federatedHook.accept(jsonMessage);
            } catch (Exception e) {
                logger.error("Error en broadcast federado a peers", e);
            }
        }
    }
}
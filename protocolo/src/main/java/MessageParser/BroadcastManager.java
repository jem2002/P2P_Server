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
 *
 * Extensión P2P: soporta un {@link FederatedBroadcastHook} opcional que,
 * cuando está presente, reenvía los broadcasts a los servidores peer.
 * Principio aplicado: OCP — el comportamiento original no se modifica.
 */
public class BroadcastManager {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);
    private final Set<OutputStream> activeStreams = new CopyOnWriteArraySet<>();

    /** Hook opcional para broadcast federado P2P. Null si cluster deshabilitado. */
    private volatile FederatedBroadcastHook federatedHook;

    public void addStream(OutputStream out) {
        activeStreams.add(out);
    }

    public void removeStream(OutputStream out) {
        activeStreams.remove(out);
    }

    /**
     * Inyecta el hook de broadcast federado (solo cuando cluster P2P está activo).
     */
    public void setFederatedHook(FederatedBroadcastHook hook) {
        this.federatedHook = hook;
        logger.info("FederatedBroadcastHook configurado — broadcasts se propagarán a peers.");
    }

    public void broadcast(String jsonMessage) {
        logger.debug("Haciendo broadcast a {} clientes locales", activeStreams.size());
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

        // Extensión P2P: propagar a servidores peer si el hook está configurado
        if (federatedHook != null) {
            try {
                federatedHook.broadcastToPeers(jsonMessage);
            } catch (Exception e) {
                logger.error("Error en broadcast federado a peers", e);
            }
        }
    }

    /**
     * Envía el mensaje SOLO a los clientes locales de este servidor,
     * sin propagar a los peers (federated hook ignorado).
     *
     * Usado para sincronización interna tras eventos de replicación,
     * evitando que la lista de clientes de un nodo sobreescriba
     * la lista del nodo receptor.
     */
    public void broadcastLocalOnly(String jsonMessage) {
        logger.debug("Broadcast LOCAL-ONLY a {} clientes", activeStreams.size());
        byte[] messageBytes = (jsonMessage + "\n").getBytes(StandardCharsets.UTF_8);
        for (OutputStream out : activeStreams) {
            try {
                out.write(messageBytes);
                out.flush();
            } catch (Exception e) {
                logger.error("Fallo al enviar broadcastLocalOnly a un stream.");
                activeStreams.remove(out);
            }
        }
    }
}
package registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro thread-safe de los OutputStreams de los clientes locales conectados.
 *
 * Permite la entrega directa de mensajes a un cliente específico sin hacer broadcast.
 * Es la pieza clave para el enrutamiento de mensajes dirigidos (local o recibidos
 * desde un peer remoto mediante PEER_ROUTE).
 *
 * Principio aplicado: Information Expert (GRASP) — esta clase es la única que conoce
 * la correlación username → socket de salida.
 */
public class LocalClientRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocalClientRegistry.class);

    /** username → OutputStream del socket del cliente */
    private final ConcurrentHashMap<String, OutputStream> clientStreams = new ConcurrentHashMap<>();

    /**
     * Registra el stream de salida de un cliente que acaba de conectarse.
     */
    public void register(String username, OutputStream out) {
        clientStreams.put(username, out);
        logger.debug("Cliente '{}' registrado en LocalClientRegistry", username);
    }

    /**
     * Elimina el stream de un cliente que se desconectó.
     */
    public void unregister(String username) {
        clientStreams.remove(username);
        logger.debug("Cliente '{}' eliminado de LocalClientRegistry", username);
    }

    /**
     * Entrega un mensaje JSON directamente al socket de un cliente local.
     *
     * @param username Nombre del cliente destino
     * @param jsonMessage Mensaje JSON a entregar
     * @return true si se entregó correctamente, false si el cliente no está registrado o el stream falló
     */
    public boolean deliver(String username, String jsonMessage) {
        OutputStream out = clientStreams.get(username);
        if (out == null) {
            logger.warn("Intento de entrega directa a '{}' pero no está registrado localmente", username);
            return false;
        }
        try {
            byte[] bytes = (jsonMessage + "\n").getBytes(StandardCharsets.UTF_8);
            out.write(bytes);
            out.flush();
            logger.debug("Mensaje entregado directamente a '{}'", username);
            return true;
        } catch (Exception e) {
            logger.error("Error entregando mensaje directamente a '{}': {}", username, e.getMessage());
            clientStreams.remove(username); // Stream muerto, limpiar
            return false;
        }
    }

    /**
     * Indica si un cliente está actualmente registrado (conectado localmente).
     */
    public boolean isRegistered(String username) {
        return clientStreams.containsKey(username);
    }

    /**
     * Número de clientes locales actualmente registrados.
     */
    public int size() {
        return clientStreams.size();
    }
}

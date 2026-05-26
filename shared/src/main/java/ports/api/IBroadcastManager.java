package ports.api;

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Interfaz para el gestor de difusión de mensajes a múltiples clientes.
 */
public interface IBroadcastManager {
    void addStream(OutputStream clientOut);
    void removeStream(OutputStream clientOut);
    void broadcast(String message);
    void broadcastLocalOnly(String message);
    void setFederatedHook(Consumer<String> hook);
}

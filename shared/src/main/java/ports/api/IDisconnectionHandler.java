package ports.api;

import java.io.OutputStream;

/**
 * Interfaz que abstrae la lógica de desconexión física de un cliente.
 */
public interface IDisconnectionHandler {
    void procesarDesconexion(String rawClientIp, OutputStream out);
}

package com.universidad.messaging.server.protocolo.api.broadcast;

import java.io.OutputStream;

/**
 * Interfaz para el gestor de difusión de mensajes a múltiples clientes.
 */
public interface IBroadcastManager {
    void addStream(OutputStream clientOut);
    void removeStream(OutputStream clientOut);
    void broadcast(String message);
}

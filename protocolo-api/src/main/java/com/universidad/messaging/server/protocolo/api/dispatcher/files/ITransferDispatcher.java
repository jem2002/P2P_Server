package com.universidad.messaging.server.protocolo.api.dispatcher.files;

import ports.api.TransferTicket;

/**
 * Interfaz para el gestor de transferencias (Dependency Inversion).
 */
public interface ITransferDispatcher {
    TransferTicket validarYConsumirTicket(String token);
    void registrarTicket(TransferTicket ticket);
}

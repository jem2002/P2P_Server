package com.universidad.messaging.server.protocolo.api.dispatcher.files;

import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;

/**
 * Interfaz para el gestor de transferencias (Dependency Inversion).
 */
public interface ITransferDispatcher {
    TransferTicket validarYConsumirTicket(String token);
    void registrarTicket(TransferTicket ticket);
}

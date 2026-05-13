package com.universidad.messaging.server.business;

public interface MessageProcessor {
    /**
     * Procesa un mensaje de un cliente y retorna una respuesta.
     * @param clientAddress La dirección del cliente (IP:Port)
     * @param payload El mensaje en crudo (JSON)
     * @return Respuesta en crudo (JSON)
     */
    String processMessage(String clientAddress, String payload);

    /**
     * Genera un mensaje genérico de rechazo por sobrecarga.
     * @return Respuesta en crudo (JSON)
     */
    String generateOverloadRejection();
}

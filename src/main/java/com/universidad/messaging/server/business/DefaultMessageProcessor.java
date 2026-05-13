package com.universidad.messaging.server.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMessageProcessor implements MessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageProcessor.class);

    @Override
    public String processMessage(String clientAddress, String payload) {
        logger.debug("Procesando mensaje de {}: {}", clientAddress, payload);
        
        // Aquí iría la lógica real de dominio: parseo de JSON, llamadas a repositorios, etc.
        
        return "{\"status\": \"ok\", \"message\": \"Mensaje recibido.\"}";
    }

    @Override
    public String generateOverloadRejection() {
        return "{\"status\": \"error\", \"message\": \"Servidor sobrecargado. Intente más tarde.\"}";
    }
}

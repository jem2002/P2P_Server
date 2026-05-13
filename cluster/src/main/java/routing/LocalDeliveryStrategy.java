package routing;

import MessageParser.BroadcastManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estrategia de entrega local: envía el mensaje solo a los clientes
 * conectados a este servidor (usa BroadcastManager existente).
 *
 * Principio aplicado: Adapter (GoF) — adapta el BroadcastManager existente
 * a la interfaz MessageRoutingStrategy.
 */
public class LocalDeliveryStrategy implements MessageRoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LocalDeliveryStrategy.class);
    private final BroadcastManager localBroadcast;

    public LocalDeliveryStrategy(BroadcastManager localBroadcast) {
        this.localBroadcast = localBroadcast;
    }

    @Override
    public void deliver(String jsonMessage, String targetUsername) {
        logger.debug("Entrega LOCAL para usuario '{}'", targetUsername);
        localBroadcast.broadcast(jsonMessage);
    }
}

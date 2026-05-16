package routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Estrategia de entrega local: envía el mensaje solo a los clientes
 * conectados a este servidor.
 *
 * Principio aplicado: Adapter (GoF) + DIP — usa Consumer<String> para
 * desacoplarse del BroadcastManager concreto (evita ciclo cluster → protocolo).
 */
public class LocalDeliveryStrategy implements MessageRoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(LocalDeliveryStrategy.class);
    private final Consumer<String> localBroadcast;

    public LocalDeliveryStrategy(Consumer<String> localBroadcast) {
        this.localBroadcast = localBroadcast;
    }

    @Override
    public void deliver(String jsonMessage, String targetUsername, String fromUser, String rawContent) {
        logger.debug("Entrega LOCAL para usuario '{}'", targetUsername);
        localBroadcast.accept(jsonMessage);
    }
}

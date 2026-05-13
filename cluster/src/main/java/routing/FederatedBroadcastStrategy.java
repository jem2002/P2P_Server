package routing;

import MessageParser.BroadcastManager;
import communication.PeerConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estrategia de broadcast federado: envía el mensaje a todos los clientes
 * locales Y a todos los servidores peer (que a su vez lo entregan a sus clientes).
 *
 * Principio aplicado: Composite (GoF) — combina dos estrategias (local + remote)
 * en una sola operación de broadcast.
 */
public class FederatedBroadcastStrategy implements MessageRoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FederatedBroadcastStrategy.class);
    private final BroadcastManager localBroadcast;
    private final PeerConnectionPool peerPool;

    public FederatedBroadcastStrategy(BroadcastManager localBroadcast, PeerConnectionPool peerPool) {
        this.localBroadcast = localBroadcast;
        this.peerPool = peerPool;
    }

    /**
     * Broadcast federado: local + todos los peers.
     *
     * @param jsonMessage     Mensaje a enviar
     * @param targetUsername  Ignorado — se envía a todos
     */
    @Override
    public void deliver(String jsonMessage, String targetUsername) {
        // 1. Broadcast local a todos los clientes de este servidor
        localBroadcast.broadcast(jsonMessage);

        // 2. Enviar a todos los peers para que lo retransmitan a sus clientes
        peerPool.broadcastToPeers(jsonMessage);

        logger.info("Broadcast federado completado (local + {} peers)", peerPool.activeCount());
    }
}

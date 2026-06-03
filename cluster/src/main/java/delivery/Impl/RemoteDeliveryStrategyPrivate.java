package delivery.Impl;

import util.InterServerProtocol;
import communication.PeerConnectionPool;
import delivery.PrivateMessageDeliveryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import topology.RoutingTable;

/**
 * Estrategia de entrega remota: reenvía el mensaje al servidor peer
 * que hospeda al cliente destino.
 *
 * Principio aplicado: Strategy (GoF) + Indirection (GRASP) — desacopla
 * el envío de mensajes del conocimiento de la topología.
 */
public class RemoteDeliveryStrategyPrivate implements PrivateMessageDeliveryStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RemoteDeliveryStrategyPrivate.class);
    private final PeerConnectionPool peerPool;
    private final RoutingTable routingTable;

    public RemoteDeliveryStrategyPrivate(PeerConnectionPool peerPool, RoutingTable routingTable) {
        this.peerPool = peerPool;
        this.routingTable = routingTable;
    }

    @Override
    public boolean deliver(String jsonMessage, String targetUsername, String fromUser, String rawContent, String clientIp) throws Exception {
        String nodeId = routingTable.resolveNode(targetUsername);
        if (nodeId == null) {
            logger.warn("No se encontró nodo para el usuario '{}' en la tabla de enrutamiento",
                    targetUsername);
            return false;
        }

        String routeMessage = InterServerProtocol.buildRouteMessage(targetUsername, jsonMessage, fromUser, rawContent, clientIp);
        peerPool.sendToPeer(nodeId, routeMessage);
        logger.info("Mensaje reenviado a peer '{}' para usuario '{}'", nodeId, targetUsername);
        return true;
    }
}

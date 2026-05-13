package replication;

import communication.PeerConnectionPool;
import discovery.MembershipList;
import events.NetworkEventListener;
import events.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestor de replicación que propaga eventos de mutación a todos los
 * peers vivos de la red y procesa eventos entrantes de otros nodos.
 *
 * Principios aplicados:
 *   - SRP: solo coordina la propagación/recepción. La lógica de aplicar
 *     el evento al dominio se delega a callbacks específicos.
 *   - Observer: implementa NetworkEventListener para reaccionar a cambios de topología.
 */
public class ReplicationManager implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationManager.class);
    private static final int MAX_EVENT_CACHE = 10000;

    private final MembershipList membership;
    private final PeerConnectionPool peerPool;
    private final EventDeduplicator deduplicator;
    private final String localNodeId;

    /** Callback opcional para aplicar eventos replicados al dominio local. */
    private ReplicationEventHandler eventHandler;

    public ReplicationManager(String localNodeId, MembershipList membership,
                              PeerConnectionPool peerPool) {
        this.localNodeId = localNodeId;
        this.membership = membership;
        this.peerPool = peerPool;
        this.deduplicator = new EventDeduplicator(MAX_EVENT_CACHE);
    }

    /**
     * Registra el handler que aplica eventos replicados al dominio local.
     */
    public void setEventHandler(ReplicationEventHandler handler) {
        this.eventHandler = handler;
    }

    /**
     * Propaga un evento de mutación local a todos los peers vivos.
     * Este método se invoca cuando ocurre un cambio local (nuevo mensaje,
     * nuevo documento, etc.) que debe replicarse.
     */
    public void propagate(ReplicationEvent event) {
        // Marcar como ya visto localmente para no reprocesarlo
        deduplicator.tryAccept(event.getEventId());

        String json = event.toJson();
        String wrappedMessage = "{\"action\":\"PEER_REPLICATE\",\"payload\":" + json + "}";

        int sent = 0;
        for (NodeInfo peer : membership.getAliveNodes()) {
            try {
                peerPool.sendToPeer(peer.getNodeId(), wrappedMessage);
                sent++;
            } catch (Exception e) {
                logger.warn("No se pudo replicar evento a {}: {}", peer.getNodeId(), e.getMessage());
            }
        }

        logger.info("Evento {} propagado a {}/{} peers",
                event.getEventType(), sent, membership.aliveCount());
    }

    /**
     * Procesa un evento de replicación recibido de otro nodo.
     * Aplica deduplicación y, si es nuevo, delega al handler de dominio
     * y re-propaga a otros peers (Gossip propagation).
     */
    public void handleIncoming(ReplicationEvent event) {
        // Deduplicación: ¿ya procesamos este evento?
        if (!deduplicator.tryAccept(event.getEventId())) {
            logger.debug("Evento duplicado ignorado: {}", event.getEventId());
            return;
        }

        logger.info("Evento de replicación recibido: {} desde {}", 
                event.getEventType(), event.getSourceNodeId());

        // Aplicar al dominio local si hay handler
        if (eventHandler != null) {
            try {
                eventHandler.apply(event);
            } catch (Exception e) {
                logger.error("Error aplicando evento replicado: {}", event, e);
            }
        }

        // Re-propagar a otros peers (Gossip fan-out)
        // Solo re-propagar si no somos el origen
        if (!localNodeId.equals(event.getSourceNodeId())) {
            rePropagateToOtherPeers(event);
        }
    }

    /**
     * Re-propaga un evento a peers que no sean el origen.
     */
    private void rePropagateToOtherPeers(ReplicationEvent event) {
        String json = event.toJson();
        String wrappedMessage = "{\"action\":\"PEER_REPLICATE\",\"payload\":" + json + "}";

        for (NodeInfo peer : membership.getAliveNodes()) {
            if (!peer.getNodeId().equals(event.getSourceNodeId())) {
                try {
                    peerPool.sendToPeer(peer.getNodeId(), wrappedMessage);
                } catch (Exception e) {
                    logger.trace("No se pudo re-propagar a {}", peer.getNodeId());
                }
            }
        }
    }

    // ============ NetworkEventListener ============

    @Override
    public void onNodeJoined(NodeInfo node) {
        logger.info("Nuevo peer detectado por ReplicationManager: {}", node);
    }

    @Override
    public void onNodeLeft(NodeInfo node) {
        logger.warn("Peer perdido en ReplicationManager: {}", node);
    }

    /**
     * Interfaz funcional para aplicar eventos replicados al dominio local.
     */
    @FunctionalInterface
    public interface ReplicationEventHandler {
        void apply(ReplicationEvent event) throws Exception;
    }
}

package service;

import com.universidad.messaging.server.gestion.cluster.api.IReplicationManager;
import com.universidad.messaging.server.shared.events.ReplicationEvent;
import communication.PeerConnectionPool;
import discovery.MembershipList;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

/**
 * Gestor de replicación que propaga eventos de mutación a todos los
 * peers vivos de la red y procesa eventos entrantes de otros nodos.
 *
 * Principios aplicados:
 *   - SRP: solo coordina la propagación/recepción. La lógica de aplicar
 *     el evento al dominio se delega a callbacks específicos.
 *   - Observer: implementa NetworkEventListener para reaccionar a cambios de topología.
 */
public class ReplicationManager implements IReplicationManager {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationManager.class);

    private final MembershipList membership;
    private final PeerConnectionPool peerPool;
    private final EventDeduplicator deduplicator;
    private final String localNodeId;

    /** Callback opcional para aplicar eventos replicados al dominio local. */
    private IReplicationEventHandler eventHandler;

    public ReplicationManager(String localNodeId, MembershipList membership,
                              PeerConnectionPool peerPool, EventDeduplicator deduplicator) {
        this.localNodeId = localNodeId;
        this.membership = membership;
        this.peerPool = peerPool;
        this.deduplicator = deduplicator;
    }


    public void propagate(ReplicationEvent event) {

        deduplicator.tryAccept(event.getEventId());

        String json = event.toJson();
        String wrappedMessage = "{\"action\":\"PEER_REPLICATE\",\"payload\":" + json + "}";

        int sent = 0;
        for (RemoteNodeInfo peer : membership.getAliveNodes()) {
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

}

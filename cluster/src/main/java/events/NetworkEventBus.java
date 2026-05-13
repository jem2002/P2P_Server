package events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bus de eventos del cluster. Despacha eventos de red a todos los
 * {@link NetworkEventListener} registrados.
 *
 * Principio aplicado: Mediator (GoF) — centraliza la comunicación
 * entre componentes del cluster sin que se conozcan entre sí.
 *
 * Thread-safe: usa CopyOnWriteArrayList para permitir suscripción/desuscripción
 * concurrente sin bloquear la publicación de eventos.
 */
public class NetworkEventBus {

    private static final Logger logger = LoggerFactory.getLogger(NetworkEventBus.class);
    private final List<NetworkEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(NetworkEventListener listener) {
        listeners.add(listener);
        logger.debug("Listener suscrito al EventBus: {}", listener.getClass().getSimpleName());
    }

    public void unsubscribe(NetworkEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Publica un evento a todos los listeners registrados.
     * Los errores en un listener no detienen la propagación a los demás.
     */
    public void publish(ClusterEvent event, NodeInfo node) {
        logger.info("Evento de cluster: {} — Nodo: {}", event, node);

        for (NetworkEventListener listener : listeners) {
            try {
                switch (event) {
                    case NODE_JOINED:
                        listener.onNodeJoined(node);
                        break;
                    case NODE_LEFT:
                        listener.onNodeLeft(node);
                        break;
                    case NODE_SUSPECTED:
                        listener.onNodeSuspected(node);
                        break;
                    case TOPOLOGY_CHANGED:
                        listener.onTopologyChanged();
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                logger.error("Error en listener {} procesando evento {}",
                        listener.getClass().getSimpleName(), event, e);
            }
        }
    }
}

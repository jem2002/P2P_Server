package events.Impl;

import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import topology.RoutingTable;
import util.InterServerProtocol;

/**
 * Escucha eventos de la red y orquesta la conexión/desconexión
 * utilizando el pool de conexiones.
 */
public class NodeConnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeConnector.class);


    private final PeerConnectionPool peerConnectionPool;
    private final String identityNodeId;
    private final RoutingTable routingTable;

    public NodeConnector(PeerConnectionPool peerConnectionPool, String identityNodeId, RoutingTable routingTable){
        this.peerConnectionPool = peerConnectionPool;
        this.identityNodeId = identityNodeId;
        this.routingTable = routingTable;
    }

    @Override
    public void onNodeJoined(RemoteNodeInfo node) {
        boolean newConnection = peerConnectionPool.connectToPeer(node);

        if(newConnection){
            synchronizeNewNode(node);
        }

    }


    private void synchronizeNewNode(RemoteNodeInfo node) {

        // Ejecutamos en un hilo aparte para dar tiempo a que el pool abra la conexión TCP
        // y para no bloquear el hilo principal/EventBus que notificó el evento.
        new Thread(() -> {
            int maxRetries = 5;
            long delay = 200; // ms iniciales

            for (int i = 0; i < maxRetries; i++) {
                try {
                    // Espera inicial/entre reintentos
                    Thread.sleep(delay);

                    String syncMsg = InterServerProtocol
                            .buildSyncMessage(identityNodeId, routingTable);
                    peerConnectionPool.sendToPeer(node.getNodeId(), syncMsg);

                    logger.info("RoutingTable enviada exitosamente a '{}' (bootstrap sync en intento {})",
                            node.getNodeId(), (i + 1));
                    return; // ¡Éxito! Salimos del hilo inmediatamente.

                } catch (InterruptedException ie) {
                    logger.error("La sincronización con '{}' fue interrumpida abruptamente", node.getNodeId(), ie);
                    Thread.currentThread().interrupt(); // Restablecer el estado de interrupción
                    return;
                } catch (Exception e) {
                    if (i == maxRetries - 1) {
                        // Último intento fallido: Log de error crítico
                        logger.error("No se pudo enviar bootstrap sync a '{}' tras {} intentos. Error: {}",
                                node.getNodeId(), maxRetries, e.getMessage(), e);
                    } else {
                        // Intentos intermedios: Log de advertencia y aplicamos backoff exponencial
                        logger.warn("Fallo en intento {}/{} enviando sync a '{}'. Reintentando en {} ms...",
                                (i + 1), maxRetries, node.getNodeId(), delay);
                    }

                    delay *= 2; // Duplica el tiempo de espera para el siguiente intento (200ms -> 400ms -> 800ms...)
                }
            }
        }, "BootstrapSync-" + node.getNodeId()).start();

    }


}
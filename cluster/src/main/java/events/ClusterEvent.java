package events;

/**
 * Tipos de evento que ocurren en la red de servidores P2P.
 */
public enum ClusterEvent {

    /** Un nuevo nodo se ha unido a la red. */
    NODE_JOINED,

    /** Un nodo ha abandonado la red (desconexión detectada). */
    NODE_LEFT,

    /** Un nodo no ha respondido en el umbral de sospecha. */
    NODE_SUSPECTED,

    /** La tabla de enrutamiento ha cambiado. */
    TOPOLOGY_CHANGED,

    /** Se ha recibido un evento de replicación de datos. */
    DATA_REPLICATED
}

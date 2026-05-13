package discovery;

/**
 * Estado de un nodo en el protocolo de membresía (Gossip).
 *
 * Transiciones válidas:
 *   JOINING → ALIVE → SUSPECTED → DOWN
 *                ↑________________________|  (si recibe heartbeat de nuevo)
 */
public enum NodeState {

    /** Nodo recién detectado, aún no confirmado. */
    JOINING,

    /** Nodo activo y respondiendo heartbeats normalmente. */
    ALIVE,

    /** Nodo no ha respondido en el umbral de sospecha (3 heartbeats perdidos). */
    SUSPECTED,

    /** Nodo confirmado como caído (timeout completo expirado). */
    DOWN
}

package events;

/**
 * Contrato Observer para componentes que reaccionan a cambios en la red P2P.
 *
 * Principio aplicado: Observer (GoF) — desacopla la detección de eventos
 * de la reacción a los mismos. Cada componente implementa solo los callbacks
 * que necesita (los métodos default permiten implementación selectiva).
 */
public interface NetworkEventListener {

    /**
     * Invocado cuando un nuevo nodo se une a la red.
     */
    default void onNodeJoined(NodeInfo node) {}

    /**
     * Invocado cuando un nodo abandona la red (confirmado como DOWN).
     */
    default void onNodeLeft(NodeInfo node) {}

    /**
     * Invocado cuando un nodo se sospecha caído (heartbeats perdidos).
     */
    default void onNodeSuspected(NodeInfo node) {}

    /**
     * Invocado cuando la topología de la red ha cambiado.
     */
    default void onTopologyChanged() {}
}

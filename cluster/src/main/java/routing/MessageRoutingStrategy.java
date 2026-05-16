package routing;

/**
 * Estrategia de entrega de mensajes en la red distribuida.
 *
 * Principio aplicado: Strategy (GoF) — permite intercambiar el algoritmo
 * de entrega sin modificar el código que lo invoca.
 */
public interface MessageRoutingStrategy {

    /**
     * Entrega un mensaje a un usuario destino específico.
     *
     * @param jsonMessage  Mensaje JSON a entregar
     * @param targetUsername  Usuario destino
     * @throws Exception si la entrega falla
     */
    void deliver(String jsonMessage, String targetUsername, String fromUser, String rawContent, String clientIp) throws Exception;
}

package MessageParser;

/**
 * Hook para extender el BroadcastManager con broadcast federado (P2P).
 * Se inyecta opcionalmente cuando el cluster está habilitado.
 *
 * Principio aplicado: OCP — el BroadcastManager no se modifica,
 * simplemente acepta un hook opcional que extiende su comportamiento.
 */
@FunctionalInterface
public interface FederatedBroadcastHook {

    /**
     * Envía un mensaje a todos los servidores peer para que lo
     * retransmitan a sus clientes locales.
     *
     * @param jsonMessage Mensaje JSON a retransmitir
     */
    void broadcastToPeers(String jsonMessage);
}

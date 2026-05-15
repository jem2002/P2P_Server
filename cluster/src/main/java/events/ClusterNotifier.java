package events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Notificador de eventos del cluster hacia los clientes locales.
 *
 * Implementa NetworkEventListener para recibir eventos del bus de red
 * (NODE_JOINED, NODE_LEFT, NODE_SUSPECTED) y retransmitirlos en formato JSON
 * a todos los clientes conectados localmente.
 *
 * Usa Consumer<String> en lugar de BroadcastManager concreto para evitar
 * la dependencia circular cluster → protocolo.
 *
 * Requerimiento cumplido: "Los servidores deberán informar el momento en que
 * se une o se desconecta algún servidor."
 *
 * Principio aplicado: Observer (GoF) + DIP — depende de una abstracción
 * funcional, no de la clase concreta de broadcast.
 */
public class ClusterNotifier implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ClusterNotifier.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Callback que publica mensajes a todos los clientes locales (ej. BroadcastManager::broadcast). */
    private final Consumer<String> broadcaster;

    public ClusterNotifier(Consumer<String> broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void onNodeJoined(NodeInfo node) {
        String json = buildServerEvent("SERVER_JOINED", node, "El servidor se ha unido a la red");
        broadcaster.accept(json);
        logger.info("Notificado a clientes: servidor '{}' JOINED", node.getNodeId());
    }

    @Override
    public void onNodeLeft(NodeInfo node) {
        String json = buildServerEvent("SERVER_LEFT", node, "El servidor se ha desconectado de la red");
        broadcaster.accept(json);
        logger.info("Notificado a clientes: servidor '{}' LEFT", node.getNodeId());
    }

    @Override
    public void onNodeSuspected(NodeInfo node) {
        String json = buildServerEvent("SERVER_SUSPECTED", node, "El servidor no responde (posiblemente caído)");
        broadcaster.accept(json);
        logger.warn("Notificado a clientes: servidor '{}' SUSPECTED", node.getNodeId());
    }

    /**
     * Construye el mensaje JSON de notificación de evento de servidor.
     * Formato: {"action":"SERVER_JOINED","payload":{"nodeId":"...","host":"...","clusterPort":9090,"message":"..."}}
     */
    private String buildServerEvent(String action, NodeInfo node, String message) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("action", action);

            ObjectNode payload = mapper.createObjectNode();
            payload.put("nodeId", node.getNodeId());
            payload.put("host", node.getHost());
            payload.put("clusterPort", node.getClusterPort());
            payload.put("message", message);

            root.set("payload", payload);
            return root.toString();
        } catch (Exception e) {
            logger.error("Error construyendo notificación de evento de cluster", e);
            return "{\"action\":\"" + action + "\",\"payload\":{\"nodeId\":\"" + node.getNodeId() + "\"}}";
        }
    }
}

package topology;

import events.NetworkEventListener;
import events.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tabla de enrutamiento que mapea cada cliente (username) al nodeId del
 * servidor que lo hospeda.
 *
 * Permite resolver si un mensaje debe entregarse localmente o reenviarse
 * a un servidor peer.
 *
 * Principio aplicado: Information Expert (GRASP) — esta clase es la
 * autoridad sobre la ubicación de cada cliente en la red.
 *
 * Implementa NetworkEventListener para limpiar entradas cuando un nodo cae.
 */
public class RoutingTable implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(RoutingTable.class);

    private final String localNodeId;
    private final ConcurrentHashMap<String, String> clientToNode = new ConcurrentHashMap<>();

    public RoutingTable(String localNodeId) {
        this.localNodeId = localNodeId;
    }

    /**
     * Registra un cliente como local (conectado a este servidor).
     */
    public void registerLocalClient(String username) {
        clientToNode.put(username, localNodeId);
        logger.debug("Cliente '{}' registrado en nodo local '{}'", username, localNodeId);
    }

    /**
     * Registra un cliente como remoto (conectado a otro servidor).
     */
    public void registerRemoteClient(String username, String nodeId) {
        clientToNode.put(username, nodeId);
        logger.debug("Cliente '{}' registrado en nodo remoto '{}'", username, nodeId);
    }

    /**
     * Elimina un cliente de la tabla de enrutamiento.
     */
    public void unregisterClient(String username) {
        clientToNode.remove(username);
    }

    /**
     * Resuelve en qué nodo se encuentra un cliente.
     *
     * @return nodeId del servidor, o null si el cliente no está en la tabla.
     */
    public String resolveNode(String username) {
        return clientToNode.get(username);
    }

    /**
     * Indica si el cliente está conectado a este servidor.
     */
    public boolean isLocal(String username) {
        return localNodeId.equals(clientToNode.get(username));
    }

    /**
     * Sincroniza la tabla con datos recibidos de un peer.
     * Solo se actualizan entradas de clientes que pertenecen al nodo fuente.
     */
    public void syncFromPeer(String sourceNodeId, Map<String, String> peerTable) {
        for (Map.Entry<String, String> entry : peerTable.entrySet()) {
            if (sourceNodeId.equals(entry.getValue())) {
                clientToNode.put(entry.getKey(), entry.getValue());
            }
        }
        logger.info("Tabla de enrutamiento sincronizada con nodo '{}'", sourceNodeId);
    }

    /**
     * Retorna una copia del snapshot actual de la tabla (para enviar a peers).
     */
    public Map<String, String> getSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(clientToNode));
    }

    /**
     * Retorna solo los clientes locales de este nodo.
     */
    public List<String> getLocalClients() {
        List<String> locals = new ArrayList<>();
        for (Map.Entry<String, String> entry : clientToNode.entrySet()) {
            if (localNodeId.equals(entry.getValue())) {
                locals.add(entry.getKey());
            }
        }
        return locals;
    }

    /**
     * Imprime la tabla de enrutamiento en consola (para administración).
     */
    public void printTable() {
        System.out.println("\n--- TABLA DE ENRUTAMIENTO ---");
        System.out.printf("%-25s | %-15s | %-10s%n", "CLIENTE", "NODO", "UBICACIÓN");
        System.out.println("-------------------------------------------------------------");
        for (Map.Entry<String, String> entry : clientToNode.entrySet()) {
            String location = localNodeId.equals(entry.getValue()) ? "LOCAL" : "REMOTO";
            System.out.printf("%-25s | %-15s | %-10s%n",
                    entry.getKey(), entry.getValue(), location);
        }
        System.out.println("-------------------------------------------------------------");
        System.out.printf("Total: %d clientes (%d locales)%n",
                clientToNode.size(), getLocalClients().size());
    }

    // ============ NetworkEventListener ============

    /**
     * Cuando un nodo cae, eliminamos todos sus clientes de la tabla.
     */
    @Override
    public void onNodeLeft(NodeInfo node) {
        String downNodeId = node.getNodeId();
        int removed = 0;
        Iterator<Map.Entry<String, String>> it = clientToNode.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (downNodeId.equals(entry.getValue())) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("Eliminados {} clientes de la tabla — nodo caído: {}", removed, downNodeId);
        }
    }
}

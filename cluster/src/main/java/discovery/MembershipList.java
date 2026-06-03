package discovery;

import models.RemoteNodeInfo;
import models.enums.NodeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lista de membresía thread-safe que mantiene el registro de todos los nodos
 * conocidos en la red P2P, junto con su estado actual.
 *
 * Principio aplicado: Information Expert (GRASP) — esta clase es la
 * autoridad central sobre el estado de membresía de la red.
 */
public class MembershipList {

    private static final Logger logger = LoggerFactory.getLogger(MembershipList.class);
    private final ConcurrentHashMap<String, MemberEntry> members = new ConcurrentHashMap<>();

    /**
     * Añade o actualiza la entrada de un nodo. Refresca el heartbeat (heartbeat DIRECTO).
     *
     * @return true si el nodo es nuevo (recién descubierto), false si ya existía.
     */
    public boolean addOrUpdate(RemoteNodeInfo remoteNodeInfo) {
        MemberEntry existing = members.get(remoteNodeInfo.getNodeId());

        if (existing != null) {
            NodeState previousState = existing.getState();
            existing.refreshHeartbeat();

            if (previousState == NodeState.SUSPECTED || previousState == NodeState.DOWN) {
                logger.info("Nodo {} ha vuelto a estar ALIVE (estado previo: {})",
                        remoteNodeInfo.getNodeId(), previousState);
                return true; // Tratarlo como reingreso
            }
            return false;
        }

        MemberEntry newEntry = new MemberEntry(remoteNodeInfo);
        newEntry.refreshHeartbeat();
        members.put(remoteNodeInfo.getNodeId(), newEntry);
        logger.info("Nuevo nodo descubierto: {}", remoteNodeInfo);
        return true;
    }

    /**
     * Registra un nodo SOLO si no se conocía previamente (gossip indirecto).
     * NO refresca el heartbeat si el nodo ya existe: su timer sigue corriendo
     * y será marcado SUSPECTED/DOWN si no manda heartbeats directos.
     *
     * @return true si el nodo era completamente desconocido.
     */
    public boolean addIfAbsent(RemoteNodeInfo remoteNodeInfo) {
        if (members.containsKey(remoteNodeInfo.getNodeId())) {
            return false; // ya lo conocemos — no tocar su timer
        }
        MemberEntry newEntry = new MemberEntry(remoteNodeInfo);
        // Iniciar su timer desde ahora para darle tiempo de mandar su propio heartbeat
        members.put(remoteNodeInfo.getNodeId(), newEntry);
        logger.info("Nodo descubierto via gossip (indirecto): {}", remoteNodeInfo);
        return true;
    }

    /**
     * Marca un nodo como sospechoso.
     *
     * @return true si el estado cambió (no era ya SUSPECTED).
     */
    public boolean markSuspected(String nodeId) {
        MemberEntry entry = members.get(nodeId);
        if (entry != null && entry.getState() == NodeState.ALIVE) {
            entry.setState(NodeState.SUSPECTED);
            logger.warn("Nodo {} marcado como SUSPECTED", nodeId);
            return true;
        }
        return false;
    }

    /**
     * Marca un nodo como caído confirmado.
     *
     * @return true si el estado cambió (no era ya DOWN).
     */
    public boolean markDown(String nodeId) {
        MemberEntry entry = members.get(nodeId);
        if (entry != null && entry.getState() != NodeState.DOWN) {
            entry.setState(NodeState.DOWN);
            logger.error("Nodo {} marcado como DOWN", nodeId);
            return true;
        }
        return false;
    }

    /**
     * Elimina un nodo de la lista de membresía.
     */
    public void remove(String nodeId) {
        members.remove(nodeId);
    }

    /**
     * Retorna la lista de nodos actualmente vivos (ALIVE).
     */
    public List<RemoteNodeInfo> getAliveNodes() {
        List<RemoteNodeInfo> alive = new ArrayList<>();
        for (MemberEntry entry : members.values()) {
            if (entry.getState() == NodeState.ALIVE) {
                alive.add(entry.getNodeInfo());
            }
        }
        return Collections.unmodifiableList(alive);
    }

    /**
     * Retorna todos los nodos que no están caídos (ALIVE + JOINING + SUSPECTED).
     * Se usa en sendHeartbeats para seguir sondeando nodos en estados intermedios
     * y darles la oportunidad de responder con su propio heartbeat directo,
     * lo que los promovería a ALIVE.
     */
    public List<RemoteNodeInfo> getNonDownNodes() {
        List<RemoteNodeInfo> reachable = new ArrayList<>();
        for (MemberEntry entry : members.values()) {
            if (entry.getState() != NodeState.DOWN) {
                reachable.add(entry.getNodeInfo());
            }
        }
        return Collections.unmodifiableList(reachable);
    }

    /**
     * Retorna todas las entradas de membresía (para diagnóstico/monitoreo).
     */
    public List<MemberEntry> getAllEntries() {
        return Collections.unmodifiableList(new ArrayList<>(members.values()));
    }

    /**
     * Obtiene la entrada de un nodo específico.
     */
    public MemberEntry getEntry(String nodeId) {
        return members.get(nodeId);
    }

    /**
     * Número total de nodos conocidos (cualquier estado).
     */
    public int size() {
        return members.size();
    }

    /**
     * Número de nodos actualmente vivos.
     */
    public int aliveCount() {
        int count = 0;
        for (MemberEntry entry : members.values()) {
            if (entry.getState() == NodeState.ALIVE) {
                count++;
            }
        }
        return count;
    }
}

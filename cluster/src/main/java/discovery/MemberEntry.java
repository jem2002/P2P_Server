package discovery;

import events.NodeInfo;

/**
 * Entrada en la lista de membresía para un nodo remoto.
 * Almacena el último heartbeat recibido y el estado actual.
 *
 * Principio aplicado: Information Expert (GRASP) — esta clase sabe
 * cuándo un nodo debe marcarse como sospechoso o caído.
 */
public class MemberEntry {

    private final NodeInfo nodeInfo;
    private volatile NodeState state;
    private volatile long lastHeartbeatMs;

    public MemberEntry(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        this.state = NodeState.JOINING;
        this.lastHeartbeatMs = System.currentTimeMillis();
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    public NodeState getState() {
        return state;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public long getLastHeartbeatMs() {
        return lastHeartbeatMs;
    }

    /**
     * Actualiza el timestamp del último heartbeat y marca el nodo como ALIVE.
     */
    public void refreshHeartbeat() {
        this.lastHeartbeatMs = System.currentTimeMillis();
        this.state = NodeState.ALIVE;
    }

    /**
     * Calcula cuántos milisegundos han pasado desde el último heartbeat.
     */
    public long getTimeSinceLastHeartbeatMs() {
        return System.currentTimeMillis() - lastHeartbeatMs;
    }
}

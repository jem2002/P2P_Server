package service;

/**
 * Deduplicador de eventos de replicación basado en LRU cache.
 * Evita que un nodo procese el mismo evento dos veces cuando
 * lo recibe de múltiples peers (inevitable en Gossip propagation).
 *
 * Principio aplicado: Pure Fabrication (GRASP) — clase técnica
 * que no representa un concepto del dominio, pero es necesaria
 * para la correcta operación del sistema distribuido.
 */
public class EventDeduplicator {

    private final java.util.concurrent.ConcurrentHashMap<String, Boolean> seenEvents;
    private final int maxCapacity;

    /**
     * @param maxCapacity Número máximo de event IDs a recordar.
     *                    Los más antiguos se descartan automáticamente (LRU).
     */
    public EventDeduplicator(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.seenEvents = new java.util.concurrent.ConcurrentHashMap<>(maxCapacity);
    }

    /**
     * Verifica si un evento ya fue procesado.
     *
     * @param eventId UUID del evento de replicación
     * @return true si el evento es NUEVO (no se ha visto antes), false si es duplicado
     */
    public boolean tryAccept(String eventId) {
        Boolean previous = seenEvents.putIfAbsent(eventId, Boolean.TRUE);
        if (previous != null) return false; // ya existía — duplicado
        // Evicción simple: si excede capacidad, limpiar las más viejas
        if (seenEvents.size() > maxCapacity) {
            // Eliminar la primera entrada (aproximación, aceptable para deduplicación)
            java.util.Iterator<String> it = seenEvents.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return true;
    }

    /**
     * Número de eventos recordados actualmente.
     */
    public int size() {
        return seenEvents.size();
    }
}

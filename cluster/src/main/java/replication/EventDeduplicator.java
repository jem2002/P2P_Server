package replication;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

    private final Set<String> seenEvents;

    /**
     * @param maxCapacity Número máximo de event IDs a recordar.
     *                    Los más antiguos se descartan automáticamente (LRU).
     */
    public EventDeduplicator(int maxCapacity) {
        // LinkedHashMap con accessOrder=true y removeEldestEntry = LRU cache
        Map<String, Boolean> lruMap = Collections.synchronizedMap(
                new LinkedHashMap<String, Boolean>(maxCapacity, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                        return size() > maxCapacity;
                    }
                }
        );
        this.seenEvents = Collections.newSetFromMap(lruMap);
    }

    /**
     * Verifica si un evento ya fue procesado.
     *
     * @param eventId UUID del evento de replicación
     * @return true si el evento es NUEVO (no se ha visto antes), false si es duplicado
     */
    public boolean tryAccept(String eventId) {
        return seenEvents.add(eventId);
    }

    /**
     * Número de eventos recordados actualmente.
     */
    public int size() {
        return seenEvents.size();
    }
}

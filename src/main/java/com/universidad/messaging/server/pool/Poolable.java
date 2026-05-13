package com.universidad.messaging.server.pool;

/**
 * Contrato base para todo objeto gestionado por un {@link ObjectPool}.
 * Cada implementación debe garantizar que {@link #reset()} deja al objeto
 * en un estado limpio, listo para ser reutilizado por un nuevo consumidor
 * sin filtraciones de estado del ciclo anterior.
 *
 * <p>Forma parte del patrón Object Pool genérico del Messaging Server.</p>
 */
public interface Poolable {

    /**
     * Restablece el estado interno del objeto para su reutilización.
     * Debe liberar recursos asociados (sockets, streams, buffers) y
     * limpiar cualquier referencia al ciclo de vida anterior.
     */
    void reset();
}

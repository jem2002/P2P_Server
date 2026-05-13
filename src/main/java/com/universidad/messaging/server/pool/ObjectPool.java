package com.universidad.messaging.server.pool;

/**
 * Contrato genérico para un Object Pool reutilizable.
 * Define las operaciones fundamentales de adquisición, liberación,
 * consulta de estado y cierre del pool.
 *
 * <p>El tipo parámetro {@code T} debe extender {@link Poolable} para
 * garantizar que cada objeto gestionado pueda ser reseteado de forma segura
 * antes de su reutilización.</p>
 *
 * @param <T> tipo concreto de los objetos almacenados en el pool.
 */
public interface ObjectPool<T extends Poolable> {

    /**
     * Adquiere un objeto disponible del pool.
     * Si no hay objetos disponibles, retorna {@code null} (estrategia fail-fast, no bloqueante).
     *
     * @return una instancia reutilizable de {@code T}, o {@code null} si el pool está agotado.
     */
    T acquire();

    /**
     * Devuelve un objeto al pool después de su uso.
     * El pool invocará {@link Poolable#reset()} sobre el objeto antes de reinsertarlo,
     * garantizando un estado limpio para el próximo consumidor.
     *
     * @param obj la instancia a liberar y reciclar.
     */
    void release(T obj);

    /**
     * Retorna la cantidad de objetos actualmente disponibles (no en uso) dentro del pool.
     *
     * @return número de objetos disponibles para ser adquiridos.
     */
    int getPoolSize();

    /**
     * Retorna la cantidad de objetos actualmente en uso (adquiridos y no devueltos).
     *
     * @return número de objetos activos fuera del pool.
     */
    int getActiveCount();

    /**
     * Inicia el apagado ordenado del pool.
     * Debe liberar todos los recursos internos (hilos, conexiones) de manera segura.
     */
    void shutdown();
}

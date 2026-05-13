package pool;

public interface IConnectionPool {
    /**
     * Adquiere una conexión inactiva del pool.
     */
    PooledClientConnection acquire();

    /**
     * Limpia y devuelve la conexión al pool para ser reutilizada.
     */
    void release(PooledClientConnection connection);

    int getAvailableCount();
}
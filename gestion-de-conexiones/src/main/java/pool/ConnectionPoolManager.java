package pool;

import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IPooledClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IConnectionPool;


public class ConnectionPoolManager implements IConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPoolManager.class);
    private final ArrayBlockingQueue<IPooledClientConnection> availableConnections;

    public ConnectionPoolManager(int maxPoolSize) {
        this.availableConnections = new ArrayBlockingQueue<>(maxPoolSize);
        // Pre-calentamiento del pool (Instanciamos todo al arrancar)
        for (int i = 0; i < maxPoolSize; i++) {
            availableConnections.offer(new PooledClientConnection());
        }
        logger.info("ConnectionPoolManager inicializado con {} conexiones.", maxPoolSize);
    }

    @Override
    public IPooledClientConnection acquire() {
        IPooledClientConnection connection = availableConnections.poll();
        if (connection == null) {
            logger.warn("¡Pool de conexiones agotado!");
        }
        return connection;
    }

    @Override
    public void release(IPooledClientConnection connection) {
        if (connection != null) {
            connection.reset();
            availableConnections.offer(connection);
            logger.debug("Conexión reciclada y devuelta al pool.");
        }
    }

    @Override
    public int getAvailableCount() {
        return availableConnections.size();
    }
}
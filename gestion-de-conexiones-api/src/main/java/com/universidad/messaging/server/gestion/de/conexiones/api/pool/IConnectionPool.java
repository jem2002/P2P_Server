
package com.universidad.messaging.server.gestion.de.conexiones.api.pool;

public interface IConnectionPool {


    IPooledClientConnection acquire() throws Exception;

    void release(IPooledClientConnection connection);

    int getAvailableCount();

}

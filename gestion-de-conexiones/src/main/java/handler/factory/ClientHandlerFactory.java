package handler.factory;

import com.universidad.messaging.server.gestion.de.conexiones.api.handler.IClientHandlerFactory;
import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IConnectionPool;
import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IPooledClientConnection;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.IClientRequestDispatcher;
import handler.ClientHandler;

public class ClientHandlerFactory implements IClientHandlerFactory {

    @Override
    public Runnable create(IPooledClientConnection connection, IConnectionPool pool, IClientRequestDispatcher router, IBroadcastManager broadcastManager, String primeraLinea) {
        return new ClientHandler(connection, pool, router,
                broadcastManager, primeraLinea);
    }

}

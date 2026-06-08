package com.universidad.messaging.server.gestion.de.conexiones.api.handler;

import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IConnectionPool;
import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IPooledClientConnection;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.IClientRequestDispatcher;

public interface IClientHandlerFactory {
    Runnable create(IPooledClientConnection connection, IConnectionPool pool, IClientRequestDispatcher router,
                    IBroadcastManager broadcastManager, String primeraLinea);

}

package com.universidad.messaging.server.gestion.de.conexiones.api.executor;

public interface IThreadPoolManager {

    void execute(Runnable task);

}

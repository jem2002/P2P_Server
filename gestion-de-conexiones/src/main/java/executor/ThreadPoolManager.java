package executor;

import com.universidad.messaging.server.gestion.de.conexiones.api.executor.IThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPoolManager implements IThreadPoolManager {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);
    private final ExecutorService threadPool;

    public ThreadPoolManager(int maxThreads) {
        // Usamos un FixedThreadPool para no exceder la capacidad de la máquina
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        logger.info("ThreadPoolManager inicializado con {} hilos.", maxThreads);
    }

    @Override
    public void execute(Runnable task) {
        threadPool.execute(task);
    }

    public void shutdown() {
        logger.info("Apagando ThreadPoolManager...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
}
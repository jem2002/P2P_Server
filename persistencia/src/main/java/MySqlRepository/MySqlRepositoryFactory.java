package MySqlRepository;

import ports.spi.IAuditLogRepository;
import ports.spi.IDocumentRepository;
import ports.spi.ISessionRepository;
import ports.spi.IUserRepository;
import ports.spi.RepositoryFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MySqlRepositoryFactory implements RepositoryFactory {

    private final MySqlDao dao;
    private final ScheduledExecutorService scheduler;

    public MySqlRepositoryFactory() {
        this.dao = new MySqlDao();
        
        // Auto-inicializar la BD (si no existe) para evitar la necesidad de scripts manuales
        DatabaseInitializer.initializeSchema();
        
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Ejecución inmediata de limpieza de conexiones muertas
        this.dao.limpiarConexionesMuertas();
        
        // Tarea programada para limpiar cada hora en un hilo interno (aislado del Main)
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                this.dao.limpiarConexionesMuertas();
            } catch (Exception e) {
                // Logueado en el DAO
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public IUserRepository getUserRepository() {
        return dao;
    }

    @Override
    public IDocumentRepository getDocumentRepository() {
        return dao;
    }

    @Override
    public ISessionRepository getSessionRepository() {
        return dao;
    }

    @Override
    public IAuditLogRepository getAuditLogRepository() {
        return dao;
    }

    @Override
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}

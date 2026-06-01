package MySqlRepository;

import ports.spi.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fábrica de repositorios MySQL.
 * Refactorizada: instancia 4 DAOs separados en vez de un único MySqlDao monolítico.
 *
 * Principios aplicados:
 *   - SRP: cada DAO implementa una sola interfaz (ISP).
 *   - DIP: DatabaseConnectionManager inyectado por constructor a cada DAO.
 */
public class MySqlRepositoryFactory implements RepositoryFactory {

    private final DatabaseConnectionManager dbManager;
    private final MySqlUserDao userDao;
    private final MySqlDocumentDao documentDao;
    private final MySqlSessionDao sessionDao;
    private final MySqlAuditLogDao auditLogDao;
    private final MySqlCommentDao commentDao;
    private final ScheduledExecutorService scheduler;

    public MySqlRepositoryFactory() {
        this.dbManager = DatabaseConnectionManager.getInstance();

        // Auto-inicializar la BD (si no existe) para evitar la necesidad de scripts manuales
        DatabaseInitializer.initializeSchema();

        // Instanciar DAOs separados — cada uno recibe la dependencia por constructor (DIP)
        this.userDao = new MySqlUserDao(dbManager);
        this.documentDao = new MySqlDocumentDao(dbManager);
        this.sessionDao = new MySqlSessionDao(dbManager);
        this.auditLogDao = new MySqlAuditLogDao(dbManager);
        this.commentDao = new MySqlCommentDao(dbManager);


        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Ejecución inmediata de limpieza de conexiones muertas
        this.sessionDao.limpiarConexionesMuertas();

        // Tarea programada para limpiar cada hora en un hilo interno (aislado del Main)
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                this.sessionDao.limpiarConexionesMuertas();
            } catch (Exception e) {
                // Logueado en el DAO
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    @Override
    public IUserRepository getUserRepository() {
        return userDao;
    }

    @Override
    public IDocumentRepository getDocumentRepository() {
        return documentDao;
    }

    @Override
    public ISessionRepository getSessionRepository() {
        return sessionDao;
    }

    @Override
    public IAuditLogRepository getAuditLogRepository() {
        return auditLogDao;
    }

    @Override
    public ICommentRepository getCommentRepository(){return commentDao;}

    @Override
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}


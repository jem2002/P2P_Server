package ports.spi;

/**
 * Service Provider Interface (SPI) for Repositories.
 * Permite cargar las implementaciones de los repositorios en tiempo de ejecución
 * (mediante ServiceLoader) sin tener dependencia en tiempo de compilación con la persistencia.
 */
public interface RepositoryFactory {
    IUserRepository getUserRepository();
    IDocumentRepository getDocumentRepository();
    ISessionRepository getSessionRepository();
    IAuditLogRepository getAuditLogRepository();
    ICommentRepository getCommentRepository();
    /**
     * Mueve el mantenimiento (ej. limpieza de conexiones muertas) al SPI.
     */
    void cleanup();
}

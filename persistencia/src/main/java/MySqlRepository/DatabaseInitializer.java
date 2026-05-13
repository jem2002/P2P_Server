package MySqlRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

/**
 * Orquestador de la inicialización de tablas de la base de datos.
 * Utiliza estrictamente las credenciales de la aplicación (messaging_app).
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    /**
     * Ejecuta schema.sql usando el ConnectionPool estándar de la app.
     * Asume que la base de datos y el usuario ya fueron creados previamente (init.sql manual).
     */
    public static void initializeSchema() {
        try {
            logger.info("Aplicando esquema de tablas desde schema.sql...");

            // Obtener conexión del pool (usa el usuario messaging_app del config.properties)
            try (Connection conn = DatabaseConnectionManager.getInstance().getConnection()) {
                SqlScriptRunner.runScript(conn, "schema.sql");
            }

            logger.info("Base de datos lista: Tablas creadas/actualizadas con éxito.");
        } catch (Exception e) {
            logger.error("Error crítico al aplicar el esquema de la base de datos", e);
            throw new RuntimeException("Fallo en la inicialización del esquema", e);
        }
    }
}
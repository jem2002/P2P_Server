import MySqlRepository.DatabaseConnectionManager;
import MySqlRepository.DatabaseInitializer;
import MySqlRepository.MySqlDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal para probar el módulo de persistencia de forma aislada.
 */
public class PersistenceTester {
 /*   private static final Logger logger = LoggerFactory.getLogger(PersistenceTester.class);

    public static void main(String[] args) {
        logger.info("=== INICIANDO SMOKE TEST DE PERSISTENCIA ===");

        try {
            // PASO 1: Crear tablas desde schema.sql (usando messaging_app)
            logger.info("PASO 1: Creando estructura de tablas...");
            DatabaseInitializer.initializeSchema();

            // PASO 2: Probar lógica de negocio del DAO
            logger.info("PASO 2: Probando inserciones en MySQLDao...");
            MySqlDao dao = new MySqlDao();

            // Registramos un usuario de prueba
            long userId = dao.obtenerORegistrarUsuario("sebastian_test", "192.168.1.50");
            logger.info("Usuario registrado exitosamente con ID: {}", userId);

            // Registramos un documento de prueba
            long docId = dao.registrarDocumento(
                    "manual_tecnico.pdf",
                    1500000L, // 1.5 MB
                    ".pdf",
                    "application/pdf",
                    "FILE",
                    "/storage/original/uuid-manual.pdf",
                    userId,
                    "192.168.1.50"
            );
            logger.info("Documento (metadatos) registrado con ID: {}", docId);

            // PASO 3: Verificar listado (SELECT con JOIN)
            logger.info("PASO 3: Verificando persistencia en el listado...");
            dao.listarDocumentosDisponibles().forEach(doc -> {
                logger.info(" -> DOC ENCONTRADO: {} (Dueño: {} | Tamaño: {} bytes)",
                        doc.get("nombre"), doc.get("propietario"), doc.get("tamaño"));
            });

            // CIERRE
            DatabaseConnectionManager.getInstance().close();
            logger.info("=== TEST FINALIZADO EXITOSAMENTE ===");

        } catch (Exception e) {
            logger.error("Error durante la prueba de persistencia", e);
            System.exit(1);
        }
    }

  */
}
import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import FileStorageService.StorageManager;
import LogService.LogManager;
import MySqlRepository.DatabaseConnectionManager;
import MySqlRepository.DatabaseInitializer;
import MySqlRepository.MySqlDao;
import UserService.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Prueba de Humo (Smoke Test) para verificar la orquestación del Módulo de Servicios.
 */
public class ServiceTester {
 /*   private static final Logger logger = LoggerFactory.getLogger(ServiceTester.class);

    public static void main(String[] args) {
        logger.info("=== INICIANDO SMOKE TEST DE SERVICIOS ===");

        try {
            // 1. Preparar la Base de Datos (Reutilizamos la lógica del módulo de persistencia)
            logger.info("1. Inicializando Base de Datos...");
            DatabaseInitializer.initializeSchema();
            MySqlDao dao = new MySqlDao();

            // 2. Instanciar los Managers de Servicios
            logger.info("2. Inicializando Capa de Negocio (Managers)...");
            UserManager userManager = new UserManager(dao);
            StorageManager storageManager = new StorageManager();
            CryptoManager cryptoManager = new CryptoManager();
            LogManager logManager = new LogManager(dao);

            // El Orquestador
            DocumentManager documentManager = new DocumentManager(storageManager, cryptoManager, dao, logManager);

            // 3. Crear un usuario de prueba para tener un owner_id válido
            logger.info("3. Registrando usuario emisor...");
            long userId = userManager.conectarUsuario("admin_servicios", "10.0.0.5");
            logger.info("   -> Usuario ID: {}", userId);

            // 4. Simular un archivo recibido por la red (en memoria)
            logger.info("4. Simulando recepción de archivo por la red...");
            String contenidoSimulado = "Este es un archivo confidencial que debe ser encriptado y hasheado correctamente por el DocumentManager.";
            InputStream redStream = new ByteArrayInputStream(contenidoSimulado.getBytes(StandardCharsets.UTF_8));
            long sizeBytes = contenidoSimulado.getBytes(StandardCharsets.UTF_8).length;

            // 5. Ejecutar el flujo completo
            logger.info("5. Ejecutando procesarRecepcionDocumento()...");
            boolean exito = documentManager.procesarRecepcionDocumento(
                    redStream,
                    "secreto.txt",
                    sizeBytes,
                    ".txt",
                    "text/plain",
                    userId,
                    "10.0.0.5"
            );

            if (exito) {
                logger.info("==================================================");
                logger.info("✅ PRUEBA EXITOSA: El archivo pasó por todas las capas.");
                logger.info("- Revisa la carpeta './storage/original' para ver el texto plano.");
                logger.info("- Revisa la carpeta './storage/encrypted' para ver el binario indescifrable (.enc).");
                logger.info("- Revisa tu base de datos (tablas: documents, document_hashes, encrypted_documents, logs).");
                logger.info("==================================================");
            } else {
                logger.error("❌ PRUEBA FALLIDA: documentManager devolvió false.");
            }

        } catch (Exception e) {
            logger.error("Error crítico en la prueba de servicios", e);
        } finally {
            // Cerrar el pool de base de datos
            DatabaseConnectionManager.getInstance().close();
        }
    }

  */
}
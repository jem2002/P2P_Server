import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import EncryptionUtils.EncryptionUtils;
import EncryptionUtils.IEncryptionUtils;
import FileSystemStorage.LocalFileManager;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import MySqlRepository.MySqlDao;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import UserService.UserManager;
import api.ServerAdminAPI;
import config.ServerConfig;
import console.InteractiveConsole;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import pool.ConnectionPoolManager;
import protocolSelector.ProtocolSelector;

/**
 * Punto de entrada de la aplicación.
 * Actúa como Composition Root: instancia y conecta todas las dependencias.
 *
 * Principio aplicado: DIP — las dependencias se inyectan via constructor.
 * Las abstracciones (interfaces) se resuelven aquí en el borde del sistema.
 */
public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        configurarNivelesDeLog();

        logger.info("Arrancando Messaging Server...");

        try {
            // 1. Configuración
            ServerConfig config = new ServerConfig();

            // 2. Módulo Persistencia (Composition Root resuelve las abstracciones)
            MySqlDao dao = new MySqlDao();
            dao.limpiarConexionesMuertas();

            // 3. Módulo Servicios (inyección de interfaces segregadas)
            UserManager userManager = new UserManager(dao, dao);    // IUserRepository + ISessionRepository
            LocalFileManager fileManager = new LocalFileManager();
            IEncryptionUtils encryptionUtils = new EncryptionUtils();
            CryptoManager cryptoManager = new CryptoManager(encryptionUtils);
            LogManager logManager = new LogManager(dao);            // IAuditLogRepository
            DocumentManager documentManager = new DocumentManager(
                    fileManager, cryptoManager, dao, dao, logManager); // IDocumentRepository + IUserRepository
            BroadcastManager broadcastManager = new BroadcastManager();
            TransferManager transferManager = new TransferManager();

            // 4. Módulo Protocolo
            MainRouter router = new MainRouter(userManager, documentManager, logManager,
                    broadcastManager, transferManager);

            // 5. Módulo Gestión de Conexiones
            int maxConnections = config.getMaxConnections();
            ConnectionPoolManager pool = new ConnectionPoolManager(maxConnections);
            ThreadPoolManager threadPool = new ThreadPoolManager(maxConnections);

            // 6. Módulo Red
            ProtocolSelector networkServer = new ProtocolSelector();
            networkServer.iniciarServidor(
                    config.getProtocol(),
                    config.getPort(),
                    pool,
                    threadPool,
                    router,
                    broadcastManager,
                    transferManager,
                    documentManager,
                    logManager);

            // 7. Interfaces Expuestas y Consola Administrativa
            ServerAdminAPI adminAPI = new ServerAdminAPI(dao, dao);  // IUserRepository + IDocumentRepository
            InteractiveConsole console = new InteractiveConsole(adminAPI, networkServer);

            // Arrancamos la consola en el hilo principal
            console.run();

        } catch (Exception e) {
            logger.error("Error crítico durante el arranque del servidor.", e);
            System.exit(1);
        }
    }

    private static void configurarNivelesDeLog() {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.getLogger("com.zaxxer.hikari").setLevel(Level.WARN);
            loggerContext.getLogger("com.zaxxer.hikari.pool.HikariPool").setLevel(Level.WARN);
        } catch (Exception e) {
            // Si no es logback, ignorar
        }
    }
}
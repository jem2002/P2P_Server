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

// Imports del módulo Cluster P2P
import communication.PeerConnectionPool;
import communication.PeerMessageHandler;
import communication.PeerServer;
import discovery.GossipProtocol;
import discovery.MembershipList;
import events.NetworkEventBus;
import health.ClusterHealthService;
import identity.NodeIdentity;
import replication.ReplicationManager;
import topology.RoutingTable;

import java.util.Arrays;

/**
 * Punto de entrada de la aplicación.
 * Actúa como Composition Root: instancia y conecta todas las dependencias.
 *
 * Principio aplicado: DIP — las dependencias se inyectan via constructor.
 * Las abstracciones (interfaces) se resuelven aquí en el borde del sistema.
 *
 * Extensión P2P: cuando cluster.enabled=true, inicializa los componentes
 * de descubrimiento (Gossip), replicación, routing y monitoreo distribuido.
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

            // 7. Módulo Cluster P2P (solo si está habilitado)
            ClusterHealthService healthService = null;
            RoutingTable routingTable = null;

            if (config.isClusterEnabled()) {
                logger.info("═══════════════════════════════════════════════════");
                logger.info("  MODO CLUSTER P2P HABILITADO");
                logger.info("═══════════════════════════════════════════════════");

                // 7a. Identidad del nodo
                NodeIdentity identity = new NodeIdentity(
                        config.getNodeId(), config.getHost(),
                        config.getPort(), config.getClusterPort());
                logger.info("Identidad del nodo: {}", identity);

                // 7b. Componentes de membresía y eventos
                NetworkEventBus eventBus = new NetworkEventBus();
                MembershipList membership = new MembershipList();

                // 7c. Tabla de enrutamiento
                routingTable = new RoutingTable(identity.getNodeId());

                // 7d. Pool de conexiones a peers
                PeerConnectionPool peerPool = new PeerConnectionPool();

                // 7e. Gestor de replicación
                ReplicationManager replicator = new ReplicationManager(
                        identity.getNodeId(), membership, peerPool);

                // 7f. Servicio de salud del cluster
                healthService = new ClusterHealthService(identity, membership, peerPool);

                // 7g. Suscribir observers al bus de eventos
                eventBus.subscribe(peerPool);       // Abre/cierra conexiones TCP a peers
                eventBus.subscribe(replicator);      // Reacciona a cambios de membresía
                eventBus.subscribe(routingTable);     // Limpia clientes de nodos caídos

                // 7h. Hook de broadcast federado (OCP: extiende BroadcastManager sin modificarlo)
                broadcastManager.setFederatedHook(peerPool::broadcastToPeers);

                // 7i. Handler de mensajes entrantes de peers
                PeerMessageHandler peerHandler = new PeerMessageHandler(replicator, routingTable);

                // 7j. Iniciar Gossip Protocol (descubrimiento por UDP)
                GossipProtocol gossip = new GossipProtocol(
                        identity, membership, eventBus,
                        Arrays.asList(config.getSeedNodes()),
                        config.getHeartbeatIntervalMs(),
                        config.getFailureTimeoutMs());
                new Thread(gossip, "Thread-GossipProtocol").start();

                // 7k. Iniciar PeerServer TCP (recibe conexiones de otros servidores)
                PeerServer peerServer = new PeerServer(config.getClusterPort(), peerHandler);
                new Thread(peerServer, "Thread-PeerServer").start();

                logger.info("Cluster P2P inicializado. Gossip UDP:{}, Peers TCP:{}",
                        config.getClusterPort(), config.getClusterPort());
            } else {
                logger.info("Modo standalone (cluster deshabilitado).");
            }

            // 8. Interfaces Expuestas y Consola Administrativa
            ServerAdminAPI adminAPI = new ServerAdminAPI(dao, dao);  // IUserRepository + IDocumentRepository
            InteractiveConsole console = new InteractiveConsole(
                    adminAPI, networkServer, healthService, routingTable);

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
import APIService.SentimentService;
import CommentService.CommentManager;
import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import DocumentService.DatabaseBackupManager;
import EncryptionUtils.EncryptionUtils;
import EncryptionUtils.IEncryptionUtils;
import FileSystemStorage.LocalFileManager;
import JsonSchema.JsonSchema;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import RequestRouter.clients.ClientRouter;
import DocumentService.TransferManager;
import RequestRouter.files.FileRouter;
import UserService.UserManager;
import api.ServerAdminAPI;
import config.NodeSetupWizard;
import config.ServerConfig;
import console.InteractiveConsole;
import events.Impl.NodeConnector;
import events.Impl.NodeDisconnector;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import pool.ConnectionPoolManager;
import ports.spi.*;
import protocolSelector.ProtocolSelector;
import java.util.ServiceLoader;

// Imports del módulo Cluster P2P
import communication.PeerConnectionPool;
import communication.PeerMessageHandler;
import communication.PeerServer;
import discovery.GossipProtocol;
import discovery.MembershipList;
import events.Impl.ClusterNotifier;
import events.NetworkEventBus;
import health.ClusterHealthService;
import models.LocalNodeInfo;
import replication.ReplicationManager;

import java.util.Arrays;

public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        configurarNivelesDeLog();
        logger.info("Arrancando Messaging Server en Modo Clúster Nativo...");

        try {
            ServerConfig config = new ServerConfig();
            NodeSetupWizard.run(config);

            logger.info("╔══ CONFIGURACIÓN EFECTIVA ══════════════════════════════");
            logger.info("║ server.host = {}", config.getHost());
            logger.info("║ server.port = {}", config.getPort());
            logger.info("║ cluster.nodeId = {}", config.getNodeId());
            logger.info("║ cluster.port = {}", config.getClusterPort());
            logger.info("║ cluster.seedNodes = {}", String.join(",", config.getSeedNodes()));
            logger.info("╚═══════════════════════════════════════════════════════");

            // ── 1. REPOSITORIOS DE PERSISTENCIA ─────────────────────────────────
            RepositoryFactory repoFactory = ServiceLoader.load(RepositoryFactory.class).iterator().next();
            IUserRepository userRepo = repoFactory.getUserRepository();
            IDocumentRepository docRepo = repoFactory.getDocumentRepository();
            IAuditLogRepository logRepo = repoFactory.getAuditLogRepository();
            ISessionRepository sessionRepo = repoFactory.getSessionRepository();
            ICommentRepository commentRepo = repoFactory.getCommentRepository();

            // ── 2. SERVICIOS DE NEGOCIO LOCALES ─────────────────────────────────
            UserManager userManager = new UserManager(userRepo, sessionRepo);
            LocalFileManager fileManager = new LocalFileManager();
            IEncryptionUtils encryptionUtils = new EncryptionUtils();
            CryptoManager cryptoManager = new CryptoManager(encryptionUtils);
            LogManager logManager = new LogManager(logRepo);
            DocumentManager documentManager = new DocumentManager(fileManager, cryptoManager, docRepo, userRepo, logManager);
            BroadcastManager broadcastManager = new BroadcastManager();
            TransferManager transferManager = new TransferManager();

            int apiPort = config.getSentimentApiPort();
            SentimentService sentimentService = new SentimentService(apiPort);
            CommentManager commentManager = new CommentManager(commentRepo, sentimentService, userManager);

            // ── 3. INFRAESTRUCTURA DEL CLÚSTER P2P ──────────────────────────────
            LocalNodeInfo identity = new LocalNodeInfo(
                    config.getNodeId(), config.getHost(),
                    config.getPort(), config.getClusterPort(),
                    config.getGatewayUrl());

            NetworkEventBus eventBus = new NetworkEventBus();
            MembershipList membership = new MembershipList();
            PeerConnectionPool peerPool = new PeerConnectionPool();

            ReplicationManager replicator = new ReplicationManager(identity.getNodeId(), membership, peerPool);


            ClusterHealthService healthService = new ClusterHealthService(identity, membership, peerPool);

            // ── 4. CONSTRUCCIÓN DE ROUTERS (INMUTABLE) ───────────────────────
            ClientRouter clientRouter = new ClientRouter(
                    userManager, documentManager, logManager, broadcastManager, transferManager, commentManager
                    , replicator,
                    identity.getNodeId(), membership, healthService, identity
            );

            FileRouter fileRouter = new FileRouter(documentManager, logManager, broadcastManager,
                    clientRouter.getHandler(JsonSchema.ACTION_LIST_LOGS),
                    clientRouter.getHandler(JsonSchema.ACTION_LIST_DOCUMENTS),
                    replicator,
                    identity
                    );

            // ── 6. ARRANQUE DEL SERVIDOR DE RED (CLIENTES SOCKET) ───────────────
            int maxConnections = config.getMaxConnections();
            ConnectionPoolManager pool = new ConnectionPoolManager(maxConnections);
            ThreadPoolManager threadPool = new ThreadPoolManager(maxConnections);

            ProtocolSelector networkServer = new ProtocolSelector();
            networkServer.iniciarServidor(config.getProtocol(), config.getPort(), pool, threadPool, clientRouter,
                    broadcastManager, transferManager, fileRouter);

            // ── 7. SUBSCRIPCIÓN DE EVENTOS DE RED Y REPLICACIÓN ──────────────────

            DatabaseBackupManager databaseBackupManager = new DatabaseBackupManager();

            NodeConnector nodeConnector = new NodeConnector(peerPool, identity.getNodeId(), documentManager, databaseBackupManager);
            eventBus.subscribe(nodeConnector);

            NodeDisconnector nodeDisconnector = new NodeDisconnector(peerPool, userManager);
            eventBus.subscribe(nodeDisconnector);

            ClusterNotifier clusterNotifier = new ClusterNotifier(broadcastManager::broadcast);
            eventBus.subscribe(clusterNotifier);

            orchestrators.ReplicationEventApplier eventApplier = new orchestrators.ReplicationEventApplier(
                    userManager, documentManager, commentManager, broadcastManager,
                    clientRouter.getHandler(JsonSchema.ACTION_NEW_MESSAGE),
                    clientRouter.getHandler(JsonSchema.ACTION_LIST_CLIENTS),
                    clientRouter.getHandler(JsonSchema.ACTION_LIST_DOCUMENTS)
                    );
            replicator.setEventHandler(eventApplier);

            // ── 8. CONFIGURACIÓN DE PEER MESSAGE HANDLER ────────────────────────
            PeerMessageHandler peerHandler = new PeerMessageHandler(replicator, databaseBackupManager);

            // ── 9. HILOS DE INFRAESTRUCTURA P2P (Gossip & Servidor entre Nodos) ─
            GossipProtocol gossip = new GossipProtocol(
                    identity, membership, eventBus, Arrays.asList(config.getSeedNodes()),
                    config.getHeartbeatIntervalMs(), config.getFailureTimeoutMs());
            new Thread(gossip, "Thread-GossipProtocol").start();

            PeerServer peerServer = new PeerServer(config.getClusterPort(), peerHandler);
            new Thread(peerServer, "Thread-PeerServer").start();

            // ── 10. INTERFACES ADMINISTRATIVAS ──────────────────────────────────
            ServerAdminAPI adminAPI = new ServerAdminAPI(userRepo, docRepo);
            com.universidad.messaging.api.SentimentApi.startApi(apiPort);

            InteractiveConsole console = new InteractiveConsole(adminAPI, networkServer, healthService, membership);
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
        } catch (Exception ignored) {}
    }
}
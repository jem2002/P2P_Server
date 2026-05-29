import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import EncryptionUtils.EncryptionUtils;
import EncryptionUtils.IEncryptionUtils;
import FileSystemStorage.LocalFileManager;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import UserService.UserManager;
import api.ServerAdminAPI;
import config.NodeSetupWizard;
import config.ServerConfig;
import console.InteractiveConsole;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import pool.ConnectionPoolManager;
import protocolSelector.ProtocolSelector;
import registry.LocalClientRegistry;
import replication.ReplicationEvent;
import routing.RemoteDeliveryStrategy;
import java.util.ServiceLoader;
import ports.spi.RepositoryFactory;
import ports.spi.IUserRepository;
import ports.spi.IDocumentRepository;
import ports.spi.IAuditLogRepository;
import ports.spi.ISessionRepository;

// Imports del módulo Cluster P2P
import communication.PeerConnectionPool;
import communication.PeerMessageHandler;
import communication.PeerServer;
import discovery.GossipProtocol;
import discovery.MembershipList;
import events.ClusterNotifier;
import events.NetworkEventBus;
import health.ClusterHealthService;
import identity.NodeIdentity;
import replication.ReplicationManager;
import topology.RoutingTable;

import java.util.Arrays;

/**
 * Punto de entrada de la aplicación — Composition Root.
 * Instancia y conecta todas las dependencias.
 *
 * Principio aplicado: DIP — las dependencias se inyectan via constructor/setter.
 *
 * Extensión P2P: cuando cluster.enabled=true inicializa los componentes distribuidos:
 *   - GossipProtocol (descubrimiento por UDP heartbeats)
 *   - PeerServer (acepta conexiones TCP de otros servidores)
 *   - LocalClientRegistry (entrega directa de mensajes a clientes locales)
 *   - RoutingTable (mapea cliente→nodo)
 *   - ReplicationManager (propaga y recibe eventos de mutación entre servidores)
 *   - ClusterNotifier (notifica a clientes locales cuando un servidor entra/sale)
 *   - ClusterHealthService (monitoreo del estado de la red)
 *
 * Requerimientos cubiertos:
 *   ✓ Detección de servidores amigos (Gossip + MembershipList)
 *   ✓ Cada servidor posee copia de mensajes y archivos de demás servidores (ReplicationManager)
 *   ✓ Notificación de join/leave de servidores (ClusterNotifier → BroadcastManager)
 *   ✓ Lista federada de clientes (ListClientsHandler + RoutingTable)
 *   ✓ Mensajes a cliente específico o a todos (SendMessageHandler con targetUsername)
 *   ✓ Info y logs de otros servidores (ListPeerInfoHandler + ListPeerLogsHandler)
 */
public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    public static void main(String[] args) {
        configurarNivelesDeLog();

        logger.info("Arrancando Messaging Server...");

        try {
            // 1. Configuración base (desde config.properties)
            ServerConfig config = new ServerConfig();

            // 1b. Asistente interactivo: permite personalizar los parámetros de red
            //     en terminal antes de arrancar. Pregunta si es el primer nodo y,
            //     si no lo es, solicita server.port, cluster.nodeId, cluster.port y seedNodes.
            NodeSetupWizard.run(config);

            // 1c. Confirmar en log los valores reales que usará el servidor
            logger.info("╔══ CONFIGURACIÓN EFECTIVA ══════════════════════════════");
            logger.info("║  server.host       = {}", config.getHost());
            logger.info("║  server.port       = {}", config.getPort());
            logger.info("║  cluster.nodeId    = {}", config.getNodeId());
            logger.info("║  cluster.port      = {}", config.getClusterPort());
            logger.info("║  cluster.seedNodes = {}", String.join(",", config.getSeedNodes()));
            logger.info("╚═══════════════════════════════════════════════════════");

            // 2. Módulo Persistencia (ServiceLoader)
            RepositoryFactory repoFactory = ServiceLoader.load(RepositoryFactory.class).iterator().next();
            IUserRepository userRepo = repoFactory.getUserRepository();
            IDocumentRepository docRepo = repoFactory.getDocumentRepository();
            IAuditLogRepository logRepo = repoFactory.getAuditLogRepository();
            ISessionRepository sessionRepo = repoFactory.getSessionRepository();

            // 3. Módulo Servicios
            UserManager userManager = new UserManager(userRepo, sessionRepo);
            LocalFileManager fileManager = new LocalFileManager();
            IEncryptionUtils encryptionUtils = new EncryptionUtils();
            CryptoManager cryptoManager = new CryptoManager(encryptionUtils);
            LogManager logManager = new LogManager(logRepo);
            DocumentManager documentManager = new DocumentManager(
                    fileManager, cryptoManager, docRepo, userRepo, logManager);
            BroadcastManager broadcastManager = new BroadcastManager();
            TransferManager transferManager = new TransferManager();

            int apiPort = config.getPort() + 7000;
            // 4. Módulo Protocolo
            MainRouter router = new MainRouter(userManager, documentManager, logManager,
                    broadcastManager, transferManager, apiPort);

            orchestrators.DisconnectionOrchestrator disconnectionService = new orchestrators.DisconnectionOrchestrator(
                    userManager, logManager, broadcastManager);
            disconnectionService.setListSuppliers(
                    () -> { try { return router.handleListClients(); } catch (Exception e) { return "{}"; } },
                    () -> { try { return router.handleListLogs(); } catch (Exception e) { return "{}"; } }
            );
            router.setDisconnectionService(disconnectionService);

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
            MembershipList membership = null;

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
                membership = new MembershipList();

                // 7c. Tabla de enrutamiento (cliente → nodeId)
                routingTable = new RoutingTable(identity.getNodeId());

                // 7d. Registro local de streams (username → OutputStream)
                LocalClientRegistry localClientRegistry = new LocalClientRegistry();

                // 7e. Pool de conexiones TCP a peers
                PeerConnectionPool peerPool = new PeerConnectionPool();

                // 7f. Gestor de replicación
                ReplicationManager replicator = new ReplicationManager(
                        identity.getNodeId(), membership, peerPool);

                // 7g. Servicio de salud del cluster
                healthService = new ClusterHealthService(identity, membership, peerPool);

                // 7h. Suscribir observers al bus de eventos
                final RoutingTable finalRoutingTable = routingTable;
                eventBus.subscribe(peerPool);         // Abre/cierra conexiones TCP a peers
                eventBus.subscribe(replicator);        // Reacciona a cambios de membresía
                eventBus.subscribe(finalRoutingTable); // Limpia clientes de nodos caídos

                // 7i. Notificador de eventos del cluster hacia clientes locales
                //     Requerimiento: "Los servidores deberán informar cuando se une o desconecta un servidor"
                ClusterNotifier clusterNotifier = new ClusterNotifier(broadcastManager::broadcastLocalOnly);
                eventBus.subscribe(clusterNotifier);

                // 7i-b. Bootstrap sync: cuando un nuevo nodo se une, enviarle nuestra
                //       RoutingTable completa para que conozca a todos los clientes existentes.
                //       Sin esto, node-3 (que llega tarde) nunca recibe los CLIENT_CONNECTED
                //       de los nodos que ya estaban activos.
                final String finalNodeId = identity.getNodeId();
                eventBus.subscribe(new events.NetworkEventListener() {
                    @Override
                    public void onNodeJoined(events.NodeInfo node) {
                        // Esperamos en un hilo aparte para que PeerConnectionPool
                        // haya tenido tiempo de abrir la conexión TCP primero.
                        new Thread(() -> {
                            int maxRetries = 5;
                            long delay = 200; // ms
                            for (int i = 0; i < maxRetries; i++) {
                                try {
                                    Thread.sleep(delay);
                                    String syncMsg = communication.InterServerProtocol
                                            .buildSyncMessage(finalNodeId, finalRoutingTable);
                                    peerPool.sendToPeer(node.getNodeId(), syncMsg);
                                    logger.info("RoutingTable enviada a nuevo nodo '{}' (bootstrap sync)", node.getNodeId());
                                    return; // éxito
                                } catch (Exception e) {
                                    delay *= 2; // backoff exponencial
                                    if (i == maxRetries - 1) {
                                        logger.warn("No se pudo enviar bootstrap sync a '{}' tras {} intentos: {}",
                                                node.getNodeId(), maxRetries, e.getMessage());
                                    }
                                }
                            }
                        }, "BootstrapSync-" + node.getNodeId()).start();
                    }
                    @Override
                    public void onNodeLeft(events.NodeInfo node) { /* no-op */ }
                });

                // 7j. Hook de broadcast federado (OCP: extiende BroadcastManager sin modificarlo)
                broadcastManager.setFederatedHook(peerPool::broadcastToPeers);

                // 7k. Conectar ReplicationManager al dominio local:
                //     Cuando un evento replicado llega de un peer, aplicarlo aquí
                final LocalClientRegistry finalRegistry = localClientRegistry;
                final MainRouter finalRouter = router;
                orchestrators.ReplicationEventApplier eventApplier = new orchestrators.ReplicationEventApplier(
                        userManager, documentManager, broadcastManager,
                        finalRoutingTable, identity.getNodeId(),
                        () -> { try { return finalRouter.handleListClients(); } catch (Exception e) { return "{}"; } },
                        () -> { try { return finalRouter.handleListDocuments(); } catch (Exception e) { return "{}"; } });
                replicator.setEventHandler(eventApplier);

                // 7l. Handler de mensajes entrantes de peers (ahora con todas las dependencias)
                PeerMessageHandler peerHandler = new PeerMessageHandler(
                        replicator, finalRoutingTable, localClientRegistry);

                // Inyectar proveedor de logs locales (para responder PEER_LOGS_REQUEST)
                peerHandler.setLocalLogsSupplier(() -> {
                    try { return finalRouter.handleListLogs(); } catch (Exception e) { return "{}"; }
                });
                // Inyectar broadcast local (para PEER_BROADCAST)
                peerHandler.setLocalBroadcast(broadcastManager::broadcast);

                peerHandler.setOnRouteDelivered((targetUser, fromUser, rawContent, clientIp) -> {
                    try {
                        long userId = userManager.obtenerIdUsuario(fromUser);
                        java.io.InputStream textStream = new java.io.ByteArrayInputStream(rawContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        String nombreArchivo = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";
                        documentManager.procesarRecepcionDocumento(textStream, nombreArchivo, rawContent.length(), ".txt", "text/plain", userId, "replicado", "PRIVATE_TO:" + targetUser);
                    } catch (Exception e) {}
                });

                documentManager.setOnLocalDocumentUploaded((docId, filename, sizeBytes, extension, mimeType, ownerUsername, ownerIp, docType) -> {
                    // Solo propagar eventos de archivos, no de mensajes (los mensajes usan NEW_MESSAGE)
                    // y evitar bucles infinitos no propagando si el archivo ya fue descargado como "replicado"
                    if (identity != null && !docType.equals("MESSAGE") && !docType.startsWith("PRIVATE_TO:") && !"replicado".equals(ownerIp)) {
                        replicator.propagate(ReplicationEvent.documentUploaded(
                                identity.getNodeId(), docId, filename, sizeBytes, extension, mimeType, docType, ownerUsername, ownerIp, identity.getHost(), identity.getClientPort()));
                    }
                });


                // 7m. Estrategia de entrega remota
                RemoteDeliveryStrategy remoteDelivery = new RemoteDeliveryStrategy(
                        peerPool, finalRoutingTable);

                // 7n. Activar integración P2P en el MainRouter
                disconnectionService.enableCluster(finalRoutingTable, localClientRegistry, replicator, identity.getNodeId());
                router.enableCluster(
                        finalRoutingTable,
                        localClientRegistry,
                        replicator,
                        remoteDelivery,
                        identity.getNodeId(),
                        membership,
                        healthService,
                        identity);

                // 7o. Iniciar Gossip Protocol (descubrimiento por UDP)
                GossipProtocol gossip = new GossipProtocol(
                        identity, membership, eventBus,
                        Arrays.asList(config.getSeedNodes()),
                        config.getHeartbeatIntervalMs(),
                        config.getFailureTimeoutMs());
                new Thread(gossip, "Thread-GossipProtocol").start();

                // 7p. Iniciar PeerServer TCP (recibe conexiones de otros servidores)
                PeerServer peerServer = new PeerServer(config.getClusterPort(), peerHandler);
                new Thread(peerServer, "Thread-PeerServer").start();

                logger.info("Cluster P2P inicializado. Gossip UDP:{}, Peers TCP:{}",
                        config.getClusterPort(), config.getClusterPort());
            } else {
                logger.info("Modo standalone (cluster deshabilitado).");
            }

            // 8. Interfaces Expuestas y Consola Administrativa
            ServerAdminAPI adminAPI = new ServerAdminAPI(userRepo, docRepo);
            
            // Iniciar API de Inteligencia Artificial (sumamos 7000 al puerto TCP para evitar colisiones)
            com.universidad.messaging.api.SentimentApi.startApi(apiPort);

            InteractiveConsole console = new InteractiveConsole(
                    adminAPI, networkServer, healthService, routingTable, membership);

            // Arrancamos la consola en el hilo principal
            console.run();

        } catch (Exception e) {
            logger.error("Error crítico durante el arranque del servidor.", e);
            System.exit(1);
        }
    }

    /**
     * Método auxiliar expuesto al ReplicationManager para recuperar la lista de clientes.
     * Usamos un helper en el main para evitar acoplamiento circular.
     */
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
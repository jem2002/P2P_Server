package RequestRouter;

import CommentService.CommentManager;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import MessageParser.JsonInputParser;
import MessageParser.MessageWrapper;
import RequestRouter.handlers.*;
import UserService.UserManager;
import models.LocalNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import delivery.Impl.LocalDeliveryStrategyPrivate;
import ports.api.IBroadcastManager;
import replication.ReplicationManager;
import delivery.Impl.RemoteDeliveryStrategyPrivate;
import topology.RoutingTable;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import ports.api.IRequestDispatcher;

/**
 * Router principal del protocolo JSON.
 * Despacha las solicitudes entrantes al ActionHandler correspondiente.
 *
 * Principios aplicados:
 * - OCP: agregar una nueva acción = crear una nueva clase ActionHandler
 * y registrarla en el Map, sin modificar este archivo.
 * - SRP: esta clase SOLO hace routing (dispatch). La lógica de negocio
 * vive en cada handler individual.
 * - Controller (GRASP): punto de entrada coordinador del protocolo.
 *
 * Arquitectura Clúster:
 * - El clúster siempre está habilitado. Las dependencias se exigen en el constructor,
 * garantizando inmutabilidad y eliminando estados incompletos.
 */
public class MainRouter implements IRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MainRouter.class);

    private final JsonInputParser parser;
    private final ResponseBuilder serializer;
    private final Map<String, ActionHandler> handlers;

    // Handlers reutilizables (para broadcast de datos actualizados)
    private final ListClientsHandler listClientsHandler;
    private final ListLogsHandler listLogsHandler;

    // Dependencias de negocio (para notificarDesconexionFisica)
    private final UserManager userManager;
    private final BroadcastManager broadcastManager;
    private final LogManager logManager;

    // Dependencias de cluster obligatorias e inmutables (FINAL)
    private final RoutingTable routingTable;
    private final LocalDeliveryStrategyPrivate localDeliveryStrategy;
    private final ReplicationManager replicationManager;
    private final String localNodeId;

    // Handler de conexión (referencia necesaria para inyección de OutputStream)
    private final ConnectHandler connectHandler;

    private ports.api.IDisconnectionHandler disconnectionService;

    public void setDisconnectionService(ports.api.IDisconnectionHandler ds) {
        this.disconnectionService = ds;
    }

    /**
     * Constructor único con inyección completa y obligatoria de componentes locales y de red.
     */
    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager,
                      BroadcastManager broadcastManager, TransferManager transferManager, CommentManager commentManager,
                      RoutingTable routingTable, LocalDeliveryStrategyPrivate localDeliveryStrategy,
                      ReplicationManager replicationManager, RemoteDeliveryStrategyPrivate remoteDelivery,
                      String localNodeId,
                      discovery.MembershipList membershipList, health.ClusterHealthService healthService,
                      LocalNodeInfo localIdentity) {

        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.broadcastManager = broadcastManager;
        this.logManager = logManager;

        // Asignación directa de dependencias del clúster
        this.routingTable = routingTable;
        this.localDeliveryStrategy = localDeliveryStrategy;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;

        // ── 1. CREACIÓN DE HANDLERS CON INYECCIÓN UNIFICADA ───────────────────
        this.listLogsHandler = new ListLogsHandler(logManager, serializer);

        // Instanciación limpia usando las variables listas del constructor
        this.listClientsHandler = new ListClientsHandler(userManager, serializer, routingTable, localNodeId);

        ListDocumentsHandler listDocumentsHandler = new ListDocumentsHandler(documentManager, serializer);
        ListMessagesHandler listMessagesHandler = new ListMessagesHandler(documentManager, serializer);

        this.connectHandler = new ConnectHandler(userManager, logManager, serializer,
                broadcastManager, listClientsHandler, routingTable, replicationManager,
                localDeliveryStrategy, localNodeId);

        // ── 2. REGISTRO DE HANDLERS EN EL MAPA (OCP) ──────────────────────────
        this.handlers = new HashMap<>();
        handlers.put(JsonSchema.ACTION_CONNECT, connectHandler);
        handlers.put(JsonSchema.ACTION_LIST_CLIENTS, listClientsHandler);
        handlers.put(JsonSchema.ACTION_LIST_DOCUMENTS, listDocumentsHandler);
        handlers.put(JsonSchema.ACTION_LIST_MESSAGES, listMessagesHandler);
        handlers.put(JsonSchema.ACTION_LIST_LOGS, listLogsHandler);

        handlers.put(JsonSchema.ACTION_UPLOAD_INIT,
                new UploadInitHandler(userManager, transferManager, logManager,
                        broadcastManager, serializer, listLogsHandler));

        handlers.put(JsonSchema.ACTION_DOWNLOAD_INIT,
                new DownloadInitHandler(userManager, documentManager, transferManager,
                        logManager, broadcastManager, serializer, listLogsHandler));

        // Mensajería basada en estrategias con dependencias completas
        handlers.put(JsonSchema.ACTION_SEND_MESSAGE,
                new SendMessageHandler(userManager, documentManager, logManager,
                        broadcastManager, serializer, listLogsHandler, routingTable,
                        localDeliveryStrategy, replicationManager, remoteDelivery,
                        localNodeId));

        handlers.put(JsonSchema.ACTION_COMMENT_DOCUMENT, new CommentDocumentHandler(commentManager, serializer));

        // Handlers de Clúster e Información P2P registrados de manera estática
        handlers.put(JsonSchema.ACTION_LIST_PEER_INFO,
                new ListPeerInfoHandler(serializer, healthService, localIdentity, membershipList));

        handlers.put(JsonSchema.ACTION_LIST_PEER_LOGS,
                new ListPeerLogsHandler(serializer, membershipList, localNodeId,
                        () -> {
                            try {
                                return listLogsHandler.handle(null, null);
                            } catch (Exception e) {
                                return "{}";
                            }
                        }));

        logger.info("MainRouter: Arquitectura federada inicializada correctamente para el nodo '{}'", localNodeId);
    }

    /**
     * Debe llamarse justo antes de routeRequest() para que ConnectHandler
     * pueda registrar el OutputStream del cliente conectado.
     */
    public void setCurrentClientOutputStream(OutputStream out) {
        connectHandler.setClientOutputStream(out);
    }

    /**
     * Despacha una solicitud JSON al handler correspondiente.
     */
    public String routeRequest(String rawJson, String clientIp) {
        MessageWrapper request = parser.parse(rawJson);

        if (request == null) {
            return serializer.buildErrorResponse("Formato JSON inválido.");
        }

        ActionHandler handler = handlers.get(request.getAction());
        if (handler == null) {
            return serializer.buildErrorResponse("Acción no soportada.");
        }

        try {
            return handler.handle(request.getPayload(), clientIp);
        } catch (Exception e) {
            logger.error("Error en router procesando acción: {}", request.getAction(), e);
            return serializer.buildErrorResponse("Error interno del servidor.");
        }
    }

    /**
     * Procesa la desconexión física de un cliente.
     */
    public void notificarDesconexionFisica(String rawClientIp, OutputStream out) {
        if (disconnectionService != null) {
            disconnectionService.procesarDesconexion(rawClientIp, out);
        } else {
            logger.warn("DisconnectionService no configurado.");
        }
    }

    // Métodos públicos para uso por FileTransferHandler (broadcast de datos actualizados)

    public String handleListDocuments() {
        try {
            return handlers.get(JsonSchema.ACTION_LIST_DOCUMENTS).handle(null, null);
        } catch (Exception e) {
            logger.error("Error generando lista de documentos para broadcast", e);
            return serializer.buildErrorResponse("Error interno.");
        }
    }

    public String handleListMessages() {
        try {
            return handlers.get(JsonSchema.ACTION_LIST_MESSAGES).handle(null, null);
        } catch (Exception e) {
            logger.error("Error generando lista de mensajes para broadcast", e);
            return serializer.buildErrorResponse("Error interno.");
        }
    }

    public String handleListLogs() {
        try {
            return listLogsHandler.handle(null, null);
        } catch (Exception e) {
            logger.error("Error generando lista de logs para broadcast", e);
            return serializer.buildErrorResponse("Error interno.");
        }
    }

    public String handleListClients() {
        try {
            return listClientsHandler.handle(null, null);
        } catch (Exception e) {
            logger.error("Error generando lista de clientes para broadcast", e);
            return serializer.buildErrorResponse("Error interno.");
        }
    }
}
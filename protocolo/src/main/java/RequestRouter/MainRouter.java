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
import replication.ReplicationManager;
import topology.RoutingTable;

import java.util.HashMap;
import java.util.Map;

import ports.api.IRequestDispatcher;
import ports.api.ActionHandler;

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
    private final Map<String,ActionHandler> handlers = new HashMap<>();;

    /**
     * Constructor único con inyección completa y obligatoria de componentes locales y de red.
     */
    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager,
                      BroadcastManager broadcastManager, TransferManager transferManager, CommentManager commentManager,
                      RoutingTable routingTable,
                      ReplicationManager replicationManager,
                      String localNodeId,
                      discovery.MembershipList membershipList, health.ClusterHealthService healthService,
                      LocalNodeInfo localIdentity) {

        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();

        handlers.put(JsonSchema.ACTION_LIST_CLIENTS,
                new ListClientsHandler(userManager, serializer, routingTable, localNodeId));

        handlers.put(JsonSchema.ACTION_CONNECT,
                new ConnectHandler(userManager, logManager, serializer,
                broadcastManager, handlers.get(JsonSchema.ACTION_LIST_CLIENTS), routingTable, replicationManager, localNodeId));

        handlers.put(JsonSchema.ACTION_DISCONNECT,
                new DisconnectHandler(userManager, logManager, serializer,
                broadcastManager, handlers.get(JsonSchema.ACTION_LIST_CLIENTS), routingTable, replicationManager, localNodeId));

        handlers.put(JsonSchema.ACTION_LIST_DOCUMENTS,
                new ListDocumentsHandler(documentManager, serializer));

        handlers.put(JsonSchema.ACTION_LIST_MESSAGES,
                new ListMessagesHandler(documentManager, serializer));

        handlers.put(JsonSchema.ACTION_LIST_LOGS,
                new ListLogsHandler(logManager, serializer));

        handlers.put(JsonSchema.ACTION_UPLOAD_INIT,
                new UploadInitHandler(userManager, transferManager, logManager,
                        broadcastManager, serializer, handlers.get(JsonSchema.ACTION_LIST_LOGS)));

        handlers.put(JsonSchema.ACTION_UPLOAD_CONFIRMATION,
                new UploadConfirmationHandler(replicationManager, localIdentity, serializer));

        handlers.put(JsonSchema.ACTION_DOWNLOAD_INIT,
                new DownloadInitHandler(userManager, documentManager, transferManager,
                        logManager, broadcastManager, serializer, handlers.get(JsonSchema.ACTION_LIST_LOGS)));

        handlers.put(JsonSchema.ACTION_SEND_MESSAGE,
                new SendMessageHandler(userManager, documentManager, logManager,
                        broadcastManager, serializer, handlers.get(JsonSchema.ACTION_LIST_LOGS),
                        handlers.get(JsonSchema.ACTION_LIST_MESSAGES)
                        ,replicationManager,
                        localNodeId));

        handlers.put(JsonSchema.ACTION_COMMENT_DOCUMENT,
                new CommentDocumentHandler(commentManager, serializer));

        handlers.put(JsonSchema.ACTION_LIST_PEER_INFO,
                new ListPeerInfoHandler(serializer, healthService, localIdentity, membershipList));

        handlers.put(JsonSchema.ACTION_LIST_PEER_LOGS,
                new ListPeerLogsHandler(serializer, membershipList, localNodeId,
                        handlers.get(JsonSchema.ACTION_LIST_LOGS)));

        logger.info("MainRouter: Arquitectura federada inicializada correctamente para el nodo '{}'", localNodeId);
    }


    /**
     * Despacha una solicitud JSON al handler correspondiente.
     */
    public String routeRequest(String rawJson, String clientIp) {
        MessageWrapper request = parser.parse(rawJson);

        if (request == null) {
            return serializer.buildErrorResponse("Formato JSON inválido.");
        }

        ports.api.ActionHandler handler = handlers.get(request.getAction());
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


    public ActionHandler getHandler(String action) {
        if (action == null || action.trim().isEmpty()) {
            return null; // Opcional: validación temprana
        }
        return handlers.get(action);
    }

}
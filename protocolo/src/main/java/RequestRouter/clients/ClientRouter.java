package RequestRouter.clients;

import CommentService.CommentManager;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import MessageParser.JsonInputParser;
import MessageParser.MessageWrapper;
import RequestRouter.clients.handlers.*;
import UserService.UserManager;
import models.LocalNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.ReplicationManager;

import java.util.HashMap;
import java.util.Map;

import ports.api.IClientRequestDispatcher;
import ports.api.ClientActionHandler;

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
public class ClientRouter implements IClientRequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ClientRouter.class);

    private final JsonInputParser parser;
    private final ResponseBuilder serializer;
    private final Map<String, ClientActionHandler> clientHandlers = new HashMap<>();;


    /**
     * Constructor único con inyección completa y obligatoria de componentes locales y de red.
     */
    public ClientRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager,
                        BroadcastManager broadcastManager, DocumentService.TransferManager transferManager, CommentManager commentManager,
                        ReplicationManager replicationManager,
                        String localNodeId,
                        discovery.MembershipList membershipList, health.ClusterHealthService healthService,
                        LocalNodeInfo localIdentity) {

        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();

        clientHandlers.put(JsonSchema.ACTION_LIST_CLIENTS,
                new ListClientsHandlerClient(userManager, serializer, localNodeId));

        clientHandlers.put(JsonSchema.ACTION_CONNECT,
                new ConnectHandlerClient(userManager, logManager, serializer,
                broadcastManager, clientHandlers.get(JsonSchema.ACTION_LIST_CLIENTS), replicationManager, localNodeId));

        clientHandlers.put(JsonSchema.ACTION_DISCONNECT,
                new DisconnectHandlerClient(userManager, logManager, serializer,
                broadcastManager, clientHandlers.get(JsonSchema.ACTION_LIST_CLIENTS), replicationManager, localNodeId));

        clientHandlers.put(JsonSchema.ACTION_LIST_DOCUMENTS,
                new ListDocumentsHandlerClient(documentManager, serializer));

        clientHandlers.put(JsonSchema.ACTION_LIST_MESSAGES,
                new ListMessagesHandlerClient(documentManager, serializer));

        clientHandlers.put(JsonSchema.ACTION_LIST_LOGS,
                new ListLogsHandlerClient(logManager, serializer));

        clientHandlers.put(JsonSchema.ACTION_UPLOAD_INIT,
                new UploadInitHandlerClient(userManager, transferManager, logManager,
                        broadcastManager, serializer, clientHandlers.get(JsonSchema.ACTION_LIST_LOGS)));

        clientHandlers.put(JsonSchema.ACTION_DOWNLOAD_INIT,
                new DownloadInitHandlerClient(userManager, documentManager, transferManager,
                        logManager, broadcastManager, serializer, clientHandlers.get(JsonSchema.ACTION_LIST_LOGS)));

        clientHandlers.put(JsonSchema.ACTION_SEND_MESSAGE,
                new SendMessageHandlerClient(userManager, documentManager, logManager,
                        broadcastManager, serializer, clientHandlers.get(JsonSchema.ACTION_LIST_LOGS),
                        clientHandlers.get(JsonSchema.ACTION_LIST_MESSAGES)
                        ,replicationManager,
                        localNodeId));

        clientHandlers.put(JsonSchema.ACTION_COMMENT_DOCUMENT,
                new CommentDocumentHandlerClient(commentManager, serializer));

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

        ClientActionHandler handler = clientHandlers.get(request.getAction());
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


    public ClientActionHandler getHandler(String action) {
        if (action == null || action.trim().isEmpty()) {
            return null; // Opcional: validación temprana
        }
        return clientHandlers.get(action);
    }

}
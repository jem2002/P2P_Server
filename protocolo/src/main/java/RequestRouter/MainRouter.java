package RequestRouter;

import JsonSchema.ClientAddress;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import MessageParser.JsonInputParser;
import MessageParser.MessageWrapper;
import RequestRouter.handlers.*;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Router principal del protocolo JSON.
 * Despacha las solicitudes entrantes al ActionHandler correspondiente.
 *
 * Principios aplicados:
 *   - OCP: agregar una nueva acción = crear una nueva clase ActionHandler
 *          y registrarla en el Map, sin modificar este archivo.
 *   - SRP: esta clase SOLO hace routing (dispatch). La lógica de negocio
 *          vive en cada handler individual.
 *   - Controller (GRASP): punto de entrada coordinador del protocolo.
 */
public class MainRouter {

    private static final Logger logger = LoggerFactory.getLogger(MainRouter.class);

    private final JsonInputParser parser;
    private final ResponseBuilder serializer;
    private final Map<String, ActionHandler> handlers;

    // Handlers reutilizables (para broadcast de datos actualizados)
    private final ListClientsHandler listClientsHandler;
    private final ListLogsHandler listLogsHandler;

    private final UserManager userManager;
    private final BroadcastManager broadcastManager;
    private final LogManager logManager;

    public MainRouter(UserManager userManager, DocumentManager documentManager, LogManager logManager,
                      BroadcastManager broadcastManager, TransferManager transferManager) {
        this.parser = new JsonInputParser();
        this.serializer = new ResponseBuilder();
        this.userManager = userManager;
        this.broadcastManager = broadcastManager;
        this.logManager = logManager;

        // Crear handlers reutilizables
        this.listClientsHandler = new ListClientsHandler(userManager, serializer);
        this.listLogsHandler = new ListLogsHandler(logManager, serializer);

        ListDocumentsHandler listDocumentsHandler = new ListDocumentsHandler(documentManager, serializer);
        ListMessagesHandler listMessagesHandler = new ListMessagesHandler(documentManager, serializer);

        // Registrar todos los handlers en el Map (OCP)
        this.handlers = new HashMap<>();
        handlers.put(JsonSchema.ACTION_CONNECT,
                new ConnectHandler(userManager, logManager, serializer));
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
        handlers.put(JsonSchema.ACTION_SEND_MESSAGE,
                new SendMessageHandler(userManager, documentManager, logManager,
                        broadcastManager, serializer, listLogsHandler));
    }

    /**
     * Despacha una solicitud JSON al handler correspondiente.
     * El switch monolítico original fue reemplazado por un Map lookup (OCP).
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
     * Cierra la sesión en BD, notifica a los demás y actualiza logs.
     */
    public void notificarDesconexionFisica(String rawClientIp, OutputStream out) {
        try {
            ClientAddress address = ClientAddress.parse(rawClientIp);
            if (out != null) broadcastManager.removeStream(out);

            long userId = userManager.desconectarPorCaidaDeRed(address.getIp(), address.getPort());

            String username = userManager.obtenerNombreUsuario(userId);
            logManager.registrarAccion(null, userId, "DISCONNECT", "SUCCESS",
                    "Desconexión física del usuario " + username + " (" + address + ")");
            broadcastManager.broadcast(listLogsHandler.handle(null, null));

            String listaTrasDesconexion = listClientsHandler.handle(null, null);
            broadcastManager.broadcast(listaTrasDesconexion);

        } catch (Exception e) {
            logger.error("Error procesando desconexión física", e);
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
}
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
import registry.LocalClientRegistry;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import routing.RemoteDeliveryStrategy;
import topology.RoutingTable;

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
 *
 * Extensión P2P: acepta dependencias de cluster opcionales (null si deshabilitado).
 * Cuando el cluster está habilitado, ConnectHandler, SendMessageHandler y
 * ListClientsHandler trabajan de forma federada.
 */
public class MainRouter {

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

    // Dependencias de cluster (null si deshabilitado)
    private RoutingTable routingTable;
    private LocalClientRegistry localClientRegistry;
    private ReplicationManager replicationManager;
    private String localNodeId;

    // Handler de conexión (referencia necesaria para inyección de OutputStream)
    private final ConnectHandler connectHandler;

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

        this.connectHandler = new ConnectHandler(userManager, logManager, serializer,
                broadcastManager, listClientsHandler);

        // Registrar todos los handlers en el Map (OCP)
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
        handlers.put(JsonSchema.ACTION_SEND_MESSAGE,
                new SendMessageHandler(userManager, documentManager, logManager,
                        broadcastManager, serializer, listLogsHandler));
    }

    /**
     * Activa la integración con el cluster P2P.
     * Se llama desde ServerApplication cuando cluster.enabled=true.
     *
     * @param routingTable         Tabla de enrutamiento cliente→nodo
     * @param localClientRegistry  Registro de streams locales (para entrega directa)
     * @param replicationManager   Gestor de replicación de eventos
     * @param remoteDelivery       Estrategia de entrega a clientes remotos
     * @param localNodeId          Identificador de este nodo
     * @param membershipList       Lista de membresía (para LIST_PEER_INFO)
     * @param healthService        Servicio de salud del cluster
     * @param localIdentity        Identidad de este nodo (para LIST_PEER_INFO)
     */
    public void enableCluster(RoutingTable routingTable,
                               LocalClientRegistry localClientRegistry,
                               ReplicationManager replicationManager,
                               RemoteDeliveryStrategy remoteDelivery,
                               String localNodeId,
                               discovery.MembershipList membershipList,
                               health.ClusterHealthService healthService,
                               identity.NodeIdentity localIdentity) {
        this.routingTable = routingTable;
        this.localClientRegistry = localClientRegistry;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;

        // Activar vista federada de clientes
        listClientsHandler.enableFederatedList(routingTable, localNodeId);

        // Activar cluster en ConnectHandler
        connectHandler.enableCluster(routingTable, replicationManager,
                localClientRegistry, localNodeId);

        // Activar entrega dirigida en SendMessageHandler
        SendMessageHandler sendHandler = (SendMessageHandler) handlers.get(JsonSchema.ACTION_SEND_MESSAGE);
        sendHandler.enableCluster(routingTable, localClientRegistry,
                replicationManager, remoteDelivery, localNodeId);

        // Registrar handler de info de peers
        ListPeerInfoHandler listPeerInfoHandler = new ListPeerInfoHandler(
                serializer, healthService, localIdentity, membershipList);
        handlers.put(JsonSchema.ACTION_LIST_PEER_INFO, listPeerInfoHandler);

        // Registrar handler de logs de peers
        ListPeerLogsHandler listPeerLogsHandler = new ListPeerLogsHandler(
                serializer, membershipList, localNodeId,
                () -> {
                    try {
                        return listLogsHandler.handle(null, null);
                    } catch (Exception e) {
                        return "{}";
                    }
                });
        handlers.put(JsonSchema.ACTION_LIST_PEER_LOGS, listPeerLogsHandler);

        logger.info("MainRouter: integración P2P habilitada para nodo '{}'", localNodeId);
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
     * En modo cluster: elimina de RoutingTable y LocalClientRegistry, propaga evento.
     */
    public void notificarDesconexionFisica(String rawClientIp, OutputStream out) {
        try {
            ClientAddress address = ClientAddress.parse(rawClientIp);
            if (out != null) broadcastManager.removeStream(out);

            long userId = userManager.desconectarPorCaidaDeRed(address.getIp(), address.getPort());
            String username = userManager.obtenerNombreUsuario(userId);

            // Fallback: si MySQL no encontró la sesión por IP/Puerto (puede pasar con IPv6 o NAT local),
            // buscamos el username directamente en el LocalClientRegistry usando el OutputStream.
            if (("UsuarioDesconocido".equals(username) || username == null) && localClientRegistry != null && out != null) {
                String fallbackName = localClientRegistry.getUsernameByStream(out);
                if (fallbackName != null) {
                    username = fallbackName;
                    logger.info("Resolución de usuario por fallback (Stream) exitosa: {}", username);
                    try {
                        userManager.cerrarSesionPorUsername(username);
                    } catch (Exception e) {
                        logger.warn("No se pudo cerrar sesión en BD por username: {}", e.getMessage());
                    }
                }
            }

            if (!"UsuarioDesconocido".equals(username) && username != null) {
                logManager.registrarAccion(null, userId > 0 ? userId : -1, "DISCONNECT", "SUCCESS",
                        "Desconexión física del usuario " + username + " (" + address + ")");
                broadcastManager.broadcast(listLogsHandler.handle(null, null));

                // --- Integración P2P: limpiar registros del cliente ───────────────
                if (routingTable != null) {
                    routingTable.unregisterClient(username);
                }
                if (localClientRegistry != null) {
                    localClientRegistry.unregister(username);
                }
                if (replicationManager != null && localNodeId != null) {
                    replicationManager.propagate(
                            ReplicationEvent.clientDisconnected(localNodeId, username));
                }
                // ──────────────────────────────────────────────────────────────────
            }

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

    public String handleListClients() {
        try {
            return listClientsHandler.handle(null, null);
        } catch (Exception e) {
            logger.error("Error generando lista de clientes para broadcast", e);
            return serializer.buildErrorResponse("Error interno.");
        }
    }
}
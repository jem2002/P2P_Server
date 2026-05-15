package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import MessageParser.BroadcastManager;
import DocumentService.DocumentManager;
import RequestRouter.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;
import registry.LocalClientRegistry;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import routing.RemoteDeliveryStrategy;
import topology.RoutingTable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Maneja la acción SEND_MESSAGE.
 *
 * Soporta dos modos según el campo opcional "targetUsername" en el payload:
 *
 *  a) Broadcast (targetUsername == null o "ALL"):
 *     - Persiste el mensaje como archivo en disco (siempre).
 *     - Hace broadcast local a todos los clientes conectados.
 *     - Propaga a todos los servidores peer via FederatedBroadcastHook.
 *     - Replica el evento NEW_MESSAGE a los peers via ReplicationManager.
 *
 *  b) Mensaje dirigido (targetUsername = "ClienteB"):
 *     - Persiste el mensaje como archivo en disco (siempre).
 *     - Resuelve si el destino es LOCAL o REMOTO:
 *         * LOCAL:  entrega directa via LocalClientRegistry.
 *         * REMOTO: reenvía al servidor peer via RemoteDeliveryStrategy (PEER_ROUTE).
 *
 * Requerimientos cumplidos:
 *   - "Los documentos/mensajes se podrán enviar a un cliente en especial o a todos."
 *   - "Cada servidor debe poseer una copia de los mensajes." (persistencia siempre activa)
 */
public class SendMessageHandler implements ActionHandler {

    private final UserManager userManager;
    private final DocumentManager documentManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ListLogsHandler listLogsHandler;

    // Componentes de cluster (null si cluster deshabilitado)
    private RoutingTable routingTable;
    private LocalClientRegistry localClientRegistry;
    private ReplicationManager replicationManager;
    private RemoteDeliveryStrategy remoteDelivery;
    private String localNodeId;

    public SendMessageHandler(UserManager userManager, DocumentManager documentManager,
                              LogManager logManager, BroadcastManager broadcastManager,
                              ResponseBuilder serializer, ListLogsHandler listLogsHandler) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.serializer = serializer;
        this.listLogsHandler = listLogsHandler;
    }

    /** Inyecta dependencias de cluster para entrega dirigida. */
    public void enableCluster(RoutingTable routingTable,
                               LocalClientRegistry localClientRegistry,
                               ReplicationManager replicationManager,
                               RemoteDeliveryStrategy remoteDelivery,
                               String localNodeId) {
        this.routingTable = routingTable;
        this.localClientRegistry = localClientRegistry;
        this.replicationManager = replicationManager;
        this.remoteDelivery = remoteDelivery;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        String fromUser      = payload.get("username").asText();
        String content       = payload.get("message").asText();
        String targetUsername = payload.has(JsonSchema.PAYLOAD_TARGET_USERNAME)
                ? payload.get(JsonSchema.PAYLOAD_TARGET_USERNAME).asText() : null;

        long userId = userManager.obtenerIdUsuario(fromUser);

        // ── Persistencia del mensaje (SIEMPRE, independiente del modo) ──────────
        InputStream textStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        String nombreArchivo   = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";
        documentManager.procesarRecepcionDocumento(
                textStream, nombreArchivo, content.length(), ".txt", "text/plain", userId, clientIp, "MESSAGE");

        // ── Log de auditoría ─────────────────────────────────────────────────────
        String logDetail = targetUsername != null && !targetUsername.equals("ALL")
                ? "Mensaje de " + fromUser + " a " + targetUsername
                : "Mensaje de " + fromUser + " (broadcast)";
        logManager.registrarAccion(null, userId, "SEND_MESSAGE", "SUCCESS", logDetail);

        // ── Construcción del mensaje de tiempo real ──────────────────────────────
        String destinatario = (targetUsername == null || targetUsername.equals("ALL"))
                ? "Todos" : targetUsername;
        String mensajeRealTime = serializer.buildSuccessResponse(
                JsonSchema.ACTION_NEW_MESSAGE,
                "De " + fromUser + " → " + destinatario + ": " + content);

        // ── Modo A: Broadcast (a todos) ──────────────────────────────────────────
        if (targetUsername == null || targetUsername.isEmpty() || targetUsername.equals("ALL")) {
            // Broadcast local a clientes de este servidor
            broadcastManager.broadcast(mensajeRealTime);
            // FederatedHook lo propaga a los peers (ya configurado en BroadcastManager)

            // Replicar el evento para que los peers actualicen su copia
            if (replicationManager != null && localNodeId != null) {
                replicationManager.propagate(
                        ReplicationEvent.newMessage(localNodeId, fromUser, content));
            }

            // Broadcast de logs actualizados
            broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

            return serializer.buildSuccessResponse(JsonSchema.ACTION_SEND_MESSAGE,
                    "Mensaje broadcast enviado a todos los clientes.");
        }

        // ── Modo B: Mensaje dirigido ─────────────────────────────────────────────
        boolean delivered = false;

        if (routingTable != null) {
            String nodeId = routingTable.resolveNode(targetUsername);

            if (nodeId == null) {
                // Cliente no encontrado en ningún servidor
                return serializer.buildErrorResponse(
                        "El cliente '" + targetUsername + "' no está conectado.");
            }

            if (localNodeId != null && localNodeId.equals(nodeId)) {
                // Cliente LOCAL: entrega directa
                if (localClientRegistry != null) {
                    delivered = localClientRegistry.deliver(targetUsername, mensajeRealTime);
                }
            } else {
                // Cliente REMOTO: reenviar al servidor peer via PEER_ROUTE
                if (remoteDelivery != null) {
                    remoteDelivery.deliver(mensajeRealTime, targetUsername);
                    delivered = true;
                }
            }
        } else {
            // Sin cluster: intentar entrega local directa
            if (localClientRegistry != null) {
                delivered = localClientRegistry.deliver(targetUsername, mensajeRealTime);
            }
        }

        // Notificar al remitente el resultado
        broadcastManager.broadcast(listLogsHandler.handle(null, clientIp));

        if (delivered) {
            return serializer.buildSuccessResponse(JsonSchema.ACTION_SEND_MESSAGE,
                    "Mensaje entregado a '" + targetUsername + "'.");
        } else {
            return serializer.buildErrorResponse(
                    "No se pudo entregar el mensaje a '" + targetUsername + "'.");
        }
    }
}

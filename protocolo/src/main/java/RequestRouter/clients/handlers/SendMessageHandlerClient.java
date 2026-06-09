package RequestRouter.clients.handlers;

import com.universidad.messaging.server.shared.schema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import MessageParser.BroadcastManager;
import com.universidad.messaging.server.gestion.cluster.api.IReplicationManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.ILogManager;
import com.universidad.messaging.server.servicios.api.IUserManager;
import com.universidad.messaging.server.shared.events.ReplicationEvent;

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
public class SendMessageHandlerClient implements ClientActionHandler {

    // Dependencias Base
    private final IUserManager userManager;
    private final IDocumentManager documentManager;
    private final ILogManager logManager;
    private final BroadcastManager broadcastManager;
    private final ResponseBuilder serializer;
    private final ClientActionHandler listLogsHandler;
    private final ClientActionHandler listMessagesHandler;

    private final IReplicationManager replicationManager;
    private final String localNodeId;

    // Un solo constructor que exige TODO
    public SendMessageHandlerClient(IUserManager userManager, IDocumentManager documentManager,
                                    ILogManager logManager, BroadcastManager broadcastManager,
                                    ResponseBuilder serializer, ClientActionHandler listLogsHandler,
                                    ClientActionHandler listMessagesHandler,
                                    IReplicationManager replicationManager,
                                    String localNodeId) {
        this.userManager = userManager;
        this.documentManager = documentManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
        this.serializer = serializer;
        this.listLogsHandler = listLogsHandler;
        this.listMessagesHandler = listMessagesHandler;
        this.replicationManager = replicationManager;
        this.localNodeId = localNodeId;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        String fromUser      = payload.get("username").asText();
        String content       = payload.get("message").asText();
        String targetUsername = payload.has(JsonSchema.PAYLOAD_TARGET_USERNAME)
                ? payload.get(JsonSchema.PAYLOAD_TARGET_USERNAME).asText() : null;

        long userId = userManager.obtenerIdUsuario(fromUser);
        boolean isBroadcast = (targetUsername == null || targetUsername.isEmpty() || targetUsername.equals("ALL"));


        String docType = isBroadcast ? "MESSAGE" : "PRIVATE_TO:" + targetUsername;
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream textStream = new ByteArrayInputStream(contentBytes);
        String nombreArchivo   = "msg_" + fromUser + "_" + System.currentTimeMillis() + ".txt";

        documentManager.procesarRecepcionDocumento(
                textStream, nombreArchivo, contentBytes.length, ".txt", "text/plain",
                userId, clientIp, docType);


        broadcastManager.broadcast(listMessagesHandler.handle(null, null));

        replicationManager.propagate(
                    ReplicationEvent.newMessage(localNodeId, fromUser, targetUsername, content, clientIp));


        String logDetail = isBroadcast
                ? "Mensaje de " + fromUser + " (broadcast)"
                : "Mensaje de " + fromUser + " a " + targetUsername;
        logManager.registrarAccion(null, userId, "SEND_MESSAGE", "SUCCESS", logDetail);


        String logUpdate = listLogsHandler.handle(null, clientIp);
        broadcastManager.broadcast(logUpdate);


        return serializer.buildSuccessResponse(
                JsonSchema.ACTION_SEND_MESSAGE,
                "De " + fromUser + " → " + targetUsername + ": " + content);
    }
}
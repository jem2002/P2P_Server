package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import RequestRouter.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import communication.InterServerProtocol;
import discovery.MembershipList;
import events.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Maneja la acción LIST_PEER_LOGS: recolecta y retorna los logs de todos los
 * servidores peer vivos en la red.
 *
 * Mecanismo:
 *   1. Para cada peer ALIVE, abre una conexión TCP temporal al cluster port.
 *   2. Envía PEER_LOGS_REQUEST.
 *   3. Espera PEER_LOGS_RESPONSE con los logs del peer.
 *   4. Consolida todo en una respuesta JSON.
 *
 * Requerimiento cumplido:
 *   - "Adicionar servicios para mostrar la información y los logs de otros servidores."
 */
public class ListPeerLogsHandler implements ActionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ListPeerLogsHandler.class);
    private static final int PEER_TIMEOUT_MS = 3000;
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ResponseBuilder serializer;
    private final MembershipList membershipList;
    private final String localNodeId;
    private final Supplier<String> localLogsSupplier;

    public ListPeerLogsHandler(ResponseBuilder serializer,
                                MembershipList membershipList,
                                String localNodeId,
                                Supplier<String> localLogsSupplier) {
        this.serializer = serializer;
        this.membershipList = membershipList;
        this.localNodeId = localNodeId;
        this.localLogsSupplier = localLogsSupplier;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<Map<String, Object>> allLogs = new ArrayList<>();

        // 1. Logs locales de este servidor
        Map<String, Object> localEntry = new HashMap<>();
        localEntry.put("nodeId", localNodeId);
        localEntry.put("tipo", "LOCAL");
        localEntry.put("logs", localLogsSupplier.get());
        allLogs.add(localEntry);

        // 2. Solicitar logs a cada peer ALIVE
        for (NodeInfo peer : membershipList.getAliveNodes()) {
            String peerLogs = requestLogsFromPeer(peer);
            Map<String, Object> peerEntry = new HashMap<>();
            peerEntry.put("nodeId", peer.getNodeId());
            peerEntry.put("tipo", "REMOTO");
            peerEntry.put("logs", peerLogs != null ? peerLogs : "{\"error\":\"sin respuesta\"}");
            allLogs.add(peerEntry);
        }

        return serializer.buildObjectListResponse(JsonSchema.ACTION_LIST_PEER_LOGS, allLogs, "peerLogs");
    }

    /**
     * Abre una conexión TCP temporal al peer, envía PEER_LOGS_REQUEST y espera
     * PEER_LOGS_RESPONSE. Timeout: PEER_TIMEOUT_MS ms.
     */
    private String requestLogsFromPeer(NodeInfo peer) {
        try (Socket socket = new Socket(peer.getHost(), peer.getClusterPort())) {
            socket.setSoTimeout(PEER_TIMEOUT_MS);

            // Enviar solicitud
            String request = InterServerProtocol.buildLogsRequest(localNodeId) + "\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            // Leer respuesta (una línea JSON)
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();

            if (line != null && line.contains("PEER_LOGS_RESPONSE")) {
                JsonNode root = mapper.readTree(line);
                if (root.has("payload") && root.get("payload").has("logsJson")) {
                    return root.get("payload").get("logsJson").asText();
                }
            }
            return line;

        } catch (Exception e) {
            logger.warn("No se pudieron obtener logs del peer '{}@{}:{}': {}",
                    peer.getNodeId(), peer.getHost(), peer.getClusterPort(), e.getMessage());
            return null;
        }
    }
}

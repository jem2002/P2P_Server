package RequestRouter.handlers;

import JsonSerializer.ResponseBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import models.LocalNodeInfo;
import ports.api.ActionHandler;
import replication.ReplicationEvent;
import replication.ReplicationManager;

public class UploadConfirmationHandler implements ActionHandler {

    private final ReplicationManager replicationManager;
    private final LocalNodeInfo localNodeInfo;
    private final ResponseBuilder serializer;

    public UploadConfirmationHandler(ReplicationManager replicationManager, LocalNodeInfo localNodeInfo, ResponseBuilder serializer) {
        this.replicationManager = replicationManager;
        this.localNodeInfo = localNodeInfo;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {


        String filename = payload.has("filename") ? payload.get("filename").asText() : "";
        long sizeBytes = payload.has("sizeBytes") ? payload.get("sizeBytes").asLong() : 
                         (payload.has("size") ? payload.get("size").asLong() : 0);
        String extension = payload.has("extension") ? payload.get("extension").asText() : "";
        String mimeType = payload.has("mimeType") ? payload.get("mimeType").asText() : "";
        String username = payload.has("username") ? payload.get("username").asText() : "";

        replicationManager.propagate(
                ReplicationEvent.documentUploaded(
                        localNodeInfo.getNodeId(),
                        0, // docId (0 if not known)
                        filename,
                        sizeBytes,
                        extension,
                        mimeType,
                        "FILE", // default docType
                        username,
                        localNodeInfo.getHost(), // host
                        localNodeInfo.getClientPort() // clientPort
                )
        );

        if (serializer != null) {
            return serializer.buildSuccessResponse("UPLOAD_CONFIRMATION", "Upload confirmed and replicated.");
        }
        return "{\"status\":\"SUCCESS\"}";
    }
}

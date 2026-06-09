package replication.handlers;

import com.universidad.messaging.server.shared.events.ReplicationEvent;
import com.universidad.messaging.server.servicios.api.ICommentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import replication.IReplicationEventHandler;

import java.math.BigDecimal;

public class NewCommentReplication implements IReplicationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(NewCommentReplication.class);
    private final ICommentManager commentManager;

    public NewCommentReplication(ICommentManager commentManager) {
        this.commentManager = commentManager;
    }

    @Override
    public void apply(ReplicationEvent event) throws Exception {

        Long id = event.getPayload().get("id").asLong();
        Long documentId = event.getPayload().get("documentId").asLong();
        String username = event.getPayload().get("username").asText();
        String content = event.getPayload().get("content").asText();
        String sentiment = event.getPayload().get("sentiment").asText();
        BigDecimal confidence = event.getPayload().get("confidence").decimalValue();

        commentManager.replicarComentario(id, documentId, username, content, sentiment, confidence);

        logger.info("Comentario registrado con ID: {} por el usuario: {}", id, username);

    }


}



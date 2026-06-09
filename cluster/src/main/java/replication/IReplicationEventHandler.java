package replication;

import com.universidad.messaging.server.shared.events.ReplicationEvent;

public interface IReplicationEventHandler {

    void apply(ReplicationEvent event) throws Exception;

}

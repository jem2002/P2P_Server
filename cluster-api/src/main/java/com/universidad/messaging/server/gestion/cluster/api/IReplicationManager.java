package com.universidad.messaging.server.gestion.cluster.api;

import com.universidad.messaging.server.shared.events.ReplicationEvent;

public interface IReplicationManager {

    void propagate(ReplicationEvent event);


}

package events.Impl;

import com.universidad.messaging.server.servicios.api.IUserManager;
import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDisconnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeDisconnector.class);


    private final PeerConnectionPool peerConnectionPool;
    private final IUserManager userManager;

    public NodeDisconnector(PeerConnectionPool peerConnectionPool, IUserManager userManager) {
        this.peerConnectionPool = peerConnectionPool;
        this.userManager = userManager;
    }


    @Override
    public void onNodeLeft(RemoteNodeInfo node) {
         peerConnectionPool.disconnectFromPeer(node);
         
         if (userManager != null) {
             userManager.desconectarClientesPorNodo(node.getNodeId());
         }
    }



}

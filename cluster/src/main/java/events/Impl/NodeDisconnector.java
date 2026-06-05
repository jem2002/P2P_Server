package events.Impl;

import UserService.UserManager;
import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDisconnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeDisconnector.class);


    private final PeerConnectionPool peerConnectionPool;
    private final UserManager userManager;

    public NodeDisconnector(PeerConnectionPool peerConnectionPool, UserManager userManager) {
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

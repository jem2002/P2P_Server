package events.Impl;

import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeDisconnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeDisconnector.class);


    private final PeerConnectionPool peerConnectionPool;

    public NodeDisconnector(PeerConnectionPool peerConnectionPool){
        this.peerConnectionPool = peerConnectionPool;
    }


    @Override
    public void onNodeLeft(RemoteNodeInfo node) {
         peerConnectionPool.disconnectFromPeer(node);

    }



}

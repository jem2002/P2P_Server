package events.Impl;

import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import topology.RoutingTable;

public class NodeDisconnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeDisconnector.class);


    private final PeerConnectionPool peerConnectionPool;
    private final RoutingTable routingTable;

    public NodeDisconnector(PeerConnectionPool peerConnectionPool, RoutingTable routingTable){
        this.peerConnectionPool = peerConnectionPool;
        this.routingTable = routingTable;
    }


    @Override
    public void onNodeLeft(RemoteNodeInfo node) {
        boolean isDisconnected = peerConnectionPool.disconnectFromPeer(node);

        if(isDisconnected){
            routingTable.removeClientsFromNode(node);
        }

    }



}

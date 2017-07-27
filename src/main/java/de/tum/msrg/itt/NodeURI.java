package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;

import java.io.Serializable;

/**
 * Created by pxsalehi on 30.06.16.
 */
public class NodeURI implements Serializable {
    private CommSystem.CommSystemType type;
    private String host;
    private int port;
    private String ID;

    public NodeURI() {}

    public NodeURI(CommSystem.CommSystemType type, String host, int port, String ID) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.ID = ID;
    }

    public NodeURI(String host, int port, String ID) {
        this.host = host;
        this.port = port;
        this.ID = ID;
    }

    public NodeURI(NodeAddress nodeAddress) {
        type = nodeAddress.getType();
        host = nodeAddress.getHost();
        port = nodeAddress.getPort();
        ID = nodeAddress.getNodeID();
    }

    public static NodeURI parse(String uri) throws BrokerCoreException {
        try { // try socket
            SocketAddress socketAddress = new SocketAddress(uri);
            return new NodeURI(socketAddress);
        } catch (CommunicationException e) {
            // not a socket or malformed
        }
        try {
            RMIAddress rmiAddress = new RMIAddress(uri);
            return new NodeURI(rmiAddress);
        } catch(CommunicationException e) {
            // not an rmi address ot malformed
        }
        throw new BrokerCoreException("Cannot parse URI: " + uri);
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public CommSystem.CommSystemType getType() {
        return type;
    }

    public void setType(CommSystem.CommSystemType type) {
        this.type = type;
    }

    // returns true if to uris have the same host and port
    public boolean equalAddress(NodeURI other) {
        return host.equalsIgnoreCase(other.host) && port == other.port;
    }

    public boolean equalNode(NodeURI other) {
        return host.equalsIgnoreCase(other.host) && port == other.port && ID.equals(other.ID);
    }

    public String getURI() {
        return String.format("%s://%s:%d/%s", type.toString().toLowerCase(), host, port, ID);
    }

    @Override
    public String toString() {
        return getURI();
    }
}

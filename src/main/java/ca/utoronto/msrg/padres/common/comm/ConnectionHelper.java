package ca.utoronto.msrg.padres.common.comm;

import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.zero.ZeroAddress;

import java.net.*;
import java.util.Enumeration;

/**
 * Created by chris on 25.09.15.
 */
public class ConnectionHelper {
    /**
     * Build a node URI string with the given parameters
     *
     * @param commProtocol
     *            communication protocol
     * @param hostname
     *            hostname of the node
     * @param port
     *            port number of the server running at the node
     * @param nodeID
     *            the ID of the node
     * @return The node URI
     */
    public static String makeURI(String commProtocol, String hostname, int port, String nodeID) {
        return String.format("%s://%s:%d/%s", commProtocol, hostname, port, nodeID);
    }

    /**
	 * Similar to the constructor {@link #NodeAddress(String)}, but implemented to be called
	 * statically.
	 *
	 * @param nodeURI
	 *            A protocol-specific server URI
	 * @return A protocol-specific node address, subclass of NodeAddress
	 * @throws CommunicationException
	 *             When there is an error parsing the URI
	 * @see CommSystem.CommSystemType
	 */
	public static INodeAddress getAddress(String nodeURI) throws CommunicationException {
		if (nodeURI == null) {
			throw new CommunicationException("Null URI given");
		}
		CommSystem.CommSystemType commType = CommSystem.CommSystemType.getType(nodeURI);
		switch (commType) {
		case RMI:
			return new RMIAddress(nodeURI);
		case SOCKET:
			return new SocketAddress(nodeURI);
        case ZERO:
            return new ZeroAddress(nodeURI);
		default:
			throw new CommunicationException("Communication system type not recognized");
		}
	}

    public static String getLocalIPAddr(String localIPAddress) throws CommunicationException {
        if (localIPAddress != null && localIPAddress.length() != 0)
            return localIPAddress;
        try {
            InetAddress localaddr = InetAddress.getLocalHost();
            localIPAddress = localaddr.getHostAddress();
            if (!localIPAddress.startsWith("127")) {
                return localIPAddress;
            } else {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface iface = en.nextElement();
                    for (InterfaceAddress inAddr : iface.getInterfaceAddresses()) {
                        InetAddress addr = inAddr.getAddress();
                        if (addr.getAddress().length == 4 && !addr.isLoopbackAddress())
                            localIPAddress = addr.getHostAddress();
                    }
                }
            }
        } catch (UnknownHostException e) {
            throw new CommunicationException("Error in getting local IP: " + e.getMessage(), e);
        } catch (SocketException e) {
            throw new CommunicationException("Error in getting local IP: " + e.getMessage(), e);
        }
        if (localIPAddress.length() == 0)
            throw new CommunicationException("Could not find local IP address");
        return localIPAddress;
    }
}

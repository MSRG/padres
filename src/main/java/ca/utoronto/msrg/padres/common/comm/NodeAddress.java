package ca.utoronto.msrg.padres.common.comm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-11
 * 
 *         Base class for managing host URIs of different communication protocols. Every
 *         communication protocol has to implement its own address class inherited from this
 *         abstract class.
 * 
 */
public abstract class NodeAddress {

	/**
	 * This string defines the protocol-specific format of server URIs. It should have the format of
	 * <protocol>:://<....>. The exact format must be defined by the subclasses extending this
	 * abstract class. This string must be a Regular Expression.
	 * 
	 * @see CommSystemType
	 */
	protected CommSystemType type;

	protected String host;

	protected int port;

	protected String remoteID;

	/**
	 * Constructor to create the node address from the given protocol specific URI string
	 * 
	 * @param nodeURI
	 *            protocol-specific URI string
	 * @throws CommunicationException
	 *             when there is a problem parsing the URI
	 * @see CommSystemType
	 */
	public NodeAddress(String nodeURI) throws CommunicationException {
		parseURI(nodeURI);
		// if the host component refers to a loopback address, convert it to the local IP address.
		// this is necessary for making the URI universally accessible (in other machines)
		if (CommSystem.isLocalAddress(host)) {
			host = CommSystem.getLocalIPAddr();
		}
	}

	public CommSystemType getType() {
		return type;
	}

	public String getNodeURI() {
		return toString();
	}

	public void convertLocalhostToIPAddress() throws CommunicationException {
		if (CommSystem.isLocalAddress(host))
			host = CommSystem.getLocalIPAddr();
	}

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
	 * @see CommSystemType
	 */
	public static NodeAddress getAddress(String nodeURI) throws CommunicationException {
		if (nodeURI == null) {
			throw new CommunicationException("Null URI given");
		}
		CommSystemType commType = CommSystemType.getType(nodeURI);
		switch (commType) {
		case RMI:
			return new RMIAddress(nodeURI);
		case SOCKET:
			return new SocketAddress(nodeURI);
		default:
			throw new CommunicationException("Communication system type not recognized");
		}
	}

	/**
	 * This is a helper method to parse a protocol-specific URI. To be used by {@link #parseURI()}
	 * 
	 * @param nodeURI
	 *            protocol-specific URI
	 * @return A matcher object, which can be used in parsing.
	 * @throws CommunicationException
	 *             If the nodeURI type is not recognized
	 */
	protected static Matcher getMatch(String nodeURI) throws CommunicationException {
		String addressFormat = null;
		CommSystemType commType = CommSystemType.getType(nodeURI);
		switch (commType) {
		case RMI:
			addressFormat = RMIAddress.RMI_REG_EXP;
			break;
		case SOCKET:
			addressFormat = SocketAddress.SOCKET_REG_EXP;
			break;
		default:
			throw new CommunicationException("Address format not recognized");
		}
		Pattern addrPattern = Pattern.compile(addressFormat);
		return addrPattern.matcher(nodeURI);
	}

	/**
	 * Checks whether a given server URI follows the protocol-specific format
	 * 
	 * @param nodeURI
	 *            The server URI to be checked for format
	 * @return true if the format is verified correct; false otherwise
	 * @throws CommunicationException
	 *             if nodeURI communication type is not recognized
	 * @see #getMatch(String)
	 */
	public static boolean checkFormat(String nodeURI) throws CommunicationException {
		return getMatch(nodeURI).find();
	}

	/**
	 * Parse the nodeURI using the format specified by protocol specific address format. Use the
	 * method {@link #getMatch(String)} when implementing this method.
	 * 
	 * @throws CommunicationException
	 *             When there is an error in the nodeURI format
	 */
	protected abstract void parseURI(String nodeURI) throws CommunicationException;

	/**
	 * Checks whether a given server URI is functionally same as the URI defined here. A string
	 * matching is not enough to verify the functional equivalence. For example, {@link #host} can
	 * be an IP address in one place, while it is the equivalent hostname in the other.
	 * 
	 * @param nodeURI
	 *            A server URI
	 * @return true if the given URI is functionally the same as the node address here; false
	 *         otherwise.
	 * @throws CommunicationException
	 *             When there is an error in parsing the URI or some network-based resolution
	 *             produces an error message
	 */
	public abstract boolean isEqual(String nodeURI) throws CommunicationException;

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + type.hashCode();
		hash = 31 * hash + host.hashCode();
		hash = 31 * hash + port;
		hash = 31 * hash + remoteID.hashCode();
		return hash;
	}

}

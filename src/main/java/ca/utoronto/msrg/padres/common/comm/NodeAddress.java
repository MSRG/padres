package ca.utoronto.msrg.padres.common.comm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.zero.ZeroAddress;

import static ca.utoronto.msrg.padres.common.comm.ConnectionHelper.getLocalIPAddr;

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
			host = getLocalIPAddr(host);
		}
	}

	public CommSystemType getType() {
		return type;
	}

	public String getNodeURI() {
		return toString();
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

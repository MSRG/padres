package ca.utoronto.msrg.padres.common.comm;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageSender;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServer;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketMessageSender;
import ca.utoronto.msrg.padres.common.comm.socket.SocketServer;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-11
 * 
 *         The class to provide the communication layer for the PADRES components. This object is
 *         not protocol-specific, but the objects it creates are. Only this object and the
 *         {@link MessageSender} objects are exposed to the upper layer. Messages from communication
 *         layer to the upper layer are conveyed via {@link MessageListenerInterface}. CommSystem
 *         objects created by Brokers can instantiate servers to accept connections and messages.
 * 
 */
public class CommSystem {

	/**
	 * @author Bala Maniymaran
	 * 
	 *         Created: 2010-08-11
	 * 
	 *         Communication System Type. Add more type here as more communication protocols are
	 *         implemented.
	 * 
	 */
	public enum CommSystemType {
		RMI, SOCKET;

		/**
		 * Given a URI string, it returns the communication sytsem type.
		 * 
		 * @param uri
		 *            The server URI. It must have the format of <comm_sys_type>://<...>
		 * @return Communication system type of the server URI
		 * @throws CommunicationException
		 *             When the communication protocol is not recognised. Either because of
		 *             malformatted URI or because the specific protocol is not available yet.
		 */
		public static CommSystemType getType(String uri) throws CommunicationException {
			if (uri.toLowerCase().startsWith("rmi"))
				return RMI;
			else if (uri.toLowerCase().startsWith("socket"))
				return SOCKET;
			else
				throw new CommunicationException("Unrecognized communication protocol: " + uri);
		}

	}

	/**
	 * @author Bala Maniymaran
	 * 
	 *         Created: 2010-08-11 The host type can be either a server or client.
	 */
	public enum HostType {
		SERVER, CLIENT
	}

	protected static Logger commInterfaceLogger = Logger.getLogger(CommSystem.class);

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	private static String localIPAddress;

	/**
	 * All the IPv4 addresses of the local machine. It is initiated inside the constructor
	 * 
	 * @see #findLocalIPv4Addresses()
	 */
	protected static List<String> localIPAddresses;

	/**
	 * The first server instantiated by the communication layer. When no server is specified, the
	 * actions take place at this server.
	 */
	protected CommServer defaultServer = null;

	/**
	 * All the connection accepting servers created by the communication layer. Identified (map key)
	 * by their specific ID given at the time of creation. These servers can be of different
	 * communication types (RMI, socket, etc.)
	 */
	protected Map<NodeAddress, CommServer> listenServers;
	
	private boolean shutdown = false;

	/**
	 * Initiates the data structures. Finds all the local IPv4 address.
	 * 
	 * @throws CommunicationException
	 *             When there is an error in finding the IPv4 addresses
	 */
	public CommSystem() throws CommunicationException {
		defaultServer = null;
		listenServers = new HashMap<NodeAddress, CommServer>();
		findLocalIPv4Addresses();
	}

	/**
	 * Finds all the local IPv4 addresses.
	 * 
	 * @throws CommunicationException
	 *             When it could not find the addresses
	 */
	protected static synchronized void findLocalIPv4Addresses() throws CommunicationException {
		localIPAddresses = new ArrayList<String>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					// check the length of the address to allow only IPv4 address
					byte[] rawAddr = address.getAddress();
					if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()
							&& rawAddr.length == 4) {
						localIPAddresses.add(address.toString());
					}
				}
			}
		} catch (Exception e) {
			commInterfaceLogger.fatal("Can't get local host address: " + e);
			exceptionLogger.fatal("Can't get local host address: " + e);
			throw new CommunicationException("Can't get local host address", e);
		}
	}

	/**
	 * To find the URI of the default server
	 * 
	 * @return The URI of the default server
	 * @throws CommunicationException
	 */
	public String getServerURI() throws CommunicationException {
		return defaultServer.getServerURI();
	}

	public NodeAddress getServerAddress() throws CommunicationException {
		return defaultServer.getAddress();
	}

	/**
	 * Checks whether a given server URI is the same as the default server's
	 * 
	 * @param remoteURI
	 *            A server URI to be checked with
	 * @return true if the URIs are the same, false otherwise
	 * @throws CommunicationException
	 *             Either default server is not initiated or given URI is malformatted or of
	 *             unrecognized communication protocol.
	 */
	public boolean isEqualServerURI(String remoteURI) throws CommunicationException {
		if (defaultServer == null) {
			throw new CommunicationException(
					"No server is instantiated in the communication system");
		}
		return defaultServer.isSameURI(remoteURI);
	}

	/**
	 * To create a server that listens for connections and messages from other entities.
	 * 
	 * @param serverID
	 *            A unique identifier for the server. It must be unique in the whole system, but the
	 *            uniqueness is not verified by the system.
	 * @param serverURI
	 *            URI of the server. It should specify the communication protocol type, IP address,
	 *            and port number
	 * @throws CommunicationException
	 *             it is thrown when
	 *             <ul>
	 *             <li>A server with the same ID already exists in the exisiting list of servers</li>
	 *             <li>The communication protocol is not recognized</li>
	 *             <li>There is a problem in created the specific type of communication server</li>
	 *             </ul>
	 * @see CommSystemType
	 */
	public void createListener(String serverURI) throws CommunicationException {
		NodeAddress serverAddress = NodeAddress.getAddress(serverURI);
		if (listenServers.containsKey(serverAddress)) {
			throw new CommunicationException("A server with the URI " + serverAddress.toString()
					+ " already exists");
		}
		CommServer server = null;
		switch (serverAddress.getType()) {
		case RMI:
			server = createNewRMIServer((RMIAddress) serverAddress);
			break;
		case SOCKET:
			server = createNewSocketServer((SocketAddress) serverAddress);
			break;
		default:
			throw new CommunicationException("Communication system type not recognized");
		}
		if (defaultServer == null) {
			defaultServer = server;
		}
		listenServers.put(serverAddress, server);
	}

	/**
	 * Add a message listener to a specific server. All the messages received by the server is
	 * passed on to this message listener.
	 * 
	 * @param serverURI
	 *            The URI of the server to which the message listener to be added
	 * @param msgListener
	 *            The message listener to be added
	 * @throws CommunicationException
	 *             if supplied serverURI is malformated.
	 */
	public void addMessageListener(String serverURI, MessageListenerInterface msgListener)
			throws CommunicationException {
		NodeAddress serverAddress = NodeAddress.getAddress(serverURI);
		listenServers.get(serverAddress).addMessageListener(msgListener);
	}

	/**
	 * Similar to {@link #addMessageListener(String, MessageListenerInterface)}, but adds the
	 * message listener to all the servers.
	 * 
	 * @param msgListener
	 *            The message listener to be added.
	 */
	public void addMessageListener(MessageListenerInterface msgListener) {
		for (CommServer server : listenServers.values()) {
			server.addMessageListener(msgListener);
		}
	}

	/**
	 * Adds a connections listener to a specific server. When the server receives a connection
	 * request from a client, the given connection listener is informed.
	 * 
	 * @param serverURI
	 *            The URI of the server to which the listener is to be added.
	 * @param connectListener
	 *            The connection listener to be added.
	 * @throws CommunicationException
	 *             If given serverURI is malformatted
	 */
	public void addConnectionListener(String serverURI, ConnectionListenerInterface connectListener)
			throws CommunicationException {
		NodeAddress serverAddress = NodeAddress.getAddress(serverURI);
		listenServers.get(serverAddress).addConnectionListener(connectListener);
	}

	/**
	 * Similar to {@link #addConnectionListener(String, ConnectionListenerInterface)}, but adds the
	 * connection listener to all the servers.
	 * 
	 * @param connectListener
	 *            The connection listener to be added.
	 */
	public void addConnectionListener(ConnectionListenerInterface connectListener) {
		for (CommServer server : listenServers.values()) {
			server.addConnectionListener(connectListener);
		}
	}

	/**
	 * Returns a message sender, with which the upper level components can send messages to a remote
	 * entity. The communication type of the message sender returned by this method varies depends
	 * on the URI of the remote entity to which the connection to be made.
	 * 
	 * @param remoteServerURI
	 *            The URI of the remote entity's server.
	 * @return An object that is the child class of {@link MessageSender}. The actual type depends
	 *         on remoteServerURI
	 * @throws CommunicationException
	 *             Either there is an error in given URI or an error occurred while creating the
	 *             MessageSender
	 * @see CommSystemType
	 */
	public MessageSender getMessageSender(String remoteServerURI) throws CommunicationException {
		NodeAddress remoteServerAddress = NodeAddress.getAddress(remoteServerURI);
		switch (remoteServerAddress.getType()) {
		case RMI:
			return createRMIMessageSender((RMIAddress) remoteServerAddress);
		case SOCKET:
			return createSocketMessageSender((SocketAddress) remoteServerAddress);
		default:
			throw new CommunicationException("Communication system type not recognized");
		}
	}

	/**
	 * Check whether a given string has the format of an IP address
	 * 
	 * @param addr
	 *            The string to be checked
	 * @return true if the string is an IP address; false otherwise
	 */
	public static boolean isIPAddress(String addr) {
		return addr.matches("\\d+(\\.\\d+)+");
	}

	public static String getLocalIPAddr() throws CommunicationException {
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

	/**
	 * Checks whether a given hostname belongs to the local machine.
	 * 
	 * @param hostname
	 *            The hostname to check
	 * @return true if the given hostname refers to a loopback name ("localhost") or resolved to one
	 *         of the local IP addresses; false otherwise
	 * @throws CommunicationException
	 *             if the given hostname could not be resolved to an IP address
	 */
	public static synchronized boolean isLocalAddress(String hostname) throws CommunicationException {
		try {
			InetAddress addr = InetAddress.getByName(hostname);
			if (addr.isLoopbackAddress()) {
				return true;
			}
			if(localIPAddresses == null)
				findLocalIPv4Addresses();
			for (String localAddr : localIPAddresses) {
				if (localAddr.equals(addr.toString())) {
					return true;
				}
			}
		} catch (UnknownHostException e) {
			throw new CommunicationException(e);
		}
		return false;
	}

	public void shutDown() throws CommunicationException {
		if(shutdown)
			return;
		else
			shutdown = true;
		
		for (CommServer server : listenServers.values()) {
			server.shutDown();
		}
	}

	protected RMIServer createNewRMIServer(RMIAddress serverAddress)
			throws CommunicationException {
		return new RMIServer(serverAddress, this);
	}

	protected SocketServer createNewSocketServer(SocketAddress serverAddress)
			throws CommunicationException {
		return new SocketServer(serverAddress, this);
	}

	protected RMIMessageSender createRMIMessageSender(
			RMIAddress remoteServerAddress) throws CommunicationException {
		return new RMIMessageSender(remoteServerAddress);
	}

	protected SocketMessageSender createSocketMessageSender(
			SocketAddress remoteServerAddress) {
		return new SocketMessageSender(remoteServerAddress);
	}
}

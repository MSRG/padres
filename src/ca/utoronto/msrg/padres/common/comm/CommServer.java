package ca.utoronto.msrg.padres.common.comm;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-11
 * 
 *         The parent class of communication server of any communication protocol. All the servers
 *         implementing different communication protocols have to be extended from this class.
 *         Communication servers accepts new connections and messages. When a connection is made
 *         from a broker, it allows the connection and let messages to be received via the
 *         connection. But when a connection is made from a client, in addition to allowing messages
 *         to be received via the connection, the connection is also informed to the upper layer
 *         using registered {@link ConnectionListenerInterface}.
 * 
 * 
 */
public abstract class CommServer {

	protected static Logger commInterfaceLogger = Logger.getLogger(CommSystem.class);

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	protected NodeAddress serverAddress;

	protected List<MessageListenerInterface> msgListeners;

	protected List<ConnectionListenerInterface> connectListeners;

	/**
	 * Constructor for the server. It parse the given serverURI into the {@link #serverAddress}, but
	 * it does not actually create the server that accepts the connections and messages. It has to
	 * be handled in the constructors of the subclasses.
	 * 
	 * @param serverID
	 *            The ID of the server
	 * @param serverURI
	 *            The URI of the server
	 * @throws CommunicationException
	 *             When there is error is parsing the given URI
	 * @see NodeAddress#getAddress(String)
	 */
	public CommServer(NodeAddress serverAddress) throws CommunicationException {
		this.serverAddress = serverAddress;
		msgListeners = new ArrayList<MessageListenerInterface>();
		connectListeners = new ArrayList<ConnectionListenerInterface>();
	}

	public NodeAddress getAddress() throws CommunicationException {
		return serverAddress;
	}

	public String getServerURI() throws CommunicationException {
		return serverAddress.getNodeURI();
	}

	public boolean isSameURI(String tempURI) throws CommunicationException {
		NodeAddress tempAddress = NodeAddress.getAddress(tempURI);
		return serverAddress.equals(tempAddress);
	}

	public List<MessageListenerInterface> getMessageListeners() {
		return msgListeners;
	}

	public List<ConnectionListenerInterface> getConnectionListeners() {
		return connectListeners;
	}

	/**
	 * Registers a connection listener with this server. Connection listeners must be informed by
	 * the server when a connection request is made by a new client. Broker connections are not
	 * informed to the connection listeners.
	 * 
	 * @param connectListener
	 *            The connection listener to be informed of new incoming client connections.
	 */
	public void addConnectionListener(ConnectionListenerInterface connectListener) {
		this.connectListeners.add(connectListener);
	}

	/**
	 * Registers a message listener with this server. Message listeners are informed of all the
	 * messages received at this server.
	 * 
	 * @param msgListener
	 *            The message listener to be informed of incoming messages
	 */
	public void addMessageListener(MessageListenerInterface msgListener) {
		this.msgListeners.add(msgListener);
	}

	public abstract void shutDown() throws CommunicationException;

}

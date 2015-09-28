package ca.utoronto.msrg.padres.common.comm;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This class provides an interfaces to connect and send messages to remote entities, either
 *         brokers (with servers) or clients (without servers.) Each communication protocol must
 *         provide its own implementation of this abstract class.
 * 
 */
public abstract class MessageSender {

	private static final int RETRY_LIMIT = 30;

	private static final int RETRY_PAUSE_TIME = 10;

	protected static Logger messagePathLogger = Logger.getLogger("MessagePath");

	protected static Logger commSysLogger = Logger.getLogger(CommSystem.class);

	/**
	 * The number of times to retry connecting to a server before throwing a connection failure
	 * exception
	 */
	protected static int connectRetryLimit = RETRY_LIMIT;

	/**
	 * The amount of pause time between two connection retries
	 */
	protected static int connectRetryPauseTime = RETRY_PAUSE_TIME;

	/**
	 * The URI of the remote server to connect; it is null when connected to a client
	 */
	protected NodeAddress remoteServerAddress = null;

	/**
	 * To mark whether a connection is active. Child classes must make sure to set/unset this
	 * variable correctly
	 */
	protected boolean connected;

	/**
	 * Constructor to be used when connecting to a server.
	 * 
	 * @param serverURI
	 *            The URI of the remote server to connect
	 */
	public MessageSender(NodeAddress remoteAddress) {
		remoteServerAddress = remoteAddress;
		connected = false;
	}

	public static void setConnectRetryLimit(int retryLimit) {
		connectRetryLimit = retryLimit;
	}

	public static void setConnectRetryPauseTime(int retryPauseTime) {
		connectRetryPauseTime = retryPauseTime;
	}

	public boolean isConnected() {
		return connected;
	}

	/**
	 * To connect to a server in unidirectional mode. In PADRES, this is used in Broker-->Broker
	 * communication
	 * 
	 * @throws CommunicationException
	 */
	public abstract void connect() throws CommunicationException;

	/**
	 * To connect to a server in bidirectional mode. In PADRES, this is used in Client-->Broker
	 * communication
	 * 
	 * @param sourceDest
	 *            Destination ID of the entity making the connection
	 * @param msgListener
	 *            A message handler which had to be informed when a message is received via the
	 *            connection to be made
	 * @throws CommunicationException
	 *             The situation when this exception is thrown depends on the exact implementation
	 *             of this method
	 */
	public abstract void connect(MessageDestination sourceDest, MessageListenerInterface msgListener)
			throws CommunicationException;

	/**
	 * To get the ID of the remote entity with which this MessageSender is communicating with
	 * 
	 * @return The ID of the remote entity
	 * @throws CommunicationException
	 */
	public abstract String getID() throws CommunicationException;

	/**
	 * To send a message to the remote entity (to a server or a client). This method is a wrapper
	 * method and uses the protocol-specific {@link #sendTo(Message, HostType)} implementation to
	 * actually send the message.
	 * 
	 * @param msg
	 *            The message to be sent
	 * @param sendingHostType
	 *            The host type of the entity that is sending the message
	 * @return The ID of the message returned by the remote entity upon receiving this message.
	 *         Sometimes the ID of the message is changed by the receiving party (especially in a
	 *         client->broker connection.)
	 * @throws CommunicationException
	 *             Either the connection is not active or the protocol-specific
	 *             {@link #sendTo(Message, HostType)} implementation threw an exception.
	 */
	public String send(Message msg, HostType sendingHostType) throws CommunicationException {
		if (!connected) {
			throw new CommunicationException("Not connected to the remote entity");
		}
		String msgID = sendTo(msg, sendingHostType);
		if (messagePathLogger.isDebugEnabled())
			messagePathLogger.debug("Message sent: " + msg.toString());
		return msgID;
	}

	/**
	 * Protocol-specific implementation of sending a message to a remote entity.
	 * 
	 * @param msg
	 *            The message to be sent
	 * @param sendingHostType
	 *            The host type of the sending party
	 * @return The message ID return by the remote party
	 * @throws CommunicationException
	 * 
	 * @see {@link #send(Message, HostType)}
	 */
	protected abstract String sendTo(Message msg, HostType sendingHostType)
			throws CommunicationException;

	public abstract void disconnect(MessageDestination sourceDest) throws CommunicationException;

}

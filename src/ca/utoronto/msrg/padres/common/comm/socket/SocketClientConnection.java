package ca.utoronto.msrg.padres.common.comm.socket;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.ConnectionListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectReplyMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.PubSubMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.PubSubReplyMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.SocketMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.SocketMessage.SocketMessageType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

/**
 * SocketClientConnection is used in both Brokers and Clients. It is the class (always executed as a
 * Thread), that reads incoming messages.
 * 
 * There is also a small amount of connection logic executed here. This is perhaps not ideal, and
 * should be handled elsewhere in the code base. The justification for doing it this way is that
 * this object is shared by every single client and broker. In practice, it was easier to code this
 * way.
 * 
 * @author Alex
 * 
 */
public class SocketClientConnection extends Thread {

	protected SocketPipe clientPipe = null;

	// Stores whether this clientConnection is being run by a server or a client.
	protected boolean serverConnection = false;

	// Stores whether we are connected to a client or a server
	protected boolean connectedToServer = false;

	// This only exists if serverConnection is true.
	protected SocketServer parentServer;

	// This only exists if serverConnection is false.
	protected MessageListenerInterface listener;

	protected MessageDestination clientDestination;

	/*
	 * Reference to the message sender associated with this clientConnection.
	 * 
	 * It is only initialised if this client connection is on the client side. It is used for
	 * providing the sendTo() in the messageSender with the messageID return value.
	 */
	protected SocketMessageSender senderCallBack = null;

	/*
	 * Constructor used by servers.
	 */
	public SocketClientConnection(SocketServer parentServer, SocketPipe clientPipe,
			ThreadGroup threadGroup) throws CommunicationException {
		super(threadGroup, clientPipe.toString());
		this.clientPipe = clientPipe;
		this.parentServer = parentServer;
		serverConnection = true;
		connectedToServer = false;
		// Find out who we're connected to, only if we are a server.
		// Because, if we are a client, we must be connected to a server.
		ConnectMessage connectMsg = (ConnectMessage) clientPipe.read();
		if (connectMsg.getSourceType() == HostType.CLIENT) {
			clientDestination = connectMsg.getSourceDestination();
			notifyConnectionListeners(clientDestination);
		} else if (connectMsg.getSourceType() == HostType.SERVER) {
			connectedToServer = true;
		} else {
			throw new CommunicationException("Incorrected HostType transmitted through the wire:"
					+ connectMsg.getSourceType() + ". Communication Layer exception.");
		}
		// reply to the connect request with broker ID
		clientPipe.write(new ConnectReplyMessage(parentServer.getServerURI()));
	}

	/*
	 * Constructor used by clients.
	 */
	public SocketClientConnection(SocketPipe clientPipe, MessageListenerInterface msgListener,
			SocketMessageSender senderCallBack) {
		this.clientPipe = clientPipe;
		listener = msgListener;
		this.senderCallBack = senderCallBack;
		serverConnection = false;
	}

	/**
	 * Alerts all relevant message listeners that a message has been received.
	 * 
	 * If this client connection is running on the side of a server, the message listeners are found
	 * through the parentServer reference.
	 * 
	 * If this client connection is running on the side of a client, the message listener is
	 * 'listener' variable.
	 * 
	 * @param msg
	 *            The message that was just received.
	 * @param hostType
	 *            The type of the host who sent the message
	 */
	protected void notifyMessageListeners(Message msg, HostType hostType) {
		if (serverConnection) {
			// TODO: This won't function if we have multiple messageListeners.
			for (MessageListenerInterface listener : parentServer.getMessageListeners())
				listener.notifyMessage(msg, hostType);
		} else {
			listener.notifyMessage(msg, hostType);
		}
	}

	/**
	 * Alerts all connection listeners that a connection has been received.
	 * 
	 * @param connectedDest
	 *            The destination that we just received a connection from.
	 * 
	 * @see: Precondition
	 * 
	 * @precondition: this method is only called if we are on a server and we receive a connection
	 *                from a client. Equivalently, if (serverConnection && !connectedToServer)
	 */
	protected void notifyConnectionListeners(MessageDestination connectedDest) {
		for (ConnectionListenerInterface cl : parentServer.getConnectionListeners()) {
			// Alert the connection listener
			SocketMessageSender msgSender = createSocketMessageSender();
			cl.connectionMade(connectedDest, msgSender);
		}
	}

	protected SocketMessageSender createSocketMessageSender() {
		return new SocketMessageSender(clientPipe);
	}

	/**
	 * Inform the connection listener of disconnection.
	 */
	protected void disconnect(MessageDestination connectedDest) {
		for (ConnectionListenerInterface cl : parentServer.getConnectionListeners())
			cl.connectionBroke(connectedDest);
	}

	/**
	 * The main thread execution method. Performs part of the connection protocol, and then loops,
	 * listening for messages.
	 */
	public void run() {
		try {
			SocketMessage socketMsg;
			// Receive until client closes connection, indicated by -1 return
			while ((socketMsg = clientPipe.read()) != null && !this.isInterrupted()) {
				/*
				 * If the received message is a string, it must be the messageID of a message that
				 * was just sent. If we are on the client side, inform the senderCallback
				 * MessageSender of the new messageID.
				 * 
				 * This will only ever occur when we are a client.
				 */
				if (socketMsg.getMessageType() == SocketMessageType.PUB_SUB_REPLY) {
					senderCallBack.messageIDReceived(((PubSubReplyMessage) socketMsg).getMessageID());
					continue;
				}
				/*
				 * Otherwise, we have received a Message. Handle it appropriately.
				 */
				// Receive subsequent messages, after the initial ID String
				if (socketMsg.getMessageType() == SocketMessageType.PUB_SUB) {
					PubSubMessage pubSubSocketMsg = (PubSubMessage) socketMsg;
					Message pubSubMsg = pubSubSocketMsg.getMessage();
					notifyMessageListeners(pubSubMsg, pubSubSocketMsg.getHostType());
					// Send back the messageID if we are a server and connected to a client
					if (serverConnection && !connectedToServer
							&& !(pubSubMsg instanceof PublicationMessage)) {
						clientPipe.write(new PubSubReplyMessage(pubSubMsg.getMessageID()));
					}
				}
			}
			// Close the socket. We are done with this client!
			clientPipe.close();
		} catch (CommunicationException e) {
			if (clientDestination != null) {
				disconnect(clientDestination);
			}
		}
	}

	public MessageDestination getClientDestination() {
		return clientDestination;
	}
}
package ca.utoronto.msrg.padres.common.comm.socket;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectReplyMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.PubSubMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.SocketMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.SocketMessage.SocketMessageType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

public class AsyncSocketClientConnection extends SocketClientConnection {

	/*
	 * Constructor used by servers.
	 */
	public AsyncSocketClientConnection(SocketServer parentServer, SocketPipe clntPipe,
			ThreadGroup threadGroup) throws CommunicationException {
		super(parentServer, clntPipe, threadGroup);
	}

	public AsyncSocketClientConnection(SocketPipe clntPipe, MessageListenerInterface msgListener,
			SocketMessageSender senderCallBack) {
		super(clntPipe, msgListener, senderCallBack);
	}

	/**
	 * The main thread execution method. Performs part of the connection protocol, and then loops,
	 * listening for messages.
	 */
	public void run() {
		MessageDestination connectedDest = null;
		try {
			// If we are a server open oos and ois and send the server's ID
			if (serverConnection) {
				clientPipe.write(new ConnectReplyMessage(parentServer.getServerURI()));
			}

			// Find out who we're connected to, only if we are a server.
			// Because, if we are a client, we must be connected to a server.
			if (serverConnection) {
				ConnectMessage connectMsg = (ConnectMessage) clientPipe.read();
				if (connectMsg.getSourceType() == HostType.CLIENT) {
					connectedToServer = false;
					connectedDest = connectMsg.getSourceDestination();
				} else if (connectMsg.getSourceType() == HostType.SERVER) {
					connectedToServer = true;
				} else {
					System.out.println("Incorrected HostType transmitted through the wire:"
							+ connectMsg.getSourceType() + ". Communication Layer exception.");
				}
			}

			// Alert ConnectionListener of connection only if we are on a server instance AND we are
			// connected to a client
			if (serverConnection && !connectedToServer) {
				notifyConnectionListeners(connectedDest);
			}

			SocketMessage msg;
			// Receive until client closes connection, indicated by -1 return
			while ((msg = clientPipe.read()) != null) {
				/*
				 * Otherwise, we have received a Message. Handle it appropriately.
				 */
				// Receive subsequent messages, after the initial ID String
				if (msg.getMessageType() == SocketMessageType.PUB_SUB) {
					PubSubMessage pubSubSocketMsg = (PubSubMessage) msg;
					Message pubSubMsg = pubSubSocketMsg.getMessage();
					notifyMessageListeners(pubSubMsg, pubSubSocketMsg.getHostType());
				} else {
					// TODO: handle error condition
				}
			}
			clientPipe.close(); // Close the socket. We are done with this
			// client!
		} catch (CommunicationException e) {
			if (serverConnection && !connectedToServer) {
				disconnect(connectedDest);
			}
		}
	}

}
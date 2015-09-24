// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 25-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.common.comm.socket;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.ConnectReplyMessage;
import ca.utoronto.msrg.padres.common.comm.socket.message.PubSubMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

/**
 * Initiates connections, and is responsible for sending all messages for both Brokers and Clients.
 * 
 * @author Alex
 * 
 */
public class SocketMessageSender extends MessageSender {

	protected SocketPipe socketPipe;

	protected SocketClientConnection clientConnection = null;

	protected String id;

	protected String sendToReturnVal;

	private boolean waitingForNotify;

	/**
	 * Constructor. Connect to specified URL. Never called from within the comm. layer.
	 * 
	 * @param remoteAddress
	 *            The URI of the remote broker's connection listener
	 * @throws CommunicationException
	 */
	public SocketMessageSender(SocketAddress remoteAddress) {
		super(remoteAddress);
	}

	/**
	 * This constructor is only called when dynamically creating a server's MessageSender.
	 * Therefore, it is only called on the side of a client.
	 * 
	 * @param socket
	 *            Socket being used by the SocketClientConnection object. (will be shared)
	 * @param oos
	 *            ObjectOutputStream being used by the SocketClientConnection object. (will be
	 *            shared)
	 * @param ois
	 *            ObjectInputStream being used by the SocketClientConnection object. (will be
	 *            shared)
	 * @precondition socket, oos and ois must all be opened.
	 */
	public SocketMessageSender(SocketPipe socketPipe) {
		super(null);
		this.socketPipe = socketPipe;
		connected = true;
	}

	/**
	 * Server Message Sender connect.
	 */
	@Override
	public void connect() throws CommunicationException {
		createConnection();
		socketPipe.write(new ConnectMessage(HostType.SERVER, null));
		// Receive the server's ID string
		ConnectReplyMessage replyMsg = (ConnectReplyMessage) socketPipe.read();
		setID(replyMsg.getServerID());
	}

	/**
	 * Client Message Sender
	 */
	@Override
	public void connect(MessageDestination sourceDest, MessageListenerInterface msgListener)
			throws CommunicationException {
		createConnection();
		socketPipe.write(new ConnectMessage(HostType.CLIENT, sourceDest));
		// Receive the server's ID string
		ConnectReplyMessage replyMsg = (ConnectReplyMessage) socketPipe.read();
		if (replyMsg == null) {
			throw new CommunicationException("No response from server");
		}
		setID(replyMsg.getServerID());
		clientConnection = createSocketClientConnection(socketPipe, msgListener);
		clientConnection.start();
	}

	/**
	 * Connect method that is called by both overloaded connect methods.
	 */
	public void createConnection() throws CommunicationException {
		SocketAddress socketAddress = (SocketAddress) remoteServerAddress;
		commSysLogger.info("Connecting to remote broker " + socketAddress.getNodeID());
		int tryCount = 0;
		Exception exception = null;
		for (tryCount = 0; tryCount < connectRetryLimit; tryCount++) {
			try {
				if (tryCount > 0) {
					commSysLogger.info("waiting for " + connectRetryPauseTime * 1000 + "ms");
					Thread.sleep(connectRetryPauseTime * 1000);
				}
				Socket socket = new Socket(socketAddress.getHost(), socketAddress.getPort());
				socketPipe = new SocketPipe(socket);
			} catch (InterruptedException e) {
				exception = e;
			} catch (UnknownHostException e) {
				exception = e;
			} catch (IOException e) {
				exception = e;
			}

			if (socketPipe != null) {
				break;
			}
			// if an exception is thrown and try count is reached throw and exception
			if (exception != null) {
				String errMsg = String.format("Connection attempt %d/%d failed:\n", (tryCount + 1),
						connectRetryLimit);
				commSysLogger.warn(errMsg + exception);
			}
		}
		if (tryCount == connectRetryLimit) {
			String errMsg = String.format("Connection to %s failed after %d attempts.\nCause: %s",
					remoteServerAddress, connectRetryLimit, exception.getMessage());
			commSysLogger.error(errMsg + exception);
			throw new CommunicationException(errMsg);
		}

		commSysLogger.info("Connected to " + remoteServerAddress);
		connected = true;
	}

	@Override
	public String getID() {
		return id;
	}

	public void setID(String id) {
		this.id = id;
	}

	@Override
	protected String sendTo(Message msg, HostType sourceHostType) throws CommunicationException {
		commSysLogger.debug("Sending message : " + msg.toString()
				+ " to remote server with destination " + msg.getNextHopID() + ".");
		if (socketPipe != null) {
			socketPipe.write(new PubSubMessage(msg, sourceHostType));
		} else {
			throw new CommunicationException("Remote entity has not been initialized");
		}

		// Get the messageID (returnValue)
		if (sourceHostType == HostType.CLIENT && !(msg instanceof PublicationMessage)) {
			synchronized (this) {
				try {
					waitingForNotify = true;
					wait();
				} catch (InterruptedException e) {
					System.err.println("DEBUG: waiting interrupted");
					e.printStackTrace();
				}
			}
			waitingForNotify = false;
			return sendToReturnVal;
		} else {
			return msg.getMessageID();
		}
	}

	/**
	 * Method invoked once the SocketClientConnection on a client side has received a messageId for
	 * a message that was just sent by this client.
	 * 
	 * Method records the messageId in this object, and notifies the waiting thread to recommence.
	 * 
	 * @param messageId
	 *            the new messageId, as received by the socketClientConnection.
	 */
	public void messageIDReceived(String messageId) {
		sendToReturnVal = messageId;
		while (!waitingForNotify) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// pass
			}
		}
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Disconnect.
	 */
	@Override
	public void disconnect(MessageDestination sourceDest) throws CommunicationException {
		if(clientConnection != null)
			clientConnection.interrupt();
		if(socketPipe != null)
			socketPipe.close();
	}

	protected SocketClientConnection createSocketClientConnection(
			SocketPipe socketPipe2, MessageListenerInterface msgListener) {
		return new SocketClientConnection(socketPipe, msgListener, this);
	}

}

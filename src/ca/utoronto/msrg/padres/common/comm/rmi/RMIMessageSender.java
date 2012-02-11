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
package ca.utoronto.msrg.padres.common.comm.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This class represents RMI Clients -- entities that make connections with RMI servers and
 *         communicate with them.
 * 
 */
public class RMIMessageSender extends MessageSender {

	private RMIServerInterface remoteServer;

	private RMIMessageListenerInterfce remoteListener;

	static Logger commSysLogger = Logger.getLogger(CommSystem.class);

	/**
	 * Constructor. Connects to the specified URI. This is a constructor used for broker->broker and
	 * client->broker connection
	 * 
	 * @param remoteAddress
	 *            The URI of the RMI server to connect to
	 * @throws CommunicationException
	 *             Thrown when the given URI is malformatted
	 */
	public RMIMessageSender(RMIAddress remoteAddress) throws CommunicationException {
		super(remoteAddress);
	}

	/**
	 * Constructor to create a message sender from a remote RMI message listener. Used in
	 * broker->client communication.
	 * 
	 * @param msgListener
	 *            The remote RMI message listener
	 */
	public RMIMessageSender(RMIMessageListenerInterfce msgListener) {
		super(null);
		remoteListener = msgListener;
		connected = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.utoronto.msrg.padres.common.comm.MessageSender#connect()
	 */
	@Override
	public void connect() throws CommunicationException {
		if (remoteServer != null) {
			throw new CommunicationException(
					"Reconnecting to a server while the connection is alive");
		}
		RMIAddress rmiAddress = (RMIAddress) remoteServerAddress;
		commSysLogger.info("Connecting to remote broker " + rmiAddress.getNodeID());
		int tryCount = 0;
		Exception exception = null;
		for (tryCount = 0; tryCount < connectRetryLimit; tryCount++) {
			try {
				if (tryCount > 0) {
					commSysLogger.info("waiting for " + connectRetryPauseTime * 1000 + "ms");
					Thread.sleep(connectRetryPauseTime * 1000);
				}
				remoteServer = (RMIServerInterface) Naming.lookup(remoteServerAddress.getNodeURI());
			} catch (MalformedURLException e) {
				exception = e;
				break;
			} catch (NotBoundException e) {
				exception = e;
			} catch (RemoteException e) {
				exception = e;
			} catch (InterruptedException e) {
				// for Thread.sleep() -- do nothing
			}
			if (remoteServer != null) {
				break;
			}
			if (exception != null) {
				String errMsg = String.format("Connection attempt %d/%d failed:\n", (tryCount + 1),
						connectRetryLimit);
				commSysLogger.warn(errMsg + exception);
			}
		}
		// if the try count is reached, throw and exception
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
	public void connect(MessageDestination sourceDest, MessageListenerInterface msgListener)
			throws CommunicationException {
		connect();
		try {
			RMIMessageListenerInterfce rmiMsgListener = new RMIMessageListener(
					sourceDest.getDestinationID(), msgListener);
			remoteServer.registerMessageListener(sourceDest, rmiMsgListener);
		} catch (RemoteException e) {
			commSysLogger.error("Failed to register RMI Message Listener" + e);
			throw new CommunicationException(e);
		}
	}

	@Override
	public String getID() throws CommunicationException {
		try {
			if (remoteServer != null) {
				return remoteServer.getID();
			} else if (remoteListener != null) {
				return remoteListener.getID();
			} else {
				throw new CommunicationException("Remote entity has not been initialized");
			}
		} catch (RemoteException e) {
			throw new CommunicationException(e);
		}
	}

	@Override
	protected String sendTo(Message msg, HostType sourceHostType) throws CommunicationException {
		commSysLogger.debug("Sending message : " + msg.toString()
				+ " to remote server with destination " + msg.getNextHopID() + ".");
		try {
			if (remoteServer != null) {
				// sending message to a server/broker
				if (sourceHostType == HostType.CLIENT && !(msg instanceof PublicationMessage)) {
					// from a client, a non-publication message; we need msg ID from server
					return remoteServer.receiveMessage(msg, sourceHostType);
				} else {
					// otherwise, no need to wait for a reply message
					remoteServer.receiveMessageWithoutReply(msg, sourceHostType);
					return msg.getMessageID();
				}
			} else if (remoteListener != null) {
				// sending to a listener/client
				if (sourceHostType == HostType.SERVER) {
					// sending from a server
					return remoteListener.receiveMessage(msg);
				} else {
					// client->client communication is not allowed, so throw and exception
					throw new CommunicationException(
							"Sending message to a client from an entity that is not a server is not allowed");
				}
			} else {
				throw new CommunicationException("Remote entity has not been initialized");
			}
		} catch (RemoteException e) {
			throw new CommunicationException(e);
		}
	}

	@Override
	public void disconnect(MessageDestination sourceDest) throws CommunicationException {
		try {
			if(remoteServer != null)
				remoteServer.unRegisterMessageListener(sourceDest);
		} catch (RemoteException e) {
			throw new CommunicationException(e);
		}
	}

}

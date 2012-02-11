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

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.socket.message.PubSubMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

public class AsyncSocketMessageSender extends SocketMessageSender {

	public AsyncSocketMessageSender(SocketAddress remoteAddress) {
		super(remoteAddress);
	}

	public AsyncSocketMessageSender(SocketPipe socketPipe) {
		super(socketPipe);
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
		return msg.getMessageID();

	}

	/**
	 * Disconnect.
	 */
	@Override
	public void disconnect(MessageDestination sourceDest) throws CommunicationException {
		socketPipe.close();
	}

}

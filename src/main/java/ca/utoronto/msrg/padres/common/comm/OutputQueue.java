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
 *         Queue to handle messages to be sent to remote entities. As it is extended from
 *         QueueHandler it contains a MessageQueue where the messages arrives to be sent. It also
 *         contains a MessageSender using which the messages are sent out
 * 
 */
public class OutputQueue extends QueueHandler {

	/**
	 * MessageSender with active connection.
	 */
	private MessageSender msgSender;

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Constructor to create a output queue. An active MessageSender and respective remote
	 * MessageDestination is required.
	 * 
	 * @param remoteDest
	 *            The MessageDestination of the remote entity to whom the output queue is created
	 * @param msgSender
	 *            The MessageSender to the remote entity. It has to be active; i.e. if the remote
	 *            entity is a server, the connect() must have to already called on the MessageSender
	 *            before being passed on to this constructor.
	 * 
	 * @see {@link MessageSender#connect()},
	 *      {@link MessageSender#connect(MessageDestination, MessageListenerInterface)}
	 */
	public OutputQueue(MessageDestination remoteDest, MessageSender msgSender) {
		super(remoteDest);
		this.msgSender = msgSender;
	}

	@Override
	public void processMessage(Message msg) {
		if (msgSender != null) {
			if (messagePathLogger.isDebugEnabled()) {
				messagePathLogger.debug("Sending message: " + msg);
			}
			try {
				msgSender.send(msg, HostType.SERVER);
			} catch (CommunicationException e) {
				messagePathLogger.error("Error in sending message to " + msg.getNextHopID() + ": "
						+ e);
				exceptionLogger.error("Error in sending message to " + msg.getNextHopID() + ": "
						+ e);
			}
		} else {
			messagePathLogger.error("MessageSender is null.");
			exceptionLogger.error("Here is an exception : ", new CommunicationException(
					"MessageSender is null."));
		}
	}

	public MessageSender getMsgSender() {
		return msgSender;
	}

}

package ca.utoronto.msrg.padres.common.comm;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This specifies the interface of a message listener object. A message listener should
 *         register itself with a message receiver so that the message receiver can inform the
 *         message listener of new messages using the interface. Note that the message receiver can
 *         be anything: it can be a communication server or it can be another message listener.
 * 
 * @see CommServer
 */
public interface MessageListenerInterface {

	/**
	 * To inform a new message is received
	 * 
	 * @param msg
	 *            The message received and to be passed on to the message listener
	 * @param sourceType
	 *            The host type of the entity from where the message is received
	 */
	public void notifyMessage(Message msg, HostType sourceType);

}

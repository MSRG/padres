package ca.utoronto.msrg.padres.common.comm;

import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This specifies the interface of an connection manager entity. When an object implementing
 *         this interface is registered with a server, it can be informed using the methods
 *         specified here when a new connection is or an existing one is broken.
 * 
 * @see CommServer
 */
public interface ConnectionListenerInterface {

	/**
	 * To inform the object implementing this interface about a connection made by a client.
	 * 
	 * @param clientDest
	 *            The client destination who made the connection
	 * @param msgSender
	 *            A new MessageSender entity created by the server who accepted the connection. This
	 *            msgSender can be used by the connection listener object to communicate with the
	 *            client who made the connection. In other words, the connection is preserved,
	 *            encapsulated in msgSender, and passed over to the connection listener to use.
	 */
	public void connectionMade(MessageDestination clientDest, MessageSender msgSender);

	/**
	 * To inform a connection listener that an existing connection is broken by the client
	 * 
	 * @param msgDest
	 *            The destination of the client with whom the connection was made (same as the one
	 *            who broke the connection)
	 */
	public void connectionBroke(MessageDestination msgDest);

}

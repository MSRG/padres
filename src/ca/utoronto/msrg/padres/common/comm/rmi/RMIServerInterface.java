package ca.utoronto.msrg.padres.common.comm.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ca.utoronto.msrg.padres.common.comm.ConnectionListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This class defines the interface for the RMI server. The methods defined here will be
 *         called by the RMI clients (PADRES clients or PADRES brokers invoking calls as clients.)
 * 
 */
public interface RMIServerInterface extends Remote {

	/**
	 * To send a message to the RMI server implementing this interface. This method will be called
	 * by the entity who sends the message.
	 * 
	 * @param msg
	 *            The message to send
	 * @param sourceType
	 *            The host type of the entity sending the message
	 * @return The message ID returned by the RMI server
	 * @throws RemoteException
	 * 
	 * @see {@link MessageSender#send(Message, HostType)}
	 */
	public String receiveMessage(Message msg, HostType sourceType) throws RemoteException;

	/**
	 * To send a message to the RMI server implementing this interface, but not expecting any reply
	 * back. This method will be called by the entity who sends the message.
	 * 
	 * @param msg
	 *            The message to send
	 * @param sourceType
	 *            The host type of the entity sending the message
	 * @throws RemoteException
	 */
	public void receiveMessageWithoutReply(Message msg, HostType sourceType) throws RemoteException;

	/**
	 * This method is called by a remote entity to register a RMI message listener at a RMI server.
	 * The server will use this message listener to convey a message back to the remote entity. In
	 * PADRES, it is used by a client in the client->broker communication. When a message listener
	 * is registered, the comm. system should inform the connection listener
	 * 
	 * @param clientDest
	 *            The destination of the remote entity to which the message listener belongs
	 * @param msgListener
	 *            The RMI message listener to receive message from the server implementing this
	 *            interface
	 * @throws RemoteException
	 * 
	 * @see {@link ConnectionListenerInterface}
	 */
	public void registerMessageListener(MessageDestination clientDest,
			RMIMessageListenerInterfce msgListener) throws RemoteException;

	/**
	 * To get the ID of the RMI server implementing this interface.
	 * 
	 * @return The ID of the RMI server
	 * @throws RemoteException
	 * 
	 * @see {@link RMIAddress}
	 */
	public String getID() throws RemoteException;

	/**
	 * To unregister a message listener registered using
	 * {@link #registerMessageListener(MessageDestination, RMIMessageListenerInterfce)}
	 * 
	 * @param clientDest
	 *            The destination of the remote entity who is removing its listener
	 * @throws RemoteException
	 */
	public void unRegisterMessageListener(MessageDestination clientDest) throws RemoteException;

}

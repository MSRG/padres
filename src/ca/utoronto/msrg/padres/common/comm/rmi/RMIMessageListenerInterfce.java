package ca.utoronto.msrg.padres.common.comm.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ca.utoronto.msrg.padres.common.message.Message;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         This specify the interface for the remote message listener using RMI system. A remote
 *         message listener register itself with an remote RMI entity, so that when the remote RMI
 *         entity wants to convery a message to this message listener, it can use the API defined
 *         here.
 * 
 */
public interface RMIMessageListenerInterfce extends Remote {

	/**
	 * To get the ID of the message listener
	 * 
	 * @return The identifier
	 * @throws RemoteException
	 */
	public String getID() throws RemoteException;

	/**
	 * Called by a remote RMI entity, this method is used to send a Message from a server to a
	 * client
	 * 
	 * @param msg
	 *            The message to be sent
	 * @return The message ID. Not very useful info, just to confirm receipt.
	 * @throws RemoteException
	 * 
	 * @see {@link RMIMessageSender#sendTo(Message, ca.utoronto.msrg.padres.common.comm.CommSystem.HostType)}
	 */
	public String receiveMessage(Message msg) throws RemoteException;

}

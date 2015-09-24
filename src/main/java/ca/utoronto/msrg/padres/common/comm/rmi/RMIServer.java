package ca.utoronto.msrg.padres.common.comm.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import ca.utoronto.msrg.padres.common.comm.CommServer;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.ConnectionListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-12
 * 
 *         The communication server that implements the RMI protocol. Most of the server
 *         functionalities are defined in the RMIServerInterface
 * 
 */
public class RMIServer extends CommServer implements RMIServerInterface {

	/**
	 * Constructor for creating a RMI server. It checks for the RMI registry given in the URI. If
	 * the registry is supposed to be in the localhost, but not found, it instantiate an RMI
	 * registry locally.
	 * 
	 * @param serverAddress
	 *            The URI of the RMI server.
	 * @param commSystem
	 *            The communication system that creates this server
	 * @throws CommunicationException
	 *             This is thrown in the cases of
	 *             <ul>
	 *             <li>there is error in parsing the serverURI
	 *             <li>the broker ID did not match the entity ID given in the serverURI</li>
	 *             <li>the registry could not be found or could not be instantiated locally</li>
	 *             <li>the entity could not be registered</li>
	 *             </ul>
	 */
	public RMIServer(RMIAddress serverAddress, CommSystem commSystem) throws CommunicationException {
		super(serverAddress);
		// if the RMI registry is to be hosted in the localhost, start it
		if (CommSystem.isLocalAddress(serverAddress.getHost())) {
			createLocalRMIRegistry();
		}
		try {
			// create an RMI communication server for the broker
			RMIServerInterface rmiServerStub = (RMIServerInterface) UnicastRemoteObject.exportObject(
					this, 0);
			// bind the server to the registry
			Registry registry = LocateRegistry.getRegistry(serverAddress.getHost(),
					serverAddress.getPort());
			// TODO: rebind does not check for name conflict, what should we do here?
			registry.rebind(serverAddress.getNodeID(), rmiServerStub);
		} catch (RemoteException e) {
			throw new CommunicationException("Could not bind server with the RMI registry: ", e);
		}
	}

	/**
	 * To create an RMI registry locally
	 * 
	 * @throws CommunicationException
	 *             If the registry could not be created
	 */
	private void createLocalRMIRegistry() throws CommunicationException {
		// check if an rmi registry is already running.
		try {
			Registry r = LocateRegistry.getRegistry(((RMIAddress) serverAddress).getPort());
			// we need to do some operation on the registry to verify it is actually there
			r.list();
		} catch (Exception e1) {
			// registry is probably not running, start it
			try {
				LocateRegistry.createRegistry(((RMIAddress) serverAddress).getPort());
			} catch (RemoteException e) {
				commInterfaceLogger.fatal("Can't create RMIRegistry on port "
						+ ((RMIAddress) serverAddress).getPort(), e);
				exceptionLogger.fatal("Can't create RMIRegistry on port "
						+ ((RMIAddress) serverAddress).getPort(), e);
				throw new CommunicationException("Can't create RMIRegistry on port "
						+ ((RMIAddress) serverAddress).getPort(), e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface#getID()
	 */
	@Override
	public String getID() throws RemoteException {
		return serverAddress.getNodeURI();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface#receiveMessage(ca.utoronto.msrg
	 * .padres.common.message.Message, ca.utoronto.msrg.padres.common.comm.CommSystem.HostType)
	 */
	@Override
	public String receiveMessage(Message msg, HostType sourceType) throws RemoteException {
		for (MessageListenerInterface listener : msgListeners)
			listener.notifyMessage(msg, sourceType);
		// TODO: when there are multiple message listeners, what to return can be a problem.
		return msg.getMessageID();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface#receiveMessageWithoutReply(ca.
	 * utoronto.msrg.padres.common.message.Message,
	 * ca.utoronto.msrg.padres.common.comm.CommSystem.HostType)
	 */
	@Override
	public void receiveMessageWithoutReply(Message msg, HostType sourceType) throws RemoteException {
		for (MessageListenerInterface listener : msgListeners)
			listener.notifyMessage(msg, sourceType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface#registerMessageListener(ca.utoronto
	 * .msrg.padres.common.message.MessageDestination,
	 * ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce)
	 */
	@Override
	public void registerMessageListener(MessageDestination clientDest,
			RMIMessageListenerInterfce msgListener) throws RemoteException {
		for (ConnectionListenerInterface listener : connectListeners)
			listener.connectionMade(clientDest, createRMIMessageSender(msgListener));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface#unRegisterMessageListener(ca.utoronto
	 * .msrg.padres.common.message.MessageDestination)
	 */
	@Override
	public void unRegisterMessageListener(MessageDestination neighborDest) throws RemoteException {
		for (ConnectionListenerInterface listener : connectListeners)
			listener.connectionBroke(neighborDest);
	}

	@Override
	public void shutDown() throws CommunicationException {
		// bind the server to the registry
		try {
			UnicastRemoteObject.unexportObject(this, true);
			Registry registry = LocateRegistry.getRegistry(((RMIAddress) serverAddress).getHost(),
					((RMIAddress) serverAddress).getPort());
			registry.unbind(((RMIAddress) serverAddress).getNodeID());
		} catch (RemoteException e) {
			throw new CommunicationException("Error in shutting down RMI Server: " + e.getMessage());
		} catch (NotBoundException e) {
			throw new CommunicationException("Error in shutting down RMI Server: " + e.getMessage());
		}
	}

	protected RMIMessageSender createRMIMessageSender(
			RMIMessageListenerInterfce msgListener) {
		return new RMIMessageSender(msgListener);
	}

}

package ca.utoronto.msrg.padres.common.comm.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;

public class RMIMessageListener implements RMIMessageListenerInterfce {

	private String listenerID;

	private MessageListenerInterface msgListener;

	public RMIMessageListener(String listenerID, MessageListenerInterface msgListener)
			throws RemoteException {
		this.listenerID = listenerID;
		this.msgListener = msgListener;
		UnicastRemoteObject.exportObject(this, 0);
	}

	@Override
	public String receiveMessage(Message msg) throws RemoteException {
		msgListener.notifyMessage(msg, HostType.SERVER);
		return msg.getMessageID();
	}

	@Override
	public String getID() throws RemoteException {
		return listenerID;
	}

}

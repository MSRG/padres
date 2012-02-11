package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.socket.SocketClientConnection;
import ca.utoronto.msrg.padres.common.comm.socket.SocketMessageSender;
import ca.utoronto.msrg.padres.common.comm.socket.SocketPipe;
import ca.utoronto.msrg.padres.common.comm.socket.SocketServer;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap SocketClientConnection's main public methods and construct
 * TesterSocketMessageSender that can interact with the IBrokerTester
 * implementing object used for testing. Other methods inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - SocketClientConnection.notifyMessageListeners():
 *   IBrokerTester.msgReceived() is appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterSocketClientConnection extends SocketClientConnection {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;

	public TesterSocketClientConnection(
			IBrokerTester brokerTester, String brokerURI,
			SocketPipe clientPipe,
			MessageListenerInterface msgListener,
			SocketMessageSender senderCallBack) {
		super(clientPipe, msgListener, senderCallBack);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}
	
	public TesterSocketClientConnection(
			IBrokerTester brokerTester, String brokerURI,
			SocketServer parentServer,
			SocketPipe clientPipe, ThreadGroup threadGroup)
			throws CommunicationException {
		super(parentServer, clientPipe, threadGroup);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}

	@Override
	protected void notifyMessageListeners(Message msg, HostType hostType) {
		_brokerTester.msgReceived(_brokerURI, msg);
		super.notifyMessageListeners(msg, hostType);
	}
	
	@Override
	protected SocketMessageSender createSocketMessageSender() {
		return new TesterSocketMessageSender(_brokerTester, _brokerURI, this, clientPipe);
	}
}
package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketClientConnection;
import ca.utoronto.msrg.padres.common.comm.socket.SocketMessageSender;
import ca.utoronto.msrg.padres.common.comm.socket.SocketPipe;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap SocketMessageSender's main public methods and construct
 * TesterSocketClientConnection that can interact with the IBrokerTester
 * implementing object used for testing. Other methods inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - SocketMessageSender.send(): IBrokerTester.msgSent() is appropriately
 *     called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterSocketMessageSender extends SocketMessageSender implements ITestMessageSender {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	public final TesterSocketClientConnection _socketClientConnection;

	public TesterSocketMessageSender(
			IBrokerTester brokerTester, String brokerURI,
			SocketAddress remoteAddress) {
		super(remoteAddress);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
		_socketClientConnection = null;
		if(remoteAddress == null)
			throw new IllegalArgumentException();
	}

	public TesterSocketMessageSender(
			IBrokerTester brokerTester, String brokerURI,
			TesterSocketClientConnection socketClientConnection,
			SocketPipe socketPipe) {
		super(socketPipe);
		
		if(brokerTester == null)
			_brokerTester = brokerTester;
		else
			_brokerTester = brokerTester;
		_brokerURI = brokerURI;
		_socketClientConnection = socketClientConnection;		
	}

	@Override
	public String send(Message msg, HostType sendingHostType) throws CommunicationException {
		String ret = super.send(msg, sendingHostType);
		getBrokerTester().msgSent(this, msg);
		
		return ret;
	}

	private IBrokerTester getBrokerTester() {
		if(_brokerTester != null)
			return _brokerTester;
		
		else
			return _socketClientConnection._brokerTester;
	}

	@Override
	public String getBrokerURI() {
		if(_brokerTester != null)
			return _brokerURI;
		
		else
			return _socketClientConnection._brokerURI;
	}

	@Override
	public String getRemoteURI() {
		if(remoteServerAddress != null)
			return remoteServerAddress.getNodeURI();
		
		return _socketClientConnection.getClientDestination().toString();
	}
	
	@Override
	protected SocketClientConnection createSocketClientConnection(
			SocketPipe socketPipe2, MessageListenerInterface msgListener) {
		TesterSocketClientConnection socketClientConn =
			new TesterSocketClientConnection(
				_brokerTester, _brokerURI, socketPipe, msgListener, this);
		
		return socketClientConn;
	}
}
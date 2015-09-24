package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketClientConnection;
import ca.utoronto.msrg.padres.common.comm.socket.SocketConnectionListener;
import ca.utoronto.msrg.padres.common.comm.socket.SocketPipe;
import ca.utoronto.msrg.padres.common.comm.socket.SocketServer;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap SocketConnectionListener's main public methods and construct
 * TesterSocketClientConnection that can interact with the IBrokerTester
 * implementing object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterSocketConnectionListener extends SocketConnectionListener {

	public IBrokerTester _brokerTester;
	public String _brokerURI;

	public TesterSocketConnectionListener(
			IBrokerTester brokerTester, String brokerURI,
			SocketAddress serverAddress,
			SocketServer parentServer) throws CommunicationException {
		super(serverAddress, parentServer);

		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}
	
	public void setBrokerTester(IBrokerTester brokerTester) {
		_brokerTester = brokerTester;
	}

	public void setBrokerURI(String brokerURI) {
		_brokerURI = brokerURI;
	}

	@Override
	protected SocketClientConnection createSocketClientConnection(
			SocketServer parentServer2, SocketPipe clientPipe,
			ThreadGroup connectionGroup) throws CommunicationException {
		return new TesterSocketClientConnection(
				_brokerTester, _brokerURI, parentServer2, clientPipe, connectionGroup);
	}
}
package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketConnectionListener;
import ca.utoronto.msrg.padres.common.comm.socket.SocketServer;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap SocketServer's main public methods and construct
 * TesterSocketConnectionListener that can interact with the IBrokerTest
 * implementing object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterSocketServer extends SocketServer {
	
	public final IBrokerTester _brokerTester;
	public final String _brokerURI;

	public TesterSocketServer(
			IBrokerTester brokerTester, String brokerURI,
			SocketAddress serverAddress, CommSystem commSystem)
			throws CommunicationException {
		super(serverAddress, commSystem);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
		
		((TesterSocketConnectionListener)connectionListener).setBrokerTester(brokerTester);
		((TesterSocketConnectionListener)connectionListener).setBrokerURI(brokerURI);
		_brokerTester.commSystemStarted(_brokerURI, serverAddress.getNodeURI());
	}
	
	@Override
	protected SocketConnectionListener createNewSocketConnectionListener(
			SocketAddress serverAddress) throws CommunicationException {
		return new TesterSocketConnectionListener(
				_brokerTester, _brokerURI, serverAddress, this);
	}

	@Override
	public void shutDown() throws CommunicationException {
		super.shutDown();
		
		_brokerTester.commSystemShutdown(_brokerURI, serverAddress.getNodeURI());
	}
}
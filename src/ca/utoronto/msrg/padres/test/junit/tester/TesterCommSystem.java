package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageSender;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServer;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketMessageSender;
import ca.utoronto.msrg.padres.common.comm.socket.SocketServer;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap CommSystem's main public methods and construct
 * TesterRMIServer, TesterSocketServer, TesterSocketMessageSender and
 * TesterRMIMessageSender that can interact with a IBrokerTester object.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterCommSystem extends CommSystem {

	protected final IBrokerTester _brokerTester;
	protected final String _brokerURI;
	
	public TesterCommSystem(IBrokerTester brokerTester,
			String brokerURI) throws CommunicationException {
		super();
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
	}
	
	@Override
	protected RMIServer createNewRMIServer(RMIAddress serverAddress) throws CommunicationException {
		return new TesterRMIServer(_brokerTester, _brokerURI, serverAddress, this);
	}
	
	@Override
	protected SocketServer createNewSocketServer(SocketAddress serverAddress) throws CommunicationException {
		return new TesterSocketServer(_brokerTester, _brokerURI, serverAddress, this);
	}
	
	@Override
	protected RMIMessageSender createRMIMessageSender(
			RMIAddress remoteServerAddress) throws CommunicationException {
		return new TesterRMIMessageSender(
				_brokerTester, _brokerURI, remoteServerAddress);
	}
	
	@Override
	protected SocketMessageSender createSocketMessageSender(
			SocketAddress remoteServerAddress) {
		return new TesterSocketMessageSender(
				_brokerTester, _brokerURI, remoteServerAddress);
	}
}
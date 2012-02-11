package ca.utoronto.msrg.padres.test.junit.tester;

import java.rmi.RemoteException;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageSender;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServer;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap RMIServer's main public methods and construct
 * TesterRMIMessageSender that can interact with the IBrokerTester
 * implementing object used for testing. Other methods inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - RMIServer.receiveMessage(): IBrokerTester.msgReceived() is appropriately
 *     called.
 *   
 *   - RMIServer.shutdown(): IBrokerTester.commSystemShutdown() is
 *   appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterRMIServer extends RMIServer {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;

	public TesterRMIServer(
			IBrokerTester brokerTester,  String brokerURI,
			RMIAddress serverAddress, CommSystem commSystem)
			throws CommunicationException {
		super(serverAddress, commSystem);
		
		_brokerTester = brokerTester;
		_brokerURI = brokerURI;
		_brokerTester.commSystemStarted(_brokerURI, serverAddress.getNodeURI());
	}
	
	@Override
	protected RMIMessageSender createRMIMessageSender(RMIMessageListenerInterfce msgListener) {
		RMIMessageSender msgSender = new TesterRMIMessageSender(_brokerTester, _brokerURI, msgListener);
		return msgSender;
	}
	
	@Override
	public String receiveMessage(Message msg, HostType sourceType) throws RemoteException {
		_brokerTester.msgReceived(_brokerURI, msg);
		String ret = super.receiveMessage(msg, sourceType);
		return ret;
	}
	
	@Override
	public void shutDown() throws CommunicationException {
		super.shutDown();
		_brokerTester.commSystemShutdown(_brokerURI, serverAddress.getNodeURI());
	}
}
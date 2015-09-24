package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap QueueManager's main public methods to interact with
 * the IBrokerTester object used for testing. Wrapped methods inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - BrokerState.addReceivedPub(): IBrokerTeseter.clientReceivedPublication()
 *   is appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */

public class TesterBrokerState extends BrokerState {

	public final IBrokerTester _brokerTester;
	public final String _clientID;

	public TesterBrokerState(IBrokerTester brokerTester, String clientID, NodeAddress brokerAddress) {
		super(brokerAddress);
		
		_brokerTester = brokerTester;
		_clientID = clientID;
	}

	@Override
	public synchronized void addReceivedPub(PublicationMessage pubMsg) {
		super.addReceivedPub(pubMsg);
		_brokerTester.clientReceivedPublication(_clientID, pubMsg);
	}
}

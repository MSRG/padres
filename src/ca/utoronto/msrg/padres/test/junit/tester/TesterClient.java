package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

/**
 * Auxiliary class used as part of test framework. The class represents
 * a Padres Client that interacts with the IBrokerTester upon execution of
 * below methods:
 * 
 * - Client.connect(): IBrokerTester.connectFailed() is called appropriately
 * called upon failure.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterClient extends Client {
	
	public final IBrokerTester _brokerTester;
	public final String _clientId;

	public TesterClient(IBrokerTester brokerTester, String clientId) throws ClientException {
		super(clientId);
		
		_brokerTester = brokerTester;
		_clientId = clientId;
	}
	
	public TesterClient(IBrokerTester brokerTester, ClientConfig newConfig) throws ClientException {
		super(newConfig);
		
		_brokerTester = brokerTester;
		_clientId = newConfig.clientID;
	}

	@Override
	protected BrokerState createBrokerState(NodeAddress brokerAddress) {
		return new TesterBrokerState(_brokerTester, _clientId, brokerAddress);
	}
	
	@Override
	public BrokerState connect(String brokerURI) throws ClientException {
		try {
			return super.connect(brokerURI);
		} catch(ClientException x) {
			_brokerTester.connectFailed(_clientId, brokerURI);
			throw x;
		}
	}
}

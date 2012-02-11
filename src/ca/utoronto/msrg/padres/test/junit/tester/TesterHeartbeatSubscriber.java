package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.HeartbeatSubscriber;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap HeartbeatSubscriber's main public methods and construct
 * TesterMessageQueue that can interact with the IBrokerTester implementing 
 * object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterHeartbeatSubscriber extends HeartbeatSubscriber {
	
	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterHeartbeatSubscriber(IBrokerTester brokerTester, BrokerCore broker) {
		super(broker);
		
		_brokerTester = brokerTester;
		_brokerURI = broker.getBrokerURI();
	}

	@Override
	protected MessageQueue createMessageQueue() {
		TesterMessageQueue mQueue = new TesterMessageQueue();
		mQueue.setBrokerTester(_brokerTester);
		mQueue.setBrokerURI(_brokerURI);
		return mQueue;
	}
}

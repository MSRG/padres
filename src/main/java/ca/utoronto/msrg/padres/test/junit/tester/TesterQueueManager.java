package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.QueueManager;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap QueueManager's main public methods and construct
 * TesterMessageQueue that can interact with the IBrokerTester
 * implementing object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterQueueManager extends QueueManager {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterQueueManager(IBrokerTester brokerTester, BrokerCore broker) throws BrokerCoreException {
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
	
	@Override
	public void registerQueue(MessageDestination msgDest, MessageQueue msgQueue) {
		super.registerQueue(msgDest, msgQueue);
		((TesterMessageQueue)msgQueue).setMyMessageDestination(msgDest);
		((TesterMessageQueue)msgQueue).setBrokerTester(_brokerTester);
		((TesterMessageQueue)msgQueue).setBrokerURI(_brokerURI);
	}
}

package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.MessageSender;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap OutputQueue's main public methods and construct
 * TesterMessageQueue that can interact with the IBrokerTester
 * implementing object used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterOutputQueue extends OutputQueue {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterOutputQueue(IBrokerTester brokerTester,
			BrokerCore broker,
			MessageDestination remoteDest,
			MessageSender msgSender) {
		super(remoteDest, msgSender);
		
		_brokerTester = brokerTester;
		_brokerURI = broker.getBrokerURI();
		
		if(TesterRMIMessageSender.class.isAssignableFrom(msgSender.getClass()))
			((TesterRMIMessageSender)msgSender).setDestination(remoteDest);
		
		((TesterMessageQueue)msgQueue).setBrokerTester(_brokerTester);
		((TesterMessageQueue)msgQueue).setMyMessageDestination(myDestination);
		((TesterMessageQueue)msgQueue).setBrokerURI(_brokerURI);
	}

	@Override
	protected MessageQueue createMessageQueue() {
		TesterMessageQueue mQueue = new TesterMessageQueue();
		mQueue.setBrokerTester(_brokerTester);
		mQueue.setBrokerURI(_brokerURI);
		return mQueue;
	}
}
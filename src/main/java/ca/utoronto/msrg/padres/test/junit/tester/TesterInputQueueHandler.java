package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap InputQueueHandler's main public methods and construct
 * TesterMessageQueue that can interact with the IBrokerTester implementing 
 * object used for testing. Other methods inform the IBrokerTester object of
 * occurrence of specific events within the object:
 * 
 *   - InputQueueHandler.prelude(): IBrokerTester.queueHandlerStarted() is
 *   appropriately called.
 *   
 *   - InputQueueHandler.cleanUp(): IBrokerTester.queueHandlerShutdown() is
 *   appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterInputQueueHandler extends InputQueueHandler {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterInputQueueHandler(IBrokerTester brokerTester,
			BrokerCore broker) {
		super(broker);
		
		_brokerTester = brokerTester;
		_brokerURI = broker.getBrokerURI();
		
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
	
	@Override
	protected void prelude() {
		super.prelude();
		_brokerTester.queueHandlerStarted(_brokerURI, myDestination);
	}
	
	@Override
	protected void cleanUp() {
		super.cleanUp();
		_brokerTester.queueHandlerShutdown(_brokerURI, myDestination);
	}
	
	@Override
	public void processMessage(Message msg) {
		super.processMessage(msg);
	}
}
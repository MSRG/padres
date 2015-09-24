package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.controller.db.DBHandler;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap DBHandler's main public methods and construct
 * TesterMessageQueue that can interact with a IBrokerTester object. methods
 * inform the IBrokerTester object of occurrence of specific events within the
 * object:
 * 
 *   - DBHandler.init(): IBrokerTeser.dbHandlerInitComplete() is appropriately
 *   called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterDBHandler extends DBHandler {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterDBHandler(IBrokerTester brokerTester, String databaseID, BrokerCore broker) {
		super(databaseID, broker);
		
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
	public void init() {
		super.init();
		_brokerTester.dbHandlerInitComplete(_brokerURI);
	}
}

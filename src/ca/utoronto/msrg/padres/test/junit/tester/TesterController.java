package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.controller.Controller;
import ca.utoronto.msrg.padres.broker.controller.LifeCycleManager;
import ca.utoronto.msrg.padres.broker.controller.OverlayManager;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap Controller's main public methods and construct
 * TesterMessageQueue, TesterOverlayManager and TesterLifeCycleManager
 * that can interact with a IBrokerTester object.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
class TesterController extends Controller {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterController(IBrokerTester brokerTester, TesterBrokerCore broker) {
		super(broker);
		
		_brokerTester = brokerTester;
		_brokerURI = broker.getBrokerURI();
		((TesterMessageQueue)inQueue).setBrokerTester(_brokerTester);
		((TesterMessageQueue)inQueue).setBrokerURI(_brokerURI);
	}

	@Override
	protected MessageQueue createMessageQueue() {
		TesterMessageQueue mQueue = new TesterMessageQueue();
		mQueue.setBrokerTester(_brokerTester);
		mQueue.setBrokerURI(_brokerURI);
		return mQueue;
	}

	@Override
	protected OverlayManager createOverlayManager()
			throws BrokerCoreException {
		return new TesterOverlayManager(
				_brokerTester, (TesterBrokerCore) brokerCore);
	}

	@Override
	protected LifeCycleManager createLifeCycleManager(BrokerCore brokerCore) {
		LifeCycleManager lcManager = new TesterLifeCycleManager(_brokerTester, brokerCore);
		lcManager.init();
		return lcManager;
	}
}
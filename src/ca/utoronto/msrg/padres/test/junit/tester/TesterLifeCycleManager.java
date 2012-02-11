package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.controller.LifeCycleManager;
import ca.utoronto.msrg.padres.broker.controller.db.DBHandler;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap LifeCycleManager's main public methods and construct
 * TesterDBHandler that can interact with IBrokerTester object. Other methods
 * inform the IBrokerTester object of occurrence of specific events within the
 * object:
 * 
 *   - LifeCycleManager.shutdownHandlers():
 *   IBrokerTester.lifecyclemanagerHandleShutdown() is appropriately called.
 *   
 *   - LifeCycleManager.resumeHandlers(): 
 *   IBrokerTester.lifecyclemanagerHandleResume() is appropriately called.
 *   
 *   - LifeCycleManager.stopHandlers():
 *   IBrokerTester.lifecyclemanagerHandleStop() is appropriately called. 
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterLifeCycleManager extends LifeCycleManager {

	public final IBrokerTester _brokerTester;
	public final String _brokerURI;
	
	public TesterLifeCycleManager(IBrokerTester brokerTester, BrokerCore broker) {
		super(broker);
		
		_brokerTester = brokerTester;
		_brokerURI = broker.getBrokerURI();
	}

	@Override
	protected void shutdownHandlers() {
		super.shutdownHandlers();
		_brokerTester.lifecyclemanagerHandleShutdown(_brokerURI);
	}
	
	@Override
	protected void resumeHandlers() {
		super.resumeHandlers();
		_brokerTester.lifecyclemanagerHandleResume(_brokerURI);
	}
	
	@Override
	protected void stopHandlers() {
		super.stopHandlers();
		_brokerTester.lifecyclemanagerHandleStop(_brokerURI);
	}
	
	@Override
	protected DBHandler createDBHandler(String databaseID, BrokerCore broker) {
		DBHandler dbHandler = new TesterDBHandler(_brokerTester, databaseID, broker);
		return dbHandler;
	}
}

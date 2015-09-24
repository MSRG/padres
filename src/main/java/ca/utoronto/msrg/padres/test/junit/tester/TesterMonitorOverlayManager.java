package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.tools.padresmonitor.ClientMonitorCommandManager;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorClient;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.OverlayManager;

/**
 * Auxiliary class used as part of test framework. The class implements
 * methods that wrap OverlayManager's main public methods and construct
 * TesterMessageQueue that can interact with the IBrokerTester
 * implementing object used for testing. Other methods inform the
 * IBrokerTester object of occurrence of specific events within the object:
 * 
 *   - OverlayManager.prelude(): IBrokerTester.queueHandlerStarted() is
 *   appropriately called.
 *   
 *   - OverlayManager.cleanUp(): IBrokerTester.queueHandlerShutdown() is
 *   appropriately called.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterMonitorOverlayManager extends OverlayManager {

	public final IBrokerTester _brokerTester;
	public final String _clientURI;

	public TesterMonitorOverlayManager(IBrokerTester brokerTester,
			MonitorFrame monitorFrame,
			ClientMonitorCommandManager comm, MonitorClient monitorClient)
			throws ClientException {
		super(monitorFrame, comm, monitorClient);
		
		_brokerTester = brokerTester;
		_clientURI = monitorClient.getClientID();
	}
	
	@Override
	protected MessageQueue createMessageQueue() {
		TesterMessageQueue mQueue = new TesterMessageQueue();
		mQueue.setBrokerTester(_brokerTester);
		mQueue.setBrokerURI(_clientURI);
		return mQueue;
	}
	
	@Override
	protected void prelude() {
		super.prelude();
		_brokerTester.queueHandlerStarted(_clientURI, myDestination);
	}
	
	@Override
	protected void cleanUp() {
		super.cleanUp();
		_brokerTester.queueHandlerShutdown(_clientURI, myDestination);
	}
}

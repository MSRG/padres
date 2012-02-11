package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.InjectMessageDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

public class TestMonitor extends TestCase {

	private BrokerCore brokerCore1;

	private BrokerCore brokerCore2;

	private BrokerCore brokerCore3;
	
	private MonitorFrame monitorFrame;

	private MessageWatchAppender messageWatcher;

	private PatternFilter msgFilter;

	@Override
	public void setUp() {
		AllTests.resetBrokerConnections();
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		msgFilter.setPattern("");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	@Override
	public void tearDown() {
		try {
			monitorFrame.shutdown();
		} catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		if (brokerCore3 != null)
			brokerCore3.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
	}

	/**
	 * Test connection among three brokers, where broker2 connected to broker1 already, and broker3
	 * starts self-governed. Then let broker2 connects broker3, where connection msgs need to be
	 * injected from monitor.
	 * 
	 * @throws ClientException
	 * @throws BrokerCoreException
	 * @throws ParseException 
	 */
	public void testOneBrokerConnectedOtherTwoBrokersAtSameTime() throws ClientException,
			BrokerCoreException, ParseException {
		// start broker 1
		brokerCore1 = new BrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI()
				+ ".+got message.+Publication.+OVERLAY-CONNECT_ACK.+");
		LogSetup.addAppender("MessagePath", messageWatcher);
		// start broker 2 connected with broker 1
		String[] neighbors = { brokerCore1.getBrokerURI() };
		BrokerConfig brockerConfig2 = new BrokerConfig(AllTests.brokerConfig02);
		brockerConfig2.setNeighborURIs(neighbors);
		brokerCore2 = new BrokerCore(brockerConfig2);
		brokerCore2.initialize();
		// start broker 3 isolated
		brokerCore3 = new BrokerCore(AllTests.brokerConfig03);
		brokerCore3.initialize();

		// wait until broker 1 & 2 are connected
		messageWatcher.getMessage();

		// start monitor for broker2 to connect Broker3
		monitorFrame = new MonitorFrame();
		monitorFrame.getOverlayManager().connect(brokerCore2.getBrokerURI());
		monitorFrame.setStatusString(MonitorResources.L_CONNECTED_TO + brokerCore2.getBrokerURI());
		// re-setup message filter
		msgFilter.setPattern(".*" + brokerCore3.getBrokerURI()
				+ ".+got message.+Publication.+OVERLAY-CONNECT_ACK.+");
		// inject the publication to connect Broker3
		String pubString = "[class,'BROKER_CONTROL'],[brokerID,'" + brokerCore2.getBrokerID()
				+ "'],[command,'OVERLAY-CONNECT'],[broker,'" + brokerCore3.getBrokerURI() + "']";
		monitorFrame.getOverlayManager().injectMessage(InjectMessageDialog.INJ_TYPE_PUB, pubString,
				brokerCore2.getBrokerID());

		// waiting for routing finished
		messageWatcher.getMessage();

		OverlayRoutingTable ort1 = brokerCore1.getOverlayManager().getORT();
		OverlayRoutingTable ort2 = brokerCore2.getOverlayManager().getORT();
		OverlayRoutingTable ort3 = brokerCore3.getOverlayManager().getORT();

		assertTrue("The Broker1 should have 1 neighbours", ort1.getNoOfNeighborBrokers() == 1);
		assertTrue("The Broker2 should have 2 neighbours", ort2.getNoOfNeighborBrokers() == 2);
		assertTrue("The Broker3 should have 1 neighbours", ort3.getNoOfNeighborBrokers() == 1);
		assertTrue("The Broker2 is not connected to the Broker1 correctly",
				ort1.isNeighbor(brokerCore2.getBrokerDestination()));
		assertTrue("The Broker3 is not connected to the Broker1 correctly",
				ort3.isNeighbor(brokerCore2.getBrokerDestination()));
		assertTrue("The Broker1 is not connected to the Broker2 correctly",
				ort2.isNeighbor(brokerCore1.getBrokerDestination()));
		assertTrue("The Broker3 is not connected to the Broker2 correctly",
				ort2.isNeighbor(brokerCore3.getBrokerDestination()));
	}
}

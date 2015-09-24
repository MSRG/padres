package ca.utoronto.msrg.padres.test.junit.cyclic;

import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.test.junit.TestBroker;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

public class TestCyclicBroker extends TestBroker {
	
	static {
        // get test specific configuration
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "5");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}

	/**
	 * Test initial Advertisement messages (sent by the InputQueueHandler) are initialized
	 * correctly.
	 * @throws ParseException 
	 */
	public void testInitialAdvMsg() throws ParseException {
		boolean foundInitialAdv = false;
		Map<String, AdvertisementMessage> fullAdvs = brokerCore.getRouter().getAdvertisements();
		String tidPredicate = "[tid,eq,'" + brokerCore.getBrokerID() + "-M0'],";
		Advertisement initialAdv = MessageFactory.createAdvertisementFromString("[class,eq,'BROKER_CONTROL']," + tidPredicate
				+ "[brokerID,isPresent,''],[command,str-contains,'-'],[broker,isPresent,''],"
				+ "[fromID,isPresent,''],[fromURI,isPresent,'']");
		for (AdvertisementMessage tempAdvMsg : fullAdvs.values()) {
			// The initial advertisement should be the first msg sent by the system,
			// so just compare with the messageId:BrokerID + "-M0"
			if (tempAdvMsg.getMessageID().equals(brokerCore.getBrokerID() + "-M0")) {
				Advertisement expectedAdv = tempAdvMsg.getAdvertisement();
				assertTrue("Initial Advertisement sent by the InputQueueHandler is malformed.",
						expectedAdv.equalPredicates(initialAdv));
				foundInitialAdv = true;
				break;
			}
		}
		assertTrue("Initial Advertisement is not initialized at all.", foundInitialAdv);
	}

	/**
	 * Test controller Subscription message (sent by Controller) is initialied correctly.
	 * @throws ParseException 
	 */
	public void testControllerSub() throws ParseException {
		/* TODO: VINOD (DONE) */
		String tidPredicate = ",[tid,eq,$S$Tid]";
		Subscription controllerSub = MessageFactory.createSubscriptionFromString("[class,eq,'BROKER_CONTROL'],[brokerID,eq,'"
				+ brokerCore.getBrokerID() + "']" + tidPredicate);
		boolean foundControllerSub = brokerCore.getRouter().checkStateForSubscription(
				MessageDestination.CONTROLLER, controllerSub);
		assertTrue("The controllerSub is not initialized correctly.", foundControllerSub);
	}

	/**
	 * Test Subscriptions (sent by SystemMonitor) are initialized correctly.
	 * @throws ParseException 
	 */
	public void testSystemMonitorSubs() throws ParseException {
		/* TODO: VINOD (DONE) */
		String tidPredicate = ",[tid,eq,$S$Tid]";
		Subscription monitorSub = MessageFactory.createSubscriptionFromString("[class,eq,BROKER_MONITOR],[brokerID,eq,'"
				+ brokerCore.getBrokerID() + "']" + tidPredicate);
		Subscription nwDiscSub = MessageFactory.createSubscriptionFromString("[class,eq,NETWORK_DISCOVERY]" + tidPredicate);
		Subscription globalSub = MessageFactory.createSubscriptionFromString("[class,eq,GLOBAL_FD],[flag,isPresent,'TEXT']"
				+ tidPredicate);
		boolean countMonitorSub = brokerCore.getRouter().checkStateForSubscription(
				MessageDestination.SYSTEM_MONITOR, monitorSub);
		boolean countNwkSub = brokerCore.getRouter().checkStateForSubscription(
				MessageDestination.SYSTEM_MONITOR, nwDiscSub);
		boolean countGlobalSub = brokerCore.getRouter().checkStateForSubscription(
				MessageDestination.SYSTEM_MONITOR, globalSub);
		assertTrue("The monitorSub is not initialized correctly.", countMonitorSub);
		assertTrue("The NETWORK_RECOVERY Sub is not initialized correctly.", countNwkSub);
		assertTrue("The GLOBAL_FD Sub is not initialized correctly.", countGlobalSub);
	}

	/**
	 * Test Advertisment Messages (sent by Heartbeat) are initialized correctly, including
	 * HeartbeatAdv and FailureDetectAdv
	 * @throws ParseException 
	 */
	public void testHeartbeatAdvMsgs() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
		// Wait until two heart beat messages are received by the input queue from heartbeat manager
		_brokerTester.
		expectRouterAddAdvertisement(
			brokerCore.getBrokerURI(),
			MessageDestination.HEARTBEAT_MANAGER,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "HEARTBEAT_MANAGER").
				addPredicate("brokerID", "isPresent", "TEXT").
				addPredicate("fromID", "eq", brokerCore.getBrokerID()).
				addPredicate("handle", "isPresent", "TEXT").
				addPredicate("type", "isPresent", "TEXT").
				addPredicate("tid", "eq", null)).
		expectRouterAddAdvertisement(
			brokerCore.getBrokerURI(),
			MessageDestination.HEARTBEAT_MANAGER,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "HEARTBEAT_MANAGER").
				addPredicate("detectedID", "isPresent", "TEXT").
				addPredicate("type", "isPresent", "TEXT").
				addPredicate("detectorID", "eq", brokerCore.getBrokerID()).
				addPredicate("tid", "eq", null));
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test HeartbeatSubscription Message (sent by Heartbeat) is initialized correctly.
	 * @throws ParseException 
	 */
	public void testHeartbeatSubMsg() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) (Fails because of possible bug in GenericBrokerTester.matchEvents().) */
		// Wait until the heart beat messages are routed.
		_brokerTester.clearExpected(). // Note: Only clear expected events; remember ads/subs already sent.
		expectRouterAddSubscription(
			brokerCore.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "HEARTBEAT_MANAGER"));
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		boolean foundHeartbeatSub = false;
		String tidPredicate = ",[tid,eq,$S$Tid]";
		Subscription heartbeatSub = MessageFactory.createSubscriptionFromString("[class,eq,HEARTBEAT_MANAGER],"
				+ "[brokerID,eq,'" + brokerCore.getBrokerID() + "']" + tidPredicate);
		foundHeartbeatSub = brokerCore.getRouter().checkStateForSubscription(
				MessageDestination.HEARTBEAT_MANAGER, heartbeatSub);
		assertTrue("The heartBeatSub is not initialized correctly.", foundHeartbeatSub);
	}
}

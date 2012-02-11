package ca.utoronto.msrg.padres.test.junit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.Controller;
import ca.utoronto.msrg.padres.broker.controller.ServerInjectionManager;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

public class TestBroker extends TestCase {

	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore brokerCore;

	protected MessageWatchAppender messageWatcher;

	protected PatternFilter msgFilter;

	@Override
	protected void setUp() throws BrokerCoreException, Exception {
		_brokerTester = new GenericBrokerTester();
		
		// start the broker
		brokerCore = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore.initialize();
		messageWatcher = new MessageWatchAppender();
		messageWatcher.addFilter(new PatternFilter(InputQueueHandler.class.getName(), ""));
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	protected void tearDown() throws Exception {
		brokerCore.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
		_brokerTester = null;
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

	/**
	 * Test initial Advertisement messages (sent by the InputQueueHandler) are initialized
	 * correctly.
	 * @throws ParseException 
	 */
	public void testInitialAdvMsg() throws ParseException {
		boolean foundInitialAdv = false;
		Map<String, AdvertisementMessage> fullAdvs = brokerCore.getRouter().getAdvertisements();
		Advertisement initialAdv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'BROKER_CONTROL'],[brokerID,isPresent,''],[command,str-contains,'-'],"
						+ "[broker,isPresent,''],[fromID,isPresent,''],[fromURI,isPresent,'']");
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
	 * @throws InterruptedException 
	 */
	public void testControllerSub() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","BROKER_MONITOR").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID())).
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","NETWORK_DISCOVERY").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID())).
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","GLOBAL_FD").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID()).
					addPredicate("flag", "isPresent","TEXT"));
	}

	/**
	 * Test Subscriptions (sent by SystemMonitor) are initialized correctly.
	 * @throws ParseException 
	 */
	public void testSystemMonitorSubs() throws ParseException {
		/* TODO: REZA (DONE) */
		_brokerTester.
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","BROKER_MONITOR").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID())).
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","NETWORK_DISCOVERY").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID())).
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(), MessageDestination.SYSTEM_MONITOR,
				new TesterMessagePredicates().
					addPredicate("class", "eq","GLOBAL_FD").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID()).
					addPredicate("flag", "isPresent","TEXT"));
	}

	/**
	 * Test Advertisment Messages (sent by Heartbeat) are initialized correctly, including
	 * HeartbeatAdv and FailureDetectAdv
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHeartbeatAdvMsgs() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.
			expectRouterAddAdvertisement(
				brokerCore.getBrokerURI(),
				MessageDestination.HEARTBEAT_MANAGER,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "HEARTBEAT_MANAGER").
					addPredicate("brokerID", "isPresent", "TEXT").
					addPredicate("fromID" , "eq", brokerCore.getBrokerID()).
					addPredicate("type", "isPresent", "TEXT").
					addPredicate("handle", "isPresent", "TEXT")).
			expectRouterAddAdvertisement(
				brokerCore.getBrokerURI(),
				MessageDestination.HEARTBEAT_MANAGER,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "HEARTBEAT_MANAGER").
					addPredicate("detectorID", "eq", brokerCore.getBrokerID()).
					addPredicate("type", "isPresent", "TEXT").
					addPredicate("detectedID", "isPresent", "TEXT"));
		
		assertTrue("The FailureDetected/Heartbeat Adv is not initialized correctly.",
				_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test HeartbeatSubscription Message (sent by Heartbeat) is initialized correctly.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHeartbeatSubMsg() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(),
				MessageDestination.HEARTBEAT_MANAGER,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "HEARTBEAT_MANAGER").
					addPredicate("brokerID", "eq", brokerCore.getBrokerID()));
		
		assertTrue("The heartBeatSub is not initialized correctly.",
				_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test publication sent for lifeCycle is recevived correctly.
	 * @throws ParseException 
	 */
	public void testPubSentForLifeCycle() throws ParseException {
		Controller controller = brokerCore.getController();
		Publication lifeCycleStopPub = MessageFactory.createPublicationFromString(
				"[class,'BROKER_CONTROL'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'LIFECYCLE-STOP']");
		PublicationMessage lcStopPubmsg = new PublicationMessage(lifeCycleStopPub,
				brokerCore.getNewMessageID());
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(Controller.class.getName(), matchMattern));
		// send the message
		brokerCore.routeMessage(lcStopPubmsg, MessageDestination.INPUTQUEUE);
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = controller.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.CONTROLLER)
						&& lifeCycleStopPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The LIFECYCLE-STOP publication must be routed from broker to controller",
				checkMsg);

		Publication lcResumePub = MessageFactory.createPublicationFromString(
				"[class,'BROKER_CONTROL'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'LIFECYCLE-RESUME']");
		PublicationMessage lcResumePubmsg = new PublicationMessage(lcResumePub,
				brokerCore.getNewMessageID());
		brokerCore.routeMessage(lcResumePubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		checkMsg = false;
		waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = controller.getCurrentPubMsg();
			checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
					&& expectedPubMsg.getNextHopID().equals(MessageDestination.CONTROLLER)
					&& lcResumePub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The LIFECYCLE-RESUME publication must be routed from broker to controller",
				checkMsg);
	}

	/**
	 * Test publication sent for overlayManager is received correctly.
	 * @throws ParseException 
	 */
	public void testPubSentForOverlay() throws ParseException {
		Controller controller = brokerCore.getController();
		// No RMI is tested here.
		Publication olStopPub = MessageFactory.createPublicationFromString(
				"[class,'BROKER_CONTROL'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'OVERLAY-STOP']");
		PublicationMessage olStopPubmsg = new PublicationMessage(olStopPub,
				brokerCore.getNewMessageID());
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(Controller.class.getName(), matchMattern));
		// send the message
		brokerCore.routeMessage(olStopPubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = controller.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.CONTROLLER)
						&& olStopPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The OVERLAY-STOP publication must to routed from broker to controller",
				checkMsg);

		Publication olResumePub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'OVERLAY-RESUME']");
		PublicationMessage olResumePubmsg = new PublicationMessage(olResumePub,
				brokerCore.getNewMessageID());
		brokerCore.routeMessage(olResumePubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		checkMsg = false;
		waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = controller.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.CONTROLLER)
						&& olResumePub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The OVERLAY-RESUME publication must be routed from broker to controller",
				checkMsg);
	}

	/**
	 * Test publication sent for overlayManager is recevived correctly.
	 * @throws ParseException 
	 */
	public void testPubSentForServerInjection() throws ParseException {
		Controller controller = brokerCore.getController();
		ConcurrentHashMap<String, Publication> payload = new ConcurrentHashMap<String, Publication>();
		Advertisement siInjectPubAdv = MessageFactory.createAdvertisementFromString(
				"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
						+ ServerInjectionManager.INJECTION_ID_TAG
						+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
						+ ServerInjectionManager.CMD_PUB_MSG + "]");
		AdvertisementMessage siInjectPubAdvmsg = new AdvertisementMessage(siInjectPubAdv,
				brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
		brokerCore.routeMessage(siInjectPubAdvmsg, MessageDestination.INPUTQUEUE);
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(Controller.class.getName(), matchMattern));
		// Inject dummy pub msg to broker
		Publication siInjectPubPub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
				+ brokerCore.getBrokerID()
				+ "'],[command,'INJECTION-INJECT_PUB'],[INJECTION_ID,'']");
		Publication payloadPub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL']");
		payload.put(ServerInjectionManager.MESSAGE_PAYLOAD_TAG, payloadPub);
		siInjectPubPub.setPayload(payload);
		PublicationMessage siInjectPubPubmsg = new PublicationMessage(siInjectPubPub,
				brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
		brokerCore.routeMessage(siInjectPubPubmsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = controller.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.CONTROLLER)
						&& siInjectPubPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The INJECT_PUB publication must be routed from broker to controller", checkMsg);
	}

	/**
	 * Test publication for systemMonitor is recevived correctly.
	 * @throws ParseException 
	 */
	public void testPubSentForSystemMonitor() throws ParseException {
		SystemMonitor systemMonitor = brokerCore.getSystemMonitor();
		MessageDestination md = new MessageDestination("Broker-D0-0");
		// This Adv is sent in client/monitor/OverlayManager.connect() originally
		Advertisement smAdv = MessageFactory.createAdvertisementFromString(
				"[class,eq,BROKER_MONITOR],[command,isPresent,'TEXT'],"
						+ "[brokerID,isPresent,'TEXT'],"
						+ "[PUBLICATION_INTERVAL,isPresent,12345],"
						+ "[TRACEROUTE_ID,isPresent,'12345']");
		AdvertisementMessage smAdvmsg = new AdvertisementMessage(smAdv,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smAdvmsg, MessageDestination.INPUTQUEUE);

		// The systemMonitor.stopBroker() method will send a "BROKER_INFO" pub,
		// so send this adv first.
		Advertisement smBrokerInfoAdv = MessageFactory.createAdvertisementFromString("[class,eq,'BROKER_INFO'],[brokerID,eq,'"
				+ brokerCore.getBrokerID() + "']");
		AdvertisementMessage smBrokerInfoAdvmsg = new AdvertisementMessage(smBrokerInfoAdv,
				brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
		brokerCore.routeMessage(smBrokerInfoAdvmsg, MessageDestination.INPUTQUEUE);
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(SystemMonitor.class.getName(), matchMattern));
		// send the publication
		Publication smStopPub = MessageFactory.createPublicationFromString("[class,'BROKER_MONITOR'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'STOP']");
		PublicationMessage smStopPubmsg = new PublicationMessage(smStopPub,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smStopPubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = systemMonitor.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.SYSTEM_MONITOR)
						&& smStopPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The STOP publication must be routed from broker to System Monitor", checkMsg);

		Publication smResumePub = MessageFactory.createPublicationFromString("[class,'BROKER_MONITOR'],[brokerID,'"
				+ brokerCore.getBrokerID() + "'],[command,'RESUME']");
		PublicationMessage smResumePubmsg = new PublicationMessage(smResumePub,
				brokerCore.getNewMessageID(), md);

		brokerCore.routeMessage(smResumePubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		checkMsg = false;
		waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = systemMonitor.getCurrentPubMsg();
			checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
					&& expectedPubMsg.getNextHopID().equals(MessageDestination.SYSTEM_MONITOR)
					&& smResumePub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The RESUME publication must be routed from broker to System Monitor", checkMsg);
	}

	/**
	 * Test networkDiscovery publication sent for systemMonitor is recevived correctly.
	 * @throws ParseException 
	 */
	public void testNetworkDiscoveryPubSentForSystemMonitor() throws ParseException {
		SystemMonitor systemMonitor = brokerCore.getSystemMonitor();
		MessageDestination md = new MessageDestination("Broker-D0-0");
		// This Adv is sent in client/monitor/OverlayManager.connect() originally
		Advertisement smAdv = MessageFactory.createAdvertisementFromString("[class,eq,NETWORK_DISCOVERY]");
		AdvertisementMessage smAdvmsg = new AdvertisementMessage(smAdv,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smAdvmsg, MessageDestination.INPUTQUEUE);
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(SystemMonitor.class.getName(), matchMattern));
		// send the publication
		Publication smNetworkDiscoveryPub = MessageFactory.createPublicationFromString("[class,'NETWORK_DISCOVERY']");
		PublicationMessage smNetworkDiscoveryPubmsg = new PublicationMessage(smNetworkDiscoveryPub,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smNetworkDiscoveryPubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 5) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = systemMonitor.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.SYSTEM_MONITOR)
						&& smNetworkDiscoveryPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue(
				"The NETWORK_DISCOVERY publication must be routed from broker to System Monitor",
				checkMsg);
	}

	/**
	 * Test globalFD publication sent for systemMonitor is recevived correctly.
	 * @throws ParseException 
	 */
	public void testGlobalFDPubSentForSystemMonitor() throws ParseException {
		SystemMonitor systemMonitor = brokerCore.getSystemMonitor();
		MessageDestination md = new MessageDestination("Broker-D0-0");

		// This Adv is sent in client/monitor/OverlayManager.connect() originally
		Advertisement smAdv = MessageFactory.createAdvertisementFromString("[class,eq,GLOBAL_FD],[flag,isPresent,'TEXT']");
		AdvertisementMessage smAdvmsg = new AdvertisementMessage(smAdv,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smAdvmsg, MessageDestination.INPUTQUEUE);
		// prepare the filter
		String matchMattern = ".+Publication.+";
		messageWatcher.clearFilters();
		messageWatcher.addFilter(new PatternFilter(SystemMonitor.class.getName(), matchMattern));
		// send the publication
		Publication smGlobalFDPub = MessageFactory.createPublicationFromString("[class,GLOBAL_FD],[flag,'" + false + "']");
		PublicationMessage smGlobalFDPubmsg = new PublicationMessage(smGlobalFDPub,
				brokerCore.getNewMessageID(), md);
		brokerCore.routeMessage(smGlobalFDPubmsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		boolean checkMsg = false;
		int waitCount = 0;
		while (!checkMsg && waitCount < 10) {
			messageWatcher.getMessage();
			PublicationMessage expectedPubMsg = systemMonitor.getCurrentPubMsg();
			if (expectedPubMsg != null)
				checkMsg = expectedPubMsg.getLastHopID().equals(brokerCore.getBrokerDestination())
						&& expectedPubMsg.getNextHopID().equals(MessageDestination.SYSTEM_MONITOR)
						&& smGlobalFDPub.equalVals(expectedPubMsg.getPublication());
			waitCount++;
		}
		assertTrue("The GLOBALFD publication must be routed from broker to System Monitor",
				checkMsg);
	}
}

package ca.utoronto.msrg.padres.test.junit.cyclic;

import java.util.Map;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.LinkInfo;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.AllTests;
import ca.utoronto.msrg.padres.test.junit.MessageWatchAppender;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterClient;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way for testing cycles function.
 * 
 * @author Shuang Hou
 */
public class TestDynamicCycles extends TestCase {

	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "7");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}

	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore brokerCore1;

	protected BrokerCore brokerCore2;

	protected BrokerCore brokerCore3;

	protected BrokerCore brokerCore4;

	protected Client clientA;

	protected Client clientB;

	private MessageWatchAppender messageWatcher;

	private PatternFilter msgFilter;

	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// setup the configurations for a cyclic network
		AllTests.setupCyclicNetwork01();

		// setup message watcher
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		messageWatcher.addFilter(msgFilter);

		// start all the brokers
		brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
		brokerCore2.initialize();
		brokerCore3 = createNewBrokerCore(AllTests.brokerConfig03);
		brokerCore3.initialize();
		brokerCore4 = createNewBrokerCore(AllTests.brokerConfig04);
		brokerCore4.initialize();

		// wait for all the connections done
		msgFilter.setPattern(brokerCore4.getBrokerURI()
				+ ".+got message.+Publication.+OVERLAY-CONNECT_REQ.+");
		LogSetup.addAppender("MessagePath", messageWatcher);
		messageWatcher.getMessage(5);

		// wait a bit further for all other advertisements are routed through
		Thread.sleep(1000);

		// start clientA for Broker1
		clientA = createNewClient(AllTests.clientConfigA);
		clientA.connect(brokerCore1.getBrokerURI());
		// start clientB for Broker4
		clientB = createNewClient(AllTests.clientConfigB);
		clientB.connect(brokerCore4.getBrokerURI());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		brokerCore3.shutdown();
		brokerCore4.shutdown();
		_brokerTester = null;
		LogSetup.removeAppender("MessagePath", messageWatcher);
	}

	protected Client createNewClient(ClientConfig newConfig) throws ClientException {
		return new TesterClient(_brokerTester, newConfig);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

	/**
	 * Test the dynamic routing. Four brokers are setup as a loop. ClientA connects to broker1,
	 * which is a publisher. ClientB connects to broker2, which is a subscriber.
	 * @throws ParseException 
	 */
	public void testDynamicCyclicWithBusyLink() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
		// Stop broker2.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "BROKER_CONTROL").
					addPredicate("command", "eq", "LIFECYCLE-STOP"),
				MessageDestination.CONTROLLER.getDestinationID()).
			expectHandleStopCommand(
				brokerCore2.getBrokerURI());
		brokerCore2.stop();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send an adv. Wait for it to reach all brokers.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE");
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Resume broker 2.
		_brokerTester.clearAll().
			expectHandleResumeCommand(brokerCore2.getBrokerURI());
		brokerCore2.resume();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Stop broker 3.
		_brokerTester.clearAll().
			expectHandleStopCommand(brokerCore3.getBrokerURI());
		brokerCore3.stop();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Send an adv. Wait for it to reach all brokers.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE");
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdA);
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Resume broker 3.
		_brokerTester.clearAll().
			expectHandleResumeCommand(brokerCore3.getBrokerURI());
		brokerCore3.resume();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Send a sub. Wait for two copies of it to reach broker 1 (via brokers 2 and 3).
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 180L).
					addPredicate("tid", "eq", advId1)).
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 180L).
					addPredicate("tid", "eq", advId2));
		clientB.handleCommand("s [class,eq,'stock'],[price,<,180]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Adjust stats so broker 1 thinks the link to broker 3 is slow.
		Map<MessageDestination, LinkInfo> statisticTable = brokerCore1.getOverlayManager().getORT().getStatisticTable();
		MessageDestination b3 = brokerCore3.getBrokerDestination();
		MessageDestination b2 = brokerCore2.getBrokerDestination();
		int msgRate2 = 0;
		if (statisticTable.containsKey(b2)) {
			LinkInfo currentLink2 = statisticTable.get(b2);
			msgRate2 = currentLink2.getMsgRate();
			msgRate2 += 10;
		}
		LinkInfo currentLink3 = statisticTable.get(b3);
		currentLink3.setMsgRate(msgRate2);
		statisticTable.put(b3, currentLink3);

		// Send a pub. Should be routed through broker 2.
		_brokerTester.clearAll().
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 160L),
				brokerCore4.getBrokerURI()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 160L));
		clientA.handleCommand("p [class,'stock'],[price,160]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the dynamic routing.
	 * @throws ParseException 
	 */
	public void testDynamicCyclicWithStopedLink() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
		// Stop broker2.
		_brokerTester.clearAll().
			expectHandleStopCommand(brokerCore2.getBrokerURI());
		brokerCore2.stop();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send an adv. Wait for it to reach all brokers.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE");
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Resume broker 2.
		_brokerTester.clearAll().
			expectHandleResumeCommand(brokerCore2.getBrokerURI());
		brokerCore2.resume();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Stop broker 3.
		_brokerTester.clearAll().
			expectHandleStopCommand(brokerCore3.getBrokerURI());
		brokerCore3.stop();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Send an adv. Wait for it to reach all brokers.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE");
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdA);
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Resume broker 3.
		_brokerTester.clearAll().
			expectHandleResumeCommand(brokerCore3.getBrokerURI());
		brokerCore3.resume();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Send a sub. Wait for two copies of it to reach broker 1 (via brokers 2 and 3).
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 180L).
					addPredicate("tid", "eq", advId1)).
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 180L).
					addPredicate("tid", "eq", advId2));
		clientB.handleCommand("s [class,eq,'stock'],[price,<,180]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Stop broker 3.
		_brokerTester.clearAll().
			expectHandleStopCommand(brokerCore3.getBrokerURI());
		brokerCore3.stop();
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Adjust stats so broker 1 thinks the link to broker 3 is down.
		Map<MessageDestination, LinkInfo> statisticTable = brokerCore1.getOverlayManager().getORT().getStatisticTable();
		MessageDestination b3 = brokerCore3.getBrokerDestination();
		if (statisticTable.containsKey(b3)) {
			LinkInfo currentLink3 = statisticTable.get(b3);
			currentLink3.setStatus();
			statisticTable.put(b3, currentLink3);
		}

		// Send a pub. Should be routed through broker 2.
		_brokerTester.clearAll().
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 160L),
				brokerCore4.getBrokerURI()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 160L));
		clientA.handleCommand("p [class,'stock'],[price,160]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the dynamic routing. Four brokers are setup as a loop. ClientA connects to broker1,
	 * which is a publisher. ClientB connects to broker2, which is a subscriber.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testDynamicCyclicWithCompositeSubscription() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		_brokerTester.clearAll().
		expectRouterAddAdvertisement(
			brokerCore1.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("comp", "eq", "ibm").
				addPredicate("price", "isPresent", 100L)).
				expectRouterAddAdvertisement(
			brokerCore2.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("comp", "eq", "ibm").
				addPredicate("price", "isPresent", 100L)).
		expectRouterAddAdvertisement(
			brokerCore3.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("comp", "eq", "ibm").
				addPredicate("price", "isPresent", 100L)).
		expectRouterAddAdvertisement(
			brokerCore4.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("comp", "eq", "ibm").
				addPredicate("price", "isPresent", 100L)).
		expectRouterAddAdvertisement(
			brokerCore1.getBrokerURI(),
			null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "isPresent", "ibm").
					addPredicate("bargainor", "isPresent", "MS")).
		expectRouterAddAdvertisement(
			brokerCore2.getBrokerURI(),
			null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "isPresent", "ibm").
					addPredicate("bargainor", "isPresent", "MS")).
		expectRouterAddAdvertisement(
			brokerCore3.getBrokerURI(),
			null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "isPresent", "ibm").
					addPredicate("bargainor", "isPresent", "MS")).
		expectRouterAddAdvertisement(
			brokerCore4.getBrokerURI(),
			null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "isPresent", "ibm").
					addPredicate("bargainor", "isPresent", "MS"));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'trade'],[buyer,isPresent,'ibm'],[bargainor,isPresent,'MS']");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("comp", "eq", "$S$X").
					addPredicate("price", ">", 100L)).
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "eq", "$S$X").
					addPredicate("bargainor", "eq", "MS"));
		clientB.handleCommand("cs {{[class,eq,'stock'],[comp,eq,$S$X],[price,>,100]}"
				+ "&{[class,eq,'trade'],[buyer,eq,$S$X],[bargainor,eq,'MS']}}");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("comp", "eq", "ibm").
					addPredicate("tid", "eq", advId1).
					addPredicate("price", "=", 120L)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "trade").
					addPredicate("buyer", "eq", "ibm").
					addPredicate("tid", "eq", advId2).
					addPredicate("bargainor", "eq", "MS"));
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		clientA.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		assertTrue("The publications [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[tid,'" + advId2
				+ "'] and [class,'stock'],[comp,'ibm'],[price,120],[tid,'" + advId1 + "']"
				+ "should be sent to ClientB", _brokerTester.waitUntilExpectedEventsHappen());
	}
}

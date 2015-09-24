package ca.utoronto.msrg.padres.test.junit.cyclic;

import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Uncompositesubscription;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.AllTests;
import ca.utoronto.msrg.padres.test.junit.MessageWatchAppender;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;
import ca.utoronto.msrg.padres.test.junit.TestMultipleBrokers;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test in the scenario of multiple brokers with multiple Clients.
 * 
 * @author Bala Maniymaran
 */
public class TestCyclicMultipleBrokers extends TestMultipleBrokers {

	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "6");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}
	
	public static int EXTENDED_WAIT_TIME = 20000;
	
	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// configure for network type 1
		AllTests.setupStarNetwork01();
		// start the brokers
		brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
		brokerCore3 = createNewBrokerCore(AllTests.brokerConfig03);
		brokerCore4 = createNewBrokerCore(AllTests.brokerConfig04);
		brokerCore5 = createNewBrokerCore(AllTests.brokerConfig05);
		
		_brokerTester.expectCommSystemStart(brokerCore1.getBrokerURI(), null).
			expectCommSystemStart(brokerCore2.getBrokerURI(), null).
			expectCommSystemStart(brokerCore3.getBrokerURI(), null).
			expectCommSystemStart(brokerCore4.getBrokerURI(), null).
			expectCommSystemStart(brokerCore5.getBrokerURI(), null);

		brokerCore1.initialize();
		brokerCore2.initialize();
		brokerCore3.initialize();
		brokerCore4.initialize();
		brokerCore5.initialize();
		
		// setup message watcher
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Publication.+"
				+ brokerCore5.getBrokerURI() + ".+OVERLAY-CONNECT_ACK.+");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
		// wait until the overlay is complete
		messageWatcher.getMessage();
		// start ClientA for Broker2
		clientA = createNewClient(AllTests.clientConfigA);
		clientA.connect(brokerCore2.getBrokerURI());
		// start ClientB for Broker3
		clientB = createNewClient(AllTests.clientConfigB);
		clientB.connect(brokerCore3.getBrokerURI());
		// start ClientC for Broker4
		clientC = createNewClient(AllTests.clientConfigC);
		clientC.connect(brokerCore4.getBrokerURI());
		// start client C for broker 5
		clientD = createNewClient(AllTests.clientConfigD);
		clientD.connect(brokerCore5.getBrokerURI());
		
		_brokerTester.waitUntilExpectedStartsHappen();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	/**
	 * Test advertisement flooding is correct in the network, where these brokers are established
	 * like a star. Broker1 is the core, other four brokers connect to Broker1 seperately.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testAdvFloodInNetwork() throws ParseException, InterruptedException {
		// send an advertisement
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,10]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		
		_brokerTester.clearAll().
			expectReceipt(
					brokerCore1.getBrokerURI(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
						addPredicate("class","eq","stock").
						addPredicate("price", ">", 10L).
						addPredicate("tid", "eq", advId), 
					"INPUTQUEUE").
			expectReceipt(
					brokerCore3.getBrokerURI(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
						addPredicate("class","eq","stock").
						addPredicate("price", ">", 10L).
						addPredicate("tid", "eq", advId), 
					"INPUTQUEUE").
			expectReceipt(
					brokerCore4.getBrokerURI(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
						addPredicate("class","eq","stock").
						addPredicate("price", ">", 10L).
						addPredicate("tid", "eq", advId), 
					"INPUTQUEUE").
			expectReceipt(
					brokerCore5.getBrokerURI(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
						addPredicate("class","eq","stock").
						addPredicate("price", ">", 10L).
						addPredicate("tid", "eq", advId), 
					"INPUTQUEUE").
			expectRouterHandle(
					brokerCore1.getBrokerURI(), 
					brokerCore2.getBrokerDestination(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", ">", 10L).
					addPredicate("tid", "eq", advId)).
			expectRouterHandle(
					brokerCore3.getBrokerURI(), 
					brokerCore1.getBrokerDestination(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", ">", 10L).
					addPredicate("tid", "eq", advId)).
			expectRouterHandle(
					brokerCore4.getBrokerURI(), 
					brokerCore1.getBrokerDestination(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", ">", 10L).
					addPredicate("tid", "eq", advId)).
			expectRouterHandle(
					brokerCore5.getBrokerURI(), 
					brokerCore1.getBrokerDestination(), 
					MessageType.ADVERTISEMENT, 
					new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", ">", 10L).
					addPredicate("tid", "eq", advId));	
		
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue("The advertisement[class,eq,stock],[price,>,10] should be sent to Broker1,3-5",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));

		// send another adv
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,10]");
		String advId1 = brokerCore3.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdB);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", "=", 10L).
					addPredicate("tid", "eq", advId1), 
				"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", "=", 10L).
					addPredicate("tid", "eq", advId1), 
				"INPUTQUEUE").
		expectReceipt(
				brokerCore4.getBrokerURI(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", "=", 10L).
					addPredicate("tid", "eq", advId1), 
				"INPUTQUEUE").
		expectReceipt(
				brokerCore5.getBrokerURI(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
					addPredicate("class","eq","stock").
					addPredicate("price", "=", 10L).
					addPredicate("tid", "eq", advId1), 
				"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 10L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 10L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 10L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore5.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.ADVERTISEMENT, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 10L).
				addPredicate("tid", "eq", advId1));			
		
		brokerCore3.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue("The advertisement[class,eq,stock],[price,=,10] should be sent to Broker1-2,4-5",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test adv/sub route in three brokers, where both broker2 and broker3 connect to broker1. Adv
	 * is sent on broker2, and sub is sent on broker3. That is, adv/sub are on uncontiguous brokers.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testAdvSubRoutingWithOneAdvWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,10]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId));		
				
		// this sub has overlap with adv, need to be forwarded
		clientB.handleCommand("s [class,eq,stock],[price,<,20]");
		assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker1-2",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		_brokerTester.clearAll().
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 5L),
		"INPUTQUEUE");
		
		// this sub has no overlap with adv, no forward
		clientB.handleCommand("s [class,eq,stock],[price,<,5]");	
		assertTrue("There should be no msg routed out on Broker3",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test sub/adv route in three brokers, where both broker2 and broker3 connect to broker1. Adv
	 * is sent on broker2, and sub is sent on broker3. Thus adv/sub are on uncontiguous brokers. The
	 * sub will be sent first, then the adv will be sent. This is a little different from adv/sub
	 * routing.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestCyclicMultipleBrokers#testAdvSubRoutingWithOneAdvWithMoreBrokers()
	 */
	@Override
	public void testSubAdvRoutingWithOneAdvWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,10]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
				
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 20L).
				addPredicate("tid", "eq", advId)).
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 5L),
		"INPUTQUEUE");		
		
		// this sub has overlap with adv, need to be forwarded
		clientB.handleCommand("s [class,eq,stock],[price,<,20]");
		// this sub has no overlap with adv, no forward
		clientB.handleCommand("s [class,eq,stock],[price,<,5]");	
		
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		
		assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker1-2",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test adv/sub route in multiple brokers, where adv and sub are sent on different brokers. Two
	 * advs are sent on the same broker.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testAdvSubRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();

		// send another adv
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).		
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1));		
				
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1-2 twice",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 160L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 160L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore4.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 160L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 160L).
				addPredicate("tid", "eq", advId));		
		
		// this sub has overlap with one of them
		clientC.handleCommand("s [class,eq,'stock'],[price,>,160]");
		assertTrue("The subscription [class,eq,stock],[price,>,160] should be sent to Broker1-2 once",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		_brokerTester.clearAll().
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 70L).
				addPredicate("tid", "eq", advId),
		"INPUTQUEUE").
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 70L).
				addPredicate("tid", "eq", advId1),
		"INPUTQUEUE");
		
		// this sub has no overlap with adv, no forward
		clientD.handleCommand("s [class,eq,'stock'],[price,=,70]");
		assertTrue("There should be no msg routed out on Broker5",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test sub/adv route in multiple brokers, where adv and sub are sent on different brokers. Two
	 * advs are sent on the same broker. The sub will be sent first, then the adv will be sent.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestCyclicMultipleBrokers#testAdvSubRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers()
	 */
	@Override
	public void testSubAdvRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);

		// send another adv
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId));
		
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");

		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1-2 once",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));

		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1));
		
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		assertTrue("Another copy of s [class,eq,stock],[price,<,150] should be sent to Broker1-2",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		// confused what the original code should do here...
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectNegativeReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore4.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", ">", 70L).
				addPredicate("tid", "eq", advId));
		
		// this sub has overlap with one of them
		clientC.handleCommand("s [class,eq,'stock'],[price,>,70]");
		
		assertTrue("s [class,eq,stock],[price,>,70] should be sent to Broker1-2 only once",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
				
		_brokerTester.clearAll().
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 70L).
				addPredicate("tid", "eq", advId),
		"INPUTQUEUE").
		expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "=", 70L).
				addPredicate("tid", "eq", advId1),
		"INPUTQUEUE");
		
		clientC.handleCommand("s [class,eq,'stock'],[attribute1,=,70]");
		
		assertTrue("There should be no msg routed out on Broker4",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));	
	}

	/**
	 * Test adv/sub route in multiple brokers, where adv and sub are sent on different brokers. Two
	 * advs are sent on the different broker.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testAdvSubRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();

		// send another adv
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore4.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore4.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).		
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1));		
				
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1-2 twice",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test sub/adv route in multiple brokers, where adv and sub are sent on different brokers. Two
	 * advs are sent on the different brokers. The sub will be sent first, then the adv will be
	 * sent.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestCyclicMultipleBrokers#testAdvSubRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers
	 */
	@Override
	public void testSubAdvRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
				
		// send another adv
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore4.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).		
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1));		
				
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
		
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();

		brokerCore4.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1-2 twice",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test adv/sub/adv route in multiple brokers, where adv and sub are sent on different brokers.
	 * Two advs are sent on the different brokers. Adv1 will be sent first, sub will be sent second,
	 * adv2 will be sent last.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testAdvSubAdvRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		// send adv
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
				
		// send another adv
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore4.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).
		expectRouterHandle(
				brokerCore2.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId)).		
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore3.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1)).
		expectRouterHandle(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 150L).
				addPredicate("tid", "eq", advId1));		
	
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
			
		brokerCore4.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();
		
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1-2 twice",
					_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test sub/pub match in three brokers, where pub and sub are sent on different brokers.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testSubPubMatchingWithOneSubWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,80]");
		String advID1 = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advID1,
				clientA.getClientDest());
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L).
					addPredicate("tid", "eq", advID1)).
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L));
		// this pub should be matched on clientB
		clientA.handleCommand("p [class,'stock'],[price,100]");
		// this pub should not be matched on clientB
		clientA.handleCommand("p [class,'stock'],[price,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test sub/pub match in three brokers, where pub and sub are sent on different brokers. Two
	 * subs are sent on the same broker.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testSubPubMatchingWithTwoSubsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		_brokerTester.clearAll().
		expectRouterAddAdvertisement(
			brokerCore3.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 80L));
		// send adv
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,80]");
		String advID = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));
		// send sub
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,<,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
		
		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		// this pub match two of them, however, it is sent only once
		clientA.handleCommand("p [class,'stock'],[price,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 90L));
		// this pub match one of them
		clientA.handleCommand("p [class,'stock'],[price,90]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L));
		// this pub match none of them
		clientA.handleCommand("p [class,'stock'],[price,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test sub/pub match in three brokers, where pub and sub are sent on different brokers. Two
	 * subs are sent on the different brokers.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testSubPubMatchingWithTwoSubsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* REZA (NEW) */
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L)).
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L)).
			expectRouterAddAdvertisement(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L));
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,80]");
		String advID = brokerCore2.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));
		// send sub
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		clientC.handleCommand("s [class,eq,'stock'],[price,<,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L).
					addPredicate("tid", "eq", advID)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L).
					addPredicate("tid", "eq", advID));
		// this pub match two of them, however, it is sent only once
		clientA.handleCommand("p [class,'stock'],[price,100]");

		assertTrue("The publication [class,'stock'],[price,100] should be sent to clientB and clientC",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test unsubscribe with multiple brokers, where broker1 is the core, broker2,3,4 connect to the
	 * broker1 seperately. clientA,B,C connect to broker2,3,4 respectively. clientA,B are publisher,
	 * clientC is subscriber. Unadvtisement on broker2.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testUnadvertisementWithMultipleBrokers() throws ParseException, InterruptedException {
		_brokerTester.clearAll().
		expectRouterAddAdvertisement(
			brokerCore4.getBrokerURI(),
			brokerCore1.getBrokerDestination(),
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 100L)).
		expectRouterAddAdvertisement(
			brokerCore4.getBrokerURI(),
			brokerCore1.getBrokerDestination(),
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 100L));
			
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore2.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		clientB.handleCommand("a [class,eq,'stock'],[price,<,150]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
	
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L));
		clientC.handleCommand("s [class,eq,'stock'],[price,>,80]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
		
		_brokerTester.clearAll().
			expectRouterHandleUnAdvertise(
				brokerCore1.getBrokerURI(),
				null,
				null,
				advId).
			expectRouterHandleUnAdvertise(
				brokerCore2.getBrokerURI(),
				null,
				null,
				advId).
			expectRouterHandleUnAdvertise(
				brokerCore3.getBrokerURI(),
				null,
				null,
				advId).
			expectRouterHandleUnAdvertise(
				brokerCore4.getBrokerURI(),
				null,
				null,
				advId);
		Unadvertisement unadv = new Unadvertisement(advId);
		UnadvertisementMessage unadvMsg = new UnadvertisementMessage(unadv,
				brokerCore2.getNewMessageID(), mdA);
		brokerCore2.routeMessage(unadvMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		boolean checkAdvInBroker1 = brokerCore1.getRouter().checkStateForAdvertisement(
				brokerCore2.getBrokerDestination(), adv);
		boolean checkAdvInBroker3 = brokerCore3.getRouter().checkStateForAdvertisement(
				brokerCore1.getBrokerDestination(), adv);
		boolean checkAdvInBroker4 = brokerCore4.getRouter().checkStateForAdvertisement(
				brokerCore1.getBrokerDestination(), adv);
		assertFalse("The advertisement should have removed from broker1", checkAdvInBroker1);
		assertFalse("The advertisement should have removed from broker3", checkAdvInBroker3);
		assertFalse("The advertisement should have removed from broker4", checkAdvInBroker4);
	
		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientC.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L));
		clientB.handleCommand("p [class,'stock'],[price,120]");
		assertTrue("The publication [class,stock],[price,120] should be sent to clientC",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	
		
		if (brokerCore2.getBrokerConfig().isPubConformCheck()) {
			_brokerTester.clearAll().
				expectClientNotReceivePublication(
					clientC.getClientID(),
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "=", 130L));
		} else {
			_brokerTester.clearAll().
				expectClientReceivePublication(
					clientC.getClientID(),
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "=", 130L));
		}
		clientA.handleCommand("p [class,'stock'],[price,130]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test composite subscription with multiple brokers, where broker1 is the core, broker2,3,4
	 * connect to the broker1 seperately.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testCompositeSubscriptionWithMultipleBrokers() throws ParseException, InterruptedException {
		// right now, in padres, composite subscription could not be sent first!!!
		// clientA is subscriber, others are publisher. adv1 from clientB is sent first, then cs is
		// sent from clientA, adv2 is sent at last from clientC
	
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv1 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,=,100],[price,isPresent,100]");
		String advID1 = brokerCore3.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advID1, mdB);
		brokerCore3.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for everything in broker is started
		messageWatcher.getMessage();

		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 80L).
				addPredicate("tid", "eq", advID1), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 80L).
				addPredicate("tid", "eq", advID1), 
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore2.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 80L).
				addPredicate("tid", "eq", advID1)). 
		expectRouterHandle(
				brokerCore3.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("price", "<", 80L).
				addPredicate("tid", "eq", advID1));		
		
		clientA.handleCommand("cs {{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");

		assertTrue("The subscription [class,eq,stock],[price,<,80] should be sent to Broker1,3",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));		

		MessageDestination mdC = clientC.getClientDest();
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		String advID2 = brokerCore4.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advID2, mdC);
		
		_brokerTester.clearAll().
		expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("number", ">", 120L).
				addPredicate("tid", "eq", advID2), 
		"INPUTQUEUE").
		expectReceipt(
				brokerCore4.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("number", ">", 120L).
				addPredicate("tid", "eq", advID2),  
		"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(), 
				brokerCore2.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("number", ">", 120L).
				addPredicate("tid", "eq", advID2)).
		expectRouterHandle(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				MessageType.SUBSCRIPTION, 
				new TesterMessagePredicates().
				addPredicate("class","eq","stock").
				addPredicate("number", ">", 120L).
				addPredicate("tid", "eq", advID2));
		
		brokerCore4.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		
		assertTrue("The subscription [class,eq,stock],[number,>,120] should be sent to Broker1,4",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));		
		
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[number,100],[price,60],[tid,'"
				+ advID1 + "']");
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,150],[tid,'" + advID2 + "']");
		
		_brokerTester.clearAll().
		expectClientReceivePublication(
				clientA.getClientID(), 
				TesterMessagePredicates.createTesterMessagePredicates(pub1)).
		expectClientReceivePublication(
				clientA.getClientID(), 
				TesterMessagePredicates.createTesterMessagePredicates(pub2));
		
		clientB.handleCommand("p [class,'stock'],[number,100],[price,60]");
		clientC.handleCommand("p [class,'stock'],[number,150]");
		
		assertTrue("The publications should be sent to Client A",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}

	/**
	 * Test uncompositesubscription with multiple brokers, where broker1 is the core, broker2,3,4
	 * connect to the broker1 separately.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	@Override
	public void testUnCompositeSubscriptionWithMultipleBrokers() throws ParseException, InterruptedException {
		// right now, in padres, composite subscription could not be sent first!!!
		// clientA,B,C connect to broker2,3,4 respectively clientA is subscriber, others are
		// publisher. adv1 from clientB is sent first, then cs is sent from clientA, adv2 is sent at
		// last from clientC

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore2.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", "=", 100L).
					addPredicate("price", "isPresent", 100L));
		Advertisement adv1 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,=,100],[price,isPresent,100]");
		String advID1 = brokerCore3.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advID1,
				clientB.getClientDest());
		brokerCore3.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));

		// send comp. sub
		CompositeSubscription csub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
		String csubId = brokerCore2.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(csub, csubId, mdA);
		brokerCore2.routeMessage(csMsg, MessageDestination.INPUTQUEUE);

		// send adv
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		String advID2 = brokerCore4.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advID2,
				clientC.getClientDest());
		
		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]"
				+ ",[tid,eq,'" + advID1 + "']");
		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[number,>,120],[tid,eq,'" + advID2
				+ "']");
		
		_brokerTester.clearAll().
		expectRouterAddSubscription(
				brokerCore3.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				TesterMessagePredicates.createTesterMessagePredicates(sub1)).
		expectRouterAddSubscription(
				brokerCore4.getBrokerURI(), 
				brokerCore1.getBrokerDestination(), 
				TesterMessagePredicates.createTesterMessagePredicates(sub2));
				
		brokerCore4.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(
				"Subscriptions forming the composite subscription should be routed to broker3-4",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		Uncompositesubscription uncsub = new Uncompositesubscription(csubId);
		UncompositesubscriptionMessage uncsubMsg = new UncompositesubscriptionMessage(uncsub,
				brokerCore2.getNewMessageID(), mdA);

		_brokerTester.clearAll().
		expectRouterHandleUnsubscribe(
				brokerCore3.getBrokerURI(), 
				null, 
				null, 
				null).
		expectRouterHandleUnsubscribe(
				brokerCore4.getBrokerURI(), 
				null, 
				null, 
				null);
		
		brokerCore2.routeMessage(uncsubMsg, MessageDestination.INPUTQUEUE);
		assertTrue(
				"Subscriptions forming the composite subscription should have been removed from broker3-4",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		boolean checkSubInBroker3 = brokerCore3.getRouter().checkStateForSubscription(
				brokerCore1.getBrokerDestination(), sub1);
		boolean checkSubInBroker4 = brokerCore4.getRouter().checkStateForSubscription(
				brokerCore1.getBrokerDestination(), sub2);
		assertFalse(
				"The subscriptions forming the composite subscription should have been removed from broker3",
				checkSubInBroker3);
		assertFalse(
				"The subscriptions forming the composite subscription should have been removed from broker4",
				checkSubInBroker4);

		
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,100],[price,60],[tid,'" + advID1
				+ "']");
		
		_brokerTester.clearAll().expectClientNotReceivePublication(clientA.getClientID(), TesterMessagePredicates.createTesterMessagePredicates(pub));
		
		// send pub
		clientB.handleCommand("p [class,'stock'],[number,100],[price,60]");
	
		assertTrue(
				"Client A should not receive the publication p [class,'stock'],[number,100],[price,60]",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
		
		pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,150],[tid,'" + advID2 + "']");
		
		_brokerTester.clearAll().expectClientNotReceivePublication(clientA.getClientID(), TesterMessagePredicates.createTesterMessagePredicates(pub));
		
		// send pub
		clientC.handleCommand("p [class,'stock'],[number,150]");
		
		assertTrue(
				"Client A should not receive the publication p [class,'stock'],[number,150]",
				_brokerTester.waitUntilExpectedEventsHappen(EXTENDED_WAIT_TIME));
	}
	
	@Override
	public void testUnsubscribeNotOnOriginalBrokerWithMultipleBrokersWithMultiplePublishers() throws ParseException, InterruptedException {
		super.testUnsubscribeNotOnOriginalBrokerWithMultipleBrokersWithMultiplePublishers();
	}
}

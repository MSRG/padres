package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
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
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterClient;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way for test in the scenario of two brokers with multiple Clients.
 * 
 * @author Shuang Hou, Bala Maniymaran
 */

public class TestTwoBrokers extends TestCase {

	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore brokerCore1;

	protected BrokerCore brokerCore2;

	protected Client clientA;

	protected Client clientB;

	protected MessageWatchAppender messageWatcher;

	protected PatternFilter msgFilter;

	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// setup the standard overlay B1-B2
		AllTests.setupStarNetwork01();
		// start the broker
		// AllTests.brokerConfig01.setHeartBeat(false);
		brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		// start broker 2
		// AllTests.brokerConfig02.setHeartBeat(false);
		brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
		brokerCore2.initialize();
		// setup filter
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI()
				+ ".+got message.+Publication.+OVERLAY-CONNECT_ACK.+");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
		// start swingClientA for Broker1
		clientA = createNewClient(AllTests.clientConfigA);
		clientA.connect(brokerCore1.getBrokerURI());
		// start swingClientB for Broker2
		clientB = createNewClient(AllTests.clientConfigB);
		clientB.connect(brokerCore2.getBrokerURI());
	}

	protected Client createNewClient(ClientConfig newConfig) throws ClientException {
		return new TesterClient(_brokerTester, newConfig);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		brokerCore1 = null;
		brokerCore2 = null;
		clientA = null;
		clientB = null;
		messageWatcher = null;
		_brokerTester = null;
		
		LogSetup.removeAppender("MessagePath", messageWatcher);
	}

	/**
	 * Test connection between two brokers, where they are initialized correctly.
	 */
	public void testConnectionBetweenTwoBrokers() {
		// wait for the message to be routed
		messageWatcher.getMessage();
		// start Broker2 with the initial connection info in the property file
		OverlayRoutingTable ort1 = brokerCore1.getOverlayManager().getORT();
		OverlayRoutingTable ort2 = brokerCore2.getOverlayManager().getORT();

		assertTrue("The Broker1 should have 1 neighbours", ort1.getNoOfNeighborBrokers() == 1);
		assertTrue("The Broker2 should have 1 neighbours", ort2.getNoOfNeighborBrokers() == 1);
		assertTrue("The Broker2 is not connected to the Broker1 correctly",
				ort1.isNeighbor(brokerCore2.getBrokerDestination()));
		assertTrue("The Broker1 is not connected to the Broker2 correctly",
				ort2.isNeighbor(brokerCore1.getBrokerDestination()));
	}

	/**
	 * Test connection and pub/sub match between two brokers, where these two brokers are already
	 * set up and connection msgs need to be injected from monitor.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 */
	public void testConnectionAndPubSubMatchingBetweenTwoExistingBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "=", 100L),
			"INPUTQUEUE");
		clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue("The subscription [class,eq,stock],[attribute,=,100] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());
		clientA.handleCommand("a [class,eq,'stock'],[price,=,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue("There should be no msg routed out on Broker2",
			_brokerTester.waitUntilExpectedEventsHappen());
	
		_brokerTester.clearAll().
			expectSend(
				brokerCore1.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI()).
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		clientA.handleCommand("p [class,'stock'],[price,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test connection between two brokers, where broker2 connected to broker1 already. Try to let
	 * broker1 connect to broker2 again, where connection msgs need to be injected from monitor.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 */
	public void testReConnectionAndPubSubMatchingBetweenTwoBrokers() throws ParseException, InterruptedException {
		/* REZA (NEW-DONE) */
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore2.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		clientA.handleCommand("a [class,eq,'stock'],[price,=,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		clientA.handleCommand("p [class,'stock'],[price,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv/sub route in two contiguous brokers, where adv and sub are sent on different
	 * brokers. adv/sub have the following five kinds of relations : equal, intersect, subset,
	 * superset and no-overlap.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 */
	public void testAdvSubRoutingWithOneAdv() throws ParseException, InterruptedException {
		/* TODO: VINOD/YOUNG (DONE2) */
		// setup message filter
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 100L),
				"INPUTQUEUE");
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		// wait for the routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price", "<", 200L),
			"INPUTQUEUE");
		// adv and sub are intersect
		clientB.handleCommand("s [class,eq,'stock'],[price,<,200]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("number", ">", 50L),
			"INPUTQUEUE");
		// adv is the subset of sub
		clientB.handleCommand("s [class,eq,'stock'],[number,>,50]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("amount", ">", 150L),
			"INPUTQUEUE");
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[amount,>,100]");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdA);
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		// adv is the superset of sub
		clientB.handleCommand("s [class,eq,'stock'],[amount,>,150]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
		expectNegativeReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price", "<", 50L),
			"INPUTQUEUE");
		// adv and sub has no intersect
		clientB.handleCommand("s [class,eq,'stock'],[price,<,50]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub sent on different brokers. The sub will
	 * be sent first, then the adv will be sent, which is different from
	 * testAdvSubRoutingWithOneAdv()
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestTwoBrokers#testAdvSubRoutingWithOneAdv()
	 * 
	 */
	public void testSubAdvRoutingWithOneAdv() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		_brokerTester.clearAll().
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				null).
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 200L),
				null);
		
		// adv and sub have overlap
		clientB.handleCommand("s [class,eq,'stock'],[price,>,100]");
		// adv and sub has no intersect
		clientB.handleCommand("s [class,eq,'stock'],[price,>,200]");
		assertTrue("The subscriptions [class,eq,'stock'],[price,>,100] and [class,eq,'stock'],[price,>,200] should not be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen(2000));
		

		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 200L),
				null).
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L));
		// waiting for routing to finish
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,200]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue("The subscriptions [class,eq,'stock'],[price,>,100] should be sent to Broker1" +
				" and [class,eq,'stock'],[price,>,200] should not.",
				_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub are sent on different brokers. Two advs
	 * are sent on the same broker.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdvSubRoutingWithTwoAdvsWithSameLastHop() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		// Check receipt of advertisement 1
		_brokerTester.
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				null);
		clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		// Check receipt of advertisement 2
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 50L),
				"INPUTQUEUE");
		clientA.handleCommand("a [class,eq,'stock'],[price,<,50]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Check for subscription 1
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L),
					brokerCore1.getBrokerURI()).
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L));
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
		assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());
		
		// Check for subscription 2
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 160L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 160L),
					brokerCore1.getBrokerURI()).
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 160L));
		clientB.handleCommand("s [class,eq,'stock'],[price,>,160]");
		assertTrue("The subscription [class,eq,'stock'],[price,>,160] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());

		// Check for not delivery of subscription 3
		_brokerTester.clearAll().
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 70L),
				"INPUTQUEUE").
			expectNegativeSend(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 70L),
					brokerCore1.getBrokerURI()).
			expectRouterNotHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 70L));
		clientB.handleCommand("s [class,eq,'stock'],[price,=,70]");
		assertTrue("There should be no msg routed out on Broker2",
				_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub sent on different brokers. The sub will
	 * be sent first, then the adv will be sent, which is different from
	 * testAdvSubRoutingWithTwoAdvsWithSameLastHop()
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestTwoBrokers#testAdvSubRoutingWithTwoAdvsWithSameLastHop()
	 */
	public void testSubAdvRoutingWithTwoAdvsWithSameLastHop() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 20L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 20L),
				brokerCore1.getBrokerURI()).
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectSend(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				brokerCore2.getBrokerURI()).
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq","stock").
					addPredicate("price", ">", 20L));
		// the following sub and adv overlap
		clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,>,20]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 50L),
				"INPUTQUEUE").
			expectNegativeReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 50L),
				"INPUTQUEUE");
		clientA.handleCommand("a [class,eq,'stock'],[price,<,50]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));

		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", "<", 50L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", "<", 50L),
				"INPUTQUEUE").
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", ">", 70L),
				null).
			expectRouterNotHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(), 
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", ">", 70L));
		clientB.handleCommand("s [class,eq,'stock'],[attribute,>,70]");
		clientA.handleCommand("a [class,eq,'stock'],[attribute,<,50]");
		assertTrue("There should be no msg routed to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen(2000));

		_brokerTester.clearAll().
			expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("attribute", ">", 100L),
			"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", ">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", ">", 70L),
				null).
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(), 
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", ">", 70L));
		clientA.handleCommand("a [class,eq,'stock'],[attribute,>,100]");
		assertTrue("The subscription [class,eq,stock],[attribute,>,70] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());
		

		_brokerTester.clearAll().
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute1", "=", 70L),
				null);
		clientB.handleCommand("s [class,eq,'stock'],[attribute1,=,70]");
		clientA.handleCommand("a [class,eq,'stock'],[attribute1,<,50]");
		clientA.handleCommand("a [class,eq,'stock'],[attribute1,>,100]");
		assertTrue("There should be no msg routed out on Broker1",
				_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test sub/pub match in two contiguous brokers, where pub and sub are sent on different
	 * brokers.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testSubPubMatchingWithOneSub() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "=", 100L),
			"INPUTQUEUE");
		clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue("The subscription [class,eq,stock],[attribute,=,100] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectSend(
				brokerCore1.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI()).
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		clientA.handleCommand("p [class,'stock'],[price,100]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectNegativeReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L),
				"INPUTQUEUE").
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L));
		clientA.handleCommand("p [class,'stock'],[price,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test pub/sub match in two contiguous brokers, where pub and sub are sent on different
	 * brokers. Pub will be sent first, then the sub will be sent. This is different from
	 * testSubPubMatchingWithOneSub()
	 * @throws ParseException 
	 * 
	 * @see TestTwoBrokers#testSubPubMatchingWithOneSub()
	 */
	public void testPubSubMatching() throws ParseException {
		// setup message filter
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI()
				+ ".+got message.+Subscription.+stock.+");

		clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
		// this pub should be matched on clientB
		clientA.handleCommand("p [class,'stock'],[price,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		// waiting for routing finished
		messageWatcher.getMessage();

		// reset message filter
		msgFilter = new PatternFilter(Client.class.getName());
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);

		// wait for the pub
		String msg = messageWatcher.getMessage(2);

		assertTrue("There should be no pub routed out on Broker1", msg == null);
	}

	/**
	 * Test sub/pub match in two contiguous brokers, where pub and sub are sent on different
	 * brokers. Two subs are sent on the same brokers
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testSubPubMatchingWithTwoSubsWithSameLastHop() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE2) */
		// Check receipt of advertisement 1
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore2.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 80L)).
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));
		clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		clientB.handleCommand("s [class,eq,'stock'],[price,<,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		// this pub match both of two subs, however, only one pub routed out on Broker2
		clientA.handleCommand("p [class,'stock'],[price,100]");
		assertTrue("The publication [class,stock],[price,100] should be sent to clientB",
				_brokerTester.waitUntilExpectedEventsHappen());

		
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "=", 90L),
			"INPUTQUEUE").
		expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 90L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
		expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 90L));

		// this pub match one of them
		clientA.handleCommand("p [class,'stock'],[price,90]");
		assertTrue("The publication [class,stock],[price,90] should be sent to clientB",
				_brokerTester.waitUntilExpectedEventsHappen());


		_brokerTester.clearAll().
		expectNegativeReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "=", 130L),
			"INPUTQUEUE").
		expectNegativeSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 130L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID());
		// this pub match one of them
		clientA.handleCommand("p [class,'stock'],[price,130]");
		assertTrue("There should be no pub routed out on Broker1",
				_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

}

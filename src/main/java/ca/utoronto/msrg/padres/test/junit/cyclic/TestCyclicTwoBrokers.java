package ca.utoronto.msrg.padres.test.junit.cyclic;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;
import ca.utoronto.msrg.padres.test.junit.TestTwoBrokers;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way for test in the scenario of two brokers with multiple Clients.
 * 
 * @author Bala Maniymaran
 */

public class TestCyclicTwoBrokers extends TestTwoBrokers {

	@Override
	public void testConnectionAndPubSubMatchingBetweenTwoExistingBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		String advID = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				"INPUTQUEUE");
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue("The subscriptions [class,eq,'stock'],[price,=,100] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
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
		assertTrue("The publication [class,'stock'],[price,100] should be sent to clientB",
				_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test connection between two brokers, where broker2 connected to broker1 already. Try to let
	 * broker1 connect to broker2 again, where connection msgs need to be injected from monitor.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 */
	public void testReConnectionAndPubSubMatchingBetweenTwoBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
					brokerCore2.getBrokerURI(),
					null,
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "=", 100L));
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		String advID = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
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
					addPredicate("price", "=", 100L));
		// route message
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
		/* TODO: YOUNG (DONE)  */
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				brokerCore1.getBrokerURI());
		clientB.handleCommand("s [class,eq,'stock'],[price,>,100]");
		// adv and sub are intersect
		clientB.handleCommand("s [class,eq,'stock'],[price,<,200]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		boolean subscriptionHasbeenSeen =
			_brokerTester.checkFirstMessageItem(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L).
					addPredicate("tid", "eq", advId),
				"INPUTQUEUE");
		assertTrue(subscriptionHasbeenSeen);
		
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 50L),
				null);
		clientB.handleCommand("s [class,eq,'stock'],[number,>,50]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));
		
		subscriptionHasbeenSeen =
			_brokerTester.checkFirstMessageItem(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 50L).
					addPredicate("tid", "eq", advId1),
				"INPUTQUEUE");
		assertTrue(subscriptionHasbeenSeen);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[amount,>,100]");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdA);
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);		
		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("amount", ">", 150L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 50L),
					"INPUTQUEUE");
		// adv is the superset of sub
		clientB.handleCommand("s [class,eq,'stock'],[amount,>,150]");
		// adv and sub has no intersect
		clientB.handleCommand("s [class,eq,'stock'],[price,<,50]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub sent on different brokers. The sub will
	 * be sent first, then the adv will be sent, which is different from
	 * testAdvSubRoutingWithOneAdv()
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestCyclicTwoBrokers#testAdvSubRoutingWithOneAdv()
	 * 
	 */
	public void testSubAdvRoutingWithOneAdv() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
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
				null);
		// waiting for routing to finish
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,200]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue("The subscriptions [class,eq,'stock'],[price,>,100] should be sent to Broker1" +
				" and [class,eq,'stock'],[price,>,200] should not.",
				_brokerTester.waitUntilExpectedEventsHappen());
		
		boolean subscriptionHasbeenSeen =
			_brokerTester.checkFirstMessageItem(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L).
					addPredicate("tid", "eq", advId),
				"INPUTQUEUE");
		assertTrue(subscriptionHasbeenSeen);
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub are sent on different brokers. Two advs
	 * are sent on the same broker.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdvSubRoutingWithTwoAdvsWithSameLastHop() throws ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "<", 150L),
			null);
		
		// sub has overlap with these two advs, however, this sub is sent only once
		clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
		// waiting for routing finished; two copies of this subscriptions will be routed
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 160L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", "=", 70L),
				"INPUTQUEUE");
		
		// this sub has overlap with one of them
		clientB.handleCommand("s [class,eq,'stock'],[price,>,160]");

		// this sub has no overlap with both of them
		clientB.handleCommand("s [class,eq,'stock'],[price,=,70]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test adv/sub route in two brokers, where adv and sub sent on different brokers. The sub will
	 * be sent first, then the adv will be sent, which is different from
	 * testAdvSubRoutingWithTwoAdvsWithSameLastHop()
	 * @throws ParseException 
	 * @throws InterruptedException 
	 * 
	 * @see TestCyclicTwoBrokers#testAdvSubRoutingWithTwoAdvsWithSameLastHop()
	 */
	public void testSubAdvRoutingWithTwoAdvsWithSameLastHop() throws ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 20L),
				"INPUTQUEUE");	
		// the following sub has overlap with both advs
		clientB.handleCommand("s [class,eq,'stock'],[price,>,20]");
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);		
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,50]");
		String advId1 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("attribute", ">", 70L),
			"INPUTQUEUE");			
		// the following sub has overlap with the second adv
		clientB.handleCommand("s [class,eq,'stock'],[attribute,>,70]");
					
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,<,50]");
		String advId2 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdA);
		brokerCore1.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("attribute", ">", 100L),
				"INPUTQUEUE");
					
		Advertisement adv3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,>,100]");
		String advId3 = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg3 = new AdvertisementMessage(adv3, advId3, mdA);
		brokerCore1.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

			
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("attribute1", "=", 70L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("attribute1", "<", 50L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("attribute1", ">", 100L),
				"INPUTQUEUE");
		// the following sub has no overlap with any adv
		clientB.handleCommand("s [class,eq,'stock'],[attribute1,=,70]");
		
		clientA.handleCommand("a [class,eq,'stock'],[attribute1,<,50]");
		
		clientA.handleCommand("a [class,eq,'stock'],[attribute1,>,100]");
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
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
				null);
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,80]");
		String advID = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		assertTrue("The subscription [class,eq,'stock'],[price,=,100] should be sent to Broker1",
				_brokerTester.waitUntilExpectedEventsHappen(2000));		

		_brokerTester.clearAll().
			expectSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
			expectNegativeSend(
				brokerCore2.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L),
				brokerCore2.getBrokerURI() + "-" + clientB.getClientID()).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L));
		clientA.handleCommand("p [class,'stock'],[price,100]");
		clientA.handleCommand("p [class,'stock'],[price,120]");
		assertTrue("Publication [class,eq,'stock'],[price,=,100] should be sent to clientB " +
				"and Publication [class,eq,'stock'],[price,=,120] should not",
				_brokerTester.waitUntilExpectedEventsHappen(2000));
	}

	/**
	 * Test sub/pub match in two contiguous brokers, where pub and sub are sent on different
	 * brokers. Two subs are sent on the same brokers
	 * @throws ParseException 
	 */
	public void testSubPubMatchingWithTwoSubsWithSameLastHop() throws ParseException {
		// setup message filter
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI()
				+ ".+got message.+Subscription.+stock.+");

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,80]");
		String advID = brokerCore1.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advID, clientA.getClientDest());
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
		// waiting for routing finished
		messageWatcher.getMessage();
		clientB.handleCommand("s [class,eq,'stock'],[price,<,120]");
		// waiting for routing finished
		messageWatcher.getMessage();

		// reset message filter
		msgFilter = new PatternFilter(Client.class.getName());
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);

		// this pub match both of two subs, however, only one pub routed out on Broker2
		clientA.handleCommand("p [class,'stock'],[price,100]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100],[tid,'" + advID + "']");
		// waiting for routing finished
		messageWatcher.getMessage();

		boolean checkPubAtClientB = clientB.checkForReceivedPub(pub);
		assertTrue("The publication [class,stock],[price,100] should be sent to clientB",
				checkPubAtClientB);

		// this pub match one of them
		clientA.handleCommand("p [class,'stock'],[price,90]");
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100],[tid,'" + advID
				+ "']");
		// waiting for routing finished
		messageWatcher.getMessage();

		checkPubAtClientB = clientB.checkForReceivedPub(pub1);
		assertTrue("The publication [class,stock],[price,90] should be sent to clientB",
				checkPubAtClientB);

		// this pub match none of them
		clientA.handleCommand("p [class,'stock'],[price,130]");
		// waiting for routing finished
		String msg = messageWatcher.getMessage(2);

		assertTrue("There should be no pub routed out on Broker1", msg == null);
	}
}

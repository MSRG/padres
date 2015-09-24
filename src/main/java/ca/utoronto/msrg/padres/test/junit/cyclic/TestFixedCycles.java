package ca.utoronto.msrg.padres.test.junit.cyclic;

import java.util.Set;

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
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Uncompositesubscription;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
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

public class TestFixedCycles extends TestCase {
	
	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "6");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}
	
	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore brokerCore1;

	protected BrokerCore brokerCore2;

	protected BrokerCore brokerCore3;

	protected BrokerCore brokerCore4;

	protected BrokerCore brokerCore5;

	protected Client clientA;

	protected Client clientB;

	protected Client clientC;

	protected MessageWatchAppender messageWatcher;

	private PatternFilter msgFilter;

	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// setup configurations according a cyclic network
		AllTests.setupCyclicNetwork03();

		// initialize brokers
		brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
		brokerCore2.initialize();
		brokerCore3 = createNewBrokerCore(AllTests.brokerConfig03);
		brokerCore3.initialize();
		brokerCore4 = createNewBrokerCore(AllTests.brokerConfig04);
		brokerCore4.initialize();
		brokerCore5 = createNewBrokerCore(AllTests.brokerConfig05);
		brokerCore5.initialize();

		// setup message watcher
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		msgFilter.setPattern(brokerCore5.getBrokerURI() + ".+got message.+Publication.+"
				+ brokerCore1.getBrokerURI() + ".+OVERLAY-CONNECT_ACK.+");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);

		// wait until the overlay is complete
		messageWatcher.getMessage();

		// start swingClientA for Broker1
		clientA = createNewClient(AllTests.clientConfigA);
		clientB = createNewClient(AllTests.clientConfigA);
		clientC = createNewClient(AllTests.clientConfigA);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		brokerCore3.shutdown();
		brokerCore4.shutdown();
		brokerCore5.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
	}
	
    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
    	return new TesterClient(_brokerTester, newConfig);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

	/**
	 * Test the overlay connections when the cyclic network is setup
	 */
	public void testCyclic() {
		OverlayRoutingTable ort1 = brokerCore1.getOverlayManager().getORT();
		OverlayRoutingTable ort2 = brokerCore2.getOverlayManager().getORT();
		OverlayRoutingTable ort3 = brokerCore3.getOverlayManager().getORT();

		assertTrue("The Broker1 should have 3 neighbours", ort1.getNoOfNeighborBrokers() == 3);
		assertTrue("The Broker2 should have 2 neighbours", ort2.getNoOfNeighborBrokers() == 2);
		assertTrue("The Broker3 should have 2 neighbours", ort3.getNoOfNeighborBrokers() == 2);
		assertTrue("The Broker2 is not connected to the Broker1 correctly",
				ort1.isNeighbor(brokerCore2.getBrokerDestination()));
		assertTrue("The Broker4 is not connected to the Broker1 correctly",
				ort1.isNeighbor(brokerCore4.getBrokerDestination()));
		assertTrue("The Broker5 is not connected to the Broker1 correctly",
				ort1.isNeighbor(brokerCore5.getBrokerDestination()));
		assertTrue("The Broker1 is not connected to the Broker2 correctly",
				ort2.isNeighbor(brokerCore1.getBrokerDestination()));
		assertTrue("The Broker3 is not connected to the Broker2 correctly",
				ort2.isNeighbor(brokerCore3.getBrokerDestination()));
		assertTrue("The Broker2 is not connected to the Broker3 correctly",
				ort3.isNeighbor(brokerCore2.getBrokerDestination()));
		assertTrue("The Broker4 is not connected to the Broker3 correctly",
				ort3.isNeighbor(brokerCore4.getBrokerDestination()));
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker1, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker3, which is also a publisher.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testNoAdvLoopInNetwork() throws ClientException, ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		// start swingClientA for Broker1
		clientA.connect(brokerCore1.getBrokerURI());
		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());
		// start swingClientC for Broker3
		clientC.connect(brokerCore3.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore3.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore3.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L)).
			expectRouterAddSubscription(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));
		clientB.handleCommand("s [class,eq,'stock'],[price,<,120]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 110L).
					addPredicate("tid", "eq", advId1)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 105L).
					addPredicate("tid", "eq", advId2));
		clientA.handleCommand("p [class,'stock'],[price,110]");
		clientC.handleCommand("p [class,'stock'],[price,105]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker2, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker4, which is also a publisher.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testNoDuplicateSubInNetwork() throws ClientException, ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		// start swingClientA for Broker2
		clientA.connect(brokerCore2.getBrokerURI());

		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());

		// start swingClientC for Broker4
		clientC.connect(brokerCore4.getBrokerURI());
		
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore2.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore4.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore4.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
	
		// reset message filter
		// broker 2 and 4 both should get the subscription
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 130L),
			"INPUTQUEUE").
		expectReceipt(
			brokerCore4.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price", ">", 130L),
			"INPUTQUEUE");

		clientB.handleCommand("s [class,eq,'stock'],[price,>,130]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 140L)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 145L));

		clientA.handleCommand("p [class,'stock'],[price,140]");
		clientC.handleCommand("p [class,'stock'],[price,145]");
		
		// waiting for routing finished
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());		
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker1, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker3, which is also a publisher. Unsub from
	 * clientB.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testUnsubWithCycles1() throws ClientException, ParseException, InterruptedException {
		/* TODO: MANIY, REZA (NEW-ErrorInOriginal) */
		// start swingClientA for Broker1
		clientA.connect(brokerCore1.getBrokerURI());
		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());
		// start swingClientC for Broker3
		clientC.connect(brokerCore3.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			//
				expectRouterAddAdvertisement(
					brokerCore1.getBrokerURI(),
					null,
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "<", 150L)).
				expectRouterAddAdvertisement(
					brokerCore2.getBrokerURI(),
					null,
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "<", 150L)).
				expectRouterAddAdvertisement(
					brokerCore4.getBrokerURI(),
					null,
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "<", 150L)).
				expectRouterAddAdvertisement(
					brokerCore5.getBrokerURI(),
					null,
					new TesterMessagePredicates().
						addPredicate("class", "eq", "stock").
						addPredicate("price", "<", 150L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore3.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore3.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// broker 1 and 3 both should get the subscription
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L)).
			expectRouterAddSubscription(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		String subId1 = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, subId1, mdB);
		brokerCore5.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// broker 1 and 3 both should get the Unsubscription
		_brokerTester.clearAll().
			expectRouterHandleUnsubscribe(
				brokerCore1.getBrokerURI(),
				null,
				null, subId1).
			expectRouterHandleUnsubscribe(
				brokerCore3.getBrokerURI(),
				null,
				null, subId1);
		Unsubscription unsub1 = new Unsubscription(subId1);
		UnsubscriptionMessage unsubMsg1 = new UnsubscriptionMessage(unsub1,
				brokerCore5.getNewMessageID(), mdB);
		brokerCore5.routeMessage(unsubMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock")).
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock"));
		clientA.handleCommand("p [class,'stock'],[price,110]");
		clientC.handleCommand("p [class,'stock'],[price,105]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());


		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 160L));
		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,160]");
		String subId2 = brokerCore5.getNewMessageID();
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, subId2, mdB);
		brokerCore5.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterHandleUnsubscribe(
				brokerCore1.getBrokerURI(),
				null,
				null,
				subId2);
		Unsubscription unsub2 = new Unsubscription(subId2);
		UnsubscriptionMessage unsubMsg2 = new UnsubscriptionMessage(unsub2,
				brokerCore5.getNewMessageID(), mdB);
		brokerCore5.routeMessage(unsubMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker2, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker4, which is also a publisher. Unsub from
	 * clientB.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testUnsubWithCycles2() throws ClientException, ParseException, InterruptedException {
		/* REZA (NEW) */
		// start swingClientA for Broker2
		clientA.connect(brokerCore2.getBrokerURI());

		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());

		// start swingClientC for Broker4
		clientC.connect(brokerCore4.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore2.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore4.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore4.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 130L)).
			expectRouterAddSubscription(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 130L));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,130]");
		String subId1 = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, subId1, mdB);
		brokerCore5.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		

		_brokerTester.clearAll().
			expectRouterHandleUnsubscribe(
				brokerCore2.getBrokerURI(),
				null,
				null,
				subId1);
		Unsubscription unsub1 = new Unsubscription(subId1);
		UnsubscriptionMessage unsubMsg1 = new UnsubscriptionMessage(unsub1,
				brokerCore5.getNewMessageID(), mdB);
		brokerCore5.routeMessage(unsubMsg1, MessageDestination.INPUTQUEUE);
		
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 140L));
		clientA.handleCommand("p [class,'stock'],[price,140]");
		clientC.handleCommand("p [class,'stock'],[price,145]");
		
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));

		
		_brokerTester.clearAll().
			expectRouterNotAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("amount", "<", 120L));
		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[amount,<,120]");
		String subId2 = brokerCore5.getNewMessageID();
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, subId2, mdB);
		brokerCore5.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));
		
		
		_brokerTester.clearAll().
			expectRouterNotHandleUnsubscribe(
				brokerCore2.getBrokerURI(),
				null,
				null,
				subId2);
		Unsubscription unsub2 = new Unsubscription(subId2);
		UnsubscriptionMessage unsubMsg2 = new UnsubscriptionMessage(unsub2,
				brokerCore5.getNewMessageID(), mdB);
		brokerCore5.routeMessage(unsubMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(2000));

		
		Set<Message> messages = brokerCore5.getInputQueue().getCurrentMessagesToRoute();
		int unsubMessagesCount = 0;
		for(Message msg : messages) if(msg.getType().equals(MessageType.UNSUBSCRIPTION)) unsubMessagesCount++;
		assertTrue("There should be no unsub routed out on Broker5", unsubMessagesCount == 0);
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker1, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker3, which is also a publisher. Unadv from
	 * clientA.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testUnadvWithCycles1() throws ClientException, ParseException, InterruptedException {
		/* TODO: REZA (NEWW) */
		// start swingClientA for Broker1
		clientA.connect(brokerCore1.getBrokerURI());
		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());
		// start swingClientC for Broker3
		clientC.connect(brokerCore3.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null, 
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null, 
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		messageWatcher.getMessage();

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore3.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore3.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L)).
			expectRouterAddSubscription(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		String subId1 = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, subId1, mdB);
		brokerCore5.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectRouterHandleUnAdvertise(
				brokerCore5.getBrokerURI(),
				null,
				null,
				advId1);

		Unadvertisement unadv1 = new Unadvertisement(advId1);
		UnadvertisementMessage unadvMsg1 = new UnadvertisementMessage(unadv1,
				brokerCore1.getNewMessageID(), mdA);
		brokerCore1.routeMessage(unadvMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				brokerCore5.getBrokerURI(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "class"));
		clientA.handleCommand("p [class,'stock'],[price,105]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker2, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker4, which is also a publisher. Unadv from
	 * clientA.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testUnadvWithCycles2() throws ClientException, ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		// start swingClientA for Broker2
		clientA.connect(brokerCore2.getBrokerURI());
		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());
		// start swingClientC for Broker4
		clientC.connect(brokerCore4.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 150L));
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		String advId1 = brokerCore2.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,150]");
		String advId2 = brokerCore4.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore4.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// broker 2 and 4 both should get the subscription
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 130L)).
			expectRouterAddSubscription(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 130L));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,130]");
		String subId1 = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, subId1, mdB);
		brokerCore5.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectRouterHandleUnAdvertise(
				brokerCore5.getBrokerURI(),
				null,
				null,
				advId1);
		Unadvertisement unadv1 = new Unadvertisement(advId1);
		UnadvertisementMessage unadvMsg1 = new UnadvertisementMessage(unadv1,
				brokerCore1.getNewMessageID(), mdA);
		brokerCore1.routeMessage(unadvMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 135L));
		clientC.handleCommand("p [class,'stock'],[price,135]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker1, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker3, which is also a publisher.
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testCompositeSubscriptionWithCycles() throws ClientException, ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		// start swingClientA for Broker1
		clientA.connect(brokerCore1.getBrokerURI());
		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());
		// start swingClientC for Broker3
		clientC.connect(brokerCore3.getBrokerURI());

		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 100L).
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore5.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "isPresent", 100L));

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,>,100],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		String advId2 = brokerCore3.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore3.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// broker 1 and 3 both should get the subscription
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 120L)).
			expectRouterAddSubscription(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 80L));
		
		CompositeSubscription csub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
		String csubId = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(csub, csubId, mdB);
		brokerCore5.routeMessage(csMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 110L).
					addPredicate("number", "=", 130L));
		clientA.handleCommand("p [class,'stock'],[price,110],[number,130]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 60L)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 110L).
					addPredicate("number", "=", 130L));
		clientC.handleCommand("p [class,'stock'],[price,60]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/* TODO: VINOD (DONE) */
	/**
	 * Test the routing when four brokers1,2,3,4 are established as a loop, broker5 connects to
	 * broker1. clientA connects to broker1, which is a publisher; clientB connects to broker5,
	 * which is a subscriber; clientC connects to broker3, which is also a publisher.
	 * UncompositeSubscription on clientB
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testUnCompositeSubscriptionWithCycles() throws ClientException, ParseException, InterruptedException {
		// start swingClientA for Broker1
		clientA.connect(brokerCore1.getBrokerURI());

		// start swingClientB for Broker5
		clientB.connect(brokerCore5.getBrokerURI());

		// start swingClientC for Broker3
		clientC.connect(brokerCore3.getBrokerURI());

		// Send adv from broker 1
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore5.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 100L).
					addPredicate("price", ">", 100L),
				"INPUTQUEUE");
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,>,100],[price,>,100]");
		String advId1 = brokerCore1.getNewMessageID();
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, advId1, mdA);
		brokerCore1.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send adv from broker 3
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore5.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "isPresent", 100L),
				"INPUTQUEUE");
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		String advId2 = brokerCore3.getNewMessageID();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, advId2, mdC);
		brokerCore3.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send composite sub from broker 5
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("number", ">", 120L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 80L),
				"INPUTQUEUE");
		CompositeSubscription csub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
		String csubId = brokerCore5.getNewMessageID();
		MessageDestination mdB = clientB.getClientDest();
		CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(csub, csubId, mdB);
		brokerCore5.routeMessage(csMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send uncompositesub from broker 5
		_brokerTester.clearAll().
			expectSend(
				brokerCore5.getBrokerURI(),
				MessageType.UNSUBSCRIPTION,
				csubId + "-s1",
				brokerCore1.getBrokerURI()).
			expectSend(
				brokerCore5.getBrokerURI(),
				MessageType.UNSUBSCRIPTION,
				csubId + "-s2",
				brokerCore1.getBrokerURI()).
			expectRouterHandleUnsubscribe(
				brokerCore1.getBrokerURI(),
				brokerCore5.getBrokerDestination(),
				null,
				csubId + "-s1").
			expectRouterHandleUnsubscribe(
				brokerCore1.getBrokerURI(),
				brokerCore5.getBrokerDestination(),
				null,
				csubId + "-s2").
			expectRouterHandleUnsubscribe(
				brokerCore3.getBrokerURI(),
				null,
				null,
				null);
		Uncompositesubscription uncsub = new Uncompositesubscription(csubId);
		UncompositesubscriptionMessage uncsubMsg = new UncompositesubscriptionMessage(uncsub,
				brokerCore5.getNewMessageID(), mdB);
		brokerCore5.routeMessage(uncsubMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send pubs from client A (broker 1) and client C (broker 3). These should not be routed.
		_brokerTester.clearAll().
			expectNegativeSend(
				brokerCore1.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 110L).
					addPredicate("number", "=", 130L),
				null).
			expectNegativeSend(
				brokerCore3.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 60L),
				null);
		clientA.handleCommand("p [class,'stock'],[price,110],[number,130]");
		clientC.handleCommand("p [class,'stock'],[price,60]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}
}

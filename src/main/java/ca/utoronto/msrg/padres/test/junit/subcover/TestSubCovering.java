package ca.utoronto.msrg.padres.test.junit.subcover;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
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

/**
 * This class provides a way to test subscription covering function with
 * LAZY/ACTIVE strategy. Any other test suites for sub covering must extend this
 * class.
 * 
 * @author Shuang Hou, Bala Maniymaran
 */

public class TestSubCovering extends TestCase {

	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "2");
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

    protected Client clientC;

    protected MessageWatchAppender messageWatcher;

    protected PatternFilter msgFilter;
	
    protected void setUp() throws Exception {
    	_brokerTester = new GenericBrokerTester();
    	
        // setup configurations for a star network
        AllTests.setupStarNetwork01();

        // setup message watcher
        messageWatcher = new MessageWatchAppender();
        msgFilter = new PatternFilter(InputQueueHandler.class.getName());
        messageWatcher.addFilter(msgFilter);

        // start the brokers
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

        Thread.sleep(500);

        // start clients //
        clientA = createNewClient(AllTests.clientConfigA);
        clientA.connect(brokerCore2.getBrokerURI());
        clientB = createNewClient(AllTests.clientConfigB);
        clientB.connect(brokerCore3.getBrokerURI());
        clientC = createNewClient(AllTests.clientConfigC);
        clientC.connect(brokerCore4.getBrokerURI());
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
        LogSetup.removeAppender("MessagePath", messageWatcher);
        
        clientA = clientB = clientC = null;
        brokerCore1 = brokerCore2 = brokerCore3 = brokerCore4 = null;
        _brokerTester = null;
    }

    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
    	return new TesterClient(_brokerTester, newConfig);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB covers/equals to sub2 from clientC
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1CoversOrEqualsToSub2() throws ParseException, InterruptedException {
		/* TODO: VINOD/YOUNG (DONE) */
		// Send adv and wait for it to be routed.
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price",">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price",">", 100L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_1 from clientB (covers sub_2 from clientC).
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 100L),
			"INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,100]"); // sub_1
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_2 from clientC (should not be routed to publisher).
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore4.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 150L),
			"INPUTQUEUE").
		expectNegativeReceipt(
			brokerCore2.getBrokerURI(), 
			MessageType.SUBSCRIPTION, 
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price",">", 150L),
			"INPUTQUEUE");
        clientC.handleCommand("s [class,eq,'stock'],[price,>,150]"); // sub_2
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send publication, and wait to reach both matching subscribers.
        _brokerTester.clearAll().
        expectClientReceivePublication(
        	clientB.getClientID(),
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price","=", 160L)).
		expectClientReceivePublication(
			clientC.getClientID(), 
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price","=", 160L));
        clientA.handleCommand("p [class,'stock'],[price,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send publication, and wait to reach the single matching subscriber.
        _brokerTester.clearAll().
        expectClientReceivePublication(
        	clientB.getClientID(),
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price","=", 120L));
        clientA.handleCommand("p [class,'stock'],[price,120]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// Send adv and wait for it to be routed.
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore3.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount",">", 100L),
			"INPUTQUEUE").
		expectReceipt(
			brokerCore4.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount",">", 100L),
			"INPUTQUEUE");
        clientA.handleCommand("a [class,eq,'stock'],[amount,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_1 from clientB (equals to sub_2 from clientC).
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount",">", 100L),
			"INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[amount,>,100]"); // sub_1
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_2 from clientC (should not be routed to publisher).
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore4.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount",">", 100L),
			"INPUTQUEUE").
		expectNegativeReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount",">", 100L),
			"INPUTQUEUE");
        clientC.handleCommand("s [class,eq,'stock'],[amount,>,100]"); // sub_2
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());;

        // Publication should reach both matching subscribers.
        _brokerTester.clearAll().
		expectReceipt(
			brokerCore3.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount","=", 160L),
			"INPUTQUEUE").
		expectReceipt(
			brokerCore4.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("amount","=", 160L),
			"INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[amount,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB does not interact with sub2 from clientC
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1NoInteractWithSub2() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price",">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L),
				"INPUTQUEUE").
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,150]"); // sub_1
        clientC.handleCommand("s [class,eq,'stock'],[price,<,120]"); // sub_2

        assertTrue("The subscription [class,eq,stock],[price,<,120] should be sent to Broker2.",
                _brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB does not interact with sub2 from clientC
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1InteractWithSub2() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price",">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore2.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L),
				"INPUTQUEUE").
			expectRouterAddSubscription(
				brokerCore2.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 160L));

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,150]"); // sub_1
        clientC.handleCommand("s [class,eq,'stock'],[price,<,160]"); // sub_2

        assertTrue("The subscription [class,eq,stock],[price,<,160] should be sent to Broker2.",
        		_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB covers from clientC, then unsub sub2
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1CoversSub2AndUnsubscribeSub2() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore3.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price",">", 100L),
				"INPUTQUEUE").
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L),
				"INPUTQUEUE").
			expectRouterAddSubscription(
				brokerCore4.getBrokerURI(),
				clientC.getClientDest(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L));

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,100]"); // sub_1

        Subscription sub = MessageFactory
                .createSubscriptionFromString("[class,eq,'stock'],[price,>,150]"); // sub_2
        String subId = brokerCore4.getNewMessageID();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, clientC.getClientDest());
        brokerCore4.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        
        _brokerTester.clearAll().
        	expectReceipt(
        		brokerCore1.getBrokerURI(),
        		MessageType.UNSUBSCRIPTION,
        		"",
    			"INPUTQUEUE").
			expectRouterHandleUnsubscribe(
				brokerCore1.getBrokerURI(),
				null,
				null,
				subId);

        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore4.getNewMessageID(), clientC.getClientDest());

        brokerCore4.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
        
        
        boolean countBroker4 = brokerCore1.getRouter().checkStateForSubscription(
                brokerCore4.getBrokerDestination(), sub);
        assertFalse("The unsubscription should remove the subscription from broker1", countBroker4);


        _brokerTester.clearAll().
        	expectClientReceivePublication(
    			clientB.getClientID(),
    			new TesterMessagePredicates().
    				addPredicate("class", "eq", "stock").
    				addPredicate("price", "=", 160L));
        
        clientA.handleCommand("p [class,'stock'],[price,160]");
        assertTrue("The publication [class,stock],[price,160] should be sent to clientB",
    		_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 separately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB covers from clientC, then unsubscribe sub1
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1CoversSub2AndUnsubscribeSub1() throws ParseException, InterruptedException {
		/* REZA (NEW-DONE) */
        // reset message filter
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L));

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
        	expectRouterAddSubscription(
        		brokerCore1.getBrokerURI(),
        		brokerCore3.getBrokerDestination(),
        		new TesterMessagePredicates().
        			addPredicate("class", "eq", "stock").
        			addPredicate("price", ">", 100L));

        // sub_1 from clientB covers sub_2 from clientC
        Subscription sub = MessageFactory
                .createSubscriptionFromString("[class,eq,'stock'],[price,>,100]"); // sub_1
        String subId = brokerCore3.getNewMessageID();
        MessageDestination mdB = clientB.getClientDest();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdB);
        brokerCore3.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
        

        _brokerTester.clearAll().
	    	expectRouterAddSubscription(
	    		brokerCore1.getBrokerURI(),
	    		brokerCore3.getBrokerDestination(),
	    		new TesterMessagePredicates().
	    			addPredicate("class", "eq", "stock").
	    			addPredicate("price", ">", 100L));
        clientC.handleCommand("s [class,eq,'stock'],[price,>,150]"); // sub_2
        // reset message filter
        msgFilter.setPattern(brokerCore1.getBrokerURI() + ".+sending message.+Unsubscription.+");

        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore3.getNewMessageID(), mdB);
        brokerCore3.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        messageWatcher.getMessage();

        // reset message filter
        msgFilter = new PatternFilter(Client.class.getName());
        msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+");
        messageWatcher.clearFilters();
        messageWatcher.addFilter(msgFilter);

        clientA.handleCommand("p [class,'stock'],[price,160]");
        Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,160]");

        // waiting for routing finished
        messageWatcher.getMessage();

        boolean countBroker1 = clientC.checkForReceivedPub(pub);
        assertTrue("The publication [class,stock],[price,160] should be sent to clientC",
                countBroker1);
    }
}

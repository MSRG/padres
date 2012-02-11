package ca.utoronto.msrg.padres.test.junit.subcover;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test subscription covering function with LAZY
 * strategy.
 * 
 * @author Shuang Hou, Bala Maniymaran
 */

public class TestLazySubCovering extends TestSubCovering {


	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "3");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}

	protected GenericBrokerTester _brokerTester;

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB is covered by sub2 from clientC
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1BeCoveredBySub2WithLazy() throws ParseException, InterruptedException {
		/* TODO: VINOD/YOUNG (DONE) */
		// Send an adv.
        _brokerTester.clearAll().
        expectRouterAddAdvertisement(
			brokerCore4.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 100L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_1 from clientB.
        _brokerTester.clearAll().
        expectRouterAddSubscription(
			brokerCore2.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 100L));
        clientB.handleCommand("s [class,eq,'stock'],[price,>,100]"); // sub_1
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_2 from clientC; sub_2 covers sub_1.
        _brokerTester.clearAll().
        expectSend(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 80L),
			brokerCore2.getBrokerURI()).
        expectRouterAddSubscription(
			brokerCore2.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 80L));
        clientC.handleCommand("s [class,eq,'stock'],[price,>,80]"); // sub_2
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");

        // sub_2 should be sent to broker2 from broker1, since it covers sub_1.
        // sub_1 can only be unsubscribed by the client, we do not trigger this
        // unsubscription here.
        boolean countBroker1 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,>,80] should be sent to Broker2.", countBroker1);
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB is covered by sub2 from clientC, then unsubscribe sub1
     * 
     * @throws ParseException
     * @throws InterruptedException 
     */
    public void testSub1BeCoveredBySub2AndUnsubscribeSub1WithLazy() throws ParseException, InterruptedException {
		/* TODO: VINOD/YOUNG (DONE) (Consistently passes with version 3.) */
		// Send adv and wait for it to propagate.
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
        
        // Send sub_1 from clientB (covered by sub_2 from clientC).
        _brokerTester.clearAll().
        expectRouterAddSubscription(
			brokerCore2.getBrokerURI(),
			null,
			new TesterMessagePredicates().
    			addPredicate("class", "eq", "stock").
				addPredicate("price",">", 150L));
        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,150]"); // sub_1
        String subId = brokerCore3.getNewMessageID();
        MessageDestination mdB = clientB.getClientDest();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdB);
        brokerCore3.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_2 from clientC (covers sub_1 from clientB).
        _brokerTester.clearAll().
        expectRouterAddSubscription(
			brokerCore2.getBrokerURI(),
			null,
			new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price",">", 100L));
        clientC.handleCommand("s [class,eq,'stock'],[price,>,100]"); // sub_2
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Unsub sub_1, and wait for it to propagate.
        _brokerTester.clearAll().
        expectReceipt(
			brokerCore2.getBrokerURI(),
			MessageType.UNSUBSCRIPTION,
			"",
			"INPUTQUEUE");
        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore3.getNewMessageID(), mdB);
        brokerCore3.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send a publication and make sure reaches clientC (sub_2) but not clientB (sub_1).
        _brokerTester.clearAll().
        expectClientReceivePublication(
			clientC.getClientID(),
    		new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("price","=", 160L)).
        expectClientNotReceivePublication(
			clientB.getClientID(),
			null);
        clientA.handleCommand("p [class,'stock'],[price,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB is covered by sub2 from clientC, then unsubscribe sub2
     * 
     * @throws ParseException
     */
    public void testSub1BeCoveredBySub2AndUnsubscribeSub2WithLazy() throws ParseException {
        // reset message filter
        msgFilter.setPattern(brokerCore3.getBrokerURI() + ".+got message.+Advertisement.+");

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        // waiting for the message to be routed
        messageWatcher.getMessage();

        // reset message filter
        msgFilter.setPattern(brokerCore1.getBrokerURI() + ".+sending message.+Subscription.+");

        // sub_1 from clientB covers sub_2 from clientC
        clientB.handleCommand("s [class,eq,'stock'],[price,>,150]"); // sub_1
        // waiting for the message to be routed
        messageWatcher.getMessage();

        Subscription sub = MessageFactory
                .createSubscriptionFromString("[class,eq,'stock'],[price,>,100]"); // sub_2
        String subId = brokerCore4.getNewMessageID();
        MessageDestination mdC = clientC.getClientDest();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdC);
        brokerCore4.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

        // waiting for the message to be routed
        messageWatcher.getMessage();

        // reset message filter
        msgFilter.setPattern(brokerCore1.getBrokerURI() + ".+sending message.+Unsubscription.+");

        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore4.getNewMessageID(), mdC);
        brokerCore4.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        messageWatcher.getMessage();

        // reset message filter
        msgFilter = new PatternFilter(Client.class.getName());
        msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+");
        messageWatcher.clearFilters();
        messageWatcher.addFilter(msgFilter);

        clientA.handleCommand("p [class,'stock'],[price,160]");
        Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,160]");

        // waiting for routing finished
        messageWatcher.getMessage();
        boolean countBroker13 = clientB.checkForReceivedPub(pub);
        assertTrue("The publication [class,stock],[price,160] should be sent to clientB",
                countBroker13);
    }
}

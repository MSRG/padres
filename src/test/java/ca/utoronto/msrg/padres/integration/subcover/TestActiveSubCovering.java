package ca.utoronto.msrg.padres.integration.subcover;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.After;

import org.junit.Test;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.integration.tester.TesterClient;
import ca.utoronto.msrg.padres.integration.tester.TesterMessagePredicates;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * This class provides a way to test subscription covering function with ACTIVE
 * strategy.
 *
 * @author Shuang Hou, Bala Maniymaran
 */

public class TestActiveSubCovering extends Assert {

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

    @Before
    public void setUp() throws Exception {
        setupConfigurations(2, "socket");

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

    @After
    public void tearDown() throws Exception {

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
     * clientB is covered by sub2 from clientC
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    /* TODO: YOUNG (DONE) */
    @Test
    public void testSub1BeCoveredBySub2WithActive() throws ParseException, InterruptedException {
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore1.getBrokerURI(),
                        brokerCore2.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L));
        // sub_1 from clientB is covered by sub_2 from clientC
        clientB.handleCommand("s [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L));
        clientC.handleCommand("s [class,eq,'stock'],[price,>,80]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectReceipt(
                        brokerCore2.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L),
                        "INPUTQUEUE").
                expectClientReceivePublication(
                        clientB.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L)).
                expectClientReceivePublication(
                        clientC.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L));

        clientA.handleCommand("p [class,'stock'],[price,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /* TODO: VINOD, look at this, is this a correct test??? [Looks ok to me. I rewrote it in blackbox2.] */

    /**
     * Test covering with multibrokers, where broker1 is the core, broker2,3,4
     * connect to broker1 seperately. clientA,B,C connect to broker2,3,4
     * respectively. clientA is publisher, clientB,C are subscribers. sub1 from
     * clientB is covered by sub2 from clientC, then unsubscribe sub1
     *
     * @throws ParseException
     * @throws InterruptedException
     */
	/* TODO: VINOD (DONE) */
    @Test
    public void testSub1BeCoveredBySub2AndUnsubscribeSub1WithActive() throws ParseException, InterruptedException {
        // Send adv.
        _brokerTester.clearAll().
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
                                addPredicate("price", ">", 100L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_1 from clientB.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 150L));
        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,150]"); // sub_1
        String subId = brokerCore3.getNewMessageID();
        MessageDestination mdB = clientB.getClientDest();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdB);
        brokerCore3.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send sub_2 from clientC and wait for automatic unsub of sub_1.
        _brokerTester.clearAll().
                expectRouterHandleUnsubscribe(
                        brokerCore2.getBrokerURI(),
                        null,
                        null,
                        subId);
        clientC.handleCommand("s [class,eq,'stock'],[price,>,100]"); // sub_2
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Explicitly unsub sub_1; there should be no unsub message sent to broker2.
        _brokerTester.clearAll().
                expectRouterNotHandleUnsubscribe( //brokerURI, destination, expectedMsgPredicates, subId)
                        brokerCore2.getBrokerURI(),
                        null,
                        null,
                        null);
        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore3.getNewMessageID(), mdB);
        brokerCore3.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send a pub; only clientC (sub_2) should get the pub.
        _brokerTester.clearAll().
                expectClientNotReceivePublication(
                        clientB.getClientID(),
                        null).
                expectClientReceivePublication(
                        clientC.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L));
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
     * @throws InterruptedException
     */
    /* TODO: YOUNG (DONE) */
    @Test
    public void testSub1BeCoveredBySub2AndUnsubscribeSub2WithActive() throws ParseException, InterruptedException {
        _brokerTester.clearAll().
                expectReceipt(
                        brokerCore2.getBrokerURI(),
                        MessageType.ADVERTISEMENT,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L),
                        "INPUTQUEUE");
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectReceipt(
                        brokerCore3.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 150L),
                        "INPUTQUEUE");
        // sub_1 from clientB covers sub_2 from clientC
        Subscription sub1 = MessageFactory
                .createSubscriptionFromString("[class,eq,'stock'],[price,>,150]"); // sub_2
        String subId1 = brokerCore3.getNewMessageID();
        MessageDestination mdB = clientB.getClientDest();
        SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, subId1, mdB);
        brokerCore3.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectReceipt(
                        brokerCore4.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L),
                        "INPUTQUEUE");

        Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]"); // sub_2
        String subId2 = brokerCore4.getNewMessageID();
        MessageDestination mdC = clientC.getClientDest();
        SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, subId2, mdC);
        brokerCore4.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Unsubscription unsub = new Unsubscription(subId2);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore4.getNewMessageID(), mdC);
        brokerCore4.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);

        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectReceipt(
                        brokerCore2.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L),
                        "INPUTQUEUE").
                expectNegativeReceipt(
                        brokerCore3.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 160L),
                        "INPUTQUEUE");

        clientA.handleCommand("p [class,'stock'],[price,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }
}

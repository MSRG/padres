package ca.utoronto.msrg.padres.integration;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.*;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.integration.tester.TesterClient;
import ca.utoronto.msrg.padres.integration.tester.TesterMessagePredicates;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * Created by chris on 12.10.15.
 */
public class TestMultipleBrokersTwo extends Assert {

    protected GenericBrokerTester _brokerTester;

    protected final int DEFAULT_WAIT_TIME = 10000;

    protected BrokerCore brokerCore1;

    protected BrokerCore brokerCore2;

    protected BrokerCore brokerCore3;

    protected BrokerCore brokerCore4;

    protected BrokerCore brokerCore5;

    protected Client clientA;

    protected Client clientB;

    protected Client clientC;

    protected Client clientD;

    protected MessageWatchAppender messageWatcher;

    protected PatternFilter msgFilter;

    @Before
    public void setUp() throws Exception {

        setupConfigurations(3, "socket");
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

    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
        return new TesterClient(_brokerTester, newConfig);
    }

    protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterBrokerCore(_brokerTester, brokerConfig);
    }

    @After
    public void tearDown() throws Exception {

        clientA.shutdown();
        clientB.shutdown();
        clientC.shutdown();
        clientD.shutdown();
        clientA = null;
        clientB = null;
        clientC = null;
        clientD = null;
        brokerCore1.shutdown();
        brokerCore2.shutdown();
        brokerCore3.shutdown();
        brokerCore4.shutdown();
        brokerCore5.shutdown();
        brokerCore1 = null;
        brokerCore2 = null;
        brokerCore3 = null;
        brokerCore4 = null;
        brokerCore5 = null;
        messageWatcher = null;

        LogSetup.removeAppender("MessagePath", messageWatcher);
        _brokerTester.waitUntilExpectedShutdownHappen();
        _brokerTester = null;
    }

    /**
     * Test advertisement flooding is correct in the network, where these brokers are established
     * like a star. Broker1 is the core, other four brokers connect to Broker1 seperately.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testAdvFloodInNetwork() throws ParseException, InterruptedException {
		/* TODO: VINOD (Fails at last assertion for countBroker5.) */
        // send an advertisement
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore4.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore5.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 10L));
        clientA.handleCommand("a [class,eq,stock],[price,>,10]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,stock],[price,>,10]");
        boolean countBroker2 = brokerCore2.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv);
        boolean countBroker3 = brokerCore3.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv);
        boolean countBroker4 = brokerCore4.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv);
        boolean countBroker5 = brokerCore5.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv);
        assertFalse("The advertisement[class,eq,stock],[price,>,10] should be sent to Broker2", countBroker2);
        assertTrue("The advertisement[class,eq,stock],[price,>,10] should be sent to Broker3", countBroker3);
        assertTrue("The advertisement[class,eq,stock],[price,>,10] should be sent to Broker4", countBroker4);
        assertTrue("The advertisement[class,eq,stock],[price,>,10] should be sent to Broker5", countBroker5);

        // send another adv
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore1.getBrokerURI(),
                        brokerCore3.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore4.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 10L)).
                expectRouterAddAdvertisement(
                        brokerCore5.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 10L));
        clientB.handleCommand("a [class,eq,stock],[price,=,10]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,stock],[price,=,10]");
        boolean countBroker1 = brokerCore1.getRouter().checkStateForAdvertisement(brokerCore3.getBrokerDestination(), adv1);
        countBroker2 = brokerCore2.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv1);
        countBroker4 = brokerCore4.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv1);
        countBroker5 = brokerCore5.getRouter().checkStateForAdvertisement(brokerCore1.getBrokerDestination(), adv1);
        assertTrue("The advertisement[class,eq,stock],[price,=,10] should be sent to Broker1", countBroker1);
        assertTrue("The advertisement[class,eq,stock],[price,=,10] should be sent to Broker2", countBroker2);
        assertTrue("The advertisement[class,eq,stock],[price,=,10] should be sent to Broker4", countBroker4);
        assertTrue("The advertisement[class,eq,stock],[price,=,10] should be sent to Broker5", countBroker5);
    }

    /**
     * Test adv/sub route in multiple brokers, where adv and sub are sent on different brokers. Two
     * advs are sent on the different broker.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testAdvSubRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
        // Send advs.
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L)).
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 50L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientC.handleCommand("a [class,eq,'stock'],[price,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Sub overlaps both advs.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore1.getBrokerURI(),
                        brokerCore3.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddSubscription(
                        brokerCore4.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,150]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        boolean checkSubInBroker1 = brokerCore1.getRouter().checkStateForSubscription(brokerCore3.getBrokerDestination(), sub);
        boolean checkSubInBroker2 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        boolean checkSubInBroker4 = brokerCore4.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1", checkSubInBroker1);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker2", checkSubInBroker2);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker4", checkSubInBroker4);
    }

    /**
     * Test adv/sub route in three brokers, where both broker2 and broker3 connect to broker1. Adv
     * is sent on broker2, and sub is sent on broker3. That is, adv/sub are on uncontiguous brokers.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testAdvSubRoutingWithOneAdvWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD X (DONE) (Consistently passes with versions 1, 2, 3, 4) */
        // Send an adv.
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 10L));
        MessageDestination mdA = clientA.getClientDest();
        Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,10]");
        String advId = brokerCore2.getNewMessageID();
        AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
        brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // This sub overlaps the adv, and should be forwarded.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 20L)).
                expectRouterAddSubscription(
                        brokerCore1.getBrokerURI(),
                        brokerCore3.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 20L));
        clientB.handleCommand("s [class,eq,stock],[price,<,20]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,stock],[price,<,20]");
        boolean countBroker3 = brokerCore1.getRouter().checkStateForSubscription(brokerCore3.getBrokerDestination(), sub);
        boolean countBroker1 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker1 by Broker3", countBroker3);
        assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker2 by Broker1", countBroker1);

        // This sub does not overlap adv, and should not be forwarded.
        _brokerTester.clearAll().
                expectNegativeSend(
                        brokerCore3.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 5L),
                        null).
                expectRouterNotAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 5L));
        clientB.handleCommand("s [class,eq,stock],[price,<,5]");
        sub = MessageFactory.createSubscriptionFromString("[class,eq,stock],[price,<,5]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        countBroker1 = brokerCore1.getRouter().checkStateForSubscription(brokerCore3.getBrokerDestination(), sub);
        assertFalse("There should be no msg routed out on Broker3", countBroker1);
    }

    /**
     * Test sub/adv route in three brokers, where both broker2 and broker3 connect to broker1. Adv
     * is sent on broker2, and sub is sent on broker3. Thus adv/sub are on uncontiguous brokers. The
     * sub will be sent first, then the adv will be sent. This is a little different from adv/sub
     * routing.
     *
     * @throws ParseException
     * @throws InterruptedException
     * @see TestMultipleBrokers#testAdvSubRoutingWithOneAdvWithMoreBrokers()
     */
    @Test
    public void testSubAdvRoutingWithOneAdvWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) (Works with versions 3 and 4.) */
        _brokerTester.clearAll().
                expectRouterHandle(
                        brokerCore3.getBrokerURI(),
                        null,
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 20L)).
                expectRouterHandle(
                        brokerCore3.getBrokerURI(),
                        null,
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 5L));
        clientB.handleCommand("s [class,eq,stock],[price,<,20]"); // this sub overlaps adv.
        clientB.handleCommand("s [class,eq,stock],[price,<,5]");  // this sub does not overlap adv.
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // send adv
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 20L)).
                expectRouterNotAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 5L));
        MessageDestination mdA = clientA.getClientDest();
        Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,10]");
        String advId = brokerCore2.getNewMessageID();
        AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
        brokerCore2.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,stock],[price,<,20]");
        boolean countBroker3 = brokerCore1.getRouter().checkStateForSubscription(brokerCore3.getBrokerDestination(), sub);
        boolean countBroker1 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker1 from Broker3", countBroker3);
        assertTrue("The subscription [class,eq,stock],[price,<,20] should be sent to Broker2 from Broker1", countBroker1);

        // TODO: VINOD (DONE) Also test if 2nd sub didn't get routed with the expectRouterNotAddSub call above.
    }

    /**
     * Test adv/sub route in multiple brokers, where adv and sub are sent on different brokers. Two
     * advs are sent on the same broker.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testAdvSubRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) (Consistently passes with version 3) */
        // Send adv.
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L)).
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 50L)).
                expectRouterAddAdvertisement(
                        brokerCore4.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L)).
                expectRouterAddAdvertisement(
                        brokerCore4.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 50L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientA.handleCommand("a [class,eq,'stock'],[price,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Sub overlaps both advs, and should be forwarded (once).
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,150]");
        boolean countBroker3 = brokerCore1.getRouter().checkStateForSubscription(brokerCore3.getBrokerDestination(), sub);
        boolean countBroker1 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker1", countBroker3);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker2", countBroker1);

        // Sub overlaps one adv, and should be forwarded once.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 160L));
        clientC.handleCommand("s [class,eq,'stock'],[price,>,160]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,160]");
        boolean countBroker4 = brokerCore1.getRouter().checkStateForSubscription(brokerCore4.getBrokerDestination(), sub1);
        countBroker1 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub1);
        assertTrue("The subscription [class,eq,stock],[price,>,160] should be sent to Broker1", countBroker4);
        assertTrue("The subscription [class,eq,stock],[price,>,160] should be sent to Broker2", countBroker1);

        // This sub overlaps neither ad.
        _brokerTester.clearAll().
                expectRouterNotAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 70L));
        clientD.handleCommand("s [class,eq,'stock'],[price,=,70]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,70]");
        countBroker1 = brokerCore1.getRouter().checkStateForSubscription(brokerCore5.getBrokerDestination(), sub);
        assertFalse("There should be no msg routed out on Broker5", countBroker1);
    }

    /**
     * Test uncompositesubscription with multiple brokers, where broker1 is the core, broker2,3,4
     * connect to the broker1 seperately.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testUnCompositeSubscriptionWithMultipleBrokers() throws ParseException, InterruptedException {
        // right now, in padres, composite subscription could not be sent first!!!
        // clientA,B,C connect to broker2,3,4 respectively clientA is subscriber, others are
        // publisher. adv1 from clientB is sent first, then cs is sent from clientA, adv2 is sent at
        // last from clientC
		/* TODO: VINOD (Fails at checkSubInBroker3) */
        // Right now, in padres, composite subscription could not be sent first!!!
        // clientA,B,C connect to broker2,3,4 respectively clientA is subscriber, others are
        // publisher. adv1 from clientB is sent first, then cs is sent from clientA, adv2 is sent at
        // last from clientC

        // Send adv.
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", "=", 100L).
                                addPredicate("price", "isPresent", 100L));
        clientB.handleCommand("a [class,eq,'stock'],[number,=,100],[price,isPresent,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Note: I moved this step before the composite sub since the note above
        // says the ad must come before the sub. TODO: Check if this is ok?
        // Send adv.
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", "isPresent", 100L));
        clientC.handleCommand("a [class,eq,'stock'],[number,isPresent,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send composite sub.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore3.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 80L)).
                expectRouterAddSubscription(
                        brokerCore4.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", ">", 120L));
        CompositeSubscription csub = new CompositeSubscription(
                "{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
        String csubId = brokerCore2.getNewMessageID();
        MessageDestination mdA = clientA.getClientDest();
        CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(csub, csubId, mdA);
        brokerCore2.routeMessage(csMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        boolean checkSubInBroker3 = brokerCore3.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), csub.getSubscriptionMap());
        boolean checkSubInBroker4 = brokerCore4.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), csub.getSubscriptionMap());
        assertTrue("One of the subscriptions forming the composite subscription should be routed to broker3", checkSubInBroker3);
        assertTrue("One of the subscriptions forming the composite subscription should be routed to broker4", checkSubInBroker4);

        // Send unsub of composite sub.
        _brokerTester.clearAll().
                expectRouterHandleUnsubscribe(
                        brokerCore3.getBrokerURI(),
                        null,
                        null,
                        csubId + "-s2").
                expectRouterHandleUnsubscribe(
                        brokerCore4.getBrokerURI(),
                        null,
                        null,
                        csubId + "-s1");
        Uncompositesubscription uncsub = new Uncompositesubscription(csubId);
        UncompositesubscriptionMessage uncsubMsg = new UncompositesubscriptionMessage(uncsub,
                brokerCore2.getNewMessageID(), mdA);
        brokerCore2.routeMessage(uncsubMsg, MessageDestination.INPUTQUEUE);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        checkSubInBroker3 = brokerCore3.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), csub.getSubscriptionMap());
        checkSubInBroker4 = brokerCore4.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), csub.getSubscriptionMap());
        assertFalse("The subscriptions forming the composite subscription should have been removed from broker3", checkSubInBroker3);
        assertFalse("The subscriptions forming the composite subscription should have been removed from broker4", checkSubInBroker4);

        // Send pubs, and make sure they don't reach ClientA.
        _brokerTester.clearAll().
                expectClientNotReceivePublication(
                        clientA.getClientID(),
                        null);
        clientB.handleCommand("p [class,'stock'],[number,100],[price,60]");
        clientC.handleCommand("p [class,'stock'],[number,150]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }


    /**
     * Test sub/adv route in multiple brokers, where adv and sub are sent on different brokers. Two
     * advs are sent on the different brokers. The sub will be sent first, then the adv will be
     * sent.
     *
     * @throws ParseException
     * @throws InterruptedException
     * @see TestMultipleBrokers#testAdvSubRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers
     */
    @Test
    public void testSubAdvRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
        // Sub overlaps both ads.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // Send advs.
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddSubscription(
                        brokerCore4.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectNegativeReceipt(
                        brokerCore5.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L),
                        null);
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientC.handleCommand("a [class,eq,'stock'],[price,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,150]");
        boolean checkSubInBroker2 = brokerCore2.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        boolean checkSubInBroker4 = brokerCore4.getRouter().checkStateForSubscription(brokerCore1.getBrokerDestination(), sub);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker2", checkSubInBroker2);
        assertTrue("The subscription [class,eq,stock],[price,<,150] should be sent to Broker4", checkSubInBroker4);
    }

    /**
     * Test sub/pub match in three brokers, where pub and sub are sent on different brokers. Two
     * subs are sent on the different brokers.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testSubPubMatchingWithTwoSubsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
        _brokerTester.
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L)).
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 120L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
        clientC.handleCommand("s [class,eq,'stock'],[price,<,120]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectClientReceivePublication(
                        clientB.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L)).
                expectClientReceivePublication(
                        clientC.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L));
        clientA.handleCommand("p [class,'stock'],[price,100]");

        assertTrue("The publication [class,'stock'],[price,100] should be sent to clientB/clientC",
                _brokerTester.waitUntilExpectedEventsHappen());
    }
}

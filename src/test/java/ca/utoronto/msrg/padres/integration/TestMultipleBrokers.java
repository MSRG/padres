package ca.utoronto.msrg.padres.integration;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import org.junit.*;


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
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
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
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.integration.tester.TesterClient;
import ca.utoronto.msrg.padres.integration.tester.TesterMessagePredicates;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * This class provides a way to test in the scenario of multiple brokers with multiple Clients.
 *
 * @author Shuang Hou, Bala Maniymaran
 */
@RunWith(Parameterized.class)
public class TestMultipleBrokers extends Assert {
    @Parameterized.Parameter(value = 0)
    public int configuration;

    @Parameterized.Parameter(value = 1)
    public String method;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, "socket"}, {1, "rmi"}, {2, "socket"}, {3, "rmi"}, {3, "socket"}, {3, "rmi"}, {4, "socket"}, {4, "rmi"}
        });
    }


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

        setupConfigurations(configuration, method);
        // configure for network type 1
        AllTests.setupStarNetwork01();
        _brokerTester = new GenericBrokerTester();

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
     * Test connection among five brokers, where these brokers are established like a star. Broker1
     * is the core, other four brokers connect to Broker1 seperately.
     *
     * @throws InterruptedException
     */
    @Test
    public void testEstablishedNetwork() throws InterruptedException {
        /* TODO: REZA (NEW-DONE) */
        final long startTime = System.currentTimeMillis();
        boolean loopingEnded = false;
        while (loopingEnded) {
            final long lastTime = System.currentTimeMillis();
            loopingEnded = (lastTime - startTime > DEFAULT_WAIT_TIME);
            OverlayRoutingTable ort1 = brokerCore1.getOverlayManager().getORT();
            OverlayRoutingTable ort2 = brokerCore2.getOverlayManager().getORT();
            OverlayRoutingTable ort3 = brokerCore3.getOverlayManager().getORT();
            OverlayRoutingTable ort4 = brokerCore4.getOverlayManager().getORT();
            OverlayRoutingTable ort5 = brokerCore5.getOverlayManager().getORT();

            try {
                assertTrue("The Broker1 should have 4 neighbours", ort1.getNoOfNeighborBrokers() == 4);
                assertTrue("The Broker2 should have 1 neighbours", ort2.getNoOfNeighborBrokers() == 1);
                assertTrue("The Broker3 should have 1 neighbours", ort3.getNoOfNeighborBrokers() == 1);
                assertTrue("The Broker4 should have 1 neighbours", ort4.getNoOfNeighborBrokers() == 1);
                assertTrue("The Broker5 should have 1 neighbours", ort5.getNoOfNeighborBrokers() == 1);

                assertTrue("The Broker2 is not connected to the Broker1 correctly",
                        ort1.isNeighbor(brokerCore2.getBrokerDestination()));
                assertTrue("The Broker1 is not connected to the Broker2 correctly",
                        ort2.isNeighbor(brokerCore1.getBrokerDestination()));
                assertTrue("The Broker3 is not connected to the Broker1 correctly",
                        ort1.isNeighbor(brokerCore3.getBrokerDestination()));
                assertTrue("The Broker1 is not connected to the Broker3 correctly",
                        ort3.isNeighbor(brokerCore1.getBrokerDestination()));
                assertTrue("The Broker4 is not connected to the Broker1 correctly",
                        ort1.isNeighbor(brokerCore4.getBrokerDestination()));
                assertTrue("The Broker1 is not connected to the Broker4 correctly",
                        ort4.isNeighbor(brokerCore1.getBrokerDestination()));
                assertTrue("The Broker5 is not connected to the Broker1 correctly",
                        ort1.isNeighbor(brokerCore5.getBrokerDestination()));
                assertTrue("The Broker1 is not connected to the Broker5 correctly",
                        ort5.isNeighbor(brokerCore1.getBrokerDestination()));
            } catch (AssertionError x) {
                if (loopingEnded)
                    throw x;
                else
                    Thread.sleep(200);
            } finally {
                loopingEnded = true;
            }
        }
    }



    /**
     * Test sub/adv route in multiple brokers, where adv and sub are sent on different brokers. Two
     * advs are sent on the same broker. The sub will be sent first, then the adv will be sent.
     *
     * @throws ParseException
     * @throws InterruptedException
     * @see TestMultipleBrokers#testAdvSubRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers()
     */
    @Test
    public void testSubAdvRoutingWithTwoAdvsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore1.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        // sub has overlap with these two advs, however, this sub is sent only once
        clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        assertTrue("Brokers 3 and 1 must receive subscription",
                _brokerTester.waitUntilExpectedEventsHappen());

        clientA.handleCommand("a [class,eq,'stock'],[price,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterNotAddSubscription(
                        brokerCore1.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("attribute", ">", 70L));
        // the following sub has overlap with the second adv
        clientC.handleCommand("s [class,eq,'stock'],[attribute,>,70]");
        clientA.handleCommand("a [class,eq,'stock'],[attribute,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());


        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("attribute", ">", 70L));
        clientA.handleCommand("a [class,eq,'stock'],[attribute,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterNotAddSubscription(
                        brokerCore1.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("attribute1", "=", 70L));
        clientC.handleCommand("s [class,eq,'stock'],[attribute1,=,70]");
        clientA.handleCommand("a [class,eq,'stock'],[attribute1,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterNotAddSubscription(
                        brokerCore1.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("attribute1", "=", 70L));
        clientA.handleCommand("a [class,eq,'stock'],[attribute1,>,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test adv/sub/adv route in multiple brokers, where adv and sub are sent on different brokers.
     * Two advs are sent on the different brokers. Adv1 will be sent first, sub will be sent second,
     * adv2 will be sent last.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testAdvSubAdvRoutingWithTwoAdvsWithDiffLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 100L)).
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,150]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore4.getBrokerURI(),
                        clientC.getClientDest(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 50L)).
                expectRouterNotAddSubscription(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterNotAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddSubscription(
                        brokerCore4.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));
        clientC.handleCommand("a [class,eq,'stock'],[price,<,50]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test sub/pub match in three brokers, where pub and sub are sent on different brokers.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
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
                                addPredicate("price", "=", 100L)).
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
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testSubPubMatchingWithTwoSubsWithSameLastHopWithMoreBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA++ (DONE) */
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L));
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

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
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        // this pub match two of them, however, it is sent only once
        _brokerTester.clearAll().
                expectClientReceivePublication(
                        clientB.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L));
        clientA.handleCommand("p [class,'stock'],[price,100]");
        assertTrue("The publication [class,stock],[price,100] should be sent to clientB",
                _brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
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
                expectClientNotReceivePublication(
                        clientB.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 120L));
        // this pub match none of them
        clientA.handleCommand("p [class,'stock'],[price,120]");
        assertTrue("There should be no pub routed out on Broker2",
                _brokerTester.waitUntilExpectedEventsHappen());
    }


    /**
     * Test unsubscribe with multiple brokers, where broker1 is the core, broker2,3,4 connect to the
     * broker1 seperately. clientA,B,C connect to broker2,3,4 respectively. clientA,B are publisher,
     * clientC is subscriber. Unsubscription is sent out on clientC.
     *
     * @throws ParseException
     */
    @Test
    public void testUnsubscribeOnOriginalBrokerWithMultipleBrokers() throws ParseException {
        // re-setup filter
        msgFilter.setPattern(".*" + brokerCore4.getBrokerURI()
                + ".+got message.+Advertisement.+stock.+");
        // send adv
        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        // waiting for routing finished
        messageWatcher.getMessage();

        // send adv
        clientB.handleCommand("a [class,eq,'stock'],[price,<,150]");
        // waiting for routing finished
        messageWatcher.getMessage();

        // re-setup filter
        msgFilter.setPattern(".*" + brokerCore4.getBrokerURI()
                + ".+sending message.+Subscription.+stock.+");
        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
        String subId = brokerCore4.getNewMessageID();
        MessageDestination mdC = clientC.getClientDest();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdC);
        brokerCore4.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        messageWatcher.getMessage();

        // re-setup filter
        msgFilter.setPattern(".+got message.+Unsubscription.+" + subId + ".*");
        // send unsub
        Unsubscription unsub = new Unsubscription(subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
                brokerCore4.getNewMessageID(), mdC);
        brokerCore4.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);

        // waiting for routing finished; three routers should get the unsubscription message
        int msgCount = 0, waitCount = 0;
        while (msgCount < 4 && waitCount < 5) {
            String msg = messageWatcher.getMessage();
            if (msg != null)
                msgCount++;
            waitCount++;
        }

        boolean checkSubInBroker1 = brokerCore1.getRouter().checkStateForSubscription(
                brokerCore4.getBrokerDestination(), sub);
        boolean checkSubInBroker2 = brokerCore2.getRouter().checkStateForSubscription(
                brokerCore1.getBrokerDestination(), sub);
        boolean checkSubInBroker3 = brokerCore3.getRouter().checkStateForSubscription(
                brokerCore1.getBrokerDestination(), sub);
        assertFalse("The subscription should have been removed from broker1", checkSubInBroker1);
        assertFalse("The subscription should have been removed from broker2", checkSubInBroker2);
        assertFalse("The subscription should have been removed from broker3", checkSubInBroker3);

        // re-setup filter
        msgFilter = new PatternFilter(Client.class.getName());
        msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
        messageWatcher.clearFilters();
        messageWatcher.addFilter(msgFilter);

        // send pub
        clientA.handleCommand("p [class,'stock'],[price,110]");
        Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
        // waiting for routing finished
        messageWatcher.getMessage(2);
        Publication receivedPub = clientC.getCurrentPub();
        boolean checkReceivedPubAtClientC = pub.equalVals(receivedPub);

        assertFalse("There should be no pub routed out on Broker2", checkReceivedPubAtClientC);
    }

    /**
     * Test unsubscribe with multiple brokers, where broker1 is the core, broker2,3 connect to the
     * broker1 seperately. broker4 connects to the broker3. clientA,B connect to broker2,1
     * respectively. clientA is publisher. clientC connect to broker3, and clientC is subscriber.
     * clientD connects to broker4, and clientD is publisher. Unsubscription is sent out on clientB.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testUnsubscribeNotOnOriginalBrokerWithMultipleBrokersWithMultiplePublishers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
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
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L)).
                expectRouterAddAdvertisement(
                        brokerCore3.getBrokerURI(),
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
                        brokerCore1.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 150L));

        clientA.handleCommand("a [class,eq,'stock'],[price,>,100]");
        clientD.handleCommand("a [class,eq,'stock'],[price,<,150]");
        assertTrue("Brokers must receive advertisements.",
                _brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore1.getBrokerURI(),
                        brokerCore4.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L)).
                expectRouterAddSubscription(
                        brokerCore5.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L)).
                expectRouterAddSubscription(
                        brokerCore2.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", ">", 80L));

        clientA.handleCommand("p [class,'stock'],[price,112]");

        Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
        String subId = brokerCore4.getNewMessageID();
        SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, clientC.getClientDest());
        brokerCore4.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
        assertTrue("Subscription must reach broker1.",
                _brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterHandleUnsubscribe(
                        brokerCore2.getBrokerURI(),
                        null, null, subId).
                expectRouterHandleUnsubscribe(
                        brokerCore5.getBrokerURI(),
                        null, null, subId);
        UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(new Unsubscription(subId),
                brokerCore1.getNewMessageID(), clientB.getClientDest());
        brokerCore1.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
        assertTrue("Unsubscription must reach broker2/3/4", _brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectClientNotReceivePublication(
                        clientC.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 110L));
        clientA.handleCommand("p [class,'stock'],[price,110]");

        assertTrue("There should be no pub routed out on Broker2",
                _brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test unsubscribe with multiple brokers, where broker1 is the core, broker2,3,4 connect to the
     * broker1 seperately. clientA,B,C connect to broker2,3,4 respectively. clientA,B are publisher,
     * clientC is subscriber. Unadvtisement on broker2.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testUnadvertisementWithMultipleBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
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
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

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
                _brokerTester.waitUntilExpectedEventsHappen());


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
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test composite subscription with multiple brokers, where broker1 is the core, broker2,3,4
     * connect to the broker1 seperately.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testCompositeSubscriptionWithMultipleBrokers() throws ParseException, InterruptedException {
		/* TODO: REZA (DONE) */
        // right now, in PADRES, composite subscription could not be sent first!!!
        // clientA is subscriber, others are publisher. adv1 from clientB is sent first, then cs is
        // sent from clientA, adv2 is sent at last from clientC
        _brokerTester.clearAll().
                expectRouterAddAdvertisement(
                        brokerCore2.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", "=", 100L).
                                addPredicate("price", "isPresent", 100L));
        clientB.handleCommand("a [class,eq,'stock'],[number,=,100],[price,isPresent,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterNotAddSubscription(
                        brokerCore3.getBrokerURI(),
                        null,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", ">", 120L)).
                expectRouterAddSubscription(
                        brokerCore3.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 80L));
        clientA.handleCommand("cs {{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectRouterAddSubscription(
                        brokerCore1.getBrokerURI(),
                        brokerCore2.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", ">", 120L)).
                expectRouterAddSubscription(
                        brokerCore4.getBrokerURI(),
                        brokerCore1.getBrokerDestination(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", ">", 120L));
        clientC.handleCommand("a [class,eq,'stock'],[number,isPresent,100]");
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

        _brokerTester.clearAll().
                expectClientReceivePublication(
                        clientA.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", "=", 100L).
                                addPredicate("price", "=", 60L)).
                expectClientReceivePublication(
                        clientA.getClientID(),
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("number", "=", 150L));
        clientB.handleCommand("p [class,'stock'],[number,100],[price,60]");
        clientC.handleCommand("p [class,'stock'],[number,150]");
        assertTrue("The publication [class,'stock'],[number,100],[price,60] and [class,'stock'],[number,150] should be sent to clientA",
                _brokerTester.waitUntilExpectedEventsHappen());
    }


}

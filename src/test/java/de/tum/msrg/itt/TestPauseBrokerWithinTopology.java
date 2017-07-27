package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.common.util.Sleep;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterClient;
import ca.utoronto.msrg.padres.integration.tester.TesterMessagePredicates;
import de.tum.msrg.itt.tester.TesterIttBrokerCore;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;
import static org.junit.Assert.assertTrue;

/**
 * Created by pxsalehi on 23.06.16.
 */
@RunWith(Parameterized.class)
public class TestPauseBrokerWithinTopology {
    @Parameterized.Parameter(value = 0)
    public int configuration;

    @Parameterized.Parameter(value = 1)
    public String method;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // config 1 is acyclic and no sub covering
                { 1, "socket" }
        });
    }

    protected final int DEFAULT_WAIT_TIME = 10000;
    protected GenericBrokerTester _brokerTester;
    protected IttBrokerCore broker1;
    protected IttBrokerCore broker2;
    protected IttBrokerCore broker3;
    protected Client clientA;
    protected Client clientB;
    protected TestingServer zkTestServer;
    protected CuratorFramework cli;
    // log
    protected MessageWatchAppender messageWatcher;
    protected PatternFilter msgFilter;

    @Before
    public void setUp() throws Exception {
        zkTestServer = new TestingServer(2181);
        cli = CuratorFrameworkFactory.newClient(zkTestServer.getConnectString(), new RetryOneTime(2000));
        cli.start();
        // setup paths first
        cli.create().creatingParentsIfNeeded().forPath(IttAgent.ZK_ROOT);
        setupConfigurations(configuration, method);
        // create a star topo with broker1 in the middle
        // clientA -- b2 --- b1 --- b3 -- clientB
        AllTests.setupStarNetwork01();
        _brokerTester = new GenericBrokerTester();
        // setup the standard overlay B1-B2-B3
        // start the brokers
        broker1 = createNewBrokerCore(AllTests.brokerConfig01);
        broker2 = createNewBrokerCore(AllTests.brokerConfig02);
        broker3 = createNewBrokerCore(AllTests.brokerConfig03);
        _brokerTester.expectCommSystemStart(broker1.getBrokerURI(), null).
                expectCommSystemStart(broker2.getBrokerURI(), null).
                expectCommSystemStart(broker3.getBrokerURI(), null);
        broker1.initialize();
        broker2.initialize();
        broker3.initialize();
        // setup filter
        messageWatcher = new MessageWatchAppender();
        msgFilter = new PatternFilter(InputQueueHandler.class.getName());
        msgFilter.setPattern(".*" + broker1.getBrokerURI() + ".+got message.+Publication.+OVERLAY-CONNECT_ACK.+");
        messageWatcher.addFilter(msgFilter);
        LogSetup.addAppender("MessagePath", messageWatcher);
        messageWatcher.getMessage();
        clientA = createNewClient(AllTests.clientConfigA);
        clientA.connect(broker2.getBrokerURI());
        clientB = createNewClient(AllTests.clientConfigB);
        clientB.connect(broker3.getBrokerURI());
        _brokerTester.waitUntilExpectedStartsHappen();
    }

    @After
    public void tearDown() throws Exception {
        LogSetup.removeAppender("MessagePath", messageWatcher);
        clientA.shutdown();
        clientB.shutdown();
        clientA = null;
        clientB = null;
        broker1.shutdown();
        broker2.shutdown();
        broker3.shutdown();
        broker1 = null;
        broker2 = null;
        broker3 = null;
        messageWatcher = null;
        _brokerTester = null;
        cli.close();
        zkTestServer.stop();
        // pause to free addresses
        Sleep.sleep(5000);
    }

    protected IttBrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterIttBrokerCore(_brokerTester, brokerConfig);
    }

    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
        return new TesterClient(_brokerTester, newConfig);
    }

    @Test
    public void testConnectionBetweenThreeBrokers() {
        // wait for the message to be routed
        messageWatcher.getMessage();
        // start Broker2 with the initial connection info in the property file
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();

        assertTrue("The Broker1 should have 2 neighbours", ort1.getNoOfNeighborBrokers() == 2);
        assertTrue("The Broker2 should have 1 neighbours", ort2.getNoOfNeighborBrokers() == 1);
        assertTrue("The Broker3 should have 1 neighbours", ort3.getNoOfNeighborBrokers() == 1);

        assertTrue("The Broker2 is not connected to the Broker1 correctly",
                ort1.isNeighbor(broker2.getBrokerDestination()));
        assertTrue("The Broker1 is not connected to the Broker2 correctly",
                ort2.isNeighbor(broker1.getBrokerDestination()));

        assertTrue("The Broker3 is not connected to the Broker1 correctly",
                ort1.isNeighbor(broker3.getBrokerDestination()));
        assertTrue("The Broker1 is not connected to the Broker3 correctly",
                ort3.isNeighbor(broker1.getBrokerDestination()));
    }

    @Test
    public void testStopInputQueueOnBroker1() throws ParseException, InterruptedException {
        // wait till all is propagated which means adv reaches b3
        _brokerTester.clearAll().expectReceipt(
                broker3.getBrokerURI(),
                MessageType.ADVERTISEMENT,
                new TesterMessagePredicates().
                        addPredicate("class", "eq", "stock").
                        addPredicate("price", ">", 80L),
                "INPUTQUEUE");
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        _brokerTester.waitUntilExpectedEventsHappen();
        _brokerTester.clearAll().
                expectReceipt(
                        broker3.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L),
                        "INPUTQUEUE").
                expectReceipt(
                        broker1.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L),
                        "INPUTQUEUE");
        broker1.pauseNormalTraffic();
        clientB.handleCommand("s [class,eq,'stock'],[price,=,100]");
        assertTrue("The subscription should be received by Broker1 and Broker3",
                _brokerTester.waitUntilExpectedEventsHappen());
        _brokerTester.clearAll().expectReceipt(
                        broker2.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 100L),
                        "INPUTQUEUE");
        assertFalse("Broker1's input queue is stopped. No msg should get to Broker2!",
                _brokerTester.waitUntilExpectedEventsHappen());
        broker1.resumeNormalTraffic();
        assertTrue("The subscription should be received by Broker2",
                _brokerTester.waitUntilExpectedEventsHappen());
    }

    @Test
    public void testStopInputQueueOnBroker1WithCtrlMsg() throws ParseException, InterruptedException {
        // clientA, publishes a normal pub and a control message and clientB is sub'ed to both
        // if b1's queue is paused, clientB will receive only the control message
        // wait till all is propagated, which means b2 receives both subs
        _brokerTester.clearAll().
                expectReceipt(
                        broker2.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "BROKER_CONTROL"),
                        "INPUTQUEUE").
                expectReceipt(
                        broker2.getBrokerURI(),
                        MessageType.SUBSCRIPTION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "<", 100),
                        "INPUTQUEUE");
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        clientA.handleCommand("a [class,eq,'BROKER_CONTROL'],[ID,eq,'clientA']");
        clientB.handleCommand("s [class,eq,'BROKER_CONTROL']");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,100]");
        _brokerTester.waitUntilExpectedEventsHappen();
        broker1.pauseNormalTraffic();
        // upon publishing a pub for each adv, b2 receives both pubs, b1 and b3 receive only the control msgs
        _brokerTester.clearAll().
                expectReceipt(
                        broker2.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "BROKER_CONTROL").
                                addPredicate("ID", "eq", "clientA"),
                        "INPUTQUEUE").
                expectReceipt(
                        broker2.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 90L),
                        "INPUTQUEUE").
                expectReceipt(
                        broker1.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "BROKER_CONTROL").
                                addPredicate("ID", "eq", "clientA"),
                        "INPUTQUEUE").
                expectReceipt(
                        broker1.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 90L),
                        "INPUTQUEUE").
                expectReceipt(
                        broker3.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "BROKER_CONTROL").
                                addPredicate("ID", "eq", "clientA"),
                        "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,90]");
        clientA.handleCommand("p [class,'BROKER_CONTROL'],[ID,clientA]");
        assertTrue("All broker should receive the control msg, only broker2 should receive the normal pub!",
                _brokerTester.waitUntilExpectedEventsHappen());
        // after unpausing b1 and b3 receive the normal pub
        _brokerTester.clearAll().
                expectReceipt(
                        broker3.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "stock").
                                addPredicate("price", "=", 90L),
                        "INPUTQUEUE");
        assertFalse("Broker1's input queue is stopped. No msg should get to Broker3 and broker1!",
                _brokerTester.waitUntilExpectedEventsHappen());
        broker1.resumeNormalTraffic();
        assertTrue("The publication [class,stock],[price,90] should be sent to Broker3 and broker1",
                _brokerTester.waitUntilExpectedEventsHappen());
    }
}

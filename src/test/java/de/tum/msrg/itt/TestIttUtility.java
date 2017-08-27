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
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by pxsalehi on 25.07.16.
 */
@RunWith(Parameterized.class)
public class TestIttUtility {
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
    protected GenericBrokerTester brokerTester;
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
        brokerTester = new GenericBrokerTester();
        // setup the standard overlay B1-B2-B3
        // start the brokers
        broker1 = createNewBrokerCore(AllTests.brokerConfig01);
        broker2 = createNewBrokerCore(AllTests.brokerConfig02);
        broker3 = createNewBrokerCore(AllTests.brokerConfig03);
        brokerTester.expectCommSystemStart(broker1.getBrokerURI(), null).
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
        brokerTester.waitUntilExpectedStartsHappen();
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
        brokerTester = null;
        cli.close();
        zkTestServer.stop();
        // pause to free addresses
        Sleep.sleep(5000);
    }

    protected IttBrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterIttBrokerCore(brokerTester, brokerConfig);
    }

    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
        return new TesterClient(brokerTester, newConfig);
    }

    @Test
    public void testModifyRoutingTable() throws ParseException, InterruptedException, ClientException {
        messageWatcher.getMessage();
        // current topology is cA--2(i)---1(j)---3(k)--cB
        // wait for adv reaching b3
        brokerTester.clearAll().expectReceipt(
                broker3.getBrokerURI(),
                MessageType.ADVERTISEMENT,
                new TesterMessagePredicates().
                        addPredicate("class", "eq", "stock").
                        addPredicate("price", ">", 80L),
                "INPUTQUEUE");
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        brokerTester.waitUntilExpectedEventsHappen();
        // wait for sub reaching b2
        brokerTester.clearAll().expectReceipt(
                broker2.getBrokerURI(),
                MessageType.SUBSCRIPTION,
                new TesterMessagePredicates().
                        addPredicate("class", "eq", "stock").
                        addPredicate("price", "<", 100L),
                "INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,100]");
        brokerTester.waitUntilExpectedEventsHappen();
        // broker2 will route a matching pub from client a to broker 1
        PublicationMessage pub = clientA.publish(MessageFactory.createPublicationFromString("[class,'stock'],[price,90]"));
        Set<Message> routed = broker2.getRouter().handleMessage(pub);
        // routing the pub on broker2 results in one message to send to broker1
        assertEquals(routed.size(), 1);
        assertEquals(routed.iterator().next().getNextHopID(), broker1.getBrokerDestination());
        // change RTs on i so that any message coming from j, comes from k
        broker2.modifySRT(broker1.getNodeURI(), broker3.getNodeURI());
        broker2.modifyPRT(broker1.getNodeURI(), broker3.getNodeURI());
        pub = clientA.publish(MessageFactory.createPublicationFromString("[class,'stock'],[price,91]"));
        routed = broker2.getRouter().handleMessage(pub);
        // routing the pub on broker2 results in one message to send to broker3
        assertEquals(routed.size(), 1);
        assertEquals(routed.iterator().next().getNextHopID(), broker3.getBrokerDestination());
    }
}

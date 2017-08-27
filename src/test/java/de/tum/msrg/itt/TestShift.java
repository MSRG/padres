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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;
import static org.junit.Assert.*;

/**
 * Created by pxsalehi on 25.07.16.
 */
@RunWith(Parameterized.class)
public class TestShift {
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
        // b2 --- b1 --- b3
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
        clientB = createNewClient(AllTests.clientConfigB);
//        clientA.connect(broker2.getBrokerURI());
//        clientB.connect(broker3.getBrokerURI());
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
        brokerConfig.setZookeeperAddress("localhost:2181");
        brokerConfig.setIttStatsOutputFile(NodeURI.parse(brokerConfig.getBrokerURI()).getID());
        return new TesterIttBrokerCore(brokerTester, brokerConfig);
    }

    protected Client createNewClient(ClientConfig newConfig) throws ClientException {
        return new TesterClient(brokerTester, newConfig);
    }

    class IttAgentThread extends Thread {
        private IttBrokerCore broker;

        public IttAgentThread(IttBrokerCore broker) {
            this.broker = broker;
        }

        @Override
        public void run() {
            try {
                broker.getIttAgent().shift(broker2.getBrokerURI(), broker1.getBrokerURI(), broker3.getBrokerURI());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test(timeout = 60000) // 60 seconds timeout
    public void testShiftLeaderSuccess() throws ParseException, InterruptedException, ClientException {
        clientA.connect(broker2.getBrokerURI());
        clientB.connect(broker3.getBrokerURI());
        messageWatcher.getMessage();
        // current topology is cA--2(i)---1(j)---3(k)--cB
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,100]");
        messageWatcher.getMessage();
        // publishing a matching pub will go through all brokers
        TesterMessagePredicates expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 90L);
        brokerTester.clearAll().
                expectReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,90]");
        assertTrue("pub must go through all three brokers!", brokerTester.waitUntilExpectedEventsHappen());
        long start = System.currentTimeMillis();
        Thread b1IttAgent = new IttAgentThread(broker1);
        Thread b2IttAgent = new IttAgentThread(broker2);
        Thread b3IttAgent = new IttAgentThread(broker3);
        b1IttAgent.start();
        b3IttAgent.start();
        b2IttAgent.start();
        // wait till they are done
        b1IttAgent.join();
        b2IttAgent.join();
        b3IttAgent.join();
        System.out.println("Shift took " + (System.currentTimeMillis() - start));
        // after shift, publishing a pub will go through 2 and 3 only
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 91L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectNegativeReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,91]");
        assertTrue("pub must go through brokers 2 and 3 only!", brokerTester.waitUntilExpectedEventsHappen());
        // publishing a sub also
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", ">", 0L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectNegativeReceipt(broker1.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,0]");
        assertTrue("sub must go through brokers 2 and 3 only!", brokerTester.waitUntilExpectedEventsHappen());
//        Sleep.sleep(10d);
    }

    @Test(timeout = 90000) // 60 seconds timeout
    public void testShiftLeaderSuccessWithZk() throws Exception {
        clientA.connect(broker2.getBrokerURI());
        clientB.connect(broker3.getBrokerURI());
        messageWatcher.getMessage();
        // current topology is cA--2(i)---1(j)---3(k)--cB
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,100]");
        messageWatcher.getMessage();
        // publishing a matching pub will go through all brokers
        TesterMessagePredicates expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 90L);
        brokerTester.clearAll().
                expectReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,90]");
        assertTrue("pub must go through all three brokers!", brokerTester.waitUntilExpectedEventsHappen());
        // deploy shift(b2, b1, b3) on zk
        ZkIttCmd cmd = new ZkIttCmd(ZkIttCmd.Type.SHIFT,
                Arrays.asList(broker2.getBrokerURI(), broker1.getBrokerURI(), broker3.getBrokerURI()));
        byte[] cmdData = cmd.toBytes();
        cli.setData().forPath(broker2.getIttAgent().getBrokerOpsZNode(), cmdData);
        cli.setData().forPath(broker1.getIttAgent().getBrokerOpsZNode(), cmdData);
        cli.setData().forPath(broker3.getIttAgent().getBrokerOpsZNode(), cmdData);
        Sleep.sleep(30000); // wait till they are done
//        Thread b1IttAgent = new IttAgentThread(broker1);
//        Thread b2IttAgent = new IttAgentThread(broker2);
//        Thread b3IttAgent = new IttAgentThread(broker3);
//        b1IttAgent.start();
//        b3IttAgent.start();
//        b2IttAgent.start();
//        // wait till they are done
//        b1IttAgent.join();
//        b2IttAgent.join();
//        b3IttAgent.join();
        // after shift, publishing a pub will go through 2 and 3 only
        assertArrayEquals(
                cli.getData().forPath(broker1.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        assertArrayEquals(
                cli.getData().forPath(broker2.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        assertArrayEquals(
                cli.getData().forPath(broker3.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 91L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectNegativeReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,91]");
        assertTrue("pub must go through brokers 2 and 3 only!", brokerTester.waitUntilExpectedEventsHappen());
        // publishing a sub also
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", ">", 0L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectNegativeReceipt(broker1.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,0]");
        assertTrue("sub must go through brokers 2 and 3 only!", brokerTester.waitUntilExpectedEventsHappen());
//        Sleep.sleep(10d);
    }

    @Test(timeout = 90000) // 60 seconds timeout
    public void testShiftLeaderSuccessKNotHavingSub() throws Exception {
        clientA.connect(broker2.getBrokerURI());
        clientB.connect(broker1.getBrokerURI());
        messageWatcher.getMessage();
        //                                 .----cB
        // current topology is cA--2(i)---1(j)---3(k)
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        clientA.handleCommand("a [class,eq,'stock'],[price,>,80]");
        clientB.handleCommand("s [class,eq,'stock'],[price,<,100]");
        messageWatcher.getMessage();
        // publishing a matching pub will go through brokers 1 and 2
        TesterMessagePredicates expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 90L);
        brokerTester.clearAll().
                expectReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectNegativeReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,90]");
        assertTrue("pub must go through brokers 1 and 2 only!", brokerTester.waitUntilExpectedEventsHappen());
        // deploy shift(b2, b1, b3) on zk
        ZkIttCmd cmd = new ZkIttCmd(ZkIttCmd.Type.SHIFT,
                Arrays.asList(broker2.getBrokerURI(), broker1.getBrokerURI(), broker3.getBrokerURI()));
        byte[] cmdData = cmd.toBytes();
        cli.setData().forPath(broker2.getIttAgent().getBrokerOpsZNode(), cmdData);
        cli.setData().forPath(broker1.getIttAgent().getBrokerOpsZNode(), cmdData);
        cli.setData().forPath(broker3.getIttAgent().getBrokerOpsZNode(), cmdData);
        Sleep.sleep(30000); // wait till they are done
        // after shift, publishing a pub will go through all three brokers
        assertArrayEquals(
                cli.getData().forPath(broker1.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        assertArrayEquals(
                cli.getData().forPath(broker2.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        assertArrayEquals(
                cli.getData().forPath(broker3.getIttAgent().getBrokerOpsStatusZnode()),
                IttUtility.ZK.StringToByte(ZkIttCmd.Status.FINISHED.name()));
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", "=", 91L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker1.getBrokerURI(), MessageType.PUBLICATION, expectedMsgPreds, "INPUTQUEUE");
        clientA.handleCommand("p [class,'stock'],[price,91]");
        assertTrue("pub must go through all brokers!", brokerTester.waitUntilExpectedEventsHappen());
        // publishing a sub also
        expectedMsgPreds = new TesterMessagePredicates().
                addPredicate("class", "eq", "stock").addPredicate("price", ">", 0L);
        brokerTester.clearAll().
                expectReceipt(broker2.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker3.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE").
                expectReceipt(broker1.getBrokerURI(), MessageType.SUBSCRIPTION, expectedMsgPreds, "INPUTQUEUE");
        clientB.handleCommand("s [class,eq,'stock'],[price,>,0]");
        assertTrue("sub must go through all brokers!", brokerTester.waitUntilExpectedEventsHappen());
//        Sleep.sleep(10d);
    }
}

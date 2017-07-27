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
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.common.util.Sleep;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterClient;
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

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;
import static org.junit.Assert.*;

/**
 * Created by pxsalehi on 25.07.16.
 */
@RunWith(Parameterized.class)
public class TestIttBrokerCore {
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
        // create a star topo which is disconnected
        // clientA -- b2 --- b1 -XXX- b3 -- clientB
        AllTests.setupStarNetwork01();
        _brokerTester = new GenericBrokerTester();
    }

    // creates  cA--b2--b1--b3--cB
    // if disconnedte is true, the link between b1 and b3 is not created
    private void initTopo(boolean disconnected) throws Exception {
        // setup the standard overlay B1-B2-B3
        // start the brokers
        broker1 = createNewBrokerCore(AllTests.brokerConfig01);
        broker2 = createNewBrokerCore(AllTests.brokerConfig02);
        if(disconnected)
            AllTests.brokerConfig03.setNeighborURIs(new String[]{});
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
    public void testConnectToNeighbor() throws Exception {
        // create disconnected topo
        initTopo(true);
        // wait for the message to be routed
        messageWatcher.getMessage();
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 1);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 0);
        broker1.connectToNeighbor(broker3.getNodeURI());
        messageWatcher.getMessage();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
    }

    @Test
    public void testConnectToNeighborWhilePaused() throws Exception {
        // create disconnected topo
        initTopo(true);
        // wait for the message to be routed
        messageWatcher.getMessage();
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 1);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 0);
        // pause broker 1 and 3 and connect them
        broker1.pauseNormalTraffic();
        broker3.pauseNormalTraffic();
        broker1.connectToNeighbor(broker3.getNodeURI());
        messageWatcher.getMessage();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        broker1.resumeNormalTraffic();
        broker3.resumeNormalTraffic();
    }

    @Test
    public void testDisconnectFromNeighbor() throws Exception {
        // create connected topo
        initTopo(false);
        // wait for the message to be routed
        messageWatcher.getMessage();
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        broker1.disconnectFromNeighbor(broker3.getNodeURI());
        messageWatcher.getMessage();
        broker3.disconnectFromNeighbor(broker1.getNodeURI());
        messageWatcher.getMessage();
        assertEquals(ort1.getNoOfNeighborBrokers(), 1);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 0);
    }

    @Test
    public void testDisconnectFromNeighborWhilePaused() throws Exception {
        // create connected topo
        initTopo(false);
        // wait for the message to be routed
        messageWatcher.getMessage();
        OverlayRoutingTable ort1 = broker1.getOverlayManager().getORT();
        OverlayRoutingTable ort2 = broker2.getOverlayManager().getORT();
        OverlayRoutingTable ort3 = broker3.getOverlayManager().getORT();
        assertEquals(ort1.getNoOfNeighborBrokers(), 2);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 1);
        // pause broker 1 and 3 and connect them
        broker1.pauseNormalTraffic();
        broker3.pauseNormalTraffic();
        broker1.disconnectFromNeighbor(broker3.getNodeURI());
        messageWatcher.getMessage();
        broker3.disconnectFromNeighbor(broker1.getNodeURI());
        messageWatcher.getMessage();
        assertEquals(ort1.getNoOfNeighborBrokers(), 1);
        assertEquals(ort2.getNoOfNeighborBrokers(), 1);
        assertEquals(ort3.getNoOfNeighborBrokers(), 0);
        broker1.resumeNormalTraffic();
        broker3.resumeNormalTraffic();
    }
}

package ca.utoronto.msrg.padres.test.junit;

import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig.CycleType;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.router.AdvertisementFilter.AdvCoveringType;
import ca.utoronto.msrg.padres.broker.router.SubscriptionFilter.SubCoveringType;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.test.junit.components.TestCmdLine;
import ca.utoronto.msrg.padres.test.junit.components.TestHistoricDataQuery;
import ca.utoronto.msrg.padres.test.junit.components.TestHistoricDataQueryWithCompositeSub;
import ca.utoronto.msrg.padres.test.junit.components.TestHistoricDataQueryWithCyclicRouting;
import ca.utoronto.msrg.padres.test.junit.components.TestPropertyFileException;
import ca.utoronto.msrg.padres.test.junit.components.advcover.IsCoveredADTest;
import ca.utoronto.msrg.padres.test.junit.components.advcover.RelationADTest;
import ca.utoronto.msrg.padres.test.junit.components.advcover.RelationIdentifierADTest;
import ca.utoronto.msrg.padres.test.junit.components.scout.RelationIdentifierTest;
import ca.utoronto.msrg.padres.test.junit.components.scout.RelationTest;
import ca.utoronto.msrg.padres.test.junit.components.scout.ScoutTestAddDupChild;
import ca.utoronto.msrg.padres.test.junit.components.scout.ScoutTestAddDupChild2;
import ca.utoronto.msrg.padres.test.junit.components.scout.ScoutTestMissingExistingChildBug;
import ca.utoronto.msrg.padres.test.junit.components.scout.ScoutTestRemoveDupChildBug;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestCyclicBroker;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestCyclicClientsException;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestCyclicHeartBeat;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestCyclicMultipleBrokers;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestCyclicTwoBrokers;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestDynamicCycles;
import ca.utoronto.msrg.padres.test.junit.cyclic.TestFixedCycles;

/**
 * @author Maniy
 * 
 */
public class AllTests {

    public static final String commProtocol;;

    public static final int testVersion;

    static {
        // get test specific configuration
        testVersion = Integer.parseInt(System.getProperty("test.version", "7"));
        commProtocol = System.getProperty("test.comm_protocol", "rmi");
        System.out.println("test version: " + testVersion + "; comm. protocol: " + commProtocol);
        // load broker configurations
        try {
            setupConfigurations(testVersion);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static BrokerConfig brokerConfig01;

    public static BrokerConfig brokerConfig02;

    public static BrokerConfig brokerConfig03;

    public static BrokerConfig brokerConfig04;

    public static BrokerConfig brokerConfig05;

    public static ClientConfig clientConfigA;

    public static ClientConfig clientConfigB;

    public static ClientConfig clientConfigC;

    public static ClientConfig clientConfigD;

    public static Test suite() throws BrokerCoreException, ClientException {
        // run tests
        TestSuite suite = new TestSuite("Test Version " + commProtocol + " " + testVersion);
        if (testVersion < 5) {
            // test for non-cyclic overlays
            if (testVersion == 1) {
                // these tests do not depend on adv/sub covering ; therefore
                // test them only once
                suite.addTestSuite(TestCmdLine.class);
                suite.addTestSuite(TestPropertyFileException.class);
                suite.addTestSuite(TestBroker.class);
                suite.addTestSuite(TestBrokerException.class);
                suite.addTestSuite(TestClientsException.class);
                suite.addTestSuite(TestClients.class);
                suite.addTestSuite(TestComplexCS.class);
                // testing queries supporting historic publications
                // they use their own broker configuration files; so run them
                // only once
                suite.addTestSuite(TestHistoricDataQuery.class);
                suite.addTestSuite(TestHistoricDataQueryWithCompositeSub.class);
            }
            // these tests include multiple brokers, therefore, can be affected
            // by adv/sub covering
            // options. Test them for all cases
            suite.addTestSuite(TestHeartBeat.class);
            suite.addTestSuite(TestTwoBrokers.class);
            suite.addTestSuite(TestMonitor.class);
            suite.addTestSuite(TestMultipleBrokers.class);
            if (testVersion == 2 || testVersion == 3) {
                // these are the checks for scout, run them only when sub
                // covering is enabled
                suite.addTestSuite(RelationIdentifierTest.class);
                suite.addTestSuite(RelationTest.class);
                suite.addTestSuite(ScoutTestAddDupChild.class);
                suite.addTestSuite(ScoutTestAddDupChild2.class);
                suite.addTestSuite(ScoutTestMissingExistingChildBug.class);
                suite.addTestSuite(ScoutTestRemoveDupChildBug.class);
            }
            if (testVersion == 2) {
                // special tests for active subscription covering
            	// TODO: Remove comment below to enable tests.
//                suite.addTestSuite(TestActiveSubCovering.class);
            }
            if (testVersion == 3) {
                // special tests for lazy subscription covering
            	// TODO: Remove comment below to enable tests.
//                suite.addTestSuite(TestLazySubCovering.class);
            }
            if (testVersion == 4) {
                // special tests for advertisement covering
                suite.addTestSuite(IsCoveredADTest.class);
                suite.addTestSuite(RelationADTest.class);
                suite.addTestSuite(RelationIdentifierADTest.class);
                // TODO: Remove comment below to enable tests.
//                suite.addTestSuite(TestAdvCovering.class);
            }
        } else {
            // tests for cyclic overlays
            suite.addTestSuite(TestHistoricDataQueryWithCyclicRouting.class);
            if (testVersion == 6) {
                // special test for fixed routing in a cyclic overlay
                suite.addTestSuite(TestFixedCycles.class);
            }
            if (testVersion == 7) {
                // special tests for dynamic routing in cyclic overlay
                suite.addTestSuite(TestDynamicCycles.class);
            }
            suite.addTestSuite(TestCyclicBroker.class);
            suite.addTestSuite(TestCyclicClientsException.class);
            suite.addTestSuite(TestCyclicHeartBeat.class);
            suite.addTestSuite(TestCyclicTwoBrokers.class);
            suite.addTestSuite(TestCyclicMultipleBrokers.class);
        }
        return suite;
    }

    static void setupConfigurations(int testVersion) throws BrokerCoreException, ClientException {
        setupBrokerConfigurations(testVersion);
        setupClientConfigurations();
    }

    private static void setupBrokerConfigurations(int testVersion) throws BrokerCoreException {
        BrokerConfig templateConfig = new BrokerConfig();
        templateConfig.setHeartBeat(true);
        templateConfig.setConnectionRetryLimit(1);
        templateConfig.setConnectionRetryPause(1);
        templateConfig.setAdvCovering(AdvCoveringType.OFF);
        switch (testVersion) {
        case 1:
            templateConfig.setCycleType(CycleType.OFF);
            templateConfig.setSubCovering(SubCoveringType.OFF);
            break;
        case 2:
            templateConfig.setCycleType(CycleType.OFF);
            templateConfig.setSubCovering(SubCoveringType.ACTIVE);
            break;
        case 3:
            templateConfig.setCycleType(CycleType.OFF);
            templateConfig.setSubCovering(SubCoveringType.LAZY);
            break;
        case 4:
            templateConfig.setCycleType(CycleType.OFF);
            templateConfig.setSubCovering(SubCoveringType.ACTIVE);
            templateConfig.setAdvCovering(AdvCoveringType.ON);
            break;
        case 5:
            templateConfig.setCycleType(CycleType.FIXED);
            templateConfig.setSubCovering(SubCoveringType.OFF);
            break;
        case 6:
            templateConfig.setCycleType(CycleType.FIXED);
            templateConfig.setSubCovering(SubCoveringType.ACTIVE);
            break;
        case 7:
            templateConfig.setCycleType(CycleType.DYNAMIC);
            templateConfig.setSubCovering(SubCoveringType.OFF);
            break;
        default:
            break;
        }
        brokerConfig01 = new BrokerConfig(templateConfig);
        brokerConfig01.setBrokerURI(commProtocol + "://localhost:3100/Broker1");
        brokerConfig02 = new BrokerConfig(templateConfig);
        brokerConfig02.setBrokerURI(commProtocol + "://localhost:3200/Broker2");
        brokerConfig03 = new BrokerConfig(templateConfig);
        brokerConfig03.setBrokerURI(commProtocol + "://localhost:3300/Broker3");
        brokerConfig04 = new BrokerConfig(templateConfig);
        brokerConfig04.setBrokerURI(commProtocol + "://localhost:3400/Broker4");
        brokerConfig05 = new BrokerConfig(templateConfig);
        brokerConfig05.setBrokerURI(commProtocol + "://localhost:3500/Broker5");
    }

    public static void setupClientConfigurations() throws ClientException {
        ClientConfig templateConfig = new ClientConfig();
        templateConfig.detailState = true;
        clientConfigA = new ClientConfig(templateConfig);
        clientConfigA.clientID = "A";
        clientConfigB = new ClientConfig(templateConfig);
        clientConfigB.clientID = "B";
        clientConfigC = new ClientConfig(templateConfig);
        clientConfigC.clientID = "C";
        clientConfigD = new ClientConfig(templateConfig);
        clientConfigD.clientID = "D";
    }

    public static void resetBrokerConnections() {
        String[] neighbors = {};
        brokerConfig01.setNeighborURIs(neighbors);
        brokerConfig02.setNeighborURIs(neighbors);
        brokerConfig03.setNeighborURIs(neighbors);
        brokerConfig04.setNeighborURIs(neighbors);
        brokerConfig05.setNeighborURIs(neighbors);
    }

    /**
     * star network with broker 1 in the center
     */
    public static void setupStarNetwork01() {
        String[] neighbors = { brokerConfig01.getBrokerURI() };
        brokerConfig02.setNeighborURIs(neighbors);
        brokerConfig03.setNeighborURIs(neighbors);
        brokerConfig04.setNeighborURIs(neighbors);
        brokerConfig05.setNeighborURIs(neighbors);
    }

    /**
     * Cyclic network 01: <code>
     * B1 --- B2
     *  |      |
     *  |      |
     * B3 --- B4
     * </code>
     */
    public static void setupCyclicNetwork01() {
        String[] oneAsNeighbors = { brokerConfig01.getBrokerURI() };
        brokerConfig02.setNeighborURIs(oneAsNeighbors);
        brokerConfig03.setNeighborURIs(oneAsNeighbors);
        String[] twoThreeAsNeighbors = { brokerConfig02.getBrokerURI(),
                brokerConfig03.getBrokerURI() };
        brokerConfig04.setNeighborURIs(twoThreeAsNeighbors);
        // brokerConfig05.setNeighborURIs(oneAsNeighbors);
    }

    /**
     * Cyclic network 02: <code>
     * B1
     *  | \
     *  |  \
     * B3 --B2
     * </code>
     */
    public static void setupCyclicNetwork02() {
        String[] oneAsNeighbors = { brokerConfig01.getBrokerURI() };
        brokerConfig02.setNeighborURIs(oneAsNeighbors);
        String[] oneTwoAsNeighbors = { brokerConfig01.getBrokerURI(), brokerConfig02.getBrokerURI() };
        brokerConfig03.setNeighborURIs(oneTwoAsNeighbors);
        // brokerConfig04.setNeighborURIs(twoThreeAsNeighbors);
        // brokerConfig05.setNeighborURIs(oneAsNeighbors);
    }

    /**
     * Cyclic network 03: <code>
     * B5 --- B1 --- B2
     *         |      |
     *         |      |
     *        B4 --- B3
     * </code>
     */
    public static void setupCyclicNetwork03() {
        String[] oneAsNeighbors = { brokerConfig01.getBrokerURI() };
        brokerConfig02.setNeighborURIs(oneAsNeighbors);
        String[] twoAsNeighbors = { brokerConfig02.getBrokerURI() };
        brokerConfig03.setNeighborURIs(twoAsNeighbors);
        String[] oneThreeAsNeighbors = { brokerConfig01.getBrokerURI(),
                brokerConfig03.getBrokerURI() };
        brokerConfig04.setNeighborURIs(oneThreeAsNeighbors);
        brokerConfig05.setNeighborURIs(oneAsNeighbors);
    }

    public static boolean[] checkRoutedSubscriptions(BrokerCore broker, MessageDestination nextHop,
            Subscription... checkSubs) {
        Set<Message> messages = broker.getInputQueue().getCurrentMessagesToRoute();
        boolean[] checks = new boolean[checkSubs.length];
        for (Message msg : messages) {
            if (msg instanceof SubscriptionMessage) {
                SubscriptionMessage tempSubMsg = (SubscriptionMessage) msg;
                if ((tempSubMsg.getLastHopID().equals(broker.getBrokerDestination()))
                        && (tempSubMsg.getNextHopID().equals(nextHop))) {
                    Subscription expectedSub = tempSubMsg.getSubscription();
                    for (int i = 0; i < checks.length; i++) {
                        if (!checks[i])
                            checks[i] = checkSubs[i].equalsPredicates(expectedSub);
                    }
                }
            }
        }
        return checks;
    }

    public static boolean[] checkRoutedPublications(BrokerCore broker, MessageDestination nextHop,
            Publication... checkPubs) {
        Set<Message> messages = broker.getInputQueue().getCurrentMessagesToRoute();
        boolean[] checks = new boolean[checkPubs.length];
        for (Message msg : messages) {
            if (msg instanceof PublicationMessage) {
                PublicationMessage tempSubMsg = (PublicationMessage) msg;
                if ((tempSubMsg.getLastHopID().equals(broker.getBrokerDestination()))
                        && (tempSubMsg.getNextHopID().equals(nextHop))) {
                    Publication expectedSub = tempSubMsg.getPublication();
                    for (int i = 0; i < checks.length; i++) {
                        if (!checks[i])
                            checks[i] = checkPubs[i].equalVals(expectedSub);
                    }
                }
            }
        }
        return checks;
    }
}

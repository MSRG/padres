package ca.utoronto.msrg.padres.integration;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.integration.tester.MessageItem;
import org.junit.*;

import ca.utoronto.msrg.padres.common.comm.ConnectionHelper;

import org.apache.log4j.Level;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.controller.LifeCycleManager;
import ca.utoronto.msrg.padres.broker.controller.OverlayManager;
import ca.utoronto.msrg.padres.broker.controller.ServerInjectionManager;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.matching.PubMsgNotConformedException;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

@RunWith(Parameterized.class)
public class TestBrokerException extends Assert {
    @Parameterized.Parameter(value = 0)
    public int configuration;

    @Parameterized.Parameter(value = 1)
    public String commProtocol;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { 1, "socket" }, }); //TODO: enable RMI { 1, "rmi" }
    }
    protected GenericBrokerTester _brokerTester;

    protected BrokerCore otherBroker = null;


    protected BrokerCore brokerCore;

    private MessageWatchAppender exceptionAppender;

    private PatternFilter patternFilter;

    @Before
    public void setUp() throws Exception {
        setupConfigurations(configuration, commProtocol);

        _brokerTester = new GenericBrokerTester();

        // start the broker
        brokerCore = createNewBrokerCore(AllTests.brokerConfig01);
        brokerCore.initialize();

        exceptionAppender = new MessageWatchAppender();
        patternFilter = new PatternFilter(OverlayManager.class.getName());
        exceptionAppender.addFilter(patternFilter);
        LogSetup.addAppender("Exception", exceptionAppender);
    }

    @After
    public void tearDown() throws Exception {
        LogSetup.removeAppender("Exception", exceptionAppender);
        brokerCore.shutdown();
        brokerCore = null;

        _brokerTester = null;
        if (otherBroker != null) {
            otherBroker.shutdown();
        }
    }


    protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterBrokerCore(_brokerTester, brokerConfig);
    }

    /**
     * Test for exception that the remoteBroker of OVERLAY-CONNECT command is malformed.
     *
     * @throws ParseException
     */
    @Test
    public void testMalformedBrokerConnectPubForOverlay() throws ParseException {
        // A malformed remoteBrokerURI is given
        Publication olConnectPub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'123']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());
        // setup message filter
        patternFilter.setLevel(Level.ERROR);
        patternFilter.setPattern(".*" + CommunicationException.class.getName() + ".+\\s*.+");
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue(
                "A CommunicationException should be thrown because the URI of remoteBroker is invaild.",
                checkReceived);
    }

    /**
     * Test for exception that the remoteBroker of OVERLAY-CONNECT command is null.
     *
     * @throws ParseException
     */
    @Test
    public void testNullRemoteBrokerConnectPubForOverlay() throws ParseException {
        // A null remoteBrokerURI is given
        Publication olConnectPub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());
        // setup message filter
        patternFilter.setLevel(Level.ERROR);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+\\s*.+");
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue("An Exception should be thrown because remoteBroker is null.", checkReceived);
    }

    /**
     * Test for exception that the remoteBroker of OVERLAY-CONNECT command is broker itself. It is
     * important to note that this will cause a loop, and the routing will be wrong totally.
     *
     * @throws ParseException
     */
    @Test
    public void testConnectItselfPubForOverlay() throws ParseException {
        // A fake remoteBrokerURI is given
        Publication olConnectPub = MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'"
                + brokerCore.getBrokerURI() + "']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());
        // setup message filter
        patternFilter.setLevel(Level.ERROR);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+\\s*.+");
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue("An Exception should be thrown because remoteBroker is broker itself.",
                checkReceived);
    }

    /**
     * Test for exception that the unrecognized command for overlay.
     *
     * @throws ParseException
     */
    @Test
    public void testUnrecognizedCommandPubForOverlay() throws ParseException {
        // An unrecognized command is given
        Publication olUnrecognizedCommandPub = MessageFactory.createPublicationFromString(
                "[class,'BROKER_CONTROL'],[brokerID,'" + brokerCore.getBrokerID()
                        + "'],[command,'OVERLAY-UNRECOGNIZEDCOMMAND']");
        PublicationMessage olUnrecognizedCommandPubmsg = new PublicationMessage(
                olUnrecognizedCommandPub, brokerCore.getNewMessageID());
        // setup message filter
        patternFilter.setLevel(Level.WARN);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+\\s*.+");
        // route message
        brokerCore.routeMessage(olUnrecognizedCommandPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue(
                "An UnrecognizedCommandException should be thrown since the command is not predefined.",
                checkReceived);
    }

    /**
     * Test for exception that the unrecognized command for lifeCycle.
     *
     * @throws ParseException
     */
    @Test
    public void testUnrecognizedCommandPubForLifeCycle() throws ParseException {
        // An unrecognized command is given
        Publication lcUnrecognizedCommandPub = MessageFactory.createPublicationFromString(
                "[class,'BROKER_CONTROL'],[brokerID,'" + brokerCore.getBrokerID()
                        + "'],[command,'LIFECYCLE-UNRECOGNIZEDCOMMAND']");
        PublicationMessage lcUnrecognizedCommandPubmsg = new PublicationMessage(
                lcUnrecognizedCommandPub, brokerCore.getNewMessageID());
        // setup message filter
        patternFilter = new PatternFilter(LifeCycleManager.class.getName());
        patternFilter.setLevel(Level.WARN);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+\\s*.+");
        exceptionAppender.clearFilters();
        exceptionAppender.addFilter(patternFilter);
        // route message
        brokerCore.routeMessage(lcUnrecognizedCommandPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue(
                "An UnrecognizedCommandException should be thrown since the command is not predefined.",
                checkReceived);
    }

    /**
     * Test for exception that the unrecognized command for serverInjection.
     *
     * @throws ParseException
     */

    @Test
    public void testUnrecognizedCommandPubForServerInjection() throws ParseException {
        // An unrecognized command is given
        Publication siUnrecognizedCommandPub = MessageFactory.createPublicationFromString(
                "[class,'BROKER_CONTROL'],[brokerID,'" + brokerCore.getBrokerID()
                        + "'],[command,'INJECTION-UNRECOGNIZEDCOMMAND']");
        PublicationMessage siUnrecognizedCommandPubmsg = new PublicationMessage(
                siUnrecognizedCommandPub, brokerCore.getNewMessageID());
        // setup the filter
        patternFilter = new PatternFilter(ServerInjectionManager.class.getName());
        patternFilter.setLevel(Level.WARN);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+");
        exceptionAppender.clearFilters();
        exceptionAppender.addFilter(patternFilter);
        // route the message
        brokerCore.routeMessage(siUnrecognizedCommandPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue(
                "An UnrecognizedCommandException should be thrown since the command is not predefined.",
                checkReceived);
    }

    /**
     * Test for exception that the unrecognized command sent for systemMonitor.
     *
     * @throws ParseException
     */

    @Test
    public void testUnrecognizedCommandPubForSystemMonitor() throws ParseException {
        MessageDestination md = new MessageDestination("Broker-D0-0");
        Advertisement smAdv = MessageFactory.createAdvertisementFromString(
                "[class,eq,BROKER_MONITOR],[command,isPresent,'TEXT'],"
                        + "[brokerID,isPresent,'TEXT'],"
                        + "[PUBLICATION_INTERVAL,isPresent,12345],"
                        + "[TRACEROUTE_ID,isPresent,'12345']");
        AdvertisementMessage smAdvmsg = new AdvertisementMessage(smAdv,
                brokerCore.getNewMessageID(), md);
        brokerCore.routeMessage(smAdvmsg, MessageDestination.INPUTQUEUE);
        // An unrecognized command is given
        Publication smUnrecognizedCommandPub = MessageFactory.createPublicationFromString(
                "[class,'BROKER_MONITOR'],[brokerID,'" + brokerCore.getBrokerID()
                        + "'],[command,'UNRECOGNIZEDCOMMAND']");
        PublicationMessage smUnrecognizedCommandPubmsg = new PublicationMessage(
                smUnrecognizedCommandPub, brokerCore.getNewMessageID(), md);
        // add new filter
        patternFilter = new PatternFilter(SystemMonitor.class.getName());
        patternFilter.setLevel(Level.WARN);
        patternFilter.setPattern(".*" + Exception.class.getName() + ".+");
        exceptionAppender.clearFilters();
        exceptionAppender.addFilter(patternFilter);
        // route message
        brokerCore.routeMessage(smUnrecognizedCommandPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        assertTrue(
                "An UnrecognizedCommandException should be thrown since the command is not predefined.",
                checkReceived);
    }

    /**
     * Test for exception that the unrecognized classArribute publication sent.
     *
     * @throws ParseException
     */
    @Test
    public void testUnrecognizedClassPub() throws ParseException {
        // An unrecognized command is given
        MessageDestination mdA = new MessageDestination(brokerCore.getBrokerID() + "-D0-0");
        Publication unrecognizedClassPub = MessageFactory.createPublicationFromString(
                "[class,'UNRECOGNIZEDCLASS'],[brokerID,'" + brokerCore.getBrokerID()
                        + "'],[command,'LIFECYCLE-CONNECT']");
        PublicationMessage unrecognizedClassPubmsg = new PublicationMessage(unrecognizedClassPub,
                brokerCore.getNewMessageID(), mdA);
        // add new filter
        patternFilter = new PatternFilter(Router.class.getName());
        patternFilter.setLevel(Level.ERROR);
        patternFilter.setPattern(".*" + PubMsgNotConformedException.class.getName() + ".+");
        exceptionAppender.clearFilters();
        exceptionAppender.addFilter(patternFilter);
        // route message
        brokerCore.routeMessage(unrecognizedClassPubmsg, MessageDestination.INPUTQUEUE);
        // waiting for routing finished
        boolean checkReceived = exceptionAppender.getMessage() != null;
        if (brokerCore.getBrokerConfig().isPubConformCheck()) {
            assertTrue(
                    "A matching exception should be thrown since you had not advertise before you published.",
                    checkReceived);
        }
    }

    /* TODO: REZA (DONE) */
    @Test
    public void testBrokerConnectUnroutableIP() throws ParseException, InterruptedException {
        String ipStr = "142.150.237.234";
        String brokerURI =
                ConnectionHelper.makeURI(commProtocol, ipStr, 1100,
                        "BrokerNull");
        Publication olConnectPub =
                MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                        + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'"
                        + brokerURI + "']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());

        _brokerTester.expectConnectionFail(brokerCore.getBrokerURI(), brokerURI);
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);
        //_brokerTester.waitUntilExpectedEventsHappen(3000);
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /* TODO: REZA (DONE) */
    @Test
    public void testBrokerConnectInvalidIP() throws ParseException, InterruptedException {
        // Invalid IP address includes a value of 400 for the last byte.
        String ipStr = "142.150.237.400";
        String brokerURI =
                ConnectionHelper.makeURI(commProtocol, ipStr, 1100,
                        "BrokerNull");
        Publication olConnectPub =
                MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                        + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'"
                        + brokerURI + "']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());

        _brokerTester.expectConnectionFail(brokerCore.getBrokerURI(), brokerURI);
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);

        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /* TODO: REZA */
    @Test
    public void testUnexistingBrokerConnect() throws ParseException, InterruptedException {
        String ipStr = "localhost";
        // No broker listening at this port.
        int port = 4000;
        String brokerURI =
                ConnectionHelper.makeURI(commProtocol, ipStr, port,
                        "BrokerNull");
        Publication olConnectPub =
                MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                        + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'"
                        + brokerURI + "']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());

        _brokerTester.expectConnectionFail(brokerCore.getBrokerURI(), brokerURI);
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);

        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /* TODO: REZA (DOES NOT WORK) */
    @Test
    public void testBrokerConnectToAnotherSpecifyingWrongID() throws BrokerCoreException, InterruptedException, ParseException {
        if (1 == 1) return;
        GenericBrokerTester.lookAtPubs = true;
        GenericBrokerTester.PRODUCE_PRINT_TRACES = true;
        GenericBrokerTester.classSpecificMessages = "BROKER_CONTROL";

        otherBroker = createNewBrokerCore(AllTests.brokerConfig02);
        otherBroker.initialize();

        String correctRemoteBrokerURI = AllTests.brokerConfig02.getBrokerURI();
        String incorrectRemoteBrokerURI = correctRemoteBrokerURI + "RandomStr";
        Publication olConnectPub =
                MessageFactory.createPublicationFromString("[class,'BROKER_CONTROL'],[brokerID,'"
                        + brokerCore.getBrokerID() + "'],[command,'OVERLAY-CONNECT'],[broker,'"
                        + incorrectRemoteBrokerURI + "']");
        PublicationMessage olConnectPubmsg = new PublicationMessage(olConnectPub,
                brokerCore.getNewMessageID());

        _brokerTester.expectConnectionFail(brokerCore.getBrokerURI(), incorrectRemoteBrokerURI);
        // route message
        brokerCore.routeMessage(olConnectPubmsg, MessageDestination.INPUTQUEUE);

        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }
}

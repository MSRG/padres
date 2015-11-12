package ca.utoronto.msrg.padres.integration.cyclic;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.integration.TestHeartBeat;
import ca.utoronto.msrg.padres.integration.tester.TesterMessagePredicates;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * This class provides a way to test HeartBeat functionality.
 *
 * @author Bala Maniymaran
 */
public class TestCyclicHeartBeat extends Assert {

    protected GenericBrokerTester _brokerTester;

    protected BrokerCore brokerCore1;

    protected BrokerCore brokerCore2;

    protected MonitorFrame padresMonitor;

    protected MessageWatchAppender messageWatcher;

    protected PatternFilter msgFilter;

    @Before
    public void setUp() throws Exception {
        setupConfigurations(5, "socket");

        _brokerTester = new GenericBrokerTester();

        // start the broker
        AllTests.setupStarNetwork01();
        brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
        brokerCore1.initialize();
        brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
        brokerCore2.initialize();
        // start monitor for broker1
        padresMonitor = new MonitorFrame();
        padresMonitor.getOverlayManager().connect(brokerCore1.getBrokerURI());
        padresMonitor.getOverlayManager().setHeartbeatParameters(brokerCore1.getBrokerID(), true,
                3000, 5000, 2);
        // setup filters and message watcher
        messageWatcher = new MessageWatchAppender();
        msgFilter = new PatternFilter(InputQueueHandler.class.getName());
        msgFilter.setPattern("");
        messageWatcher.addFilter(msgFilter);
        LogSetup.addAppender("MessagePath", messageWatcher);
    }

    protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterBrokerCore(_brokerTester, brokerConfig);
    }

    @After
    public void tearDown() throws Exception {

        padresMonitor.shutdown();
        brokerCore1.shutdown();
        brokerCore2.shutdown();
        LogSetup.removeAppender("MessagePath", messageWatcher);
        padresMonitor = null;

        // TODO: remove this line
        // padresMonitor.exitMonitor();
        brokerCore1 = null;
        brokerCore2 = null;
        _brokerTester = null;
    }

    /**
     * Test the HeartBeat ACK publication is sent back to the source broker, where broker1.HeartBeat
     * is ON, and broker2.HeartBeat is OFF. broker1 sends heartBeat REQ to broker2, and broker2
     * sends heartBeat ACK to broker1.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testHeartBeatACK() throws ParseException, InterruptedException {
        /* TODO: VINOD (DONE) */
        // TODO: Can we also test for the tid and handle attributes?
        _brokerTester.clearAll().
                expectSend(
                        brokerCore2.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "HEARTBEAT_MANAGER").
                                addPredicate("type", "eq", "HEARTBEAT_ACK").
                                addPredicate("brokerID", "eq", brokerCore1.getBrokerID()).
                                addPredicate("fromID", "eq", brokerCore2.getBrokerID()),
                        brokerCore1.getBrokerURI());
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }

    /**
     * Test the HeartBeat failureDetect, where broker1.HeartBeat is ON, and broker2.HeartBeat is
     * OFF. broker2 is stoped, then broker1 sends failureDetect publication to monitor. The
     * failureDetect can be shown on the monitor. Then broker2 is resumed, and broker1 sends
     * failureCleared publication to monitor.
     *
     * @throws ParseException
     * @throws InterruptedException
     */
    @Test
    public void testHeartBeatFailureDetectAndFailureCleared() throws ParseException, InterruptedException {
		/* TODO: VINOD (DONE) */
        // Stop broker 2 and wait for the failure to be detected.
        // TODO: Also test for tid attribute?
        _brokerTester.clearAll().
                expectSend(
                        brokerCore1.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "HEARTBEAT_MANAGER").
                                addPredicate("type", "eq", "FAILURE_DETECTED").
                                addPredicate("detectedID", "eq", brokerCore2.getBrokerID()).
                                addPredicate("detectorID", "eq", brokerCore1.getBrokerID()),
                        brokerCore1.getBrokerURI() + "-" + padresMonitor.getMonitorID());
        padresMonitor.getOverlayManager().stopBroker(brokerCore2.getBrokerID());
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen(16000));

        // Resume broker 2 and wait for the failure to be cleared.
        // TODO: Also test for tid attribute?
        _brokerTester.clearAll().
                expectSend(
                        brokerCore1.getBrokerURI(),
                        MessageType.PUBLICATION,
                        new TesterMessagePredicates().
                                addPredicate("class", "eq", "HEARTBEAT_MANAGER").
                                addPredicate("type", "eq", "FAILURE_CLEARED").
                                addPredicate("detectedID", "eq", brokerCore2.getBrokerID()).
                                addPredicate("detectorID", "eq", brokerCore1.getBrokerID()),
                        brokerCore1.getBrokerURI() + "-" + padresMonitor.getMonitorID());
        padresMonitor.getOverlayManager().resumeBroker(brokerCore2.getBrokerID());
        assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
    }
}

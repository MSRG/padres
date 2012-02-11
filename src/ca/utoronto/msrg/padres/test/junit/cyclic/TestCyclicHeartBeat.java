package ca.utoronto.msrg.padres.test.junit.cyclic;

import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.test.junit.TestHeartBeat;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test HeartBeat functionality.
 * 
 * @author Bala Maniymaran
 */
public class TestCyclicHeartBeat extends TestHeartBeat {

	/**
	 * Test the HeartBeat ACK publication is sent back to the source broker, where broker1.HeartBeat
	 * is ON, and broker2.HeartBeat is OFF. broker1 sends heartBeat REQ to broker2, and broker2
	 * sends heartBeat ACK to broker1.
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
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
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
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
				brokerCore1.getBrokerURI() + "-" +  padresMonitor.getMonitorID());
		padresMonitor.getOverlayManager().stopBroker(brokerCore2.getBrokerID());
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(40000));

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
				brokerCore1.getBrokerURI() + "-" +  padresMonitor.getMonitorID());
		padresMonitor.getOverlayManager().resumeBroker(brokerCore2.getBrokerID());
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(40000));
	}
}

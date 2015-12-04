package ca.utoronto.msrg.padres.broker.brokercore;

import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.controller.LinkInfo;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.timer.TimerClient;
import ca.utoronto.msrg.padres.common.util.timer.TimerEvent;
import ca.utoronto.msrg.padres.common.util.timer.TimerThread;

public class HeartbeatPublisher extends Thread implements TimerClient {

	public static final long DEFAULT_HEARTBEAT_INTERVAL = 3000; // TODO: remove hardcoded hack!

	public static final long DEFAULT_HEARTBEAT_TIMEOUT = 5000; // TODO: remove hardcode hack!

	public static final int DEFAULT_FAILURE_THRESHOLD = 3; // TODO: remove hardcode hack!

	protected boolean m_PublishHeartbeats = true;

	protected long m_HeartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

	protected long m_HeartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;

	protected int m_FailureThreshold = DEFAULT_FAILURE_THRESHOLD;

	protected static final int HEARTBEAT_INTERVAL_TIMER = 1000; // type of timer event

	protected static final int HEARTBEAT_TIMEOUT_TIMER = 1001; // type of timer event

	protected static final int DEFAULT_SLEEP_TIME = 60000;

	// private static HeartbeatPublisher m_Instance = null;
	protected TimerEvent m_ReturnedEventsQ = null;

	protected BrokerCore m_BrokerCore;

	protected TimerThread m_TimerThread;

	protected boolean m_NeighborListChanged = false;

	// add for testing
	protected int currentHandle = 0;

	// add for waitUntilStated
	protected boolean started = false;

	protected Object started_lock = new Object();

	static Logger heartbeatLogger = Logger.getLogger("HeartBeat");
	static Logger exceptionLogger = Logger.getLogger("Exception");

	public HeartbeatPublisher(BrokerCore broker) {
		super("PADRES Hartbeat Publisher");
		m_BrokerCore = broker;
	}

	public void run() {
		synchronized (started_lock) {
			m_TimerThread = m_BrokerCore.getTimerThread();

			// advertise that we will be publishing heartbeat related messages
			advertiseHeartbeat();
			advertiseFailureDetected();

			// set the initial heartbeat timer
			m_TimerThread.setTimer(new TimerEvent(this, m_HeartbeatInterval,
					HEARTBEAT_INTERVAL_TIMER));

			// wake up threads waiting for HeartbeatPublisher to start
			started = true;
			started_lock.notifyAll();
		}

		while (started) {
			try {
				sleep(DEFAULT_SLEEP_TIME);
			} catch (InterruptedException ignored) {
			}

			if(!started)
				break;
			
			synchronized (this) {
				// check if neighbor list changed
				if (m_NeighborListChanged) {
					// TODO: process changed neighbor list
					m_NeighborListChanged = false;
				}

				// check for expired events
				while (m_ReturnedEventsQ != null) {
					TimerEvent event = m_ReturnedEventsQ;

					// process the event
					if (event.getStatus() == TimerEvent.TIMER_STATUS_EXPIRED) {
						if (event.getType() == HEARTBEAT_INTERVAL_TIMER) {
							// send the next batch of heartbeats
							if (m_PublishHeartbeats)
								publishHeartbeats();
							// reset the interval timer
							m_TimerThread.setTimer(new TimerEvent(this, m_HeartbeatInterval,
									HEARTBEAT_INTERVAL_TIMER));
						} else if (event.getType() == HEARTBEAT_TIMEOUT_TIMER) {
							heartbeatLogger.info("Broker " + event.getAttachment()
									+ " did not reply to a heartbeat");

							// get the name of the offending broker
							String broker = (String) event.getAttachment();

							// a broker heartbeat timed out, increment the fail count
							Map<MessageDestination, OutputQueue> neighbors1 = m_BrokerCore.getOverlayManager().getORT().getBrokerQueues();
							synchronized (neighbors1) {
								for (MessageDestination md : neighbors1.keySet()) {
									if (md.getDestinationID().equals(broker)) {
										int failCount = md.incrementFailCount();
										if (failCount >= m_FailureThreshold) {
											publishFailureDetected(broker);
											break;
										}
									}
								}
							}
						}
					}
					m_ReturnedEventsQ = event.getNext();
				}
			}
		}
	}

	/**
	 * called by the timer thread when one of the timer events we set has expired.
	 */
	public synchronized void returnToClientQ(TimerEvent event) {
		if (m_ReturnedEventsQ == null) {
			m_ReturnedEventsQ = event;
			event.setNext(null);
		} else {
			TimerEvent current = m_ReturnedEventsQ;
			while (current.getNext() != null)
				current = current.getNext();
			current.setNext(event);
			event.setNext(null);
		}

		interrupt();
	}

	private void advertiseHeartbeat() {
		String advStr = "[class,eq," + HeartbeatSubscriber.MESSAGE_CLASS + "],"
				+ "[brokerID,isPresent,'TEXT']," + "[fromID,eq,'" + m_BrokerCore.getBrokerID()
				+ "']," + "[type,isPresent,'TEXT']," + "[handle,isPresent,'TEXT']";
		Advertisement adv;
		try {
			adv = MessageFactory.createAdvertisementFromString(advStr);
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		AdvertisementMessage msg = new AdvertisementMessage(adv, m_BrokerCore.getNewMessageID(),
				MessageDestination.HEARTBEAT_MANAGER);
		msg.setTTL(1);
		heartbeatLogger.debug("Sending initial advertisement for heartbeat.");
		m_BrokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
	}

	private void advertiseFailureDetected() {
		String advStr = "[class,eq," + HeartbeatSubscriber.MESSAGE_CLASS + "],"
				+ "[detectorID,eq,'" + m_BrokerCore.getBrokerID() + "'],"
				+ "[detectedID,isPresent,'TEXT']," + "[type,isPresent,'TEXT']";
		Advertisement adv;
		try {
			adv = MessageFactory.createAdvertisementFromString(advStr);
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		AdvertisementMessage msg = new AdvertisementMessage(adv, m_BrokerCore.getNewMessageID(),
				MessageDestination.HEARTBEAT_MANAGER);
		heartbeatLogger.debug("Sending initial advertisement for failure detected.");
		m_BrokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
	}

	private void publishHeartbeats() {
		try {
			Map<MessageDestination, OutputQueue> neighbors = m_BrokerCore.getOverlayManager().getORT().getBrokerQueues();
			synchronized (neighbors) {
				for (MessageDestination md : neighbors.keySet()) {
					String brokerID = md.getDestinationID();
	
					// create and set a timer event for the heartbeat request
					TimerEvent event = new TimerEvent(this, m_HeartbeatTimeout, HEARTBEAT_TIMEOUT_TIMER);
					event.setAttachment(brokerID);
					int handle = m_TimerThread.setTimer(event);
	
					// publish the heartbeat request
					String pubStr = "[class," + HeartbeatSubscriber.MESSAGE_CLASS + "]," + "[brokerID,'"
							+ brokerID + "']," + "[fromID,'" + m_BrokerCore.getBrokerID() + "'],"
							+ "[type,'HEARTBEAT_REQ']," + "[handle,'" + handle + "']";
					Publication pub = MessageFactory.createPublicationFromString(pubStr);
					PublicationMessage pubMsg = new PublicationMessage(pub,
							m_BrokerCore.getNewMessageID(), MessageDestination.HEARTBEAT_MANAGER);
					heartbeatLogger.info("Broker " + m_BrokerCore.getBrokerID()
							+ " is sending heartbeat REQ to broker " + brokerID + " with handle "
							+ handle);
					m_BrokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
					// add by shuang for testing
					currentHandle = handle;
	
				}
			}
		} catch (ParseException e) {
			heartbeatLogger.error(e.getMessage());
			exceptionLogger.error(e.getMessage());
		}
	}

	private void publishFailureDetected(String broker) {
		try {
			String pubStr = "[class," + HeartbeatSubscriber.MESSAGE_CLASS + "]," + "[detectorID,'"
					+ m_BrokerCore.getBrokerID() + "']," + "[detectedID,'" + broker + "'],"
					+ "[type,'FAILURE_DETECTED']";
			Publication pub = MessageFactory.createPublicationFromString(pubStr);
			PublicationMessage pubMsg = new PublicationMessage(pub, m_BrokerCore.getNewMessageID(),
					MessageDestination.HEARTBEAT_MANAGER);
	
			Map<MessageDestination, LinkInfo> statisticTable = m_BrokerCore.getOverlayManager().getORT().getStatisticTable();
			MessageDestination failureBroker = new MessageDestination(broker, DestinationType.BROKER);
			if (statisticTable.containsKey(failureBroker)) {
				LinkInfo link = statisticTable.get(failureBroker);
				link.setStatus();
			}
	
			heartbeatLogger.info("Sending failure of " + broker + " detected messsage.");
			m_BrokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		} catch (ParseException e) {
			heartbeatLogger.error(e.getMessage());
			exceptionLogger.error(e.getMessage());
		}
	}

	/**
	 * Block until the HeartbeatPublisher is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}

	public long getHeartbeatInterval() {
		return m_HeartbeatInterval;
	}

	public void setHeartbeatInterval(long interval) {
		m_HeartbeatInterval = interval;
	}

	public long getHeartbeatTimeout() {
		return m_HeartbeatTimeout;
	}

	public void setHeartbeatTimeout(long timeout) {
		m_HeartbeatTimeout = timeout;
	}

	public int getFailureThreshold() {
		return m_FailureThreshold;
	}

	public void setFailureThreshold(int threshold) {
		m_FailureThreshold = threshold;
	}

	// add for testing
	public int getCurrentHandle() {
		return currentHandle;
	}

	// HACK: for demo
	public void setHeartbeatParams(boolean enabled, long interval, long timeout, int threshold) {
		m_PublishHeartbeats = enabled;
		m_HeartbeatInterval = (interval > 0) ? interval : DEFAULT_HEARTBEAT_INTERVAL;
		m_HeartbeatTimeout = (timeout > 0) ? timeout : DEFAULT_HEARTBEAT_TIMEOUT;
		m_FailureThreshold = (threshold > 0) ? threshold : DEFAULT_FAILURE_THRESHOLD;
	}

	// HACK: for demo
	public void setPublishHeartbeats(boolean publish) {
		m_PublishHeartbeats = publish;
	}
	
	public void shutdown() {
		started = false;
		this.interrupt();
	}
}

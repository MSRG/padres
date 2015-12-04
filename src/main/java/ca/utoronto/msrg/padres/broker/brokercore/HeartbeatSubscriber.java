package ca.utoronto.msrg.padres.broker.brokercore;

import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.controller.LinkInfo;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.ShutdownMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.timer.TimerEvent;
import ca.utoronto.msrg.padres.common.util.timer.TimerThread;

public class HeartbeatSubscriber extends Thread {
	public static final String MESSAGE_CLASS = "HEARTBEAT_MANAGER";

	protected BrokerCore m_BrokerCore;

	protected MessageQueue m_MessageQ;

	protected TimerThread m_TimerThread;

	// add for waitUntilStarted
	protected boolean started = false;

	protected Object started_lock = new Object();

	static Logger heartbeatLogger = Logger.getLogger("HeartBeat");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public HeartbeatSubscriber(BrokerCore broker) {
		super("PADRES Hartbeat Subscriber");
		m_BrokerCore = broker;
	}

	public void run() {
		synchronized (started_lock) {
			m_MessageQ = createMessageQueue();
			m_BrokerCore.registerQueue(MessageDestination.HEARTBEAT_MANAGER, m_MessageQ);
			m_TimerThread = m_BrokerCore.getTimerThread();

			// subscribe to heartbeat messages
			subscribeHeartbeat();

			// wake up threads waiting for HeartbeatSubscriber to start
			started = true;
			started_lock.notifyAll();
		}
		// listen for incoming heartbeat messages
		while (started) {
			try {
				Message msg = m_MessageQ.blockingRemove();
				
				if(msg.getType().equals(MessageType.SHUTDOWN)){
					continue;
				}
				
				heartbeatLogger.debug("HeartbeatSubscriber receives message : " + msg.toString());
				if (msg.getClass() == PublicationMessage.class) {
					Publication pub = ((PublicationMessage) msg).getPublication();
					Map<String, Serializable> pairs = pub.getPairMap();
					if (((String) pairs.get("class")).equalsIgnoreCase(MESSAGE_CLASS)) {
						String fromID = (String) pairs.get("fromID");
						String type = (String) pairs.get("type");
						String handle = (String) pairs.get("handle");
	
						if (type.equalsIgnoreCase("HEARTBEAT_REQ")) {
							heartbeatLogger.info("Broker " + m_BrokerCore.getBrokerID()
									+ " got a heartbeat REQ from broker " + fromID + " with handle "
									+ handle + ".");
							// send a heartbeat ack
							Publication ack = MessageFactory.createPublicationFromString("[class," + MESSAGE_CLASS + "],"
									+ "[brokerID,'" + fromID + "']," + "[fromID,'"
									+ m_BrokerCore.getBrokerID() + "']," + "[type,'HEARTBEAT_ACK'],"
									+ "[handle,'" + handle + "']");
							PublicationMessage ackMsg = new PublicationMessage(ack,
									m_BrokerCore.getNewMessageID(),
									MessageDestination.HEARTBEAT_MANAGER);
							// m_BrokerCore.getBrokerDestination());
							heartbeatLogger.info("Broker " + m_BrokerCore.getBrokerID()
									+ " is sending the ACK.");
							m_BrokerCore.routeMessage(ackMsg, MessageDestination.INPUTQUEUE);
						} else if (type.equalsIgnoreCase("HEARTBEAT_ACK")) {
							heartbeatLogger.info("Broker " + m_BrokerCore.getBrokerID()
									+ " got a heartbeat ACK from broker " + fromID + " with handle "
									+ handle);
							// got a heartbeat, cancel appropriate heartbeat timer
							int timerHandle = Integer.parseInt(handle);
							TimerEvent event = m_TimerThread.cancelTimer(timerHandle);
	
							// clear failure if necessary
							if (event != null) {
								String broker = (String) event.getAttachment();
								Map<MessageDestination, OutputQueue> neighbors2 = m_BrokerCore.getOverlayManager().getORT().getBrokerQueues();
								synchronized (neighbors2) {
									for (MessageDestination md : neighbors2.keySet()) {
										if (md.getDestinationID().equals(broker)) {
											int oldFailCount = md.getFailCount();
											md.setFailCount(0);
											if (oldFailCount >= m_BrokerCore.getHeartbeatPublisher().getFailureThreshold()) {
												publishFailureCleared(broker);
											}
											break;
										}
									}
								}
							} else {
								heartbeatLogger.error("The TimerEvent is null.");
								exceptionLogger.error("Here is an exception : "
										+ new Exception("The TimerEvent is null."));
							}
						}
					}
				} else {
					heartbeatLogger.warn("The coming message for HeartbeatSubscriber is not a publication message.");
					exceptionLogger.warn("Here is an exception : "
							+ new Exception(
									"The coming message for HeartbeatSubscriber is not a publication message."));
				}
			} catch (ParseException e) {
				exceptionLogger.error("ParseException" + e.getMessage());
				heartbeatLogger.error("ParseException" + e.getMessage());
			}
		}
	}

	protected MessageQueue createMessageQueue() {
		MessageQueue mQueue = new MessageQueue();
		return mQueue;
	}

	private void subscribeHeartbeat() {
		String subStr = "[class,eq," + MESSAGE_CLASS + "]," + "[brokerID,eq,'"
				+ m_BrokerCore.getBrokerID() + "']";

		Subscription sub;
		try {
			sub = MessageFactory.createSubscriptionFromString(subStr);
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		SubscriptionMessage msg = new SubscriptionMessage(sub, m_BrokerCore.getNewMessageID(),
				MessageDestination.HEARTBEAT_MANAGER);
		heartbeatLogger.debug("Sending initial subscription for heartbeat.");
		m_BrokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
	}

	private void publishFailureCleared(String broker) {
		try {
			String pubStr = "[class," + MESSAGE_CLASS + "]," + "[detectorID,'"
					+ m_BrokerCore.getBrokerID() + "']," + "[detectedID,'" + broker + "'],"
					+ "[type,'FAILURE_CLEARED']";
			Publication pub = MessageFactory.createPublicationFromString(pubStr);
			PublicationMessage pubMsg = new PublicationMessage(pub, m_BrokerCore.getNewMessageID(),
					MessageDestination.HEARTBEAT_MANAGER);
	
			Map<MessageDestination, LinkInfo> statisticTable = m_BrokerCore.getOverlayManager().getORT().getStatisticTable();
			MessageDestination clearedBroker = new MessageDestination(broker, DestinationType.BROKER);
			if (statisticTable.containsKey(clearedBroker)) {
				LinkInfo link = statisticTable.get(clearedBroker);
				link.resetStatus();
			}
	
			heartbeatLogger.info("Sending failure of " + broker + " cleared message.");
			m_BrokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		} catch (ParseException e) {
			exceptionLogger.error("ParseException:" + e.getMessage());
			heartbeatLogger.error("ParseException:" + e.getMessage());
		}
	}

	/**
	 * Block until the HeartbeatSubscriber is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}
	
	public void shutdown(){
		started = false;

		m_MessageQ.addFirst(new ShutdownMessage());
	}
}

// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================

/*
 * Created on Oct 25, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.monitor;

// Java
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.ShutdownMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Alex Cheung
 * 
 *         This class was added to support all of the functional requirements for the
 *         "Federated Publish/Subscribe Monitoring Tool". If you want to add more functionaility to
 *         the tool, this is right place. All of the accounting and statistical information of a
 *         broker is calculated and maintained in this class. Hence, no other classes should be
 *         doing this job nor contain it's logic
 * 
 *         Message classes used by the System Monitor: BROKER_MONITOR TRACEROUTE_MESSAGE BROKER_INFO
 *         NETWORK_DISCOVERY
 */

/*
 * Dirty workarounds:
 * 
 * 1) advertise traceroute publication messages on demand (ie. when publishing first traceroute
 * message). This is needed because advertisements do not get routed to new joining brokers if it
 * was sent out previously.
 * 
 * 2) BROKER_DISCOVERY - Triggers BROKER_INFO advertisements to flow when an admin client connects
 * to a fully established network (no brokers will join the network in the future). This is needed
 * because advertisements do not get routed to new joining brokers if it was sent out previously.
 */
public class SystemMonitor extends Thread {

	// Public constants
	public static final String MESSAGE_CLASS = "BROKER_MONITOR";

	// Constant values used to describe the status of the broker
	protected static final String BROKER_STATUS_KEY = "Broker Status";

	protected static final String BROKER_STOPPED = "STOPPED";

	protected static final String BROKER_RUNNING = "RUNNING";

	protected static final String BROKER_SHUTDOWN = "SHUTDOWN";

	// The following constants are used in more than one area of the code,
	// Hence, they are here for consistency
	protected static final String QUEUE_TIME_KEY = "Queue Time";

	public static final String TRACEROUTE_MESSAGE_KEY = "TRACEROUTE_MESSAGE";

	// private static final String TRACEROUTE_PREV_BROKER_KEY =
	// "TRACEROUTE_PREV_BROKER";

	// Constants for hard coding
	protected static final long SLEEP_AFTER_ADV_TRACEROUTE = 6000;

	// Stores the singleton instance of this object
	// private static SystemMonitor instance = null;

	// Class private variables
	protected boolean started = false;

	protected Object started_lock = new Object();

	protected MessageQueue inQueue; // reference to monitor message queue

	protected BrokerCore brokerCore;

	protected BrokerInfoPublisher brokerInfoPublisher;

	// private boolean advertisedTraceroute;

	protected boolean advertisedBrokerInfo;

	protected boolean advertisedMsgSetDelivery;

	// The following variables are used for calculating the
	// publication rate
	protected long[] startTimes;

	protected long[] messageCounts;

	protected static final Object rateMutex = new Object();

	// Used for indexing the above arrays
	protected static final int MESSAGES_TO_TRACK = 2;

	protected static final int PUBLICATION_MESSAGES = MESSAGES_TO_TRACK - 1;

	protected static final int CONTROL_MESSAGES = MESSAGES_TO_TRACK - 2;

	// messageID-time pairs for calculating queue and match times
	private QueueTimeManager queueTimeManager;

	// private Hashtable timeOfEnqueue;
	// private Hashtable timeOfDequeue;
	protected long totalQueueTime;

	protected long totalMatchTime;

	protected long messagesTimed; // number of messages used in calculation

	protected static final Object timerMutex = new Object();

	/* Constant for the broker info's payload key */
	public static final String PUB_INTERVAL = "Publication Interval";

	public static final String INPUT_QUEUE_SIZE = "InputQueue Size";

	public static final String NEIGBOURS = "Neighbours";

	public static final String CLIENTS = "Clients";

	public static final String JVM_VERSION = "JVM_VERSION";

	public static final String JVM_VENDOR = "JVM_VENDOR";

	public static final String PORT = "PORT";

	public static final String HOST = "HOST";

	/* Constant for getting set of adv or sub to the client */
	public static final String COMM_GET_ADV = "COMM_GET_ADV";

	public static final String COMM_GET_SUB = "COMM_GET_SUB";

	public static final String MSG_SET_DELIVERY_CLASS = "MSG_SET_DELIVERY_CLASS";

	public static final String COMM_SESSION_ID = "COMM_SESSION_ID";

	public static final String MSG_SET_MAP_KEY = "MSG_SET_MAP_KEY";

	public static final int TYPE_ADV = 0;

	public static final int TYPE_SUB = 1;

	// command line argument and properties related

	public static final String PROP_ADV_SUB_INFO_TYPE = "padres.monitor.advsubinfo";

	public enum AdvSubInfoType {
		FULL, COUNT;
	}

	// for testing, store the current processed publication msg
	private PublicationMessage currentPubMsg = null;

	// add for logging. Log performance information with a log_interval
	private int log_interval = 20000; // the log_interval can also be defined

	// in the broker property file

	static Logger performanceLogger = Logger.getLogger("Performance");

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	static Logger systemMonitorLogger = Logger.getLogger(SystemMonitor.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Constructor
	 */
	public SystemMonitor(BrokerCore broker) {
		super("SystemMonitor");
		brokerCore = broker;
		performanceLogger.debug("AverageMatchTime    AverageQueueTime    PubMsgRate    ControlMsgRate    FreeMemory"
				+ "    #Advertisements    #Subscriptions    #Neighbours    #Clients");
		// log performance metric
	}

	/*
	 * Implementation of java.lang.Runnable's run() This is actually the initialization part
	 */
	public void run() {
		synchronized (started_lock) {
			// advertisedTraceroute = false;
			advertisedBrokerInfo = false;
			advertisedMsgSetDelivery = false;

			// register system monitor with broker core
			inQueue = createMessageQueue();
			brokerCore.registerQueue(MessageDestination.SYSTEM_MONITOR, inQueue);

			// Initialize message rate calculation variables
			startTimes = new long[MESSAGES_TO_TRACK];
			messageCounts = new long[MESSAGES_TO_TRACK];
			startTimes[PUBLICATION_MESSAGES] = new Date().getTime();
			startTimes[CONTROL_MESSAGES] = new Date().getTime();
			messageCounts[PUBLICATION_MESSAGES] = 0;
			messageCounts[CONTROL_MESSAGES] = 0;

			// Initialize timing variables
			queueTimeManager = new QueueTimeManager();

			totalQueueTime = 0;
			totalMatchTime = 0;
			messagesTimed = 0;

			// Prepare to receive commands...
			sendSubscriptionForMsgQueue();

			sendSubscriptionForNetworkDiscovery();

			// subscribe to golbal failure detection enable/disable message
			sendSubscriptionForGlobalFD();

			advertiseTraceroute();

			systemMonitorLogger.debug("Starting BrokerInfoPublisher.");
			// Start up the automatic broker info publisher
			brokerInfoPublisher = new BrokerInfoPublisher(brokerCore);
			brokerInfoPublisher.start();

			systemMonitorLogger.info("SystemMonitor is fully started.");
			// wake up threads waiting for SystemMonitor to start
			started = true;
			started_lock.notifyAll();

			ActionListener logPerformanceTaskPerformer = new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					// do logging
					if (performanceLogger.isDebugEnabled()) {
						ConcurrentHashMap<String, Object> brokerInfo = getBrokerInfo();
						String averageMatchTime = brokerInfo.get("Match Time").toString();
						String averageQueueTime = brokerInfo.get(QUEUE_TIME_KEY).toString();
						String incomingPubMsgRate = brokerInfo.get(
								"Incoming Publication Message Rate").toString();
						String incomingControlMsgRate = brokerInfo.get(
								"Incoming Control Message Rate").toString();
						String freeMemory = brokerInfo.get("Free Memory").toString();
						int numberOfAdvs = 0;
						int numberOfSubs = 0;
						AdvSubInfoType advSubInfoType = brokerCore.getBrokerConfig().getAdvSubInfoType();
						if (advSubInfoType == AdvSubInfoType.COUNT) {
							numberOfAdvs = Integer.parseInt(brokerInfo.get("Advertisements").toString());
							numberOfSubs = Integer.parseInt(brokerInfo.get("Subscriptions").toString());
						} else {
							numberOfAdvs = ((Set) brokerInfo.get("Advertisements")).size();
							numberOfSubs = ((Set) brokerInfo.get("Subscriptions")).size();
						}
						int numberOfNeighbours = ((Set) brokerInfo.get(NEIGBOURS)).size();
						int numberOfClients = ((Set) brokerInfo.get(CLIENTS)).size();
						performanceLogger.debug(averageMatchTime + "      " + averageQueueTime
								+ "      " + incomingPubMsgRate + "      " + incomingControlMsgRate
								+ "      " + freeMemory + "      " + numberOfAdvs + "      "
								+ numberOfSubs + "      " + numberOfNeighbours + "      "
								+ numberOfClients);
					}
				}
			};
			Timer performanceLogtimer = new Timer(log_interval, logPerformanceTaskPerformer);
			performanceLogtimer.start();
		}

		// we have to wait sometime for the timer thread is fully started. If
		// not, we could not get any log infomation.

		// Wait for incoming messages from the monitor queue to tell
		// us to do something
		listen();
	}

	protected MessageQueue createMessageQueue() {
		return new MessageQueue();
	}

	/**
	 * Block until the SystemMonitor is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}

	/*
	 * For testing
	 */
	public PublicationMessage getCurrentPubMsg() {
		return currentPubMsg;
	}

	/*
	 * We want to subscribe to all messages with class equal to BROKER_MONITOR
	 */
	private void sendSubscriptionForMsgQueue() {
		Subscription monitorSub;
		try {
			monitorSub = MessageFactory.createSubscriptionFromString("[class,eq," + MESSAGE_CLASS
					+ "],[brokerID,eq,'" + getBrokerID() + "']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		SubscriptionMessage monitorSubMsg = new SubscriptionMessage(monitorSub,
				brokerCore.getNewMessageID(), MessageDestination.SYSTEM_MONITOR);
		systemMonitorLogger.debug("SystemMonitor is sending subscription for messageQueue : "
				+ monitorSubMsg.toString() + ".");
		brokerCore.routeMessage(monitorSubMsg, MessageDestination.INPUTQUEUE);
	}

	/*
	 * We want to subscribe to all messages with class NETWORK_DISCOVERY This is a temporary hack
	 * against the advertisement problem (see above)
	 */
	private void sendSubscriptionForNetworkDiscovery() {
		Subscription sub;
		try {
			sub = MessageFactory.createSubscriptionFromString("[class,eq,NETWORK_DISCOVERY]");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(),
				MessageDestination.SYSTEM_MONITOR);
		systemMonitorLogger.debug("SystemMonitor is sending subscription for network discovery : "
				+ subMsg.toString() + ".");
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
	}

	public void sendSubscriptionForGlobalFD() {
		Subscription sub;
		try {
			sub = MessageFactory.createSubscriptionFromString("[class,eq,GLOBAL_FD],[flag,isPresent,'TEXT']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(),
				MessageDestination.SYSTEM_MONITOR);
		systemMonitorLogger.debug("SystemMonitor is sending subscription for global FD : "
				+ subMsg.toString() + ".");
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
	}

	/**
	 * Listen to input queue and see what we are told to do in the message
	 */
	private void listen() {
			while (started) {
				try {
				Message msg = (Message) inQueue.blockingRemove();
				
				if(msg.getType().equals(MessageType.SHUTDOWN))
					continue;
				
				systemMonitorLogger.debug("SystemMonitor receives message : " + msg.toString());
				if (msg.getClass() == PublicationMessage.class) {
					// for testing
					currentPubMsg = (PublicationMessage) msg;
					Publication pub = ((PublicationMessage) msg).getPublication();
					Map<String, Serializable> pairMap = pub.getPairMap();
					if (((String) pairMap.get("class")).equalsIgnoreCase(SystemMonitor.MESSAGE_CLASS)) {
						String command = (String) pairMap.get("command");
						systemMonitorLogger.debug("SystemMonitor receives command: " + command);
						// handle command
						// publication interval can only be integer since the matching engine does not
						// handle Long
						if (command.equalsIgnoreCase("SET_PUBLICATION_INTERVAL")) {
							setPublicationInterval(Integer.parseInt(pairMap.get("PUBLICATION_INTERVAL").toString()));
						} else if (command.equalsIgnoreCase("RESUME")) {
							resumeBroker();
						} else if (command.equalsIgnoreCase("SHUTDOWN")) {
							shutdownBroker();
						} else if (command.equalsIgnoreCase("STOP")) {
							stopBroker();
						} else if (command.equalsIgnoreCase("PUBLISH_BROKER_INFO")) {
							// Avoid cluttering up the output queue if stopped
							if (brokerCore.isRunning()) {
								// Wake it up and it will publish
								systemMonitorLogger.debug("Publishing brokerInfo for the system monitor.");
								brokerInfoPublisher.interrupt();
							}
						} else if (command.equalsIgnoreCase("TRACE_PUBLICATION_MSG")) {
							// Avoid cluttering up the output queue if stopped
							if (brokerCore.isRunning()) {
								String tracerouteID = pub.getPairMap().get("TRACEROUTE_ID").toString();
								String publication = pub.getPayload().toString();
								if (systemMonitorLogger.isDebugEnabled())
									systemMonitorLogger.debug("Sending publication : " + publication
											+ " with traceID " + tracerouteID + ".");
								tracePubMsg(tracerouteID, publication);
							}
						} else if (command.equalsIgnoreCase("TRACE_SUBSCRIPTION_MSG")) {
							// Avoid cluttering up the output queue if stopped
							if (brokerCore.isRunning()) {
								String tracerouteID = pub.getPairMap().get("TRACEROUTE_ID").toString();
								String subscription = pub.getPayload().toString();
								if (systemMonitorLogger.isDebugEnabled())
									systemMonitorLogger.debug("Sending subscription : " + subscription
											+ " with traceID " + tracerouteID + ".");
								traceSubMsg(tracerouteID, subscription);
							}
						} else if (command.equalsIgnoreCase(COMM_GET_ADV)) {
							if (brokerCore.isRunning()) {
								String commandSessionID = (String) pub.getPairMap().get(COMM_SESSION_ID);
								systemMonitorLogger.debug("Get advertisement set.");
								sendMsgSet(commandSessionID, TYPE_ADV);
							}
						} else if (command.equalsIgnoreCase(COMM_GET_SUB)) {
							if (brokerCore.isRunning()) {
								String commandSessionID = (String) pub.getPairMap().get(COMM_SESSION_ID);
								systemMonitorLogger.debug("Get subscription set.");
								sendMsgSet(commandSessionID, TYPE_SUB);
							}
						} else if (command.equalsIgnoreCase("SET_FD_PARAMS")) {
							ConcurrentHashMap<String, Object> payload = (ConcurrentHashMap) pub.getPayload();
							// Map payload = (Map)pub.getPayload();
							boolean enabled = ((String) payload.get("enabled")).equals("true") ? true
									: false;
							long interval = Long.parseLong((String) payload.get("interval"));
							long timeout = Long.parseLong((String) payload.get("timeout"));
							int threshold = Integer.parseInt((String) payload.get("threshold"));
							systemMonitorLogger.debug("Set Heartbeat parameters.");
							brokerCore.getHeartbeatPublisher().setHeartbeatParams(enabled, interval,
									timeout, threshold);
						} else {
							// unrecognized message for 'BROKER_MONITOR'
							systemMonitorLogger.warn("The command " + command
									+ " is not a valid command for systemMonitor");
							exceptionLogger.warn(new Exception("The command " + command
									+ " is not a valid command for systemMonitor"));
						}
					} else if (((String) pairMap.get("class")).equalsIgnoreCase("NETWORK_DISCOVERY")) {
						// Avoid cluttering up the output queue if stopped
						if (brokerCore.isRunning()) {
							if (!advertisedBrokerInfo) {
								advertiseBrokerInfo();
								brokerInfoPublisher.resumePublishing();
								mySleep(100); // let it sleep before waking it up
								advertisedBrokerInfo = false; // so that the broker can later refresh
							}
							systemMonitorLogger.debug("Publish brokerInfo immediately.");
							brokerInfoPublisher.interrupt();
						}
					} else if (((String) pairMap.get("class")).equalsIgnoreCase("GLOBAL_FD")) {
						// check flag and take appropriate action
						boolean flag = ((String) pairMap.get("flag")).equals("true") ? true : false;
						systemMonitorLogger.debug("Set GLOBAL_FD parameter.");
						brokerCore.getHeartbeatPublisher().setPublishHeartbeats(flag);
					} else {
						systemMonitorLogger.warn("Invalid message for systemMonitor.");
						exceptionLogger.warn(new Exception("Invalid message for systemMonitor."));
					}
				} else {
					systemMonitorLogger.warn("Non-publication message is sent to systemMonitor.");
					exceptionLogger.warn(new Exception(
							"Non-publication message is sent to systemMonitor."));
				}
				messagePathLogger.debug("SystemMonitor receives message : " + msg.toString());
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
			}
			}
	}

	/*
	 * Sends out an advertisement for the BROKER_INFO publication messages that we will publish
	 */
	private void advertiseBrokerInfo() {
		advertisedBrokerInfo = true;

		Advertisement advertisement;
		try {
			advertisement = MessageFactory.createAdvertisementFromString("[class,eq,BROKER_INFO],[brokerID,eq,'"
					+ brokerCore.getBrokerID() + "']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		AdvertisementMessage advMessage = new AdvertisementMessage(advertisement,
				brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
		systemMonitorLogger.debug("Sending advertisement for brokerInfo.");
		brokerCore.routeMessage(advMessage, MessageDestination.INPUTQUEUE);

		mySleep(SLEEP_AFTER_ADV_TRACEROUTE);
	}

	/*
	 * Little functions ------------------------------- START --------------------------------
	 */

	/**
	 * Gives all neighbours attached to this broker
	 * 
	 * @return The set of broker neighbours
	 */
	public Set<MessageDestination> getNeighbours() {
		Set<MessageDestination> neighborSet = Collections.synchronizedSet(new HashSet<MessageDestination>());
		Map<MessageDestination, OutputQueue> origSet = brokerCore.getOverlayManager().getORT().getBrokerQueues();
		synchronized (origSet) {
			for (MessageDestination dest : origSet.keySet()) {
				neighborSet.add(dest);
			}
		}

		return neighborSet;
	}

	/**
	 * Gives all clients attached to this broker
	 * 
	 * @return The set of broker clients
	 */
	public Set<MessageDestination> getClients() {
		Set<MessageDestination> neighborSet = Collections.synchronizedSet(new HashSet<MessageDestination>());
		Set<MessageDestination> origSet = brokerCore.getOverlayManager().getORT().getClients();
		synchronized (origSet) {
			for (MessageDestination dest : origSet) {
				neighborSet.add(dest);
			}
		}

		return neighborSet;

	}

	/**
	 * Get the advertisements in the broker.
	 * 
	 * @return The set of advertisements in the broker.
	 */
	public Set<AdvertisementMessage> getAdvertisements() {
		Set<AdvertisementMessage> advSet = Collections.synchronizedSet(new HashSet<AdvertisementMessage>());
		Map<String, AdvertisementMessage> origSet = brokerCore.getAdvertisements();
		synchronized (origSet) {
			for (AdvertisementMessage advMsg : origSet.values()) {
				advSet.add(advMsg.duplicate());
			}
		}
		return advSet;
	}

	/**
	 * Get the subscriptions in the broker.
	 * 
	 * @return The set of subscriptions in the broker.
	 */
	public Set<SubscriptionMessage> getSubscriptions() {
		Set<SubscriptionMessage> subSet = Collections.synchronizedSet(new HashSet<SubscriptionMessage>());
		Map<String, SubscriptionMessage> origSet = brokerCore.getSubscriptions();
		synchronized (origSet) {
			for (SubscriptionMessage subMsg : origSet.values()) {
				subSet.add(subMsg.duplicate());
			}
		}
		return subSet;
	}

	/**
	 * Set the debug mode of the broker
	 * 
	 * @param debugMode
	 *            True means to turn on debug mode, false means to turn off debug mode
	 */
	public void setBrokerDebugMode(boolean debugMode) {
		brokerCore.setDebugMode(debugMode);
	}

	/**
	 * Get the debug mode of the broker
	 * 
	 * @return Debug mode of the broker, where true means the broker is in debug mode
	 */
	public boolean getBrokerDebugMode() {
		return brokerCore.getDebugMode();
	}

	/**
	 * Returns the number of messages in the broker's input queue
	 * 
	 * @return the number of messages in the broker's input queue
	 */
	public int getInputQueueSize() {
		return brokerCore.getInputQueueSize();
	}

	/**
	 * Shuts down the broker
	 * @throws ParseException 
	 */
	public void shutdownBroker() {
		try {
			// Shutdown the broker info publisher
			brokerInfoPublisher.shutdown();
			stopMonitor();
			
			// Notify all monitors in the network that we're shutting down
			PublicationMessage pubMsg = getBrokerInfoInPublicationMessage();
			// Map payload = (Map) pubMsg.getPublication().getPayload();
			ConcurrentHashMap<String, Object> payload = (ConcurrentHashMap) pubMsg.getPublication().getPayload();
	
			// Overwrites the original value of BROKER_STATUS_KEY
			payload.put(BROKER_STATUS_KEY, BROKER_SHUTDOWN);
			systemMonitorLogger.debug("Sending brokerInfo for shutting down broker.");
			brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
	
			// Send the neighbours that the broker is going to shutdown, and update
			// neighbours'ORT
			OverlayRoutingTable ORT = brokerCore.getOverlayManager().getORT();
			Map<MessageDestination, OutputQueue> neighbours = ORT.getBrokerQueues();
	
			systemMonitorLogger.debug("Notify the neighbours that the broker is going to shutdown.");
			synchronized (neighbours) {
				for (MessageDestination nextHopID : neighbours.keySet()) {
					if (!nextHopID.equals(brokerCore.getBrokerDestination())) {
						Publication sdPub = MessageFactory.createPublicationFromString("[class,BROKER_CONTROL],[brokerID,'"
								+ nextHopID.getDestinationID() + "'],[fromID,'" + getBrokerID()
								+ "'],[command,'OVERLAY-SHUTDOWN_REMOTEBROKER']");
						PublicationMessage sdPubMsg = new PublicationMessage(sdPub,
								brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
						sdPubMsg.setNextHopID(nextHopID);
						brokerCore.routeMessage(sdPubMsg);
					}
				}
			}
	
			// Allow some time for the message to get through the queue using
			// some statistical data :)
			mySleep(new Long((String) payload.get(QUEUE_TIME_KEY)).longValue() + 500); // for
			// variance
			
			inQueue.addFirst(new ShutdownMessage());

			
			// Shutdown the broker AFTER we have routed the BROKER_INFO message above
			brokerCore.shutdown();
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
		}
	}

	/**
	 * Start up the broker
	 */
	public void resumeBroker() {
		if (!isRunning()) {
			brokerCore.resume();

			// Resume publishing broker info messages
			brokerInfoPublisher.resumePublishing();
			mySleep(100);

			// Immediately send out an update of our state
			systemMonitorLogger.debug("Sending brokerInfo immediately for resuming broker.");
			brokerInfoPublisher.interrupt();
		}
	}

	/**
	 * Stops all of the broker's operations
	 */
	public void stopBroker() {
		if (isRunning()) {
			// This must come first to prevent sending out false information later
			brokerInfoPublisher.stopPublishing();

			// Send out a final BROKER_INFO saying that we stopped before stopping
			PublicationMessage pubMsg;
			try {
				pubMsg = getBrokerInfoInPublicationMessage();
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
				return;
			}
			// Map payload = (Map) pubMsg.getPublication().getPayload();
			ConcurrentHashMap<String, Object> payload = (ConcurrentHashMap) pubMsg.getPublication().getPayload();

			payload.put(BROKER_STATUS_KEY, BROKER_STOPPED);
			systemMonitorLogger.debug("Sending brokerInfo for stopping broker.");
			brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

			// Allow some time for the message to get through the queue using
			// some statistical data :)
			mySleep(new Long((String) payload.get(QUEUE_TIME_KEY)).longValue() + 500); // for
			// variance

			// Stop BrokerInfoPublisher from filling up the input queue when
			// no one processes the queues
			brokerCore.stop();
		}
	}

	/**
	 * Indicates whether this broker is running or not
	 * 
	 * @return boolean value, true indicates the broker is running, false means the broker is
	 *         stopped.
	 */
	public boolean isRunning() {
		return brokerCore.isRunning();
	}

	/**
	 * Returns the ID of the broker
	 * 
	 * @return the ID of the broker
	 */
	public String getBrokerID() {
		return brokerCore.getBrokerID();
	}

	/**
	 * Returns the URI of the broker
	 * 
	 * @return the URI of the broker
	 */
	public String getBrokerURI() {
		return brokerCore.getBrokerURI();
	}

	/**
	 * Returns the time between broker info publications
	 * 
	 * @return the time between broker info publications
	 */
	public long getPublicationInterval() {
		return brokerInfoPublisher.getPublicationInterval();
	}

	/**
	 * Set the time between broker info publications
	 * 
	 * @param interval
	 *            time between broker info publications
	 */
	public void setPublicationInterval(long interval) {
		brokerInfoPublisher.setPublicationInterval(interval);

		// make the sleep time immediately take effect
		brokerInfoPublisher.interrupt();
	}

	public void forcePublishBrokerInfo() {
		brokerInfoPublisher.publishBrokerInfo();
	}

	// ------------------------ END OF GOOD TIMES --------------------------

	/**
	 * Creates a publication message that contains all of the: - general - detailed - performance -
	 * system information about this broker.
	 * 
	 * @return publication message containing all of the information
	 */
	public ConcurrentHashMap<String, Object> getBrokerInfo() {
		// Initializations
		Runtime rt = Runtime.getRuntime();
		ConcurrentHashMap<String, Object> pairMap = new ConcurrentHashMap<String, Object>();

		try {
			// Broker-level "general" information
			pairMap.put("Broker ID", getBrokerID());
			pairMap.put("Broker URI", getBrokerURI());
			pairMap.put(BROKER_STATUS_KEY, (isRunning() ? BROKER_RUNNING : BROKER_STOPPED));

			// Broker-level "detailed" information
			pairMap.put("Debug Mode", Boolean.valueOf(brokerCore.getDebugMode()));
			pairMap.put(PUB_INTERVAL, new Long(getPublicationInterval()).toString());

			pairMap.put(INPUT_QUEUE_SIZE, Integer.toString(getInputQueueSize()));
			pairMap.put(NEIGBOURS, getNeighbours());
			pairMap.put(CLIENTS, getClients());

			AdvSubInfoType advSubInfoType = brokerCore.getBrokerConfig().getAdvSubInfoType();
			if (advSubInfoType == AdvSubInfoType.COUNT) {
				pairMap.put("Advertisements", brokerCore.getAdvertisements().size());
				pairMap.put("Subscriptions", brokerCore.getSubscriptions().size());
				// } else if (advSubInfoType == advSubInfoType.FULL) {
			} else {
				pairMap.put("Advertisements", getAdvertisements());
				pairMap.put("Subscriptions", getSubscriptions());
			}

			// Broker-level performance data
			pairMap.put("Incoming Publication Message Rate",
					Long.toString(getPublicationMessageRate()));
			pairMap.put("Incoming Control Message Rate", Long.toString(getControlMessageRate()));
			String processingTimes = getProcessingTimes();
			pairMap.put(QUEUE_TIME_KEY, getQueueTime(processingTimes));
			pairMap.put("Match Time", getMatchTime(processingTimes));

			// System related information
			// pairMap.put(PORT, new Integer(brokerCore.getPort()));
			// pairMap.put(HOST, brokerCore.getHost());
			pairMap.put("URI", brokerCore.getBrokerConfig().getBrokerURI());
			pairMap.put(JVM_VERSION, System.getProperty("java.vm.version"));
			pairMap.put(JVM_VENDOR, System.getProperty("java.vm.vendor"));
			pairMap.put("Operating System",
					System.getProperty("os.name") + ", " + System.getProperty("os.arch") + ", "
							+ System.getProperty("os.version"));
			pairMap.put("Available Processors", Long.toString(rt.availableProcessors()));
			pairMap.put("Free Memory", Long.toString(rt.freeMemory()));
			pairMap.put("Maximum Memory Usage", Long.toString(rt.maxMemory()));
			pairMap.put("Total Memory", Long.toString(rt.totalMemory()));
		} catch (NullPointerException e) {
			e.printStackTrace();
			systemMonitorLogger.error("Null value in the payload of BrokerInfo message : "
					+ e.toString());
			exceptionLogger.error("Null value in the payload of BrokerInfo message : "
					+ e.toString());
		}
		return pairMap;
	}

	/**
	 * Used primarily by BrokerInfoPublisher
	 * 
	 * @return publication message containing the pair map of all information about the broker
	 * @throws ParseException 
	 */
	public PublicationMessage getBrokerInfoInPublicationMessage() throws ParseException {
		return makeInfoPubMsg(getBrokerInfo());
	}

	/*
	 * Prepares the publication message containing all of the broker's information
	 */
	private PublicationMessage makeInfoPubMsg(Serializable payload) throws ParseException {
		// Make the publication
		Publication pub = MessageFactory.createPublicationFromString("[class,BROKER_INFO]," + "[brokerID,'" + getBrokerID()
				+ "']");
		pub.setPayload(payload);

		// Make the publication message
		PublicationMessage pubmsg = new PublicationMessage(pub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());

		return pubmsg;
	}

	/*
	 * The following is code that supports getting of incoming publication rate and control message
	 * rate ----------------------------- START -------------------------------
	 */

	/**
	 * Returns the publications processed per second
	 * 
	 * @return the publications processed per second
	 * 
	 *         Note: If problem arises with the publication count, then make this method
	 *         synchronized
	 */
	public long getPublicationMessageRate() {
		long publicationRate;
		long elapsedTime;

		synchronized (rateMutex) {
			// Calculate the publication rate
			elapsedTime = (new Date()).getTime() - startTimes[PUBLICATION_MESSAGES];

			if (elapsedTime <= 0) {
				publicationRate = 0;
			} else {
				publicationRate = Math.round(messageCounts[PUBLICATION_MESSAGES] / elapsedTime
						* 1000);
			}

			// reset all counters
			startTimes[PUBLICATION_MESSAGES] = new Date().getTime();
			messageCounts[PUBLICATION_MESSAGES] = 0;
		}

		return publicationRate;
	}

	/**
	 * Returns the control messages processed per second
	 * 
	 * @return the control messages processed per second
	 * 
	 *         Note: If problem arises with the message count, then make this method synchronized
	 */
	public long getControlMessageRate() {
		long publicationRate;
		long elapsedTime;

		synchronized (rateMutex) {
			// Calculate the publication rate
			elapsedTime = (new Date()).getTime() - startTimes[CONTROL_MESSAGES];

			if (elapsedTime <= 0) {
				publicationRate = 0;
			} else {
				publicationRate = Math.round(messageCounts[CONTROL_MESSAGES] / elapsedTime * 1000);
			}

			// reset all counters
			startTimes[CONTROL_MESSAGES] = new Date().getTime();
			messageCounts[CONTROL_MESSAGES] = 0;
		}

		return publicationRate;
	}

	/**
	 * Gives a (slight future) estimate of the messages processed by this broker All subscription,
	 * unsubscription, advertisement, unadvertisement messages are considered control messages
	 * because they establish the routing tables. Also, only publication messages with its
	 * class/topic set to "BROKER_CONTROL" will also be considered control messages.
	 */
	public void countMessage(Message message) {
		MessageType messageType = message.getType();

		synchronized (rateMutex) {
			// Publication Message
			if (messageType == MessageType.PUBLICATION) {
				if (isControlMessage((PublicationMessage) message))
					messageCounts[CONTROL_MESSAGES]++;
				else
					messageCounts[PUBLICATION_MESSAGES]++;
			}
			// All other messages are considered control messages
			else {
				messageCounts[CONTROL_MESSAGES]++;
			}
		}
	}

	/*
	 * Tests if a publicaiton message is a control message @param pubMsg @return
	 */
	private boolean isControlMessage(PublicationMessage pubMsg) {
		Map<String, Serializable> pairMap = pubMsg.getPublication().getPairMap();
		if (pairMap.containsKey("class")) {
			String value = pairMap.get("class").toString();
			if (value.equalsIgnoreCase("BROKER_CONTROL")
					|| value.equalsIgnoreCase(SystemMonitor.MESSAGE_CLASS)
					|| value.equalsIgnoreCase(TRACEROUTE_MESSAGE_KEY)
					|| value.equalsIgnoreCase("NETWORK_DISCOVERY")
					|| value.equalsIgnoreCase("GLOBAL_FD")) {
				return true;
			}
		}
		return false;
	}

	/*
	 * ----------------------------- END ---------------------------------
	 */
	/*
	 * The following is code that supports getting of average queueing time and matching time
	 * ----------------------------- START -------------------------------
	 */

	/**
	 * Returns a string tuple <average queue time>,<average match time>
	 * 
	 * @return a string tuple <average queue time>,<average match time>
	 */
	public String getProcessingTimes() {
		Long averageQueueTime;
		Long averageMatchTime;

		synchronized (timerMutex) {
			if (messagesTimed == 0) {
				averageQueueTime = new Long(0);
				averageMatchTime = new Long(0);
			} else {
				averageQueueTime = new Long(totalQueueTime / messagesTimed);
				averageMatchTime = new Long(totalMatchTime / messagesTimed);
			}

			messagesTimed = 0;
			totalQueueTime = 0;
			totalMatchTime = 0;
		}

		return averageQueueTime.toString() + "," + averageMatchTime.toString();
	}

	/**
	 * Set the time the message got enqueued into the input queue of the broker. Called only by
	 * countMessageAndSetEnqueueTime()
	 * 
	 * @param messageID
	 *            id of the message that got enqueued
	 */
	public void setEnqueueTime(String messageID) {
		queueTimeManager.setEnqueueTime(messageID);

		// timeOfEnqueue.put(messageID, new Date());
	}

	/**
	 * Set the time the message got dequeued out of the input queue of the broker
	 * 
	 * @param messageID
	 *            id of the message that got dequeued
	 */
	public void setDequeueTime(String messageID) {
		queueTimeManager.setDequeueTime(messageID);

		// timeOfDequeue.put(messageID, new Date());
	}

	/**
	 * Set the time the matching engine returned after processing the message
	 * 
	 * @param messageID
	 *            id fo the message that got processed by the matching engine
	 */
	public void setMatchTime(String messageID) {
		long matchTime = new Date().getTime();
		long enqueueTime = 0;
		long dequeueTime = 0;

		enqueueTime = queueTimeManager.removeEnqueueTime(messageID);
		dequeueTime = queueTimeManager.removeDequeueTime(messageID);

		// enqueueTime = ((Date)timeOfEnqueue.remove(messageID)).getTime();
		// dequeueTime = ((Date)timeOfDequeue.remove(messageID)).getTime();
		synchronized (timerMutex) {
			totalQueueTime += dequeueTime - enqueueTime;
			totalMatchTime += matchTime - dequeueTime;
			messagesTimed++;
		}
	}

	/*
	 * Gives the queueing time from the string returned from getProcessingTimes()
	 */
	private String getQueueTime(String times) {
		return times.substring(0, times.indexOf(","));
	}

	/*
	 * Gives the matching time from the string returned from getProcessingTimes()
	 */
	private String getMatchTime(String times) {
		return times.substring(times.indexOf(",") + 1);
	}

	/*
	 * ----------------------------- END ---------------------------------
	 */
	/*
	 * To make the code cleaner :)
	 */
	private void mySleep(long time) {
		try {
			Thread.sleep(time);
		} catch (Exception e) {
			System.err.println("ERROR: " + e.getMessage());
		}
	}

	// ========================== START ====================================
	// The following code supports the tracing of a publication message

	/*
	 * Makes a "fake" publication message to allow the monitoring tool to trace the path of the
	 * publication. Payload should be a HashMap containing all of the predicates to set in the fake
	 * publication.
	 * 
	 * Note: if things behave weirdly, comment out the line that routes the unadvertisement message
	 */
	private void tracePubMsg(String tracerouteID, String strPublication) throws ParseException {
		// TODO: Modify brokerCore not to route these special publications
		// to clients

		/*
		 * 1. Advertise the fake publication (using given pair)
		 * 
		 * 2. publish the fake publication (using given pair) - in the pairMap of the publication,
		 * include a special key indicating not to route to clients
		 * 
		 * 3. Unadvertise fake publication (using given pair)
		 * 
		 * 4. Send an acknowledgement if necessary
		 */

		// Used for unadvertising later.
		String advertisementID = brokerCore.getNewMessageID();

		// Traced publication messages are specially identified by a predicate
		// [TRACEROUTE_MESSAGE,'<traceID>']

		// Make the advertisement message
		Advertisement adv = Advertisement.toAdvertisement(strPublication);
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advertisementID,
				brokerCore.getBrokerDestination());

		// Make the publication message
		Publication pub = MessageFactory.createPublicationFromString(strPublication);
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());
		pubMsg.setPrevBrokerID(getBrokerID());
		pubMsg.setTraceRouteID(tracerouteID);

		// Make the unadvertisement message for the traced publication message
		Unadvertisement unadv = new Unadvertisement(advertisementID);
		UnadvertisementMessage unadvMsg = new UnadvertisementMessage(unadv,
				brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
		systemMonitorLogger.debug("Sending advertisement corresponding to publication with tracedID.");
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		systemMonitorLogger.debug("Sending publication with tracedID.");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		systemMonitorLogger.debug("Sending unadvertisement corresponding to publication with tracedID.");
		brokerCore.routeMessage(unadvMsg, MessageDestination.INPUTQUEUE);
	}

	private void traceSubMsg(String tracerouteID, String strSub) throws ParseException {

		/*
		 * 
		 * 1. subscribe the fake subscription (using given pair) - in the pairMap of the
		 * publication, include a special key indicating not to route to clients
		 */

		// Traced publication messages are specially identified by a predicate
		// [TRACEROUTE_MESSAGE,'<traceID>']
		/*
		 * strSub = strSub.concat( ",[" + TRACEROUTE_MESSAGE_KEY + ",eq,'" + tracerouteID + "']"+
		 * ",[" + TRACEROUTE_PREV_BROKER_KEY + ",eq,'" + getBrokerID() + "']");
		 */

		// Make the subscription message
		Subscription sub = MessageFactory.createSubscriptionFromString(strSub);
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());

		subMsg.setPrevBrokerID(getBrokerID());
		subMsg.setTraceRouteID(tracerouteID);
		systemMonitorLogger.debug("Sending subscription with tracedID.");
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

	}

	/*
	 * Deterines whether a message is a traceroute message
	 */
	private boolean isTracerouteMessage(Message message) {
		/*
		 * if (message.getClass() == PublicationMessage.class) { Publication pub =
		 * ((PublicationMessage) message).getPublication();
		 * 
		 * return (pub.getPairMap().get(TRACEROUTE_MESSAGE_KEY) != null); } else if
		 * (message.getClass() == SubscriptionMessage.class) { Subscription sub =
		 * ((SubscriptionMessage)message).getSubscription();
		 * 
		 * return (sub.getPredicateMap().get(TRACEROUTE_MESSAGE_KEY) != null); } return false;
		 */

		return (!message.getPrevBrokerID().equals("dummy") && !message.getTraceRouteID().equals(
				"dummy"));

		// Add subscription and advertisement message cases in the future

	}

	/**
	 * Prevents delivering traceroute messages (fake publication messages injected by the admin
	 * client) to normal clients
	 * 
	 * @param msg
	 *            message to be routed
	 * @return boolean result indicating whether to stop the delivery
	 */
	public boolean stopTracerouteMsgDelivery(Message msg) {

		// return (isTracerouteMessage(msg) && !msg.getNextHopID().isBroker());
		/**
		 * ** This is a Hack. We have two type of trace message. 1. Inject by monitor. Don't deliver
		 * 2. Normal message with trace info put in. Deliver For CASCON we will not have case 1. but
		 * have case 2. For now return flase.
		 */
		return false;

	}

	public void notifyOfTracerouteMessage(Message msg, boolean flag) {
		// flag is used to help generating TraceRoute message for the
		// client->broker message
		if (isTracerouteMessage(msg)) {
			try {
				if (msg.getClass() == PublicationMessage.class
						|| msg.getClass() == SubscriptionMessage.class) {
	
					String tracerouteID = msg.getTraceRouteID();
					String previousBrokerID = msg.getPrevBrokerID();
					String previousClientID = msg.getPrevClientID();
					String destination = msg.getNextHopID().getDestinationID();
					msg.setPrevBrokerID(destination);
					msg.setPrevClientID("dummy");
	
					// Prevent the loop back message gotten published
					if (!destination.equals(previousBrokerID)) {
						Publication pub = MessageFactory.createPublicationFromString("[class," + TRACEROUTE_MESSAGE_KEY + "],"
								+ "[to,'" + destination + "']," + "[from, '" + previousBrokerID + "'],"
								+ "[TRACEROUTE_ID,'" + tracerouteID + "']");
	
						PublicationMessage pubMsg = new PublicationMessage(pub,
								brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
	
						systemMonitorLogger.debug("Trace Notifcation message:" + pubMsg);
						brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
					}
	
					if ((!previousClientID.equals("dummy")) && flag) {
						Publication pub = MessageFactory.createPublicationFromString("[class," + TRACEROUTE_MESSAGE_KEY + "],"
								+ "[to,'" + previousBrokerID + "']," + "[from, '" + previousClientID
								+ "']," + "[TRACEROUTE_ID,'" + tracerouteID + "']");
	
						PublicationMessage pubMsg = new PublicationMessage(pub,
								brokerCore.getNewMessageID(), brokerCore.getBrokerDestination());
	
						systemMonitorLogger.debug("Trace Notifcation message:" + pubMsg);
						brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
					}
				}
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
			}
		}
		// Fill in the blanks for subscription and advertisement messages
		// in the future.

	}

	// Send out an advertisement for traceroute message notification
	private void advertiseTraceroute() {
		// advertisedTraceroute = true;

		Advertisement adv;
		try {
			adv = MessageFactory.createAdvertisementFromString("[class,eq," + TRACEROUTE_MESSAGE_KEY + "],"
					+ "[from,isPresent,'Dummy']," + "[to,isPresent,'" + getBrokerID() + "']," +
					// change Apr 14, 2007 -shou, for trace route message from
					// broker(client) to client(broker).
					// "[brokerID,eq,'" + getBrokerID() + "']," +
					"[TRACEROUTE_ID,isPresent,'12345']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());

		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		// mySleep(SLEEP_AFTER_ADV_TRACEROUTE);
	}

	private void advertiseMsgSetDelivery() {
		advertisedMsgSetDelivery = true;

		String advMsgID = brokerCore.getNewMessageID();
		/* Advertise first */
		String advStr = getMsgSetDeliveryAdvStr(getBrokerID());

		Advertisement adv;
		try {
			adv = MessageFactory.createAdvertisementFromString(advStr);
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}

		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advMsgID,
				brokerCore.getBrokerDestination());

		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		mySleep(SLEEP_AFTER_ADV_TRACEROUTE);
	}

	private void sendMsgSet(String sessionID, int type) throws ParseException {

		if (!advertisedMsgSetDelivery) {
			advertiseMsgSetDelivery();
		}

		/* publish now */
		String pubStr = getMsgSetDeliveryPubStr(getBrokerID(), sessionID);

		Publication pub = MessageFactory.createPublicationFromString(pubStr);
		// for concurrentModificatoin
		Map<String, Set<Message>> pairMap = Collections.synchronizedMap(new HashMap<String, Set<Message>>());
		Set<Message> msgSet = Collections.synchronizedSet(new HashSet<Message>());
		switch (type) {
		case TYPE_ADV:
			msgSet.addAll(getAdvertisements());
			pairMap.put(MSG_SET_MAP_KEY, msgSet);
			break;
		case TYPE_SUB:
			msgSet.addAll(getSubscriptions());
			pairMap.put(MSG_SET_MAP_KEY, msgSet);
			break;
		default:
			break;
		}

		pub.setPayload((Serializable) pairMap);
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(),
				brokerCore.getBrokerDestination());
		systemMonitorLogger.debug("Sending publication for getting message set.");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
	}

	/*
	 * Return the string that represent the subscription of the message that will deliver the set of
	 * adv or sub to the client
	 */
	public static String getMsgSetDeliverySubStr(String brokerID, String commandSessionID) {
		return "[class,eq,'" + MSG_SET_DELIVERY_CLASS + "']," + "[" + COMM_SESSION_ID + ",eq,'"
				+ commandSessionID + "']";
	}

	public static String getMsgSetDeliveryAdvStr(String brokerID) {
		return "[class,eq,'" + MSG_SET_DELIVERY_CLASS + "']," + "[" + COMM_SESSION_ID
				+ ",isPresent,'dummy']";
	}

	public static String getMsgSetDeliveryPubStr(String brokerID, String commandSessionID) {
		return "[class,'" + MSG_SET_DELIVERY_CLASS + "']," + "[" + SystemMonitor.COMM_SESSION_ID
				+ ",'" + commandSessionID + "']";
	}

	public static String getGetAdvSetCommClientPubStr(String brokerID, String commandSessionID) {
		return "[class,BROKER_MONITOR]," + "[brokerID,'" + brokerID + "']," + "[command,'"
				+ COMM_GET_ADV + "']," + "[" + COMM_SESSION_ID + ",'" + commandSessionID + "']";
	}

	public static String getGetSubSetCommClientPubStr(String brokerID, String commandSessionID) {
		return "[class,BROKER_MONITOR]," + "[brokerID,'" + brokerID + "']," + "[command,'"
				+ COMM_GET_SUB + "']," + "[" + COMM_SESSION_ID + ",'" + commandSessionID + "']";
	}

	public void stopMonitor(){
		started = false;
	}
	
	// ============================ END ====================================
}

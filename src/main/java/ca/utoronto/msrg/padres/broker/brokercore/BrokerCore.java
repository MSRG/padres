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
 * Created on 16-Jul-2003
 */
package ca.utoronto.msrg.padres.broker.brokercore;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Map;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig.CycleType;
import ca.utoronto.msrg.padres.broker.controller.Controller;
import ca.utoronto.msrg.padres.broker.controller.LinkInfo;
import ca.utoronto.msrg.padres.broker.controller.OverlayManager;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.broker.management.console.ConsoleInterface;
import ca.utoronto.msrg.padres.broker.management.web.ManagementServer;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.RouterFactory;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
import ca.utoronto.msrg.padres.broker.webmonitor.monitor.WebUIMonitor;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.common.util.timer.TimerThread;

/**
 * The core of the broker. The broker is instantiated through this class. BrokerCore provides unique
 * message ID generation, component location, message routing.
 * 
 * @author eli
 * 
 */
public class BrokerCore {

	protected BrokerConfig brokerConfig;

	protected Controller controller;

	protected SystemMonitor systemMonitor;

	protected QueueManager queueManager;

	protected InputQueueHandler inputQueue;

	protected Router router;

	protected WebUIMonitor webuiMonitor;

	protected TimerThread timerThread;

	protected HeartbeatPublisher heartbeatPublisher;

	protected HeartbeatSubscriber heartbeatSubscriber;

	protected MessageDestination brokerDestination;

	protected int currentMessageID;

	protected CommSystem commSystem;

	protected boolean debug = BrokerConfig.DEBUG_MODE_DEFAULT;

	// Indicates whether this broker is running or not
	protected boolean running = false;

	protected boolean isCycle = false;

	protected boolean isDynamicCycle = false;

	// for dynamic cycle, check message rate. the time_window_interval can also be defined in the
	// broker property file
	protected int time_window_interval = 5000;

	private boolean isShutdown = false;

	protected static Logger brokerCoreLogger;

	protected static Logger exceptionLogger;

	/**
	 * Constructor for one argument. To take advantage of command line arguments, use the
	 * 'BrokerCore(String[] args)' constructor
	 * 
	 * @param arg
	 * @throws IOException
	 */
	public BrokerCore(String arg) throws BrokerCoreException {
		this(arg.split("\\s+"));
	}

	public BrokerCore(String[] args, boolean def) throws BrokerCoreException {
		if (args == null) {
			throw new BrokerCoreException("Null arguments");
		}
		CommandLine cmdLine = new CommandLine(BrokerConfig.getCommandLineKeys());
		try {
			cmdLine.processCommandLine(args);
		} catch (Exception e) {
			throw new BrokerCoreException("Error processing command line", e);
		}
		// make sure the logger is initialized before everything else
		initLog(cmdLine.getOptionValue(BrokerConfig.CMD_ARG_FLAG_LOG_LOCATION));
		brokerCoreLogger.debug("BrokerCore is starting.");
		// load properties from given/default properties file get the broker configuration
		String configFile = cmdLine.getOptionValue(BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS);
		try {
			if (configFile == null)
				brokerConfig = new BrokerConfig();
			else
				brokerConfig = new BrokerConfig(configFile, def);
		} catch (BrokerCoreException e) {
			brokerCoreLogger.fatal(e.getMessage(), e);
			exceptionLogger.fatal(e.getMessage(), e);
			throw e;
		}
		// overwrite the configurations from the config file with the configurations from the
		// command line
		brokerConfig.overwriteWithCmdLineArgs(cmdLine);
		// check broker configuration
		try {
			brokerConfig.checkConfig();
		} catch (BrokerCoreException e) {
			brokerCoreLogger.fatal("Missing uri key or uri value in the property file.");
			exceptionLogger.fatal("Here is an exception : ", e);
			throw e;
		}
		// initialize the message sequence counter
		currentMessageID = 0;
	}
	
	/**
	 * Constructor
	 * 
	 * @param args
	 */
	public BrokerCore(String[] args) throws BrokerCoreException {
		this(args, true);
	}

	public BrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		// make sure the logger is initialized before everything else
		initLog(brokerConfig.getLogDir());
		brokerCoreLogger.debug("BrokerCore is starting.");
		this.brokerConfig = brokerConfig;
		try {
			this.brokerConfig.checkConfig();
		} catch (BrokerCoreException e) {
			brokerCoreLogger.fatal("Missing uri key or uri value in the property file.");
			exceptionLogger.fatal("Here is an exception : ", e);
			throw e;
		}
		currentMessageID = 0;
	}

	protected void initLog(String logPath) throws BrokerCoreException {
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			try {
				new LogSetup(logPath);
			} catch (LogException e) {
				throw new BrokerCoreException("Initialization of Logger failed: ", e);
			}
		}
		brokerCoreLogger = Logger.getLogger(BrokerCore.class);
		exceptionLogger = Logger.getLogger("Exception");
	}

	/**
	 * Initialize the broker. It has to be called externally; the constructor does not use this
	 * method. Components are started up in a particular order, and initialize() doesn't return
	 * until the broker is fully started.
	 * 
	 * @throws BrokerCoreException
	 */
	public void initialize() throws BrokerCoreException {
		// Initialize some parameters
		isCycle = brokerConfig.isCycle();
		isDynamicCycle = brokerConfig.getCycleOption() == CycleType.DYNAMIC;
		// Initialize components
		initCommSystem();
		brokerDestination = new MessageDestination(getBrokerURI(), DestinationType.BROKER);
		initQueueManager();
		initRouter();
		initInputQueue();
		// System monitor must be started before sending/receiving any messages
		initSystemMonitor();
		initController();
		startMessageRateTimer();
		initTimerThread();
		initHeartBeatPublisher();
		initHeartBeatSubscriber();
		initWebInterface();
		initNeighborConnections();
		initManagementInterface();
		initConsoleInterface();
		running = true;
		brokerCoreLogger.info("BrokerCore is started.");
	}

	/**
	 * Initialize the communication layer in the connection listening mode.
	 * 
	 * @throws BrokerCoreException
	 */
	protected void initCommSystem() throws BrokerCoreException {
		// initialize the communication interface
		try {
			commSystem = createCommSystem();
			commSystem.createListener(brokerConfig.brokerURI);
			brokerCoreLogger.info("Communication System created and a listening server is initiated");
		} catch (CommunicationException e) {
			brokerCoreLogger.error("Communication layer failed to instantiate: " + e);
			exceptionLogger.error("Communication layer failed to instantiate: " + e);
			throw new BrokerCoreException("Communication layer failed to instantiate: " + e + "\t" + brokerConfig.brokerURI);
		}
	}

	/**
	 * Initialize the message queue manager which acts as a multiplexer between the communication
	 * layer and all the queues for different internal components as well as external connections.
	 * Initialize the queue manager only after initialzing the communication layer.
	 * 
	 * @throws BrokerCoreException
	 */
	protected void initQueueManager() throws BrokerCoreException {
		queueManager = createQueueManager();
		brokerCoreLogger.info("Queue Manager is created");
	}

	protected QueueManager createQueueManager() throws BrokerCoreException {
		return new QueueManager(this);
	}

	/**
	 * Initialize the router.
	 * 
	 * @throws BrokerCoreException
	 */
	protected void initRouter() throws BrokerCoreException {
		try {
			router = RouterFactory.createRouter(brokerConfig.matcherName, this);
			router.initialize();
			brokerCoreLogger.info("Router/Matching Engine is initialized");
		} catch (MatcherException e) {
			brokerCoreLogger.error("Router failed to instantiate: " + e);
			exceptionLogger.error("Router failed to instantiate: " + e);
			throw new BrokerCoreException("Router failed to instantiate: " + e);
		}
	}

	/**
	 * Initialize the input queue that is the first place a message enters from communication layer.
	 * It exploits the router to redirect traffic to different other queues.
	 * 
	 * @throws BrokerCoreException
	 */
	protected void initInputQueue() throws BrokerCoreException {
		inputQueue = createInputQueueHandler();
		inputQueue.start();
		registerQueue(inputQueue);
		brokerCoreLogger.debug("InputQueueHandler is starting.");
		try {
			inputQueue.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("InputQueueHandler failed to start: " + e);
			exceptionLogger.error("InputQueueHandler failed to start: " + e);
			throw new BrokerCoreException("InputQueueHandler failed to start", e);
		}
		brokerCoreLogger.info("InputQueueHandler is started.");
	}
	
	protected InputQueueHandler createInputQueueHandler() {
		return new InputQueueHandler(this);
	}
	
	protected Controller createController() {
		return new Controller(this);
	}
	
	protected CommSystem createCommSystem() throws CommunicationException {
		return new CommSystem();
	}


	/**
	 * Initialize the system monitor which collects broker system information. QueueManager and
	 * InputQueue must have been initialized before using this method.
	 * 
	 * @throws BrokerCoreException
	 */
	protected void initSystemMonitor() throws BrokerCoreException {
		systemMonitor = createSystemMonitor();
		brokerCoreLogger.info("System Monitor is created");
		// register the system monitor with queue manager and input queue, so that they can feed
		// data into the monitor
		if (queueManager == null)
			throw new BrokerCoreException(
					"QueueManager must have been initialized before SystemMonitor");
		queueManager.registerSystemMonitor(systemMonitor);
		if (inputQueue == null)
			throw new BrokerCoreException(
					"InputQueue must have been initialized before SystemMonitor");
		inputQueue.registerSystemMonitor(systemMonitor);
		systemMonitor.start();
		brokerCoreLogger.debug("System monitor is starting.");
		try {
			systemMonitor.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("System monitor failed to start: " + e);
			exceptionLogger.error("System monitor failed to start: " + e);
			throw new BrokerCoreException("System monitor failed to start", e);
		}
		brokerCoreLogger.info("System monitor is started.");
	}

	protected SystemMonitor createSystemMonitor() {
		return new SystemMonitor(this);
	}

	protected void initController() throws BrokerCoreException {
		controller = createController();
		controller.start();
		brokerCoreLogger.debug("Controller is starting.");
		try {
			controller.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("Controller failed to start: " + e);
			exceptionLogger.error("Controller failed to start: " + e);
			throw new BrokerCoreException("Controller failed to start", e);
		}
		brokerCoreLogger.info("Controller is started.");
	}

	protected void startMessageRateTimer() {
		ActionListener checkMsgRateTaskPerformer = new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				OverlayRoutingTable ort = getOverlayManager().getORT();
				Map<MessageDestination, LinkInfo> statisticTable = ort.getStatisticTable();
				Map<MessageDestination, OutputQueue> neighbors = ort.getBrokerQueues();
				synchronized (neighbors) {
					for (MessageDestination temp : neighbors.keySet()) {
						if (statisticTable.containsKey(temp)) {
							LinkInfo tempLink = statisticTable.get(temp);
							if (inputQueue.containsDest(temp)) {
								Integer tempI = inputQueue.getNum(temp);
								tempLink.setMsgRate(tempI.intValue());
								inputQueue.setNum(temp, new Integer(0));
							}
						}
					}
				}
			}
		};
		Timer msgRateTimer = new Timer(time_window_interval, checkMsgRateTaskPerformer);
		msgRateTimer.start();
	}

	protected void initTimerThread() throws BrokerCoreException {
		// start the timer thread (for timing heartbeats)
		timerThread = new TimerThread();
		timerThread.start();
		brokerCoreLogger.debug("TimerThread is starting.");
		try {
			timerThread.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("TimerThread failed to start: " + e);
			exceptionLogger.error("TimerThread failed to start: " + e);
			throw new BrokerCoreException("TimerThread failed to start", e);
		}
		brokerCoreLogger.info("TimerThread is started.");
	}

	protected void initHeartBeatPublisher() throws BrokerCoreException {
		// start the heartbeat publisher thread
		heartbeatPublisher = new HeartbeatPublisher(this);
		heartbeatPublisher.setPublishHeartbeats(brokerConfig.isHeartBeat());
		heartbeatPublisher.start();
		brokerCoreLogger.debug("HeartbeatPublisher is starting.");
		try {
			heartbeatPublisher.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("HeartbeatPublisher failed to start: " + e);
			exceptionLogger.error("HeartbeatPublisher failed to start: " + e);
			throw new BrokerCoreException("HeartbeatPublisher failed to start", e);
		}
		brokerCoreLogger.info("HeartbeatPublisher is started.");
	}

	protected void initHeartBeatSubscriber() throws BrokerCoreException {
		// start the heartbeat subscriber thread
		heartbeatSubscriber = createHeartbeatSubscriber();
		heartbeatSubscriber.start();
		brokerCoreLogger.debug("HeartbeatSubscriber is starting.");
		try {
			heartbeatSubscriber.waitUntilStarted();
		} catch (InterruptedException e) {
			brokerCoreLogger.error("HeartbeatSubscriber failed to start: " + e);
			exceptionLogger.error("HeartbeatSubscriber failed to start: " + e);
			throw new BrokerCoreException("HeartbeatSubscriber failed to start", e);
		}
		brokerCoreLogger.info("HeartbeatSubscriber is started.");
	}

	protected HeartbeatSubscriber createHeartbeatSubscriber() {
		return new HeartbeatSubscriber(this);
	}

	protected void initWebInterface() {
		if (brokerConfig.isWebInterface()) {
			// start the management interface web server
			webuiMonitor = new WebUIMonitor(this);
			webuiMonitor.initialize();
			brokerCoreLogger.info("ManagementInterface is started.");
		}
	}

	protected void initNeighborConnections() {
		// connect to initial remote brokers from configuration
		if (brokerConfig.getNeighborURIs().length == 0) {
			brokerCoreLogger.warn("Missing remoteBrokers key or remoteBrokers value in the property file.");
			exceptionLogger.warn("Here is an exception : ", new Exception(
					"Missing remoteBrokers key or remoteBrokers value in the property file."));
		}
		for (String neighborURI : brokerConfig.getNeighborURIs()) {
			// send OVERLAY-CONNECT(s) to controller
			Publication p = MessageFactory.createEmptyPublication();
			p.addPair("class", "BROKER_CONTROL");
			p.addPair("brokerID", getBrokerID());
			p.addPair("command", "OVERLAY-CONNECT");
			p.addPair("broker", neighborURI);
			PublicationMessage pm = new PublicationMessage(p, "initial_connect");
			if (brokerCoreLogger.isDebugEnabled())
				brokerCoreLogger.debug("Broker " + getBrokerID()
						+ " is sending initial connection to broker " + neighborURI);
			queueManager.enQueue(pm, MessageDestination.INPUTQUEUE);
		}
	}

	protected void initManagementInterface() {
		if (brokerConfig.isManagementInterface()) {
			ManagementServer managementServer = new ManagementServer(this);
			managementServer.start();
		}
	}

	protected void initConsoleInterface() {
		if (brokerConfig.isCliInterface()) {
			ConsoleInterface consoleInterface = new ConsoleInterface(this);
			consoleInterface.start();
		}
	}

	/**
	 * @return The configuration of the broker
	 */
	public BrokerConfig getBrokerConfig() {
		return brokerConfig;
	}

	public WebUIMonitor getWebuiMonitor() {
		return webuiMonitor;
	}

	/**
	 * @return The
	 */
	public String getDBPropertiesFile() {
		return brokerConfig.getDbPropertyFileName();
	}

	public String getMIPropertiesFile() {
		return brokerConfig.getManagementPropertyFileName();
	}

	/**
	 * @return The ID of the broker
	 */
	public String getBrokerID() {
		return getBrokerURI();
	}

	/**
	 * @return The MessageDestination for the broker.
	 */
	public MessageDestination getBrokerDestination() {
		return brokerDestination;
	}

	/**
	 * @return
	 */
	public String getBrokerURI() {
		try {
//			return commSystem.getServerURI();
			return NodeAddress.getAddress(brokerConfig.brokerURI).getNodeURI();
		} catch (CommunicationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

	/**
	 * Get a new (globally unique) message ID
	 * 
	 * @return The new message ID
	 */
	public synchronized String getNewMessageID() {
		return getBrokerID() + "-M" + currentMessageID++;
	}

	public MessageListenerInterface getMessageListener() {
		return queueManager;
	}

	/**
	 * Route a Message to a given destination. Errors are handled by the queueManager.
	 * 
	 * @param msg
	 *            The message to send
	 * @param destination
	 *            The destination for the message
	 */
	public void routeMessage(Message msg, MessageDestination destination) {
		queueManager.enQueue(msg, destination);
	}

	/**
	 * Route a Message to its nextHopID. Errors are handled by the queueManager.
	 * 
	 * @param msg
	 *            The message to send
	 */
	public void routeMessage(Message msg) {
		queueManager.enQueue(msg);
	}

	public void registerQueue(QueueHandler queue) {
		MessageQueue msgQueue = queueManager.getMsgQueue(queue.getDestination());
		if (msgQueue == null)
			queueManager.registerQueue(queue.getDestination(), queue.getMsgQueue());
		else
			queue.setMsgQueue(msgQueue);
	}

	public void registerQueue(MessageDestination msgDest, MessageQueue msgQueue) {
		queueManager.registerQueue(msgDest, msgQueue);
	}

	public void removeQueue(MessageDestination dest) {
		queueManager.removeQueue(dest);
	}

	public CommSystem getCommSystem() {
		return commSystem;
	}

	/**
	 * Get the queue for a given destination.
	 * 
	 * @param destination
	 *            The identifier for the desired queue
	 * @return The desired queue, or null if it doesn't exist
	 */
	public MessageQueue getQueue(MessageDestination destination) {
		return queueManager.getQueue(destination);
	}

	/**
	 * Get the advertisements in the broker.
	 * 
	 * @return The set of advertisements in the broker.
	 */
	public Map<String, AdvertisementMessage> getAdvertisements() {
		return router.getAdvertisements();
	}

	/**
	 * Get the subscriptions in the broker.
	 * 
	 * @return The set of subscriptions in the broker.
	 */
	public Map<String, SubscriptionMessage> getSubscriptions() {
		return router.getSubscriptions();
	}

	/**
	 * Retrieve the debug mode of this broker
	 * 
	 * @return Boolean value where true indicates debug mode
	 */
	public boolean getDebugMode() {
		return debug;
	}

	/**
	 * Set the debug mode of this broker
	 * 
	 * @param debugMode
	 *            True to set broker to debug mode, false to turn off debug mode
	 */
	public void setDebugMode(boolean debugMode) {
		debug = debugMode;
	}

	/**
	 * Returns the number of messages in the input queue
	 * 
	 * @return the number of messages in the input queue
	 */
	public int getInputQueueSize() {
		return inputQueue.getInputQueueSize();
	}

	/**
	 * Shuts down this broker along with all services under this broker
	 */
	public void shutdown() {
		
		if(isShutdown)
			return;
		
		isShutdown  = true;
		systemMonitor.shutdownBroker();
		
		// Let's be nice
		try {
//			stop();
			brokerCoreLogger.info("BrokerCore is shutting down.");
//			orderQueuesTo("SHUTDOWN");
			if (commSystem != null)
				commSystem.shutDown();
		} catch (CommunicationException e) {
			e.printStackTrace();
			exceptionLogger.error(e.getMessage());
		}
		
		controller.shutdown();
		inputQueue.shutdown(); 
		timerThread.shutdown(); 
		heartbeatPublisher.shutdown();
		heartbeatSubscriber.shutdown();	
	}

	/**
	 * Stops all broker activity Publishers/Neighbours can still send messages to the brokercore
	 */
	public void stop() {
		// Stop all input/output queues from receiving messages.
		// NOTE: The input queue is never stopped or else there will be no way to start it up again
		// remotely
		try {
			brokerCoreLogger.info("BrokerCore is stopping.");
			orderQueuesTo("STOP");
			running = false;
		} catch (ParseException e) {
			e.printStackTrace();
			exceptionLogger.error(e.getMessage());
		}
	}

	/**
	 * Resumes all broker activity
	 * 
	 */
	public void resume() {
		// Allow messages to be delivered
		try {
			brokerCoreLogger.info("BrokerCore is resuming.");
			orderQueuesTo("RESUME");
			running = true;
		} catch (ParseException e) {
			e.printStackTrace();
			exceptionLogger.error(e.getMessage());
		}
	}

	/*
	 * Send a STOP, RESUME, or SHUTDOWN control message to the LifeCycle, Overlay Managers and System Monitor
	 */
	protected void orderQueuesTo(String command) throws ParseException {
		// Send a control message to the LifeCycle Manager
		Publication lcPub = MessageFactory.createPublicationFromString("[class,BROKER_CONTROL],[brokerID,'" + getBrokerID()
				+ "'],[command,'LIFECYCLE-" + command + "']");
		PublicationMessage lcPubmsg = new PublicationMessage(lcPub, getNewMessageID(),
				getBrokerDestination());
		brokerCoreLogger.debug("Command " + command + " is sending to LifecycleManager.");
		if (queueManager != null)
			queueManager.enQueue(lcPubmsg, MessageDestination.CONTROLLER);

		// Send a control message to the Overlay Manager
		Publication omPub = MessageFactory.createPublicationFromString("[class,BROKER_CONTROL],[brokerID,'" + getBrokerID()
				+ "'],[command,'OVERLAY-" + command + "']");
		PublicationMessage omPubmsg = new PublicationMessage(omPub, getNewMessageID(),
				getBrokerDestination());
		brokerCoreLogger.debug("Command " + command + " is sending to OverlayManager.");
		if (queueManager != null)
			queueManager.enQueue(omPubmsg, MessageDestination.CONTROLLER);
	}

	/**
	 * Indicates whether this broker is running or not
	 * 
	 * @return boolean value, true indicates the broker is running, false means the broker is
	 *         stopped.
	 */
	public boolean isRunning() {
		return running;
	}

	public CycleType getCycleOption() {
		return brokerConfig.getCycleOption();
	}

	public boolean isDynamicCycle() {
		return isDynamicCycle;
	}

	public boolean isCycle() {
		return isCycle;
	}

	public SystemMonitor getSystemMonitor() {
		if (systemMonitor == null) {
			System.err.println("Call to getSystemMonitor() before initializing the system monitor");
		}
		return systemMonitor;
	}

	public Controller getController() {
		return controller;
	}

	public OverlayManager getOverlayManager() {
		return controller == null ? null : controller.getOverlayManager();
	}

	public HeartbeatPublisher getHeartbeatPublisher() {
		return heartbeatPublisher;
	}

	public TimerThread getTimerThread() {
		return timerThread;
	}

	public Router getRouter() {
		return router;
	}

	public InputQueueHandler getInputQueue() {
		return inputQueue;
	}

	public static void main(String[] args) {
		try {
			BrokerCore brokerCore = new BrokerCore(args);
			brokerCore.initialize();
//			brokerCore.shutdown();
		} catch (Exception e) {
			// log the error the system error log file and exit
			Logger sysErrLogger = Logger.getLogger("SystemError");
			if (sysErrLogger != null)
				sysErrLogger.fatal(e.getMessage() + ": " + e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	public boolean isShutdown() {
		return isShutdown;
	}

}

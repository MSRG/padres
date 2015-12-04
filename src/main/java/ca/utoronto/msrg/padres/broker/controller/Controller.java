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
 * Created on 21-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.broker.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.ShutdownMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * The controller for the broker. The broker is started through this class. All broker control
 * functions occur from this class.
 * 
 * @author eli
 */
public class Controller extends Thread {

	protected boolean started = false;

	protected Object started_lock = new Object();

	protected BrokerCore brokerCore;

	protected MessageQueue inQueue; // reference to controller message queue

	// map of managers, indexed by uppercase name
	protected Map<String, Manager> managers;

	// for testing, store the current processed publication msg
	protected PublicationMessage currentPubMsg = null;

	protected static Logger controllerLogger = Logger.getLogger(Controller.class);

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	protected static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Instantiate the Controller thread (named "Controller")
	 */
	public Controller(BrokerCore broker) {
		super("Controller");
		brokerCore = broker;
		managers = new HashMap<String, Manager>();
		// register controller with broker core
		inQueue = createMessageQueue();
		brokerCore.registerQueue(MessageDestination.CONTROLLER, inQueue);
	}

	protected MessageQueue createMessageQueue() {
		return new MessageQueue();
	}

	/**
	 * Register Controller with BrokerCore, instantiate managers and enter listening loop
	 */
	public void run() {
		synchronized (started_lock) {
			// instantiate managers
			addManager(LifeCycleManager.MANAGER_NAME, createLifeCycleManager(brokerCore));
			try {
				addManager(OverlayManager.MANAGER_NAME, createOverlayManager());
			} catch (BrokerCoreException e) {
				exceptionLogger.fatal(e.getMessage());
				e.printStackTrace();
				System.exit(1);
			}
			addManager(ServerInjectionManager.MANAGER_NAME, new ServerInjectionManager(brokerCore));

			// subscribe to controller events
			Subscription controllerSub;
			try {
				controllerSub = MessageFactory.createSubscriptionFromString(
						"[class,eq,'BROKER_CONTROL'],[brokerID,eq,'" + brokerCore.getBrokerID() + "']");
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
				return;
			}
			SubscriptionMessage controllerSubMsg = new SubscriptionMessage(controllerSub,
					brokerCore.getNewMessageID(), MessageDestination.CONTROLLER);
			if (controllerLogger.isDebugEnabled())
				controllerLogger.debug("Controller is sending controllerSubMsg : "
						+ controllerSubMsg.getSubscription().toString());
			brokerCore.routeMessage(controllerSubMsg, MessageDestination.INPUTQUEUE);

			controllerLogger.info("Controller is fully started");
			// wake up threads waiting for Controller to start
			started = true;
			started_lock.notifyAll();
		}

		listen();
	}

	protected Manager createLifeCycleManager(BrokerCore brokerCore) {
		LifeCycleManager lcManager = new LifeCycleManager(brokerCore);
		lcManager.init();
		return lcManager;
	}

	/**
	 * Block until the Controller is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}

	private void addManager(String managerName, Manager manager) {
		controllerLogger.debug("Starting " + managerName + " Manager.");
		managers.put(managerName, manager);
	}

	public LifeCycleManager getLifeCycleManager() {
		return (LifeCycleManager) managers.get(LifeCycleManager.MANAGER_NAME);
	}

	public OverlayManager getOverlayManager() {
		return (OverlayManager) managers.get(OverlayManager.MANAGER_NAME);
	}

	/*
	 * For testing
	 */
	public PublicationMessage getCurrentPubMsg() {
		return currentPubMsg;
	}

	/**
	 * Listen to input queue and dispatch messages to managers
	 */
	private void listen() {
		while (started) {
			Message msg = (Message) inQueue.blockingRemove();
			
			if(msg.getType().equals(MessageType.SHUTDOWN)){
				continue;
			}
			
			controllerLogger.debug("Controller receives message : " + msg.toString());
			if (msg.getClass() == PublicationMessage.class) {
				currentPubMsg = null;
				currentPubMsg = (PublicationMessage) msg;
				Publication pub = ((PublicationMessage) msg).getPublication();
				Map<String, Serializable> pairMap = pub.getPairMap();
				Serializable payload = pub.getPayload();
				if (pub.getClassVal().equalsIgnoreCase("BROKER_CONTROL")) {
					String command = (String) pairMap.get("command");
					// handle command
					String managerName = command.substring(0, command.indexOf('-'));
					// find appropriate Manager
					controllerLogger.debug("This message will be sent to manager " + managerName
							+ ".");
					Manager manager = managers.get(managerName.toUpperCase());
					if (manager != null) {
						manager.handleCommand(pairMap, payload);
					} else {
						controllerLogger.warn("Invalid manager for Controller.");
						exceptionLogger.warn("Here is an exception : ", new Exception(
								"Invalid manager for Controller."));
					}
				} else {
					controllerLogger.warn("Invalid message for Controller.");
					exceptionLogger.warn("Here is an exception : ", new Exception(
							"Invalid message for Controller."));
				}
			} else {
				controllerLogger.warn("Non-publication message is sent to Controller.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Non-publication message is sent to Controller."));
			}
			messagePathLogger.debug("Controller receives message : " + msg.toString());
		}
	}

	protected OverlayManager createOverlayManager() throws BrokerCoreException {
		return new OverlayManager(brokerCore);
	}
	
	public void shutdown() {
		started = false;
		inQueue.addFirst(new ShutdownMessage());
		
		for(Manager manager : managers.values()){
			manager.shutdown();
		}
	}
}

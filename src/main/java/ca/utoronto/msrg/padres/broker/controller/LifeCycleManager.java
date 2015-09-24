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
import ca.utoronto.msrg.padres.broker.controller.db.DBHandler;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;

/**
 * The LifeCycleManager is responsible for starting and stopping components of the broker.
 * 
 * @author eli
 */
public class LifeCycleManager implements Manager {

	public static final String MANAGER_NAME = "LIFECYCLE";

	BrokerCore brokerCore = null;

	// This is a map in case we want to be able to selectively control a handler
	Map<String, QueueHandler> handlers = null;

	static Logger lifeCycleLogger = Logger.getLogger(LifeCycleManager.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	private boolean running = false;

	public LifeCycleManager(BrokerCore broker) {
		brokerCore = broker;
		handlers = new HashMap<String, QueueHandler>();
		running = true;
	}
	
	public void init() {
		String[] whichHandlers = brokerCore.getBrokerConfig().getManagers();
		for (String managerName : whichHandlers) {
			if (managerName.equals("DB")) {
				// instantiate DBHandler
				lifeCycleLogger.debug("Starting DBHandler.");
				DBHandler dbHandler = createDBHandler(brokerCore.getBrokerID() + "-DB", brokerCore);
				dbHandler.init();
				handlers.put("DB", dbHandler);
				lifeCycleLogger.debug("DBHandler is started.");
			} else {
				lifeCycleLogger.warn("Unrecognized handler for LifeCycleManager.");
				exceptionLogger.warn(new Exception("Unrecognized handler for LifeCycleManager."));
			}
		}
		// Start all handlers
		startHandlers();
	}

	protected DBHandler createDBHandler(String databaseID, BrokerCore broker) {
		DBHandler dbHandler = new DBHandler(databaseID, broker);
		return dbHandler;
	}

	/**
	 * Iterate through handlers and start them all.
	 * 
	 * startHandlers() doesn't return until all handlers are started.
	 */
	private void startHandlers() {
		for (QueueHandler handler : handlers.values()) {
			handler.start();
			lifeCycleLogger.debug("Starting " + handler.getName() + ".");
			try {
				handler.waitUntilStarted();
			} catch (InterruptedException e) {
				lifeCycleLogger.error(handler.getName() + " failed to start : " + e);
				exceptionLogger.error(handler.getName() + " failed to start : " + e);
			}
			this.brokerCore.registerQueue(handler);
			lifeCycleLogger.debug(handler.getName() + " is fully started.");
		}
	}

	public void handleCommand(Map<String, Serializable> pairMap, Serializable payload) {
		String command = (String) pairMap.get("command");
		command = command.substring(command.indexOf('-') + 1);
		lifeCycleLogger.debug("LifeCycleManager receives command : " + command + ".");
		if (command.equalsIgnoreCase("STOP")) {
			stopHandlers();
		} else if (command.equalsIgnoreCase("RESUME")) {
			resumeHandlers();
		} else if (command.equalsIgnoreCase("SHUTDOWN")) {
			shutdown();
		} else {
			// add by shuang, for handling unrecognized command for lifeCycle
			// manager
			lifeCycleLogger.warn("Invalid command for lifeCycleManager.");
			exceptionLogger.warn(new Exception("Invalid command for lifeCycleManager."));
		}
	}

	/*
	 * Stops the transport handler and all handlers
	 */
	protected void stopHandlers() {
		lifeCycleLogger.info("Handlers are stopping.");
		for (QueueHandler handler : handlers.values()) {
			lifeCycleLogger.debug(handler.getName() + " is stopping handler.");
			handler.stopOperation();
		}
	}

	/*
	 * Resumes the transport handler and all handlers
	 */
	protected void resumeHandlers() {
		lifeCycleLogger.info("Handlers are resuming.");
		for (QueueHandler handler : handlers.values()) {
			lifeCycleLogger.debug(handler.getName() + " is resuming handler.");
			handler.resumeOperation();
		}
	}

	/*
	 * Shuts down the transport handler and all handlers
	 */
	protected void shutdownHandlers() {	
		lifeCycleLogger.info("Handlers are shutting down.");
		for (QueueHandler handler : handlers.values()) {
			lifeCycleLogger.debug(handler.getName() + " is shutting down handler.");
			handler.shutdown();
		}
		handlers.clear();
	}

	
	
	public DBHandler getDBHandler() {
		return (DBHandler) handlers.get("DB");
	}

	@Override
	public void shutdown() {
		if(!running)
			return;
		else
			running = false;
		
		shutdownHandlers();
		if (brokerCore != null)
			lifeCycleLogger.info("Broker " + brokerCore.getBrokerID() + " is shut down.");
		// Clean up
		brokerCore = null;
		handlers = null;
	}
}

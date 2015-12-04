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
 * Created on Oct 29, 2003
 *
 */
package ca.utoronto.msrg.padres.broker.monitor;

/**
 * @author Alex Cheung
 *
 * This guy periodically gets all broker information from the system 
 * monitor, then publishes them.
 */

// Padres
import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class BrokerInfoPublisher extends Thread {

	public static final String BROKER_INFO_ON = "ON";

	public static final String BROKER_INFO_OFF = "OFF";

	public static final String PROP_BROKER_INFO = "padres.monitor.brokerinfo";

	// Constants
	private static final long DEFAULT_PUBLICATION_INTERVAL = 20000;

	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	private BrokerCore brokerCore;

	private SystemMonitor systemMonitor;

	private MasterSlaveLock publishingLock;

	private long publicationInterval = DEFAULT_PUBLICATION_INTERVAL;

	private volatile boolean running = true;

	private final boolean publishBrokerInfo;

	public BrokerInfoPublisher(BrokerCore broker) {
		super("BrokerInfoPublisher");
		brokerCore = broker;
		systemMonitor = broker.getSystemMonitor();
		publishingLock = new MasterSlaveLock();
		publishBrokerInfo = broker.getBrokerConfig().isBrokerInfoOn();
	}

	public void run() {
		// ...and publish broker information
		// We have the SystemMonitor doing this for us due to the adv problem
		sendAdvertisementMessage();

		while (isRunning()) {
			if (!publishBrokerInfo)
				stopPublishing();

			// Stop when the brokercore is stopped
			publishingLock.waitForLock();
			
			if(!isRunning())
				break;

			try {
				Thread.sleep(getPublicationInterval());
			} catch (Exception e) {
				/*
				 * This is normal. It happens when this thread is being interrupted out of its
				 * sleep. See SystemMonitor for details.
				 */
			}
			publishBrokerInfo();
		}

		cleanUp();
	}

	public void publishBrokerInfo() {
		try {
			brokerCore.routeMessage(systemMonitor.getBrokerInfoInPublicationMessage(),
					MessageDestination.INPUTQUEUE);
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
		}
	}

	/*
	 * Sends out an advertisement for the BROKER_INFO publication messages that it will publish in
	 * the future
	 */
	private void sendAdvertisementMessage() {
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

		brokerCore.routeMessage(advMessage, MessageDestination.INPUTQUEUE);
	}

	/**
	 * Returns the time between broker info publications
	 * 
	 * @return the time between broker info publications
	 */
	public synchronized long getPublicationInterval() {
		return publicationInterval;
	}

	/**
	 * Set the time between broker info publications
	 * 
	 * @param interval
	 *            time between broker info publications
	 */
	public synchronized void setPublicationInterval(long interval) {
		publicationInterval = interval;
	}

	/**
	 * Stops the broker info publisher from publishing BROKER_INFO messages
	 */
	public void stopPublishing() {
		if(publishingLock == null)
			return;
		publishingLock.setLock();
	}

	/**
	 * Resumes publication of BROKER_INFO messages
	 */
	public void resumePublishing() {
		if(publishingLock == null)
			return;
		publishingLock.releaseLock();
	}

	/**
	 * Shuts down this publisher
	 * 
	 */
	public synchronized void shutdown() {
		running = false;
		resumePublishing();
	}

	/**
	 * Test to see if we can shutdown yet
	 * 
	 * @return boolean - true means continue to run, false means to stop and exit
	 */
	private synchronized boolean isRunning() {
		return running;
	}

	/*
	 * Clean up our mess when we shutdown
	 */
	private void cleanUp() {
		brokerCore = null;
		systemMonitor = null;
		publishingLock = null;
	}

}

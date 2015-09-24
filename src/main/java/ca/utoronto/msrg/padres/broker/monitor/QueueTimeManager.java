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
 * Created on Nov 11, 2003
 */
package ca.utoronto.msrg.padres.broker.monitor;

/**
 * @author Alex Cheung
 *
 * Manages the enqueue and dequeue time of messages
 * This class fixes the synchronization problem of getting the queuing time 
 * of a message before the message's enqueue time is recorded
 */

import java.util.HashMap;
import java.util.Date;

public class QueueTimeManager {

	private HashMap<String, Date> timeOfEnqueue;

	private HashMap<String, Date> timeOfDequeue;

	// For synchronization purposes only
	private final Object enqueueMutex = new Object();

	private final Object dequeueMutex = new Object();

	public QueueTimeManager() {
		timeOfEnqueue = new HashMap<String, Date>();
		timeOfDequeue = new HashMap<String, Date>();
	}

	public void setEnqueueTime(String messageID) {
		synchronized (enqueueMutex) {
			timeOfEnqueue.put(messageID, new Date());
			enqueueMutex.notify();
		}
	}

	public void setDequeueTime(String messageID) {
		synchronized (dequeueMutex) {
			timeOfDequeue.put(messageID, new Date());
			dequeueMutex.notify();
		}
	}

	public long removeEnqueueTime(String messageID) {
		synchronized (enqueueMutex) {
			while (!timeOfEnqueue.containsKey(messageID)) {
				try {
					enqueueMutex.wait();
				} catch (Exception e) {
				}
			}

			return timeOfEnqueue.remove(messageID).getTime();
		}
	}

	public long removeDequeueTime(String messageID) {
		synchronized (dequeueMutex) {
			while (!timeOfDequeue.containsKey(messageID)) {
				try {
					dequeueMutex.wait();
				} catch (Exception e) {
				}
			}

			return timeOfDequeue.remove(messageID).getTime();
		}
	}
}

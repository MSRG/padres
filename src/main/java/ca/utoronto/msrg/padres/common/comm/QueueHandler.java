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
 *
 */
package ca.utoronto.msrg.padres.common.comm;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.ShutdownMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;

/**
 * QueueHandler is a generic class for a thread which performs blocking reads on a synchronized
 * queue, handling messages from the queue with an abstract processMessage() method. Basically it
 * contains a MessageQueue and a MessageDestination.
 * 
 * @author eli
 */
public abstract class QueueHandler extends Thread {

	protected final long SLEEP_TIME = 1000;

	protected boolean started = false;

	protected Object started_lock = new Object();

	protected volatile boolean shutdown;

	protected volatile boolean stopped;

	protected MessageQueue msgQueue; // reference to input queue

	protected MessageDestination myDestination = null;

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Constructor. It accepts a name for the thread and destination type, creates a
	 * {@link MessageDestination} and then build a QueueHandler with
	 * {@link QueueHandler#QueueHandler(MessageDestination)} constructor.
	 * 
	 * @param threadName
	 *            The name of this Thread.
	 * @param queueDestinationType
	 *            Destination type of the application creating the queue
	 */
	public QueueHandler(String threadName, DestinationType queueDestinationType) {
		this(new MessageDestination(threadName, queueDestinationType));
	}

	/**
	 * Constructor. Build a Thread named dest with "-QueueHandler" suffix to handle queue assigned
	 * to MessageDestination dest.
	 * 
	 * @param threadName
	 *            The name of this Thread.
	 * @param dest
	 *            The destination for this thread to handle.
	 */
	public QueueHandler(MessageDestination dest) {
		super(dest + "-QueueHandler");
		shutdown = false;
		stopped = false;
		myDestination = dest;
		msgQueue = createMessageQueue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public synchronized final void run() {
		prelude();
		while (!shutdown) {
			Message msg = msgQueue.blockingRemove();
			
			// Stop message delivery when the broker stops; this line must appear before
			// processMessage()	
			stopIfStopped();
						
			if(msg.getType().equals(MessageType.SHUTDOWN))
				continue;
			
			processMessage(msg);
		}
		cleanUp();
	}

	public void addMessage(Message msg) {
		msgQueue.add(msg);
	}

	/**
	 * Block until the QueueHandler is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}

	/**
	 * Wake up every SLEEP_TIME (milliseconds) to check whether we are still stopped. Since the
	 * broker isn't doing anything when it is stopped, polling every one second ain't gonna hurt
	 */
	private void stopIfStopped() {
		while (stopped && !shutdown) {
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Shutdown the handler thread.
	 */
	public void shutdown() {
		if(shutdown)
			return;
		
		shutdown = true;
		stopped = true; // shutdown-able even when stopped
		
		msgQueue.addFirst(new ShutdownMessage());
	}

	public final void stopOperation() {
		stopped = true;
	}

	public final void resumeOperation() {
		stopped = false;
	}

	public boolean isStopped() {
		return stopped;
	}

	/**
	 * Get the MessageDestination for the MessageQueue handled by this thread.
	 * 
	 * @return The MessageDestination for the MessageQueue handled by this thread.
	 */
	public MessageDestination getDestination() {
		return myDestination;
	}

	/**
	 * To assign a message queue to the queue handler.
	 * 
	 * @param msgQueue
	 *            The new message queue for the queue handler
	 */
	public void setMsgQueue(MessageQueue msgQueue) {
		this.msgQueue = msgQueue;
	}

	public MessageQueue getMsgQueue() {
		return msgQueue;
	}

	/**
	 * Returns the number of messages in the input queue
	 * 
	 * @return the number of messages in the input queue
	 */
	public int getInputQueueSize() {
		return msgQueue.size();
	}

	/**
	 * Clean up thread. This method can be overridden if cleanup is necessary for a particular
	 * handler.
	 */
	protected void cleanUp() {
	}

	/**
	 * Handle an incoming message from the queue.
	 * 
	 * @param msg
	 *            The incoming message
	 */
	public abstract void processMessage(Message msg);

	protected void prelude() {
		synchronized (started_lock) {
			// wake up threads waiting for QueueHandler to start
			started = true;
			started_lock.notifyAll();
		}
	}
	
	protected MessageQueue createMessageQueue() {
		return new MessageQueue();
	}
}

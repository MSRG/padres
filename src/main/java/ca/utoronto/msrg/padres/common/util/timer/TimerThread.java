package ca.utoronto.msrg.padres.common.util.timer;

/**
 * Singleton timer thread that handles all timer event requests in the vm. TimerClients use this
 * class by setting timers that are later returned to the requesting TimerClient's queue when they
 * have expired.
 */
public class TimerThread extends Thread {
	/** Time to sleep in periods of inactivity. Should be large. */
	protected static final long EMPTY_QUEUE_SLEEP_TIME = 60000;

	/** Queue of pending timer events sorted by time remaining */
	protected TimerEvent m_TimerEventQ = null;

	/** Queue of requested timers that haven't been added to the pending queue yet. */
	protected TimerEvent m_TimerRequestQ = null;

	/** Used to return handles when timers are set. */
	protected int m_CurrHandle = 1;

	// add by shuang, for waitUntilStarted
	protected boolean started = false;

	protected Object started_lock = new Object();

	/**
	 * Private constructor to create the singleton instance of this class.
	 */
	public TimerThread() {
		super("PADRES Timer Thread");

		// TODO: this is temporary, it doesn't matter what the handle is, this
		// is simply for ease of testing.
		m_CurrHandle = (new java.util.Random(System.currentTimeMillis())).nextInt();
	}

	/**
	 * Process timer events. The timer thread simply sleeps when it has nothing to do. If there are
	 * any pending timers the timer thread sleeps for the amount of time equal to the shortest
	 * remaining timer, then it wakes up and delivers the expired timer(s) to the appropriate
	 * clients. If a new timer is set while this thread is sleeping it promptly wakes up and sets
	 * the new timer to pending and adds it to the timer queue.
	 */
	public void run() {
		synchronized (started_lock) {
			// wake up threads waiting for TimerThread to start
			started = true;
			started_lock.notifyAll();
		}

		while (started) {
			long sleepStart = System.currentTimeMillis();

			// figure out how long to sleep
			long sleepTime = 0;
			if (m_TimerEventQ == null)
				sleepTime = EMPTY_QUEUE_SLEEP_TIME;
			else
				sleepTime = m_TimerEventQ.m_Delay;

			// go to sleep
			try {
				sleep(sleepTime);
			} catch (InterruptedException ignored) {
			}

			if(!started)
				break;
			
			// how long was i sleeping?
			long sleepDelta = System.currentTimeMillis() - sleepStart;
			if (sleepDelta < 0)
				sleepDelta = 0; // can happen if user set clock back while i was sleeping

			// check if any timers have expired
			checkForExpiredTimers(sleepDelta);

			// add new requests to the timer event queue
			handleNewRequests();
		}
	}

	/**
	 * Timer clients call this method to set a timer.
	 * 
	 * @param event
	 *            The timer event
	 * @return The timer handle
	 */
	public synchronized int setTimer(TimerEvent event) {
		event.m_Status = TimerEvent.TIMER_STATUS_PENDING;
		event.m_TimerHandle = m_CurrHandle++;
		if (event.m_Delay < 0)
			event.m_Delay = 0;

		// add this timer event to the request queue
		if (null == m_TimerRequestQ) {
			m_TimerRequestQ = event;
			event.m_Next = null;
		} else {
			TimerEvent current = m_TimerRequestQ;
			while (current.m_Next != null)
				current = current.m_Next;
			current.m_Next = event;
			event.m_Next = null;
		}

		// interrupt the timer thread so it will enqueue this timer
		interrupt();

		return event.m_TimerHandle;
	}

	/**
	 * Check for expired timers that need to be returned to their clients. Also clean up any
	 * cancelled timers.
	 * 
	 * @param delta
	 *            Elapsed milliseconds since i checked last
	 */
	private synchronized void checkForExpiredTimers(long delta) {
		TimerEvent thisEvent = m_TimerEventQ;
		TimerEvent nextEvent = null;

		while (thisEvent != null) {
			thisEvent.m_Delay -= delta;
			nextEvent = thisEvent.m_Next;
			if (thisEvent.m_Delay <= 0) {
				// the event has expired
				m_TimerEventQ = nextEvent;

				// only return timers that went from pending to expired
				if (thisEvent.m_Status == TimerEvent.TIMER_STATUS_PENDING) {
					thisEvent.m_Status = TimerEvent.TIMER_STATUS_EXPIRED;
					thisEvent.m_Client.returnToClientQ(thisEvent);
				}
			}
			thisEvent = nextEvent;
		}
	}

	/**
	 * Put all timer events on the request queue into the timer event queue.
	 */
	private synchronized void handleNewRequests() {
		while (m_TimerRequestQ != null) {
			TimerEvent event = m_TimerRequestQ;
			m_TimerRequestQ = m_TimerRequestQ.m_Next;
			event.m_Next = null;

			enqueueNewRequest(event);
		}
	}

	/**
	 * Enqueue a timer event to the timer event queue.
	 * 
	 * @param event
	 *            The timer event
	 */
	private void enqueueNewRequest(TimerEvent event) {
		// stick the new event at the correct position based on its delay
		if (m_TimerEventQ == null) {
			m_TimerEventQ = event;
			event.m_Next = null;
		} else {
			if (m_TimerEventQ.m_Delay > event.m_Delay) {
				// new event is first in the queue
				event.m_Next = m_TimerEventQ;
				m_TimerEventQ = event;
			} else {
				// event goes somewhere in the middle (or at the end)
				TimerEvent current = m_TimerEventQ;
				while (current != null) {
					if (null == current.m_Next) {
						// we're at the end, append the new event
						current.m_Next = event;
						event.m_Next = null;
						break;
					} else if (current.m_Next.m_Delay > event.m_Delay) {
						event.m_Next = current.m_Next;
						current.m_Next = event;
						break;
					}
					current = current.m_Next;
				}
			}
		}
	}

	/**
	 * Cancel the timer with the given handle. This simply sets its status to cancelled.
	 * 
	 * @param handle
	 *            Handle of timer to cancel
	 */
	public synchronized TimerEvent cancelTimer(int handle) {
		TimerEvent event = m_TimerEventQ;

		while (event != null) {
			if (event.m_TimerHandle == handle) {
				event.m_Status = TimerEvent.TIMER_STATUS_CANCELLED;
				// TODO: should i remove it from the queue too?
				return event;
			}
			event = event.getNext();
		}

		event = m_TimerRequestQ;

		while (event != null) {
			if (event.m_TimerHandle == handle) {
				event.m_Status = TimerEvent.TIMER_STATUS_CANCELLED;
				// TODO: should i remove it from the queue too?
				return event;
			}
			event = event.getNext();
		}
		// no matching event found
		return null;
	}

	/**
	 * Block until the TimerThread is started.
	 */
	public void waitUntilStarted() throws InterruptedException {
		synchronized (started_lock) {
			while (started == false) {
				started_lock.wait();
			}
		}
	}

	public void shutdown() {
		started = false;
		this.interrupt();
	}
}

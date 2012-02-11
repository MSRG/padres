/*
 * Created on Nov 15, 2003
 *
 */
package ca.utoronto.msrg.padres.broker.monitor;

/**
 * @author Alex Cheung
 *
 * This is almost like a producer/consumer lock except that both entities do not
 * have equal status.  You should use this class if you have a master who
 * wants to tell a slave when it should stop and start working.  Do not use
 * this class if your slave is a bottleneck in your system since the slave
 * has to acquire a mutex lock everytime it calls the synchronized method 
 * waitForLock()  
 */

public class MasterSlaveLock {

	// Don't make this static!  We want other to be able to use this class too
	// and not mess up with their (and our) synchronization
	private boolean lock = false;

	/**
	 * Constructor, nice and simple :)
	 */
	public MasterSlaveLock() {
	}
	
	
	/**
	 * This function is used only by the master to make the slave stop 
	 * working and wait till the master invokes the releaseLock() function
	 */
	public synchronized void setLock() {
		if (lock == false)
			lock = true;
	}

	
	/**
	 * This function is used only by the slave to know when to wait and
	 * stop working.  It will work again once the master invokes the
	 * releaseLock() function 
	 */
	public synchronized void waitForLock() {
		while (lock == true) {
			try {
				wait();
			} catch (Exception e) {
			}
		}

	}

	
	/**
	 * This function is used only by the master to wake up the slave
	 */
	public synchronized void releaseLock() {
		if (lock == true) {
			lock = false;
			notify();
		}
	}

}

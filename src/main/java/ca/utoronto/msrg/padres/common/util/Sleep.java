package ca.utoronto.msrg.padres.common.util;

/**
 * Just a helper class with some sleep related functions.  For instance,
 * the sleep function allows you to sleep without typing all that try/catch
 * mess.
 * 
 * @author cheung
 *
 */
import java.util.Calendar;


public class Sleep {
	
	private static final int MINUTES_PER_HOUR = 60;
	private static final int SECONDS_PER_MINUTE = 60;
	private static final int MILLISECONDS_PER_SECOND = 1000;
	
	
	/**
	 * Sleep for given amount of time in milliseconds as long
	 * just like Thread.sleep)
	 * 
	 * @param timeInSeconds
	 * @return true if successful
	 */
	public static void sleep(long timeInMS) {
		try {
			Thread.sleep(timeInMS);
		} catch (Exception e) {
			// rarely fails :P
		}
	}
	
	/**
	 * Sleep for given amount of time in seconds as double
	 * @param timeInSeconds
	 * @return true if successful
	 */
	public static void sleep(double timeInSeconds) {
		sleep(Math.round(timeInSeconds * MILLISECONDS_PER_SECOND));
	}
	
	
	/**
	 * Useful when you want to sleep until the start of the next period.
	 * For example, if your period is 15s, then this function will wait until
	 * the current time is 0, 15, 30, 45 seconds into the minute.  Or, you can
	 * give a time greater than a minute such as 900 (for 15 minute period) and
	 * this function will wait until the current time 0, 15, 30, 45 minutes into
	 * the hour.  
	 * 
	 * This method was developed to synchronize the logging of different entities
	 * on different machines (assuming the machines have loosely synchronized times)
	 *  
	 * @param periodInSeconds
	 */
	public static void sleepTillStartOfNextPeriod(int periodInSeconds) {
		Calendar date = null;
		long timeInMilliseconds = -1;
		
		// Handle minute-scale input
		// Check if we can divide the period equally in one hour span
		if (periodInSeconds > SECONDS_PER_MINUTE &&
				(SECONDS_PER_MINUTE*MINUTES_PER_HOUR) % periodInSeconds == 0) {
			date = Calendar.getInstance();
			timeInMilliseconds = 
				date.get(Calendar.MINUTE) * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND
				+ date.get(Calendar.SECOND) * MILLISECONDS_PER_SECOND
				+ date.get(Calendar.MILLISECOND);
		} else if (periodInSeconds <= SECONDS_PER_MINUTE &&
				SECONDS_PER_MINUTE % periodInSeconds == 0) {
			date = Calendar.getInstance();
			timeInMilliseconds = date.get(Calendar.SECOND) * MILLISECONDS_PER_SECOND
				+ date.get(Calendar.MILLISECOND);
		}
		
		// Find out how much longer to wait till next starting period
		if (timeInMilliseconds != -1) {
			// Wait as least as possible to next logging time slot
			while (timeInMilliseconds > 0) {
				timeInMilliseconds -= (periodInSeconds * MILLISECONDS_PER_SECOND);
			} 
			
			// We now have the time to wait.  Just need to make it positive again
			timeInMilliseconds = Math.abs(timeInMilliseconds);
			
			sleep(timeInMilliseconds);

 			// Uncomment the following for debugging
/*
			Calendar after = Calendar.getInstance();
			System.out.println(date.get(Calendar.MINUTE) + ":" + date.get(Calendar.SECOND)
					+ "." + date.get(Calendar.MILLISECOND));
			System.out.println("Slept for " + timeInMilliseconds / 1000.0);
			System.out.println(after.get(Calendar.MINUTE) + ":" + after.get(Calendar.SECOND)
					+ "." + after.get(Calendar.MILLISECOND));
*/
		}
	}
}

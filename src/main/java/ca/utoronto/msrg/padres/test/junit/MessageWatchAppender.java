package ca.utoronto.msrg.padres.test.junit;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import ca.utoronto.msrg.padres.common.util.Utils;

public class MessageWatchAppender extends AppenderSkeleton {

	private static final int TIMEOUT_TIME = 10; // seconds

	private BlockingQueue<String> messagePathMessages;

	public MessageWatchAppender() {
		super();
		this.layout = new PatternLayout("[%t] %-5p %l %m");
		messagePathMessages = new LinkedBlockingDeque<String>();
	}

	public void clear() {
		messagePathMessages.clear();
	}

	public String getMessage() {
		return getMessage(TIMEOUT_TIME);
	}

	public String getMessage(int timeoutInSeconds) {
		try {
			return messagePathMessages.poll(timeoutInSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean[] checkMessages(String origin, String... messageRegEx) {
		boolean[] msgCheck = new boolean[messageRegEx.length];
		for (String msg : messagePathMessages) {
			for (int i = 0; i < messageRegEx.length; i++) {
				if (msg.matches(messageRegEx[i])) {
					msgCheck[i] = true;
					break;
				}
			}
			if (Utils.checkAllTrue(msgCheck)) {
				break;
			}
		}
		return msgCheck;
	}

	@Override
	protected synchronized void append(LoggingEvent event) {
		synchronized (this) {
			String message = this.layout.format(event);
			if (layout.ignoresThrowable()) {
				String[] throwableMessages = event.getThrowableStrRep();
				if (throwableMessages != null && throwableMessages.length > 0)
					message += throwableMessages[0] + "\n";
			}
			messagePathMessages.add(message);
			this.notifyAll();
		}
	}

	@Override
	public void close() {
		// nothing to close
		messagePathMessages.clear();
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	public void printMessages() {
		System.out.println("error messages:");
		for (String msg : messagePathMessages) {
			System.out.println(msg);
		}
	}
}

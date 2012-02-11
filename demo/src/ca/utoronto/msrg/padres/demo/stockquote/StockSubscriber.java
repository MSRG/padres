package ca.utoronto.msrg.padres.demo.stockquote;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.swing.Timer;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

// import loadbalancing.mediator.Mediator;

/**
 * A subscriber that subscribes to stock quotes and logs delivery delay to file. Logging is synced
 * at the beginning of each second (almost in fact).
 * 
 * @author cheung
 * 
 */
public class StockSubscriber extends StockClient {

	// Constants
	private final static short MAX_DUPLICATE_HISTORY_SIZE = 16;

	private final static String DEFAULT_SUBSCRIBER_ID = "Sub-" + DEFAULT_CLIENT_ID;

	private final static long DELAY_BETWEEN_NEW_SUBSCRIPTION = 1000; // ms

	// Command-line argument-related constants
	private static final String CMD_ARG_SUBSCRIPTION = "s";

	public static final String SUB_DELIMIT_CHAR = "/";

	// For duplicate detection Accounting
	private final LinkedHashMap<Publication, Object> messageHistory;

	// Synchronization/mutex/locks
	private final Object avgDelaySync = new Object();

	private final Object logSync = new Object();

	// Holds this subscriber's subscription
	private Map<String, Subscription> subId2SubMap;

	// Logging
	private final Timer loggingTimer;

	private int duplicates;

	// Performance Measurements
	private double minDelay;

	private double maxDelay;

	private long received;

	private double cumulativeDelay;
	
	private String brokerId;

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param strSubscription
	 * @throws ClientException
	 * @throws RemoteException
	 */
	public StockSubscriber(ClientConfig clientConfig, String strSubscription)
			throws ClientException {
		super(clientConfig);
		brokerId = clientConfig.connectBrokerList[0];
		// send subscriptions
		sendSubscriptions(strSubscription);
		// duplicate detection stuff
		duplicates = 0;
		messageHistory = new LinkedHashMap<Publication, Object>(MAX_DUPLICATE_HISTORY_SIZE,
				(float) 0.75, true) {

			private static final long serialVersionUID = 1L;

			// We want to limit the history size to MAX_HISTORY_SIZE
			protected boolean removeEldestEntry(Map.Entry<Publication, Object> eldest) {
				return size() > MAX_DUPLICATE_HISTORY_SIZE;
			}
		};
		// performance metrics
		minDelay = Double.MAX_VALUE;
		maxDelay = Double.MIN_VALUE;
		received = 0;
		cumulativeDelay = 0;
		// For logging
		loggingTimer = setupLoggingTimer();
	}

	public double getMinDelay() {
		return minDelay;
	}

	public void setMinDelay(double value) {
		minDelay = value;
	}

	public double getMaxDelay() {
		return maxDelay;
	}

	public void setMaxDelay(double value) {
		maxDelay = value;
	}

	private boolean sendSubscriptions(String strSubscription) throws ClientException {
		// Parse the subscription string
		String[] subStrList = strSubscription.split(SUB_DELIMIT_CHAR);
		List<Subscription> subList = new ArrayList<Subscription>();
		for (String subStr : subStrList) {
			Subscription sub;
			try {
				sub = MessageFactory.createSubscriptionFromString(subStr);
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
				throw new ClientException(e);
			}
			subList.add(sub);
		}
		// subscribe to all the brokers with all the given subscriptions
		subId2SubMap = new HashMap<String, Subscription>();
		for (String brokerURI : getBrokerURIList())
			for (Subscription sub : subList) {
				SubscriptionMessage subMsg = subscribe(sub, brokerURI);
				subId2SubMap.put(subMsg.getMessageID(), sub);
				try {
					Thread.sleep(DELAY_BETWEEN_NEW_SUBSCRIPTION);
				} catch (Exception e) {
				}
			}
		return true;
	}

	/*
	 * Merely just instantiating the timer object, have not started the timer yet!
	 */
	private Timer setupLoggingTimer() {
		ActionListener taskPerformer = new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				synchronized (logSync) {
					logSync.notify();
				}
			}
		};
		return new Timer(clientConfig.logPeriod * 1000, taskPerformer);
	}

	/*
	 * Called by the timer to do logging whenever the logging period is up.
	 */
	private void doLogging() {
		// Calculate the average delay
		double avgDelay = 0;

		synchronized (avgDelaySync) {
			if (received > 0) {
				avgDelay = cumulativeDelay / (double) received;
				cumulativeDelay = 0;
				received = 0;
				// Can't compute the average if no messages were received
			} else {
				avgDelay = 0;
			}
			avgDelaySync.notifyAll();
		}

		clientLogger.info(Math.round(Calendar.getInstance(TimeZone.getTimeZone("GMT-5")).getTimeInMillis() / 1000.0)
				+ "\t"
				+ avgDelay
				+ "\t"
				+ (minDelay == Double.MAX_VALUE ? 0 : minDelay)
				+ "\t"
				+ (maxDelay == Double.MIN_VALUE ? 0 : maxDelay) + "\t" + duplicates);

		// reset some metrics
		minDelay = Double.MAX_VALUE;
		maxDelay = Double.MIN_VALUE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see stockquote.StockClient#handlePub(message.Publication)
	 */
	public void handlePub(Publication pub) {
		String topic = pub.getClassVal();
		// Only log stock quote publication messages
		if (topic.equalsIgnoreCase("STOCK")) {
			handleStockQuotePub(pub);
			// } else if (topic.equalsIgnoreCase(Mediator.CLASS_CLIENT_CONTROL)) {
			// handleSubscriberControlPub(pub);
		} else {
			clientLogger.warn("Got unexpected publication message: " + pub);
		}
	}

	/**
	 * Nothing else except do periodic logging
	 * 
	 */
	public void run() {
		// write column headers into log file
		// logger.info("# Avg delay | Min delay | Max delay | Duplicates");
		syncStartLoggingTimer();

		// Wait till the period has come to log
		synchronized (logSync) {
			while (true) {
				try {
					logSync.wait();
				} catch (InterruptedException e) {
				}
				doLogging();
			}
		}
	}

	public void processMessage(Message msg) {
		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage) msg).getPublication();
			long currTime = System.currentTimeMillis();
			long timestamp = pub.getTimeStamp().getTime();
			long delay = currTime - timestamp;
			System.out.println(pub.getPairMap().get("num") + "\t" + timestamp + "\t" + currTime
					+ "\t" + delay);
		}
	}

	/*
	 * Want to sync the logging to the start of every minute
	 */
	private void syncStartLoggingTimer() {
		Calendar now = Calendar.getInstance(TimeZone.getTimeZone("GMT-5"));
		int seconds = now.get(Calendar.SECOND);
		int milliseconds = now.get(Calendar.MILLISECOND);
		long waitTimeInMs = ((59 - seconds) * 1000) + (1000 - milliseconds);
		try {
			Thread.sleep(waitTimeInMs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		loggingTimer.start();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see universal.rmi.RMIUniversalClient#shutdown()
	 */
	public void shutdown() {
		loggingTimer.stop();
		// doLogging();
	}

	private static void showUsage() {
		System.out.printf("Usage: java StockPublisher -%s <id> -%s <broker_uri>\n\t"
				+ "-%s <subscription>\n", ClientConfig.CLI_OPTION_ID,
				ClientConfig.CLI_OPTION_BROKER_LIST, CMD_ARG_SUBSCRIPTION);
	}

	private void handleStockQuotePub(Publication pub) {
		// Update performance results
		// convert from ms to s
		double delay = (Calendar.getInstance(TimeZone.getTimeZone("GMT-5")).getTimeInMillis() - pub.getTimeStamp().getTime()) / 1000.0;
		minDelay = Math.min(minDelay, delay);
		maxDelay = Math.max(maxDelay, delay);

		// Update variables for computing the average
		synchronized (avgDelaySync) {
			cumulativeDelay += delay;
			received++;
			avgDelaySync.notifyAll();
		}

		clientLogger.debug("Got publication " + pub + " with delay " + delay);

		System.out.println(pub.getTimeStamp().getTime() + "\t" + delay);

		// Detect for duplcates
		if (messageHistory.containsKey(pub)) {
			clientLogger.debug("Publication is duplicate!");
			duplicates++;
			messageHistory.remove(pub);
		} else {
			messageHistory.put(pub, null);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] cliKeys = ClientConfig.getCommandLineKeys();
		String[] fullCLIKeys = new String[cliKeys.length + 1];
		System.arraycopy(cliKeys, 0, fullCLIKeys, 0, cliKeys.length);
		fullCLIKeys[cliKeys.length] = CMD_ARG_SUBSCRIPTION + ":";
		CommandLine cmdLine = new CommandLine(fullCLIKeys);
		try {
			cmdLine.processCommandLine(args);
		} catch (Exception e) {
			e.printStackTrace();
			showUsage();
			System.exit(1);
		}
		String subscription = cmdLine.getOptionValue(CMD_ARG_SUBSCRIPTION);

		try {
			new LogSetup(null);
		} catch (LogException e1) {
			e1.printStackTrace();
			System.exit(1);
		}

		Logger logger = Logger.getLogger(Client.class);
		try {
			ClientConfig clientConfig = new ClientConfig();
			clientConfig.clientID = DEFAULT_SUBSCRIBER_ID;
			clientConfig.overwriteWithCmdLineArgs(cmdLine);
			if (subscription == null) {
				logger.fatal("Don't know what to subscribe because subscription is not specified.");
				showUsage();
				System.exit(1);
			} else if (clientConfig.connectBrokerList == null
					|| clientConfig.connectBrokerList.length == 0) {
				logger.fatal("Don't know which broker to connect to.");
				showUsage();
				System.exit(1);
			}
			StockSubscriber subscriber = new StockSubscriber(clientConfig, subscription);
			logger.info("Stock subscriber created. Subscribing to " + subscription);
			// Let it run forever until this program is killed
			subscriber.run();
		} catch (ClientException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}

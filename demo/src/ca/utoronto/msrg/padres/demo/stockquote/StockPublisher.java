package ca.utoronto.msrg.padres.demo.stockquote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.util.LogException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

/**
 * 
 * @author cheung
 * 
 *         Publishes stock quote publications just like the publisher client in the simulator
 * 
 *         Features: - delivery delay logging - does not shutdown if publisher is not connected with
 *         anyone
 * 
 */
public class StockPublisher extends StockClient implements Runnable {

	// CONSTANTS
	private static final String DEFAULT_ADVERTISEMENT = "[class,eq,'STOCK'],[symbol,isPresent,'A'],[open,isPresent,1],"
			+ "[high,isPresent,1],[low,isPresent,1],[close,isPresent,1],"
			+ "[volume,isPresent,1],[date,isPresent,'A']";

	private final static String DEFAULT_PUBLISHER_ID = "P" + DEFAULT_CLIENT_ID;

	private final static String DEFAULT_PUB_RATE = "1.0"; // msg/min

	private final static String DEFAULT_PUB_DATA_PATH = ClientConfig.PADRES_HOME
			+ "/demo/data/stockquotes/";

	private final static String DEFAULT_PUB_DELAY = "0";

	private final static long DELAY_BETWEEN_NEW_PUBLISHER = 100; // ms

	// Command-line argument-related constants
	private static final String CMD_ARG_SYMBOL = "s";

	private static final String CMD_ARG_RATE = "r";

	private static final String CMD_ARG_PUB_DELAY = "d";

	// make sure .split() uses "\\" for special chars
	public static final String PUB_DELIMIT_CHAR = "/";

	// The stock that this publisher publishes
	private final String symbol;

	// Time in between publishing in milliseconds.
	private final long publishPeriod;

	private final double rate; // just for reference when logging

	// time to start publish after instantiation in ms
	private final long pubDelay;

	// Advertisement for this publisher. This doesn't change after it is initialized
	private final Advertisement advertisement;

	// Useful when we want to disconnect from a specified broker and when we want to send a
	// publication to all of our current connected brokers
	private final HashMap<String, String> brokerIdAdvIdMap;

	// Used for reading in publications to publish
	private BufferedReader reader;

	// The number of publication sent
	private long pubCount;

	// The number of times the entire publication file was published
	private int publicationRounds;

	private Publication lastPublished = null;

	// to read from a file or just to repeat one publication again and again
	// it is a hack for profiling experiments; keep it "false" for normal StockPublisher functions
	private boolean repeatPub = false;

	public StockPublisher(ClientConfig clientConfig, String pubSymbol, double pubRate, double delay)
			throws ClientException {
		super(clientConfig);
		symbol = pubSymbol;
		rate = pubRate;
		pubDelay = Math.round(delay * 1000.0); // s to ms
		publishPeriod = toMilliSecondPeriod(rate); // convert to pub/sec
		brokerIdAdvIdMap = new HashMap<String, String>(DEFAULT_CONNECTION_CAPACITY);
		pubCount = 0;
		publicationRounds = 0;
		initializeReader();
		// advertise to the brokers and update our bookkeeping
		advertisement = initializeAdvertisement();
		for (String brokerURI : getBrokerURIList()) {
			AdvertisementMessage advMsg = advertise(advertisement, brokerURI);
			brokerIdAdvIdMap.put(brokerURI, advMsg.getMessageID());
		}
	}

	/**
	 * Time to publish!
	 */
	public void publish() {
		String publicationStr = null;
		if (repeatPub) {
			// this is just a hack for performance testing
			publicationStr = "[class,'STOCK'],[symbol,'STEM'],[open,3.26],[high,3.26],[low,3.15],[close,3.18],[volume,1630900],[date,'21-Dec-04']";
		} else {
			// If the end of the file is reached, go back to the first line
			try {
				if ((publicationStr = reader.readLine()) == null) {
					initializeReader();
					publicationRounds++;
					publicationStr = reader.readLine();
				}
			} catch (IOException e) {
				exceptionLogger.error(e.getMessage());
			}
		}
		publicationStr += ",[num," + pubCount + "]";

		// Send the publication to all brokers that we're connected to
		for (String brokerURI : brokerIdAdvIdMap.keySet()) {
			Publication pub = null;
			PublicationMessage pubId = null;
			
			try {
				pub = MessageFactory.createPublicationFromString(publicationStr);
				lastPublished = pub;
				pubId = null;
				pubId = super.publish(pub, brokerURI);
				exceptionLogger.debug("Sent publication (" + pubId + "): " + pub);
				pubCount++;
			} catch (ClientException e) {
				exceptionLogger.error(e.getMessage());
			} catch (ParseException e){
				exceptionLogger.error(e.getMessage());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see universal.rmi.RMIUniversalClient#shutdown()
	 */
	public void shutdown() {
		StringBuffer output = new StringBuffer();
		output.append("Summary log:\n");
		output.append("Publisher ID  	: " + getClientID() + "\n");
		output.append("Publication   	: " + symbol + "\n");
		output.append("Publication Rate	: " + rate + "msgs/min\n");
		output.append("Broker History	: " + getBrokerHistory() + "\n");
		output.append("Messages sent 	: " + pubCount + "\n");
		output.append("Rounds        	: " + publicationRounds + "\n");
		output.append("Last published   : " + lastPublished + "\n");

		exceptionLogger.info(output);
	}

	/**
	 * Converts the publication rate from messages per minute to messages per second
	 * 
	 * @param msgsPerMinute
	 * @return
	 */
	private long toMilliSecondPeriod(double msgsPerMinute) {
		return Math.round(1000.0 / (msgsPerMinute / 60.0));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		pause(pubDelay);
		while (true) {
			// let's delay first so that we give the advertisement some time to propagate
			pause(publishPeriod);
			// Time to publish!
			publish();
		}
	}

	private void pause(long delay) {
		try {
			Thread.sleep(delay);
		} catch (Exception e) {
		}
	}

	/*
	 * Initializes the reader to start reading from the beginning of the file
	 */
	private void initializeReader() {
		reader = null;
		try {
			exceptionLogger.debug("Initialize reader count: " + publicationRounds);
			reader = new BufferedReader(new FileReader(DEFAULT_PUB_DATA_PATH + symbol));
		} catch (IOException e) {
			exceptionLogger.error(e.getMessage());
		}
	}

	/*
	 * Makes an advertisement by examining the first publication in the publication file
	 */
	private Advertisement initializeAdvertisement() {
		Advertisement adv = null;
		try {
			String pubString = reader.readLine();
			pubString += ",[num,0]";
			adv = Advertisement.toAdvertisement(pubString);
			// change op for attribute symbol from "isPresent" to "eq"
			((Predicate) adv.getPredicateMap().get("symbol")).setOp("eq");
		} catch (Exception e) {
			System.out.println("Failed: (Publisher) Error making advertisement.  Using default:"
					+ DEFAULT_ADVERTISEMENT);

			try {
				adv = MessageFactory.createAdvertisementFromString(DEFAULT_ADVERTISEMENT);
			} catch (ParseException e1) {
				exceptionLogger.error(e.getMessage());
				return null;
			}
		}

		return adv;
	}

	private static void showUsage() {
		System.out.printf("Usage: java StockPublisher -%s <id> -%s <broker_uri>\n\t"
				+ "-%s <symbol> -%s <pub_rate/min> -%s <pub_start_time>\n",
				ClientConfig.CLI_OPTION_ID, ClientConfig.CLI_OPTION_BROKER_LIST, CMD_ARG_SYMBOL,
				CMD_ARG_RATE, CMD_ARG_PUB_DELAY);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			new LogSetup(null);
		} catch (LogException e) {
			e.printStackTrace();
			System.exit(1);
		}
		String[] cliKeys = ClientConfig.getCommandLineKeys();
		String[] fullCLIKeys = new String[cliKeys.length + 3];
		System.arraycopy(cliKeys, 0, fullCLIKeys, 0, cliKeys.length);
		fullCLIKeys[cliKeys.length] = CMD_ARG_SYMBOL + ":";
		fullCLIKeys[cliKeys.length + 1] = CMD_ARG_RATE + ":";
		fullCLIKeys[cliKeys.length + 2] = CMD_ARG_PUB_DELAY + ":";
		CommandLine cmdLine = new CommandLine(fullCLIKeys);
		try {
			cmdLine.processCommandLine(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		String rateString = cmdLine.getOptionValue(CMD_ARG_RATE, DEFAULT_PUB_RATE);
		String stockString = cmdLine.getOptionValue(CMD_ARG_SYMBOL);
		String pubDelay = cmdLine.getOptionValue(CMD_ARG_PUB_DELAY, DEFAULT_PUB_DELAY);

		// Now that logging is set up, we can start logging
		Logger logger = Logger.getLogger(Client.class);
		// Exit if stock symbol is not specified
		if (stockString == null) {
			logger.fatal("Don't know what to publish because stock symbol not specified.");
			showUsage();
			System.exit(1);
			// Exit if no broker address is specified
		} else if (cmdLine.getOptionValue(ClientConfig.CLI_OPTION_BROKER_LIST) == null) {
			logger.fatal("Don't know which broker to connect to.");
			showUsage();
			System.exit(1);
		}

		String[] stockSymbols = stockString.split(PUB_DELIMIT_CHAR);
		String[] stockRates = rateString.split(PUB_DELIMIT_CHAR);

		if (stockSymbols.length != stockRates.length) {
			System.err.println("Number of stock symbols does not match with number of rates provided! "
					+ stockSymbols.length + " != " + stockRates.length);
			System.exit(1);
		}

		StockPublisher[] publishers = new StockPublisher[stockSymbols.length];
		Thread pubThread[] = new Thread[stockSymbols.length];
		for (int i = 0; i < stockSymbols.length; i++) {
			// Before logging and exiting with error, need to instantiate the publisher
			// class so that logging is initialized before we use it
			try {
				ClientConfig clientConfig = new ClientConfig();
				clientConfig.clientID = DEFAULT_PUBLISHER_ID;
				clientConfig.overwriteWithCmdLineArgs(cmdLine);
				clientConfig.clientID = clientConfig.clientID + "_" + i;
				//
				// System.out.println("clientConfig = " + clientConfig.toString());
				// System.out.println("stock = " + stockSymbols[i]);
				// System.out.println("rate = " + stockRates[i]);
				// System.out.println();

				publishers[i] = new StockPublisher(clientConfig, stockSymbols[i],
						Double.parseDouble(stockRates[i]), Double.parseDouble(pubDelay));
				// Let it publish forever until this program is killed
				pubThread[i] = new Thread(publishers[i]);
				pubThread[i].start();
				Thread.sleep(DELAY_BETWEEN_NEW_PUBLISHER);
			} catch (ClientException e) {
				System.out.println(e.getMessage());
				System.exit(1);
			} catch (InterruptedException e) {
			}
		}

		for (int i = 0; i < stockSymbols.length; i++) {
			try {
				pubThread[i].join();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}

		// Now wait forever
		final Object dummyObj = new Object();
		synchronized (dummyObj) {
			try {
				dummyObj.wait();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

}

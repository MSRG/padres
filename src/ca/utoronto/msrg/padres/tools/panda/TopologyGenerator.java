/**
 * 
 */
package ca.utoronto.msrg.padres.tools.panda;

/**
 * @author cheung
 *
 * Constructs a topology in the form of a tree with a branch of N
 * 
 * work on create publisher
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;

import ca.utoronto.msrg.padres.common.util.TypeChecker;
import ca.utoronto.msrg.padres.common.util.io.FileOperation;
import ca.utoronto.msrg.padres.demo.stockquote.StockPublisher;
import ca.utoronto.msrg.padres.demo.stockquote.StockSubscriber;

public class TopologyGenerator {

	private final String ipAddrFile;
	private final BufferedReader in;
	private BufferedWriter out; 
	private final Properties properties;
	private final Random rand;
	
	private final String pubDataPath;
	private final double minPubRate;
	private final double maxPubRate;
	private final String minMemory;
	private final String maxMemory;
	private final String stockSymbols[];
	private final String filename;
	private final int maxSubsPerClient;
	private final int maxPubsPerClient;
		
	// records the stock symbols that chosen for publisher already.
	private HashSet<String> publisherSet;
	
	// Used to quickly pick which subscription template to use
	private HashMap<Integer, String> subTemplate;
	
	// CONSTANTS
	public static final String PADRES_HOME =
		System.getenv("PADRES_HOME") == null
				? null
				: System.getenv("PADRES_HOME") + "/";
	private static final String DEFAULT_FILENAME = "input.txt";
	private final String CONFIG_FILE_PATH = PADRES_HOME + "etc/panda/generator.cfg";
	
	private static final int MAX_ADDRESSES_TO_REPLACE = 1000;
	private static final String ADDR_PATTERN_PREFIX = "<A_";
	private static final String ADDR_PATTERN_SUFFIX = ">";
	
	private static final String BROKER_ID_PREFIX = "Broker";
	private static final String DEFAULT_BROKER_PORT = "21979";
	
	private static final String START_STOCKPUBLISHER_SCRIPT = "startSQpublisher.sh";
	private static final String START_STOCKSUBSCRIBER_SCRIPT = "startSQsubscriber.sh";
	private static final String START_BROKER_SCRIPT = "startbroker.sh";
	
	/**
	 * Constructor
	 * @param ipAddrFilename
	 */
	public TopologyGenerator(String ipAddrFilename) {
		ipAddrFile = ipAddrFilename;
		publisherSet = new HashSet<String>();
		subTemplate = new HashMap<Integer, String>();
		
		// load config
		properties = new Properties();
		try {
			properties.load(new FileInputStream(CONFIG_FILE_PATH));
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(0);
		}
		
		// getting input from console
		in = new BufferedReader(new InputStreamReader(System.in));
		
		minPubRate = Double.parseDouble(properties.getProperty("MIN_PUB_RATE"));
		maxPubRate = Double.parseDouble(properties.getProperty("MAX_PUB_RATE"));
		minMemory = properties.getProperty("MEMORY_LOW");
		maxMemory = properties.getProperty("MEMORY_HIGH");
		maxSubsPerClient = Integer.parseInt(properties.getProperty("MAX_SUBSCRIPTIONS_PER_CLIENT"));
		maxPubsPerClient = Integer.parseInt(properties.getProperty("MAX_PUBLICATIONS_PER_CLIENT"));
		filename = properties.getProperty("FILENAME", DEFAULT_FILENAME);
		rand = new Random(Long.parseLong(properties.getProperty("SEED", "1")));
		pubDataPath = properties.getProperty("PUB_DATA_PATH");
		stockSymbols = getStockNames();
		
		// set up output stream for writing output to file
		initOutput();
		initSubscriptions();
	}
	
	public void run() {
		// Get number of nodes
		int choice;
		while (true) {
			System.out.println("[1] Create a tree broker network");
			System.out.println("[2] Create publishers for a broker");
			System.out.println("[3] Create subscribers for a broker");
			System.out.println("[4] Reset output");
			System.out.println("[5] Search-&-Replace address");
			System.out.println("[6] Exit this program");

			choice = getPositiveIntegerInput("Enter a number to select one of the following choices:");
			
			switch(choice) {
			case 1:
				createBrokerNetwork();
				break;
			case 2:
				createPublishers();
				break;
			case 3:
				createSubscribers();
				break;
			case 4:
				initOutput();
				break;
			case 5:
				searchAndReplaceAddress();
				break;
			case 6:
				System.out.println("Exiting...");
				System.exit(0);
				break;
			default:
				break;
			}
		}
	}
		
	
	
	
	
	/*	PUBLISHER
	 * ======================================================================================
	 */
	private void createPublishers()
	{
		ArrayList<String> ipList = new ArrayList<String>();
		System.out.print("Time to create publishers (s): ");
		String timeStr = getUserInput();
		System.out.print("Number of publishers to create: ");
		String pubCount = getUserInput();
		System.out.print("IP address and port (10.0.0.1:1099) of the broker to connect to: ");
		String brokerAddr = getUserInput();
		System.out.println("IP address of machine to run these publishers.");
		do {
			System.out.println("\n" + ipList.toString() + "\n");
			System.out.println("(Enter 'q' to stop): ");
			String addr = getUserInput().trim();
			if (addr.equalsIgnoreCase("q")) {
				if (!ipList.isEmpty()) {
					break;
				}
			} else if (addr.equals("")) {
				continue;
			} else {
				ipList.add(addr);
			}
		} while (true);
		
		if (TypeChecker.isNumeric(timeStr) && TypeChecker.isNumeric(pubCount) && 
				TypeChecker.isString(brokerAddr)) {
			// insert a comment in the simulation file
			write("\n\n# " + pubCount + " unique publishers at " + brokerAddr + "\n");
			double time = Double.parseDouble(timeStr);
			
			int publishersToCreate = Integer.parseInt(pubCount);
			final int numberOfClients = (int)Math.ceil((double)publishersToCreate / maxPubsPerClient); 
			for (int j = 0; j < numberOfClients; j++) {
				String rateStr = "";
				String symbolStr = "";
				// To get rid of 0.0000000000000001 differences
				time = (double)(Math.round((time + 0.01) * 100.0)) / 100.0;
			
				int publisherCount = Math.min(maxPubsPerClient, publishersToCreate);
				publishersToCreate -= publisherCount;
				
				for (int i = 0; i < publisherCount; i++) {
					// we want rate like 12.30000000000000
					double rate = (double)Math.round( 
						((rand.nextDouble() * (maxPubRate - minPubRate)) + minPubRate) * 10.0 
						) / 10.0;
					rateStr += rate + StockPublisher.PUB_DELIMIT_CHAR;
					
					boolean isDuplicateSymbol = true;
					while (isDuplicateSymbol) {
						String symbol = getRandomSymbol();
						if (!publisherSet.contains(symbol) || publisherSet.size() == stockSymbols.length) {
							publisherSet.add(symbol);
							symbolStr += symbol + StockPublisher.PUB_DELIMIT_CHAR;
							isDuplicateSymbol = false;
						}
					}
				}
				
//				 trim off the last delimiter character
				symbolStr = symbolStr.substring(
						0, symbolStr.lastIndexOf(StockPublisher.PUB_DELIMIT_CHAR));
				rateStr = rateStr.substring(
						0, rateStr.lastIndexOf(StockPublisher.PUB_DELIMIT_CHAR));
				createPublisherClient(time, "P" + j, symbolStr, rateStr, brokerAddr,
					ipList.get(rand.nextInt(ipList.size())));


			}
		} else {
			System.out.println("Aborted, one or more arguments were not well-formed");	 
		}
	}
	
	// Writes a create publisher command into the output file
	private void createPublisherClient(double time, String publisherID, 
		String symbol, String rate, String brokerAddr, String addr)
	{
		StringBuffer command = new StringBuffer();
		command.append(time + " ");
		command.append("ADD ");
		command.append(publisherID + " ");
		command.append(addr + " ");
		command.append(START_STOCKPUBLISHER_SCRIPT + " ");
		command.append("-hostname " + addr + " ");
		command.append("-i " + publisherID + " ");
		command.append("-s " + symbol + " ");
		command.append("-r " + rate + " ");
		command.append("-d 0 ");	// for initial delay
		command.append("-b " + brokerAddr);
		
		write(command.toString());
	}
	/*
	 * ======================================================================================
	 */
	
	
	
	
	
	/*	SUBSCRIBERS
	 * ======================================================================================
	 */
	private void createSubscribers()
	{
		ArrayList<String> ipList = new ArrayList<String>();
		System.out.print("Start time to create subscribers (s): ");
		String startTimeStr = getUserInput();
		System.out.print("End time to create subscribers (s): ");
		String endTimeStr = getUserInput();
		int subCount = getPositiveIntegerInput("Number of subscribers to create: ");
		System.out.print("Percentage of subscribers with zero traffic (50 for 50%): ");
		String strZeroTrafficRatio = getUserInput();
		System.out.print("IP address and port (10.0.0.1:1099) of the broker to connect to: ");
		String brokerAddr = getUserInput();
		System.out.println("IP address of machine to run these subscribers.");
		do {
			System.out.println("\n" + ipList.toString() + "\n");
			System.out.println("(Enter 'q' to stop): ");
			String addr = getUserInput();
			if (addr.equalsIgnoreCase("q")) {
				if (!ipList.isEmpty()) {
					break;
				}
			} else if (addr.equals("")) {
				continue;
			} else {
				ipList.add(addr);
			}
		} while (true);
		

		if (TypeChecker.isNumeric(startTimeStr) && TypeChecker.isNumeric(endTimeStr) 
				&& TypeChecker.isNumeric(strZeroTrafficRatio) 
				&& TypeChecker.isString(brokerAddr)) {
					
			// insert a comment in the simulation file
			write("\n\n# " + subCount + " subscribers at " + brokerAddr 
				+ ", with " + strZeroTrafficRatio + "% have no traffic\n");

			double zeroTrafficRatio = Double.parseDouble(strZeroTrafficRatio);
			zeroTrafficRatio = zeroTrafficRatio / 100.0;
			double startTime = Double.parseDouble(startTimeStr);
			double endTime = Double.parseDouble(endTimeStr);
			double time;
			int id = 0;
			
			// Create zero traffic subscribers
			int zeroTrafficSubCount = (int)Math.round((double)subCount * zeroTrafficRatio);
			final int orgZeroTrafficSubCount = zeroTrafficSubCount;
			int zeroTrafficClientCount = (int)Math.ceil((double)zeroTrafficSubCount / maxSubsPerClient); 
			for (int i = 0; i < zeroTrafficClientCount; i++) {
				// If all stock symbols have publishers, then break
				if (publisherSet.size() == stockSymbols.length) {
					System.out.println("Warning, all stocks have publishers. "
						+ "No zero traffic subscriber will be created");
					break;
				}
					
				int subCountForClient = Math.min(maxSubsPerClient, zeroTrafficSubCount);
				zeroTrafficSubCount -= subCountForClient;
				
				String subscriptionStringForClient = "";
				for (int j = 0; j < subCountForClient; j++) {
					boolean zeroTrafficSubFound = false;
					while (!zeroTrafficSubFound) {
						String symbol = getRandomSymbol();
						String subscription = getRandomSubscription(symbol); 
						// subscriptions not having a specific symbol attracts traffic, so forbid them
						if (!publisherSet.contains(symbol) && subscription.indexOf(symbol) != -1) {
							zeroTrafficSubFound = true;
							subscriptionStringForClient += subscription + StockSubscriber.SUB_DELIMIT_CHAR;
						}
					}
				}
				// trim off the last delimiter character
				subscriptionStringForClient = subscriptionStringForClient.substring(
						0, subscriptionStringForClient.lastIndexOf(StockSubscriber.SUB_DELIMIT_CHAR));
				
				time = Math.round(	// reduce to nearest hundredths
					((rand.nextDouble() * (endTime - startTime)) + startTime) * 100.0) / 100.0;
				createSubscriberClient(time, "S" + id, subscriptionStringForClient, brokerAddr, 
						ipList.get(rand.nextInt(ipList.size())));
				id++;
			}

			// Create nonzero traffic subscribers
			int nonZeroTrafficSubCount = subCount - orgZeroTrafficSubCount;
			int nonZeroTrafficClientCount = (int)Math.ceil(nonZeroTrafficSubCount / maxSubsPerClient);
			for (int i = 0; i < nonZeroTrafficClientCount; i++) {
				// If no publishers were created yet, then we should skip this
				if (publisherSet.isEmpty()) {
					System.out.println("Warning, no publishers were created yet. "
						+ "No nonzero traffic subscribers created.");
					break;
				}
				
				int subCountForClient = Math.min(maxSubsPerClient, nonZeroTrafficSubCount);
				nonZeroTrafficSubCount -= subCountForClient;
				
				String subscriptionStringForClient = "";
				for (int j = 0; j < subCountForClient; j++) {
					boolean nonZeroTrafficSubFound = false;
					while (!nonZeroTrafficSubFound) {
						String symbol = getRandomSymbol();
						if (publisherSet.contains(symbol)) {
							nonZeroTrafficSubFound = true;
							subscriptionStringForClient += getRandomSubscription(symbol) + StockSubscriber.SUB_DELIMIT_CHAR;
						}
					}
				}
				// trim off the last delimiter character
				subscriptionStringForClient = subscriptionStringForClient.substring(
						0, subscriptionStringForClient.lastIndexOf(StockSubscriber.SUB_DELIMIT_CHAR));
				
				time = Math.round(	// reduce to nearest hundredths
					((rand.nextDouble() * (endTime - startTime)) + startTime) * 100.0) / 100.0;
				createSubscriberClient(time, "S" + id, subscriptionStringForClient, brokerAddr,
						ipList.get(rand.nextInt(ipList.size())));
				id++;
			}
		} else {
			System.out.println("Aborted, one or more arguments were not well-formed");	 
		}
	}
	
	// Writes a create subscriber command into the output file
	private void createSubscriberClient(double time, String subscriberID, 
		String subscription, String brokerAddr, String addr)
	{
		StringBuffer command = new StringBuffer();
		command.append(time + " ");
		command.append("ADD ");
		command.append(subscriberID + " ");
		command.append(addr + " ");
		command.append(START_STOCKSUBSCRIBER_SCRIPT + " ");
		command.append("-hostname " + addr + " ");
		command.append("-i " + subscriberID + " ");
		command.append("-s \"" + subscription + "\" ");
		command.append("-b " + brokerAddr);
		
		write(command.toString());
	}
	
	/*
	 * symbol can be null, in which case this function will randomly choose one
	 */
	private String getRandomSubscription(String symbol)
	{
		int index = rand.nextInt(subTemplate.size());
		String template = subTemplate.get(index).toString();
		
		while (template.indexOf("REPLACE_") > -1)
		{
			if (template.indexOf("REPLACE_SYMBOL") > -1) {
				// pick a stock randomly
				symbol = (symbol == null) ? getRandomSymbol() : symbol;
				template = template.replaceAll("REPLACE_SYMBOL", symbol);
			} else if (template.indexOf("REPLACE_HIGH") > -1) {
				symbol = (symbol == null) ? getRandomSymbol() : symbol;
				template = template.replaceAll("REPLACE_HIGH", 
					getRandomValue(symbol, "high"));
			} else if (template.indexOf("REPLACE_LOW") > -1) {
				symbol = (symbol == null) ? getRandomSymbol() : symbol;
				template = template.replaceAll("REPLACE_LOW", 
					getRandomValue(symbol, "low"));
			} else if (template.indexOf("REPLACE_VOLUME") > -1) {
				symbol = (symbol == null) ? getRandomSymbol() : symbol;
				template = template.replaceAll("REPLACE_VOLUME", 
					getRandomValue(symbol, "volume"));
			} else {
				System.out.println("Don't know what to do with REPLACE field");
				System.out.println("Please add logic into getSubscription() function");
				System.exit(0);		
			}
		}
		
		return template;
	}

	private String getRandomValue(String symbol, String field) {
		File stockFile = new File(pubDataPath + "/" + symbol);
		String value = "";	// sample value to be returned
		String sample = "";
		// A 703165 byte file contains roughly 5109 lines.
		// Each line has roughly 120 characters.
		int offset = Math.round(stockFile.length() * 5109 / 703165 * 120 );
		try {
			BufferedReader reader = new BufferedReader(new FileReader(
				pubDataPath + "/" + symbol));
			reader.skip(rand.nextInt(offset));
			
			// the line read after the offset might be cut off, so discard it
			reader.readLine();
			sample = reader.readLine();
			String searchPattern = "[" + field.toLowerCase() + ",";	// like '[high,'
			int valueIndex = sample.indexOf(searchPattern) + searchPattern.length();
			value = sample.substring(valueIndex, 
				sample.indexOf("]", valueIndex));
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}
		
		return value;
	}
	/*
	 * ======================================================================================
	 */	
	
	
	
	
	
	
	/*	BROKER
	 * ======================================================================================
	 */
	private void createBrokerNetwork() {
		checkInputFileExists();
		int maxNumberOfNodes = FileOperation.getTotalLineCount(ipAddrFile);
		
		int numberOfNodes = getPositiveIntegerInput("How many nodes do you want to create?");
		if (numberOfNodes > maxNumberOfNodes) {
			System.out.println("The number of nodes cannot exceed "
					+ maxNumberOfNodes + ". Try again.");
			return;
		} 
		
		int fanOutDegree = getPositiveIntegerInput("What is the degree or fan-out per node?");
		
		generateTreeTopologyFile(ipAddrFile, numberOfNodes, fanOutDegree);
	}
	
	private void generateTreeTopologyFile(
			String ipAddrFile, int numberOfNodes, int fanOutDegree) {
		
		String ipAddr, brokerId, neighborAddr, output;
		LinkedHashMap<String, Integer> availableParentAddrMap = new LinkedHashMap<String, Integer>();
		String outputFile = "topology-" + numberOfNodes + "N-" + fanOutDegree + "F.txt";
		// set up output stream for writing output to file
		try {
			out = new BufferedWriter(new FileWriter(outputFile, false));
		} catch (IOException e) {
			System.err.println("Problem initializing output file: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		try {
			BufferedReader reader = new BufferedReader(new FileReader(ipAddrFile));
			for (int i = 0; 
					(i < numberOfNodes) && ((ipAddr = reader.readLine()) != null);
					i++) {
				// Prepare the values to replace
				brokerId = BROKER_ID_PREFIX + i;
				neighborAddr = availableParentAddrMap.isEmpty() 
					? ""
					: availableParentAddrMap.keySet().iterator().next();
				output = "0.0 ADD " + brokerId + " " + ipAddr.trim() + " " + START_BROKER_SCRIPT + " "
					+ "-Xmx " + maxMemory + " "
					+ "-Xms " + minMemory + " "
					+ "-hostname " + ipAddr.trim() + " "
					+ "-i " + brokerId + " "
					+ "-p " + DEFAULT_BROKER_PORT + " "
					+ "-n " + neighborAddr + " ";
					
				write(output);
				
				// Need to update the number of children nodes attached to the 
				// currently selected parent
				if (neighborAddr != "") {
					int childrenCount = availableParentAddrMap.get(neighborAddr) + 1;
					if (childrenCount < fanOutDegree)
						availableParentAddrMap.put(neighborAddr, new Integer(childrenCount));
					else
						availableParentAddrMap.remove(neighborAddr);
				}
				
				// Need to add the newly added node to the available parent addr map
				availableParentAddrMap.put(ipAddr.trim() + ":" + DEFAULT_BROKER_PORT, 0);
			}
			
			reader.close();
			reader = null;
		} catch (Exception e) {
			System.err.println("Something wrong with generating topology file:");
			e.printStackTrace();
		}	
	}
	/*
	 * ======================================================================================
	 */
	
	
	
	
	/* SEARCH AND REPLACE ADDRESS
	 * ======================================================================================
	 */
	private void searchAndReplaceAddress() {
		System.out.print("File name of template file: ");
		String templateFilename = getUserInput();
		System.out.print("File name of address file: ");
		String addressFilename = getUserInput();
		
		// Check for errors
		if (!FileOperation.exists(templateFilename)) {
			System.out.println("Template file " + templateFilename + " not found.");
			return;
		}
		if (!FileOperation.exists(addressFilename)) {
			System.out.println("Address file " + addressFilename + " not found.");
			return;
		}		

		String outputFile = templateFilename + ".out";
		BufferedWriter writer = getFileWriter(outputFile);
		
		// load all lines from template file
		String templateContents = FileOperation.getLastNLines(templateFilename, Integer.MAX_VALUE);
		String addressContents = FileOperation.getLastNLines(addressFilename, Integer.MAX_VALUE);
		StringTokenizer addresses = new StringTokenizer(addressContents);
	
		// Do search and replace up till address MAX_ADDRESSES_TO_REPLACE
		String addrPattern;
		for (int i = 0; i < MAX_ADDRESSES_TO_REPLACE; i++) {
			addrPattern = ADDR_PATTERN_PREFIX + i + ADDR_PATTERN_SUFFIX;
			if (templateContents.indexOf(addrPattern) > -1) {
				if (!addresses.hasMoreTokens()) {
					System.out.println("Insufficient addresses.  Abort address replacement");
					templateContents = "";
					return;
				}
				templateContents = templateContents.replaceAll(addrPattern, addresses.nextToken().trim());
			}
		}
		
		// Done search & replace.  Write result to file
		try {
			writer.write(templateContents);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			System.err.println("Problem writing S&R output: " + e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("Search & Replace OK.  Results written to " + outputFile);
	}
	
	private BufferedWriter getFileWriter(String filename) {
		try {
			return new BufferedWriter(new FileWriter(filename, false));
		} catch (IOException e) {
			System.err.println("Problem initializing output file: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	/*
	 * ======================================================================================
	 */
	
	private void initOutput() {
		try {
			out = new BufferedWriter(new FileWriter(filename, false));
		} catch (IOException e) {
			System.err.println("Problem initializing output file: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private int getPositiveIntegerInput(String prompt) {
		int result;
		
		System.out.println(prompt);
		while (true) {
			System.out.print("> ");
			try {
				String input = in.readLine().trim();
				result = Integer.parseInt(input);
				if (result > 0)
					return result;
			} catch (Exception e) {
			}
			System.out.println("Invalid input.  Input must be numeric and greater "
					+ "than 0.  Try again."); 
		}
	}

	
	private void checkInputFileExists() {
		if (!FileOperation.exists(ipAddrFile))  {
			System.out.println("IP address file at " + ipAddrFile 
					+ " does not exist.  Exiting...");
			System.exit(1);
		}
	}
	
	// Returns a random stock symbol
	private String getRandomSymbol()
	{
		return stockSymbols[rand.nextInt(stockSymbols.length)];
	}
	
	// A cleaner way of getting input from the user
	private String getUserInput()
	{
		String input = "";
		try {
			input = in.readLine();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(0);	
		}
		return input;
	}
	
	// Retrieve subscription templates from config file and store them
	// into a hashmap data-structure for easy random picks in the future.
	private void initSubscriptions()
	{
		// for parsing each subscription template
		StringTokenizer st = new StringTokenizer(
			properties.getProperty("SUBSCRIPTION_TEMPLATES"), ";");
		
		int count = 0, probability = 0;
		
		while (count < 100)
		{
			// for parsing each field
			StringTokenizer st2 = new StringTokenizer(st.nextToken(), ":");
			probability = Integer.parseInt(st2.nextToken());
			String template = st2.nextToken();
			
			// At the end, there will be 100 entries in the hashmap
			for (int i = count; i < count + probability; i++)
				subTemplate.put(i, template);
			
			count += probability;
		}
		
		if (count != 100) {
			System.out.println("Subscription template probabilities do not add up!");
			System.exit(0);
		}
	}
	
	/*
	 * Writes to the outputfile
	 */
	private void write(String line)
	{
		try {
			out.write(line);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	private String[] getStockNames()
	{
		return new File(pubDataPath).list();
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TopologyGenerator generator = new TopologyGenerator(
				args.length > 0 ? args[0] : null);
		generator.run();
	}

}

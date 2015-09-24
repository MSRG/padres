package ca.utoronto.msrg.padres.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import ca.utoronto.msrg.padres.common.util.CommandLine;

public class ClientConfig {

	public static final String DIR_SLASH = System.getProperty("file.separator");

	// default configurations
	public static final String PADRES_HOME = System.getenv("PADRES_HOME") == null ? "."
			+ File.separator : System.getenv("PADRES_HOME") + File.separator;

	protected static final String CONFIG_FILE_PATH = String.format("%s%setc%sclient.properties",
			PADRES_HOME, File.separator, File.separator);

	protected static final String LOG_CONFIG_FILE_PATH = String.format(
			"%s%setc%sclient_log4j.properties", PADRES_HOME, File.separator, File.separator);

	protected static final String LOG_LOCATION_PATH = System.getProperty("user.home")
			+ File.separator + ".padres" + File.separator + "logs" + File.separator;

	// Command line options
	public static final String CLI_OPTION_CONFIG_FILE = "c";

	public static final String CLI_OPTION_ID = "i";

	public static final String CLI_OPTION_BROKER_LIST = "b";

	public static final String CLI_OPTION_CONNECT_RETRY = "retry";

	public static final String CLI_OPTION_CONNECT_PAUSE = "pause";

	public static final String CLI_OPTION_DETAIL_STATE = "OFF";

	public static final String CLI_OPTION_LOG_CONFIG = "lc";

	public static final String CLI_OPTION_LOG_LOCATION = "ll";

	protected Properties clientProps;

	// configuration options
	public String configFile = CONFIG_FILE_PATH;

	public String clientID = null;

	public String[] connectBrokerList = null;

	public int connectionRetries = -1;

	public int retryPauseTime = -1;

	public boolean detailState;

	public int logPeriod = 60;

	public String logPropertyFile = LOG_CONFIG_FILE_PATH;

	public String logLocation = LOG_LOCATION_PATH;

	public ClientConfig() throws ClientException {
		this(CONFIG_FILE_PATH);
	}

	public ClientConfig(String configFile) throws ClientException {
		if (configFile != null)
			this.configFile = configFile;
		try {
			clientProps = new Properties();
			clientProps.load(new FileInputStream(this.configFile));
			clientID = clientProps.getProperty("client.id");
			String neighborList = clientProps.getProperty("client.remoteBrokers");
			if (neighborList != null)
				connectBrokerList = neighborList.split(",\\s*");
			else
				connectBrokerList = new String[0];
			connectionRetries = Integer.parseInt(clientProps.getProperty("connection.retries"));
			retryPauseTime = Integer.parseInt(clientProps.getProperty("connection.retry.pauseTime"));
			detailState = clientProps.getProperty("client.store_detail_state", "OFF").toLowerCase().trim().equals(
					"on");
			logPeriod = Integer.parseInt(clientProps.getProperty("log.period"));
		} catch (FileNotFoundException e) {
			throw new ClientException("Config file not found: " + this.configFile, e);
		} catch (IOException e) {
			throw new ClientException("Error reading config file: " + this.configFile, e);
		} catch (NumberFormatException e) {
			// parseInt() is given a null value: pass
		}
	}

	public ClientConfig(ClientConfig origConfig) {
		configFile = origConfig.configFile;
		clientID = origConfig.clientID;
		connectBrokerList = Arrays.copyOf(origConfig.connectBrokerList,
				origConfig.connectBrokerList.length);
		connectionRetries = origConfig.connectionRetries;
		retryPauseTime = origConfig.retryPauseTime;
		detailState = origConfig.detailState;
		logPeriod = origConfig.logPeriod;
		logPropertyFile = origConfig.logPropertyFile;
		logLocation = origConfig.logLocation;
	}

	public static String[] getCommandLineKeys() {
		List<String> cliKeys = new ArrayList<String>();
		cliKeys.add(CLI_OPTION_ID + ":");
		cliKeys.add(CLI_OPTION_BROKER_LIST + ":");
		cliKeys.add(CLI_OPTION_CONNECT_RETRY + ":");
		cliKeys.add(CLI_OPTION_CONNECT_PAUSE + ":");
		cliKeys.add(CLI_OPTION_DETAIL_STATE + ":");
		cliKeys.add(CLI_OPTION_CONFIG_FILE + ":");
		cliKeys.add(CLI_OPTION_LOG_CONFIG + ":");
		cliKeys.add(CLI_OPTION_LOG_LOCATION + ":");
		return cliKeys.toArray(new String[0]);
	}

	public void overwriteWithCmdLineArgs(CommandLine cmdLine) {
		String buffer = null;
		if ((buffer = cmdLine.getOptionValue(CLI_OPTION_ID)) != null)
			clientID = buffer.trim();
		if ((buffer = cmdLine.getOptionValue(CLI_OPTION_BROKER_LIST)) != null)
			connectBrokerList = buffer.trim().split(",");
		if ((buffer = cmdLine.getOptionValue(CLI_OPTION_CONNECT_RETRY)) != null)
			connectionRetries = Integer.parseInt(buffer.trim());
		if ((buffer = cmdLine.getOptionValue(CLI_OPTION_CONNECT_PAUSE)) != null)
			retryPauseTime = Integer.parseInt(buffer.trim());
		if ((buffer = cmdLine.getOptionValue(CLI_OPTION_DETAIL_STATE)) != null)
			detailState = buffer.trim().toLowerCase().equals("on");
	}

	public String toString() {
		String outString = "\nClient ID: " + clientID;
		outString += "\nBroker Connections: " + Arrays.toString(connectBrokerList);
		outString += "\nConfig File: " + configFile;
		outString += "\nConnection Retries: " + connectionRetries;
		outString += "\nConnection Retry Pause: " + retryPauseTime;
		outString += "\nLog Period: " + logPeriod;
		outString += "\nLog Property File: " + logPropertyFile;
		outString += "\nLogs Location: " + logLocation;
		return outString + "\n";
	}

}

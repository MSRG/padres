package ca.utoronto.msrg.padres.broker.brokercore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import ca.utoronto.msrg.padres.broker.controller.db.DBConnector;
import ca.utoronto.msrg.padres.broker.management.web.ManagementServer;
import ca.utoronto.msrg.padres.broker.monitor.BrokerInfoPublisher;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor.AdvSubInfoType;
import ca.utoronto.msrg.padres.broker.router.AdvertisementFilter;
import ca.utoronto.msrg.padres.broker.router.AdvertisementFilter.AdvCoveringType;
import ca.utoronto.msrg.padres.broker.router.RouterFactory.MatcherType;
import ca.utoronto.msrg.padres.broker.router.SubscriptionFilter;
import ca.utoronto.msrg.padres.broker.router.SubscriptionFilter.SubCoveringType;
import ca.utoronto.msrg.padres.broker.topk.TopkInfo;
import ca.utoronto.msrg.padres.common.util.CommandLine;

/**
 * The data structure to process broker configuration files and command line options and store the
 * broker configurations.
 * 
 * @author Bala Maniymaran
 * 
 *         Created: 2011-02-02
 * 
 */
public class BrokerConfig {

	public enum CycleType {
		OFF, FIXED, DYNAMIC;
	}

	public static final String PADRES_HOME = System.getenv("PADRES_HOME") == null ? "."
			+ File.separator : System.getenv("PADRES_HOME") + File.separator;

	// default values
	public final static boolean DEBUG_MODE_DEFAULT = false;

	protected static final String DEFAULT_PROPS_FILE_PATH = PADRES_HOME + "etc" + File.separator
			+ "broker.properties";

	protected static final String DEFAULT_MI_PROPS_FILE_PATH = PADRES_HOME + "etc" + File.separator
			+ "mi.properties";

	// Command-line argument-related constants
	public static final String CMD_ARG_FLAG_CLI = "cli";

	public static final String CMD_ARG_FLAG_URI = "uri";

	public static final String CMD_ARG_FLAG_CONFIG_PROPS = "c";

	public static final String CMD_ARG_FLAG_NEIGHBORS = "n";

	public static final String CMD_ARG_FLAG_MANAGERS = "b";

	public static final String CMD_ARG_FLAG_HEARTBEAT = "h";

	public static final String CMD_ARG_FLAG_SUB_COVERING = "s";

	public static final String CMD_ARG_FLAG_ADV_COVERING = "a";

	public static final String CMD_ARG_FLAG_PUB_CONFORM = "pubc";

	public static final String CMD_ARG_FLAG_DB_PROPS = "d";

	public static final String CMD_ARG_FLAG_CYCLES = "cy";

	public static final String CMD_ARG_FLAG_RETRY_LIMIT = "rl";

	public static final String CMD_ARG_FLAG_RETRY_PAUSE = "rp";

	public static final String CMD_ARG_FLAG_LOG_LOCATION = "ll";

	public static final String CMD_ARG_FLAG_MI = "m";

	public static final String CMD_ARG_FLAG_MI_PROPS = "mf";

	public static final String CMD_ARG_FLAG_MON_BI = "mon.bi";

	public static final String CMD_ARG_FLAG_MON_BI_ASINFO = "mon.bi.advsubinfo";

	public static final String CMD_ARG_FLAG_ORDER = "ord";

	private static final String CMD_ARG_FLAG_TOPK = "topk";

	protected String brokerURI = "socket://localhost:1100/BrokerA";

	protected String[] neighborURIs;

	protected int connectionRetryLimit = 30;

	protected int connectionRetryPause = 10; // seconds

	protected String[] managers;

	protected MatcherType matcherName = MatcherType.NewRete;

	protected String dbPropertyFileName = DBConnector.DEFAULT_DB_PROPS_FILE_PATH;

	protected CycleType cycleOption = CycleType.OFF;

	protected SubCoveringType subCovering = SubscriptionFilter.DEFAULT_COVERING_OPTION;

	protected AdvCoveringType advCovering = AdvertisementFilter.DEFAULT_COVERING_OPTION;

	protected boolean pubConformCheck = true;

	protected boolean heartBeat = false;

	protected boolean msgTrace = false;

	protected boolean cliInterface = false;

	protected boolean webInterface = false;

	protected int webPort = 9090;

	protected String webDirectory = ManagementServer.WEB_DIR;

	protected boolean managementInterface = false;

	protected String managementPropertyFileName = DEFAULT_MI_PROPS_FILE_PATH;

	protected String logDir = null;

	protected boolean monitorBrokerInfo = false;

	protected AdvSubInfoType monitorBrokerInfoAdvSubInfoType = AdvSubInfoType.FULL;

	protected boolean totalOrder = false;

	private boolean topk = false;

	private TopkInfo topkInfo = null;

	public BrokerConfig() throws BrokerCoreException {
		Properties properties;
		try {
			properties = loadProperties(DEFAULT_PROPS_FILE_PATH, null);
			convertProperties(properties);
		} catch (IOException e) {
			throw new BrokerCoreException("Cannot load broker properties file: ", e);
		}
	}

	public BrokerConfig(String configPath, boolean def) throws BrokerCoreException {
		// Load the properties file into the Properties object
		try {
			Properties properties;
			
			if(def) {
				Properties defaultProperties = loadProperties(DEFAULT_PROPS_FILE_PATH, null);
				properties = loadProperties(configPath, defaultProperties);
			} else {
				properties = loadProperties(configPath, null);
			}
			
			convertProperties(properties);
		} catch (IOException e) {
			throw new BrokerCoreException("Cannot load broker properties file: ", e);
		}
	}

	
	public BrokerConfig(String configPath) throws BrokerCoreException {
		this(configPath, true);
	}

	public BrokerConfig(BrokerConfig origConfig) {
		this.brokerURI = origConfig.brokerURI;
		this.neighborURIs = Arrays.copyOf(origConfig.neighborURIs, origConfig.neighborURIs.length);
		this.connectionRetryLimit = origConfig.connectionRetryLimit;
		this.connectionRetryPause = origConfig.connectionRetryPause;
		this.managers = Arrays.copyOf(origConfig.managers, origConfig.managers.length);
		this.matcherName = origConfig.matcherName;
		this.dbPropertyFileName = origConfig.dbPropertyFileName;
		this.cycleOption = origConfig.cycleOption;
		this.subCovering = origConfig.subCovering;
		this.advCovering = origConfig.advCovering;
		this.pubConformCheck = origConfig.pubConformCheck;
		this.heartBeat = origConfig.heartBeat;
		this.msgTrace = origConfig.msgTrace;
		this.cliInterface = origConfig.cliInterface;
		this.webInterface = origConfig.webInterface;
		this.webPort = origConfig.webPort;
		this.webDirectory = origConfig.webDirectory;
		this.managementInterface = origConfig.managementInterface;
		this.managementPropertyFileName = origConfig.managementPropertyFileName;
		this.logDir = origConfig.logDir;
		this.monitorBrokerInfo = origConfig.monitorBrokerInfo;
		this.monitorBrokerInfoAdvSubInfoType = origConfig.monitorBrokerInfoAdvSubInfoType;
		this.totalOrder = origConfig.totalOrder;
		this.topk = origConfig.topk;
		this.topkInfo = origConfig.topkInfo;
	}


	protected Properties loadProperties(String propertyFile, Properties defaultProperties)
			throws IOException {
		Properties properties = new Properties(defaultProperties);
		InputStream propFileStream = new FileInputStream(propertyFile);
		properties.load(propFileStream);
		propFileStream.close();
		return properties;
	}

	protected void convertProperties(Properties properties) {
		// convert the properties into configuration parameters
		brokerURI = properties.getProperty("padres.uri");

		String neighborList = properties.getProperty("padres.remoteBrokers");
		if (neighborList != null) {
			neighborURIs = neighborList.split(",\\s*");
		} else {
			neighborURIs = new String[0];
		}

		connectionRetryLimit = Integer.parseInt(properties.getProperty(
				"padres.remoteBrokers.retry.limit", "30"));

		connectionRetryPause = Integer.parseInt(properties.getProperty(
				"padres.remoteBrokers.retry.pauseTime", "10"));

		String managerList = properties.getProperty("padres.managers");
		if (managerList == null)
			managers = new String[0];
		else
			managers = managerList.split(",\\s*");

		matcherName = MatcherType.valueOf(properties.getProperty("padres.routerfactory", "NewRete"));

		dbPropertyFileName = properties.getProperty("padres.dbpropertyfile");
		if (dbPropertyFileName == null)
			dbPropertyFileName = DBConnector.DEFAULT_DB_PROPS_FILE_PATH;

		pubConformCheck = properties.getProperty("padres.pub.conformcheck").equals("ON") ? true
				: false;

		cycleOption = CycleType.valueOf(properties.getProperty("padres.cycles", "OFF"));
		if (cycleOption != CycleType.OFF)
			pubConformCheck = true;

		subCovering = SubCoveringType.valueOf(properties.getProperty(
				"padres.covering.subscription", "OFF"));

		advCovering = AdvCoveringType.valueOf(properties.getProperty(
				"padres.covering.advertisement", "OFF"));

		heartBeat = properties.getProperty("padres.heartbeat").equals("ON") ? true : false;

		msgTrace = properties.getProperty("padres.traceall").equals("ON") ? true : false;

		cliInterface = properties.getProperty("padres.consoleinterface").equals("ON") ? true
				: false;

		webInterface = properties.getProperty(ManagementServer.PROP_WEB_MANAGEMENT).equals("ON")
				? true : false;

		webPort = Integer.parseInt(properties.getProperty(ManagementServer.PROP_WEB_PORT,
				ManagementServer.DEFAULT_WEB_PORT));

		webDirectory = properties.getProperty(ManagementServer.PROP_WEB_DIR);
		if (webDirectory == null)
			webDirectory = ManagementServer.WEB_DIR;

		managementInterface = properties.getProperty("padres.managementinterface").equals("ON")
				? true : false;

		managementPropertyFileName = properties.getProperty("padres.mipropertyfile");
		if (managementPropertyFileName == null)
			managementPropertyFileName = DEFAULT_MI_PROPS_FILE_PATH;

		monitorBrokerInfo = properties.getProperty(BrokerInfoPublisher.PROP_BROKER_INFO,
				BrokerInfoPublisher.BROKER_INFO_OFF).equals(BrokerInfoPublisher.BROKER_INFO_ON)
				? true : false;

		monitorBrokerInfoAdvSubInfoType = AdvSubInfoType.valueOf(properties.getProperty(
				SystemMonitor.PROP_ADV_SUB_INFO_TYPE, AdvSubInfoType.FULL.toString()));

		totalOrder = properties.getProperty("padres.order").equals("ON") ? true : false;
		
		topk = properties.getProperty("padres.topk").equals("ON") ? true : false;
		
		topkInfo = new TopkInfo(Integer.parseInt(properties.getProperty("padres.k", "0")), 
								Integer.parseInt(properties.getProperty("padres.W", "0")),
								Integer.parseInt(properties.getProperty("padres.shift", "0")),
								Integer.parseInt(properties.getProperty("padres.chunk", "0")));
	}

	public static String[] getCommandLineKeys() {
		List<String> cliKeys = new ArrayList<String>();
		cliKeys.add(CMD_ARG_FLAG_CONFIG_PROPS + ":");
		cliKeys.add(CMD_ARG_FLAG_CLI);
		cliKeys.add(CMD_ARG_FLAG_URI + ":");
		cliKeys.add(CMD_ARG_FLAG_NEIGHBORS + ":");
		cliKeys.add(CMD_ARG_FLAG_RETRY_LIMIT + ":");
		cliKeys.add(CMD_ARG_FLAG_RETRY_PAUSE + ":");
		cliKeys.add(CMD_ARG_FLAG_MANAGERS + ":");
		cliKeys.add(CMD_ARG_FLAG_HEARTBEAT + ":");
		cliKeys.add(CMD_ARG_FLAG_DB_PROPS + ":");
		cliKeys.add(CMD_ARG_FLAG_CYCLES + ":");
		cliKeys.add(CMD_ARG_FLAG_SUB_COVERING + ":");
		cliKeys.add(CMD_ARG_FLAG_ADV_COVERING + ":");
		cliKeys.add(CMD_ARG_FLAG_PUB_CONFORM + ":");
		cliKeys.add(CMD_ARG_FLAG_MI + ":");
		cliKeys.add(CMD_ARG_FLAG_MI_PROPS + ":");
		cliKeys.add(CMD_ARG_FLAG_LOG_LOCATION + ":");
		cliKeys.add(CMD_ARG_FLAG_MON_BI + ":");
		cliKeys.add(CMD_ARG_FLAG_MON_BI_ASINFO + ":");
		cliKeys.add(CMD_ARG_FLAG_ORDER + ":");
		cliKeys.add(CMD_ARG_FLAG_TOPK + ":");
		return cliKeys.toArray(new String[0]);
	}

	protected void overwriteWithCmdLineArgs(CommandLine cmdLine) {
		String buffer = null;
		if (cmdLine.isSwitch(CMD_ARG_FLAG_CLI))
			cliInterface = true;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_URI)) != null)
			brokerURI = buffer.trim();
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_NEIGHBORS)) != null)
			neighborURIs = buffer.trim().split(",");
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_RETRY_LIMIT)) != null)
			connectionRetryLimit = Integer.parseInt(buffer);
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_RETRY_PAUSE)) != null)
			connectionRetryPause = Integer.parseInt(buffer);
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_MANAGERS)) != null)
			managers = buffer.trim().split(",");
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_PUB_CONFORM)) != null)
			pubConformCheck = buffer.trim().equals("ON") ? true : false;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_HEARTBEAT)) != null)
			heartBeat = buffer.trim().equals("ON") ? true : false;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_DB_PROPS)) != null)
			dbPropertyFileName = buffer.trim();
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_CYCLES)) != null) {
			cycleOption = CycleType.valueOf(buffer.trim());
			if (cycleOption != CycleType.OFF)
				pubConformCheck = true;
		}
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_SUB_COVERING)) != null)
			subCovering = SubCoveringType.valueOf(buffer.trim());
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_ADV_COVERING)) != null)
			advCovering = AdvCoveringType.valueOf(buffer.trim());
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_MI)) != null)
			managementInterface = buffer.trim().equals("ON") ? true : false;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_MI_PROPS)) != null)
			managementPropertyFileName = buffer.trim();
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_LOG_LOCATION)) != null)
			logDir = buffer.trim();
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_MON_BI)) != null)
			monitorBrokerInfo = buffer.trim().equals(BrokerInfoPublisher.BROKER_INFO_ON) ? true
					: false;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_MON_BI_ASINFO)) != null)
			monitorBrokerInfoAdvSubInfoType = AdvSubInfoType.valueOf(buffer.trim());
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_ORDER)) != null)
			totalOrder = buffer.trim().equals("ON") ? true : false;
		if ((buffer = cmdLine.getOptionValue(CMD_ARG_FLAG_TOPK)) != null)
			topk = buffer.trim().equals("ON") ? true : false;
	}

	public boolean checkConfig() throws BrokerCoreException {
		if (brokerURI == null || brokerURI.length() == 0) {
			throw new BrokerCoreException("Missing uri key or uri value in the property file.");
		}
		return true;
	}

	public String getBrokerURI() {
		return brokerURI;
	}

	public String[] getNeighborURIs() {
		return neighborURIs;
	}

	public int getConnectionRetryLimit() {
		return connectionRetryLimit;
	}

	public int getConnectionRetryPause() {
		return connectionRetryPause;
	}

	public String[] getManagers() {
		return managers;
	}

	public MatcherType getMatcherName() {
		return matcherName;
	}

	public String getDbPropertyFileName() {
		return dbPropertyFileName;
	}

	public CycleType getCycleOption() {
		return cycleOption;
	}

	public boolean isCycle() {
		return cycleOption == CycleType.DYNAMIC || cycleOption == CycleType.FIXED;
	}

	public SubCoveringType getSubCovering() {
		return subCovering;
	}

	public AdvCoveringType getAdvCovering() {
		return advCovering;
	}

	public AdvSubInfoType getAdvSubInfoType() {
		return monitorBrokerInfoAdvSubInfoType;
	}

	public boolean isPubConformCheck() {
		return pubConformCheck;
	}

	public boolean isHeartBeat() {
		return heartBeat;
	}

	public boolean isMsgTrace() {
		return msgTrace;
	}

	public boolean isCliInterface() {
		return cliInterface;
	}

	public boolean isWebInterface() {
		return webInterface;
	}

	public int getWebPort() {
		return webPort;
	}

	public String getWebDirectory() {
		return webDirectory;
	}

	public boolean isManagementInterface() {
		return managementInterface;
	}

	public boolean isBrokerInfoOn() {
		return monitorBrokerInfo;
	}

	public String getManagementPropertyFileName() {
		return managementPropertyFileName;
	}

	public String getLogDir() {
		return logDir;
	}

	public void setBrokerURI(String brokerURI) {
		this.brokerURI = brokerURI;
	}

	public void setNeighborURIs(String[] neighborURIs) {
		this.neighborURIs = neighborURIs;
	}

	public void setConnectionRetryLimit(int connectionRetryLimit) {
		this.connectionRetryLimit = connectionRetryLimit;
	}

	public void setConnectionRetryPause(int connectionRetryPause) {
		this.connectionRetryPause = connectionRetryPause;
	}

	public void setManagers(String[] managers) {
		this.managers = managers;
	}

	public void setMatcherName(MatcherType matcherName) {
		this.matcherName = matcherName;
	}

	public void setDbPropertyFileName(String dbPropertyFileName) {
		this.dbPropertyFileName = dbPropertyFileName;
	}

	public void setCycleType(CycleType cycleType) {
		this.cycleOption = cycleType;
	}

	public void setSubCovering(SubCoveringType subCovering) {
		this.subCovering = subCovering;
	}

	public void setAdvCovering(AdvCoveringType advCoveringType) {
		this.advCovering = advCoveringType;
	}

	public void setHeartBeat(boolean heartBeat) {
		this.heartBeat = heartBeat;
	}

	public void setMsgTrace(boolean msgTrace) {
		this.msgTrace = msgTrace;
	}

	public void setCliInterface(boolean cliInterface) {
		this.cliInterface = cliInterface;
	}

	public void setWebInterface(boolean webInterface) {
		this.webInterface = webInterface;
	}

	public void setWebDirectory(String webDirectory) {
		this.webDirectory = webDirectory;
	}

	public void setManagementInterface(boolean managementInterface) {
		this.managementInterface = managementInterface;
	}

	public void setManagementPropertyFileName(String managementPropertyFileName) {
		this.managementPropertyFileName = managementPropertyFileName;
	}

	public void setMonitorBrokerInfo(boolean flag) {
		monitorBrokerInfo = flag;
	}

	public boolean isTotalOrder() {
		return totalOrder;
	}

	public void setTotalOrder(boolean totalOrder) {
		this.totalOrder = totalOrder;
	}

	public String toString() {
		String outString = "\nBroker URI: " + brokerURI;
		outString += "\nNeighbors: " + Arrays.toString(neighborURIs);
		outString += "\nConnectionRetryLimit: " + connectionRetryLimit;
		outString += "\nConnectionRetryPause: " + connectionRetryPause;
		outString += "\nManagers: " + Arrays.toString(managers);
		outString += "\nMatcherName: " + matcherName;
		outString += "\nDB prop file: " + dbPropertyFileName;
		outString += "\nCycle: " + cycleOption;
		outString += "\nSub. Covering: " + subCovering;
		outString += "\nAdv. Covering: " + advCovering;
		outString += "\nHeart Beat: " + heartBeat;
		outString += "\nMsg Trace: " + msgTrace;
		outString += "\nCLI Interface: " + cliInterface;
		outString += "\nWeb Interface: " + webInterface;
		outString += "\nWeb Port: " + webPort;
		outString += "\nWeb Dir: " + webDirectory;
		outString += "\nMgnt. Interface: " + managementInterface;
		outString += "\nMgnt. prop. file: " + managementPropertyFileName;
		outString += "\nTotal Order: " + totalOrder;
		outString += "\nTop-k: " + topk;
		return outString + "\n";
	}

	public boolean isTopk() {
		return topk ;
	}

	public TopkInfo getTopk() {
		return topkInfo ;
	}
}

package ca.utoronto.msrg.padres.broker.management.web;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.management.CommandHandler;

public class ManagementServer {

	public static final String PROP_WEB_MANAGEMENT = "padres.management";

	public static final String PROP_WEB_PORT = "padres.management.port";

	public static final String PROP_WEB_DIR = "padres.management.dir";

	public static final String DEFAULT_WEB_PORT = "9090";

	public static final String WEB_DIR = BrokerConfig.PADRES_HOME + "etc/web/management";

	private BrokerCore brokerCore;

	private CommandHandler cmdHandler;

	private static String webDir;

	public ManagementServer(BrokerCore brokerCore, CommandHandler cmdHandler) {
		this.brokerCore = brokerCore;
		this.cmdHandler = cmdHandler;
		webDir = WEB_DIR;
	}

	public ManagementServer(BrokerCore brokerCore) {
		this.brokerCore = brokerCore;
		this.cmdHandler = new CommandHandler(brokerCore);
	}

	public void start() {
		BrokerConfig brokerConfig = brokerCore.getBrokerConfig();
		webDir = brokerConfig.getWebDirectory() + "/";
		SimpleServer server = new SimpleServer(brokerConfig.getWebPort(), this);
		server.startServer();
	}

	public String getWebDir() {
		return webDir;
	}

	public CommandHandler getCmdHandler() {
		return cmdHandler;
	}

}

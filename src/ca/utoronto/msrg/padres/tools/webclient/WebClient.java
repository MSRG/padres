package ca.utoronto.msrg.padres.tools.webclient;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.CommandResult;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.tools.webclient.services.PageService;

public class WebClient extends Client {

	// simpleServer must be static to function properly
	private static SimpleServer simpleServer = null;

	private List<Message> notifications = new LinkedList<Message>();

	public WebClient(WebClientConfig userConfig) throws ClientException {
		super(userConfig);
	}

	protected void initialize(ClientConfig userConfig) throws ClientException {
		super.initialize(userConfig);
		// start a simple HTTP server
		WebClientConfig webConfig = (WebClientConfig) clientConfig;
		simpleServer = new SimpleServer(webConfig.webPort, this);
		PageService.setDefaultPage(webConfig.startPage);
	}

	@Override
	public void shutdown() throws ClientException {
		super.shutdown();
		System.exit(0);
	}

	public String getWebDir() {
		return ((WebClientConfig) clientConfig).webDir;
	}

	public void run() {
		simpleServer.startServer();
	}

	public void processMessage(Message msg) {
		notifications.add(msg);
	}

	public CommandResult handleCommand(String commandString) throws ParseException {
		commandString = commandString.trim();
		CommandResult results = new CommandResult(commandString);
		if (results.command.equals("getid")) {
			results.resString = clientID;
		} else if (results.command.equals("getnotifications")) {
			if (notifications.size() > 0) {
				results.resString = notifications.remove(0).toString();
			} else {
				results.resString = "";
			}
		} else {
			return super.handleCommand(commandString);
		}
		return results;
	}

	public static void main(String[] list) throws Exception {
		try {
			CommandLine cmdLine = new CommandLine(WebClientConfig.getCommandLineKeys());
			cmdLine.processCommandLine(list);
			String configFile = cmdLine.getOptionValue(WebClientConfig.CLI_OPTION_CONFIG_FILE,
					WebClientConfig.WEB_CONFIG_FILE_PATH);
			WebClientConfig userConfig = new WebClientConfig(configFile);
			userConfig.overwriteWithCmdLineArgs(cmdLine);
			WebClient webClient = new WebClient(userConfig);
			Map<String, String> brokers = webClient.getBrokerConnections();
			if (brokers.size() > 0) {
				System.out.print("Connetcted to: ");
				for (String brokerID : brokers.keySet()) {
					System.out.printf("%s[%s] ", brokerID, brokers.get(brokerID));
				}
			}
			System.out.println("Running WebClient");
			webClient.run();
		} catch (ClientException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
}

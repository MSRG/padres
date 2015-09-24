package ca.utoronto.msrg.padres.tools.cliclient;

import java.io.Console;
import java.util.Map;

import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.CommandResult;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;

public class CLIClient extends Client {

	public static final String ANSI_CLS;

	public static final String ANSI_NORMAL;

	public static final String ANSI_RED;

	public static final String ANSI_GREEN;

	public static final String ANSI_BLUE;

	protected static final String PROMPT;

	private static final String CONFIG_FILE_PATH = String.format(
			"%s/etc/cliclient/client.properties", ClientConfig.PADRES_HOME);

	// giving some color to the command line interface
	static {
		if (System.getProperty("os.name").toLowerCase().contains("linux")) {
			ANSI_CLS = "\u001b[2J";
			ANSI_NORMAL = "\u001b[0m";
			ANSI_RED = "\u001b[31m";
			ANSI_GREEN = "\u001b[32m";
			ANSI_BLUE = "\u001b[34m";
			PROMPT = ANSI_BLUE + "Client[%s] >> " + ANSI_NORMAL;
		} else {
			ANSI_CLS = ANSI_NORMAL = ANSI_RED = ANSI_GREEN = ANSI_BLUE = "";
			PROMPT = "Client[%s] >> ";
		}
	}

	public CLIClient(String id) throws ClientException {
		this(new ClientConfig());
		clientID = id;
	}

	public CLIClient(ClientConfig clientConfig) throws ClientException {
		super(clientConfig);
		addCommandHandler(new CLICommandHandler(this));
	}

	public void run() {
		System.out.println("Use 'exit' or 'ctrl+d' to quit\n");
		Console con = System.console();
		while (true) {
			String input = con.readLine(PROMPT, clientID);
			if (input == null) {
				// got an EOF (Ctrl-D); exit the shell and quit probably we should quit the shell
				// and put the broker in the background?
				break;
			}
			// run the command, terminate if it returns false
			System.out.println();
			CommandResult result;
			try {
				result = handleCommand(input);
			} catch (ParseException e) {
				e.printStackTrace();
				continue;
			}
			printResults(result);
			if (result.command.equals("exit"))
				break;
		}
		shutdown();
	}

	public void processMessage(Message msg) {
		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage) msg).getPublication();
			if (pub.getClassVal().equals("ERROR")) {
				System.out.printf("%s\n\n---\nReceived an Error from %s\n", ANSI_RED,
						msg.getLastHopID());
				System.out.println(pub.getPairMap().get("message"));
				System.out.println("---\n" + ANSI_NORMAL);
				System.out.printf(PROMPT, clientID);
				return;
			}
		}
		System.out.printf("%s\n\n---\nReceived a %s from %s\n", ANSI_GREEN, msg.getType(),
				msg.getLastHopID());
		System.out.println(msg);
		System.out.println("---\n" + ANSI_NORMAL);
		System.out.printf(PROMPT, clientID);
	}

	@Override
	public void printResults(CommandResult results) {
		if (results.isError())
			System.err.println(results.errMsg);
		else if (results.resString != null && !results.resString.equals(""))
			System.out.println(results.resString);
	}

	public void shutdown() {
		System.out.printf("Terminating the client %s\n\n", clientID);
		try {
			super.shutdown();
		} catch (ClientException e) {
			System.err.println(e.getMessage());
		}
		System.exit(0);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		try {
			CommandLine cmdLine = new CommandLine(ClientConfig.getCommandLineKeys());
			cmdLine.processCommandLine(args);
			String configFile = cmdLine.getOptionValue(ClientConfig.CLI_OPTION_CONFIG_FILE,
					CONFIG_FILE_PATH);
			ClientConfig userConfig = new ClientConfig(configFile);
			userConfig.overwriteWithCmdLineArgs(cmdLine);
			CLIClient cliClient = new CLIClient(userConfig);
			Map<String, String> brokers = cliClient.getBrokerConnections();
			System.out.print("Connetcted to: ");
			for (String brokerID : brokers.keySet()) {
				System.out.printf("%s[%s] ", brokerID, brokers.get(brokerID));
			}
			System.out.println();
			cliClient.run();
		} catch (ClientException e) {
			System.err.println(ANSI_RED + e.getMessage() + ANSI_NORMAL);
			System.exit(1);
		}
	}

}

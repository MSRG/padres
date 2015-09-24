package ca.utoronto.msrg.padres.client;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.Utils;

public class BaseCommandHandler extends CommandHandler {

	String localhostIPAddr = null;

	public BaseCommandHandler(Client client) {
		super(client);
		try {
			localhostIPAddr = CommSystem.getLocalIPAddr();
		} catch (CommunicationException e) {
			// Do nothing.  localhostIPAddr remains null.
		}
	}

	@Override
	protected void initCommandHelps() {
		commandHelps.put("exit", "Terminate and Exit.");
		commandHelps.put("setid", "Set the ID of the client. Usage: setid <client_id>");
		commandHelps.put("help", "Print a help screen. Usage: help [command]");
		commandHelps.put("info", "Show the system information of the client.");
		commandHelps.put("brokerinfo", "Prints the list of connected brokers. "
				+ " Usage: brokerinfo broker_id");
		commandHelps.put("printsubs", "Show subscriptions subscribed by the client (on a broker.)"
				+ " Usage: printsubs [broker_id]");
		commandHelps.put("printadvs", "Show advertisements sent out by the client (on a broker.)"
				+ " Usage: printadvs [broker_id]");
		commandHelps.put("connect", "Connect to a specific Broker. Usage: connect <broker_URI>");
		commandHelps.put("disconnect",
				"Disconnect from a specific Broker. Usage: disconnect <broker_ID>");
		commandHelps.put("sub",
				"Inject a subscription. Usage: sub <subscription string> [broker_id]");
		commandHelps.put("csub",
				"Inject a composite subscription. Usage: csub <composite subscription string> [broker_id]");
		commandHelps.put("pub", "Inject a publication."
				+ " Usage: pub publication_string [broker_id]");
		commandHelps.put("adv", "Inject an advertisement."
				+ " Usage: adv advertisement_string [broker_id]");
		commandHelps.put("unsub", "Inject an unsubscription. "
				+ "Usage: unsub subscription ID1[,subscription ID2,..]");
		commandHelps.put("unadv", "Inject an unadvertisement."
				+ " Usage: unadv advertisement_ID1[,advertisement_ID2,..]");
		commandHelps.put("batch",
				"Process a batch of commands from a text file. Usage: batch <filename>");
	}

	@Override
	protected void initCommandAliases() {
		commandAlias.put("quit", "exit");
		commandAlias.put("c", "connect");
		commandAlias.put("dc", "disconnect");
		commandAlias.put("a", "adv");
		commandAlias.put("s", "sub");
		commandAlias.put("cs", "csub");
		commandAlias.put("p", "pub");
	}

	@Override
	public void runCommand(CommandResult cmd) throws ParseException {
		if (commandAlias.containsKey(cmd.command))
			cmd.command = commandAlias.get(cmd.command);
		try {
			if (cmd.command.equals("setid")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Client ID is not provided");
				client.setClientID(cmd.cmdData[0]);
			} else if (cmd.command.equals("exit")) {
				cmd.resString = "exiting...";
				// do nothing specific
			} else if (cmd.command.equals("help")) {
				getHelp(cmd);
			} else if (cmd.command.equals("info")) {
				getInfo(cmd);
			} else if (cmd.command.equals("brokerinfo")) {
				getBrokerConnectionsInfo(cmd);
			} else if (cmd.command.equals("printadvs")) {
				printAdvertisements(cmd);
			} else if (cmd.command.equals("printsubs")) {
				printSubscriptions(cmd);
			} else if (cmd.command.equals("connect")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("BrokerURI is not provided");
				BrokerState brokerState = client.connect(cmd.cmdData[0]);
				cmd.resString = String.format("Connected to %s",
						brokerState.getBrokerAddress().getNodeURI());
			} else if (cmd.command.equals("disconnect")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("BrokerURI is not provided");
				BrokerState brokerState = client.disconnect(cmd.cmdData[0]);
				cmd.resString = String.format("Disconnected from %s",
						brokerState.getBrokerAddress().getNodeURI());
			} else if (cmd.command.equals("adv")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: adv <adv_string> [broker_id]");
				String brokerURI = cmd.cmdData.length > 1 ? cmd.cmdData[1] : null;
				AdvertisementMessage advMsg = client.advertise(cmd.cmdData[0], brokerURI);
				cmd.resString = advMsg.toString();
			} else if (cmd.command.equals("sub")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: sub <sub_string> [broker_id]");
				String brokerURI = cmd.cmdData.length > 1 ? cmd.cmdData[1] : null;
				SubscriptionMessage subMsg = client.subscribe(cmd.cmdData[0], brokerURI);
				cmd.resString = subMsg.toString();
			} else if (cmd.command.equals("csub")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: csub <csub_string> [broker_id]");
				String brokerURI = cmd.cmdData.length > 1 ? cmd.cmdData[1] : null;
				cmd.resString = client.subscribeCS(cmd.cmdData[0], brokerURI).toString();
			} else if (cmd.command.equals("pub")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: pub <pub_string> [broker_id]");
				String brokerURI = cmd.cmdData.length > 1 ? cmd.cmdData[1] : null;
				cmd.resString = client.publish(cmd.cmdData[0], brokerURI).toString();
			} else if (cmd.command.equals("unsub")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: unsub <sub_id[,sub_id...]>");
				List<UnsubscriptionMessage> unSubMsgs = client.unSubscribe(cmd.cmdData[0].split(",\\s*"));
				cmd.resString = Utils.messageListToString(unSubMsgs);
			} else if (cmd.command.equals("unadv")) {
				if (cmd.cmdData == null || cmd.cmdData.length == 0
						|| cmd.cmdData[0].trim().equals(""))
					throw new ClientException("Usage: unadv <adv_id[,adv_id...]>");
				List<UnadvertisementMessage> unAdvMsgs = client.unAdvertise(cmd.cmdData[0].split(",\\s*"));
				cmd.resString = Utils.messageListToString(unAdvMsgs);
			} else if (cmd.command.equals("batch")) {
				batchProcess(cmd);
			} else {
				cmd.errMsg = "Unrecognized pub/sub command: " + cmd.command;
			}
		} catch (ClientException e) {
			cmd.errMsg = e.getMessage();
		}
	}

	protected void getHelp(CommandResult cmd) {
		if (cmd.cmdData == null || cmd.cmdData.length == 0 || cmd.cmdData[0].trim().equals("")) {
			String[] cmds = client.getAvailableCommands().toArray(new String[0]);
			Arrays.sort(cmds);
			cmd.resString = "Available Commands:";
			for (int i = 0; i < cmds.length - 1; i++) {
				if (i % 5 == 0)
					cmd.resString += "\n     ";
				cmd.resString += cmds[i] + ", ";
			}
			cmd.resString += cmds[cmds.length - 1] + "\n";
			cmd.resString += "For more info use 'help <command>'\n";
			return;
		}
		String help = client.getCommandHelp(cmd.cmdData[0]);
		if (help == null) {
			cmd.errMsg = "there is no such command: " + cmd.cmdData[0] + "\n";
		} else {
			cmd.resString = String.format("%s:\n    %s\n", cmd.cmdData[0], help);
		}
	}

	protected void getInfo(CommandResult cmd) {
		cmd.resString = "System Info:\n";
		cmd.resString += String.format("\tClient ID: %s\n", client.getClientID());
		cmd.resString += String.format("\tJVM Version: %s\n", System.getProperty("java.vm.version"));
		cmd.resString += String.format("\tOS: %s\n", System.getProperty("os.name"));
		cmd.resString += String.format("\tArchitecture: %s\n", System.getProperty("os.arch"));
		cmd.resString += String.format("\tKernel Version: %s\n", System.getProperty("os.version"));
	}

	protected void getBrokerConnectionsInfo(CommandResult cmd) {
		cmd.resString = "Broker ID\t\tBroker URI\n";
		cmd.resString += "---------------------------------------------------------------------\n";
		Map<String, String> brokerList = client.getBrokerConnections();
		String[] idList = brokerList.keySet().toArray(new String[0]);
		Arrays.sort(idList);
		for (String brokerID : idList) {
			cmd.resString += String.format("%s\t\t%s\n", brokerID, brokerList.get(brokerID));
		}
	}

	protected void printSubscriptions(CommandResult cmd) {
		try {
			Map<String, SubscriptionMessage> subList = client.getSubscriptions();
			if (subList.size() > 0) {
				cmd.resString = "Subscriptions:\n";
				cmd.resString += Utils.messageMapToString(subList);
			} else {
				cmd.errMsg = "No subscriptions found\n";
			}
		} catch (ClientException e) {
			cmd.errMsg = e.getMessage();
		}
	}

	protected void printAdvertisements(CommandResult cmd) {
		try {
			Map<String, AdvertisementMessage> advList = client.getAdvertisements();
			if (advList.size() > 0) {
				cmd.resString = "Advertisements:\n";
				cmd.resString += Utils.messageMapToString(advList);
			} else {
				cmd.errMsg = "No advertisements found\n";
			}
		} catch (ClientException e) {
			cmd.errMsg = e.getMessage();
		}
	}

	protected void batchProcess(CommandResult cmd) {
		if (cmd.cmdData == null || cmd.cmdData.length == 0 || cmd.cmdData[0].trim().equals("")) {
			System.err.println("Usage: batch <file_name>");
			return;
		}
		try {
			BufferedReader in = new BufferedReader(new FileReader(cmd.cmdData[0]));
			String inLine = in.readLine();

			while (inLine != null) {
				// Replace variables in the command.
				if (localhostIPAddr != null) {
					try {
						inLine = inLine.replaceAll("\\$\\{localhostIP\\}", localhostIPAddr);
					} catch (Exception e) {
						cmd.errMsg = e.getMessage();
						System.err.printf("Command variable substitution error for command: %s\n", inLine);
					}
				}

				// Execute the command.
				CommandResult result = client.handleCommand(inLine);
				client.printResults(result);
				if (result.command.equals("exit"))
					break;
				
				inLine = in.readLine();
			}
			in.close();
		} catch (ParseException e) {
			System.err.printf("Command parsing error\n", cmd.cmdData[0]);
		} catch (FileNotFoundException e) {
			System.err.printf("Batch file '%s' not found\n", cmd.cmdData[0]);
		} catch (IOException e) {
			System.err.printf("Error in reading batch file '%s'\n", cmd.cmdData[0]);
		}
	}

}

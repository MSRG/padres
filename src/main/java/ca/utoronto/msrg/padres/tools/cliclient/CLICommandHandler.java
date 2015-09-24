package ca.utoronto.msrg.padres.tools.cliclient;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import ca.utoronto.msrg.padres.client.CommandHandler;
import ca.utoronto.msrg.padres.client.CommandResult;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class CLICommandHandler extends CommandHandler {

	public CLICommandHandler(CLIClient client) throws ClientException {
		super(client);
	}

	@Override
	protected void initCommandHelps() {
		commandHelps.put("cls", "Clear console text.");
	}

	@Override
	public void runCommand(CommandResult cmd) {
		if (cmd.command.equals("cls")) {
			clearScreen();
		}
	}

	protected void clearScreen() {
		System.out.println(CLIClient.ANSI_CLS);
	}

	@Override
	protected void initCommandAliases() {
		// no command aliases; nothing to initialize
	}

}
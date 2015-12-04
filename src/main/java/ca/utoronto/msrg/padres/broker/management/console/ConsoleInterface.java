// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 2-Sep-2003
 *
 */
package ca.utoronto.msrg.padres.broker.management.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.management.CommandHandler;

/**
 * The console interface for the broker. Allows the user to issue commands directly to the broker
 * from the broker's console.
 * 
 * @author eli
 */
public class ConsoleInterface extends Thread {

	public static final String PROP_CONSOLE_INTERFACE = "padres.consoleinterface";

	private BrokerCore brokerCore;

	private CommandHandler cmdHandler;

	/**
	 * Constructor.
	 */
	public ConsoleInterface(BrokerCore broker) {
		super("ConsoleInterface");
		brokerCore = broker;
		cmdHandler = new CommandHandler(brokerCore);
	}

	public void run() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			try {
				System.out.printf("%s >> ", brokerCore.getBrokerID());
				String input = in.readLine();
				if (input == null) {
					// got an EOF (Ctrl-D); exit the shell and quit
					// probably we should quit the shell and put the broker in the background?
					System.out.printf("\nTerminating the Broker %s\n", brokerCore.getBrokerID());
					System.exit(0);
				}
				input = input.trim();
				String[] inputParts = input.split("\\s+");
				String command = inputParts.length > 0 ? inputParts[0] : "";
				if (command.equals(""))
					// pressed an empty "Enter"
					continue;
				if (command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit")) {
					// stop and exit the broker
					System.out.printf("Terminating the Broker %s\n", brokerCore.getBrokerID());
					System.exit(0);
				}

				Properties respProps = cmdHandler.handleCommand(input);
				if (respProps != null) {
					String[] propNames = respProps.stringPropertyNames().toArray(new String[0]);
					Arrays.sort(propNames);
					for (String key : propNames)
						System.out.printf("%15s: %s\n", key, respProps.getProperty(key));
				}

			} catch (IOException e) {
				System.err.println("Error in reading commandline");
			}
		}
	}
}

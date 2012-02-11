package ca.utoronto.msrg.padres.tools.panda.input;

/*
 * Created on May 6, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author Mr. Biggy
 *
 * This objects stores an input command read from the input file or the 
 * deployer's console
 */

import java.util.StringTokenizer;
import java.util.Map;

import ca.utoronto.msrg.padres.common.util.TypeChecker;
import ca.utoronto.msrg.padres.common.util.datastructure.DataTypeException;
import ca.utoronto.msrg.padres.common.util.datastructure.ParseException;
import ca.utoronto.msrg.padres.tools.panda.CommandGenerator;

public abstract class InputCommand {
	protected final double time;

	protected final String command;

	protected final String id;

	private final String nodeIpAddress;

	protected String executableCommand = null;

	// stores the command to be executed on remote machine

	/**
	 * Constructor
	 */
	protected InputCommand(String input, StringTokenizer st) {
		try {
			time = Double.parseDouble(st.nextToken());
			command = st.nextToken();
			id = st.nextToken();
			nodeIpAddress = st.nextToken();
		} catch (Exception e) {
			throw new ParseException("ERROR: (InputCommand) First 4 parameters for " + "input '"
					+ input + "' is malformed.");
		}

		verifyDataTypes(input);
	}

	private void verifyDataTypes(String input) {
		if (!TypeChecker.isString(command) || !TypeChecker.isString(id)
				|| !TypeChecker.isString(nodeIpAddress)) {
			throw new DataTypeException("ERROR: (InputCommand) One of first 4 parameters "
					+ "for input '" + input + "' has invalid data.\n");
		}
	}

	public static InputCommand toSpecificCommand(String line) {
		InputCommand dummyCmd = null;
		InputCommand realCmd = null;

		try {
			dummyCmd = new DummyCommand(line);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}

		// Even if the command and operator are valid, a null result
		// can be returned if the parameters are incorrect
		if (dummyCmd.getCommand().equalsIgnoreCase("ADD")) {
			realCmd = ProcessAddCommand.toProcessAddCommand(line);
		} else if (dummyCmd.getCommand().equalsIgnoreCase("REMOVE")) {
			realCmd = ProcessRemoveCommand.toProcessRemoveCommand(line);
		} else {
			System.out.println("ERROR: (InputCommand) Unrecognized command '"
					+ dummyCmd.getCommand() + ".\n");
			return null;
		}

		dummyCmd = null;

		// Finally, all is ok
		return realCmd;
	}

	/**
	 * Call this to initialize the remote executable command for a command
	 * object. We can't just rely on the extended class to set its parent
	 * variable because they can't see directly and usually don't know about
	 * parent class members.
	 */
	public static boolean initExecutableCommand(InputCommand cmd, CommandGenerator cmdGenerator) {

		try {
			cmd.setExecutableCommand(cmd.getExecutableCommand(cmdGenerator.getCmdTemplateMap()));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// Returns the IP addresss of the node to execute this command.
	public String getNodeIpAddress() {
		return nodeIpAddress;
	}

	/**
	 * This is called by the factory to force extending classes to initialize
	 * the executableCommand member variable.
	 */
	public abstract String getExecutableCommand(Map<Object, String> cmdTemplateMap);

	/**
	 * @return
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return
	 */
	public double getTime() {
		return time;
	}

	/**
	 * @return
	 */
	public String getExecutableCommand() {
		return executableCommand;
	}

	/**
	 * @param string
	 */
	public void setExecutableCommand(String string) {
		executableCommand = string;
	}

}

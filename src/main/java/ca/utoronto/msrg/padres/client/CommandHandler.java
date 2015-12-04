package ca.utoronto.msrg.padres.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * This is base class for any class that handle a particular command issued to a client. Any new
 * client that provides a new user command must have a new CommandHandler class extending this
 * class.
 * 
 * @author Bala Maniymaran
 * 
 *         Created: 2011-05-19
 * 
 */
public abstract class CommandHandler {

	/**
	 * The client for which the command handler provides functionalities.
	 */
	protected Client client;

	/**
	 * Data structure that holds the help strings that explains the usage of each command supported
	 * by a CommandHandler.
	 */
	protected Map<String, String> commandHelps;

	/**
	 * Any alias to the commands supported by a CommandHandler.
	 */
	protected Map<String, String> commandAlias;

	public CommandHandler(Client client) {
		this.client = client;
		commandHelps = new HashMap<String, String>();
		commandAlias = new HashMap<String, String>();
		initCommandHelps();
		initCommandAliases();
	}

	public Set<String> getCommandList() {
		return commandHelps.keySet();
	}

	public Map<String, String> getCommandAliases() {
		return commandAlias;
	}

	/**
	 * Checks whether this command handler supports a specific command.
	 * 
	 * @param cmd
	 *            The command to be checked.
	 * @return true if the specified command is supported by this command handler; false otherwise.
	 */
	public boolean supportCommand(String cmd) {
		if (commandAlias.containsKey(cmd))
			cmd = commandAlias.get(cmd);
		return commandHelps.containsKey(cmd);
	}

	public String getHelp(String cmd) {
		if (commandAlias.containsKey(cmd))
			cmd = commandAlias.get(cmd);
		if (commandHelps.containsKey(cmd))
			return commandHelps.get(cmd);
		return null;
	}

	/**
	 * Initialise the help strings for the commands.
	 */
	protected abstract void initCommandHelps();

	/**
	 * Sets the aliases for the commands, if any.
	 */
	protected abstract void initCommandAliases();

	/**
	 * Implements the primary logic in executing a command supported by this command handler.
	 * 
	 * @param cmd
	 *            The command to be executed.
	 * @throws ParseException
	 */
	public abstract void runCommand(CommandResult cmd) throws ParseException ;

}

package ca.utoronto.msrg.padres.client;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandResult {

	/**
	 * This is a string of space separated words"
	 */
	public String cmdString;

	/**
	 * first word in the cmdString
	 */
	public String command;

	/**
	 * second word in the cmdString
	 */
	public String[] cmdData;

	/**
	 * Result string; produced by the command executioner who accepts this object
	 */
	public String resString;

	/**
	 * Error message; produced by the command executioner who accepts this object, if things does
	 * not go right
	 */
	public String errMsg;

	/**
	 * Constructor; accepts the space-separated string of words and process them into command,
	 * cmdData, and cmdOptions.
	 * 
	 * @param cmdString
	 *            A string of space-separated words.
	 */
	public CommandResult(String cmdString) {
		this.cmdString = cmdString;
		try {
			// Look for whitespace separated words, but don't break spaces included in single quotes (e.g., 'Thu Mar 22') 
			List<String> words = new LinkedList<String>();
			Matcher m = Pattern.compile("('[^']+'|\\S)+").matcher(cmdString);
			while (m.find())
				words.add(m.group());
			
			// Sanity check.
			if (words.size() < 1)
				throw new IllegalStateException("Couldn't find any words");

			// Convert to array
			String[] cmdStrParts = words.toArray(new String[0]);
			command = cmdStrParts[0];
			if (cmdStrParts.length > 1) {
				cmdData = Arrays.copyOfRange(cmdStrParts, 1, cmdStrParts.length);
			}
		} catch (IllegalStateException e) {
			errMsg = "Unrecognized input or Syntax error: " + cmdString;
		}
	}

	/**
	 * To check whether an error occurred in launching this command. The error is set by the
	 * executing {@link CommandHandler}.
	 * 
	 * @return true if there is an error, false otherwise.
	 */
	public boolean isError() {
		return errMsg != null && !errMsg.trim().equals("");
	}

	/**
	 * Converts the object into Java Properties object.
	 * 
	 * @return
	 */
	public Properties toProperties() {
		Properties props = new Properties();
		props.put("cmd_string", cmdString);
		props.put("command", command);
		if (cmdData == null || cmdData.length == 0) {
			props.put("cmd_data", "");
		} else {
			String cmdDataString = cmdData[0];
			for (int i = 1; i < cmdData.length; i++)
				cmdDataString += " " + cmdData[i];
			props.put("cmd_data", cmdDataString);
		}
		if (resString == null)
			props.put("success", "");
		else
			props.put("success", resString);
		if (errMsg == null)
			props.put("error", "");
		else
			props.put("error", errMsg);
		return props;
	}

	public String toString() {
		return toProperties().toString();
	}

}

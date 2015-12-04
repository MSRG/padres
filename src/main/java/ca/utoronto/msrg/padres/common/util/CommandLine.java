/**
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "Exolab" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of Exoffice Technologies.  For written permission,
 *    please contact info@exolab.org.
 *
 * 4. Products derived from this Software may not be called "Exolab"
 *    nor may "Exolab" appear in their names without prior written
 *    permission of Exoffice Technologies. Exolab is a registered
 *    trademark of Exoffice Technologies.
 *
 * 5. Due credit should be given to the Exolab Project
 *    (http://www.exolab.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY EXOFFICE TECHNOLOGIES AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * EXOFFICE TECHNOLOGIES OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 1999 (C) Exoffice Technologies Inc. All Rights Reserved.
 *
 * $Id$
 *
 * Date         Author  Changes
 * 1/6/2000     jima    Created
 * 1/9/2000     jima    changed package name from com.comware to org.exolab
 * 7/1/2000     jima    removed the CW prefix from the class name
 * 1/8/2001     jima    Removed it from jtf library and imported it into
 *                      the openjms library
 */

package ca.utoronto.msrg.padres.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This core class is responsible for processing the command line and storing the list of options
 * and parameters specified. The difference between an option and a command line is that an option
 * does not accept any argument (set to true if it is specified and false otherwise), while a
 * parameter always has an associated value.
 * 
 * @author Bala Maniymaran
 * 
 *         Created: 2011-02-07
 * 
 */
public class CommandLine {

	/**
	 * A list of accepted switches. Switches that are given in a command line are set to true, false
	 * otherwise.
	 */
	private Map<String, Boolean> switches = new HashMap<String, Boolean>();

	/**
	 * A dictionary of all the options and their associated values
	 */
	private Map<String, String> options = new HashMap<String, String>();

	/**
	 * List of values that are not associated with any options
	 */
	private List<String> parameters = new ArrayList<String>();

	/**
	 * Construct an instance of this class with that accepts the specified list of options and
	 * switches. Once this class is initialized, the command line arguments can be processed using
	 * {@link #processCommandLine(String[])}
	 * 
	 * @param optionSpecs
	 *            command line argument
	 * 
	 * @see {@link #initOptions(String[])}
	 * 
	 */
	public CommandLine(String[] optionSpecs) {
		if (optionSpecs != null)
			initOptions(optionSpecs);
	}

	/**
	 * Default constructor which simply initialised the class.
	 * 
	 * @see {@link #CommandLineNew(String[])}
	 */
	public CommandLine() {
	}

	/**
	 * Initialize allowed options and switches in the class.
	 * 
	 * @param optionKeys
	 *            Allowed options and switches. Note:
	 *            <ul>
	 *            <li>Both options and switches can be multi-character.</li>
	 *            <li>Neither option nor option are specified here with the "-" character (which is
	 *            necessary at the command line.)</li>
	 *            <li>Options are marked with a trailing ":" character.</li>
	 *            <li>Note that this class does not understand mandatory options/switches.</li>
	 *            </ul>
	 */
	public void initOptions(String[] optionKeys) {
		for (String key : optionKeys) {
			if (key.endsWith(":")) {
				options.put(key.substring(0, key.length() - 1), null);
			} else {
				switches.put(key, false);
			}
		}
	}

	public void reset() {
		for (String switchKey : switches.keySet()) {
			switches.put(switchKey, false);
		}
		for (String optionKey : options.keySet()) {
			options.put(optionKey, null);
		}
		parameters.clear();
	}

	/**
	 * Check if the following option or command has been specified. It can be called only after the
	 * {@link #processCommandLine(String[])} method has been called to process a command line.
	 * 
	 * @param name
	 *            name of option or command
	 * @return boolean true if it has been specified
	 */
	public boolean isSpecified(String name) {
		return isSwitch(name) || isOption(name);
	}

	/**
	 * Check if the following option has been specified. It can be called only after the
	 * {@link #processCommandLine(String[])} method has been called to process a command line.
	 * 
	 * @param name
	 *            name of the option
	 * @return boolean true if it has been specified
	 */
	public boolean isSwitch(String name) {
		return switches.containsKey(name) && switches.get(name) == true;
	}

	/**
	 * Check if the following parameter has been specified. It can be called only after the
	 * {@link #processCommandLine(String[])} method has been called to process a command line.
	 * 
	 * @param name
	 *            name of the parameter
	 * @return boolean true if it has been specified
	 */
	public boolean isOption(String name) {
		return options.containsKey(name) && (options.get(name).length() > 0);
	}

	/**
	 * Return the value of an option. It can be called only after the
	 * {@link #processCommandLine(String[])} method has been called to process a command line.
	 * 
	 * @param name
	 *            name of an option
	 * @return String value of parameter if it is a valid option and specified in the processed
	 *         command line or null if it is not a valid option name or has not specified in the
	 *         processed command line.
	 */
	public String getOptionValue(String name) {
		return options.get(name);
	}

	public String getOptionValue(String name, String defaultValue) {
		if (options.get(name) != null) {
			return options.get(name);
		}
		return defaultValue;
	}

	/**
	 * Returns the list of parameters (values that are not associated with any option) that are
	 * specified in a processed command line. It is better be called after the
	 * {@link #processCommandLine(String[])} method has been called to process a command line.
	 * 
	 * @return List of String, which are the parameters found in the command line.
	 */
	public List<String> getParameters() {
		return parameters;
	}

	/**
	 * Adds an option, switch, or parameter to the CommandLine data structure. Either name or value
	 * can be null, but not both at the same time.
	 * 
	 * @param name
	 *            Name of an option or switch. It must not precede with the "-" character. It must
	 *            be null, if it is a parameter that is being added.
	 * @param value
	 *            The value of an option or parameter. It must be null, if name is a switch.
	 * @return boolean true if the given option/switch/parameter is successfully added
	 * @throws Exception
	 */
	public boolean add(String name, String value) throws Exception {
		if (name == null) {
			if (value != null)
				parameters.add(value);
		} else if (switches.containsKey(name)) {
			if (value != null) {
				throw new Exception("Error: switch '" + name + "' does not accept a value");
			}
			switches.put(name, true);
		} else if (options.containsKey(name)) {
			options.put(name, value);
		} else {
			throw new Exception("Error: option '" + name + "' is not recognized");
		}
		return true;
	}

	/**
	 * This method processes the command line and extracts the list of options and command lines. It
	 * doesn't interpret the meaning of the entities, which is left to the application.
	 * 
	 * @param args
	 *            command line as a collection of tokens
	 * @return true if the command line arguments are successfully processed, false otherwise
	 * @throws Exception
	 */
	public boolean processCommandLine(String[] args) throws Exception {
		for (int index = 0; index < args.length; index++) {
			if (args[index].startsWith("-")) {
				String key = args[index].substring(1);
				if (switches.containsKey(key)) {
					switches.put(key, true);
				} else if (options.containsKey(key)) {
					String optValue = args[++index];
					if (optValue.startsWith("-")) {
						throw new Exception("Error: option '" + key
								+ "' must be followed by a value");
					}
					options.put(key, optValue);
				} else {
					throw new Exception("Warning: option '" + key + "' is not recognized");
				}
			} else {
				parameters.add(args[index]);
			}
		}
		return true;
	}

	public String toString() {
		String outString = "";
		for (String switchKey : switches.keySet()) {
			outString += " -" + switchKey;
		}
		for (String optionKey : options.keySet()) {
			outString += " -" + optionKey + " " + options.get(optionKey);
		}
		for (String param : parameters) {
			outString += " " + param;
		}
		return outString;
	}

}

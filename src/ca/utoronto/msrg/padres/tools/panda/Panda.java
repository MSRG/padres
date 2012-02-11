/*
 * Created on May 2, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

package ca.utoronto.msrg.padres.tools.panda;

/**
 * @author Alex
 * 
 * This is the Padres Automated Node Deployer and Administrator tool, a.k.a.
 * PANDA. You can find more about this in the Wiki page.
 * 
 * You cannot use this program in Windows, as all system commands and shell
 * support is in unix/bash.
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.util.TypeChecker;
import ca.utoronto.msrg.padres.common.util.io.FileOperation;

public class Panda {

	// stores values found in properties file
	private final Properties configProps;

	private boolean isValidInputFile;

	// Final paths
	private final String workingPath;

	private final String helpFilePath;

	private final CommandGenerator cmdGenerator;

	private final Deployer deployer;

	private final ScriptExecutor executor;

	// Some default paths, relative to PADRES_HOME
	public static final String PADRES_HOME = System.getenv("PADRES_HOME") == null ? null
			: System.getenv("PADRES_HOME");

	private static final String DEFAULT_CONFIG_FILE = PADRES_HOME + "/etc/panda/panda.properties";

	private final static String DEFAULT_HELP_FILE_PATH = PADRES_HOME + "/etc/panda/consoleHelp.txt";

	private static final String DEFAULT_WORKING_DIR = System.getProperty("user.home")
			+ "/.padres/panda/";

	public final static String DEFAULT_REMOTE_PADRES_HOME = "/home/padreslab/padres/";

	private final static String DEFAULT_PANDA_EXCEPTION_LOG_PATH = System.getProperty("user.home")
			+ "/.padres/logs/";

	// names used as arguments when running this program
	private static final String CMD_ARG_CONFIG = "c";

	private static final String CMD_ARG_INPUTFILE = "i";

	// Default script names and location
	public static final String FILENAME_SETUP_SCRIPT = "setup";

	public static final String FILENAME_DEPLOY_SCRIPT = "deploy";

	public static final String FILENAME_EXECUTE_SCRIPT = "execute";

	public static final String PATH_LOCAL_SCRIPTS = PADRES_HOME + "/bin/panda/";

	/**
	 * Constructor
	 * 
	 * @param topoFile
	 * @param confFile
	 */
	public Panda(String configFile) {

		configProps = loadConfig(configFile);

		// Then populate our member variables from the properties object
		helpFilePath = configProps.getProperty("panda.helpFile", DEFAULT_HELP_FILE_PATH);

		// create working directory
		workingPath = configProps.getProperty("panda.workdir", DEFAULT_WORKING_DIR).toString()
				+ (new SimpleDateFormat("MMdd.HH.mm.ss")).format(new Date()) + "/";
		FileOperation.mkdir(workingPath);

		// Fetch the padres home setting from config file and put it into config
		// object
		// so that CommandGenerator and ScriptExecutor and retrieve it
		if (!configProps.containsKey("remote.padres.path"))
			System.out.println("Warning: Value of environment variable remote.padres.path"
					+ " not found in properties file, using '" + DEFAULT_REMOTE_PADRES_HOME
					+ "' as default.");
		String remotePadresHome = configProps.getProperty("remote.padres.path",
				DEFAULT_REMOTE_PADRES_HOME);

		configProps.put("remote.padres.path", remotePadresHome);

		// Instantiate the various components
		cmdGenerator = new CommandGenerator(this);
		executor = new ScriptExecutor(this, cmdGenerator);
		deployer = new Deployer(this, executor);
	}

	private Properties loadConfig(String configFile) {
		// Load file into a properties object first
		Properties configProps = new Properties();
		try {
			InputStream confFile = new FileInputStream(configFile);
			configProps.load(confFile);
			confFile.close();
		} catch (FileNotFoundException e1) {
			System.err.println("Failed: (Deployer) Configuration file '" + configFile
					+ "' not found.");
			printUsageAndExit();
		} catch (Exception e) {
			System.err.println("ERROR: Cannot load configuration file: " + configFile + ":\n" + e);
			printUsageAndExit();
		}

		// Search and replace all instances of "~" with value of $HOME
		for (Iterator<Object> i = configProps.keySet().iterator(); i.hasNext();) {
			String key = i.next().toString();
			String value = configProps.get(key).toString();
			if (value.startsWith("~"))
				configProps.put(key, value.replaceFirst("~", System.getProperty("user.home")));
		}
		// Search and replace all instances of "$PADRES_HOME" with actual value
		// of PADRES_HOME
		for (Iterator<Object> i = configProps.keySet().iterator(); i.hasNext();) {
			String key = i.next().toString();
			String value = configProps.get(key).toString();
			if (value.startsWith("$PADRES_HOME"))
				configProps.put(key, value.replaceFirst("\\$PADRES_HOME", PADRES_HOME));
		}

		return configProps;
	}

	/**
	 * This is the console
	 * 
	 */
	private void run(String inputFile) {
		// Load input file and keep a copy of it in working dir if
		// successfully loaded
		try {
			isValidInputFile = (inputFile == null) ? false
					: cmdGenerator.generateCmdsForInputFile(inputFile);
		} catch (ClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Console setup
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Type 'help' or '?' for help.");

		// Console
		while (true) {
			try {
				System.out.print("> ");
				// We will first parse the commands that are only
				// available to the console
				String input = in.readLine().trim();
				StringTokenizer st = new StringTokenizer(input);
				String command = st.nextToken();

				if (isValidInputFile && executor.isValidDeployCmd(command)) {
					executor.handleInput(input);

				} else if (command.equalsIgnoreCase("load")) {
					if (!st.hasMoreTokens()) {
						System.out.println("Failed: Missing filename for 'load' command\n");
					} else {
						inputFile = st.nextToken().trim();
						System.out.println("Loading " + inputFile);
						isValidInputFile = cmdGenerator.generateCmdsForInputFile(inputFile);
					}

				} else if (command.equalsIgnoreCase("status")) {
					handleStatusCommand();

				} else if (command.equalsIgnoreCase("help") || command.equalsIgnoreCase("?")) {
					FileOperation.displayFile(helpFilePath);

				} else if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("cu")
						|| command.equalsIgnoreCase("ciao") || command.equalsIgnoreCase("bye")
						|| command.equalsIgnoreCase("quit")) {
					System.exit(1);

				} else if (command.equalsIgnoreCase("sshtest")) {
					executor.handleInput(input);

				} else if (TypeChecker.isNumeric(command)) {
					if (cmdGenerator.generateCmdsForConsole(input)) {
						// If add/remove broker/client is successful, then the
						// loaded
						// input file is erased already
						isValidInputFile = false;
						deployer.deployConsoleCmd();
					} else {
						System.out.println("Failed: The input '" + input + "' is invalid.");
					}
				} else {
					if (executor.isValidDeployCmd(command)) {
						System.out.println("Failed: You cannot use this command until "
								+ "a correct topology file is loaded\n");
					} else {
						System.out.println("Failed: '" + command + "' is an invalid command");
					}
				}

			} catch (NoSuchElementException e1) {
				System.out.println("Failed: (Panda) Input is invalid");

			} catch (IOException e2) {
				e2.printStackTrace();
			} catch (ClientException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

	}

	private void handleStatusCommand() {
		if (isValidInputFile) {
			System.out.println("Input file was successfully loaded...");
			deployer.showStatus();
		} else {
			System.out.println("No input file was loaded, or loaded input file was\n"
					+ "invalid or contains errors.");
		}
	}

	private void printUsageAndExit() {
		String output = "Usage:\n" + " \tjava Panda [-i inputFile] [-c configFile]\n"
				+ " \tjava Panda [inputFile]\n" + "Note: All parameters are optional.\n";

		System.out.println(output);
		System.exit(1);
	}

	public String getWorkingPath() {
		return workingPath;
	}

	public Properties getConfigProperties() {
		return configProps;
	}

	public Deployer getDeployer() {
		return deployer;
	}

	/*
	 * This is used by the command generator to dynamically verify the connectivity of node
	 * addresses when it loads a topology file.
	 */
	public ScriptExecutor getScriptExecutor() {
		return executor;
	}

	public static void main(String[] args) {
		if (PADRES_HOME == null) {
			System.err.println("Environment variable $PADRES_HOME is not defined.  Bye!");
			System.exit(1);
		}

		try {
			FileOperation.mkdir(DEFAULT_PANDA_EXCEPTION_LOG_PATH);
			String logFile = DEFAULT_PANDA_EXCEPTION_LOG_PATH + "systemErr-Panda.log";
			FileOutputStream outStr = new FileOutputStream(logFile, true);
			PrintStream printStream = new PrintStream(outStr);
			System.setErr(printStream);
		} catch (Exception e) {
			System.err.println("Error initializing output file. system error");
		}

		CommandLine cmdLine = new CommandLine(ClientConfig.getCommandLineKeys());
		try {
			cmdLine.processCommandLine(args);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Both are optional parameters
		String topologyFile = cmdLine.getOptionValue(CMD_ARG_INPUTFILE);
		String configFile = cmdLine.getOptionValue(CMD_ARG_CONFIG, DEFAULT_CONFIG_FILE);

		// If only one argument, accept it as the input file
		if (args.length == 1) {
			topologyFile = args[0];
		}

		Panda panda = new Panda(configFile);
		panda.run(topologyFile);
	}
}

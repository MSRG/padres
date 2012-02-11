package ca.utoronto.msrg.padres.tools.panda;

/*
 * Created on May 5, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author Cheung
 *
 * Give it a file and a script and it will run it
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.util.StringTokenizer;

import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.TypeChecker;
import ca.utoronto.msrg.padres.common.util.io.FileOperation;
import ca.utoronto.msrg.padres.common.util.io.URLOperation;

public class ScriptExecutor {

	private final Panda panda;

	// private final CommandGenerator cmdGenerator;

	// Stores all environment variables used to run all the scripts
	private final static short ENV_COUNT = 4;

	private final Map<String, String> envMap;

	// various paths
	private final String workingPath; // this has a "/" char as its suffix

	private final File fileWorkingPath;

	private final String localScriptsPath;

	private final static String SCRIPT_LOG_PATH = "log/";

	private final static String SCRIPT_FAILED_LOG_EXT = ".failed.log";

	private final static String SCRIPT_OK_LOG_EXT = ".ok.log";

	// Default values for all environment variables for scripts
	private static final String DEFAULT_ENV_IDENTITY = "privatekey";

	private static final String DEFAULT_ENV_SLICE = "utoronto_jacobsen_lb";

	private static final String DEFAULT_ENV_SETUP = "http://www.msrg.utoronto.ca/planetlab/setup/setup";

	private static final String DEFAULT_ENV_TARBALL = "http://www.msrg.utoronto.ca/planetlab/cyber.tar.gz";

	private static final String TARBALL_ENV_KEY = "TARBALL";

	// relative to remote computer's home directory
	private final static String DEFAULT_REMOTE_LOGS_PATH = ".padres/logs";
	
	private final static String REMOTE_LOGS_PATH_KEY = "REMOTE_LOGS_PATH";
	
	// This is referred in 2 places in the code below, hence it's defined here
	// for consistency sake.

	// Default script path/filename constants, relative to PADRES_HOME
	public final static String DEFAULT_LOCAL_SCRIPTS_PATH = Panda.PADRES_HOME + "/bin/panda";


	// Organized way of keeping track of what are all deployment-related
	// commands
	// that we support
	private static final String[] validDeployCmds = { "install", "uninstall", "reinstall",
			"upload", "clean", "deploy", "stop", "auto", "get" };

	private final Set<String> validDeployCmdSet;

	/**
	 * Constructor
	 * 
	 * @param thePanda
	 */
	public ScriptExecutor(Panda thePanda, CommandGenerator cGenerator) {
		panda = thePanda;
		// cmdGenerator = cGenerator;

		Properties props = panda.getConfigProperties();
		workingPath = panda.getWorkingPath();
		localScriptsPath = props.getProperty("scripts.local.path", DEFAULT_LOCAL_SCRIPTS_PATH);

		fileWorkingPath = new File(workingPath);
		// load environment variables for running the scripts
		envMap = loadScriptEnvVars(props);

		validDeployCmdSet = new HashSet<String>(Arrays.asList(validDeployCmds));

		// Copy all panda scripts to the working directory
		FileOperation.syncExecute(localScriptsPath + "copy " + localScriptsPath + "* "
				+ workingPath, null, null);

		// Check to ensure we have a valid SSH version
		FileOperation.asyncExecute(localScriptsPath + "sshversion", null, null);
	}

	/*
	 * I'm spitting out warnings because if these are wrong, then the deployment
	 * can break without knowing what happened
	 */
	private Map<String, String> loadScriptEnvVars(Properties props) {
		String uri = null; // this is used to temporaily hold file location
		Map<String, String> tempEnvMap = new HashMap<String, String>((int) (ENV_COUNT / 0.75));

		/*
		 * (1) Load value of IDENTITY environment variable from properties file
		 */
		if (!props.containsKey("scripts.env.IDENTITY"))
			System.out.println("Warning: Value of environment variable IDENTITY"
					+ " not found in properties file, using '" + DEFAULT_ENV_IDENTITY
					+ "' as default.");
		uri = props.getProperty("scripts.env.IDENTITY", DEFAULT_ENV_IDENTITY);
		// Check that it exists
		if (!FileOperation.exists(uri)) {
			System.out.println("File '" + uri + "' specified for environment "
					+ "variable IDENTITY does not exist!  Exiting.");
			System.exit(1);
		}
		tempEnvMap.put("IDENTITY", uri);

		/*
		 * (2) Load value of SLICE environment variable from properties file
		 */
		if (!props.containsKey("scripts.env.SLICE"))
			System.out.println("Warning: Value of environment variable SLICE"
					+ " not found in properties file, using '" + DEFAULT_ENV_SLICE
					+ "' as default.");
		// Can't check this validity of this value, too bad! :P
		tempEnvMap.put("SLICE", props.getProperty("scripts.env.SLICE", DEFAULT_ENV_SLICE));

		/*
		 * (3) Load value of SETUP environment variable from properties file
		 */
		if (!props.containsKey("scripts.env.SETUP"))
			System.out.println("Warning: Value of environment variable SETUP"
					+ " not found in properties file, using '" + DEFAULT_ENV_SETUP
					+ "' as default.");
		uri = props.getProperty("scripts.env.SETUP", DEFAULT_ENV_SETUP);
		// System.out.println("Reminder to uncomment the setup file checker");
		if (!uri.startsWith("http")) {
			System.out.println("Location of scripts.env.SETUP is not a http url!  Exiting.");
			System.exit(1);
			// Check that it exists
		} else if (!URLOperation.exists(uri)) {
			System.out.println("URI '" + uri + "' specified for environment "
					+ "variable SETUP does not exist!  Exiting.");
			System.exit(1);
		}
		tempEnvMap.put("SETUP", uri);

		/*
		 * (4) Load value of TARBALL environment variable from properties file
		 */
		if (!props.containsKey("scripts.env.TARBALL"))
			System.out.println("Warning: Value of environment variable TARBALL"
					+ " not found in properties file, using '" + DEFAULT_ENV_TARBALL
					+ "' as default.");
		uri = props.getProperty("scripts.env.TARBALL", DEFAULT_ENV_TARBALL);
		// System.out.println("Reminder to uncomment the tarball file checker");
		if (!uri.startsWith("http")) {
			System.out.println("Location of scripts.env.TARBALL is not a http url!  Exiting.");
			System.exit(1);
			// Check that it exists
		} else if (!URLOperation.exists(uri)) {
			System.out.println("URI '" + uri + "' specified for environment "
					+ "variable TARBALL does not exist!  Exiting.");
			System.exit(1);
		}
		tempEnvMap.put(TARBALL_ENV_KEY, uri);

		/*
		 * (5) Load value of remote PADRES_HOME path properties file
		 */
		// Not easy to check the validity of this value, too bad! :P
		// Get rid of an ending slash if there is one. For cleanliness.
		String buildPath = props.getProperty("remote.padres.path");
		while (buildPath.endsWith("/"))
			buildPath = buildPath.substring(0, buildPath.length() - 1);
		tempEnvMap.put("PADRES_PATH", buildPath);

		/*
		 * (6) Load value of remote log directory
		 */
		tempEnvMap.put(REMOTE_LOGS_PATH_KEY, 
				props.getProperty("logs.remote.path", DEFAULT_REMOTE_LOGS_PATH));

		// all is ok :)
		return tempEnvMap;
	}

	public boolean handleInput(String input) throws ClientException, ParseException {
		input = input.trim();
		StringTokenizer st = new StringTokenizer(input);
		String command = st.nextToken().trim();
		String param = st.hasMoreTokens() ? input.substring(input.indexOf(" ") + 1).trim() : "";

		// handle sshtest, the only command that can be run even if topology
		// file
		// is not loaded yet
		if (command.equalsIgnoreCase("sshtest")) {
			if (TypeChecker.isString(param)) {
				return syncExec(input);
			} else {
				System.out.println("ScriptExecutor: Missing argument to command '" + command + "'.");
				return false;
			}
		}

		// For all other commands, we need to get/set argument appropriately
		if (isValidDeployCmd(command)) {
			if (command.equalsIgnoreCase("install") || command.equalsIgnoreCase("uninstall")) {
				return syncExec(command + " " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);

			} else if (command.equalsIgnoreCase("clean")) {
				if (param != null && param.length() > 0) {
					if (param.equalsIgnoreCase("log") || param.equalsIgnoreCase("logs")) {
						return syncExec("cleanLogs "
								+ CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
					} else if (param.equalsIgnoreCase("all")) {
						return syncExec("cleanAll " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
					} else {
						System.out.println("Unrecognized parameter '" + param
								+ "' for 'clean' command.");
						return false;
					}
				} else {
					System.out.println("Missing required parameter to the 'clean' command.  Type 'help' to "
							+ "see clean's parameter options.");
					return false;
				}

			} else if (command.equalsIgnoreCase("upload")) {
				if (param != null && param.length() > 0) {
					// Check if the URL is valid
					if (!param.startsWith("http")) {
						System.out.println("Parameter to 'upload' command must be a URL.");
						return false;
						// Check that it exists
					} else if (!URLOperation.exists(param)) {
						System.out.println("URL '" + param + "' does not exist.");
						return false;
					}

					// If all is ok, then encoded URL into the env variable map.
					// Need to
					// backup original first so that we can revert back to it
					// afterwards
					String originalTarballURL = envMap.get(TARBALL_ENV_KEY).toString();
					envMap.put(TARBALL_ENV_KEY, param);
					boolean execResult = syncExec(command + " "
							+ CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
					// restore original tarball url
					envMap.put(TARBALL_ENV_KEY, originalTarballURL);
					originalTarballURL = null;
					return execResult;
				} else {
					return syncExec(command + " " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
				}

			} else if (command.equalsIgnoreCase("reinstall")) {
				handleInput("uninstall");
				return handleInput("install");

			} else if (command.equalsIgnoreCase("stop")) {
				if (param != "" && !TypeChecker.isNumeric(param) && Double.parseDouble(param) >= 0) {
					System.out.println("ScriptExecutor: Argument to 'stop' command must be numeric\n.");
					return false;
				}
				return syncExec(command + " " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST + " "
						+ param);

			} else if (command.equalsIgnoreCase("deploy")) {
				if (param != null && param.length() > 0 && param.equalsIgnoreCase("auto")) {
					return panda.getDeployer().deployBatchCmds(true);
				} else {
					return panda.getDeployer().deployBatchCmds(false);
				}
			} else if (command.equalsIgnoreCase("auto")) {
				if (handleInput("install"))
					if (handleInput("upload " + param))
						return handleInput("deploy");
					else
						System.out.println("ScriptExecutor: Upload process failed, "
								+ "deployment aborted.");
				else
					System.out.println("ScriptExecutor: Install proces failed, "
							+ "deployment aborted.");
			} else if (command.equalsIgnoreCase("get")) {
				if (param != null && param.length() > 0) {
					if (param.equalsIgnoreCase("log") || param.equalsIgnoreCase("logs")) {
						return syncExec("getLogs " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
					} else if (param.equalsIgnoreCase("error") || param.equalsIgnoreCase("errors")) {
						syncExec("getErrors " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);
						String getErrorsOkLog = getOkScriptLogFilePath("getErrors");
						if (FileOperation.exists(getErrorsOkLog)) {
							System.out.println("\n\nHere are the Exceptions found:\n===========================");
							FileOperation.displayFile(getErrorsOkLog);
						} else
							System.out.println("No ERROR exceptions found.  Congratulations!");
					} else {
						System.out.println("Unrecognized parameter '" + param
								+ "' for 'get' command.");
						return false;
					}
				} else {
					System.out.println("Missing required parameter to the 'get' command.  Type 'help' to "
							+ "see get's parameter options.");
					return false;
				}
			}
		} else {
			System.out.println("ScriptExecutor:handleInput(): Unrecognized deployment"
					+ " command.");
		}

		return false;
	}

	/**
	 * Runs the given command (which includes the script and its arguments)
	 * Automatically prints out the failure log if any errors were encountered.
	 * 
	 * Note: This function needs to be public for the deployer to call this
	 * method
	 * 
	 * @param script
	 * @param args
	 * @return true if all goes well
	 */
	public boolean syncExec(String input) {
		String scriptName = input.substring(0, input.indexOf(" "));
		input = "bash " + input; // get 255 exit code if no "bash" prefix :(
		int exitCode = FileOperation.syncExecute(input, envMap, fileWorkingPath);

		// Show failure log if something went wrong. This means either:
		// - exit code of script is > 0
		// - a failed log is generated
		// - both of the above
		String failedLogPath = getFailedScriptLogFilePath(scriptName);
		boolean failed = exitCode > 0 || FileOperation.exists(failedLogPath);
		// show log file if something went wrong
		if (failed) {
			// FileOperation.displayFile(failedLogPath);
			System.out.println("See failed log '" + failedLogPath + "' for more details");
		}

		return !failed;
	}

	public void asyncExec(String input) {
		input = "bash " + input;
		FileOperation.asyncExecute(input, envMap, fileWorkingPath);
	}

	public boolean isValidDeployCmd(String cmd) {
		return validDeployCmdSet.contains(cmd);
	}

	private String getFailedScriptLogFilePath(String scriptName) {
		return workingPath + SCRIPT_LOG_PATH + scriptName + SCRIPT_FAILED_LOG_EXT;
	}

	private String getOkScriptLogFilePath(String scriptName) {
		return workingPath + SCRIPT_LOG_PATH + scriptName + SCRIPT_OK_LOG_EXT;
	}
}

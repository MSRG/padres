package ca.utoronto.msrg.padres.tools.panda;

/*
 * Created on May 5, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author alex
 *
 * This class accepts an input file and generates various command-line 
 * calls to start brokers, clients, rmiregistry, etc.
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.io.FileOperation;
import ca.utoronto.msrg.padres.tools.panda.input.InputCommand;
import ca.utoronto.msrg.padres.tools.panda.input.InputCommandComparator;
import ca.utoronto.msrg.padres.tools.panda.input.ProcessAddCommand;
import ca.utoronto.msrg.padres.tools.panda.input.ProcessRemoveCommand;

public class CommandGenerator {
	private final Panda panda;

	private final String workingPath;

	private final TopologyValidator topologyValidator;

	// For writing commands from topology file as a batch
	private BufferedWriter allUniqueIpAddrWriter = null;

	private BufferedWriter phaseOneCmdWriter = null; // for starting up brokers

	// at time 0s

	private BufferedWriter phaseTwoCmdWriter = null; // all other things after

	// 0s

	// For writing commands from console
	private BufferedWriter consoleCmdWriter = null;

	// Filenames for the above corresponding files, respectively
	private final String ipAddrListFilePath;

	private final String phaseOneCmdFilePath;

	private final String phaseTwoCmdFilePath;

	private final String consoleCmdFilePath;

	// keeps track of all machines used by topology file to allow installation
	// on
	// all referenced machines in one shot
	private final Set<String> allIpAddrSet;

	// Stores the commands for each phase before writing them in sorted order to
	// a file
	private final List<InputCommand> phaseOneCmdList;

	private final List<InputCommand> phaseTwoCmdList;

	private final List<InputCommand> consoleCmdList; // this is just one command

	// Generated script filenames. All dirs must end with "/"
	// These output files are for processing from an input file
	public static final String FILENAME_BATCH_ALL_NODE_LIST = "allIpAddrList.txt";

	public static final String FILENAME_BATCH_PHASE_ONE_CMDS = "phaseOneCmdFileList.txt";

	public static final String FILENAME_BATCH_PHASE_TWO_CMDS = "phaseTwoCmdFileList.txt";

	public static final String FILE_EXTENSION_PHASE_ONE_CMDS = ".p1";

	public static final String FILE_EXTENSION_PHASE_TWO_CMDS = ".p2";

	// These output files are for processing from command line
	public static final String FILENAME_CONSOLE_CMD = "consoleFileList.txt";

	public static final String FILE_EXTENSION_CONSOLE_CMDS = ".con";

	// Stores command name -> command template mapping
	private final Map<Object, String> commandTemplateMap;

	public final static String PREFIX_PROCESS_NAME = "PROCESS-";

	public final static String CMD_NAME_KILL = "KillCommand";

	public final static String CMD_NAME_KILL_ALL = "KillAllCommand";

	/**
	 * Constructor
	 * 
	 * @param plDeployer
	 */
	public CommandGenerator(Panda panda) {
		this.panda = panda;
		workingPath = panda.getWorkingPath();
		ipAddrListFilePath = workingPath + FILENAME_BATCH_ALL_NODE_LIST;
		phaseOneCmdFilePath = workingPath + FILENAME_BATCH_PHASE_ONE_CMDS;
		phaseTwoCmdFilePath = workingPath + FILENAME_BATCH_PHASE_TWO_CMDS;
		consoleCmdFilePath = workingPath + FILENAME_CONSOLE_CMD;
		allIpAddrSet = new HashSet<String>(800);
		phaseOneCmdList = new LinkedList<InputCommand>();
		phaseTwoCmdList = new LinkedList<InputCommand>();
		consoleCmdList = new LinkedList<InputCommand>();
		topologyValidator = new TopologyValidator(panda);

		// Initialize the remote commands
		commandTemplateMap = new HashMap<Object, String>();
		initCommandTemplate(panda.getConfigProperties());
	}

	/*
	 * Initializes commandTemplateMap
	 */
	private void initCommandTemplate(Properties props) {
		loadCommandTemplate(ProcessAddCommand.class, "command.process.add", props);
		loadCommandTemplate(ProcessRemoveCommand.class, "command.process.remove", props);
		loadCommandTemplate(CMD_NAME_KILL_ALL, "command.all.remove", props);
	}

	/*
	 * All command templates will be valid, otherwise an error will trigger this
	 * program to exit
	 */
	private void loadCommandTemplate(Object cmdName, String cmdConfigkey, Properties configProps) {
		try {
			String cmd = configProps.getProperty(cmdConfigkey, null).replaceAll(
					"<REMOTE_PADRES_PATH>", configProps.getProperty("remote.padres.path"));

			commandTemplateMap.put(cmdName, cmd);
			cmd = null;

		} catch (NullPointerException e) {
			System.err.println("ERROR: (CommandGenerator) A required "
					+ "command, script, or path is not found in the configuration file.  "
					+ "This error occured inside composeCommand().\n");
			System.exit(1);
		}

	}

	/*
	 * START - PROCESSING INPUT
	 */
	/**
	 * Main function call available to outside of this class.
	 * 
	 * Read in line by line.
	 * 
	 * @throws ClientException
	 */
	public boolean generateCmdsForInputFile(String filename) throws ClientException {
		int lineCount = 0;
		boolean result = true;
		String line;
		InputCommand inputCmd;

		try {
			// Create new output files, overwrite old ones
			initializeBatchProcessing();

			// Process each line one by one
			if (filename == null)
				throw new FileNotFoundException("Not found");

			BufferedReader in = new BufferedReader(new FileReader(filename));

			// Read and store every processed line into phase-I or phase-II list
			while ((line = in.readLine()) != null) {
				lineCount++;

				// pre-process the line
				line = line.trim();

				// Empty line
				if (line == null || line == "" || line.length() < 1)
					continue;

				// Skip comments
				if (line.startsWith("#"))
					continue;

				inputCmd = InputCommand.toSpecificCommand(line);

				// Stop immediately if an input line is incorrect
				if (inputCmd == null) {
					System.out.println("ERROR: (CommandGenerator) Syntax error found " + "on line "
							+ lineCount + ".\n");
					System.out.println("Load aborted\n");
					in.close();
					terminateBatchProcessing();
					return false;
				}

				// Just a check that spits out warnings, does not interrupt our
				// execution
				topologyValidator.verify(inputCmd);

				// If something went wrong, stop
				if ((result &= handleBatchInput(inputCmd)) == false)
					break;
			}
			in.close();

			// Only write results to file if all went ok
			if (result) {
				// Write phase-I and phase-II commands to file. Need to sort
				// phase-II
				// commands first according to order in time.
				Collections.sort(phaseTwoCmdList, new InputCommandComparator<InputCommand>());

				result &= writeCmdsToFile(phaseOneCmdList, phaseOneCmdWriter,
						FILE_EXTENSION_PHASE_ONE_CMDS);
				result &= writeCmdsToFile(phaseTwoCmdList, phaseTwoCmdWriter,
						FILE_EXTENSION_PHASE_TWO_CMDS);
			}

			// Flush out our buffers and close output files
			terminateBatchProcessing();

			// Now that we have all of the nodes' unique IP addresses recorded
			// to a file,
			// we need to run sshtest to see if all of them are reachable. Abort
			// and
			// return false if not.
			System.out.println("Checking reachability of referenced nodes in topology file ...");
			result &= panda.getScriptExecutor().handleInput(
					"sshtest " + CommandGenerator.FILENAME_BATCH_ALL_NODE_LIST);

		} catch (ParseException e1) {
			System.out.println("Failed: (CommandGenerator)" + " Input file '" + filename
					+ "' not found.  Load one using 'load <filename>'. Exception is: " + e1);
			return false;
		} catch (FileNotFoundException e1) {
			System.out.println("Failed: (CommandGenerator)" + " Input file '" + filename
					+ "' not found.  Load one using 'load <filename>'.");
			return false;
		} catch (IOException e2) {
			System.out.println("Failed: (CommandGenerator) Exception is:\n" + e2.getMessage()
					+ "\n");
			return false;
		}

		if (result) {
			System.out.println(filename + " successfully loaded\n");

			// Make a copy of all successfully loaded input files in working
			// directory
			FileOperation.asciiCopy(filename, workingPath + FileOperation.getFilename(filename));
		} else {
			System.out.println("An error occured while loading " + filename + ".  Load aborted\n");
		}

		return result;
	}

	/*
	 * These are files that contain the command-lines that start broker and
	 * clients, etc.
	 */
	private void initializeBatchProcessing() throws IOException {
		// Just in case we forgot to call terminateBatchProcessing() to clear
		// this
		allIpAddrSet.clear();

		// Do not append for all output files
		try {
			allUniqueIpAddrWriter = new BufferedWriter(new FileWriter(ipAddrListFilePath, false));
			phaseOneCmdWriter = new BufferedWriter(new FileWriter(phaseOneCmdFilePath, false));
			phaseTwoCmdWriter = new BufferedWriter(new FileWriter(phaseTwoCmdFilePath, false));
		} catch (IOException ioe) {
			// Undo initialization
			if (allUniqueIpAddrWriter != null) {
				allUniqueIpAddrWriter.close();
				allUniqueIpAddrWriter = null;
			}
			if (phaseOneCmdWriter != null) {
				phaseOneCmdWriter.close();
				phaseOneCmdWriter = null;
			}
			if (phaseTwoCmdWriter != null) {
				phaseTwoCmdWriter.close();
				phaseTwoCmdWriter = null;
			}
			throw new IOException("Could not create batch output files.");
		}
	}

	private void terminateBatchProcessing() throws IOException {
		// reset everything
		allIpAddrSet.clear();
		phaseOneCmdList.clear();
		phaseTwoCmdList.clear();

		try {
			allUniqueIpAddrWriter.flush();
			phaseOneCmdWriter.flush();
			phaseTwoCmdWriter.flush();

			allUniqueIpAddrWriter.close();
			phaseOneCmdWriter.close();
			phaseTwoCmdWriter.close();

			allUniqueIpAddrWriter = null;
			phaseOneCmdWriter = null;
			phaseTwoCmdWriter = null;

		} catch (IOException ioe) {
			throw new IOException("Could not close batch output files.");
		}
	}

	private boolean handleBatchInput(InputCommand cmd) {
		boolean result = true;

		result &= recordUniqueIpAddress(cmd);
		result &= InputCommand.initExecutableCommand(cmd, this);

		// Put the command into Phase-I or II list. Finally sort phase-II
		// commands
		// when all commands inserted into the list
		if (cmd.getTime() == 0) {
			phaseOneCmdList.add(cmd);
		} else {
			phaseTwoCmdList.add(cmd);
		}

		return result;
	}

	/*
	 * Returns false if anything goes wrong
	 */
	private boolean recordUniqueIpAddress(InputCommand cmd) {
		String nodeIpAddress = cmd.getNodeIpAddress();
		// Only write address to address list if we have not seen it before
		if (allIpAddrSet.add(nodeIpAddress)) {
			return writeln(allUniqueIpAddrWriter, nodeIpAddress);
		}

		return true;
	}

	/*
	 * END - PROCESSING FILE INPUT
	 */

	/*
	 * START - PROCESSING COMMAND LINE INPUT
	 */
	/*
	 * These are files that contain the command-lines that start broker and
	 * clients, etc.
	 */
	private void initializeConsoleProcessing() throws IOException {
		// Do not append for all output files
		try {
			consoleCmdWriter = new BufferedWriter(new FileWriter(consoleCmdFilePath, false));
		} catch (IOException ioe) {
			if (consoleCmdWriter != null) {
				consoleCmdWriter.close();
				consoleCmdWriter = null;
			}
			throw new IOException("Could not create cmd line output files.");
		}
	}

	private void terminateConsoleProcessing() throws IOException {
		consoleCmdList.clear();
		try {
			consoleCmdWriter.flush();
			consoleCmdWriter.close();
			consoleCmdWriter = null;
		} catch (IOException ioe) {
			throw new IOException("Could not close cmd line output files.");
		}
	}

	public boolean generateCmdsForConsole(String line) {
		try {
			InputCommand cmd = InputCommand.toSpecificCommand(line.trim());

			// Return false if input is malformed, otherwise proceed
			// Stop immediately if an input line is incorrect
			if (cmd == null)
				return false;

			// Create new output files, overwrite old ones
			initializeConsoleProcessing();

			boolean result = handleCmdLineInput(cmd);

			// Flush out our buffers and close output files
			terminateConsoleProcessing();

			return result;
		} catch (IOException e2) {
			System.out.println("Failed: (CommandGenerator) Exception is:\n" + e2.getMessage()
					+ "\n");
			return false;
		}
	}

	/*
	 * Have to write to two files just like the case if we load from a topology
	 * file 1. file containing the name of the file containing the command named
	 * as consoleFileList.txt 2. file containing the command named something
	 * like 10.0.0.1.con
	 */
	private boolean handleCmdLineInput(InputCommand cmd) {
		boolean result = true;

		result &= InputCommand.initExecutableCommand(cmd, this);
		consoleCmdList.add(cmd);

		result &= writeCmdsToFile(consoleCmdList, consoleCmdWriter, FILE_EXTENSION_CONSOLE_CMDS);

		return result;
	}

	/*
	 * END - PROCESSING COMMAND LINE INPUT
	 */

	/*
	 * Writes cmds to 2 files. One is named like phaseOneCmdFileList.txt and it
	 * contains the list of address-specific command files, such as 10.0.0.1.p1.
	 * Within 10.0.0.1.p1, it contains the actual commands to be executed at the
	 * remote machine as indicated by its filename, which is 10.0.0.1. The
	 * extension 'p1' denotes this file is responsible for phase-I.
	 */
	private boolean writeCmdsToFile(List<InputCommand> cmdList, BufferedWriter addrListWriter,
			String cmdFileExtension) {

		boolean success = true;
		// maps addresses to writers
		Map<String, BufferedWriter> addrToWriterMap = new HashMap<String, BufferedWriter>();
		Set<String> ipAddrSet = new LinkedHashSet<String>();
		// Sorted by order of commands, which is in increasing time

		// Write commands into their address specific file
		for (InputCommand cmd : cmdList) {
			String nodeIpAddress = cmd.getNodeIpAddress();
			ipAddrSet.add(nodeIpAddress);

			// Need to create the writer for an address if its not created yet
			if (!addrToWriterMap.containsKey(nodeIpAddress)) {
				try {
					BufferedWriter cmdWriter = new BufferedWriter(new FileWriter(workingPath
							+ nodeIpAddress + cmdFileExtension, false));
					// store the new writer in the map
					addrToWriterMap.put(nodeIpAddress, cmdWriter);
				} catch (IOException e) {
					System.err.println("ERROR: (CommandGenerator) Error when "
							+ "trying to create cmd file " + workingPath + nodeIpAddress
							+ cmdFileExtension + ".\n" + e.getMessage());
					return false;
				}
			}

			BufferedWriter cmdWriter = addrToWriterMap.get(nodeIpAddress);
			success &= writeln(cmdWriter, cmd.getExecutableCommand());
		}

		// Need to close and clean up the writers
		for (BufferedWriter writer : addrToWriterMap.values()) {
			try {
				writer.flush();
				writer.close();
			} catch (Exception e) {
				System.out.println("WARNING: (CommandGenerator) Exception occured "
						+ "while flushing and closing a command file.\n" + e.getMessage());
			}
			writer = null;
		}
		addrToWriterMap.clear();
		addrToWriterMap = null;

		// Now write the file that contains the set of files containing the
		// commands
		for (String ipAddr : ipAddrSet) {
			success &= writeln(addrListWriter, ipAddr + cmdFileExtension);
		}
		ipAddrSet.clear();
		ipAddrSet = null;

		return success;
	}

	/*
	 * Customizes a
	 */
	public static String bindCommandToAddress(String orgCommand, String ipAddress) {
		// Leave a space at the end to prepare for arguments, can't hurt :P
		return ipAddress + " " + orgCommand + " ";
	}

	private boolean writeln(BufferedWriter writer, String data) {
		try {
			writer.write(data);
			writer.newLine();
			return true;
		} catch (IOException e) {
			System.err.println("ERROR: (CommandGenerator) Exception while writing to "
					+ " output file.\n");
			return false;
		}
	}

	/**
	 * Using this method for giving the command template for a command is safer
	 * than giving the commandTemplateMap.
	 * 
	 * @param cmdName
	 * @return
	 */
	public Map<Object, String> getCmdTemplateMap() {
		return commandTemplateMap;
	}

	/**
	 * Get the IP address from 2nd field in command line
	 */
	public static String getNodeIpAddressFromCmd(String cmdLine) {
		return cmdLine.substring(cmdLine.indexOf(" ") + 1, cmdLine.indexOf(" ",
				cmdLine.indexOf(" ") + 1));
	}

}

package ca.utoronto.msrg.padres.tools.panda;

/*
 * Created on May 14, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author cheung
 *
 * This class implements the 2-Phase deployment and interfaces with the
 * ScriptExecutor that invokes the supporting bash scripts.
 * 
 * - Subscribes to BROKER_INFO from any broker
 * - Ensures non-empty files are created from CommandGenerator
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.Timer;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
//import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.DumbCommandLine;
import ca.utoronto.msrg.padres.common.util.datastructure.HashMapSet;
import ca.utoronto.msrg.padres.common.util.io.FileOperation;
import ca.utoronto.msrg.padres.common.util.io.QuickPrompt;

public class Deployer {

	private final Panda panda;

	// Need this guy to execute the deploy script
	private final ScriptExecutor executor;

	private final Properties configProps;

	// Timer and related variables to handle what happens when Phase-I ends or
	// times out
	private final Timer phaseOneTimer;

	private final Object phaseOneLock = new Object();

	private PhaseOneMonitoringClient p1Monitor = null;

	// This is set to true when phase-I is done or aborted
	private boolean startPhaseTwo = false;

	private static final String PHASE_ONE_TIMEOUT = "600"; // in seconds

	private static final String START_BROKER_SCRIPT = "startbroker";

	// This is called when phaseOneTimer times out
	private final ActionListener phaseOneTimeoutHandler = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			notifyPhaseOneFailed();
		}
	};

	/**
	 * Constructor
	 * 
	 * @param configProps
	 */
	public Deployer(Panda thePanda, ScriptExecutor sExecutor) {
		panda = thePanda;
		executor = sExecutor;

		configProps = panda.getConfigProperties();
		int phaseOneTimeout = Math.round(Float.parseFloat(configProps.getProperty(
				"deployer.phase1.timeout", PHASE_ONE_TIMEOUT))) * 1000; // convert
		// to ms
		phaseOneTimer = new Timer(phaseOneTimeout, phaseOneTimeoutHandler);
	}

	/**
	 * Shows current status of this deployment coordinator on the console
	 * 
	 */
	public void showStatus() {
		if (startPhaseTwo) {
			System.out.println("Phase-II of deployment have started.");
		} else {
			if (p1Monitor == null) {
				System.out.println("Phase-I ready to be deployed.");
			} else {
				System.out.println("Engaging Phase-I deployment...");
				p1Monitor.showStatus();
			}
		}
	}

	/**
	 * Call this to start broker/client process on remote nodes according to what was given in the
	 * console
	 * 
	 * @return true if successful
	 */
	public boolean deployConsoleCmd() {
		return executor.syncExec("deploy " + CommandGenerator.FILENAME_CONSOLE_CMD);
	}

	/**
	 * Call this to start broker/client processes on remote nodes according to what was specified in
	 * topology file
	 * 
	 * @return true if successful
	 * @throws ClientException
	 * @throws ParseException 
	 */
	public boolean deployBatchCmds(boolean auto) throws ClientException, ParseException {
		if (runPhaseOne(auto))
			return runPhaseTwo();
		else {
			System.out.println("Deployer: Aborted phase-II deployment.");
		}

		return false;
	}

	/*
	 * Runs the script that deploys phase 1, then use the PhaseOneMonitoringClient to see when our
	 * broker network is full up
	 */
	private boolean runPhaseOne(boolean auto) throws ClientException, ParseException {
		executor.asyncExec("deploy " + CommandGenerator.FILENAME_BATCH_PHASE_ONE_CMDS);

		// Do not give user choice to skip monitor or not if deployed with
		// "auto" parameter
		if (!auto) {
			QuickPrompt skipPrompt = new QuickPrompt("Deploying Phase-I...\n"
					+ "Do you wish to skip the monitor and proceed immediately "
					+ "with Phase-II deployment");
			skipPrompt.addResponse("y");
			skipPrompt.addResponse("n");

			if (skipPrompt.promptAndGetResponse().equalsIgnoreCase("y")) {
				QuickPrompt abortPrompt = new QuickPrompt(
						"Do you wish to (a)bort or (c)ontinue with Phase-II?");
				abortPrompt.addResponse("a");
				abortPrompt.addResponse("c");

				if (abortPrompt.promptAndGetResponse().equalsIgnoreCase("a")) {
					return false;
				} else {
					// startPhaseTwo = true; // need to reset this back to false
					// anyway
					return true;
				}
			}
		}
		// otherwise, proceed below...

		// Maps a broker's id to its neighbors' ids, bidirectionally
		HashMapSet neighborUriMap = new HashMapSet();
		// stores address to brokerID map
		Map<String, String> addrToIdMap = new LinkedHashMap<String, String>();

		// stop if there are any exceptions.
		if (!loadInfoFromPhaseOneCmdFile(neighborUriMap, addrToIdMap)) {
			System.out.println("Error loading information about brokers belonging in Phase-I.");
			return false;
		}

		// No brokers in phase-I of startup, quit.
		if (addrToIdMap.isEmpty()) {
			System.out.println("No brokers found in Phase-I of deployment.  "
					+ "Proceeding with Phase-II.");
			return true;
		}

		// Now that we have broker addresses, try connecting to one of them

		p1Monitor = new PhaseOneMonitoringClient(neighborUriMap, addrToIdMap, this, auto);
		// we don't need to reference this
		neighborUriMap = null;
		addrToIdMap = null;

		// Report failure if we can't connect to a broker for monitoring
		try {
			if (!p1Monitor.connect()) {
				System.out.println("Could not connect to any deployed broker.");
				return false;
			}
		} catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			return false;
		}

		// once connected, start the phaseOneTimer and monitor when the broker
		// federation have fully started up
		phaseOneTimer.start();
		p1Monitor.run();

		// Wait until timeout or success
		while (true) {
			synchronized (phaseOneLock) {
				System.out.println("Waiting for incoming broker info");
				try {
					phaseOneLock.wait();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			p1Monitor.showStatus();

			// need to determine if phase-I was successful or encountered a
			// timeout
			// 1. No timeout, startup all ok
			if (startPhaseTwo) {
				p1Monitor.shutdown();
				p1Monitor = null;
				startPhaseTwo = false; // reset for next deployment
				return true;
				// 2. Timeout occurred, see if user wants to wait. If yes, do
				// nothing,
				// but if no, then ask if user wants to continue with phase-II
				// or abort
			} else {
				// Do not give user choice to skip monitor or not if deployed
				// with "auto" parameter
				if (!auto) {
					QuickPrompt yesNoPrompt = new QuickPrompt(
							"Phase-I deployment timed out after waiting "
									+ phaseOneTimer.getInitialDelay() / 1000
									+ "s.  Continue waiting?");
					yesNoPrompt.addResponse("y");
					yesNoPrompt.addResponse("n");

					if (yesNoPrompt.promptAndGetResponse().equalsIgnoreCase("N")) {
						p1Monitor.shutdown();
						p1Monitor = null;

						QuickPrompt abortPrompt = new QuickPrompt(
								"Do you wish to (a)bort or (c)ontinue with Phase-II?");
						abortPrompt.addResponse("a");
						abortPrompt.addResponse("c");

						if (abortPrompt.promptAndGetResponse().equalsIgnoreCase("a")) {
							return false;
						} else {
							// startPhaseTwo = true; // need to reset this back
							// to false anyway
							return true;
						}

					} else {
						// user want to wait longer, so restart phaseOneTimer
						phaseOneTimer.restart();
					}
					// for auto case, just keep waiting until phase I is
					// complete.
				} else {
					phaseOneTimer.restart();
				}
			}
		}
	}

	/*
	 * Runs the script that deploys phase II
	 */
	private boolean runPhaseTwo() {
		// if nothing to deploy in phase-II, then skip and return.
		if (FileOperation.getTotalLineCount(panda.getWorkingPath()
				+ CommandGenerator.FILENAME_BATCH_PHASE_TWO_CMDS) == 0)
			return true;

		return executor.syncExec("deploy " + CommandGenerator.FILENAME_BATCH_PHASE_TWO_CMDS);
	}

	public void notifyPhaseOneOk() {
		phaseOneTimer.stop();
		startPhaseTwo = true;
		System.out.println("Got OK from Phase-I monitoring client.  Safe to proceed"
				+ " with Phase-II deployement. :)");
		System.out.flush();
		synchronized (phaseOneLock) {
			phaseOneLock.notify();
		}
	}

	public void notifyPhaseOneFailed() {
		synchronized (phaseOneLock) {
			phaseOneLock.notify();
		}
	}

	/*
	 * Reads from phase-I file to compose Returns true even if phase-I does not have any brokers
	 */
	private boolean loadInfoFromPhaseOneCmdFile(HashMapSet neighborIdMapSet,
			Map<String, String> addrToIdMap) {

		// maps a broker's URI to its neighbors' URI
		HashMapSet neighborAddrMapSet = new HashMapSet();

		// temp variables used in 'while' loop below
		String filename, cmd;
//		CommandLine cmdLine = new CommandLine(BrokerConfig.getCommandLineKeys());
		BufferedReader cmdReader;
		final String screenCmd = "screen -d -m";

		// First load the mappings of each broker's address to its ID. Also load each broker's
		// neighbors' addresses. These neighbor addresses are translated to ids below.
		try {
			BufferedReader filenameReader = new BufferedReader(new FileReader(
					panda.getWorkingPath() + CommandGenerator.FILENAME_BATCH_PHASE_ONE_CMDS));

			while ((filename = filenameReader.readLine()) != null) {
				// Retrieve the filenames of each file containing the start
				// broker commands
				cmdReader = new BufferedReader(new FileReader(panda.getWorkingPath() + filename));

				while ((cmd = cmdReader.readLine()) != null) {
					// We only look at BROKER ADD commands
					if (cmd.indexOf(START_BROKER_SCRIPT) >= 0) {
						// trim the input to just the start broker command
						int indexOfScreenCmd = cmd.lastIndexOf(screenCmd);
						cmd = cmd.substring(indexOfScreenCmd + screenCmd.length(), cmd.indexOf("&&", indexOfScreenCmd));
						
						DumbCommandLine cmdLine = new DumbCommandLine(cmd.split(" "));
//						System.out.println("Processed command is " + cmd);
						String brokerUri = cmdLine.value(BrokerConfig.CMD_ARG_FLAG_URI);
//						System.out.println("URI of broker is " + brokerUri);
//						cmdLine.processCommandLine(cmd.split(" "));
//						String brokerUri = cmdLine.getOptionValue(BrokerConfig.CMD_ARG_FLAG_URI);
						NodeAddress brokerAddr = NodeAddress.getAddress(brokerUri);
						
						String brokerId = "";
						
						if (brokerAddr.getClass() == RMIAddress.class) {
							brokerId = ((RMIAddress)brokerAddr).getNodeID();
						} else if (brokerAddr.getClass() == SocketAddress.class) {
							brokerId = ((SocketAddress)brokerAddr).getNodeID();
						}

						// first record the broker's address to id mapping
						addrToIdMap.put(brokerAddr.getNodeURI(), brokerId);

						String neighborUri = cmdLine.value(BrokerConfig.CMD_ARG_FLAG_NEIGHBORS);
						
						// it is possible that the first broker started does not
						// have neighbors
						if (neighborUri != null && neighborUri.length() > 0) {
//							System.out.println("URI of neighbor is " + neighborUri);
							// Record the neighbors' addresses for this broker
							String neighborUris[] = neighborUri.split(",");
							for (int i = 0; i < neighborUris.length; i++) {
								// all keys and values are String URIs 
//								System.out.println("Putting mapping of " + brokerUri + " -> " + neighborUris[i]);
//								System.out.flush();
								neighborAddrMapSet.put(brokerUri, neighborUris[i]);
							}
						}
					}
				}
				cmdReader.close();
				cmdReader = null;
			}

//			System.out.println(addrToIdMap.toString() + "\n");
//			System.out.println(neighborAddrMapSet.toString() + "\n");
//			System.out.flush();
			
			// Now translate all broker address -> neighbor addresses mapping to
			// broker id -> neighbor id mappings
			for (Object addrStr : neighborAddrMapSet.keySet()) {
				String brokerUri = addrStr.toString();
				String brokerId = addrToIdMap.get(brokerUri);
//				System.out.println("Obtained broker ID of " + brokerUri + " to be " + brokerId);
//				System.out.flush();
				// store the id of the broker into our map first
				// Brokers w/wo neighbors will have a null value mapped to it
				// neighborIdMapSet.put(brokerId, null);

				// only do the mapping for the broker if it has neighbors
				for (Object addrObj2 : neighborAddrMapSet.getSet(brokerUri)) {
//					System.out.println("Neighbors of " + brokerUri + " is " + (String)addrObj2);
//					System.out.flush();
					String neighborUri = addrObj2.toString();
					String neighborId = addrToIdMap.get(neighborUri);
//					System.out.println("Neighbor ID is " + neighborId);
					// add to our brokerId to neighborId map set
//					neighborIdMapSet.put(brokerId, neighborId);
//					neighborIdMapSet.put(neighborId, brokerId); // bi-directional
					neighborIdMapSet.put(brokerUri, neighborUri);
					neighborIdMapSet.put(neighborUri, brokerUri); // bi-directional
				}
			}

			// Clean up first
			filenameReader.close();
			filenameReader = null;
			neighborAddrMapSet.clear();
			neighborAddrMapSet = null;

			// all done, return final result, whew!
			return true;

		} catch (Exception e) {
			System.out.println("ERROR: (Deployer) In loadInfoFromPhaseOneCmdFile():\n" + e.getMessage());
			e.printStackTrace(System.out);
			System.out.println("Your topology file probably has neighbors that are incorrectly defined");
			System.out.flush();
			return false;
		}
	}

	/**
	 * @return
	 */
	public Properties getConfigProps() {
		return configProps;
	}

}

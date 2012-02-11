package ca.utoronto.msrg.padres.tools.panda;

/*
 * Created on May 18, 2006
 *
 */

/**
 * @author cheung
 *
 * Subscribes to BROKER_INFO messages from a broker and tells the deployment
 * coordinator when all brokers and links have established.  All this runs in 
 * a separate thread.
 */

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.datastructure.HashMapSet;
import ca.utoronto.msrg.padres.common.util.io.QuickPrompt;

public class PhaseOneMonitoringClient extends Client {

	// Note: not many member variables below are declared final because this is only a temporary
	// class and I want to be able to nullify them when this class is done.

	// contains all broker uri to id map in phase-I of deployment
	private Map<String, String> addrToIdMap;

	// maps a broker's id to its set of neighbors' ids
	private HashMapSet neighborUriMapSet;

	// Stores all broker ids in phase-I deployment
	private Set<String> brokerUriSet;

	// we need to call back to the deployer when all brokers and
	// links have started
	private Deployer deployer;

	// Number of retries on connecting to a broker
	private final int rmiRetryLimit;

	// Does not ask user for any inputs on timeouts
	private final boolean noPrompt;

	// Publish NETWORK_DISCOVERY messages periodically to pull for BROKER_INFO
	private Timer discoveryTimer; // force brokers to publish BROKER_INFO

	// id used to unsubscribe from previously subscribed BROKER_INFO message
	// or unadvertise from NETWORK_DISCOVERY
	private String unsubscriptionId = null;

	private String unadvId = null;

	private static final String BROKER_INFO_SUB_STR = "[class,eq,BROKER_INFO]";

	private static final String ADV_NETWORK_DISCOVERY_STR = "[class,eq,NETWORK_DISCOVERY]";

	private static final String PUB_NETWORK_DISCOVERY_STR = "[class,NETWORK_DISCOVERY]";

	private static final String CONNECTION_RETRIES = "10";

	private static final String NETWORK_DISCOVERY_TIMER = "30"; // seconds

	// This is called when discoveryTimer times out
	private final ActionListener discoveryTimeoutHandler = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			publishNetworkDiscovery();
		}
	};

	/**
	 * Constructor
	 * 
	 * @param address
	 * @throws ClientException
	 * @throws RemoteException
	 */
	public PhaseOneMonitoringClient(HashMapSet theNeighborUriMapSet,
			Map<String, String> theAddrToIdMap, Deployer theDeployer, boolean auto)
			throws ClientException {
		super(new ClientConfig());
		clientID = "PHASE_ONE_MONITORING_CLIENT";
		deployer = theDeployer;
		neighborUriMapSet = theNeighborUriMapSet;
		addrToIdMap = theAddrToIdMap;
		noPrompt = auto;
		brokerUriSet = Collections.synchronizedSet(new HashSet<String>(addrToIdMap.keySet()));
		rmiRetryLimit = Integer.parseInt(deployer.getConfigProps().getProperty(
				"deployer.phase1.retries", CONNECTION_RETRIES));

		int discoveryPeriod = Integer.parseInt(deployer.getConfigProps().getProperty(
				"deployer.phase1.discovery.timer", NETWORK_DISCOVERY_TIMER)) * 1000;
		discoveryTimer = new Timer(discoveryPeriod, discoveryTimeoutHandler);
	}

	/**
	 * Starts monitoring process
	 * 
	 * @throws ClientException
	 * @throws ParseException 
	 */
	public void run() throws ClientException, ParseException {
		if (advertiseNetworkDiscovery()) {
			if (subscribeToBrokerInfo()) {
				System.out.println("OK");
				// force brokers to publish BROKER_INFO periodically
				discoveryTimer.start();
			} else {
				deployer.notifyPhaseOneFailed();
			}
		} else {
			deployer.notifyPhaseOneFailed();
		}
	}

	/*
	 * Need to try all ports to a broker
	 */
	public boolean connect() throws MalformedURLException, ClientException {
		String[] strBrokerUris = new String[1];
		strBrokerUris = addrToIdMap.keySet().toArray(strBrokerUris);

		// Keep trying until the user says 'no'
		while (true) {
			// Try to connect to N random brokers
			for (int retry = 0; retry < rmiRetryLimit; retry++) {
				// Pick a broker to connect to
				String strBrokerUri = strBrokerUris[retry % strBrokerUris.length];

				System.out.println("Connecting to " + strBrokerUri + "...");
				if (super.connect(strBrokerUri) != null) {
					// Success!
					System.out.println("PhaseOneMonitoringClient: Successfully connected to "
							+ strBrokerUri);
					return true;
				}
			}

			// Just keep retrying to connect if no prompt is set to true
			if (!noPrompt) {
				QuickPrompt quickPrompt = new QuickPrompt(
						"Cannot connect to a broker in deployment.  Try again?");
				quickPrompt.addResponse("Y");
				quickPrompt.addResponse("N");

				if (quickPrompt.promptAndGetResponse().equalsIgnoreCase("N")) {
					return false;
				}
			}
		}
	}

	private boolean advertiseNetworkDiscovery() throws ClientException, ParseException {
		if (super.isConnected()) {
			Advertisement adv = MessageFactory.createAdvertisementFromString(ADV_NETWORK_DISCOVERY_STR);
			AdvertisementMessage advMsg = super.advertise(adv, null);
			if (advMsg != null) {
				unadvId = advMsg.getMessageID();
				return true;
			} else {
				System.out.println("PhaseOneMonitoringClient: An error was encountered when "
						+ "advertising network discovery.");
				return false;
			}
		} else {
			return false;
		}
	}

	private void publishNetworkDiscovery() {
		if (isConnected()) {
			try {
				Publication pub = MessageFactory.createPublicationFromString(PUB_NETWORK_DISCOVERY_STR);
				super.publish(pub, null);
			} catch (Exception e) {
				exceptionLogger.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private boolean subscribeToBrokerInfo() throws ClientException, ParseException {
		Subscription subscription = MessageFactory.createSubscriptionFromString(BROKER_INFO_SUB_STR);
		SubscriptionMessage subMsg = super.subscribe(subscription, null);
		if (subMsg != null) {
			unsubscriptionId = subMsg.getMessageID();
			return true;
		} else {
			System.out.println("PhaseOneMonitoringClient: An error was encountered when "
					+ "subscribing to broker info.");
			return false;
		}
	}

	/*
	 * Simply remove the observed entries in BROKER_INFO from 'neighborIdMapSet' Deployment is
	 * complete when 'neighborIdMapSet' is empty
	 */
	private void updateCheckList(Publication pub) {
		Map<String, Serializable> pairMap = pub.getPairMap();
		if (pairMap.get("class").toString().equalsIgnoreCase("BROKER_INFO")) {

			String brokerUri = pairMap.get("brokerID").toString();
			// update our accounting
			try {
				synchronized (brokerUriSet) {
					if (brokerUriSet.remove(brokerUri)) {
						System.out.println("PhaseOneMonitoringClient: Broker " + brokerUri
								+ " is up and running.");
					}
				}
				clientLogger.debug("remove broker id from brokerIdSet OK");
			} catch (Exception e) {
				clientLogger.error("EXCEPTION in remove broker id from brokerIdSet: " + e);
			}

			ConcurrentHashMap payload = null;
			try {
				payload = new ConcurrentHashMap((Map) pub.getPayload());

				// Check!
				if (payload == null) {
					System.out.println("WARNING: (PhaseOneMonitoringClient) Received a "
							+ "BROKER_INFO message from " + brokerUri + " with null payload."
							+ "  Ignoring message.");
					return;
				}
				clientLogger.debug("retrieving payload OK");
			} catch (Exception e) {
				clientLogger.error("EXCEPTION in retrieving payload: " + e);
			}

			Set neighborSet = null;
			try {
				neighborSet = new HashSet((Set) payload.get(SystemMonitor.NEIGBOURS));
				// Check!
				if (neighborSet == null) {
					System.out.println("WARNING: (PhaseOneMonitoringClient) Received a "
							+ "BROKER_INFO message with null neighbor set from broker " + brokerUri
							+ ".  Ignoring message.");
					return;
				}
				clientLogger.debug("retrieving neighbor set from payload OK");
			} catch (Exception e) {
				clientLogger.error("EXCEPTION in retrieving neighbor set from payload: " + e);
			}

			try {
				// Update our accounting.
				// Clear all neighbor ids found in our 'neighborIdMapSet'
				for (Iterator neighborUris = neighborSet.iterator(); neighborUris.hasNext();) {
					String neighborUri = neighborUris.next().toString();
					if (neighborUriMapSet.remove(brokerUri, neighborUri)) {
						System.out.println("PhaseOneMonitoringClient: Link " + brokerUri + "-"
								+ neighborUri + " is established.");
					}
				}
				clientLogger.debug("Updating neigborIdMapSet OK");
			} catch (Exception e) {
				clientLogger.error("EXCEPTION in Updating neigborIdMapSet: " + e);
			}

			// Check if deployment is complete yet
			if (neighborUriMapSet.isEmpty()) {
				deployer.notifyPhaseOneOk();
			}

			neighborSet = null;
		} else {
			System.out.println("PhaseOneMonitoringClient: Got an unexpected publication message:\n"
					+ pub.toString());
		}

		pairMap = null;
	}

	/**
	 * Displays the status of phase-I deployment on the screen
	 * 
	 */
	public void showStatus() {
		String output;

		if (neighborUriMapSet.isEmpty()) {
			output = "All brokers and links have successfully deployed in Phase-I.\n";
		} else {
			String brokerIdSetString;
			synchronized (brokerUriSet) {
				brokerIdSetString = brokerUriSet.toString().replaceAll("\\[", "").replaceAll("\\]",
						"");
			}

			output = "Pending brokers:\n"
					+ brokerIdSetString
					+ "\n\n"
					+ "Pending links: "
					+ neighborUriMapSet.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(
							":", "->").replaceAll(";", "\n") + "\n";
		}

		System.out.println(output);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see universal.rmi.RMIUniversalClient#shutdown()
	 */
	public void shutdown() throws ClientException {
		if (super.isConnected()) {
			if (unsubscriptionId != null)
				super.unSubscribe(unsubscriptionId);
			if (unadvId != null)
				super.unAdvertise(unadvId);
			super.disconnectAll();
		}

		addrToIdMap = null;
		brokerUriSet = null;
		neighborUriMapSet = null;
		deployer = null;
		unsubscriptionId = null;
		unadvId = null;
		if (discoveryTimer != null)
			discoveryTimer.stop();
		discoveryTimer = null;
	}

	/*
	 * (non-Javadoc)
	 * @see ca.utoronto.msrg.padres.client.Client#processMessage(ca.utoronto.msrg.padres.common.message.Message)
	 */
	public void processMessage(Message msg) {
		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage)msg).getPublication();
			System.out.println("Got BROKER_INFO!!!");
			updateCheckList(pub);
		}
	}
}

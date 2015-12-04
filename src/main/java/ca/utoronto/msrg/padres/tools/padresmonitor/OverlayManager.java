package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.HeartbeatSubscriber;
import ca.utoronto.msrg.padres.broker.controller.ServerInjectionManager;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorVertex;

/**
 * Manages the connection to the overlay network. Also responsible for publishing user commands to
 * the network.
 */
public class OverlayManager extends QueueHandler {

	/** MonitorFrame that owns this OverlayManager. */
	private MonitorFrame monitorFrame;

	private ClientMonitorCommandManager cmdManager;

	/** Manager handling connection to the broker */
	private MonitorClient monitorClient = null;

	/** The graphical representation of the overlay network. */
	private OverlayUI overlayInterface;

	/** Manager handling injection of message into broker */
	private ClientInjectionManager msgInjectManager = null;

	/** ADDED */
	private Map<String, Set<String>> brokerNeighbors;

	private String traceSubId;

	private JScrollPane scrollPane;

	private Vector<String> traceMsgID;

	// for testing, store the current processed publication
	private Publication currentPub = null;

	private static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Construct a new OverlayManager with the specified MonitorFrame.
	 * 
	 * @param monitorFrame
	 *            MonitorFrame that owns this OverlayManager.
	 * @throws ClientException
	 */
	public OverlayManager(MonitorFrame monitorFrame, ClientMonitorCommandManager comm,
			MonitorClient monitorClient) throws ClientException {
		super(monitorClient.getClientDest());
		// hook to the MonitorFrame
		this.monitorFrame = monitorFrame;
		this.cmdManager = comm;
		this.monitorClient = monitorClient;
		brokerNeighbors = new HashMap<String, Set<String>>();

		overlayInterface = new JungOverlayUI(this.monitorFrame, cmdManager);
		scrollPane = new JScrollPane(overlayInterface);
		scrollPane.setVisible(true);
		// Create the injection manager
		msgInjectManager = new ClientInjectionManager(monitorFrame);
		traceSubId = "";
		traceMsgID = new Vector<String>();
	}

	/**
	 * Return the GUI component that represents the overlay network.
	 * 
	 * @return Graphical representation of the overlay network.
	 */
	public OverlayUI getOverlayUI() {
		return overlayInterface;
	}

	public void clearUI() {
		overlayInterface.clear();
		brokerNeighbors.clear();
	}

	/**
	 * Connect to the federation of brokers through a single broker. Also, advertise and subscribe
	 * to the appropriate monitor messages.
	 * 
	 * @param brokerURL
	 *            URL of the broker to connect. in the format of rmi://hostname[:port]/broker_id
	 * @param port
	 *            Broker's port number.
	 * @return boolean true if successful.
	 * @throws ClientException
	 */
	public void connect(String brokerURL) throws ClientException {
		monitorClient.connect(brokerURL, this);
		
		try {
			// subscribe to broker info messages
			monitorClient.subscribe(MessageFactory.createSubscriptionFromString("[class,eq,BROKER_INFO]"));
			// subscribe to broker failure detection message and broker failure state clear message
			monitorClient.subscribe(MessageFactory.createSubscriptionFromString("[class,eq," + HeartbeatSubscriber.MESSAGE_CLASS
					+ "]," + "[detectorID,isPresent,'TEXT']," + "[detectedID,isPresent,'TEXT'],"
					+ "[type,isPresent,'TEXT']"));
			// advertise that we can publich commands
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_MONITOR],[command,isPresent,'TEXT'],"
							+ "[brokerID,isPresent,'TEXT'],"
							+ "[PUBLICATION_INTERVAL,isPresent,12345],"
							+ "[TRACEROUTE_ID,isPresent,'12345']"));
		
			// advertise that we will be publishing network discovery messages
			monitorClient.advertise(MessageFactory.createAdvertisementFromString("[class,eq,NETWORK_DISCOVERY]"));
			// advertise that we can publish global failure detection enable/disable
			monitorClient.advertise(MessageFactory.createAdvertisementFromString("[class,eq,GLOBAL_FD],[flag,isPresent,'TEXT']"));
			// advertise injection Command (adv)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_ADV_MSG + "]"));
			// advertise injection Command (sub)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_SUB_MSG + "]"));
			// advertise injection Command (pub)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_PUB_MSG + "]"));
			// advertise injection Command (flush pub)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_FLUSH_PUB_MSG + "]"));
			// advertise injection Command (unsub)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_UNSUB_MSG + "]"));
			// advertise injection Command (unadv)
			monitorClient.advertise(MessageFactory.createAdvertisementFromString(
					"[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
							+ ServerInjectionManager.INJECTION_ID_TAG
							+ ",isPresent,'DUMMY_INJECTION_ID']," + "[command,isPresent,"
							+ ServerInjectionManager.CMD_UNADV_MSG + "]"));
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			throw new ClientException(e);
		}

		try {
			// Gerald: It is a hack for racing condition where We need to wait until the adv go
			// through the network before publish network discovery
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}
		publishNetDiscovery();
	}

	public void publishNetDiscovery() throws ClientException {
		try {
			monitorClient.publish(MessageFactory.createPublicationFromString("[class,NETWORK_DISCOVERY]"));
		} catch (ParseException e) {
			throw new ClientException(e);
		}
	}

	public void applyLayout(int algorithm) {
		if (isConnected()) {
			overlayInterface.applyLayout(algorithm);
		}
	}

	public void publishGlobalFD(boolean enable) throws ClientException {
		Publication p;
		try {
			p = MessageFactory.createPublicationFromString("[class,GLOBAL_FD],[flag,'" + enable + "']");
		} catch (ParseException e) {
			throw new ClientException(e);
		}
		
		monitorClient.publish(p);
	}

	/**
	 * Disconnect from broker federation.
	 * 
	 * @return true if successful.
	 */
	public boolean disconnect() {
		boolean successUntrace = true;
		if (!traceSubId.equals("")) {
			try {
				monitorClient.untracePublicationMessage(traceSubId);
			} catch (ClientException e) {
				successUntrace = false;
				monitorFrame.setErrorString(e.getMessage());
			}
		}

		boolean successDisconnect = true;
		try {
			monitorClient.disconnect();
		} catch (ClientException e) {
			successDisconnect = false;
			monitorFrame.setErrorString(e.getMessage());
		}
		
		return successUntrace && successDisconnect;
	}

	/**
	 * Send a publication message telling the specified broker to stop.
	 * 
	 * @param brokerID
	 *            ID of the broker to be stopped.
	 * @return true if the publication was sent successfully.
	 */
	public boolean stopBroker(String brokerID) {
		// create the command message
		Publication command;
		try {
			command = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID
					+ "'],[command,'STOP']");
		} catch (ParseException e1) {
			monitorFrame.setErrorString(e1.getMessage());
			return false;
		}

		// LOG MESSAGE
		// System.out.println(command);
		try {
			monitorClient.publish(command);
		} catch (ClientException e) {
			monitorFrame.setErrorString(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Send a publication message telling the specified broker to resume.
	 * 
	 * @param brokerID
	 *            ID of the broker to resume.
	 * @return true if the publication was sent successfully.
	 */
	public boolean resumeBroker(String brokerID) {
		// create the command message
		Publication command;
		try {
			command = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID
					+ "'],[command,'RESUME']");
		} catch (ParseException e1) {
			monitorFrame.setErrorString(e1.getMessage());
			return false;
		}

		try {
			monitorClient.publish(command);
		} catch (ClientException e) {
			monitorFrame.setErrorString(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Send a publication message telling the specified broker to shutdown.
	 * 
	 * @param brokerID
	 *            ID of the broker to be shutdown.
	 * @return true if the publication was sent successfully.
	 */
	public boolean shutdownBroker(String brokerID) {
		// create the command message
		Publication command;
		try {
			command = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID
					+ "'],[command,'SHUTDOWN']");
		} catch (ParseException e1) {
			monitorFrame.setErrorString(e1.getMessage());
			return false;
		}
		
		try {
			monitorClient.publish(command);
		} catch (ClientException e) {
			monitorFrame.setErrorString(e.getMessage());
			return false;
		}
		return true;
	}

	public boolean isConnected() {
		return monitorClient.isConnected();
	}

	/**
	 * Get the broker selected in the overlay diagram and return it. If no broker is selected just
	 * return null.
	 * 
	 * @return The selected broker.
	 */
	public String getSelectedBroker() {
		String brokerID = null;
		BrokerUI broker = overlayInterface.getSelectedBrokerUI();
		if (broker != null) {
			brokerID = broker.getBrokerID();
		}

		return brokerID;
	}

	/**
	 * Get the broker selected in the overlay diagram and return it. If no broker is selected just
	 * return null.
	 * 
	 * @return The selected brokerui.
	 */
	public BrokerUI getSelectedBrokerUI() {

		BrokerUI broker = overlayInterface.getSelectedBrokerUI();
		return broker;
	}

	/** ADDED */
	boolean updateRequired(BrokerUI broker) {
		// NOTE: To add or remove this functionality, simply comment/uncomment the following line.
		// if ( 1== 1) return true;
		String bID = broker.getBrokerID();
		Set<String> neighbors = brokerNeighbors.get(bID);

		if (neighbors == null) {
			neighbors = new HashSet<String>();
			for (Iterator<String> it = broker.neighbourIterator(); it.hasNext();) {
				neighbors.add(it.next());
			}
			for (Iterator<String> it = broker.clientIterator(); it.hasNext();) {
				neighbors.add(it.next());
			}
			brokerNeighbors.put(bID, neighbors);
			// System.out.println("*** Adding new broker.\n");
			return true;
		} else {
			int oldSize = neighbors.size();
			int newSize = 0;
			for (Iterator<String> it = broker.neighbourIterator(); it.hasNext();) {
				newSize++;
				if (neighbors.add(it.next())) {
					brokerNeighbors.remove(bID);
					// System.out.println("*** A new neighbor.\n");
					return updateRequired(broker);
				}
			}
			for (Iterator<String> it = broker.clientIterator(); it.hasNext();) {
				newSize++;
				if (neighbors.add(it.next())) {
					brokerNeighbors.remove(bID);
					// System.out.println("*** A new client.\n");
					return updateRequired(broker);
				}
			}
			if (oldSize != newSize) {
				brokerNeighbors.remove(bID);
				// System.out.println("*** A missing neighbor.\n");
				return updateRequired(broker);
			}
		}
		// System.out.println("*** No change.\n");
		return false;
	}

	/**
	 * Handle an incoming broker info. message.
	 * 
	 * @param payload
	 *            The body of the broker info. message.
	 */
	private void handleBrokerInfoMessage(ConcurrentHashMap<String, Object> payload) {
		BrokerUI broker = new BrokerUI(payload);
		/** ADDED */
		if (!updateRequired(broker))
			return;
		// Add the borker in the overlay
		overlayInterface.addBroker(broker);
		overlayInterface.activeteBroker(broker);
		overlayInterface.removeOldClient(broker, broker.getClientSet());
		// now add it's neighbors and links
		for (Iterator<String> i = broker.neighbourIterator(); i.hasNext();) {
			BrokerUI neighbour = new BrokerUI(i.next().toString());
			overlayInterface.addBroker(neighbour);
			overlayInterface.addNeighbour(broker, neighbour);
		}
	}

	public boolean traceByTraceID(String traceID) {
		try {
			Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,TRACEROUTE_MESSAGE],[TRACEROUTE_ID,eq,'"
					+ traceID + "']");
			String msgID = monitorClient.subscribe(sub).getMessageID();
			traceMsgID.add(msgID);
		} catch (ClientException e) {
			return false;
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	public boolean untraceByTraceID() {
		for (String stringRep : traceMsgID) {
			try {
				monitorClient.unSubscribe(stringRep);
				overlayInterface.deactivateAllEdge();
			} catch (ClientException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Subscribes to broker messages that indicate they have received the publication messsage. Then
	 * tells the specified broker to advertise and then publish the specified publicatin message.
	 * After the broker publishes the message, the broker unadvertises
	 * 
	 * @return true if successful
	 */
	public boolean tracePublicationMessage(String brokerID, String strPublication) {
		boolean result;
		if (traceSubId == "") {
			try {
				traceSubId = monitorClient.tracePublicationMessage(brokerID, strPublication);
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
				result = false;
			}
			// trace route fail or success
			result = traceSubId == "" ? false : true;
		} else {
			// There is still an active trace route message unsubscribe it first
			result = false;
		}
		return result;
	}

	/**
	 * Subscribes to broker messages that indicate they have received the subscription messsage.
	 * 
	 * @return true if successful
	 */
	public boolean traceSubscriptionMessage(String brokerID, String strSubscription) {
		boolean result;
		if (traceSubId == "") {
			try {
				traceSubId = monitorClient.traceSubscriptionMessage(brokerID, strSubscription);
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
				result = false;
			}
			// trace route fail or success
			result = traceSubId == "" ? false : true;
		} else {
			// There is still an active trace route message unsubscribe it first
			result = false;
		}
		return result;
	}

	/**
	 * Unsubscribes from the current traceroute messages Note: We only support viewing one
	 * publication at a time right now
	 * 
	 * @return true if successful
	 */
	public boolean untraceMessage() {
		boolean result;
		if (traceSubId != "") {
			try {
				monitorClient.untracePublicationMessage(traceSubId);
				result = true;
				traceSubId = "";
				overlayInterface.deactivateAllEdge();
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
				result = false;
			}
		} else {
			monitorFrame.setErrorString("No Active Trace Message");
			result = false;
		}
		return result;
	}

	public boolean injectMessage(int msgType, String msgBody, String brokerID) throws ParseException {
		boolean result = false;
		if (msgBody.equals("")) {
			monitorFrame.setErrorString("Cannot send blank message");
			return result;
		}
		result = msgInjectManager.injectMessage(msgType, msgBody, brokerID, monitorClient);
		return result;
	}

	public boolean uninjecMessage(InjectMessageStore cell, String brokerID) throws ParseException {
		return msgInjectManager.uninjectMessage(cell, brokerID, monitorClient);
	}

	public ClientInjectionManager getClientInjectionManager() {
		return msgInjectManager;
	}

	public MonitorClient getMonitorClient() {
		return monitorClient;
	}

	/*
	 * Add for testing
	 */
	public Publication getCurrentPub() {
		return currentPub;
	}

	public boolean setHeartbeatParameters(String brokerID, boolean enabled, long interval,
			long timeout, int threshold) {
		Publication command;
		try {
			command = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID
					+ "'],[command,'SET_FD_PARAMS']");
			// jam the heartbeat parameters into the payload
			ConcurrentHashMap<String, String> payload = new ConcurrentHashMap<String, String>();
			payload.put("enabled", "" + enabled);
			payload.put("interval", "" + interval);
			payload.put("timeout", "" + timeout);
			payload.put("threshold", "" + threshold);
			command.setPayload(payload);
			// publish
			monitorClient.publish(command);
		} catch (ClientException e) {
			monitorFrame.setErrorString(monitorClient.getErrorStr());
			return false;
		} catch (ParseException e1) {
			monitorFrame.setErrorString(e1.toString());
			return false;
		}
		return true;
	}

	public Map<String, Set<String>> getBrokerList() {
		return brokerNeighbors;
	}

	/**
	 * Display message counter for all edges
	 * 
	 */
	public void showAllEdgeMessages() {
		if (isConnected()) {
			overlayInterface.showAllEdgeMessages();
		}
	}

	/**
	 * Hide message counter for all edges
	 * 
	 */
	public void hideAllEdgeMessages() {
		if (isConnected()) {
			overlayInterface.hideAllEdgeMessages();
		}
	}

	public void setEdgeThroughputIndicator(boolean state) {
		if (isConnected()) {
			overlayInterface.setEdgeThroughputIndicator(state);
		}
	}

	public void resetEdgeThroughputIndicator() {
		if (isConnected()) {
			overlayInterface.resetEdgeThroughputIndicator();
		}
	}

	public void useNodeLabelType(MonitorVertex.LabelType type) {
		if (isConnected()) {
			overlayInterface.useNodeLabelType(type);
		}
	}

	@Override
	public void processMessage(Message msg) {
		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage) msg).getPublication();
			currentPub = pub;
			messagePathLogger.info("Publication: " + currentPub);

			Map<String, Serializable> header = pub.getPairMap();
			String msgType = header.get("class").toString();
			if (msgType.equals("BROKER_INFO")) {
				// we only care about BROKER_INFO messages
				ConcurrentHashMap payload = (ConcurrentHashMap) pub.getPayload();
				handleBrokerInfoMessage(payload);
			} else if (msgType.equals("TRACEROUTE_MESSAGE")) {
				String toID = (String) header.get("to");
				String fromID = (String) header.get("from");
				if (!(fromID.indexOf("-") == -1)) {
					// last hop is not a broker.
					BrokerUI broker1 = new BrokerUI(toID);
					overlayInterface.activeClientBrokerEdge(broker1, fromID);
				} else if (!(toID.indexOf("-") == -1)) {
					// next hop is not a broker.
					BrokerUI broker1 = new BrokerUI(fromID);
					overlayInterface.activeClientBrokerEdge(broker1, toID);
				} else {
					BrokerUI broker1 = new BrokerUI(toID);
					BrokerUI broker2 = new BrokerUI(fromID);
					overlayInterface.activateEdge(broker1, broker2);
				}
			} else if (msgType.equals(SystemMonitor.MSG_SET_DELIVERY_CLASS)) {
				cmdManager.handleMessage(pub);
			} else if (msgType.equals(HeartbeatSubscriber.MESSAGE_CLASS)) {
				String detectorBrokerID = (String) header.get("detectorID");
				String failureBrokerID = (String) header.get("detectedID");
				String type = (String) header.get("type");
				overlayInterface.handleFailureClassMsg(detectorBrokerID, failureBrokerID, type);
			}
		}
	}

}

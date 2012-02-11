package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

/**
 * This class encapsulates all the properties of a BrokerUI element. This class has package level
 * visibility because it should only ever be used in an OverlayUI subclass which must be in this
 * package.
 */
public class BrokerUI {

	public enum BrokerStatus {
		BROKER_STATUS_RUNNING, BROKER_STATUS_STOPPED, BROKER_STATUS_SHUTDOWN, BROKER_STATUS_UNKNOWN
	}

	/**
	 * A flag to indicate this bokerUI contain full information for a broker or not
	 */
	private boolean hasFullInfo;

	/** ID of the broker represented by this BrokerUI. */
	private String mBrokerID;

	/** Broker's current status. */
	private BrokerStatus mBrokerStatus;

	/** The set of this broker's neighbouring brokers. */
	private Set<String> mNeighbours;

	/** The set of this broker's clients */
	private Set<String> mClients;

	/** The broker's URI. */
	private String mBrokerURI;

	/** The publication interval */
	private String mPubInterval;

	/** The input queue size */
	private String mInputQueueSize;

	/** The Incoming Publication Message Rate */
	private String mInPubRate;

	/** The Incoming Control Message Rate */
	private String mInConRate;

	/** The Queue Time */
	private String mQueueTime;

	/** The Match Time */
	private String mMatchTime;

	/** The OS info name */
	private String mOsInfo;

	/** Port **/
	private String mPort;

	/** Host name **/
	private String mHostName;

	/** JVM version **/
	private String mJVMversion;

	/** JVM vendor **/
	private String mJVMVendor;

	/* The set of edge id of the connection(edge) that have failed */
	private HashSet<String> mFailureEdge;

	// TODO: add all other broker fields/properties as members

	/**
	 * Construct a BrokerUI for the given broker ID. All other fields take on default values. This
	 * constructor is needed because we can get a broker ID as a neighbour of another broker before
	 * actually getting a broker info. message from that broker.
	 * 
	 * @param brokerID
	 *            ID of the broker.
	 */
	public BrokerUI(String brokerID) {
		// set the broker ID
		mBrokerID = brokerID;

		// default values for the rest
		mBrokerStatus = BrokerStatus.BROKER_STATUS_UNKNOWN;
		// m_Neighbours = new HashSet();
		mNeighbours = Collections.synchronizedSet(new HashSet<String>());
		// m_Clients = new HashSet();
		mClients = Collections.synchronizedSet(new HashSet<String>());
		mBrokerURI = null;
		hasFullInfo = false;
		mFailureEdge = new HashSet<String>();

	}

	/**
	 * Construct a BrokerUI from a Map object. The map object is typically the payload portion of a
	 * broker info. message.
	 * 
	 * @param map
	 *            Map of key-value pairs carrying properties of a broker.
	 */
	public BrokerUI(ConcurrentHashMap<String, Object> map) {
		String brokerID = (String) map.get("Broker ID");
		String brokerStatus = (String) map.get("Broker Status");
		Set<MessageDestination> neighbourSet = (Set<MessageDestination>) map.get(SystemMonitor.NEIGBOURS);
		Set<MessageDestination> clientSet = (Set<MessageDestination>) map.get(SystemMonitor.CLIENTS);
		String brokerURI = (String) map.get("Broker URI");
		String pubinterval = (String) map.get("Publication Interval");
		String inputQueueSize = (String) map.get("InputQueue Size");
		String inPubRate = (String) map.get("Incoming Publication Message Rate");
		String inConRate = (String) map.get("Incoming Control Message Rate");
		String queueTime = (String) map.get("Queue Time");
		String matchTime = (String) map.get("Match Time");
		String osInfo = (String) map.get("Operating System");
		String uri = (String) map.get("URI");
		String jvmVersion = (String) map.get(SystemMonitor.JVM_VERSION);
		String jvmVendor = (String) map.get(SystemMonitor.JVM_VENDOR);

		// TODO: should assert if brokerID is null!
		BrokerStatus status;
		if (brokerStatus == null)
			status = BrokerStatus.BROKER_STATUS_UNKNOWN;
		else if (brokerStatus.equals("RUNNING"))
			status = BrokerStatus.BROKER_STATUS_RUNNING;
		else if (brokerStatus.equals("STOPPED"))
			status = BrokerStatus.BROKER_STATUS_STOPPED;
		else if (brokerStatus.equals("SHUTDOWN"))
			status = BrokerStatus.BROKER_STATUS_SHUTDOWN;
		else
			status = BrokerStatus.BROKER_STATUS_UNKNOWN;

		if (brokerURI == null)
			brokerURI = "";

		mBrokerID = brokerID;
		mBrokerStatus = status;
		mNeighbours = Collections.synchronizedSet(new HashSet<String>());
		// m_Clients = new HashSet();
		mClients = Collections.synchronizedSet(new HashSet<String>());
		mBrokerURI = brokerURI;
		mPubInterval = pubinterval;
		mInputQueueSize = inputQueueSize;
		mInPubRate = inPubRate;
		mInConRate = inConRate;
		mQueueTime = queueTime;
		mMatchTime = matchTime;
		mOsInfo = osInfo;
		mPort = "undefined";
		mHostName = "undefined";
		try {
			NodeAddress brokerAddr = NodeAddress.getAddress(uri);
			if (brokerAddr.getType() == CommSystemType.RMI) {
				mPort = String.valueOf(((RMIAddress) brokerAddr).getPort());
				mHostName = ((RMIAddress) brokerAddr).getHost();
			} else if (brokerAddr.getType() == CommSystemType.SOCKET) {
				mPort = String.valueOf(((SocketAddress) brokerAddr).getPort());
				mHostName = ((SocketAddress) brokerAddr).getHost();
			}
		} catch (CommunicationException e) {
			e.printStackTrace();
		}
		mJVMversion = jvmVersion;
		mJVMVendor = jvmVendor;

		recordNeighbours(neighbourSet);
		recordClients(clientSet);

		hasFullInfo = true;

		mFailureEdge = new HashSet<String>();

	}

	/**
	 * Add a broker to this BrokerUI's list of neighbors.
	 * 
	 * @param brokerID
	 *            ID of the new neighbour.
	 * @return true if the broker wasn't already neighbour and is now added.
	 */
	public boolean addNeighbour(String brokerID) {
		return mNeighbours.add(brokerID);
	}

	/**
	 * Remove a broker from this BrokerUI's neighbour list.
	 * 
	 * @param brokerID
	 *            ID of the ex-neighbour.
	 * @return true if the broker was a neighbour and is now removed.
	 */
	public boolean removeNeighbour(String brokerID) {
		return mNeighbours.remove(brokerID);
	}

	/**
	 * Determine if the specified broker is a neighbour of the broker represented by this BrokerUI.
	 * 
	 * @param brokerID
	 *            ID of the broker.
	 * @return true if the specified broker is a neighbour.
	 */
	public boolean isNeighbour(String brokerID) {
		return mNeighbours.contains(brokerID);
	}

	/**
	 * Get an iterator that iterates over all neighbours.
	 * 
	 * @return The neighbours list iterator.
	 */
	public Iterator<String> neighbourIterator() {
		return mNeighbours.iterator();
	}

	public Set<String> getNeighbourSet() {
		return mNeighbours;
	}

	/**
	 * Get an iterator that iterates over all failEdges.
	 * 
	 * @return The neighbours list iterator.
	 */

	public Set<String> getFailureEdge() {
		return mFailureEdge;
	}

	/**
	 * Get an iterator that iterates over all clients
	 * 
	 * @return The client list iterator.
	 */
	public Iterator<String> clientIterator() {
		return mClients.iterator();
	}

	/**
	 * Get the broker ID of this BrokerUI.
	 * 
	 * @return Ther broker ID.
	 */
	public String getBrokerID() {
		return mBrokerID;
	}

	/**
	 * Get this BrokerUI's status.
	 * 
	 * @return Broker status.
	 */
	public BrokerStatus getBrokerStatus() {
		return mBrokerStatus;
	}

	/* Return the port that this broker is using */
	public String getPort() {
		return mPort;
	}

	/* Return the host name that this borker is using */
	public String getHostName() {
		return mHostName;
	}

	/* Return the JVM version that this borker is using */
	public String getJVMVersion() {
		return mJVMversion;
	}

	/* Return the JVM vendor that this borker is using */
	public String getJVMVendor() {
		return mJVMVendor;
	}

	/**
	 * Set this BrokerUI's status.
	 * 
	 * @param status
	 *            New status.
	 */
	public void setBrokerStatus(BrokerStatus status) {
		mBrokerStatus = status;
	}

	/**
	 * Get the URI for this BrokerUI.
	 * 
	 * @return Broker URI
	 */
	public String getBrokerURI() {
		return mBrokerURI;
	}

	/**
	 * Return the publication interval of this broker
	 * 
	 * @return publication interval
	 */
	public String getPubInterval() {
		return mPubInterval;
	}

	/**
	 * Return the input queue size of this borker
	 * 
	 * @return input queue size
	 */
	public String getInputQueueSize() {
		return mInputQueueSize;
	}

	/**
	 * Return the os info of this broker
	 * 
	 * @return os info
	 */
	public String getOsInfo() {
		return mOsInfo;
	}

	/**
	 * Return a string representation of this broker. Because broker IDs are unique, simply return
	 * the broker ID (as a String).
	 * 
	 * @return The String representation of this BrokerUI.
	 */
	public String toString() {
		return mBrokerID;
	}

	/**
	 * Checks if this BrokerUI is equal to the passed in object. This method can only return true if
	 * the passed object is an instance of BrokerUI, and only if the ID is the same as this broker's
	 * ID. Because broker IDs are unique in an overlay network we only have to check this field, all
	 * others are ignored.
	 * 
	 * @param o
	 *            An Object.
	 */
	public boolean equals(Object o) {
		boolean equal = false;

		if (o instanceof BrokerUI) {
			if (mBrokerID.equals(((BrokerUI) o).getBrokerID())) {
				equal = true;
			}
		}

		return equal;
	}

	/**
	 * Return the hash code for this BrokerUI. The returned hash code is completely determined by
	 * the broker ID.
	 * 
	 * @return The hash code.
	 */
	public int hashCode() {
		return mBrokerID.hashCode();
	}

	/**
	 * Return the set of client of this broker
	 * 
	 * @return set of client
	 */
	public Set<String> getClientSet() {
		return mClients;
	}

	/* Private Function */

	/**
	 * It will turn the neighbour set from brokerInfo into a HashMap m_Neighbour will be modified.
	 * 
	 * @param neighbourSet
	 *            The set of neighbour recieved from the brokerInfo
	 */
	private void recordNeighbours(Set<MessageDestination> neighbourSet) {
		synchronized (neighbourSet) {
			// now add it's neighbors and links
			for (Iterator<MessageDestination> i = neighbourSet.iterator(); i.hasNext();) {
				mNeighbours.add(i.next().toString());
			}
		}

	}

	/**
	 * It will turn the client set from brokerInfo into a HashMap m_Client will be modified.
	 * 
	 * @param clientSet
	 *            The set of client received from the brokerInfo
	 */
	private void recordClients(Set<MessageDestination> clientSet) {
		synchronized (clientSet) {
			// now add it's neighbors and links
			for (Iterator<MessageDestination> i = clientSet.iterator(); i.hasNext();) {
				mClients.add(i.next().toString());
			}
		}
	}

	/**
	 * Return a boolean indicate that
	 * 
	 * @return true if this brokerUI have full information of the broker it represent
	 */
	public boolean hasFullInfo() {
		return hasFullInfo;
	}

	/**
	 * @return incoming control message rate
	 */
	public String getInConRate() {
		return mInConRate;
	}

	/**
	 * @return incoming publication message rate
	 */
	public String getInPubRate() {
		return mInPubRate;
	}

	/**
	 * @return match time
	 */
	public String getMatchTime() {
		return mMatchTime;
	}

	/**
	 * @return queue time
	 */
	public String getQueueTime() {
		return mQueueTime;
	}

	public boolean addFailureEdgeID(String edgeID) {
		boolean result = false;
		result = mFailureEdge.add(edgeID);
		return result;
	}

	public boolean removeFailureEdgeID(String edgeID) {
		boolean result = false;
		result = mFailureEdge.remove(edgeID);
		return result;
	}

	public boolean isInFailureState() {
		boolean result = false;
		result = (mFailureEdge.size() > 0);
		return result;
	}

}

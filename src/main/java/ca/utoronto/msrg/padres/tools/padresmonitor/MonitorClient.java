/*
 * Created on Apr 14, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.tools.padresmonitor;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Gerald
 * 
 *         The Connection Manager was implement using RMI.
 * 
 */
public class MonitorClient extends Client {

	/** The owner of this RMI connection */
	protected OverlayManager overlayMgr;

	private int traceCount;

	/**
	 * The string that contain the error message. It will be set when an error occur. Thus, the new
	 * error message will override the old one.
	 * 
	 * @author Gerald
	 */
	private String errorStr;

	public MonitorClient(String monitorID) throws ClientException {
		super(monitorID);
		traceCount = 0;
	}

	/**
	 * Connect to broker federation.
	 * 
	 * @return connected broker state if successful
	 * @throws ClientException
	 */
	public BrokerState connect(String remoteURL, OverlayManager overlayMgr) throws ClientException {
		this.overlayMgr = overlayMgr;
		return super.connect(remoteURL);
	}

	/**
	 * Disconnect from broker federation.
	 * 
	 * @return disconnection messages if successful
	 * @throws ClientException
	 */
	public String disconnect() throws ClientException {
		return super.disconnectAll();
	}

	/**
	 * Get the error String from the Connection Manager
	 * 
	 * @return errorStr
	 */
	public String getErrorStr() {
		return errorStr;
	}

	/**
	 * Subscribes to broker messages that indicate they have received the publication messsage. Then
	 * tells the specified broker to advertise and then publish the specified publicatin message.
	 * After the broker publishes the message, the broker unadvertises
	 * 
	 * @param brokerID
	 *            the broker that is going to publish the message
	 * @param strSubsciption
	 *            The actual string for the publication message
	 * 
	 * @return subscribtion Id of the trace route message
	 * @throws ClientException
	 */
	public String tracePublicationMessage(String brokerID, String strPublication)
			throws ClientException {
		String currentTracerouteID = getTraceID(defaultBrokerAddress.getNodeURI());
		Publication pub;
		SubscriptionMessage subMsg;
		try {
			Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,TRACEROUTE_MESSAGE],[TRACEROUTE_ID,eq,'"
					+ currentTracerouteID + "']");
			subMsg = subscribe(sub, null);
			pub = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID + "'],"
					+ "[command,'TRACE_PUBLICATION_MSG']," + "[TRACEROUTE_ID,'" + currentTracerouteID
					+ "']");
		} catch (ParseException e) {
			throw new ClientException(e);
		}
		pub.setPayload(strPublication);
		publish(pub, null);
		return subMsg.getMessageID();
	}

	// Unsubscribes from the current traceroute messages
	// Note: We only support viewing one publication at a time right now
	public void untracePublicationMessage(String tracerouteSubscriptionID) throws ClientException {
		unSubscribe(tracerouteSubscriptionID, null);
	}

	/**
	 * Subscribes to broker messages that indicate they have received the publication messsage. Then
	 * tells the specified broker to subscribe the specified subscription message.
	 * 
	 * @param brokerID
	 *            the broker that is going to subscribe the message
	 * @param strSubsciption
	 *            The actual string for the subscription message
	 * @return subscribtion Id of the trace route message
	 * @throws ClientException
	 */
	public String traceSubscriptionMessage(String brokerID, String strSubscription)
			throws ClientException {
		String currentTracerouteID = getTraceID(defaultBrokerAddress.getNodeURI());
		Publication pub;
		SubscriptionMessage subMsg;
		try {
			Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,TRACEROUTE_MESSAGE],[TRACEROUTE_ID,eq,'"
					+ currentTracerouteID + "']");
			subMsg = subscribe(sub, null);
			pub = MessageFactory.createPublicationFromString("[class,BROKER_MONITOR],[brokerID,'" + brokerID + "'],"
					+ "[command,'TRACE_SUBSCRIPTION_MSG']," + "[TRACEROUTE_ID,'" + currentTracerouteID
					+ "']");
		} catch (ParseException e) {
			throw new ClientException(e);
		}
		pub.setPayload(strSubscription);
		publish(pub, null);
		return subMsg.getMessageID();
	}

	public String getTraceID(String id) {
		String result;
		result = id + "-Trace-" + (new Integer(traceCount)).toString();
		traceCount++;
		return result;
	}

}

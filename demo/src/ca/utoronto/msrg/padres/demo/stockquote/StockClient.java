package ca.utoronto.msrg.padres.demo.stockquote;

/**
 * @author cheung
 *
 * Both virtual subscribers and publishers inherit this client
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;

public abstract class StockClient extends Client {

	// Keeps track of the brokers that we visited
	protected LinkedList<String> brokerHistory = null;

	// CONSTANTS
	// Size of the hash map in publication payload.
	protected static final short DEFAULT_CONNECTION_CAPACITY = 4; // assume likely 3 connections

	public static final String TOPIC_CLIENT_CONTROL = "CLIENT_CONTROL";

	public static final String CMD_CONNECT = "CONNECT";

	public static final String CMD_DISCONNECT = "DISCONNECT";

	protected static final String DEFAULT_CLIENT_ID = getCurrentDateTime();

	/**
	 * 
	 * @param id
	 * @throws ClientException
	 */
	protected StockClient(ClientConfig clientConfig) throws ClientException {
		super(clientConfig);
		if (brokerHistory == null) {
			brokerHistory = new LinkedList<String>();
		}
	}

	/**
	 * Returns a list of all broker IDs of brokers that serviced this client in the past
	 * 
	 * @return
	 */
	public String getBrokerHistory() {
		return brokerHistory.toString();
	}

	/**
	 * Upon stopping or exiting the simulator
	 * 
	 */
	public void terminate() {
		brokerHistory.clear();
	}

	protected static String getCurrentDateTime() {
		return new SimpleDateFormat("MMdd_HH.mm.ss").format(new Date());
	}

	public BrokerState connect(String addr) throws ClientException {
		BrokerState brokerState = super.connect(addr);
		
		// record the set of brokers visited
		if (brokerState != null) {
			if (brokerHistory == null) {
				brokerHistory = new LinkedList<String>();
			}
			brokerHistory.addLast(brokerState.getBrokerAddress().getNodeURI());
		}
		
		return brokerState;
	}

}

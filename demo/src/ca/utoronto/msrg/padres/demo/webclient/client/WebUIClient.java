package ca.utoronto.msrg.padres.demo.webclient.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.demo.webclient.services.PageService;

/*
 * Just run this class as a normal Java application to start the client.
 * 
 */
public class WebUIClient extends Client {

	/*
	 * Set these properties by supplying a -D<property_name>=<property_value> as a JVM argument.
	 * Although using property files is more correct, I find that the JVM arguments are quicker and
	 * easier to use from Eclipse, in scripts, etc.
	 * 
	 * TODO: Low priority: Create a property file for those who like property files TODO: Create
	 * custom command line arguments rather than usurp JVM arguments
	 */

	private static WebUIClient webClient = null;

	private static SimpleServer simpleServer = null;

	/*** Instance methods ***/
	private Map<String, EventQueue> eventQueues;

	private WebUIClient() throws ClientException {
		super(new WebClientConfig());
		eventQueues = new HashMap<String, EventQueue>();
	}

	/*
	 * Custom Demo clients should grab a WebUIClient instance using this method.
	 * 
	 * Use the "handle*" methods if the GUI should be aware of the operations being performed.
	 * Otherwise, access the "*" methods directly from the RMI client superclass.
	 */
	public static synchronized WebUIClient getClient() throws ClientException {
		if (webClient == null)
			return new WebUIClient();
		return webClient;
	}

	/***
	 * Proxy between the web service and client. The web UI will only be aware of events passing
	 * through these methods.
	 ***/
	public String handleSubscribe(String content, String bid) throws ClientException {
		String id;
		// verify syntax early
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			id = subscribe(MessageFactory.createSubscriptionFromString(content), bid).getMessageID();
		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new ClientException("Invalid Subscription Syntax");
		}
		storeEvent(new ClientEvent(ClientEvent.TYPE_SUBSCRIBE, id, content));
		return id;
	}

	public String handlePublish(String content, String bid) throws ClientException {
		// verify syntax early
		try {
			String id = String.valueOf(System.currentTimeMillis());
			storeEvent(new ClientEvent(ClientEvent.TYPE_PUBLISH, id, content));

			return publish(MessageFactory.createPublicationFromString(content), bid).getMessageID();
		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new ClientException("Invalid Publication Syntax");
		}
	}

	public String handlePublishWithPayload(String content, String bid,
			ConcurrentHashMap<? extends Object, ? extends Object> payload) throws ClientException {
		// verify syntax early
		try {
			// semi-colon to make parser happy
			String id = String.valueOf(System.currentTimeMillis());
			storeEvent(new ClientEvent(ClientEvent.TYPE_PUBLISH, id, content));
	
			Publication pub = MessageFactory.createPublicationFromString(content);
			pub.setPayload(payload);
			return publish(pub, bid).getMessageID();
		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new ClientException("Invalid Publication Syntax");
		}
	}

	public String handleAdvertise(String content, String bid) throws ClientException {
		String id;
		// verify syntax early
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			id = advertise(MessageFactory.createAdvertisementFromString(content), bid).getMessageID();
		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new ClientException("Invalid Advertisement Syntax");
		}

		storeEvent(new ClientEvent(ClientEvent.TYPE_ADVERTISE, id, content));

		return id;
	}

	public String handleUnsubscription(String unsubId) throws ClientException {
		try {
			unSubscribe(unsubId);
		} catch (ClientException e) {
			unSubscribeCS(unsubId);
		}

		storeEvent(new ClientEvent(ClientEvent.TYPE_UNSUBSCRIBE, unsubId, ""));

		return unsubId;
	}

	public String handleUnadvertise(String unadvId) throws ClientException {
		unAdvertise(unadvId);
		storeEvent(new ClientEvent(ClientEvent.TYPE_UNADVERTISE, unadvId, ""));
		return unadvId;
	}

	public NodeAddress handleConnect(String addr) throws ClientException {
		// The second argument was originally for use with the client names
		// patch, which allows clients to specify a "human readable" client
		// name on connection. This patch is not included in the main branch
		// but can be found in the CAWorldDemo2007 branch - wun@eecg
		// bid = connect(addr, getClientName());
		BrokerState brokerState = connect(addr);
		if (brokerState == null)
			throw new ClientException("Could not connect to broker at " + addr);

		storeEvent(new ClientEvent(ClientEvent.TYPE_CONNECT,
				brokerState.getBrokerAddress().getNodeURI(), addr));

		return brokerState.getBrokerAddress();
	}

	public String handleDisconnect(String bid) throws ClientException {
		BrokerState brokerState = disconnect(bid);

		storeEvent(new ClientEvent(ClientEvent.TYPE_DISCONNECT,
				brokerState.getBrokerAddress().getNodeURI(),
				brokerState.getBrokerAddress().getNodeURI()));

		return brokerState.getBrokerAddress().getNodeURI();
	}

	/*
	 * Note that unsubscription will handle removal of both composite and normal subscriptions
	 */
	public String handleSubscribeCS(String content, String bid) throws ClientException {
		// verify syntax early
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new ClientException("Invalid Composite Subscription Syntax");
		}

		String id = null;
		boolean success = false;
		while (!success) {
			try {
				id = subscribeCS(new CompositeSubscription(content), bid).getMessageID();
				success = true;
			} catch (NullPointerException ex) {
				// This can be caused by an extra set of root-level brackets
				// Strip them off and try again
				if (content.startsWith("{{") && content.endsWith("}}")) {
					content = content.substring(1, content.length() - 1);
				} else {
					throw ex;
				}
			}
		}

		// UI does not explicitly distinguish composite and normal subscriptions
		storeEvent(new ClientEvent(ClientEvent.TYPE_SUBSCRIBE, id, content));

		return id;
	}

	/*** Events ***/

	public void registerEventQueue(String qid) {
		if (!eventQueues.containsKey(qid))
			eventQueues.put(qid, new EventQueue());
	}

	public void removeEventQueue(String qid) {
		eventQueues.remove(qid);
	}

	public EventQueue getEventQueue(String qid) {
		return eventQueues.get(qid);
	}

	private void storeEvent(ClientEvent event) {
		Object qids[] = (Object[]) eventQueues.keySet().toArray();
		for (int ii = 0; ii < qids.length; ii++) {
			EventQueue q = eventQueues.get(qids[ii]);
			q.put(event);
		}
	}

	/*** Incoming notifications from RMI client superclass ***/

	protected void handlePublication(Publication pub) {
		String pubString = pub.toString();
		// strip off redundant timestamp ...
		int idx = pubString.lastIndexOf(";");
		if (idx > 0)
			pubString = pubString.substring(0, idx);
		storeEvent(new ClientEvent(ClientEvent.TYPE_NOTIFICATION, pub.getTimeStamp().toString(),
				pubString));
	}

	public static String getWebDir() {
		return ((WebClientConfig) webClient.getClientConfig()).getWebDir();
	}

	public static void main(String[] list) throws Exception {
		// Properties props = System.getProperties();
		//
		// HTTP_PORT = Integer.parseInt(props.getProperty(PROP_HTTP_PORT,
		// String.valueOf(HTTP_PORT)));
		// DEFAULT_CLIENT_NAME = props.getProperty(PROP_CLIENT_NAME, DEFAULT_CLIENT_NAME);
		// WEB_DIR = ClientConfig.PADRES_HOME + props.getProperty(PROP_WEB_DIR, WEB_DIR);
		// WEB_DIR = props.getProperty(PROP_WEB_DIR, WEB_DIR);
		// CLIENT_PROPERTIES_PATH = ClientConfig.PADRES_HOME
		// + props.getProperty(PROP_CLIENT_PROPERTIES_PATH, CLIENT_PROPERTIES_PATH);

		webClient = getClient();
		WebClientConfig clientConfig = (WebClientConfig) webClient.getClientConfig();
		simpleServer = new SimpleServer(clientConfig.getHttpPort());
		simpleServer.startServer();
		// set the index page
		PageService.setDefaultPage(clientConfig.getWebStartPage());

		System.out.println("WebUIClient started");

		/*
		 * Default startup options
		 */
		// String defaultBrokers = props.getProperty(PROP_DEFAULT_BROKERS, "");
		// if (defaultBrokers.length() > 0) {
		// String addrs[] = defaultBrokers.split(DEFAULT_BROKER_SEPARATOR);
		// for (String addr : addrs) {
		// if (addr.length() > 0) {
		// NodeAddress bid;
		// try {
		// if ((bid = webClient.handleConnect(addr)) != null) {
		// System.out.println("Connected to " + bid + " at " + addr);
		// }
		// } catch (ClientException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// }

	}
}

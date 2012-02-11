package ca.utoronto.msrg.padres.demo.webclient.demo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.demo.webclient.client.ClientEvent;
import ca.utoronto.msrg.padres.demo.webclient.client.WebClientException;
import ca.utoronto.msrg.padres.demo.webclient.client.EventQueue;
import ca.utoronto.msrg.padres.demo.webclient.client.PageWriter;
import ca.utoronto.msrg.padres.demo.webclient.client.WebUIClient;

/*
 * Provides all the Java code support for basic operations in the default demos
 */
public class BaseDemo {

	private static final String PROP_MSG_ID = "msg_id";

	private static final String PROP_MSG_CONTENT = "msg_content";

	private static final String PROP_MSG_TYPE = "msg_type";

	private static final String PROP_BROKER_ID = "broker_id";

	private static final String PROP_BROKER_ADDRESS = "broker_address";

	private static final String PROP_FILE_PATH = "file_path";

	private static final String PROP_FILE_CONTENTS = "file_contents";

	private static final String PROP_EVENT_QID = "event_qid";

	private static final String PROP_EVENT_TYPE = "event_type";

	private static final String PROP_EVENT_ID = "event_id";

	private static final String PROP_EVENT_CONTENT = "event_content";

	private static final String PROP_CLIENT_NAME = "client_name";

	private static final String PROP_REQ_TYPE = "req_type";

	// private static final String MSG_TYPE_PUB = "publication";

	private static final String MSG_TYPE_SUB = "subscription";

	private static final String MSG_TYPE_ADV = "advertisement";

	private static final String TYPE_BRK = "broker";

	private WebUIClient client;

	public BaseDemo() throws ClientException {
		client = WebUIClient.getClient();
	}

	public Properties publish(Properties props) throws WebClientException, ClientException {
		String publication = buildEvent(props.getProperty(PROP_MSG_CONTENT), null);
		if (publication == null)
			throw new WebClientException("Invalid publication syntax");

		// call into RMIClient to publish
		String bid = brokerAddressToId(props.getProperty(PROP_BROKER_ADDRESS));
		client.handlePublish(publication, bid);

		return props;
	}

	public Properties subscribe(Properties props) throws WebClientException, ClientException {
		String subscription = buildFilter(props.getProperty(PROP_MSG_CONTENT));
		if (subscription == null)
			throw new WebClientException("Invalid subscription syntax");

		// call into RMIClient to subscribe
		String bid = brokerAddressToId(props.getProperty(PROP_BROKER_ADDRESS));

		String subId;
		if (isCompositeSubscription(subscription))
			subId = client.handleSubscribeCS(subscription, bid);
		else
			subId = client.handleSubscribe(subscription, bid);

		props = new Properties();
		props.setProperty(PROP_MSG_ID, subId);
		props.setProperty(PROP_MSG_CONTENT, replaceSpecialXmlChars(subscription));
		props.setProperty(PROP_MSG_TYPE, MSG_TYPE_SUB);
		return props;
	}

	public Properties advertise(Properties props) throws WebClientException, ClientException {
		String advertisement = buildFilter(props.getProperty(PROP_MSG_CONTENT));
		if (advertisement == null)
			throw new WebClientException("Invalid advertisement syntax");

		// call into RMIClient to advertise
		String bid = brokerAddressToId(props.getProperty(PROP_BROKER_ADDRESS));
		String advId = client.handleAdvertise(advertisement, bid);
		props = new Properties();
		props.setProperty(PROP_MSG_ID, advId);
		props.setProperty(PROP_MSG_CONTENT, replaceSpecialXmlChars(advertisement));
		props.setProperty(PROP_MSG_TYPE, MSG_TYPE_ADV);

		return props;
	}

	public Properties waitForNextEvent(Properties props) {
		String qid = props.getProperty(PROP_EVENT_QID);
		// will be ignored if queue already exists
		client.registerEventQueue(qid);

		EventQueue queue = client.getEventQueue(qid);
		ClientEvent event = queue.blockingGet();

		props.setProperty(PROP_EVENT_TYPE, event.getType());
		props.setProperty(PROP_EVENT_ID, replaceSpecialXmlChars(event.getId()));
		props.setProperty(PROP_EVENT_CONTENT, replaceSpecialXmlChars(event.getContent()));
		return props;
	}

	public Properties unfilter(Properties props) throws WebClientException, ClientException {
		String type = props.getProperty(PROP_MSG_TYPE);

		// these Strings are expected from the xmlHttp.send() command
		if (type.equalsIgnoreCase(MSG_TYPE_ADV))
			return unadvertise(props);
		else if (type.equalsIgnoreCase(MSG_TYPE_SUB))
			return unsubscribe(props);

		return null;
	}

	public Properties filter(Properties props) throws WebClientException, ClientException {
		String type = props.getProperty(PROP_MSG_TYPE);

		if (type.equalsIgnoreCase(MSG_TYPE_ADV))
			return advertise(props);
		else if (type.equalsIgnoreCase(MSG_TYPE_SUB))
			return subscribe(props);

		return null;
	}

	private Properties unsubscribe(Properties props) throws WebClientException, ClientException {
		client.handleUnsubscription(props.getProperty(PROP_MSG_ID));
		return props;
	}

	private Properties unadvertise(Properties props) throws WebClientException, ClientException {
		client.handleUnadvertise(props.getProperty(PROP_MSG_ID));
		return props;
	}

	/*
	 * Automatically build an advertisement with 'isPresent' predicates for advertisement
	 */
	public Properties advertisePublish(Properties props) throws WebClientException, ClientException {
		String content = props.getProperty(PROP_MSG_CONTENT);
		String bid = brokerAddressToId(props.getProperty(PROP_BROKER_ADDRESS));

		String advertisement = buildEvent(content, "isPresent");
		String publication = buildEvent(content, null);
		if (advertisement == null || publication == null) {
			throw new WebClientException("Invalid Publication");
		}

		String advResponse = client.handleAdvertise(advertisement, bid);
		String pubResponse = client.handlePublish(publication, bid);
		if (advResponse == null || pubResponse == null) {
			throw new WebClientException("Error Publishing");
		}

		props = new Properties();
		props.put(PROP_MSG_ID, advResponse);
		props.put(PROP_MSG_CONTENT, replaceSpecialXmlChars(advertisement));
		return props;
	}

	public Properties getfilters(Properties props) throws ClientException {
		String type = props.getProperty(PROP_MSG_TYPE);

		if (type.equalsIgnoreCase(MSG_TYPE_ADV))
			return getAdvertisements();
		else if (type.equalsIgnoreCase(MSG_TYPE_SUB))
			return getSubscriptions();

		return null;
	}

	private Properties getSubscriptions() throws ClientException {
		Map<String, SubscriptionMessage> activeSubscriptions = client.getSubscriptions();
		Map<String, CompositeSubscriptionMessage> activeCompositeSubscriptions = client.getCompositeSubscriptions();
		Properties props = new Properties();
		for (String msgID : activeSubscriptions.keySet()) {
			Subscription sub = activeSubscriptions.get(msgID).getSubscription();
			props.put(msgID, replaceSpecialXmlChars(sub.toString()));
		}
		for (String msgID : activeCompositeSubscriptions.keySet()) {
			CompositeSubscription sub = activeCompositeSubscriptions.get(msgID).getSubscription();
			props.put(msgID, replaceSpecialXmlChars(sub.toString()));
		}
		props.setProperty(PROP_MSG_TYPE, MSG_TYPE_SUB);
		return props;
	}

	private Properties getAdvertisements() throws ClientException {
		Map<String, AdvertisementMessage> activeAdvertisements = client.getAdvertisements();
		Properties props = new Properties();
		for (String msgID : activeAdvertisements.keySet()) {
			Advertisement adv = activeAdvertisements.get(msgID).getAdvertisement();
			props.put(msgID, replaceSpecialXmlChars(adv.toString()));
		}
		props.setProperty(PROP_MSG_TYPE, MSG_TYPE_ADV);
		return props;
	}

	/*
	 * Note: This method is not for generic file loading the returned values are actually formatted
	 * as as bunch of <id>"text line"</id> tags.
	 * 
	 * To just read all the file contents with no special formatting, use loadRawFile().
	 */
	public Properties loadfile(Properties props) throws WebClientException {
		String filepath = props.getProperty(PROP_FILE_PATH);
		File file = openFile(filepath);

		String reqType = props.getProperty(PROP_REQ_TYPE);
		props = new Properties();
		props.setProperty(PROP_REQ_TYPE, reqType);

		/*
		 * TODO: protect against reading non-text files maybe place a special header in "readable"
		 * files
		 */
		// String fileContents = "";
		try {
			int cnt = 0;
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				String item = line;
				String id = "Item" + cnt++;

				int idx = line.indexOf(':');
				if (idx > 0) {
					id = line.substring(0, idx);
					item = line.substring(idx + 1).trim();
				}
				item = replaceSpecialXmlChars(item);
				props.setProperty(id, item);
				// fileContents += line + "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new WebClientException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			throw new WebClientException(e.getMessage());
		}

		// TODO: Remove unnecessary properties before returning to server
		return props;
	}

	/*
	 * Loads a file and returns its contents inside one big text tag
	 */
	public Properties loadRawFile(Properties props) throws WebClientException {
		ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
		String filepath = props.getProperty(PROP_FILE_PATH);

		PageWriter.sendPage(new PrintStream(fileContents), filepath);
		props.put(PROP_FILE_CONTENTS, replaceSpecialXmlChars(fileContents.toString()));

		return props;
	}

	public Properties connect(Properties props) throws WebClientException, ClientException {
		String address = props.getProperty(PROP_BROKER_ADDRESS);
		NodeAddress bid = client.handleConnect(address);

		props.setProperty(PROP_BROKER_ID, bid.getNodeURI());
		return props;
	}

	public Properties disconnect(Properties props) throws WebClientException, ClientException {
		String bid = brokerAddressToId(props.getProperty(PROP_BROKER_ADDRESS));
		client.handleDisconnect(bid);
		props.setProperty(PROP_BROKER_ID, bid);
		return props;
	}

	public Properties getBrokers(Properties props) {
		Map<String, String> brokers = client.getBrokerConnections();
		props = new Properties();
		for (String brokerID : brokers.keySet()) {
			props.setProperty(brokerID, brokers.get(brokerID));
		}
		props.setProperty(PROP_REQ_TYPE, TYPE_BRK);
		return props;
	}

	public Properties getClientName(Properties props) {
		props = new Properties();
		props.setProperty(PROP_CLIENT_NAME, client.getClientID());
		return props;
	}

	public Properties setClientName(Properties props) {
		// default is to keep the name unchanged if no new name is found
		String name = props.getProperty(PROP_CLIENT_NAME, client.getClientID());
		client.setClientID(name);
		return props;
	}

	/*** Helper methods **/
	private final String TOKENS_WHITESPACE_REGEX = "[(*)*\\s*]+";

	private String[] splitTrim(String str) {
		String[] split = str.split(TOKENS_WHITESPACE_REGEX);

		// split() can trim trailing empty Strings but not leading empty Strings
		// so skip leading empty String if it's there
		if (split.length > 0 && split[0].length() == 0) {
			String[] trim = new String[split.length - 1];
			System.arraycopy(split, 1, trim, 0, trim.length);
			return trim;
		}

		return split;
	}

	private String buildFilter(String content) {
		String[] tokens = splitTrim(content);

		// composite subscriptions get special treatment
		boolean isComposite = isCompositeSubscription(content);

		// we expect triples: attr,op,val with at most one leading empty String
		if (tokens.length % 3 != 0 && !isComposite) {
			return null;
		}

		String filter = "";
		try {
			String atom = ""; // only needed for building composite subscriptions because the
			// MessageParser
			// insists that the class predicate comes first
			int ii = 0;
			while (ii < tokens.length) {
				if (isComposite) {
					// find composite subscription characters
					while (ii < tokens.length
							&& (tokens[ii].startsWith("{") || tokens[ii].startsWith("}")
									|| tokens[ii].startsWith("&") || tokens[ii].startsWith("||"))) {

						if (!tokens[ii].startsWith("{")) {
							// complete an atom
							filter += atom + tokens[ii];
						} else {
							filter += tokens[ii];
						}

						// start a new atomic subscription
						atom = "";
						ii++;
					} // end while composite subscription token
				} // end if composite

				// don't expect another predicate if the composite subscription atom is complete
				if (ii < tokens.length || !tokens[ii - 1].endsWith("}")) {
					String attr = tokens[ii];
					String op = tokens[ii + 1];
					String val = tokens[ii + 2];

					String item = "[" + attr + "," + (attr.equalsIgnoreCase("class") ? "eq" : op)
							+ "," + val + "]";

					if (attr.equalsIgnoreCase("class")) {
						atom = item + (atom.length() > 0 ? "," + atom : "");
					} else {
						if (atom.length() > 0)
							atom += ",";
						atom += item;
					}

					ii += 3;
				}
			}

			// finish it off
			filter += atom;
		} catch (ArrayIndexOutOfBoundsException ex) {
			// Caller is expected to throw an appropriate exception
			dumpTokens(tokens);
			filter = null;
		}

		// finish off the root-level brackets
		if (isComposite)
			filter = "{" + filter + "}";

		return filter;
	}

	private String buildEvent(String content, String op) {
		String[] tokens = splitTrim(content);

		// we expect pairs
		if (tokens.length % 2 != 0)
			return null;

		String event = "";
		try {
			for (int ii = 0; ii < tokens.length; ii += 2) {
				String attr = tokens[ii];
				String val = tokens[ii + 1];

				String item = "[" + attr + ","
				// build event with supplied operator if it exists
				// useful for building an auto-advertisement by supplying 'isPresent'
				//
						+ ((op == null || op.length() == 0) ? "" : (attr.equalsIgnoreCase("class")
								? "eq," : op + ",")) + val + "]";

				if (attr.equalsIgnoreCase("class")) {
					event = item + (event.length() > 0 ? "," + event : "");
				} else {
					if (event.length() > 0)
						event += ",";
					event += item;
				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			// Caller is expected to throw an appropriate exception
			dumpTokens(tokens);
			event = null;
		}

		return event;
	}

	private String brokerAddressToId(String address) throws ClientException {
		BrokerState brokerState = client.getBrokerState(address);
		return brokerState.getBrokerAddress().getNodeURI();
	}

	private File openFile(String filepath) throws WebClientException {
		File file = new File(filepath);
		if (!file.exists()) {
			System.err.println(filepath + " not found!");
			System.err.println("Current dir: " + System.getProperty("user.dir"));
			throw new WebClientException(filepath + " not found.");
		}
		return file;
	}

	private void dumpTokens(String[] tokens) {
		System.err.println("Filter tokens: ");
		for (int ii = 0; ii < tokens.length; ii++)
			System.err.print(tokens[ii] + ",");
		System.err.println("END_TOKENS");
	}

	private boolean isCompositeSubscription(String sub) {
		return (sub.contains("&") || sub.contains("||"));
	}

	// TODO: this should go somewhere else
	public static String replaceSpecialXmlChars(String item) {
		item = item.replaceAll("&", "&amp;");
		item = item.replaceAll("<", "&lt;");
		item = item.replaceAll(">", "&gt;");
		return item;
	}
}

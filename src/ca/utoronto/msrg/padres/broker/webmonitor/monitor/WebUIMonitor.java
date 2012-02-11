package ca.utoronto.msrg.padres.broker.webmonitor.monitor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class WebUIMonitor {

	protected static final int HTTP_PORT = 9595;

	protected static final String DEFAULT_MONITOR_NAME = "Default Monitor Name";

	// private static WebUIMonitor monitor = null;

	protected static SimpleServer simpleServer = null;

	protected String monitorName = "";

	protected BrokerCore brokercore;

	static Logger managementInterfaceLogger = Logger.getLogger("ManagementInterface");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public WebUIMonitor(BrokerCore broker) {
		brokercore = broker;
	}

	/** * Instance methods ** */
	private Map<String, EventQueue> eventQueues;

	public void initialize() {
		String mipropfilePath = brokercore.getMIPropertiesFile();
		Properties prop = new Properties();

		int httpPort = 9090;
		try {
			prop.load(new FileInputStream(mipropfilePath));

			if (prop.getProperty("http.port", "").equals("")) {
				managementInterfaceLogger.warn("Missing port key or port value in the management interface property file.");
				exceptionLogger.warn(
						"Here is an exception : ",
						new Exception(
								"Missing port key or port value in the management interface property file."));
				prop.setProperty("http.port", String.valueOf(HTTP_PORT));
			}
			httpPort = Integer.parseInt(prop.getProperty("http.port"));

			if (prop.getProperty("monitor.name", "").equals("")) {
				managementInterfaceLogger.warn("Missing monitor name key or name value in the management interface property file.");
				exceptionLogger.warn(
						"Here is an exception : ",
						new Exception(
								"Missing monitor name key or name value in the management interface property file."));
				prop.setProperty("monitor.name", DEFAULT_MONITOR_NAME);
			}
			monitorName = prop.getProperty("monitor.name");

		} catch (FileNotFoundException ef) {
			managementInterfaceLogger.error("Fail to load management interface property file, FileNotFoundException: "
					+ ef);
			exceptionLogger.error("Fail to load management interface property file, FileNotFoundException: "
					+ ef);

		} catch (IOException ei) {
			managementInterfaceLogger.error("Fail to load management interface property file, IOException: "
					+ ei);
			exceptionLogger.error("Fail to load management interface property file, IOException: "
					+ ei);

		}

		// monitor = brokercore.getWebuiMonitor();

		eventQueues = new HashMap<String, EventQueue>();

		simpleServer = new SimpleServer(httpPort);
		simpleServer.startServer();

		managementInterfaceLogger.info("ManagementInterface is started");

	}

	public String handleAdvertise(String content) throws MonitorException {
		EventQueue webuiQ = getEventQueue("#default_webui");
		String id;
		AdvertisementMessage advMsg = null;
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			Advertisement adv = MessageFactory.createAdvertisementFromString(content);
			id = brokercore.getNewMessageID();
			advMsg = new AdvertisementMessage(adv, id, brokercore.getBrokerDestination());
			brokercore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new MonitorException("Invalid Advertisement Syntax");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e.getMessage());
		}

		if (webuiQ != null) {
			MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
					advMsg.toString());
			webuiQ.put(tmp);
		}

		return id;
	}

	public String handleSubscribe(String content) throws MonitorException {
		EventQueue webuiQ = getEventQueue("#default_webui");
		String id;
		SubscriptionMessage subMsg = null;
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			Subscription sub = MessageFactory.createSubscriptionFromString(content);
			id = brokercore.getNewMessageID();
			subMsg = new SubscriptionMessage(sub, id, brokercore.getBrokerDestination());
			brokercore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new MonitorException("Invalid Advertisement Syntax");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e.getMessage());
		}

		if (webuiQ != null) {
			MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
					subMsg.toString());
			webuiQ.put(tmp);
		}
		return id;
	}

	public String handleCompositeSubscribe(String content) throws MonitorException {
		EventQueue webuiQ = getEventQueue("#default_webui");
		String id;
		CompositeSubscriptionMessage csMsg = null;
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			CompositeSubscription cs = new CompositeSubscription(content);
			id = brokercore.getNewMessageID();
			csMsg = new CompositeSubscriptionMessage(cs, id, brokercore.getBrokerDestination());
			brokercore.routeMessage(csMsg, MessageDestination.INPUTQUEUE);

		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new MonitorException("Invalid Advertisement Syntax");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e.getMessage());
		}

		if (webuiQ != null) {
			MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
					csMsg.toString());
			webuiQ.put(tmp);
		}
		return id;
	}

	public String handlePublish(String content) throws MonitorException {
		EventQueue webuiQ = getEventQueue("#default_webui");
		String id;
		PublicationMessage pubMsg = null;
		try {
			// semi-colon to make parser happy
			new MessageParser(content + ";");
			Publication pub = MessageFactory.createPublicationFromString(content);
			id = brokercore.getNewMessageID();
			pubMsg = new PublicationMessage(pub, id, brokercore.getBrokerDestination());
			brokercore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new MonitorException("Invalid Advertisement Syntax");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e.getMessage());
		}

		if (webuiQ != null) {
			MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
					pubMsg.toString());
			webuiQ.put(tmp);
		}
		return id;
	}

	public String handlePublishWithPayload(String content, ConcurrentHashMap payload)
			throws MonitorException {
		EventQueue webuiQ = getEventQueue("#default_webui");
		String id;
		PublicationMessage pubMsg = null;
		try {
			// semi-colon to make parser happy
//			new MessageParser(content + ";");
			Publication pub = MessageFactory.createPublicationFromString(content);
			pub.setPayload(payload);
			id = brokercore.getNewMessageID();
			pubMsg = new PublicationMessage(pub, id, brokercore.getBrokerDestination());
			brokercore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		} catch (ParseException e) {
			System.out.println("!! Parser: " + content);
			e.printStackTrace();
			throw new MonitorException("Invalid Advertisement Syntax");
		} catch (Exception e) {
			e.printStackTrace();
			throw new MonitorException(e.getMessage());
		}

		if (webuiQ != null) {
			MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
					pubMsg.toString());
			webuiQ.put(tmp);
		}
		return id;
	}

	public void getInputQueue() {
		EventQueue webuiQ = getEventQueue("#default_webui");
		MessageQueue inputqueue = brokercore.getQueue(MessageDestination.INPUTQUEUE);

		// some fake advertisements for testing
		Advertisement adv = new Advertisement().addPredicate("class", new Predicate("eq" ,"stock"));
		for (int j = 0; j < 10; j++) {
			AdvertisementMessage advMsg = new AdvertisementMessage(adv,
					brokercore.getNewMessageID(), brokercore.getBrokerDestination());
			inputqueue.add(advMsg);
		}

		// try{Thread.sleep(1000);}catch(Exception e){}

		Object[] queueContent = inputqueue.toArray();
		for (int i = 0; i < queueContent.length; i++) {
			if (webuiQ != null) {
				MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, "" + i,
						queueContent[i].toString());
				webuiQ.put(tmp);
			}
		}

	}

	public void stopBroker() {
		brokercore.getSystemMonitor().stopBroker();
	}

	public void resuemBroker() {
		brokercore.getSystemMonitor().resumeBroker();
	}

	public void shutdownBroker() {
		brokercore.getSystemMonitor().shutdownBroker();
	}

	public String getMonitorName() {
		return monitorName;
	}

	public static WebUIMonitor getMonitor() {
		return null;
	}

	public String getBrokerID() {
		return brokercore.getBrokerID();
	}

	/** * Events ** */

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

	/**
	 * TODO: check whether this method is needed and remove it if not. <code>
	private void storeEvent(MonitorEvent event) {
		Object qids[] = (Object[]) eventQueues.keySet().toArray();
		for (int ii = 0; ii < qids.length; ii++) {
			EventQueue q = eventQueues.get(qids[ii]);
			q.put(event);
		}
	}
	</code>
	 */

}

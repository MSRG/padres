package ca.utoronto.msrg.padres.broker.webmonitor.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.controller.ServerInjectionManager;
import ca.utoronto.msrg.padres.broker.webmonitor.monitor.EventQueue;
import ca.utoronto.msrg.padres.broker.webmonitor.monitor.MonitorEvent;
import ca.utoronto.msrg.padres.broker.webmonitor.monitor.MonitorException;
import ca.utoronto.msrg.padres.broker.webmonitor.monitor.WebUIMonitor;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class BaseDemo {

	private static final String PROP_MONITOR_NAME = "monitor_name";

	public static final String PROP_EVENT_QID = "event_qid";

	public static final String PROP_EVENT_TYPE = "event_type";

	public static final String PROP_EVENT_ID = "event_id";

	public static final String PROP_EVENT_CONTENT = "event_content";

	private static final int NUM_OF_LOG_FILE = 10;

	private WebUIMonitor monitor;

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	private static String LOG_FILE_PATH = System.getProperty("user.dir");

	public BaseDemo(BrokerCore broker) {
		monitor = broker.getWebuiMonitor();
	}

	public Properties getMonitorName(Properties props) {
		props = new Properties();
		props.setProperty(PROP_MONITOR_NAME, monitor.getMonitorName());
		return props;
	}

	public Properties sendMsg(Properties props) throws MonitorException {

		String msgType = props.getProperty("msgtype");
		String msgContent = props.getProperty("msgcontent");

		if (msgType.equals("adv")) {
			monitor.handleAdvertise(msgContent);
		} else if (msgType.equals("sub")) {
			monitor.handleSubscribe(msgContent);
		} else if (msgType.equals("compositeSub")) {
			monitor.handleCompositeSubscribe(msgContent);
		} else if (msgType.equals("pub")) {
			monitor.handlePublish(msgContent);
		}

		return props;

	}

	public Properties retractPub(Properties props) throws MonitorException {
		String classname = props.getProperty("classname");
		String bid = monitor.getBrokerID();
		String adv = "[class,eq,BROKER_CONTROL],[brokerID,isPresent,'Text']," + "["
				+ ServerInjectionManager.INJECTION_ID_TAG + ",isPresent,'DUMMY_INJECTION_ID'],"
				+ "[command,isPresent," + ServerInjectionManager.CMD_PUB_MSG + "]";

		monitor.handleAdvertise(adv);
		String pub = "[class,'BROKER_CONTROL'],[brokerID,'" + bid
				+ "'],[command,'INJECTION-FLUSH_PUB'],[INJECTION_ID,'']";
		ConcurrentHashMap<String, Publication> payload = new ConcurrentHashMap<String, Publication>();

		Publication payloadPub;
		try {
			payloadPub = MessageFactory.createPublicationFromString("[class," + classname + "]");
		} catch (ParseException e) {
			throw new MonitorException(e);
		}
		payload.put(ServerInjectionManager.MESSAGE_PAYLOAD_TAG, payloadPub);

		monitor.handlePublishWithPayload(pub, payload);

		return new Properties();
	}

	public Properties stopBroker(Properties props) {
		monitor.stopBroker();
		return props;
	}

	public Properties resumeBroker(Properties props) {
		monitor.resuemBroker();
		return props;
	}

	public Properties shudownBroker(Properties props) {
		monitor.shutdownBroker();
		return props;
	}

	public Properties getQueue(Properties props) {
		String queueName = props.getProperty("queue");
		if (queueName.equals("inputQueue")) {
			monitor.getInputQueue();
		}
		return props;

	}

	public Properties getLogFile(Properties props) throws MonitorException {
		String loadFile = props.getProperty("logfile");
		String pa = props.getProperty("pattern");
		String pattern[] = pa.split(" ");

		String logFileList[] = new String[NUM_OF_LOG_FILE];
		int j = 0;
		File file = new File(LOG_FILE_PATH);
		File list[] = file.listFiles();
		if (loadFile.equals("messagePath")) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].isFile() && list[i].toString().contains("MessagePath")
						&& list[i].toString().contains(monitor.getBrokerID())) {
					logFileList[j++] = list[i].toString();
				}
			}
		} else if (loadFile.equals("systemError")) {
			logFileList[j] = LOG_FILE_PATH + FILE_SEPARATOR + "systemErr-" + monitor.getBrokerID()
					+ ".log";
		} else if (loadFile.equals("exception")) {
			for (int i = 0; i < list.length; i++) {
				if (list[i].isFile() && list[i].toString().contains("Exception")
						&& list[i].toString().contains(monitor.getBrokerID())) {
					logFileList[j++] = list[i].toString();
				}
			}
		}
		Grep grep = new Grep(pattern, logFileList);
		grep.run();

		return new Properties();
	}

	private class Grep {

		String[] pattern; // the strings to be searched for

		String[] args; // args[0..] = files to be searched

		int cnt;

		String id = null;

		// Constructor
		public Grep(String[] pa, String[] args) {
			this.pattern = pa;
			this.args = args;
			cnt = 0;

		}

		// Prints every line containing strings pattern in files f1,...,fn in turn.
		// prefixes every output line of a file with the file name.
		public void run() throws MonitorException {

			for (int i = 0; i < args.length; i++) {
				if (args[i] != null) {
					// grep i'th argument for string pattern
					try {
						grep(pattern, new BufferedReader(new FileReader(args[i])), args[i] + ": ");
					} catch (FileNotFoundException e) {
						System.err.println(args[i] + " not found!");
						System.err.println("Current dir: " + System.getProperty("user.dir"));
						throw new MonitorException(args[i] + " not found.");
					}
				}
			}
		}

		// For every line in file containing string pattern, prints prefix and line
		private void grep(String[] pattern, BufferedReader file, String prefix)
				throws MonitorException {
			EventQueue webuiQ = monitor.getEventQueue("#default_webui");
			try {
				String line = file.readLine();
				while (line != null) {
					boolean flag = true;
					for (int j = 0; j < pattern.length && flag; j++) {
						if (pattern[j].equals("")) {
							flag = true;
						} else if (line.indexOf(pattern[j]) < 0) {
							flag = false;
						}
					}
					if (flag) {
						id = "" + cnt++;
						String item = prefix + line;
						item = replaceSpecialXmlChars(item);

						if (webuiQ != null) {
							MonitorEvent tmp = new MonitorEvent(MonitorEvent.TYPE_NOTIFICATION, id,
									item);
							webuiQ.put(tmp);
						}

					}
					line = file.readLine();
				}
			} catch (IOException e) {
				throw new MonitorException(e.getMessage());
			}
		}
	}

	public Properties waitForNextEvent(Properties props) {
		String qid = props.getProperty(PROP_EVENT_QID);
		// will be ignored if queue already exists
		monitor.registerEventQueue(qid);

		EventQueue queue = monitor.getEventQueue(qid);
		MonitorEvent event = queue.blockingGet();

		props.setProperty(PROP_EVENT_TYPE, event.getType());
		props.setProperty(PROP_EVENT_ID, replaceSpecialXmlChars(event.getId()));
		props.setProperty(PROP_EVENT_CONTENT, replaceSpecialXmlChars(event.getContent()));

		return props;
	}

	// TODO: this should go somewhere else
	public static String replaceSpecialXmlChars(String item) {
		item = item.replaceAll("&", "&amp;");
		item = item.replaceAll("<", "&lt;");
		item = item.replaceAll(">", "&gt;");
		return item;
	}

}

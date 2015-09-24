package ca.utoronto.msrg.padres.broker.management;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jess.JessException;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.matching.jess.JessMatcher;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

public class CommandHandler {
	public static final String CMD_SEP = "\\s+";

	private BrokerCore brokerCore;

	// Maintains a mapping of command keywords to method names.
	private Map<String, String> commandMethodMap;

	private Map<String, String> commandDescriptionMap;

	private final String PROP_ERROR = "error";

	private static final String ARG_NOHEADER = "nohead";

	private static final String ARG_CLASSONLY = "classonly";

	public CommandHandler(BrokerCore brokerCore) {
		this.brokerCore = brokerCore;
		commandMethodMap = new HashMap<String, String>();
		commandDescriptionMap = new HashMap<String, String>();
		initializeCommandMap();
	}

	/**
	 * Recognized command must appear in these mappings
	 */
	private void initializeCommandMap() {
		commandMethodMap.put("cls", "dummyMethod");
		commandDescriptionMap.put("cls", "Clear console text.");

		commandMethodMap.put("help", "printHelp");
		commandDescriptionMap.put("help", "Print a help screen. Usage: help [command]");

		commandMethodMap.put("printsubs", "printSubscriptions");
		commandDescriptionMap.put("printsubs",
				"Show subscriptions on broker. Usage: printsubs [nohead] [classonly]");

		commandMethodMap.put("printadvs", "printAdvertisements");
		commandDescriptionMap.put("printadvs",
				"Show advertisements on broker. Usage: printadvs [nohead] [classonly]");

		commandMethodMap.put("info", "printInfo");
		commandDescriptionMap.put("info", "Show broker info");

		commandMethodMap.put("sub", "injectSubscription");
		commandDescriptionMap.put("sub", "Inject a subscription. Usage: sub <subscription string>");

		commandMethodMap.put("pub", "injectPublication");
		commandDescriptionMap.put("pub", "Inject a publication. Usage: pub <publication string>");

		commandMethodMap.put("adv", "injectAdvertisement");
		commandDescriptionMap.put("adv",
				"Inject an advertisement. Usage: adv <advertisement string>");

		commandMethodMap.put("unsub", "injectUnsubscription");
		commandDescriptionMap.put("unsub",
				"Inject an unsubscription. Usage: unsub <subscription ID1> [subscription ID2] ... ");

		commandMethodMap.put("unadv", "injectUnadvertisement");
		commandDescriptionMap.put("unadv",
				"Inject an unadvertisement. Usage: unadv <advertisement ID1> [advertisement ID2] ... ");

		commandMethodMap.put("batch", "batchProcess");
		commandDescriptionMap.put("batch",
				"Process a batch of commands from a simple text file. Usage: batch <filename>");

		/*** Jess specific commands ***/
		commandMethodMap.put("srt", "srtCommand");
		commandDescriptionMap.put("srt", "Run Jess command on Jess-based SRT. Usage: srt <command>");

		commandMethodMap.put("prt", "prtCommand");
		commandDescriptionMap.put("prt", "Run Jess command on Jess-based PRT. Usage: prt <command>");

		commandMethodMap.put("flush", "flushPRT");
		commandDescriptionMap.put("flush",
				"Flush facts from Jess-based PRT. Usage: flush <class1> [class2] ...");
	}

	public Properties handleCommand(String cmd) {
		if (cmd == null || cmd.length() == 0)
			return null;

		String[] _args = cmd.split(CMD_SEP);
		String command = _args[0];
		// Strip off the command keyword itself
		String[] args = Arrays.copyOfRange(_args, 1, _args.length);

		return runCommand(command, args);
	}

	public Properties runCommand(String command, String[] args) {
		Properties respProps = new Properties();

		// Methods are expected to accept a String[] as input and return a Properties object
		String className = this.getClass().getName();
		String methodName = commandMethodMap.get(command);
		if (methodName == null) {
			respProps.put(PROP_ERROR, "Unrecognized command: " + command);
			return respProps;
		}
		Class<?>[] methodArgs = { (new String[0]).getClass() };
		Object[] invokeArgs = { args };

		try {
			respProps = (Properties) Class.forName(className).getMethod(methodName, methodArgs).invoke(
					this, invokeArgs);
		} catch (ClassNotFoundException e) {
			respProps.put(PROP_ERROR, className + "class not found");
		} catch (NoSuchMethodException e) {
			respProps.put(PROP_ERROR, e.getClass().toString()
					+ (e.getMessage() == null ? "" : ": " + e.getMessage()));
		} catch (NullPointerException e) {
			respProps.put(PROP_ERROR, "Unrecognized command");
		} catch (Exception e) {
			// Not sure why it won't print the stack trace unless I explicitly give it System.out
			e.printStackTrace(System.out);
			// Return the true cause of the exception to the web interface
			respProps.put(e.getCause().getClass().getName(), e.getCause().getMessage());
		}

		return respProps;
	}

	/*** COMMANDS ***/

	// Methods are MUST accept a String[] argument and return a Properties object
	public Properties dummyMethod(String[] args) {
		return new Properties();
	}

	public Properties printHelp(String[] args) {
		Properties respProp = new Properties();
		if (args == null || args.length == 0) {
			args = commandMethodMap.keySet().toArray(new String[0]);
		}
		for (String cmd : args) {
			if (commandDescriptionMap.containsKey(cmd)) {
				respProp.put(cmd, commandDescriptionMap.get(cmd));
			} else {
				respProp.put(PROP_ERROR, "there is no command: " + cmd);
			}
		}
		return respProp;
	}

	public Properties printSubscriptions(String[] args) {
		Properties props = new Properties();

		boolean noHeader = false;
		boolean classOnly = false;
		if (args != null)
			for (String arg : args) {
				if (arg.equals(ARG_NOHEADER)) {
					noHeader = true;
				} else if (arg.equals(ARG_CLASSONLY)) {
					classOnly = true;
				}
			}

		for (SubscriptionMessage subMsg : brokerCore.getSubscriptions().values()) {
			String sub = "";
			if (!noHeader) {
				for (String hdr : subMsg.getAllHeaderFields().keySet()) {
					sub += "(" + hdr + "," + subMsg.getAllHeaderFields().get(hdr) + ")";
				}
				sub += ":";
			}

			if (classOnly) {
				Predicate pred = ((Predicate) subMsg.getSubscription().getPredicateMap().get(
						"class"));
				sub += "[class,eq," + pred.getValue() + "]";
			} else {
				sub += subMsg.getSubscription().toString();
			}
			props.put(subMsg.getMessageID(), sub);
		}
		return props;
	}

	public Properties printAdvertisements(String[] args) {
		Properties props = new Properties();

		boolean noHeader = false;
		boolean classOnly = false;
		if (args != null)
			for (String arg : args) {
				if (arg.equals(ARG_NOHEADER)) {
					noHeader = true;
				} else if (arg.equals(ARG_CLASSONLY)) {
					classOnly = true;
				}
			}

		for (Object obj : brokerCore.getAdvertisements().values()) {
			if (obj instanceof AdvertisementMessage) {
				AdvertisementMessage msg = (AdvertisementMessage) obj;
				String adv = "";

				if (!noHeader) {
					for (Object hdr : msg.getAllHeaderFields().keySet()) {
						adv += "(" + hdr + "," + msg.getAllHeaderFields().get(hdr) + ")";
					}
					adv += ":";
				}

				if (classOnly) {
					Predicate pred = ((Predicate) msg.getAdvertisement().getPredicateMap().get(
							"class"));
					adv += "[class,eq," + pred.getValue() + "]";
				} else {
					adv += msg.getAdvertisement().toString();
				}
				props.put(msg.getMessageID(), adv);
			}
		}
		return props;
	}

	public Properties printInfo(String[] args) {
		Properties props = new Properties();
		props.put("BrokerID", brokerCore.getBrokerID());
		props.put("URI", brokerCore.getBrokerURI());
		props.put("JVM", System.getProperty("java.vm.version"));
		props.put("OS", System.getProperty("os.name") + "-" + System.getProperty("os.arch") + "-"
				+ System.getProperty("os.version"));
		return props;
	}

	public Properties injectSubscription(String[] args) throws ParseException {
		Properties props = new Properties();
		if (args != null && args.length > 0) {
			// to check that the input is correctly formated
			// the following invocation throws ParseException if the format is wrong
			new MessageParser(args[0] + ";");

			Subscription sub = MessageFactory.createSubscriptionFromString(args[0]);
			String msgID = brokerCore.getNewMessageID();
			SubscriptionMessage msg = new SubscriptionMessage(sub, msgID);
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			props.put("SubID", msgID);
		}
		return props;
	}

	public Properties injectAdvertisement(String[] args) throws ParseException {
		Properties props = new Properties();
		if (args != null && args.length > 0) {
			new MessageParser(args[0] + ";");

			Advertisement adv = MessageFactory.createAdvertisementFromString(args[0]);
			String msgID = brokerCore.getNewMessageID();
			AdvertisementMessage msg = new AdvertisementMessage(adv, msgID);
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			props.put("AdvID", msgID);
		}
		return props;
	}

	public Properties injectPublication(String[] args) throws ParseException {
		Properties props = new Properties();
		if (args != null && args.length > 0) {
			new MessageParser(args[0] + ";");

			Publication pub = MessageFactory.createPublicationFromString(args[0]);
			String msgID = brokerCore.getNewMessageID();
			PublicationMessage msg = new PublicationMessage(pub, msgID);
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			props.put("PubID", msgID);
		}
		return props;
	}

	public Properties injectUnsubscription(String[] args) {
		Properties props = new Properties();
		if (args == null)
			return props;

		for (String unsubID : args) {
			UnsubscriptionMessage msg = new UnsubscriptionMessage(new Unsubscription(unsubID),
					brokerCore.getNewMessageID());
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
		}

		return props;
	}

	public Properties injectUnadvertisement(String[] args) {
		Properties props = new Properties();
		if (args == null)
			return props;

		for (String unadvID : args) {
			UnadvertisementMessage msg = new UnadvertisementMessage(new Unadvertisement(unadvID),
					brokerCore.getNewMessageID());
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
		}

		return props;
	}

	public Properties batchProcess(String[] args) {
		Properties props = new Properties();
		if (args == null || args.length == 0) {
			props.put(PROP_ERROR, "batch command requires an argument");
		} else {
			try {
				BufferedReader in = new BufferedReader(new FileReader(args[0]));
				int lineNo = 0;
				String inLine = in.readLine();
				while (inLine != null) {
					Properties resProp = handleCommand(inLine);
					for (String key : resProp.stringPropertyNames()) {
						String newKey = String.format("#%03d-%s", lineNo, key);
						props.put(newKey, resProp.getProperty(key));
					}
					inLine = in.readLine();
					lineNo++;
				}
			} catch (FileNotFoundException e) {
				props.put(PROP_ERROR, "Batch file not found");
			} catch (IOException e) {
				props.put(PROP_ERROR, "Error in reading batch file");
			}
		}

		return props;
	}

	public Properties srtCommand(String[] args) {
		Properties props = new Properties();

		if (!(brokerCore.getRouter().getMatcher() instanceof JessMatcher)) {
			props.put(PROP_ERROR, "Matching engine is not implemented using Jess.");
		} else if (args == null) {
			return props;
		} else {
			String jessCommand = "";
			for (String cmdPart : args) {
				jessCommand += cmdPart + " ";
			}
			try {
				String jessResult = ((JessMatcher) brokerCore.getRouter().getMatcher()).runSRTJessCommand(jessCommand);
				props.put("SRTJessResults", jessResult);
			} catch (JessException e) {
				props.put(PROP_ERROR, e.getMessage());
			}
		}

		return props;
	}

	public Properties prtCommand(String[] args) {
		Properties props = new Properties();

		if (!(brokerCore.getRouter().getMatcher() instanceof JessMatcher)) {
			props.put(PROP_ERROR, "Matching engine is not implemented using Jess.");
		} else if (args == null) {
			return props;
		} else {
			String jessCommand = "";
			for (String cmdPart : args) {
				jessCommand += cmdPart + " ";
			}
			try {
				String jessResult = ((JessMatcher) brokerCore.getRouter().getMatcher()).runPRTJessCommand(jessCommand);
				props.put("PRTJessResults", jessResult);
			} catch (JessException e) {
				props.put(PROP_ERROR, e.getMessage());
			}
		}

		return props;
	}

	public Properties flushPRT(String[] args) {
		Properties props = new Properties();
		if (!(brokerCore.getRouter().getMatcher() instanceof JessMatcher)) {
			props.put(PROP_ERROR, "Matching engine is not implemented using Jess.");
		} else if (args == null) {
			props.put(PROP_ERROR, "Must specify classnames of publications to flush");
		} else {
			/*
			 * FIXME: Can we do this atomically? i.e. If an exception occurs after flushing a few of
			 * the messages.
			 */
			for (String classname : args) {
				// Currently, flushPRTByClassName does not throw any exceptions but it should if we
				// want immediate feedback about flushing problems in the web interface.
				brokerCore.getRouter().getMatcher().flushPRTByClassName(classname);
			}
		}

		return props;
	}
}

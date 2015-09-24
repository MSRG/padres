/*
 * Created on Jun 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.controller;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

public class ServerInjectionManager implements Manager {

	/* Name of this manager (server injection manager) */
	public static final String MANAGER_NAME = "INJECTION";

	/* Key for retriving the injection id from the incoming payload */
	public static final String INJECTION_ID_TAG = "INJECTION_ID";

	/* Key for retriving the message attached from the incoming payload */
	public static final String MESSAGE_PAYLOAD_TAG = "MESSAGE_PAYLOAD";

	/* Command for publication */
	public static final String CMD_PUB_MSG = "INJECT_PUB";

	/* Command for flushing publication */
	public static final String CMD_FLUSH_PUB_MSG = "FLUSH_PUB";

	/* Command for subscription */
	public static final String CMD_SUB_MSG = "INJECT_SUB";

	/* Command for advertisment */
	public static final String CMD_ADV_MSG = "INJECT_ADV";

	/* Command for unadvertisment */
	public static final String CMD_UNADV_MSG = "INJECT_UNADV";

	/* Command for unsubscription */
	public static final String CMD_UNSUB_MSG = "INJECT_UNSUB";

	private BrokerCore brokerCore;

	/*
	 * The hash map for linking injection id to message id. It is used when performing unadvertise
	 * and unsubscribe
	 */
	private HashMap<String, String> injIdToMsgId;

	static Logger serverInjectionLogger = Logger.getLogger(ServerInjectionManager.class);

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Constructor.
	 */
	public ServerInjectionManager(BrokerCore broker) {
		brokerCore = broker;
		injIdToMsgId = new HashMap<String, String>();
	}

	public void handleCommand(Map<String, Serializable> pairs, Serializable payload) {

		String command = (String) pairs.get("command");
		command = command.substring(command.indexOf('-') + 1);
		serverInjectionLogger.debug("serverInjectionManager receives command : " + command + ".");
		Message msg = null;
		ConcurrentHashMap map = (ConcurrentHashMap) payload;
		// HashMap map = (HashMap)payload;
		String injectionId = (String) pairs.get(INJECTION_ID_TAG);
		if (command.equalsIgnoreCase(CMD_PUB_MSG)) {
			String msgId = brokerCore.getNewMessageID();
			Publication pub = (Publication) map.get(MESSAGE_PAYLOAD_TAG);
			msg = new PublicationMessage(pub, msgId);
			serverInjectionLogger.debug("ServerInjection manager is sending publication : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			messagePathLogger.debug("ServerInjection manager is sending publication : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
		} else if (command.equalsIgnoreCase(CMD_FLUSH_PUB_MSG)) {
			Publication pub = (Publication) map.get(MESSAGE_PAYLOAD_TAG);
			String classname = pub.getPairMap().get("class").toString();
			brokerCore.getRouter().getMatcher().flushPRTByClassName(classname);
		} else if (command.equalsIgnoreCase(CMD_SUB_MSG)) {
			String msgId = brokerCore.getNewMessageID();
			Subscription sub = (Subscription) map.get(MESSAGE_PAYLOAD_TAG);
			msg = new SubscriptionMessage(sub, msgId);
			serverInjectionLogger.debug("ServerInjection manager is sending subscription : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			messagePathLogger.debug("ServerInjection manager is sending subscription : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			injIdToMsgId.put(injectionId, msgId);
		} else if (command.equalsIgnoreCase(CMD_ADV_MSG)) {
			String msgId = brokerCore.getNewMessageID();
			Advertisement adv = (Advertisement) map.get(MESSAGE_PAYLOAD_TAG);
			msg = new AdvertisementMessage(adv, msgId);
			serverInjectionLogger.debug("ServerInjection manager is sending advertisement : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			messagePathLogger.debug("ServerInjection manager is sending advertisement : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			injIdToMsgId.put(injectionId, msgId);
		} else if (command.equalsIgnoreCase(CMD_UNADV_MSG)) {
			String msgId = brokerCore.getNewMessageID();
			String unadvMsgId = (String) injIdToMsgId.get(injectionId);
			Unadvertisement unadv = new Unadvertisement(unadvMsgId);
			msg = new UnadvertisementMessage(unadv, msgId);
			serverInjectionLogger.debug("ServerInjection manager is sending unadvertisement : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			messagePathLogger.debug("ServerInjection manager is sending unadvertisement : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			injIdToMsgId.remove(unadvMsgId);
		} else if (command.equalsIgnoreCase(CMD_UNSUB_MSG)) {
			String msgId = brokerCore.getNewMessageID();
			String unsubMsgId = (String) injIdToMsgId.get(injectionId);
			Unsubscription unsub = new Unsubscription(unsubMsgId);
			msg = new UnsubscriptionMessage(unsub, msgId);
			serverInjectionLogger.debug("ServerInjection manager is sending unsubscription : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			messagePathLogger.debug("ServerInjection manager is sending unsubscription : "
					+ msg.toString() + " to broker" + brokerCore.getBrokerID() + ".");
			brokerCore.routeMessage(msg, MessageDestination.INPUTQUEUE);
			injIdToMsgId.remove(unsubMsgId);
		} else {
			// add by shuang, for handling unrecognized command for
			// serverInjection manager
			serverInjectionLogger.warn("The command " + command
					+ " is not a valid command for serverInjection manager.");
			exceptionLogger.warn(new Exception("The command " + command
					+ " is not a valid command for serverInjection manager."));
		}

	}

	@Override
	public void shutdown() {
		// ignore		
	}

}

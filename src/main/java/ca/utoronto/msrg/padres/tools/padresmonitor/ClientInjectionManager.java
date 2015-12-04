package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import ca.utoronto.msrg.padres.broker.controller.ServerInjectionManager;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.InjectMessageDialog;

/**
 * @author Gerald Chan
 * 
 */
public class ClientInjectionManager {

	/*
	 * The set of injected message The key is the broker id The associated obj a InjectMessageStore
	 * object
	 */
	private HashMap<String, Vector<InjectMessageStore>> msgStoreMap;

	private MonitorFrame monitorFrame;

	private int m_numInjection;

	public ClientInjectionManager(MonitorFrame monitor) {
		this.monitorFrame = monitor;
		msgStoreMap = new HashMap<String, Vector<InjectMessageStore>>();
		m_numInjection = 0;
	}

	/**
	 * Inject message into the target broker
	 * 
	 * @param msgType
	 *            The type of the message being injected. Can be adv, sub or pub
	 * @param msgBody
	 *            The text of the message
	 * @param brokerID
	 *            The id of the target of the message being injected
	 * @return true if injection is successful
	 * @throws ParseException 
	 */
	public boolean injectMessage(int msgType, String msgBody, String brokerID, MonitorClient con) throws ParseException {
		boolean result = true;
		String command = "";
		Object payloadObject = null;
		switch (msgType) {
		case InjectMessageDialog.INJ_TYPE_ADV:
			payloadObject = MessageFactory.createAdvertisementFromString(msgBody);
			command = ServerInjectionManager.CMD_ADV_MSG;
			break;
		case InjectMessageDialog.INJ_TYPE_PUB:
			payloadObject = MessageFactory.createPublicationFromString(msgBody);
			command = ServerInjectionManager.CMD_PUB_MSG;
			break;
		case InjectMessageDialog.INJ_TYPE_SUB:
			payloadObject = MessageFactory.createSubscriptionFromString(msgBody);
			command = ServerInjectionManager.CMD_SUB_MSG;
			break;
		default:
			monitorFrame.setErrorString("No Message Type Selected");
			result = false;
			break;
		}

		if (result) {
			// setup the payload
			ConcurrentHashMap<String, Object> payload = new ConcurrentHashMap<String, Object>();
			payload.put(ServerInjectionManager.MESSAGE_PAYLOAD_TAG, payloadObject);
			// build the message to send
			String InjectionID = getInjectionID(con.getDefaultBrokerAddress().getNodeURI());
			String msgStr = "[class,BROKER_CONTROL]," + "[brokerID,'" + brokerID + "']," + "["
					+ ServerInjectionManager.INJECTION_ID_TAG + ",'" + InjectionID + "'],"
					+ "[command,'" + ServerInjectionManager.MANAGER_NAME + "-" + command + "']";
			Publication msg = MessageFactory.createPublicationFromString(msgStr);
			msg.setPayload(payload);
			// send the message
			try {
				con.publish(msg);
				if (msgStoreMap.containsKey(brokerID)) {
					/* There were previous inject message */
					Vector<InjectMessageStore> msgList = msgStoreMap.get(brokerID);
					msgList.add(new InjectMessageStore(msgBody, msgType, InjectionID));
				} else {
					/* No previous inject message, create a store now */
					Vector<InjectMessageStore> newMsgList = new Vector<InjectMessageStore>();
					InjectMessageStore msgStore = new InjectMessageStore(msgBody, msgType,
							InjectionID);
					newMsgList.add(msgStore);
					msgStoreMap.put(brokerID, newMsgList);
				}
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
				result = false;
			}
		}
		return result;
	}

	/**
	 * It uninject a message from a monitor
	 * 
	 * @param cell
	 *            The inject message store that contain all the injected message
	 * @param brokerID
	 *            the broker id of the target broker
	 * @param con
	 *            connection to the federation
	 * @return true if success
	 * @throws ParseException 
	 */
	public boolean uninjectMessage(InjectMessageStore cell, String brokerID, MonitorClient con) throws ParseException {
		if (cell == null) {
			monitorFrame.setErrorString("No injected message to uninject");
			return false;
		}

		boolean result = true;
		String command = "";
		switch (cell.getType()) {
		case InjectMessageDialog.INJ_TYPE_ADV:
			command = ServerInjectionManager.CMD_UNADV_MSG;
			break;
		case InjectMessageDialog.INJ_TYPE_SUB:
			command = ServerInjectionManager.CMD_UNSUB_MSG;
			break;
		default:
			result = false;
			break;
		}

		if (result) {
			String InjectionID = cell.getInjectID();
			String msgStr = "[class,BROKER_CONTROL]," + "[brokerID,'" + brokerID + "']" + "["
					+ ServerInjectionManager.INJECTION_ID_TAG + "'" + InjectionID + "']"
					+ "[command,'" + ServerInjectionManager.MANAGER_NAME + "-" + command + "']";
			Publication msg = MessageFactory.createPublicationFromString(msgStr);
			try {
				con.publish(msg);
				Vector<InjectMessageStore> msgList = msgStoreMap.get(brokerID);
				msgList.remove(msgList.indexOf(new InjectMessageStore(cell.getMsg(),
						cell.getType(), InjectionID)));
				if (msgList.size() == 0) {
					msgStoreMap.remove(brokerID);
				}
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
				result = false;
			}
		}
		return result;
	}

	/**
	 * It will reutrn the message store base on the broker id
	 * 
	 * @param brokerID
	 *            the traget broker id
	 * @return the vector that contain all the inject message cell of the broker
	 */
	public Vector<InjectMessageStore> getMsgStore(String brokerID) {
		Vector<InjectMessageStore> result = null;
		if (msgStoreMap.containsKey(brokerID)) {
			/* There were previous inject message */
			result = msgStoreMap.get(brokerID);
		}
		return result;

	}

	private String getInjectionID(String id) {
		String result;
		result = id + "-Inject-" + (new Integer(m_numInjection)).toString();
		m_numInjection++;
		return result;
	}
}

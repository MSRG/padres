package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.util.Map;

import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.MonitorDialog;

public class ClientMonitorCommandManager {

	public static final int TYPE_ADV = 0;

	public static final int TYPE_SUB = 1;

	private MonitorFrame monitorFrame;

	/** Used to create command id */
	private int commandSessionCount;

	private MonitorDialog monitorDialog;

	private boolean hasAdvCommand;

	/*
	 * ID of the subscription to the message that will deliver the set of adv or sub to the client
	 */
	private String subID;

	private MonitorClient monitorClient;

	/**
	 * Create a client monitor command manager class
	 * 
	 * @param monitor
	 *            the hook to the monitor frame
	 * @param client
	 *            the connection to the federation
	 */
	public ClientMonitorCommandManager(MonitorFrame monitor, MonitorClient client) {
		monitorFrame = monitor;
		monitorClient = client;
		commandSessionCount = 0;
	}

	public void advCommand() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,BROKER_MONITOR],[command,isPresent,'TEXT'],"
						+ "[brokerID,isPresent,'TEXT']," + "[" + SystemMonitor.COMM_SESSION_ID
						+ ",isPresent,'dummy']");
		try {
			monitorClient.advertise(adv);
			hasAdvCommand = true;
		} catch (ClientException e) {
			hasAdvCommand = false;
		}
	}

	public void setConnectionManager(MonitorClient client) {
		monitorClient = client;
	}

	public void setNotifyDialog(MonitorDialog dialog) {
		monitorDialog = dialog;
	}

	/**
	 * To get set of adv or sub that is in the broker
	 * 
	 * @param brokerID
	 *            the target broker id
	 * @param type
	 *            to indicate advertisement or subscription
	 * @return true if success
	 */
	public boolean sendGetMsgSetCommand(String brokerID, int type) {
		boolean result = false;
		try {
			if (!hasAdvCommand) {
				advCommand();
			}
			String commandSessionID = getCommandSessionID(monitorClient.getDefaultBrokerAddress().getNodeURI());
			Publication pub;
			switch (type) {
			case TYPE_ADV:
				pub = MessageFactory.createPublicationFromString(SystemMonitor.getGetAdvSetCommClientPubStr(brokerID,
						commandSessionID));
				break;
			case TYPE_SUB:
				pub = MessageFactory.createPublicationFromString(SystemMonitor.getGetSubSetCommClientPubStr(brokerID,
						commandSessionID));
				break;
			default:
				result = false;
				monitorFrame.setErrorString("Unknow msg set type detected");
				return result;
			}

			Subscription sub = MessageFactory.createSubscriptionFromString(SystemMonitor.getMsgSetDeliverySubStr(brokerID,
					commandSessionID));
			subID = monitorClient.subscribe(sub).getMessageID();
			monitorClient.publish(pub);
			result = true;
		} catch (ClientException e) {
			monitorFrame.setErrorString(e.getMessage());
			result = false;
		} catch (ParseException e) {
			monitorFrame.setErrorString(e.getMessage());
			result = false;
		}
		
		return result;
	}

	/**
	 * handle the return message from the broker
	 * 
	 * @param message
	 *            the publication message gotten back from the broker
	 */
	public void handleMessage(Publication message) {
		if (monitorDialog != null) {
			Map<String, Object> pairMap = (Map<String, Object>) message.getPayload();
			Object setofMsg = pairMap.get(SystemMonitor.MSG_SET_MAP_KEY);
			monitorDialog.notify(setofMsg);
			/* Now unsubscribe the msg delivery message */
			try {
				monitorClient.unSubscribe(subID);
			} catch (ClientException e) {
				monitorFrame.setErrorString(e.getMessage());
			}
		}
	}

	private String getCommandSessionID(String id) {
		String result = id + "-commandSession-" + commandSessionCount;
		commandSessionCount++;
		return result;
	}

}

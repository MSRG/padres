package ca.utoronto.msrg.padres.broker.router;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig.CycleType;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.controller.LinkInfo;
import ca.utoronto.msrg.padres.broker.controller.OverlayManager;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.common.comm.OutputQueue;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

public class ForwarderImpl implements Forwarder {

	private Router router;

	private BrokerCore brokerCore;

	private static int INITIAL_MAX_MSG_RATE = 10000;

	static Logger routerLogger = Logger.getLogger(Router.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	static Logger msgPathLogger = Logger.getLogger("MessagePath");

	public ForwarderImpl(Router router, BrokerCore broker) {
		this.router = router;
		brokerCore = broker;
	}

	/**
	 * Checking forward condition for the publication message.
	 * 
	 * @param pubMsg
	 *            : publicationMessage,
	 * @param subIDs
	 *            : set of subscriptionMessages that matching m
	 * @return
	 */
	private Set<PublicationMessage> forwardPubMsg(PublicationMessage pubMsg, Set<String> subIDs) {
		Set<PublicationMessage> messagesToRoute = new HashSet<PublicationMessage>();
		for (String subMsgID : subIDs) {
			MessageDestination nextHopID = null;
			Serializable payload = pubMsg.getPublication().getPayload();
			PublicationMessage pubMsgCopy = pubMsg.duplicate();
			pubMsgCopy.getPublication().setPayload(payload);
			nextHopID = router.getMessageDestination(subMsgID, MessageType.SUBSCRIPTION);
			if (nextHopID == null) {
				nextHopID = router.getMessageDestination(	subMsgID,
															MessageType.COMPOSITESUBSCRIPTION);
			}
			if (brokerCore.isDynamicCycle()) {
				// shou Apr 19,2007
				// HACK for dynamic cycle and composite subscription.
				List<Object> tidAndLasthop;
				if (subMsgID.contains("_")) {
					tidAndLasthop = chooseBestPathForPublication(subMsgID, nextHopID);
				} else {
					String tempTid = (String) ((PublicationMessage) pubMsgCopy).getPublication()
							.getPairMap().get("tid");
					tidAndLasthop = chooseBestPathForPublication(	subMsgID + "_" + tempTid,
																	nextHopID);
				}
				String newTid = (String) tidAndLasthop.get(0);
				nextHopID = (MessageDestination) tidAndLasthop.get(1);
				pubMsgCopy.getPublication().addPair("tid", newTid);
			}

			boolean nextHopIDNone = nextHopID.getDestinationID().equals("none");
			boolean nextHopNotAClient = nextHopID.isBroker();
			boolean nextHopSameAsLastHop = pubMsgCopy.getLastHopID().equals(nextHopID);

			String pubMsgID = pubMsgCopy.getMessageID();

			pubMsgCopy.setNextHopID(nextHopID);
			// Criteria for when not to forward publications:
			// - Must not forward to source (next hop ID different than last hop
			// ID), unless source is a client
			// - Must not forward if next hop ID is "none". This is a keyword to
			// indicate next hop ID is unset
			if (!((nextHopSameAsLastHop && nextHopNotAClient) || nextHopIDNone)) {
				boolean addIt = true;
				for (PublicationMessage tmp : messagesToRoute) {
					try {
						// check the duplicated message
						if (pubMsgID.equals(tmp.getMessageID())
								&& tmp.getNextHopID().equals(nextHopID)) {
							addIt = false;
						}
					} catch (Exception e) {
						routerLogger.error("Jess Forwarder got the exception : " + e);
						exceptionLogger.error("Jess Forwarder got the exception : " + e);
					}
				}
				if (addIt) {
					routerLogger.debug("Adding message: " + pubMsgCopy.toString()
							+ " to messagesToRoute.");
					messagesToRoute.add(pubMsgCopy);
				} else {
					routerLogger.debug("Not adding message: " + pubMsgCopy.toString()
							+ " to messagesToRoute.");
				}
			}
		}

		return messagesToRoute;
	}

	public Set<PublicationMessage> forwardPubMsgs(
			Map<PublicationMessage, Set<String>> pubMsgsToSubIDs) {
		Set<PublicationMessage> messagesToRoute = new HashSet<PublicationMessage>();
		// Set messagesToRoute = new TreeSet(new MessageComparator());
		for (PublicationMessage pubMsg : pubMsgsToSubIDs.keySet()) {
			routerLogger.debug("forwarding in " + brokerCore.getBrokerID() + ": " + pubMsg);
			msgPathLogger.info("forwarding in " + brokerCore.getBrokerID() + ": " + pubMsg);
			Set<String> subIDs = pubMsgsToSubIDs.get(pubMsg);
			Set<PublicationMessage> messagesToAdd = forwardPubMsg(pubMsg, subIDs);
			messagesToRoute.addAll(messagesToAdd);
		}

		return messagesToRoute;
	}

	public Set<SubscriptionMessage> forwardSubMsg(SubscriptionMessage subMsg,
			Set<AdvertisementMessage> advMsgSet) {
		msgPathLogger.info(brokerCore.getBrokerID() + ": " + subMsg);
		Set<SubscriptionMessage> messagesToRoute = new HashSet<SubscriptionMessage>();
		for (AdvertisementMessage advMsg : advMsgSet) {
			String advID = advMsg.getMessageID();
			SubscriptionMessage subMsgCopy = subMsg.duplicate();
			MessageDestination advNextHopID = router
					.getMessageDestination(advID, MessageType.ADVERTISEMENT);
			if (brokerCore.isCycle()) {
				Predicate subTIDPredicate = subMsgCopy.getSubscription().getPredicateMap()
						.get("tid");
				String tidValue = (String) subTIDPredicate.getValue();
				if (tidValue.startsWith("$S$Tid")) {
					Map<String, Predicate> advPredicateMap = advMsg.getAdvertisement()
							.getPredicateMap();
					String advTID = (String) advPredicateMap.get("tid").getValue();
					// set the TID value in the subscription
					subTIDPredicate.setValue(advTID);
					subMsgCopy.setMessageID(subMsg.getMessageID() + "_" + advTID);
				}
			}

			subMsgCopy.setNextHopID(advNextHopID);

			// Criteria for when not to forward subscriptions:
			// - Must not forward if next hop ID is "none". This is a keyword to
			// indicate next hop ID is unset
			// - forward only to brokers and DB clients (gli)
			// - Must not forward to source (next hop ID different than last hop
			// ID)
			if (!advNextHopID.getDestinationID().equals("none")
					&& (advNextHopID.isDB() || advNextHopID.isBroker())
					&& !advNextHopID.equals(subMsgCopy.getLastHopID())) {
				boolean addIt = true;
				if (!brokerCore.isCycle()) {
					for (SubscriptionMessage sub_j : messagesToRoute) {
						// added by gli
						// check duplicated forwarding message;
						if (subMsgCopy.getMessageID().equals(sub_j.getMessageID())
								&& sub_j.getNextHopID().equals(advNextHopID)) {
							addIt = false;
						}
					}
				}
				if (addIt) {
					routerLogger.debug("Adding message: " + subMsgCopy.toString()
							+ " to messagesToRoute.");
					messagesToRoute.add(subMsgCopy);
				} else {
					routerLogger.debug("Not adding message: " + subMsgCopy.toString()
							+ " to messagesToRoute.");
				}
			}
		}

		return messagesToRoute;
	}

	public Set<SubscriptionMessage> forwardSubMsgs(
			Map<SubscriptionMessage, Set<AdvertisementMessage>> subMsgsToAdvMsgs) {
		Set<SubscriptionMessage> messagesToRoute = new HashSet<SubscriptionMessage>();
		for (SubscriptionMessage subMsg : subMsgsToAdvMsgs.keySet()) {
			msgPathLogger.info(brokerCore.getBrokerID() + " (CS): " + subMsg);
			Set<SubscriptionMessage> messagesToAdd = new HashSet<SubscriptionMessage>();
			messagesToAdd = forwardSubMsg(subMsg, subMsgsToAdvMsgs.get(subMsg));
			messagesToRoute.addAll(messagesToAdd);
		}

		return messagesToRoute;
	}

	public Set<SubscriptionMessage> forwardSubMsgs(AdvertisementMessage advMsg,
			Set<SubscriptionMessage> subMsgs) {
		msgPathLogger.info(brokerCore.getBrokerID() + " for adv: " + advMsg);
		Set<SubscriptionMessage> messagesToRoute = new HashSet<SubscriptionMessage>();
		MessageDestination nextHopID = advMsg.getLastHopID();
		// get TID value of the advertisement if applicable
		Map<String, Predicate> predMap = advMsg.getAdvertisement().getPredicateMap();
		String advTID = predMap.containsKey("tid") ? (String) predMap.get("tid").getValue() : "";
		for (SubscriptionMessage msg : subMsgs) {
			SubscriptionMessage subMsgCopy = msg.duplicate();
			// Criteria for when not to forward subscriptions:
			// - Must not forward if next hop ID is "none". This is a keyword to
			// indicate next hop
			// ID is unset
			// - forward subscriptions only to DB clients (gli) and brokers
			// - Must not forward to source (next hop ID different than last hop
			// ID)
			if (!nextHopID.getDestinationID().equals("none")
					&& (nextHopID.isDB() || nextHopID.isBroker())
					&& !nextHopID.equals(subMsgCopy.getLastHopID())) {
				routerLogger.debug("Adding message: " + subMsgCopy.toString()
						+ " to messagesToRoute.");
				subMsgCopy.setNextHopID(nextHopID);
				Map<String, Predicate> subPredMap = subMsgCopy.getSubscription().getPredicateMap();
				if (subPredMap.containsKey("tid") && !advTID.equals("nil")) {
					subPredMap.get("tid").setValue(advTID);
					subMsgCopy.setMessageID(subMsgCopy.getMessageID() + "_" + advTID);
				}
				messagesToRoute.add(subMsgCopy);
			}
		}
		return messagesToRoute;
	}

	public Set<AdvertisementMessage> broadcastAdvMsg(AdvertisementMessage advMsg) {
		msgPathLogger.info(brokerCore.getBrokerID() + ": " + advMsg);
		Set<AdvertisementMessage> messagesToRoute = new HashSet<AdvertisementMessage>();

		// Broadcast advertisements
		OverlayManager overlayManager = brokerCore.getOverlayManager();
		if (overlayManager != null) {
			OverlayRoutingTable ORT = overlayManager.getORT();
			Map<MessageDestination, OutputQueue> neighbours = ORT.getBrokerQueues();
			routerLogger.debug("Adding broadcast advertisement to messagesToRoute.");
			synchronized (neighbours) {
				for (MessageDestination nextHopID : neighbours.keySet()) {
					if (!nextHopID.equals(advMsg.getLastHopID())) {
						AdvertisementMessage advMessageToAdd = advMsg.duplicate();
						advMessageToAdd.setNextHopID(nextHopID);
						messagesToRoute.add(advMessageToAdd);
					}
				}
			}
		}
		return messagesToRoute;
	}

	public Set<UnsubscriptionMessage> forwardUnsubMsg(UnsubscriptionMessage m, Set<String> advIDs) {
		msgPathLogger.info(brokerCore.getBrokerID() + ": " + m);
		Set<UnsubscriptionMessage> messagesToRoute = new HashSet<UnsubscriptionMessage>();

		for (String id : advIDs) {
			UnsubscriptionMessage tempMessage = new UnsubscriptionMessage(new Unsubscription(m
					.getUnsubscription().getSubID()), m.getMessageID(), m.getLastHopID());

			MessageDestination tempNextHopID = router
					.getMessageDestination(id, MessageType.ADVERTISEMENT);
			tempMessage.setNextHopID(tempNextHopID);
			// We need to figure out those advertisements that does not really
			// match this
			// subscription. They have the same last hop with sub's last hop.
			if (!tempNextHopID.getDestinationID().equals("none") && tempNextHopID.isBroker()
					&& !tempMessage.getLastHopID().equals(tempNextHopID)) {
				boolean addIt = true;
				for (UnsubscriptionMessage unsubMsg : messagesToRoute) {
					if (tempMessage.equals(unsubMsg)) {
						addIt = false;
					}
				}
				if (addIt) {
					messagesToRoute.add(tempMessage);
				}
			}
		}

		return messagesToRoute;
	}

	public Set<UnsubscriptionMessage> forwardUnsubMsgs(
			Map<UnsubscriptionMessage, Set<String>> unsubMsgsToAdvIDs) {
		Set<UnsubscriptionMessage> messagesToRoute = new HashSet<UnsubscriptionMessage>();
		for (UnsubscriptionMessage unsubMsg : unsubMsgsToAdvIDs.keySet()) {
			msgPathLogger.info(brokerCore.getBrokerID() + " (CS): " + unsubMsg);
			Set<UnsubscriptionMessage> messagesToAdd = new HashSet<UnsubscriptionMessage>();
			Set<String> advIDs = unsubMsgsToAdvIDs.get(unsubMsg);
			messagesToAdd = forwardUnsubMsg(unsubMsg, advIDs);
			messagesToRoute.addAll(messagesToAdd);
		}

		return messagesToRoute;
	}

	public Set<UnadvertisementMessage> broadcastUnadvMsg(UnadvertisementMessage m) {
		msgPathLogger.info(brokerCore.getBrokerID() + ": " + m);
		Set<UnadvertisementMessage> messagesToRoute = new HashSet<UnadvertisementMessage>();
		String advMessageID = m.getUnadvertisement().getAdvID();
		// Broadcast unadvertisement
		OverlayRoutingTable ORT = brokerCore.getOverlayManager().getORT();
		Map<MessageDestination, OutputQueue> neighbours = ORT.getBrokerQueues();

		routerLogger.debug("Adding broadcast unadvertisement to messagesToRoute.");
		for (MessageDestination nextHopID : neighbours.keySet()) {
			if (!nextHopID.equals(m.getLastHopID())) {
				MessageDestination lastHopID = brokerCore.getBrokerDestination();
				Unadvertisement tempUnadv = new Unadvertisement(advMessageID);
				UnadvertisementMessage tempUnadvMessage = new UnadvertisementMessage(tempUnadv,
						m.getMessageID(), lastHopID);
				tempUnadvMessage.setNextHopID(nextHopID);

				messagesToRoute.add(tempUnadvMessage);
			}
		}
		if (routerLogger.isDebugEnabled())
			routerLogger.debug("Set of messages need to be routed out: "
					+ messagesToRoute.toString());
		return messagesToRoute;
	}

	private List<Object> chooseBestPathForPublication(String msgID, MessageDestination lastHop) {
		// first is tid, second is message destination
		List<Object> result = new ArrayList<Object>();
		int index = msgID.indexOf("_");
		String subMessageId = msgID.substring(0, index);
		String tid = msgID.substring(index + 1);
		result.add(0, tid);
		result.add(1, lastHop);
		Map<MessageDestination, LinkInfo> statisticTable = brokerCore.getOverlayManager().getORT()
				.getStatisticTable();
		if (brokerCore.getCycleOption() == CycleType.DYNAMIC) {
			int minMsgRate = INITIAL_MAX_MSG_RATE;
			if (statisticTable.containsKey(lastHop)) {
				LinkInfo currentLink = statisticTable.get(lastHop);
				if (currentLink.getStatus()) {
					minMsgRate = currentLink.getMsgRate();
					if (minMsgRate == 0) {
						return result;
					}
				}
			}

			Map<String, SubscriptionMessage> allSubs = router.getSubscriptions();
			for (String msgIDTid : allSubs.keySet()) {
				if (msgIDTid.startsWith(subMessageId)) {
					SubscriptionMessage subMsg = allSubs.get(msgIDTid);
					String tidValue = (String) subMsg.getSubscription().getPredicateMap()
							.get("tid").getValue();
					if (!tidValue.equals("$S$Tid")) {
						MessageDestination tempLast = subMsg.getLastHopID();
						if (!tempLast.equals(lastHop)) {
							if (statisticTable.containsKey(tempLast)) {
								LinkInfo tempLink = statisticTable.get(tempLast);
								int tempMsgRate = tempLink.getMsgRate();
								if ((tempMsgRate < minMsgRate) && (tempLink.getStatus())) {
									minMsgRate = tempMsgRate;
									result.add(0, tidValue);
									result.add(1, tempLast);
								}
							}
						}
					}
				}
			}
		}
		return result;
	}
}

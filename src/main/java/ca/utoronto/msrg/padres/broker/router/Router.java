/**
 * 
 */
package ca.utoronto.msrg.padres.broker.router;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.matching.AdvMsgNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.CSMsgNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.DuplicateMsgFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.Matcher;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
import ca.utoronto.msrg.padres.broker.router.matching.MessageNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.PubMsgNotConformedException;
import ca.utoronto.msrg.padres.broker.router.matching.SubMsgNotFoundException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

// import matching.MessageComparator;

/**
 * @author shou
 * 
 */
public abstract class Router {

	protected static Logger messagePathLogger = Logger.getLogger("MessagePath");

	protected static Logger routerLogger = Logger.getLogger(Router.class);

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	protected BrokerCore brokerCore;

	protected Matcher matcher;

	protected Forwarder forwarder;

	protected PreProcessor preprocessor;

	protected PostProcessor postprocessor;

	protected Map<String, AdvertisementMessage> workingAdvs;

	protected Map<String, UnadvertisementMessage> workingUnAdvs;

	protected Map<String, SubscriptionMessage> workingSubs;

	protected Map<String, PublicationMessage> workingPubs;

	protected Map<String, CompositeSubscriptionMessage> workingCompSubs;

	protected Map<String, Set<MessageDestination>> routedSubs;

	public Router(BrokerCore broker) {
		brokerCore = broker;
		workingAdvs = new ConcurrentHashMap<String, AdvertisementMessage>();
		workingUnAdvs = new HashMap<String, UnadvertisementMessage>();
		workingSubs = new ConcurrentHashMap<String, SubscriptionMessage>();
		workingPubs = new ConcurrentHashMap<String, PublicationMessage>();
		workingCompSubs = new ConcurrentHashMap<String, CompositeSubscriptionMessage>();
		routedSubs = new HashMap<String, Set<MessageDestination>>();
		matcher = createMatcher(broker);
		preprocessor = createPreProcessor(broker);
		forwarder = createForwarder(this, broker);
		postprocessor = createPostProcessor(broker);
	}

	protected abstract Matcher createMatcher(BrokerCore broker);

	protected abstract PreProcessor createPreProcessor(BrokerCore broker);

	protected abstract Forwarder createForwarder(Router router, BrokerCore broker);

	protected abstract PostProcessor createPostProcessor(BrokerCore broker);

	public void initialize() {
		postprocessor.initialize();
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public Map<String, SubscriptionMessage> getSubscriptions() {
		return workingSubs;
	}

	public SubscriptionMessage getSubscriptionMessage(String subID) {
		return workingSubs.get(subID);
	}

	public SubscriptionMessage getSubscriptionMessage(Subscription sub) {
		for (SubscriptionMessage subMsg : workingSubs.values()) {
			if (subMsg.getSubscription().equals(sub))
				return subMsg;
		}
		return null;
	}

	public Map<String, CompositeSubscriptionMessage> getCompositeSubscriptions() {
		return workingCompSubs;
	}

	public CompositeSubscriptionMessage getCompositeSubscription(String csMsgID) {
		return workingCompSubs.get(csMsgID);
	}

	public Map<String, AdvertisementMessage> getAdvertisements() {
		return workingAdvs;
	}

	public Map<String, UnadvertisementMessage> getUnAdvertisements() {
		return workingUnAdvs;
	}

	public AdvertisementMessage getAdvertisement(String msgID) {
		return workingAdvs.get(msgID);
	}

	public Set<AdvertisementMessage> getAdvertisements(Set<String> advIDs) {
		Set<AdvertisementMessage> advs = new HashSet<AdvertisementMessage>();
		for (String id : advIDs) {
			advs.add(workingAdvs.get(id));
		}
		return advs;
	}

	public PublicationMessage getPublicationMessage(String pubMsgID) {
		return workingPubs.get(pubMsgID);
	}

	public Map<String, Set<MessageDestination>> getRoutedSubs() {
		return routedSubs;
	}

	/**
	 * This method will return all messages with next hop IDs set, that are to be sent
	 * 
	 * @param msg
	 * @return set of messages to be routed out.
	 */
	public Set<Message> handleMessage(Message msg) {
		messagePathLogger.info(brokerCore.getBrokerID() + ": " + msg);
		MessageType type = msg.getType();
		Set<Message> messagesToRoute = new HashSet<Message>();

		// sets the tree IDs for the message
		preprocessor.preprocess(msg);
		try {
			// process through the matcher
			if (type.equals(MessageType.ADVERTISEMENT)) {
				messagesToRoute = handleMessage((AdvertisementMessage) msg);
			} else if (type.equals(MessageType.SUBSCRIPTION)) {
				messagesToRoute = handleMessage((SubscriptionMessage) msg);
			} else if (type.equals(MessageType.COMPOSITESUBSCRIPTION)) {
				messagesToRoute = handleMessage((CompositeSubscriptionMessage) msg);
			} else if (type.equals(MessageType.PUBLICATION)) {
				messagesToRoute = handleMessage((PublicationMessage) msg);
			} else if (type.equals(MessageType.UNSUBSCRIPTION)) {
				messagesToRoute = handleMessage((UnsubscriptionMessage) msg);
			} else if (type.equals(MessageType.UNCOMPOSITESUBSCRIPTION)) {
				messagesToRoute = handleMessage((UncompositesubscriptionMessage) msg);
			} else if (type.equals(MessageType.UNADVERTISEMENT)) {
				messagesToRoute = handleMessage((UnadvertisementMessage) msg);
			} else if (type.equals(MessageType.UNDEFINED)) {

			} else {
				// what??
			}
		} catch (PubMsgNotConformedException e) {
			routerLogger.error(e);
			exceptionLogger.error(e);
		} catch (MessageNotFoundException e) {
			routerLogger.error(e);
			exceptionLogger.error(e);
		} catch (DuplicateMsgFoundException e) {
			routerLogger.warn(e);
			exceptionLogger.warn(e);
		} catch (MatcherException e) {
			routerLogger.warn(e);
			exceptionLogger.warn(e);
		}

		postprocessor.postprocess(msg, type, messagesToRoute);

		return messagesToRoute;
	}

	protected void addAdvWorkingMemory(AdvertisementMessage advMsg) {
		workingAdvs.put(advMsg.getMessageID(), advMsg);
	}
	
	protected Set<Message> handleMessage(AdvertisementMessage advMsg)
			throws DuplicateMsgFoundException, MatcherException {
		// check for duplicate add to working memory
		if (workingAdvs.containsKey(advMsg.getMessageID()))
			throw new DuplicateMsgFoundException("Duplicate advertisement message is found : "
					+ advMsg);
		addAdvWorkingMemory(advMsg);

		// pass through matcher
		Set<String> subMsgIDSet = matcher.add(advMsg);
		// check whether any of these sub msgs has been already forwarded to the
		// broker where the advertisement came from.
		Set<SubscriptionMessage> forwardSubMsgs = new HashSet<SubscriptionMessage>();
		for (String subMsgID : subMsgIDSet) {
			// if a cyclic network is used, forward all the subs matching the adv; otherwise forward
			// only the subs that are not already forwards towards the adv origin
			if (brokerCore.isCycle()) {
				// this should happen only at the edge broker, at the subscriber side. We need to
				// get the sub message with unassigned tid.
				if (subMsgID.indexOf("_") == -1)
					forwardSubMsgs.add(workingSubs.get(subMsgID));
			} else {
				if (routedSubs.containsKey(subMsgID)) {
					Set<MessageDestination> msgDestinations = routedSubs.get(subMsgID);
					if (!msgDestinations.contains(advMsg.getLastHopID())) {
						msgDestinations.add(advMsg.getLastHopID());
						forwardSubMsgs.add(workingSubs.get(subMsgID));
					}
				} else {
					// all the stored sub also must have an entry in routedSubs
					// this is handled in handleMessage(SubscriptionMessage)
					routerLogger.error("Sub with message ID " + subMsgID
							+ " not found in forward set");
					throw new MatcherException("Sub with message ID " + subMsgID
							+ " not found in forward set");
				}
			}
		}
		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardSubMsgs(advMsg, forwardSubMsgs));
		messagesToRoute.addAll(forwarder.broadcastAdvMsg(advMsg));
		return messagesToRoute;
	}

	protected Set<Message> handleMessage(SubscriptionMessage subMsg) throws MatcherException {
		// add sub message to working memory
		addSubWorkingMemory(subMsg);
		// send through matcher to get matching advertisements
		Set<String> advIDs = matcher.add(subMsg);
		if (!brokerCore.isCycle()) {
			// add all the destinations the subscription is to be forwarded
			Set<MessageDestination> forwardedDest = routedSubs.get(subMsg.getMessageID());
			for (String advID : advIDs) {
				forwardedDest.add(getAdvertisement(advID).getLastHopID());
			}
		}
		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardSubMsg(subMsg, getAdvertisements(advIDs)));
		return messagesToRoute;
	}

	protected void addSubWorkingMemory(SubscriptionMessage subMsg) {
		String subID = subMsg.getMessageID();
		if (!workingSubs.containsKey(subID)) {
			workingSubs.put(subID, subMsg);
			// add to the routed messages
			routedSubs.put(subID, new HashSet<MessageDestination>());
		} else {
			// TODO: Need to make sure we haven't seen the sub before?
		}
	}

	protected Set<Message> handleMessage(CompositeSubscriptionMessage csMsg)
			throws MatcherException {
		// add to the working memory
		String csMsgID = csMsg.getMessageID();
		workingCompSubs.put(csMsgID, csMsg);
		CompositeSubscription compositeSub = csMsg.getSubscription();
		Map<String, Subscription> atomicSubs = compositeSub.getSubscriptionMap();
		for (Entry<String, Subscription> subEntry : atomicSubs.entrySet()) {
			String aSubMsgID = csMsgID + "-" + subEntry.getKey();
			Subscription aSub = subEntry.getValue();
			SubscriptionMessage aSubMsg = new SubscriptionMessage(aSub, aSubMsgID,
					csMsg.getLastHopID());
			// add to atomic subscription map
			addSubWorkingMemory(aSubMsg);
		}
		// send through matcher
		Map<SubscriptionMessage, Set<String>> subMsgsToAdvIDs = matcher.add(csMsg);
		// get the matching advertisement messages
		Map<SubscriptionMessage, Set<AdvertisementMessage>> subMsgsToAdvMsgs = new HashMap<SubscriptionMessage, Set<AdvertisementMessage>>();
		for (SubscriptionMessage subMsg : subMsgsToAdvIDs.keySet()) {
			Set<String> advIDs = subMsgsToAdvIDs.get(subMsg);
			subMsgsToAdvMsgs.put(subMsg, getAdvertisements(advIDs));
			if (!brokerCore.isCycle()) {
				// add all the destinations the subscription is to be forwarded
				Set<MessageDestination> forwardedDest = routedSubs.get(subMsg.getMessageID());
				for (String advID : advIDs) {
					forwardedDest.add(getAdvertisement(advID).getLastHopID());
				}
			}
		}
		// send through the forwarder
		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardSubMsgs(subMsgsToAdvMsgs));
		return messagesToRoute;
	}

	protected Set<Message> handleMessage(PublicationMessage pubMsg)
			throws PubMsgNotConformedException {
		if (brokerCore.getBrokerConfig().isPubConformCheck()) {
			// checking whether the publication is a special one.
			boolean specialPub = false;
			MessageDestination lasthop = pubMsg.getLastHopID();
			// For network construction, publications of BROKER_CTL, BROKER_MONITOR,
			// HEARTBEAT_MANAGER and NETWORK_DISCOVERY should be conformed by advertisement
			String msgClass = pubMsg.getPublication().getClassVal();
			if (msgClass.equals("BROKER_CONTROL") || msgClass.equals("BROKER_MONITOR")
					|| msgClass.equals("TRACEROUTE_MESSAGE")
					|| msgClass.equals("HEARTBEAT_MANAGER") || msgClass.equals("NETWORK_DISCOVERY")
					|| msgClass.equals("BROKER_INFO")) {
				if (lasthop.isBroker() && !lasthop.isInternalQueue()
						&& !lasthop.equals(brokerCore.getBrokerDestination())
						&& !msgClass.equals("BROKER_CONTROL")) {
					specialPub = false;
				} else {
					specialPub = true;
				}
			}
			// verify against the given advertisement if it is not a special case
			if (!lasthop.isBroker() || lasthop.getDestinationID().equals("none") || specialPub) {
				Set<String> matchedAdvs = matcher.getMatchingAdvs(pubMsg);
				// get the matching advertisement came from the same origin as the publication
				AdvertisementMessage matchingAdvMsg = null;
				if (matchedAdvs != null) {
					for (String advID : matchedAdvs) {
						AdvertisementMessage matchedAdvMsg = workingAdvs.get(advID);
						if (matchedAdvMsg.getLastHopID().equals(lasthop)) {
							matchingAdvMsg = matchedAdvMsg;
							break;
						}
					}
				}
				// if nothing matched, throw an exception
				if (matchingAdvMsg == null) {
					routerLogger.error("You did not advertise correctly before you published : "
							+ pubMsg);
					throw new PubMsgNotConformedException(
							"You did not advertise correctly before you published : " + pubMsg);
				}
				// set the publication's TID to the matching advertisement's.
				Map<String, Serializable> pubPairMap = pubMsg.getPublication().getPairMap();
				if (pubPairMap.containsKey("tid")
						&& pubPairMap.get("tid").toString().startsWith("$S$")) {
					String advTID = (String) matchingAdvMsg.getAdvertisement().getPredicateMap().get(
							"tid").getValue();
					pubPairMap.put("tid", advTID);
				}
			}
		}

		// add publication message to the working memory
		workingPubs.put(pubMsg.getMessageID(), pubMsg);

		// process it through the matcher
		Map<PublicationMessage, Set<String>> pubMsgToSubIDs = matcher.getMatchingSubs(pubMsg);
		routerLogger.debug("got " + pubMsgToSubIDs.size() + " matching subs for " + pubMsg);

		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardPubMsgs(pubMsgToSubIDs));

		// remove the publication messages from the working memory, if possible
		if (!matcher.isPartialMatch())
			workingPubs.remove(pubMsg.getMessageID());
		return messagesToRoute;
	}

	protected Set<Message> handleMessage(UnsubscriptionMessage unsubMsg)
			throws SubMsgNotFoundException {
		String sid = unsubMsg.getUnsubscription().getSubID();

		MessageDestination subMsgLastHop = null;
		Set<String> advIDs = new HashSet<String>();
		for (String subID : workingSubs.keySet()) {
			String subIDPart = subID.split("_")[0];
			if (subIDPart.equals(sid)) {
				UnsubscriptionMessage tempUnSubMsg = unsubMsg.duplicate();
				tempUnSubMsg.getUnsubscription().setSubID(subID);
				advIDs.addAll(matcher.add(tempUnSubMsg));
				subMsgLastHop = workingSubs.get(subID).getLastHopID();
			}
		}

		if (subMsgLastHop == null)
			throw new SubMsgNotFoundException("Subscription message is not found : " + unsubMsg);

		// get the IDs of the advertisements whose last hop is not the same as the subscription
		// being removed
		Set<String> forwardAdvIDs = new HashSet<String>();
		for (String advID : advIDs) {
			if (!workingAdvs.get(advID).getLastHopID().equals(subMsgLastHop)) {
				forwardAdvIDs.add(advID);
			}
		}

		// remove the subscriptions from the working memory
		List<String> remIDs = new ArrayList<String>();
		for (String subID : workingSubs.keySet()) {
			String subIDPart = subID.split("_")[0];
			if (subIDPart.equals(sid))
				remIDs.add(subID);
		}
		for (String subID : remIDs)
			workingSubs.remove(subID);

		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardUnsubMsg(unsubMsg, forwardAdvIDs));
		return messagesToRoute;
	}

	protected Set<Message> handleMessage(UncompositesubscriptionMessage unsubMsg)
			throws CSMsgNotFoundException {
		String sid = unsubMsg.getUncompositesubscription().getSubID();

		MessageDestination subMsgLastHop = null;
		if (workingCompSubs.containsKey(sid)) {
			subMsgLastHop = workingCompSubs.get(sid).getLastHopID();
		} else {
			throw new CSMsgNotFoundException("Composite Subscription message is not found : "
					+ unsubMsg);
		}

		Map<UnsubscriptionMessage, Set<String>> unSubadvIDMap = matcher.add(unsubMsg);

		// get the IDs of the advertisements whose last hop is not the same as the subscription
		// being removed
		Map<UnsubscriptionMessage, Set<String>> filteredMap = new HashMap<UnsubscriptionMessage, Set<String>>();
		for (Entry<UnsubscriptionMessage, Set<String>> advIDsFromMatcher : unSubadvIDMap.entrySet()) {
			Set<String> filteredAdvIDs = new HashSet<String>();
			for (String advID : advIDsFromMatcher.getValue()) {
				if (!workingAdvs.get(advID).getLastHopID().equals(subMsgLastHop)) {
					filteredAdvIDs.add(advID);
				}
			}
			filteredMap.put(advIDsFromMatcher.getKey(), filteredAdvIDs);
		}

		// remove the subscriptions from the working memory
		CompositeSubscription compositeSub = workingCompSubs.remove(sid).getSubscription();
		Map<String, Subscription> atomicSubs = compositeSub.getSubscriptionMap();
		for (Entry<String, Subscription> subEntry : atomicSubs.entrySet()) {
			String aSubMsgID = sid + "-" + subEntry.getKey();
			List<String> remIDs = new ArrayList<String>();
			for (String subID : workingSubs.keySet()) {
				String subIDPart = subID.split("_")[0];
				if (subIDPart.equals(aSubMsgID))
					remIDs.add(subID);
			}
			for (String subID : remIDs)
				workingSubs.remove(subID);
		}

		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.forwardUnsubMsgs(filteredMap));
		return messagesToRoute;
	}

	protected Set<Message> handleMessage(UnadvertisementMessage unAdvMsg)
			throws AdvMsgNotFoundException, DuplicateMsgFoundException {
		// Remove the adv from this broker.
		String aid = unAdvMsg.getUnadvertisement().getAdvID();
		if (!workingAdvs.containsKey(aid)) {
			throw new AdvMsgNotFoundException("Advertisement message is not found : " + unAdvMsg);
		}

		matcher.add(unAdvMsg);
		workingAdvs.remove(aid);

		Set<Message> messagesToRoute = new HashSet<Message>();
		messagesToRoute.addAll(forwarder.broadcastUnadvMsg(unAdvMsg));
		return messagesToRoute;
	}

	public MessageDestination getMessageDestination(String msgID, MessageType type) {
		MessageDestination dest = null;
		if (type.equals(MessageType.ADVERTISEMENT)) {
			if (workingAdvs.containsKey(msgID)) {
				dest = workingAdvs.get(msgID).getLastHopID();
			}
		} else if (type.equals(MessageType.SUBSCRIPTION)) {
			if (workingSubs.containsKey(msgID))
				dest = workingSubs.get(msgID).getLastHopID();
		} else if (type.equals(MessageType.COMPOSITESUBSCRIPTION)) {
			if (workingCompSubs.containsKey(msgID)) {
				dest = workingCompSubs.get(msgID).getLastHopID();
			}

		}
		return dest;
	}

	public List<SubscriptionMessage> getSubscriptions(PublicationMessage pub) {
		List<SubscriptionMessage> list = new ArrayList<SubscriptionMessage>();
		for (Set<String> s : matcher.getMatchingSubs(pub).values()) {
			for (String id : s)
				list.add(getSubscriptionMessage(id));
		}
		return list;
	}

	/**
	 * Checks the internal states to see if a particular advertisement is received from a given
	 * destination.
	 * 
	 * @param fromDestination
	 *            The from-destination we are looking for
	 * @param advertisement
	 *            The advertisement to be checked for
	 * @return true if an advertisement from the given destination is found in the internal states;
	 *         false otherwise.
	 */
	public boolean checkStateForAdvertisement(MessageDestination fromDestination,
			Advertisement advertisement) {
		for (AdvertisementMessage advMsg : workingAdvs.values()) {
			if (advMsg.getLastHopID().equals(fromDestination)
					&& advMsg.getAdvertisement().equalPredicates(advertisement))
				return true;
		}
		return false;
	}

	/**
	 * Checks the internal states to see if a particular subscription is received from a given
	 * destination.
	 * 
	 * @param lastHop
	 *            The from-destination we are looking for
	 * @param subscription
	 *            The subscription to be checked
	 * @return true if a subscription from the given destination is found in the internal states;
	 *         false otherwise.
	 */
	public boolean checkStateForSubscription(MessageDestination lastHop, Subscription subscription) {
		for (SubscriptionMessage subMsg : workingSubs.values()) {
			if (lastHop == null || subMsg.getLastHopID().equals(lastHop))
				if(subMsg.getSubscription().equalsPredicates(subscription))
					return true;
		}
		return false;
	}

	public boolean checkStateForSubscription(MessageDestination lastHop,
			Map<String, Subscription> subscriptions) {
		for (SubscriptionMessage subMsg : workingSubs.values()) {
			if (subMsg.getLastHopID().equals(lastHop)) {
				Subscription localSub = subMsg.getSubscription();
				for (Subscription sub : subscriptions.values()) {
					if (localSub.equalsPredicates(sub)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public String getBrokerId() {
		return brokerCore.getBrokerID();
	}
}

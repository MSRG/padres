/*
 * Created on Mar 28, 2010
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router;

/**
 * @author Chen
 *
 * Enforces advertisement covering in routing of subscriptions.  That is,
 * advertisements that are equal or covered by previously sent subscriptions
 * are dropped.  A SCOUT tree is used for each next hop to maintain the
 * respective latest advertisement covering set.
 * 
 * When an unadvertisement occurs, advertisements that become the new
 * covering advertisement set for that next hop will be forwarded
 */

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.router.scoutad.ScoutAD;
import ca.utoronto.msrg.padres.broker.router.scoutad.ScoutNodeAD;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;

public class AdvertisementFilter {

	public enum AdvCoveringType {
		ON, OFF;
	}

	public static final AdvCoveringType DEFAULT_COVERING_OPTION = AdvCoveringType.OFF;

	// Stores this broker's destination
	private MessageDestination brokerDest = null;

	// Stores the SCOUT tree for each next broker hop
	private Map<MessageDestination, ScoutAD> nextHopScoutMap = null;

	// Stores the reference to the matching engine's subscription map (subId ->
	// subMsg) This field is OPTIONAL, meaning it can be null, but the
	// reconstructed message will not have the same header values as the
	// original, such as the priority
	private final Map<String, AdvertisementMessage> advertisementIdToMsgMap;

	// Block list, like a firewall almost ;)
	private final Set<String> advIDBlockList;

	// true if subscription covering is turned on
	private final boolean advCoveringIsOn;

	// true if user specified lazy covering. false if user specified active
	// covering
	private final boolean isActiveCovering;

	private static Logger exceptionLogger = Logger.getLogger("Exception");

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Constructor
	 * 
	 * @param ourDest
	 */
	public AdvertisementFilter(AdvCoveringType coveringType, MessageDestination ourDest,
			Map<String, AdvertisementMessage> advMap) {
		// Determine if subscription covering is turned on first before
		// instantiating required variables
		// advCoveringIsOn = isAdvCoveringOn(props);
		//
		// String coveringOption = props.getProperty("padres.covering.advertisement",
		// DEFAULT_COVERING_OPTION);
		// if (coveringOption.equalsIgnoreCase(COVERING_OPTION_LAZY))
		// isActiveCovering = false;
		// else if (coveringOption.equalsIgnoreCase(COVERING_OPTION_ACTIVE))
		// isActiveCovering = true;
		// else
		// isActiveCovering = false; // no meaning

		advCoveringIsOn = true;
		isActiveCovering = true;

		if (advCoveringIsOn) {
			// though waste of memory, we have to initialize these "final"
			// variables
			brokerDest = ourDest;
			nextHopScoutMap = Collections.synchronizedMap(new HashMap<MessageDestination, ScoutAD>(
					5));
			// most brokers are not cluster-heads anyway, so 2 is fine
			advIDBlockList = Collections.synchronizedSet(new HashSet<String>());
			advertisementIdToMsgMap = advMap;
		} else {
			brokerDest = null;
			nextHopScoutMap = null;
			advIDBlockList = null;
			advertisementIdToMsgMap = null;
		}
	}

	/**
	 * Removes advertisements that were covered by previously sent out advertisements sent along the
	 * same next hop. Retains advertisements that need to be forwarded. Other types of messages are
	 * untouched. The resulting message set is in the passed-in messageSet variable
	 * 
	 * @param messageSet
	 */
	public int removeCoveredAdvertisements(Set<Message> messageSet) {
		// This is a benchmark variable that tells you the total number of non-unique scout nodes
		// traversed when inserting a new advertisement in each of the "next destination" scout
		// trees.
		int totalRelationTestsDone = 0;

		if (advCoveringIsOn) {
			// Corner case
			if (messageSet == null || messageSet.isEmpty())
				return 0;

			Set<AdvertisementMessage> subMsgSet = new HashSet<AdvertisementMessage>();

			// Insert all subscription messages to be routed into the scout map for each next hop's
			// SCOUT tree
			for (Message msg : messageSet) {
				// We don't filter advertisements because they are flooded
				if (msg.getClass() == AdvertisementMessage.class) {
					AdvertisementMessage advMsg = (AdvertisementMessage) msg;
					MessageDestination nextDest = advMsg.getNextHopID();

					// If this is the first time seeing this next hop, add a scout for it
					if (!nextHopScoutMap.containsKey(nextDest)) {
						nextHopScoutMap.put(nextDest, new ScoutAD());
					}
					ScoutAD scoutad = nextHopScoutMap.get(nextDest);
					synchronized (scoutad) {
						totalRelationTestsDone += scoutad.insert(advMsg);
					}
					// a new scout node is not created if it has the same subscription space as an
					// existing scout node
					subMsgSet.add(advMsg);
				}
			}

			// We will rebuild the subscription messages from the subscription stored in the scout
			// nodes and retain all other types of messages
			messageSet.removeAll(subMsgSet);

			subMsgSet.clear();
			subMsgSet = null;

			addUnsentRootAdvs(messageSet);
		}

		return totalRelationTestsDone;
	}

	/*
	 * This function will automatically add in subscriptions in the covering subscription set
	 */
	private void addUnsentRootAdvs(Set<Message> messageSet) {
		// Retrieve the subscription covering set for each next hop's scout tree and determine if
		// the subscriptions were forwarded before. If not (ie. flag == false), we should send it
		// out.
		Set<MessageDestination> nextHopSet = new HashSet<MessageDestination>(
				nextHopScoutMap.keySet());
		for (MessageDestination nextHopID : nextHopSet) {
			ScoutAD scoutad = nextHopScoutMap.get(nextHopID);

			// Protect only on the current scout tree that's worked on.
			synchronized (scoutad) {
				Set<ScoutNodeAD> coveringAdvSet = scoutad.coveringAdvertisementSet();

				if (coveringAdvSet == null || coveringAdvSet.isEmpty())
					continue;

				// check for duplicate unsub message with same unsubid
				Set<String> unsubIdSet = new HashSet<String>();
				for (ScoutNodeAD node : coveringAdvSet) {
					// only forward the subscription in CSS if it was not sent out before
					if (node.flag == false) {
						node.flag = true;
						messageSet.add(toAdvertisementMessage(node, nextHopID));

						// if active covering is turned on, then we need to
						// unsubscribe subs that were previously in the CSS but
						// now no longer are. This 'if' statement is nested
						// inside the other 'if' statement because a node in CSS
						// with its subscriptions sent already is guaranteed to
						// have children nodes whose subscriptions have already
						// been unsubscribed.
						if (isActiveCovering) {
							// just send an unsubscription for a child scout
							// node that have sent a subscription
							// in the past. Update its flag to false if we need
							// to unsub.
							Set<ScoutNodeAD> childrenNodeSet = node.childSet;
							for (ScoutNodeAD childNode : childrenNodeSet) {
								if (childNode.flag == true) {
									childNode.flag = false;

									// The following will add a unsub message
									// only if we haven't already added
									// an unsub message yet (handles the case of
									// tid subs with equal subId)
									String unsubId = childNode.getNextAdvertisementID();
									if (!unsubIdSet.contains(unsubId)) {
										messageSet.add(toUnadvertisementMessage(childNode,
												nextHopID));
										unsubIdSet.add(unsubId);
									}
								}
							}
							childrenNodeSet = null;
						}
					}
				}

				unsubIdSet.clear();
				unsubIdSet = null;
				coveringAdvSet = null;
			}
		}
		nextHopSet = null;
	}

	/*
	 * Builds a advertisement message given a advertisement object
	 */
	private AdvertisementMessage toAdvertisementMessage(ScoutNodeAD node,
			MessageDestination nextHopID) {

		AdvertisementMessage advMsg;
		String advId = node.getNextAdvertisementID();
		if (node.predicateMap.containsKey("tid")) {
			Predicate tidPredicate = node.predicateMap.get("tid");
			advId += "_" + (String) tidPredicate.getValue();
		}

		// First, try to fetch original sub from matching engine's map if
		// 1. the map is not null (i.e. this is not a junit test)
		// 2. map contains the subscription
		// 3. the value returned is not some tid mapping
		if (advertisementIdToMsgMap != null && advertisementIdToMsgMap.containsKey(advId)
				&& advertisementIdToMsgMap.get(advId).getClass() == AdvertisementMessage.class) {
			// don't want to alter the original copy
			advMsg = advertisementIdToMsgMap.get(advId).duplicate();
			advMsg.setLastHopID(brokerDest);
			advMsg.setMessageTime(new Date());

			// The fetch failed, construct from info in scout node
		} else {
			Advertisement adv = new Advertisement();
			adv.setPredicateMap(node.predicateMap);
			// the first subscription id from the set is fine.
			adv.setAdvID(advId);
			advMsg = new AdvertisementMessage(adv, adv.getAdvID(), brokerDest);
			// subMsg.getSizeInBytes();
		}

		advMsg.setNextHopID(nextHopID);
		// use the maximum priority
		advMsg.setPriority((short) Math.max(advMsg.getPriority(), 1));

		return advMsg;
	}

	/*
	 * Builds a unsubscription message given a subscription object
	 */
	private UnadvertisementMessage toUnadvertisementMessage(ScoutNodeAD node,
			MessageDestination nextHopID) {
		Unadvertisement unadv = new Unadvertisement(node.getNextAdvertisementID());
		// HACK: should get new message id from broker core
		UnadvertisementMessage unadvMsg = new UnadvertisementMessage(unadv, unadv.getAdvID()
				+ "-u_" + brokerDest.getBrokerId(), brokerDest);
		unadvMsg.setNextHopID(nextHopID);
		unadvMsg.setPriority((short) 1);
		return unadvMsg;
	}

	public static boolean isAdvCoveringOn(AdvCoveringType coveringOption) {
		if (coveringOption == AdvCoveringType.OFF) {
			return false;
		} else if (coveringOption == AdvCoveringType.ON) {
			return true;
		} else {
			exceptionLogger.warn("Unrecognized advertisement covering option: " + coveringOption
					+ ".  Using default value of " + DEFAULT_COVERING_OPTION);
			return false;
		}
	}
}
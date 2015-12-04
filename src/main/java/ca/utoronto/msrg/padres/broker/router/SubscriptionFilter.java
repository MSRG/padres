/*
 * Created on Mar 28, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router;

/**
 * @author Alex
 *
 * Enforces subscription covering in routing of subscriptions.  That is,
 * subscriptions that are equal or covered by previously sent subscriptions
 * are dropped.  A SCOUT tree is used for each next hop to maintain the
 * respective latest subscription covering set.
 * 
 * When an unsubscription occurs, subscriptions that become the new
 * covering subscription set for that next hop will be forwarded
 */

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.router.scout.Scout;
import ca.utoronto.msrg.padres.broker.router.scout.ScoutNode;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

public class SubscriptionFilter {

	public enum SubCoveringType {
		OFF, LAZY, ACTIVE;
	}

	public static final SubCoveringType DEFAULT_COVERING_OPTION = SubCoveringType.OFF;

	// Stores this broker's destination
	private MessageDestination brokerDest = null;

	// Stores the SCOUT tree for each next broker hop
	private Map<MessageDestination, Scout> nextHopScoutMap = null;

	// Stores the reference to the matching engine's subscription map (subId ->
	// subMsg) This field is OPTIONAL, meaning it can be null, but the
	// reconstructed message will not have the same header values as the
	// original, such as the priority
	private final Map<String, SubscriptionMessage> subscriptionIdToMsgMap;

	// Block list, like a firewall almost ;)
	private final Set<String> subIDBlockList;

	// true if subscription covering is turned on
	private final boolean subCoveringIsOn;

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
	public SubscriptionFilter(SubCoveringType coverType, MessageDestination ourDest,
			Map<String, SubscriptionMessage> subMap) {
		// Determine if subscription covering is turned on first before
		// instantiating required variables
		subCoveringIsOn = isSubCoveringOn(coverType);
		isActiveCovering = coverType == SubCoveringType.ACTIVE;

		if (subCoveringIsOn) {
			// though waste of memory, we have to initialize these "final"
			// variables
			brokerDest = ourDest;
			nextHopScoutMap = Collections.synchronizedMap(new HashMap<MessageDestination, Scout>(5));
			// most brokers are not cluster-heads anyway, so 2 is fine
			subIDBlockList = Collections.synchronizedSet(new HashSet<String>());
			subscriptionIdToMsgMap = subMap;
		} else {
			brokerDest = null;
			nextHopScoutMap = null;
			subIDBlockList = null;
			subscriptionIdToMsgMap = null;
		}
	}

	/**
	 * Removes subscriptions that were covered by previously sent out subscriptions sent along the
	 * same next hop. Retains subscriptions that need to be forwarded. Other types of messages are
	 * untouched. The resulting message set is in the passed-in messageSet variable
	 * 
	 * @param messageSet
	 */
	public int removeCoveredSubscriptions(Set<Message> messageSet) {
		// This is a benchmark variable that tells you the total number of non-unique scout nodes
		// traversed when inserting a new subscription in each of the "next destination" scout
		// trees.
		int totalRelationTestsDone = 0;

		if (subCoveringIsOn) {
			// Corner case
			if (messageSet == null || messageSet.isEmpty())
				return 0;

			// do not insert temporarily subscribed subscriptions into the tree.
			// It will only mess things up when future advs/unsubs come in the future.
			applyBlockList(messageSet);

			Set<SubscriptionMessage> subMsgSet = new HashSet<SubscriptionMessage>();

			// Insert all subscription messages to be routed into the scout map for each next hop's
			// SCOUT tree
			for (Message msg : messageSet) {
				// We don't filter advertisements because they are flooded
				if (msg.getClass() == SubscriptionMessage.class) {
					SubscriptionMessage subMsg = (SubscriptionMessage) msg;
					MessageDestination nextDest = subMsg.getNextHopID();

					// If this is the first time seeing this next hop, add a scout for it
					if (!nextHopScoutMap.containsKey(nextDest)) {
						nextHopScoutMap.put(nextDest, new Scout());
					}
					Scout scout = nextHopScoutMap.get(nextDest);
					synchronized (scout) {
						totalRelationTestsDone += scout.insert(subMsg);
					}
					// a new scout node is not created if it has the same subscription space as an
					// existing scout node
					subMsgSet.add(subMsg);
				}
			}

			// We will rebuild the subscription messages from the subscription stored in the scout
			// nodes and retain all other types of messages
			messageSet.removeAll(subMsgSet);

			subMsgSet.clear();
			subMsgSet = null;

			addUnsentRootSubs(messageSet);
		}

		return totalRelationTestsDone;
	}

	/*
	 * This function will automatically add in subscriptions in the covering subscription set
	 */
	private void addUnsentRootSubs(Set<Message> messageSet) {
		// Retrieve the subscription covering set for each next hop's scout tree and determine if
		// the subscriptions were forwarded before. If not (ie. flag == false), we should send it
		// out.
		Set<MessageDestination> nextHopSet = new HashSet<MessageDestination>(
				nextHopScoutMap.keySet());
		for (MessageDestination nextHopID : nextHopSet) {
			Scout scout = nextHopScoutMap.get(nextHopID);

			// Protect only on the current scout tree that's worked on.
			synchronized (scout) {
				Set<ScoutNode> coveringSubSet = scout.coveringSubscriptionSet();

				if (coveringSubSet == null || coveringSubSet.isEmpty())
					continue;

				// check for duplicate unsub message with same unsubid
				Set<String> unsubIdSet = new HashSet<String>();
				for (ScoutNode node : coveringSubSet) {
					// only forward the subscription in CSS if it was not sent out before
					if (node.flag == false) {
						node.flag = true;
						messageSet.add(toSubscriptionMessage(node, nextHopID));

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
							Set<ScoutNode> childrenNodeSet = node.childSet;
							for (ScoutNode childNode : childrenNodeSet) {
								if (childNode.flag == true) {
									childNode.flag = false;

									// The following will add a unsub message
									// only if we haven't already added
									// an unsub message yet (handles the case of
									// tid subs with equal subId)
									String unsubId = childNode.getNextSubscriptionID();
									if (!unsubIdSet.contains(unsubId)) {
										messageSet.add(toUnsubscriptionMessage(childNode, nextHopID));
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
				coveringSubSet = null;
			}
		}
		nextHopSet = null;
	}

	/*
	 * Builds a subscription message given a subscription object
	 */
	private SubscriptionMessage toSubscriptionMessage(ScoutNode node, MessageDestination nextHopID) {
		SubscriptionMessage subMsg;
		String subId = node.getNextSubscriptionID();
		if (node.predicateMap.containsKey("tid")) {
			Predicate tidPredicate = node.predicateMap.get("tid");
			subId += "_" + (String) tidPredicate.getValue();
		}

		// First, try to fetch original sub from matching engine's map if
		// 1. the map is not null (i.e. this is not a junit test)
		// 2. map contains the subscription
		// 3. the value returned is not some tid mapping
		if (subscriptionIdToMsgMap != null && subscriptionIdToMsgMap.containsKey(subId)
				&& subscriptionIdToMsgMap.get(subId).getClass() == SubscriptionMessage.class) {
			// don't want to alter the original copy
			subMsg = subscriptionIdToMsgMap.get(subId).duplicate();
			subMsg.setLastHopID(brokerDest);
			subMsg.setMessageTime(new Date());

			// The fetch failed, construct from info in scout node
		} else {
			Subscription sub = new Subscription();
			sub.setPredicateMap(node.predicateMap);
			// the first subscription id from the set is fine.
			sub.setSubscriptionID(subId);
			subMsg = new SubscriptionMessage(sub, sub.getSubscriptionID(), brokerDest);
			// subMsg.getSizeInBytes();
		}

		subMsg.setNextHopID(nextHopID);
		// use the maximum priority
		subMsg.setPriority((short) Math.max(subMsg.getPriority(), 1));

		return subMsg;
	}

	/*
	 * Builds a unsubscription message given a subscription object
	 */
	private UnsubscriptionMessage toUnsubscriptionMessage(ScoutNode node,
			MessageDestination nextHopID) {
		Unsubscription unsub = new Unsubscription(node.getNextSubscriptionID());
		// HACK: should get new message id from broker core
		UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub, unsub.getSubID() + "-u_"
				+ brokerDest.getBrokerId(), brokerDest);
		unsubMsg.setNextHopID(nextHopID);
		unsubMsg.setPriority((short) 1);
		return unsubMsg;
	}

	/**
	 * Removes the subscription with the specified unsubscription id. Runs
	 * removeCoveredSubscriptions() to add subscriptions that moved up to become part of the
	 * covering subscription set (ie. subscriptions that were not forwarded before because the
	 * subscription that just got removed was covering it.
	 * 
	 * @param messageID
	 * @param messageSet
	 */
	public void removeCoveredUnsubscriptions(Set<Message> messageSet) {
		if (subCoveringIsOn) {
			// do not insert temporarily subscribed subscriptions into the tree.
			// It will only mess things up when future advs/unsubs come in the future.
			applyBlockList(messageSet);

			Set<Message> msgSetToRemove = new HashSet<Message>();
			for (Message msg : messageSet) {
				if (msg.getClass() == UnsubscriptionMessage.class) {
					MessageDestination nextHopID = ((UnsubscriptionMessage) msg).getNextHopID();
					String unsubID = ((UnsubscriptionMessage) msg).getUnsubscription().getSubID();

					// Retrieve the scout node for that subscription along the
					// respective nextHopID
					Scout scout = (Scout) nextHopScoutMap.get(nextHopID);

					// Protect this particular scout tree only
					synchronized (scout) {
						Set<ScoutNode> nodeSet = scout.getNodes(unsubID);

						// The subscription does not exist because it was
						// removed by the load balancer
						if (nodeSet == null || nodeSet.isEmpty()) {
							msgSetToRemove.add(msg);
							// skip the scout.remove, since it's removed already
							continue;
						}

						// In cycles, there could be multiple subs with the same
						// unsubId/subId
						for (ScoutNode node : nodeSet) {
							// Do not forward this unsubscription if:
							// 1. if subscription does not exist (unlikely) OR
							// 2. no subscription was forwarded in the past for this predicate set
							// 3. a subscription with the same predicate set was forwarded before,
							// but it was not this subscription that was forwarded (ie. some other
							// subscription with a different subID).
							if (node == null
									|| (node != null && node.flag == false)
									|| (node != null && node.flag == true && !node.getNextSubscriptionID().equals(
											unsubID))) {
								msgSetToRemove.add(msg);
							} else { // node != null && node.flag == true &&
								// unsubID was the first in the subID
								// set
								// before, the flag was true, now update it to
								// false
								node.flag = false;
							}
						}

						// we can now safely remove the unsub's subID from the
						// scout node
						scout.remove(unsubID);
					}
				}
			}
			// Remove unneeded unsubscription messages from the message set
			messageSet.removeAll(msgSetToRemove);

			msgSetToRemove.clear();
			msgSetToRemove = null;

			// This function will automatically add in subscriptions in the
			// covering subscription
			// set for each next hops that was not forwarded before this
			// subscription got deleted
			addUnsentRootSubs(messageSet);
		}
	}

	/**
	 * For debugging use only
	 * 
	 */
	public void showDestinationScoutTrees() {
		if (subCoveringIsOn) {
			for (MessageDestination dest : nextHopScoutMap.keySet()) {
				// System.out.println("Destination " + dest + ":");
				Scout scout = (Scout) nextHopScoutMap.get(dest);
				synchronized (scout) {
					scout.showTree();
				}
				// System.out.println();
			}
			// System.out.println();
		}
	}

	/*
	 * Block temporarily subscribed subscriptions and unsubscriptions. Also remove subscriptions
	 * with destination to this broker.
	 */
	private void applyBlockList(Set<Message> messageSet) {
		Set<Message> removeSet = new HashSet<Message>();
		String subID = null;

		for (Message msg : messageSet) {
			if (msg.getType().equals(MessageType.SUBSCRIPTION)) {
				// Retrieve the subscription id
				subID = ((SubscriptionMessage) msg).getSubscription().getSubscriptionID();
				// our last resort to getting the subscription id
				if (subID == null || subID == "") {
					subID = msg.getMessageID();
				}

				// add to to-remove list if sub ID is found in block list
				if (subIDBlockList.contains(subID)) {
					messagePathLogger.info("Sub message found in block list!  Removing " + msg);
					removeSet.add(msg);
				}

			} else if (msg.getType().equals(MessageType.UNSUBSCRIPTION)) {
				// Recall, unsubID of a subscription is equal to its subID
				subID = ((UnsubscriptionMessage) msg).getUnsubscription().getSubID();

				// add to to-remove list if sub ID is found in block list
				if (subIDBlockList.contains(subID)) {
					messagePathLogger.info("Unsub message found in block list!  Removing " + msg);
					removeSet.add(msg);
				}
			}

			// Remove messages with nexthop to this broker
			if (msg.getNextHopID().equals(brokerDest)) {
				removeSet.add(msg);
			}
		}

		messageSet.removeAll(removeSet);

		// Clean up
		subID = null;
		removeSet.clear();
		removeSet = null;
	}

	/**
	 * Adds the given set of subscription ids to the block list
	 * 
	 * @param subIDSet
	 */
	public void addToBlockList(String subID) {
		if (subCoveringIsOn)
			subIDBlockList.add(subID);
	}

	public void removeFromBlockList(String subID) {
		if (subCoveringIsOn)
			subIDBlockList.remove(subID);
	}

	public void clearBlockList() {
		if (subCoveringIsOn)
			subIDBlockList.clear();
	}

	public static boolean isSubCoveringOn(SubCoveringType coveringOption) {
		if (coveringOption == SubCoveringType.OFF) {
			return false;
		} else if (coveringOption == SubCoveringType.LAZY
				|| coveringOption == SubCoveringType.ACTIVE) {
			return true;
		} else {
			exceptionLogger.warn("Unrecognized subscription covering option: " + coveringOption
					+ ".  Using default value of " + DEFAULT_COVERING_OPTION);
			return false;
		}
	}
}
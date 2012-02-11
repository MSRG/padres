package ca.utoronto.msrg.padres.broker.router.matching.rete;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.matching.Matcher;
import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;
import ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork.ReteNetwork;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

/**
 * This is the Rete matcher engine. It creates and maintains seperate Rete networks for
 * advertisements and subscriptions. It also creates and maintains the working memores for
 * advertisements, subscriptions, and publications.
 * 
 */
public class ReteMatcher implements Matcher {

	protected static Logger reteMatcherLogger = Logger.getLogger(ReteMatcher.class);

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	protected BrokerCore brokerCore;

	protected Router router;

	protected ReteNetwork subRete;

	protected ReteNetwork advRete;

	/**
	 * Creates a Rete matcher engine.
	 */
	public ReteMatcher(BrokerCore broker, Router router) {
		subRete = new ReteNetwork(ReteNetwork.NWType.SUB_TREE, router);
		advRete = new ReteNetwork(ReteNetwork.NWType.ADV_TREE, router);
		brokerCore = broker;
		this.router = router;
	}

	public void printMatches() {
		System.out.print("Sub Rete: ");
		subRete.printMatchingPubSubs();
		System.out.print("Adv Rete: ");
		advRete.printMatchingPubSubs();
	}

	public void logMatches() {
		reteMatcherLogger.info("Matching Memory in Sub Rete: " + subRete.getMatchingPubSubs());
		reteMatcherLogger.info("Matching Memory in Adv Rete: " + advRete.getMatchingPubSubs());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.AdvertisementMessage)
	 */
	public Set<String> add(AdvertisementMessage advMsg) throws MatcherException {
		String advID = advMsg.getMessageID();
		Advertisement adv = advMsg.getAdvertisement();
		// Add adv to this broker.
		advRete.addAdv(adv, advID);
		// Return all matching subs.
		return subRete.getMatches(adv);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.SubscriptionMessage)
	 */
	public Set<String> add(SubscriptionMessage subMsg) throws MatcherException {
		String subID = subMsg.getMessageID();
		Subscription sub = subMsg.getSubscription();
		// add to the subscription tree
		subRete.addSub(sub, subID);
		// Return matching advs.
		return advRete.getMatches(sub);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.CompositeSubscriptionMessage)
	 */
	public Map<SubscriptionMessage, Set<String>> add(CompositeSubscriptionMessage csMsg)
			throws MatcherException {
		Map<SubscriptionMessage, Set<String>> results = new HashMap<SubscriptionMessage, Set<String>>();
		String csMsgID = csMsg.getMessageID();

		// get the composing atomic subscriptions and add them to the
		// subscription matcher network
		CompositeSubscription cs = csMsg.getSubscription();
		Map<String, Subscription> atomicSubs = cs.getSubscriptionMap();
		for (String subID : atomicSubs.keySet()) {
			String aSubMsgID = csMsgID + "-" + subID;
			Subscription aSub = atomicSubs.get(subID);
			SubscriptionMessage aSubMsg = new SubscriptionMessage(aSub, aSubMsgID,
					csMsg.getLastHopID());
			subRete.addSub(aSub, aSubMsgID);
			// collect matches for the atomic subscription from the
			// advertisement matcher
			Set<String> advIDs = advRete.getMatches(aSub);
			if (advIDs.size() > 0) {
				results.put(aSubMsg, advIDs);
			}
		}

		// add the composite subscriptions to the subscription matcher
		subRete.addCompositeSub(cs, csMsgID);

		return results;
	}

	public Set<String> getMatchingAdvs(PublicationMessage pubMsg) {
		Map<Publication, Set<String>> matchedPubAdvs = advRete.getMatches(pubMsg.getPublication());
		return matchedPubAdvs.get(pubMsg.getPublication());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.PublicationMessage)
	 */
	public Map<PublicationMessage, Set<String>> getMatchingSubs(PublicationMessage pubMsg) {
		Map<Publication, Set<String>> pubToSubIDMap = subRete.getMatches(pubMsg.getPublication());
		return copyResultMap(pubToSubIDMap);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.UnsubscriptionMessage)
	 */
	public Set<String> add(UnsubscriptionMessage msg) {
		String sid = msg.getUnsubscription().getSubID();
		subRete.removeSub(sid);

		Set<String> advIDs = new HashSet<String>();
		advIDs.addAll(advRete.getMatches(router.getSubscriptionMessage(sid).getSubscription()));
		return advIDs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.UncompositesubscriptionMessage )
	 */
	public Map<UnsubscriptionMessage, Set<String>> add(UncompositesubscriptionMessage msg) {
		HashMap<UnsubscriptionMessage, Set<String>> results = new HashMap<UnsubscriptionMessage, Set<String>>();
		// check whether the composite subscription exists
		String csID = msg.getUncompositesubscription().getSubID();
		CompositeSubscriptionMessage csMsg = router.getCompositeSubscription(csID);
		// remove all the composing atomic subscriptions.
		Map<String, Subscription> subMap = csMsg.getSubscription().getSubscriptionMap();
		Iterator<String> i = subMap.keySet().iterator();
		while (i.hasNext()) {
			String aSid = i.next();
			String aSubMsgID = csID + "-" + aSid;
			router.getRoutedSubs().remove(aSubMsgID);
			Subscription tmpSub = subMap.get(aSid);
			// TODO: may be we have to check that there is no subscription for
			// the atomic subscription.
			subRete.removeSub(aSubMsgID);
			UnsubscriptionMessage tempMessage = new UnsubscriptionMessage(new Unsubscription(csID
					+ "-" + aSid), csID + "-" + aSid, msg.getLastHopID());
			results.put(tempMessage, advRete.getMatches(tmpSub));
		}
		subRete.removeSub(csID);

		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#add(message.UnadvertisementMessage)
	 */
	public void add(UnadvertisementMessage uAdvMsg) {
		// we can remove only that exists
		String aid = uAdvMsg.getUnadvertisement().getAdvID();
		advRete.removeAdv(aid);

		// Find the subs that intersect this adv.
		AdvertisementMessage advMsg = router.getAdvertisement(aid);
		Set<String> subIDSet = subRete.getMatches(advMsg.getAdvertisement());

		// Find which of these subs still intersect any other advertisement.
		HashSet<String> stillMatchingSubs = new HashSet<String>();
		for (String subMsgID : subIDSet) {
			SubscriptionMessage subMsg = router.getSubscriptionMessage(subMsgID);
			Subscription s = subMsg.getSubscription();
			if (advRete.getMatches(s).size() > 0)
				stillMatchingSubs.add(subMsgID);
		}

		// Remove those subs that DON'T intersect any other advertisements.
		subIDSet.removeAll(stillMatchingSubs);
		for (String subMsgID : subIDSet)
			subRete.removeSub(subMsgID);
	}

	/**
	 * Converts a map of publication -&gt; matchSet into publicationMessage -&gt; matchSet
	 * 
	 * @param results
	 *            the map to be copied
	 * @return a copy of the map <code>results</code>
	 */
	private Map<PublicationMessage, Set<String>> copyResultMap(Map<Publication, Set<String>> results) {
		Map<PublicationMessage, Set<String>> tmpResult = new HashMap<PublicationMessage, Set<String>>();
		for (Publication pub : results.keySet()) {
			PublicationMessage pubMsg = router.getPublicationMessage(pub.getPubID());
			tmpResult.put(pubMsg, results.get(pub));
		}
		return tmpResult;
	}

	@Override
	public boolean isPartialMatch() {
		return subRete.isPartialMatch();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.routerFactory.Matcher#flushPRTByClassName(java.lang.String)
	 */
	public void flushPRTByClassName(String classname) {
	}

	public String toString() {
		return "ReteMatcher in " + brokerCore.getBrokerID();
	}
}

package ca.utoronto.msrg.padres.broker.router.matching;

import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UncompositesubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

public interface Matcher {

	/**
	 * Add new advertisement message to matching engine. No matter if the matching engine supports
	 * the composite subscriptions or not, this method will return set of
	 * subscriptionMessage/compositeSubscriptionMessage matching this advertisement message.
	 * 
	 * @param m
	 * @return set of subscriptionMessage/compositeSubscriptionMessage matching this advertisement
	 *         message
	 * @throws MatcherException
	 */
	public Set<String> add(AdvertisementMessage m) throws DuplicateMsgFoundException,
			MatcherException;

	/**
	 * Add new subscription message to matching engine. Since the subMsg is an atomic subscription
	 * message, the method only returns set of advIDs, where the advertisement messages match this
	 * subscription message. At the same time, if the ME contains some composite subscription, the
	 * composite subscription itself may keep some partial results (old publications are matched),
	 * so, the method has to deal with the new subscription does not match old publication.
	 * 
	 * @param m
	 * @return set
	 * @throws MatcherException
	 */
	public Set<String> add(SubscriptionMessage m) throws MatcherException;

	/**
	 * Add new composite subscription message to matching engine. This method will return the
	 * map<subscriptionMessage/compositeSubscriptionMessage, set of advIDs>. It is because that the
	 * composite subscription may be splited into atomic subscription or composite subscription
	 * message, so it will find where to forward each part in the composite subscription.
	 * 
	 * @param m
	 * @return map
	 * @throws MatcherException
	 */
	public Map<SubscriptionMessage, Set<String>> add(CompositeSubscriptionMessage m)
			throws MatcherException;

	public Set<String> getMatchingAdvs(PublicationMessage pubMsg);

	/**
	 * Add new publication message to matching engine. No matter if the matching engine supports the
	 * composite subscription or not, this method will return the map<publicationMessage, set of
	 * subIDs>. If the matching engine has some composite subscriptions, the new publicationMessage
	 * may result in some composite subscription is fully matched. So, not only the new
	 * publicationMessage need to be routed out, some old publicationMessage (partially matched
	 * composite subscription before) still need to be routed. It will throw
	 * PubMsgNotConformedException if the publication message is not conformed by any
	 * advertisements.
	 * 
	 * @param m
	 * @return map
	 */
	public Map<PublicationMessage, Set<String>> getMatchingSubs(PublicationMessage m);

	/**
	 * Add an unsubscription message to matching engine. This method only returns set of advIDs,
	 * where these advertisement messges matching the corresponding subscription message. It will
	 * throw SubMsgNotFoundException if the unsubscription message wants to unsubscribe an invalid
	 * subscriptionMsg.
	 * 
	 * @param m
	 * @return set
	 */
	public Set<String> add(UnsubscriptionMessage m);

	/**
	 * Add an uncompositesubscription message to matching engine. This method will return the
	 * map<UnsubscriptionMessage/UncompositesubscriptionMessage, set of advIDs>. It is because that
	 * the composite subscription may be splited into atomic subscription or composite subscription
	 * message. So we need to find out where to forward each part of the original composite
	 * subscription message. It will return CSMsgNotFoundException if the
	 * uncompositesubscriptionMessage wants to unsubscribe an invalid compositeSubscription message.
	 * 
	 * @param m
	 * @return map
	 * 
	 */
	public Map<UnsubscriptionMessage, Set<String>> add(UncompositesubscriptionMessage m);

	/**
	 * Add an unadvertisement message to matching engine. This method need to find all
	 * subscriptionMessages/compositesubscriptionMessage that only match this advertisement message,
	 * and remove these subscriptionMessages/compositeSubscriptionMessage properly. It will throw
	 * AdvMsgNotFoundException if the unadvertisement message wants to unadvertise an invalid
	 * advertisement message.
	 * 
	 * @param m
	 * @throws AdvMsgNotFoundException
	 */
	public void add(UnadvertisementMessage m) throws AdvMsgNotFoundException,
			DuplicateMsgFoundException;

	/**
	 * To check whether there is a partial match for the last publication sent through the matcher
	 * 
	 * @return true if there is a partial match for the last publication, false otherwise
	 */
	public boolean isPartialMatch();

	public void flushPRTByClassName(String classname);

}

package ca.utoronto.msrg.padres.broker.router;

import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

public interface Forwarder {

	/**
	 * Checking forward condition for each publication message in the map.
	 * 
	 * @param pubMsgsToLasthops
	 *            : map of <publicationMessage, set of subIDs>,
	 * @return set of publication messages to route out
	 */
	public Set<PublicationMessage> forwardPubMsgs(
			Map<PublicationMessage, Set<String>> pubMsgsToSubIDs);

	/**
	 * Checking forward condition for the subscription message.
	 * 
	 * @param m
	 *            : subscriptionMessage, advIDs: set of advertisementMessages
	 *            that matching m
	 * @return set of subscription messages to route out
	 */
	public Set<SubscriptionMessage> forwardSubMsg(SubscriptionMessage m,
			Set<AdvertisementMessage> advMsgs);

	/**
	 * Checking forward condition for each
	 * subscriptionMessage/compositeSubscriptionMessage. in the map.
	 * 
	 * @param subMsgsToAdvMsgs
	 *            : map of <subscriptionMessage/compositeSubscriptionMessage,
	 *            set of advIDs>,
	 * @return set of subscriptionMessage/compositeSubscriptionMessage to route
	 *         out
	 */
	public Set<SubscriptionMessage> forwardSubMsgs(
			Map<SubscriptionMessage, Set<AdvertisementMessage>> subMsgsToAdvMsgs);

	/**
	 * Checking forward condition for each
	 * subscriptionMessage/compositeSubscriptionMessage.
	 * 
	 * @param m
	 *            : the advertisementMessage, subMsgs set of
	 *            subscriptionMessages/compositeSubscriptionMessage matching m
	 * @return set of subscription messages to route out
	 */
	public Set<SubscriptionMessage> forwardSubMsgs(AdvertisementMessage m,
			Set<SubscriptionMessage> subMsgs);

	/**
	 * Broadcast advertisement message
	 * 
	 * @param m
	 *            advertisementMessage,
	 * @return set of advertisement messages to route out
	 */
	public Set<AdvertisementMessage> broadcastAdvMsg(AdvertisementMessage m);

	/**
	 * Checking forward condition for the unsubscription message.
	 * 
	 * @param m
	 *            UnsubscriptionMessage, advIDs set of advertisementMessage that
	 *            matching the corresponding subscriptionMsg
	 * @return set of unsubscription messages to route out
	 */
	public Set<UnsubscriptionMessage> forwardUnsubMsg(UnsubscriptionMessage m, Set<String> advIDs);

	/**
	 * Checking forward condition for the each
	 * unsubscriptionMessage/uncompositeSubscriptionMessage. in the map.
	 * 
	 * @param unsubMsgsToAdvIDs
	 *            : map of
	 *            <unsubscriptionMessage/uncompositeSubscriptionMessage, set of
	 *            advIDs>,
	 * @return set of unsubscriptionMessage/uncompositeSubscriptionMessage to
	 *         route out
	 */
	public Set<UnsubscriptionMessage> forwardUnsubMsgs(
			Map<UnsubscriptionMessage, Set<String>> unsubMsgsToAdvIDs);

	/**
	 * Broadcast unadvertisement message.
	 * 
	 * @param m
	 *            unadvertisementMessage,
	 * @return set of unadvertisement messages to route out
	 */
	public Set<UnadvertisementMessage> broadcastUnadvMsg(UnadvertisementMessage m);

}

package ca.utoronto.msrg.padres.broker.router;

import java.util.Set;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.SenderTagger;
import ca.utoronto.msrg.padres.broker.order.TotalOrderEngine;
import ca.utoronto.msrg.padres.broker.topk.TopkEngine;
import ca.utoronto.msrg.padres.broker.topk.count.DesynchronizedChunkTopkEngine;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class PostProcessorImpl implements PostProcessor {

	private BrokerCore brokerCore;

	// To enforce Siena subscription covering routing
	private final boolean subCoveringIsOn;

	private SubscriptionFilter subFilter;

	private final boolean advCoveringIsOn;

	private AdvertisementFilter advFilter;

	private final boolean totalOrderIsOn;

	private TotalOrderEngine totalOrderEngine;

	private final boolean topkIsOn;
	
	private TopkEngine topkEngine;

	public PostProcessorImpl(BrokerCore broker) {
		brokerCore = broker;
		// Covering-related code
		subCoveringIsOn = SubscriptionFilter.isSubCoveringOn(brokerCore.getBrokerConfig().getSubCovering());
		advCoveringIsOn = AdvertisementFilter.isAdvCoveringOn(brokerCore.getBrokerConfig().getAdvCovering());
		// Total Order
		totalOrderIsOn = brokerCore.getBrokerConfig().isTotalOrder() && !(brokerCore.isCycle());
		topkIsOn = brokerCore.getBrokerConfig().isTopk();
	}

	public void initialize() {
		subFilter = subCoveringIsOn ? new SubscriptionFilter(
				brokerCore.getBrokerConfig().getSubCovering(), brokerCore.getBrokerDestination(),
				brokerCore.getRouter().getSubscriptions()) : null;
		advFilter = advCoveringIsOn ? new AdvertisementFilter(
				brokerCore.getBrokerConfig().getAdvCovering(), brokerCore.getBrokerDestination(),
				brokerCore.getRouter().getAdvertisements()) : null;
		totalOrderEngine = totalOrderIsOn ? new TotalOrderEngine(brokerCore.getRouter()) : null;
		topkEngine = topkIsOn ? new DesynchronizedChunkTopkEngine(brokerCore.getRouter(), brokerCore.getBrokerConfig().getTopk()) : null;
	}

	public void postprocess(Message msg, MessageType type, Set<Message> messageSet) {
		if (type.equals(MessageType.PUBLICATION)) {
			if (totalOrderIsOn) {
				totalOrderEngine.processMessage((PublicationMessage) msg, messageSet);
			}
			if(topkIsOn){
				topkEngine.processMessage((PublicationMessage) msg, messageSet);
			}
		} else if (type.equals(MessageType.SUBSCRIPTION)) {
			if (subCoveringIsOn) {
				// Do not forward subscriptions that were covered by previously sent out
				// subscriptions
				subFilter.removeCoveredSubscriptions(messageSet);
			}
			if (totalOrderIsOn || topkIsOn)
				SenderTagger.processMessage((SubscriptionMessage) msg);
			if (topkIsOn)
				topkEngine.processMessage((SubscriptionMessage) msg);
		} else if (type.equals(MessageType.COMPOSITESUBSCRIPTION)) {

		} else if (type.equals(MessageType.ADVERTISEMENT)) {
			if (advCoveringIsOn) {
				// System.out.println(messageSet.size());
				advFilter.removeCoveredAdvertisements(messageSet);
			}
			if (subCoveringIsOn) {
				// Do not forward subscriptions that were covered by previously sent out
				// subscriptions Advertisement messages are untouched
				subFilter.removeCoveredSubscriptions(messageSet);
			}
		} else if (type.equals(MessageType.UNSUBSCRIPTION)) {
			if (subCoveringIsOn) {
				// We may need to forward previously suppressed subscriptions that were not part of
				// the subscription covering set for a particular next hop.
				subFilter.removeCoveredUnsubscriptions(messageSet);
			}
		} else if (type.equals(MessageType.UNCOMPOSITESUBSCRIPTION)) {

		} else if (type.equals(MessageType.UNADVERTISEMENT)) {

		} else if (type.equals(MessageType.UNDEFINED)) {

		} else {

		}
	}

}

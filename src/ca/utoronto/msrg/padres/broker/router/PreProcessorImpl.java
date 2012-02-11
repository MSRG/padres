package ca.utoronto.msrg.padres.broker.router;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.monitor.SystemMonitor;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class PreProcessorImpl implements PreProcessor {

	protected BrokerCore brokerCore;

	protected Logger msgPathLog = Logger.getLogger("MessagePath");

	// Enables/disables tracing of all publications by class name
	protected boolean traceAllPublicationsEnabled = false;
	
	protected Set<String> toTrace;

	public PreProcessorImpl(BrokerCore broker) {
		brokerCore = broker;
		traceAllPublicationsEnabled = broker.getBrokerConfig().isMsgTrace();
		toTrace = new HashSet<String>();
	}

	public void preprocess(Message msg) {
		msgPathLog.info(brokerCore.getBrokerID() + ": " + msg);
		MessageType type = msg.getType();

		if (type.equals(MessageType.PUBLICATION)) {
			process((PublicationMessage) msg);
		} else if (type.equals(MessageType.SUBSCRIPTION)) {
			process((SubscriptionMessage) msg);
		} else if (type.equals(MessageType.COMPOSITESUBSCRIPTION)) {
			process((CompositeSubscriptionMessage) msg);
		} else if (type.equals(MessageType.ADVERTISEMENT)) {
			process((AdvertisementMessage) msg);
		} else if (type.equals(MessageType.UNSUBSCRIPTION)) {

		} else if (type.equals(MessageType.UNCOMPOSITESUBSCRIPTION)) {

		} else if (type.equals(MessageType.UNADVERTISEMENT)) {

		} else if (type.equals(MessageType.UNDEFINED)) {

		} else {

		}
	}

	public void process(AdvertisementMessage advMsg) {
		if (brokerCore.isCycle()) {
			// attach the TID to advertisement message
			Advertisement adv = advMsg.getAdvertisement();
			String advMessageID = advMsg.getMessageID();
			if (adv.getPredicateMap().containsKey("tid")) {
				// the advertisement has been already attached with a TID attribute
				// check if the incoming advertisement is duplicate
			} else {
				adv.addPredicate("tid", new Predicate("eq", advMessageID));
			}
		}
	}

	public void process(SubscriptionMessage subMsg) {
		if(subMsg.getSubscription().getClassVal().equals(SystemMonitor.TRACEROUTE_MESSAGE_KEY)){
			toTrace.add((String)(subMsg.getSubscription().getPredicateMap().get("TRACEROUTE_ID").getValue()));
		}
		if (brokerCore.isCycle()) {
			Subscription sub = subMsg.getSubscription();
			// attach TID predicate to subscription message, if missing
			if (!sub.getPredicateMap().containsKey("tid")) {
				sub.addPredicate("tid", new Predicate("eq", "$S$Tid"));
			}
		}
	}

	public void process(CompositeSubscriptionMessage csMsg) {
		if (brokerCore.isCycle()) {
			CompositeSubscription compositeSub = csMsg.getSubscription();
			Map<String, Subscription> atomicSubs = compositeSub.getSubscriptionMap();
			int aSubCount = 0;
			for (Subscription aSub : atomicSubs.values()) {
				// attach TID predicates to subscription messages, if missing
				if (!aSub.getPredicateMap().containsKey("tid")) {
					String tidVarName = String.format("$S$Tid-%03d", aSubCount++);
					aSub.addPredicate("tid", new Predicate("eq", tidVarName));
				}
			}
		}
	}

	public void process(PublicationMessage pubMsg) {
		Publication pub = pubMsg.getPublication();
		Map<String, Serializable> attrPairMap = pub.getPairMap();
		if (brokerCore.isCycle()) {
			// attach the TID to publication message
			if (!attrPairMap.containsKey("tid")) {
				pub.addPair("tid", "$S$Tid");
			}
		}
		// Handling TraceRoute features for the monitor
		if (pubMsg.getTraceRouteID().equals("dummy")) {
			String traceID = null;
			String msgClass = attrPairMap.get("class").toString();
			if (msgClass.equalsIgnoreCase("Trigger") || msgClass.equalsIgnoreCase("JOBSTATUS")) {
				traceID = (String) attrPairMap.get("applname");
				traceID = traceID.concat((String) attrPairMap.get("GID"));
			} else if (msgClass.equalsIgnoreCase("AGENT_CTL")) {
				String content = attrPairMap.get("content").toString();
				int index = content.indexOf("applname");
				if (content.substring(index + 8, index + 9).equals(",")) {
					int end = content.indexOf("]", index);
					traceID = content.substring(index + 12, end);
				} else if (content.substring(index + 8, index + 9).equals(":")) {
					int end = content.indexOf(";", index);
					traceID = content.substring(index + 9, end);
				}
				traceID = traceID.concat("_DEPLOY");
				msgPathLog.debug("The trace publication message: " + pubMsg.toString());
			} else if (traceAllPublicationsEnabled || toTrace.contains(msgClass)) {
				traceID = msgClass;
				if (pubMsg.getLastHopID().isBroker() || pubMsg.getLastHopID().isDB()) {
					// message is from a broker.
					pubMsg.setPrevClientID("dummy");
				} else {
					// message is from a client.
					pubMsg.setPrevClientID(pubMsg.getLastHopID().getDestinationID());
				}
			}
			
			if (traceID != null) {
				pubMsg.setTraceRouteID(traceID);
				pubMsg.setPrevBrokerID(brokerCore.getBrokerID());
			}
		}
	}

}

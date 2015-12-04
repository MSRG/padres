package ca.utoronto.msrg.padres.broker.order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.scout.Relation;
import ca.utoronto.msrg.padres.broker.router.scout.RelationIdentifier;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class TotalOrderEngine {

	static Logger toLogger = Logger.getLogger(TotalOrderEngine.class);
	static Logger brokerCoreLogger = Logger.getLogger(BrokerCore.class);
	static Logger exceptionLogger = Logger.getLogger("Exception");
	static Logger messagePathLogger = Logger.getLogger("MessagePath");
	
	private Router router;
	private TotalOrderConflictManager manager;

	private long avgDelay = 0L;
	private int received = 0;
	private int conflicts = 0;
	private int outgoingTraffic = 0;
	
	private int numSubs = 0;
	private int numAds = 0;
	
	public TotalOrderEngine(Router router){
		this.router = router;
		this.manager = new TotalOrderConflictManager(router.getBrokerId());
		brokerCoreLogger.info("Total Order Engine started.");
	}
	
	public void processMessage(PublicationMessage msg, Set<Message> messageSet) {	
		
		List<MessageDestination> hops = null; 
		Map<MessageDestination,PublicationMessage> msgs = null;
		Map<MessageDestination, Set<AdvertisementMessage>> advs = null;
		Map<MessageDestination, Set<SubscriptionMessage>> subs = null;
		
		int count = 0;
		
		if(msg.hasConflict()){
			if(!msg.getConflict().isAck()){
				count = messageSet.size();
			} else {
				count = -1;
			}
		} else 
			count = messageSet.size();
		
		if(msg.hasConflict() || count > 1){
			hops = new ArrayList<MessageDestination>();
			msgs = new HashMap<MessageDestination,PublicationMessage>();
						
			for(Message message : messageSet){
				hops.add(message.getNextHopID());
				msgs.put(message.getNextHopID(), (PublicationMessage)message);
			}
			
			advs = getAdvsByHop(hops);
			subs = getSubsByHop(hops, msg);
		}
		
		if(msg.hasConflict()){
			if(msg.getConflict().isAck()){
				messageSet.clear();
				messageSet.addAll(manager.removeConflict(msg));
			}
			else {
				if(msg.getConflict().isRequest()){
					resetRequests(messageSet);
					boolean conflict = false;
					
					for(MessageDestination hop : hops){
						if(hop.isBroker()){
							for(SubscriptionMessage sub : subs.get(hop)){
								boolean loop = true;
								for(AdvertisementMessage ad : advs.get(hop)){
									if(RelationIdentifier.getRelation(ad.getAdvertisement().getPredicateMap(), sub.getSubscription().getPredicateMap()) != Relation.EMPTY){
										msgs.get(hop).getConflict().setRequest(true);
										manager.addConflict(msgs.get(hop));
										conflict = true;
										loop = false;
										break;
									}
								}
								
								if(!loop)
									break;
							}
						}
					}			
					
					if(!conflict){
						clearRequest(messageSet);
						sendAck(msg, messageSet);
					}
				}
				if(msg.getConflict().isWait()){
					resolveWait(msg, messageSet, msgs);
				}
			}
		}

		if(count == -1){
			checkPubs(messageSet);
			return;
		}
		
		int oldConflicts = conflicts;
		
		received++;
		numSubs = ((received-1)*numSubs + router.getSubscriptions().size())/received;
		numAds = ((received-1)*numAds + router.getAdvertisements().size())/received;
		long startTime = System.nanoTime();
		if(count > 1){			
			advs = getAdvsByHop(hops);
			subs = getSubsByHop(hops, msg);
											
			for(MessageDestination hop : hops){
				for(MessageDestination hop2 : hops){
					if(!hop2.equals(hop)){
						if(hop.isBroker()){
							boolean loop = true;
							for(SubscriptionMessage sub : subs.get(hop)){
								for(AdvertisementMessage ad : advs.get(hop)){
									if(RelationIdentifier.getRelation(ad.getAdvertisement().getPredicateMap(), sub.getSubscription().getPredicateMap()) != Relation.EMPTY){		
										for(SubscriptionMessage sub2 : subs.get(hop2)){
											if((RelationIdentifier.getRelation(sub2.getSubscription().getPredicateMap(), ad.getAdvertisement().getPredicateMap()) != Relation.EMPTY) && ((RelationIdentifier.getRelation(sub2.getSubscription().getPredicateMap(), sub.getSubscription().getPredicateMap()) != Relation.EMPTY))){
												manager.addConflict(msg, sub, ad, sub2, msgs.get(hop), msgs.get(hop2));
												if(!hop2.isBroker()){
													manager.storePub(msgs.get(hop2));
													messageSet.remove(msgs.get(hop2));
												}
												if(conflicts == oldConflicts)
													conflicts++;
												loop = false;
												break;
											}
										}
										
										if(!loop)
											break;
									}
								}	
								if(!loop)
									break;
							}
						}
					}
				}
			}
		}
		
		checkPubs(messageSet);
		
//		outgoingTraffic += messageSet.size();
//		
//		avgDelay = (avgDelay*(received-1)+(System.nanoTime() - startTime))/received;
//		if((received % 10) == 0){
//			System.out.println(avgDelay + "," + conflicts + "," + received + "," + outgoingTraffic + "," + numSubs + "," + numAds);
//			avgDelay = 0;
//			conflicts = 0;
//			received = 0;
//			numSubs = 0;
//			numAds = 0;
//		}
	}

	private void checkPubs(Set<Message> messageSet) {
		Set<Message> toRemove = new HashSet<Message>();
		
		for(Message m : messageSet){
			if(!m.getNextHopID().isBroker()){
				if(!manager.isEmpty(m)){
					toRemove.add(m);
					manager.storePub((PublicationMessage)m);
				}
			}		
		}
		
		messageSet.removeAll(toRemove);
	}

	private void resolveWait(PublicationMessage msg, Set<Message> messageSet, Map<MessageDestination, PublicationMessage> msgs) {
		for(MessageDestination hop : msgs.keySet()){
			manager.addWait(msgs.get(hop));
			if(!hop.isBroker()){
				manager.storePub(msgs.get(hop));
				messageSet.remove(msgs.get(hop));
			}
		}
	}

//	private void distributeRequest(Set<String> subsToKeep,
//			Set<String> adsToKeep, Set<Message> messageSet, Map<MessageDestination, PublicationMessage> msgs) {
//		
//		for(Message m : messageSet){
//			((PublicationMessage)m).getConflict().empty();
//			((PublicationMessage)m).getConflict().setRequest(false);
//		}
//		
//		for(String sub : subsToKeep){
//			MessageDestination hop = router.getSubscriptionMessage(sub).getLastHopID();
//			msgs.get(hop).getConflict().addSubId(sub);
//			msgs.get(hop).getConflict().setRequest(true);
//		}
//		
//		for(String ad : adsToKeep){
//			MessageDestination hop = router.getAdvertisement(ad).getLastHopID();
//			msgs.get(hop).getConflict().addAdId(ad);
//			msgs.get(hop).getConflict().setRequest(true);			
//		}
//		
//		for(Message m : messageSet){
//			if(!((PublicationMessage)m).getConflict().isRequest()){
//				if(!((PublicationMessage)m).getConflict().isWait())
//					((PublicationMessage)m).clearConflict();
//			} else {
//				manager.addConflict((PublicationMessage)m);
//			}
//		}
//	}
	
	private void resetRequests(Set<Message> messageSet){
		for(Message m : messageSet){
			((PublicationMessage)m).getConflict().empty();
			((PublicationMessage)m).getConflict().setRequest(false);
		}
	}
	
	private void clearRequest(Set<Message> messageSet) {
		for(Message m : messageSet){
			if(((PublicationMessage)m).getConflict().isWait()){
				((PublicationMessage)m).getConflict().clear();
				((PublicationMessage)m).getConflict().setRequest(false);
			}
			else
				((PublicationMessage)m).clearConflict();
		}
	}

	private void sendAck(PublicationMessage msg, Set<Message> messageSet) {
		PublicationMessage ackMsg = msg.duplicate();
		ackMsg.getConflict().clear();
		ackMsg.getConflict().setWait(false);
		ackMsg.getConflict().setAck(true);
		ackMsg.getPublication().setPayload(null);
		ackMsg.setNextHopID(msg.getLastHopID());
		messageSet.add(ackMsg);
	}

	private Map<MessageDestination, Set<AdvertisementMessage>> getAdvsByHop(
			List<MessageDestination> hops) {
		Map<MessageDestination, Set<AdvertisementMessage>> advs = new HashMap<MessageDestination, Set<AdvertisementMessage>>();
		
		for(MessageDestination hop : hops){
			advs.put(hop, new HashSet<AdvertisementMessage>());
		}
		
		for(AdvertisementMessage msg : router.getAdvertisements().values()){
			if(hops.contains(msg.getLastHopID())){
				advs.get(msg.getLastHopID()).add(msg);
			}
		}
		
		return advs;
	}
	
	private Map<MessageDestination, Set<SubscriptionMessage>> getSubsByHop(List<MessageDestination> hops, PublicationMessage pub){
		Map<MessageDestination, Set<SubscriptionMessage>> subs = new HashMap<MessageDestination, Set<SubscriptionMessage>>();
		
		for(MessageDestination hop : hops){
			subs.put(hop, new HashSet<SubscriptionMessage>());
		}
		
		for(SubscriptionMessage msg : router.getSubscriptions(pub)){
			if(hops.contains(msg.getLastHopID())){
				subs.get(msg.getLastHopID()).add(msg);
			}
		}
		
		return subs;
	}
	
	public void processMessage(SubscriptionMessage msg) {
		if(!msg.getLastHopID().isBroker()){ // This is an edge broker
			msg.setSender(msg.getLastHopID());
		}
	}
}

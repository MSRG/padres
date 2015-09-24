package ca.utoronto.msrg.padres.broker.order;

import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class TotalOrderConflict {

	private String pubId;
//	private Set<String> adIds;
//	private Set<String> subIds;
	
	private MessageDestination hop;
	private MessageDestination otherHop;
	
	public TotalOrderConflict(String pubId, MessageDestination subHop, MessageDestination otherHop) {
		this.pubId = pubId;
//		this.adIds = new HashSet<String>();
//		this.subIds = new HashSet<String>();
		this.hop = subHop;
		this.otherHop = otherHop;
	}

	public TotalOrderConflict(PublicationMessage pub, SubscriptionMessage sub, SubscriptionMessage other) {
		this(pub.getMessageID(),sub.getLastHopID(),other.getLastHopID());
//		this.addAdId(ad.getMessageID());
//		this.addSubId(sub.getMessageID());
	}

	public String getPubId() {
		return pubId;
	}

	public MessageDestination getHop() {
		return hop;
	}

	public MessageDestination getOtherHop() {
		return otherHop;
	}	
	
//	public void addSubId(String subId){
//		subIds.add(subId);
//	}
//	
//	public void addAdId(String adId){
//		adIds.add(adId);
//	}
	
	public String toString(){
		return "PID: " + pubId + ", OTHERHOP: " + otherHop + ", HOP: " + hop;
	}

//	public Set<String> getAdIds() {
//		return adIds;
//	}
//
//	public Set<String> getSubIds() {
//		return subIds;
//	}
//
//	public void addAdIds(Set<String> adIds) {
//		this.adIds.addAll(adIds);
//	}
//
//	public void addSubIds(Set<String> subIds) {
//		this.subIds.addAll(subIds);		
//	}
}
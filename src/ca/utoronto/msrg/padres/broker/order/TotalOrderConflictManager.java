package ca.utoronto.msrg.padres.broker.order;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class TotalOrderConflictManager {

	private Map<MessageDestination,List<TotalOrderConflict>> acks;
	private Map<MessageDestination,List<PublicationMessage>> queue;
	private String brokerId;
	
	private double rDelay = 0.0;
	private int rCount = 0;
	
	public TotalOrderConflictManager(String brokerId){
		this.acks = new HashMap<MessageDestination,List<TotalOrderConflict>>();
		this.queue = new HashMap<MessageDestination,List<PublicationMessage>>();
		this.brokerId = brokerId;
	}

	/**
	 * The ad and sub must be at the same next hop, other sub is on a diff next hop.
	 * @param sub
	 * @param other
	 * @param ad
	 * @param msgWait 
	 * @param msgRequest 
	 */
	public void addConflict(PublicationMessage pub, SubscriptionMessage sub, AdvertisementMessage ad, SubscriptionMessage other, PublicationMessage msgRequest, PublicationMessage msgWait) {
		MessageDestination hop = other.getLastHopID();
		
		if(!acks.containsKey(hop))
			acks.put(hop, new ArrayList<TotalOrderConflict>());
		
//		boolean added = false;
		TotalOrderConflict target = null;
		
		for(TotalOrderConflict conflict : acks.get(hop)){
			if(conflict.getHop().equals(sub.getLastHopID()) && conflict.getPubId().equals(pub.getMessageID())){
				return;
			}
		}
		
//		if(!added){
		target = new TotalOrderConflict(pub, sub, other);
		acks.get(hop).add(target);
//		}
		
		if(!msgRequest.hasConflict()){
			msgRequest.setConflict(new TotalOrderConflictInfo(target));
		}
		
//		msgRequest.getConflict().addAdIds(target.getAdIds());
//		msgRequest.getConflict().addSubIds(target.getSubIds());
		msgRequest.getConflict().setRequest(true);
		
		if(!msgWait.hasConflict()){
			msgWait.setConflict(new TotalOrderConflictInfo(target.getPubId()));
		}
		
//		msgWait.getConflict().addWaitSubs(other.getMessageID());
		msgWait.getConflict().setWait(true);
	}

	public TotalOrderConflict getConflict(MessageDestination otherHop, MessageDestination hop, String pubId) {
		if(!acks.containsKey(otherHop))
			return null;
		else{
			for(TotalOrderConflict conflict : acks.get(otherHop)){
				if(conflict.getPubId().equals(pubId) && conflict.getHop().equals(hop))
					return conflict;
			}
			
			return null;
		}
	}
	
	public List<PublicationMessage> removeConflict(PublicationMessage msg){
		List<PublicationMessage> list = new ArrayList<PublicationMessage>();
		
		for(MessageDestination hop : acks.keySet()){
			TotalOrderConflict conflict = getConflict(hop, msg.getLastHopID(), msg.getConflict().getPubId());
			if(conflict != null){
				acks.get(hop).remove(conflict);
				resolve(list, conflict, msg);
			}
		}
		
		return list;
	}

	private void resolve(List<PublicationMessage> list, TotalOrderConflict conflict, PublicationMessage msg) {
		if(conflict.getOtherHop().isBroker()){
			PublicationMessage ackMsg = msg.duplicate();
			ackMsg.setNextHopID(conflict.getOtherHop());
			list.add(ackMsg);
		} else {
			list.addAll(removeFromQueue(conflict));
		}
	}

	private List<PublicationMessage> removeFromQueue(TotalOrderConflict conflict) {
		
		List<PublicationMessage> toQueue = new ArrayList<PublicationMessage>();
		List<PublicationMessage> buffer = queue.get(conflict.getOtherHop());
			
		if(conflict.getPubId().equals(buffer.get(0).getMessageID())){
			for(PublicationMessage m : buffer){
				for(TotalOrderConflict c : acks.get(conflict.getOtherHop())){
					if(c.getPubId().equals(m.getMessageID())){
						buffer.removeAll(toQueue);
						Collections.sort(toQueue);
						return toQueue;
					}
				}
				
				toQueue.add(m);
//				if(Message.getBrokerIdFromMsgId(m.getMessageID()).equals(brokerId)){
//					rCount++;
//					double delay = (Calendar.getInstance(TimeZone.getTimeZone("GMT-5")).getTimeInMillis() - m.getPublication().getTimeStamp().getTime()) / 1000.0;
//					rDelay = ((rCount-1)*rDelay+delay)/rCount;
//					
//					if(rCount % 10 == 0){
//						System.out.println("[[[" + rDelay + "," + rCount);
//						rCount = 0;
//						rDelay = 0;
//					}
//				}
			}
		}
		
		buffer.removeAll(toQueue);
		Collections.sort(toQueue);
		return toQueue;
	}

	public void storePub(PublicationMessage pub) {
		if(!queue.containsKey(pub.getNextHopID())){
			queue.put(pub.getNextHopID(), new ArrayList<PublicationMessage>());
		}
		
		queue.get(pub.getNextHopID()).add(pub);
	}

	
	
	public void addConflict(PublicationMessage pub) {		
		MessageDestination hop = pub.getLastHopID();
		
		if(!acks.containsKey(hop))
			acks.put(hop, new ArrayList<TotalOrderConflict>());
		
		TotalOrderConflict target = null;
//		boolean added = false;
		
		for(TotalOrderConflict conflict : acks.get(hop)){
			if(conflict.getHop().equals(pub.getNextHopID()) && conflict.getPubId().equals(pub.getMessageID())){
//				added = true;
//				target = conflict;
//				break;
				return;
			}
		}
		
//		if(!added){
			target = new TotalOrderConflict(pub.getMessageID(),pub.getNextHopID(),pub.getLastHopID());
			acks.get(hop).add(target);
//		}
		
//		target.addAdIds(pub.getConflict().getAdIds());
//		target.addSubIds(pub.getConflict().getSubIds());
	}

	public void addWait(PublicationMessage pub) {
		MessageDestination hop = pub.getNextHopID();
		
		if(!acks.containsKey(hop))
			acks.put(hop, new ArrayList<TotalOrderConflict>());
		
		TotalOrderConflict target = null;
//		boolean added = false;
		
		for(TotalOrderConflict conflict : acks.get(hop)){
			if(conflict.getHop().equals(pub.getLastHopID()) && conflict.getPubId().equals(pub.getMessageID())){
//				added = true;
//				target = conflict;
//				break;
				return;
			}
		}
		
//		if(!added){
			target = new TotalOrderConflict(pub.getMessageID(),pub.getLastHopID(),pub.getNextHopID());
			acks.get(hop).add(target);
//		}
	}

//	public void addConflict(PublicationMessage pub, Set<String> subsToKeep,
//			Set<String> adsToKeep) {
//		MessageDestination hop = pub.getLastHopID();
//		
//		if(!acks.containsKey(hop))
//			acks.put(hop, new ArrayList<TotalOrderConflict>());
//		
//		TotalOrderConflict target = null;
//		boolean added = false;
//		
//		for(TotalOrderConflict conflict : acks.get(hop)){
//			if(conflict.getHop().equals(pub.getNextHopID()) && conflict.getPubId().equals(pub.getMessageID())){
//				added = true;
//				target = conflict;
//				break;
//			}
//		}
//		
//		if(!added){
//			target = new TotalOrderConflict(pub.getMessageID(),pub.getNextHopID(),pub.getLastHopID());
//			acks.get(hop).add(target);
//		}
//		
//		target.addAdIds(adsToKeep);
//		target.addSubIds(subsToKeep);
//	}

	public boolean isEmpty(Message pub) {
		if(!queue.containsKey(pub.getNextHopID())){
			return true;
		} else
			return queue.get(pub.getNextHopID()).isEmpty();
	}
}
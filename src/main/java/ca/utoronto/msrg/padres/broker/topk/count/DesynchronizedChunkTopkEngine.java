package ca.utoronto.msrg.padres.broker.topk.count;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.topk.Chunk;
import ca.utoronto.msrg.padres.broker.topk.TopkInfo;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class DesynchronizedChunkTopkEngine extends CountTopkEngine {

	private HashMap<String,Chunk> chunks;
	private HashMap<String,List<String>> queueList;
	private HashMap<String,Chunk> otherChunks;
	
	public DesynchronizedChunkTopkEngine(Router router, TopkInfo info) {
		super(router, info);
		chunks = new HashMap<String,Chunk>();
		queueList = new HashMap<String,List<String>>();
		otherChunks = new HashMap<String,Chunk>();
	}

	@Override
	public void processMessage(PublicationMessage msg, Set<Message> messageSet) {
		HashMap<MessageDestination, PublicationMessage> msgs = new HashMap<MessageDestination, PublicationMessage>();
		
		if(msg.isControl())
			return;
				
		for(Message m : messageSet){
			msgs.put(m.getNextHopID(), (PublicationMessage)m);
		}
		
		if(msg.hasTopk()){
			forwardMessage(msg, msgs, messageSet);
			return;
		} else {
			if(!msg.getLastHopID().isBroker()){ // source broker
				for(SubscriptionMessage sub : router.getSubscriptions(msg)){
//					The following check should not be required
//					if(!sub.isControl())
					
					if(events.containsKey(sub.getMessageID())){ 
						events.get(sub.getMessageID()).add(msgs.get(sub.getLastHopID()).duplicate());
						messageSet.remove(msgs.get(sub.getLastHopID()));
						process(sub.getMessageID(), messageSet);
					}
				}
			}
		}
	}

	private void process(String subID, Set<Message> messageSet) {
		Chunk current = chunks.get(subID);
		if(current == null){ // start a new chunk
			chunks.put(subID, new Chunk(1, subID));
			current = chunks.get(subID);
		} else if (current.getCount() == info.getChunkSize()){
			chunks.put(subID, new Chunk(current.getSubCount()+1, subID));
			current = chunks.get(subID);
		}
		
		if((current.getCount() < info.getWindowSize()) || (current.getCount() >= info.getChunkSize() - info.getWindowSize())){
			PublicationMessage pub = events.get(subID).remove(0);
			pub.addTopkSubId(subID);
			pub.setChunkId(current.getChunkId());
			messageSet.add(pub);
		} else {
			processWindow(subID, messageSet, current.getChunkId());
		}
		
		current.incrementCount();
	}

	private void forwardMessage(
			PublicationMessage msg, HashMap<MessageDestination, PublicationMessage> msgs,
			Set<Message> messageSet) {
		
		List<String> subIds = msg.getTopkSubIds();
		Set<MessageDestination> newDests = new HashSet<MessageDestination>();
		Set<SubscriptionMessage> subsToProcess = new HashSet<SubscriptionMessage>();
		
		for(SubscriptionMessage sub : router.getSubscriptions(msg)){
			if(subIds.contains(sub.getMessageID())){
				if(sub.getLastHopID().isBroker()) // forward it
					newDests.add(sub.getLastHopID());
				else { // edge broker
					subsToProcess.add(sub);
				}
			}
		}
		
		Set<Message> newMessageSet = new HashSet<Message>();
		
		for(MessageDestination dest : newDests){
			newMessageSet.add(msgs.get(dest));
		}
		
		messageSet.clear();
		messageSet.addAll(newMessageSet);
		
		for(SubscriptionMessage sub : subsToProcess){
			processIncoming(sub, msgs, messageSet);
		}
	}

	private void processIncoming(SubscriptionMessage sub,
			HashMap<MessageDestination, PublicationMessage> msgs, Set<Message> messageSet) {
		
		String subID = sub.getMessageID();
		
		PublicationMessage pub = msgs.get(sub.getLastHopID());
		
		if(!pub.hasTopk())
			return; // something wrong if this happens
		
		Chunk current = chunks.get(subID);
		
		if(current == null){
			chunks.put(subID, new Chunk(pub.getChunkId(),subID));
			current = chunks.get(subID);
		}
		
		if(current.getChunkId().equals(pub.getChunkId())) {
			events.get(subID).add(pub);
			
			processWindow(subID, messageSet, current.getChunkId());
			
			if(pub.isProcessed()){ // not a guard, must be forwarded
				messageSet.add(pub);
			}
			
			current.incrementCount();
			
			checkAndProcessNewChunks(subID, messageSet, current);
		} else {			
			if(!queueList.get(subID).contains(pub.getChunkId())){
				queueList.get(subID).add(pub.getChunkId());
				otherChunks.put(pub.getChunkId(), new Chunk(pub.getChunkId(), subID));
			}
			
			otherChunks.get(pub.getChunkId()).addPublication(pub);
		}
	}

	private void checkAndProcessNewChunks(String subID, Set<Message> messageSet, Chunk current) {
		if(current.getCount() == info.getChunkSize()){ // choose a new chunk
			if(queueList.get(subID).isEmpty()){
				current = null;
			} else {
				current = otherChunks.remove(queueList.get(subID).remove(0));
			}
			
			chunks.put(subID, current);
			
			if(current != null){
				for(PublicationMessage newPub : current.getPublications()){
					events.get(subID).add(newPub);
					
					processWindow(subID, messageSet, current.getChunkId());
					
					if(newPub.isProcessed()){ // not a guard, must be forwarded
						messageSet.add(newPub);
					}
					
					current.incrementCount();
				}
				
				checkAndProcessNewChunks(subID, messageSet, current); // recursive because multiple chunks might be full
			}
		}
	}

	public void processWindow(String subID, Set<Message> messageSet, String chunkID){
		int size = Math.max(info.getWindowSize(), info.getShift());
		if(events.get(subID).size() >= size){
			Collection<PublicationMessage> topk = sortAndRemove(events.get(subID),0,subID.hashCode());
			Collection<PublicationMessage> finalTopk = new Vector<PublicationMessage>();
			
			for(PublicationMessage m : topk){
				if(!m.isProcessed()){
					finalTopk.add(m);
					m.setProcessed(true);
					m.addTopkSubId(subID);
					m.setChunkId(chunkID);
				}
			}
			
			messageSet.addAll(finalTopk);
		}
	}
	
	@Override
	public void processMessage(SubscriptionMessage sub) {
		if(sub.isControl())
			return;
		if(!events.containsKey(sub.getMessageID())){
			events.put(sub.getMessageID(), new Vector<PublicationMessage>());
			chunks.put(sub.getMessageID(), null);
			queueList.put(sub.getMessageID(), new ArrayList<String>());
		}
	}
	
	@Override
	public String toString() {
		int queueSize = 0;
		
		for(Vector<PublicationMessage> v : events.values()){
			queueSize += v.size();
		}
		
		for(Chunk c : otherChunks.values()){
			queueSize += c.getPublications().size();
		}
		
		return "" + queueSize;
	}	
}

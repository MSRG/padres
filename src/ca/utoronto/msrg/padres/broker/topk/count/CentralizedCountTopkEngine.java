package ca.utoronto.msrg.padres.broker.topk.count;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.topk.TopkInfo;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class CentralizedCountTopkEngine extends CountTopkEngine {

	public CentralizedCountTopkEngine(Router router, TopkInfo info) {
		super(router, info);
	}

	@Override
	public void processMessage(PublicationMessage msg, Set<Message> messageSet) {
		HashMap<MessageDestination, PublicationMessage> msgs = new HashMap<MessageDestination, PublicationMessage>();
			
		if(msg.isControl())
			return;
		
		for(Message m : messageSet){
			msgs.put(m.getNextHopID(), (PublicationMessage)m);
		}
		
		for(SubscriptionMessage sub : router.getSubscriptions(msg)){
//			The following check should not be required
//			if(!sub.isControl())
			
			if(events.containsKey(sub.getMessageID())){ // Sub handled at the edge of this broker
				events.get(sub.getMessageID()).add(msgs.get(sub.getLastHopID()).duplicate());
				messageSet.remove(msgs.get(sub.getLastHopID()));
				processWindow(sub.getMessageID(), messageSet);
			}
		}
	}

	private void processWindow(String subID, Set<Message> messageSet) {
		int size = Math.max(info.getWindowSize(), info.getShift());
		if(events.get(subID).size() >= size){
			Collection<PublicationMessage> topk = sortAndRemove(events.get(subID),0,subID.hashCode());
			Collection<PublicationMessage> finalTopk = new Vector<PublicationMessage>();
			
			for(PublicationMessage m : topk){
				if(!m.isProcessed()){
					finalTopk.add(m);
					m.setProcessed(true);
				}
			}
			
			messageSet.addAll(finalTopk);
		}
	}

	@Override
	public void processMessage(SubscriptionMessage sub) {
		if(sub.isControl())
			return;
		if(!events.containsKey(sub.getMessageID()) && !sub.getLastHopID().isBroker()){
			events.put(sub.getMessageID(), new Vector<PublicationMessage>());
		}
	}

	@Override
	public String toString() {
		int queueSize = 0;
		
		for(Vector<PublicationMessage> v : events.values()){
			queueSize += v.size();
		}
		
		return "" + queueSize;
	}	
}

package ca.utoronto.msrg.padres.broker.topk.count;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.topk.TopkInfo;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public class BufferedDesynchronizedChunkTopkEngine extends
		DesynchronizedChunkTopkEngine {

	private HashMap<String,List<MessageDestination>> sentDestinations;
	
	public BufferedDesynchronizedChunkTopkEngine(Router router, TopkInfo info) {
		super(router, info);
		sentDestinations = new HashMap<String,List<MessageDestination>>();
	}

	@Override
	public void processMessage(PublicationMessage msg, Set<Message> messageSet) {
		// TODO Auto-generated method stub
		super.processMessage(msg, messageSet);
	}

	@Override
	public void processWindow(String subID, Set<Message> messageSet,
			String chunkID) {
		// TODO Auto-generated method stub
		super.processWindow(subID, messageSet, chunkID);
	}

	@Override
	public void processMessage(SubscriptionMessage sub) {
		// TODO Auto-generated method stub
		super.processMessage(sub);
	}
	
	
	

}

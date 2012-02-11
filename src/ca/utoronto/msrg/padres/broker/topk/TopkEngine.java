package ca.utoronto.msrg.padres.broker.topk;

import java.util.Collection;
import java.util.Set;
import java.util.Vector;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

public abstract class TopkEngine {

	public TopkEngine() {
		super();
	}

	public abstract void processMessage(PublicationMessage msg, Set<Message> messageSet);
	
	public abstract void processMessage(SubscriptionMessage sub);

	protected abstract Collection<PublicationMessage> sortAndRemove(Vector<PublicationMessage> events, int start,
			int seed);

}
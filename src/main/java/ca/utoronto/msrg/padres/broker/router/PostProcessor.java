package ca.utoronto.msrg.padres.broker.router;

import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;


public interface PostProcessor {
		
	public void postprocess(Message msg, MessageType type, Set<Message> messagesToRoute);

	public void initialize();


}

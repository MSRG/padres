package ca.utoronto.msrg.padres.client;

import java.util.HashSet;
import java.util.Set;

import ca.utoronto.msrg.padres.common.comm.MessageListenerInterface;
import ca.utoronto.msrg.padres.common.comm.MessageQueue;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;

/**
 * This is the class that accepts messages from the communication layer. The thread in the
 * communication layer that listens for the messages will use the notify() method of this class to
 * pass the message to this class.
 * 
 * This class processes the messages in two ways. (a) by enqueuing the message to all the registered
 * message queues; and (b) by calling the processMessage() method of the Client object who
 * instantiated this object.
 * 
 * Generally, calling the processMessage() method should be enough, but in case the message has to
 * passed through multiple processing entities, those processing entities can register their message
 * queues with this class and when the message is received by this class, a copy the message will be
 * disseminated to all those processing entities.
 * 
 * @author Bala Maniymaran
 * 
 *         Created: 2011-01-26
 * 
 */
public class MessageQueueManager implements MessageListenerInterface {

	private Client myClient;

	private Set<MessageQueue> msgQueues;

	public MessageQueueManager(Client client) {
		myClient = client;
		msgQueues = new HashSet<MessageQueue>();
	}

	@Override
	public void notifyMessage(Message msg, HostType sourceType) {
		// received a message from broker, re-distribute a copy of it to all the registered queue.
		for (MessageQueue msgQueue : msgQueues) {
			msgQueue.add(msg.duplicate());
		}
		// the original message is passed on to the client for processing
		myClient.processMessage(msg);
	}

	/**
	 * If an object wants to receive a copy of the message received at the communication interface
	 * (from the broker), it can use this method to register its message queue so that it can
	 * receive the message when it is available.
	 * 
	 * @param msgQueue
	 *            A message queue to hold the incoming messages.
	 */
	public void addMessageQueue(MessageQueue msgQueue) {
		msgQueues.add(msgQueue);
	}

}

package ca.utoronto.msrg.padres.test.junit.tester;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

/**
 * Auxiliary class used as part of test framework. The class collects,
 * timestamps and queues all messages received/sent as part of a testcase.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class TesterMessageCollector {
	
	protected Queue<MessageItem> _collectedMsgs =
		new ConcurrentLinkedQueue<MessageItem>();
	
	public TesterMessageCollector() { }
	
	public synchronized void addMessage(String uri, Message msg, String destination) {
		MessageItem msgItem = new MessageItem(uri, msg, destination);
		_collectedMsgs.add(msgItem);
	}
	
	public Queue<MessageItem> getCollectedMessages() {
		return _collectedMsgs;
	}
	
	public List<MessageItem> checkMultipleMessageItem(
			String uri, MessageType msgType, TesterMessagePredicates msgPredicates, String destination) {
		List<MessageItem> foundMessageItems = new LinkedList<MessageItem>();
		
		for(MessageItem collectedMessageItem : _collectedMsgs) {
			Message collectedMessage = collectedMessageItem._msg;
			if(collectedMessage.getType() != msgType)
				continue;
			
			TesterMessagePredicates preds;
			if(msgType.equals(MessageType.SUBSCRIPTION)) {
				SubscriptionMessage subMessage = (SubscriptionMessage) collectedMessage;
				Subscription sub = subMessage.getSubscription();
				preds = TesterMessagePredicates.createTesterMessagePredicates(sub);
			} else if(msgType.equals(MessageType.PUBLICATION)) {
				PublicationMessage pubMessage = (PublicationMessage) collectedMessage;
				Publication pub = pubMessage.getPublication();
				preds = TesterMessagePredicates.createTesterMessagePredicates(pub);
			} else if(msgType.equals(MessageType.ADVERTISEMENT)) {
				AdvertisementMessage advMessage = (AdvertisementMessage) collectedMessage;
				Advertisement adv = advMessage.getAdvertisement();
				preds = TesterMessagePredicates.createTesterMessagePredicates(adv);
			} else {
				throw new UnsupportedOperationException();
			}
			
			if(msgPredicates.match(preds))
				foundMessageItems.add(collectedMessageItem);
		}
			
		return foundMessageItems;
	}
	
	public long checkFirstMessageItem(
			String uri, MessageType msgType,
			TesterMessagePredicates msgPredicates, String destination) {
		List<MessageItem> foundMessageItems = checkMultipleMessageItem(
				uri, msgType, msgPredicates, destination);
		if(foundMessageItems.isEmpty())
			return -1;
		else
			return foundMessageItems.get(0)._time;
	}
}

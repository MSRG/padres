package ca.utoronto.msrg.padres.test.junit.tester;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;

/**
 * Implementation of the IBrokerTester interface used for testing.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class GenericBrokerTester implements IBrokerTester {

	public static int DEFAULT_WAIT = 10000;  // millisec.
	public static int DEFAULT_SHUTDOWN_WAIT = 15000; // millisec.
	public static int DEFAULT_START_WAIT = 10000; // millisec.
	public static boolean PRODUCE_PRINT_TRACES = false;
	public static boolean lookAtPubs = false;
	public static boolean lookAtSubs = false;
	public static boolean lookAtAdvs = false;
	public static String classSpecificMessages = "stock";

	
	public List<SimpleExpectedEvent> _simpleExpectedEvents =
		new LinkedList<SimpleExpectedEvent>();

	enum SimpleExpectedEventType {
		QUEUE_HANDLER_STOP,
		QUEUE_HANDLER_START,
		QUEUE_HANDLER_SHUTDOWN,
		
		COMM_SYSTEM_START,
		COMM_SYSTEM_SHUTDOWN,
	}
	
	class SimpleExpectedEvent {
		public final SimpleExpectedEventType _type;
		public final String _brokerURI;
		
		protected SimpleExpectedEvent(SimpleExpectedEventType type, String brokerURI) {
			_type = type;
			_brokerURI = brokerURI;
		}
		
		public boolean match(SimpleExpectedEventType type, String brokerURI) {
			if(type != _type) return false;
			if(_brokerURI != null)
				return _brokerURI.equals(brokerURI);
			
			return true;
		}
		
		@Override
		public String toString() {
			return "SE:" + _type + ":" + _brokerURI;
		}
	}
	
	abstract class ExpectedQueueHandlerEvent extends SimpleExpectedEvent {
		public final MessageDestination _dest;
		
		protected ExpectedQueueHandlerEvent(SimpleExpectedEventType type, String brokerURI, MessageDestination dest) {
			super(type, brokerURI);
			_dest = dest;
		}
		
		public boolean match(SimpleExpectedEventType type, String brokerURI, MessageDestination dest) {
			if(!super.match(type, brokerURI))
				return false;
			
			if(_dest != null)
				return _dest.equals(dest);
				
			return true;
		}
		
		@Override
		public String toString() {
			return super.toString() + ":" + _dest;
		}
	}
	
	class ExpectedQueueHandlerStopEvent extends ExpectedQueueHandlerEvent {
		
		ExpectedQueueHandlerStopEvent(String brokerURI, MessageDestination dest) {
			super(SimpleExpectedEventType.QUEUE_HANDLER_STOP, brokerURI, dest);
		}
	}
	
	class ExpectedQueueHandlerStartEvent extends ExpectedQueueHandlerEvent {
		
		ExpectedQueueHandlerStartEvent(String brokerURI, MessageDestination dest) {
			super(SimpleExpectedEventType.QUEUE_HANDLER_START, brokerURI, dest);
		}
	}
	
	class ExpectedQueueHandlerShutdownEvent extends ExpectedQueueHandlerEvent {
		
		ExpectedQueueHandlerShutdownEvent(String brokerURI, MessageDestination dest) {
			super(SimpleExpectedEventType.QUEUE_HANDLER_SHUTDOWN, brokerURI, dest);
		}
	}
	
	abstract class ExpectedCommSystemEvent extends SimpleExpectedEvent {
		
		public final String _serverAddress;
		
		ExpectedCommSystemEvent(SimpleExpectedEventType type, String brokerURI, String serverAddress) {
			super(type, brokerURI);
			_serverAddress = serverAddress;
		}
		
		public boolean match(SimpleExpectedEventType type, String brokerURI, String serverAddress) {
			if(!super.match(type, brokerURI))
				return false;
			
			if(_serverAddress != null)
				return _serverAddress.equals(serverAddress);
			
			return true;
		}
		
		@Override
		public String toString() {
			return super.toString() + ":" + _serverAddress;
		}
	}

	
	class ExpectedCommSystemStartEvent extends ExpectedCommSystemEvent {
		ExpectedCommSystemStartEvent(String brokerURI, String serverAddress) {
			super(SimpleExpectedEventType.COMM_SYSTEM_START, brokerURI, serverAddress);
		}
	}

	class ExpectedCommSystemShutdownEvent extends ExpectedCommSystemEvent {
		ExpectedCommSystemShutdownEvent(String brokerURI, String serverAddress) {
			super(SimpleExpectedEventType.COMM_SYSTEM_SHUTDOWN, brokerURI, serverAddress);
		}
	}
	
	enum BrokerEventType {
		// Message queue related.
		ENQUEUE,
		DEQUEUE,
		SENT,
//		RECEIVED, /* Currently we use ENQUEUE on INPUTQUEUE, instead of RECEIVED */
		
		// Router related
		ROUTER_HANDLE,
		ROUTER_ADD_SUB(MessageType.SUBSCRIPTION),
		ROUTER_ADD_ADV(MessageType.ADVERTISEMENT),
		ROUTER_ADD_UNSUB(MessageType.UNSUBSCRIPTION),
		ROUTER_ADD_UNADV(MessageType.UNADVERTISEMENT),
		ROUTER_ADD_COMPOSITE_SUBSCRIPTION(MessageType.COMPOSITESUBSCRIPTION),
		
		
		// LifeCycleManager
		LIFECICLEMANAGER_HANDLE_SHUTDOWN,
		LIFECICLEMANAGER_HANDLE_RESUME,
		LIFECICLEMANAGER_HANDLE_STOP,
		
		// DBHandler
		DB_HANLDER_INIT,
		
		// Connection related
		CONNECT_FAILED,
		CONNECTION_FAILED,
		
		// Client related
		CLIENT_PUB_RECEIVE,
		
		// Add more...
		;
		
		public final MessageType _messageType;
		
		private BrokerEventType() {
			this(null);
		}
		
		private BrokerEventType(MessageType messageType) {
			_messageType = messageType;
		}
	}
	
	class ExpectedBrokerEvent {
		public final String _brokerURI;
		public final BrokerEventType _direction;
		public final MessageType _msgType;
		public final String _objectId;
		public final TesterMessagePredicates _msgPredicates;
		public final String _queueDestination;
		public final boolean _negative;
		
		ExpectedBrokerEvent(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				String objectId, // The ad/sub/pub/unad/unsub id (not the Message id)
				TesterMessagePredicates msgPredicates,
				String queueDestination,
				boolean negative) {
			_brokerURI = brokerURI;
			_direction = direction;
			_msgType = msgType;
			_objectId = objectId;
			_msgPredicates = msgPredicates;
			_queueDestination = queueDestination;
			_negative = negative;
		}
		
		ExpectedBrokerEvent(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination,
				boolean negative) {
			// Call the full constructor with null objectId.
			this(brokerURI, direction, msgType, null, msgPredicates, queueDestination, negative);
		}
		
		ExpectedBrokerEvent(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				String objectId,
				String queueDestination,
				boolean negative) {
			// Call the full constructor with null predicates.
			this(brokerURI, direction, msgType, objectId, null, queueDestination, negative);
		}

		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String subId,
				String queueDestination) {
			return match(brokerURI, direction, msgType, subId, msgPredicates, queueDestination);
		}
		
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				CompositeSubscription cs,
				String queueDestination) {

			return false;
		}
		
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			return match(brokerURI, direction, msgType, null, msgPredicates, queueDestination);
		}
	
			public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				String objectId,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			if(_brokerURI != null)
				if(!_brokerURI.equals(brokerURI))
					return false;
			
			if(_direction != null)
				if(_direction != direction)
					return false;
			
			if(_msgType != null)
				if(msgType == null || !_msgType.equals(msgType))
					return false;
			
			if(_objectId != null && !_objectId.isEmpty())
				if (!_objectId.equals(objectId))
					return false;

			if(_msgPredicates != null)
				if(!_msgPredicates.match(msgPredicates))
					return false;
			
			if(_queueDestination != null)
				if(!_queueDestination.equals(queueDestination))
					return false;
			
			return true;
		}
		
		@Override
		public String toString() {
			return "Event:::"
				+ _brokerURI + ":::"
				+ _direction + ":::"
				+ _msgType + ":::"
				+ _objectId + ":::"
				+ _msgPredicates + ":::"
				+ _queueDestination + ":::"
				+ _negative;
		}
	}
	
	class ExpectedCompositeSubscribeBrokerEvent extends ExpectedBrokerEvent {

		public final CompositeSubscription _cs;

		ExpectedCompositeSubscribeBrokerEvent(
				String brokerURI,
				CompositeSubscription cs,
				String queueDestination, boolean negative) {
			super(brokerURI, BrokerEventType.ROUTER_ADD_COMPOSITE_SUBSCRIPTION,
					MessageType.COMPOSITESUBSCRIPTION, null, null,
					queueDestination, negative);
			
			_cs = cs;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				CompositeSubscription cs,
				String queueDestination) {
			
			if(_brokerURI != null)
				if(!_brokerURI.equals(brokerURI))
					return false;
			
			if(_direction != null)
				if(_direction != direction)
					return false;
			
			if(_msgType != null)
				if(msgType == null || !_msgType.equals(msgType))
					return false;
			
			if(_cs != null)
				if(!_cs.equals(cs))
					return false;
				
			if(_queueDestination != null)
				if(!_queueDestination.equals(queueDestination))
					return false;
			
			return true;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String subId,
				String queueDestination) {
			if(!super.match(brokerURI, direction, msgType, msgPredicates, queueDestination))
				return false;
			
			return false;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			return false;
		}
	}
	
	class ExpectedUnsubscribeBrokerEvent extends ExpectedBrokerEvent {

		public final String _subId;

		ExpectedUnsubscribeBrokerEvent(String brokerURI,
				TesterMessagePredicates msgPredicates, String subId,
				String queueDestination, boolean negative) {
			super(brokerURI, BrokerEventType.ROUTER_ADD_UNSUB,
					MessageType.UNSUBSCRIPTION, msgPredicates,
					queueDestination, negative);
			_subId = subId;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String subId,
				String queueDestination) {
			if(!super.match(brokerURI, direction, msgType, msgPredicates, queueDestination))
				return false;
			
			if(_subId != null)
				return _subId.equals(subId);
			
			return true;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			return false;
		}
		
		@Override
		public String toString() {
			return super.toString() + ":::" + _subId;
		}
	}
	
	class ExpectedUnadvertiseBrokerEvent extends ExpectedBrokerEvent {

		public final String _advId;

		ExpectedUnadvertiseBrokerEvent(String brokerURI,
				TesterMessagePredicates msgPredicates, String advId,
				String queueDestination, boolean negative) {
			super(brokerURI, BrokerEventType.ROUTER_ADD_UNADV,
					MessageType.UNADVERTISEMENT, msgPredicates,
					queueDestination, negative);
			_advId = advId;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String advId,
				String queueDestination) {
			if(!super.match(brokerURI, direction, msgType, msgPredicates, queueDestination))
				return false;
			
			return _advId.equals(advId);
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			return false;
		}
		
		@Override
		public String toString() {
			return super.toString() + ":::" + _advId;
		}
	}
	
	class ExpectedConnectFailedBrokerEvent extends ExpectedBrokerEvent {

		public final String _toBrokerURI;

		ExpectedConnectFailedBrokerEvent(
				String brokerURI,
				String toBrokerURI, boolean negative) {
			super(brokerURI, BrokerEventType.CONNECT_FAILED,
					null, null, null, null, negative);
			
			_toBrokerURI = toBrokerURI;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				CompositeSubscription cs,
				String queueDestination) {
			return false;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String subId,
				String queueDestination) {
			return false;
		}
		
		@Override
		public boolean match(
				String brokerURI,
				BrokerEventType direction,
				MessageType msgType,
				TesterMessagePredicates msgPredicates,
				String queueDestination) {
			return false;
		}
		
		public boolean match(String brokerURI, String toBrokerURI) {
			if(_brokerURI != null)
				if(!_brokerURI.equalsIgnoreCase(brokerURI))
					return false;
			
			if(_toBrokerURI != null)
				if(!_toBrokerURI.equalsIgnoreCase(toBrokerURI))
					return false;
			
			return true;
		}
		
		@Override
		public String toString() {
			return super.toString() + ":::" + _toBrokerURI;
		}
	}
	
	TesterMessageCollector _msgCollector =
		new TesterMessageCollector();
	
	protected boolean _error = false;
	
	protected List<ExpectedBrokerEvent> _expectedEvents =
		new LinkedList<ExpectedBrokerEvent>();
	
	@Override
	public boolean waitUntilExpectedStartsHappen() throws InterruptedException {
		return waitUntilExpectedStartsHappen(DEFAULT_START_WAIT, true);
	}
	
	@Override
	public boolean waitUntilExpectedShutdownHappen() throws InterruptedException {
		return waitUntilExpectedShutdownHappen(DEFAULT_SHUTDOWN_WAIT, true);
	}

	@Override
	public boolean waitUntilExpectedEventsHappen() throws InterruptedException {
		return waitUntilExpectedEventsHappen(DEFAULT_WAIT);
	}
	
	public boolean checkFirstMessageItem(
			String uri, MessageType msgType,
			TesterMessagePredicates msgPredicates, String destination) {
		return _msgCollector.checkFirstMessageItem(
				uri, msgType, msgPredicates, destination) != -1;
	}
	
	public Queue<MessageItem> getCollectedMessages() {
		return _msgCollector.getCollectedMessages();
	}
	
	public boolean noError() {
		return _error == false;
	}

	@Override
	public boolean waitUntilExpectedEventsHappen(
			int waitMillis) throws InterruptedException {
		return waitUntilExpectedEventsHappen(waitMillis, true);
	}
	
	public boolean waitUntilExpectedEventsHappen(
			int waitMillis, boolean printErrors) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		
		synchronized (_expectedEvents) {
			while((_expectedEvents.size() != 0) &&
					(waitMillis - (System.currentTimeMillis() - startTime) > 0)) {
				_expectedEvents.wait(500);
			
				if(!matchEvents())
					return false;
			}
			
			for(ExpectedBrokerEvent expectedEvent : _expectedEvents) {
				if(!expectedEvent._negative) {
					if(printErrors)
						System.err.println("Error: " + expectedEvent.toString());
					return false;
				}
			}
			
			return !_error;
		}
	}
	
	protected boolean matchEvents() {
		Iterator<ExpectedBrokerEvent> expectedBrokerEventIt = _expectedEvents.iterator();
		while(expectedBrokerEventIt.hasNext()){
			ExpectedBrokerEvent expectedEvent = expectedBrokerEventIt.next();
			String brokerURI = expectedEvent._brokerURI;
			
			List<MessageEventTypePair> messages = _addedSubscriptionsAdvertisements.get(brokerURI);
			if(messages != null) {
				for(MessageEventTypePair msgEventType : messages) {
					Message msg = msgEventType._msg;
					BrokerEventType eventType = msgEventType._type;
					MessageDestination msgDestination = msg.getLastHopID();
					if(expectedEvent.match(
							brokerURI, eventType, msg.getType(),
							getFullPredicates(msg), msgDestination.getDestinationID())) {
						if(expectedEvent._negative) {
							System.err.println("Event should not happen: " + expectedEvent.toString() + " vs. " + msgEventType);
							return false;
						} else {
							expectedBrokerEventIt.remove();
						}
					}
				}
			}
		}
		
		return true;
	}
	
	public IBrokerTester clearAll() {
		synchronized (_expectedEvents) {
			_addedSubscriptionsAdvertisements.clear();
			_expectedEvents.clear();
			_expectedEvents.notifyAll();
			setError(false);
		}
		
		return this;
	}
	
	public IBrokerTester clearExpected() {
		synchronized (_expectedEvents) {
			_expectedEvents.clear();
			_expectedEvents.notifyAll();
			setError(false);
		}

		return this;
	}

	@Override
	public IBrokerTester expectReceipt(
			String brokerURI,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String queueDestination) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ENQUEUE, msgType,
							expectedMsgPredicates, queueDestination, false));	
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectReceipt(
			String brokerURI,
			MessageType msgType,
			String objectId,
			String queueDestination) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ENQUEUE, msgType,
							objectId, queueDestination, false));	
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectNegativeReceipt(
			String brokerURI,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String queueDestination) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ENQUEUE, msgType,
							expectedMsgPredicates, queueDestination, true));	
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectSend(
			String brokerURI,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String remoteBrokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.SENT, msgType,
							expectedMsgPredicates, remoteBrokerURI, false));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectSend(
			String brokerURI,
			MessageType msgType,
			String objectId,
			String remoteBrokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.SENT, msgType,
							objectId, remoteBrokerURI, false));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectNegativeSend(
			String brokerURI,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String remoteBrokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.SENT, msgType,
							expectedMsgPredicates, remoteBrokerURI, true));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public void msgReceived(String brokerURI, Message msg) {
		_msgCollector.addMessage(brokerURI, msg, null);
		String queuename = "INPUTQUEUE";
		report(
				"RV(" + queuename + "->" + brokerURI + "): ", null, msg);

		MessageType msgType = msg.getType();
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				boolean doesItMatch = false;
				if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ENQUEUE, MessageType.COMPOSITESUBSCRIPTION,
							((CompositeSubscriptionMessage)msg).getSubscription(),
							queuename);
				} else if(msgType.equals(MessageType.UNADVERTISEMENT)) {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ENQUEUE, MessageType.UNADVERTISEMENT,
							getFullPredicates(msg),
							((UnadvertisementMessage) msg).getUnadvertisement().getAdvID(),
							queuename);
				} else if(msgType.equals(MessageType.UNSUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ENQUEUE, MessageType.UNSUBSCRIPTION,
							getFullPredicates(msg),
							((UnsubscriptionMessage) msg).getUnsubscription().getSubID(),
							queuename);
				} else {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ENQUEUE, msgType,
							getFullPredicates(msg),
							queuename);
				}
				
				if(doesItMatch) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + 
								"@" + brokerURI + ":::" +
								expectedEvent.toString() + " v. "
								+ msg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void msgSent(ITestMessageSender messageSender, Message msg) {
		String localURI = messageSender.getBrokerURI();
		String remoteURI = messageSender.getRemoteURI();
		MessageType msgType = msg.getType();
		
		if(remoteURI == null)
			report("ST(" + remoteURI + "=>" + remoteURI + ")", null, msg);
		
		_msgCollector.addMessage(localURI, msg, remoteURI);
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				boolean doesItMatch = false;
				if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							localURI, BrokerEventType.SENT, MessageType.COMPOSITESUBSCRIPTION,
							((CompositeSubscriptionMessage)msg).getSubscription(),
							remoteURI);
				} else if(msgType.equals(MessageType.UNADVERTISEMENT)) {
					doesItMatch = expectedEvent.match(
							localURI, BrokerEventType.SENT, MessageType.UNADVERTISEMENT,
							getFullPredicates(msg),
							((UnadvertisementMessage) msg).getUnadvertisement().getAdvID(),
							remoteURI);
				} else if(msgType.equals(MessageType.UNSUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							localURI, BrokerEventType.SENT, MessageType.UNSUBSCRIPTION,
							getFullPredicates(msg),
							((UnsubscriptionMessage) msg).getUnsubscription().getSubID(),
							remoteURI);
				} else {
					doesItMatch = expectedEvent.match(
							localURI, BrokerEventType.SENT, msgType,
							getFullPredicates(msg),
							remoteURI);
				}
				
				if(doesItMatch) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + msg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void msgDequeued(TesterMessageQueue msgQueue, Message msg) {
		MessageType msgType = msg.getType();
		
		if(msgType.equals(MessageType.SHUTDOWN))
			return;
		
		report("DQ", msgQueue, msg);
		MessageDestination msgDestination = msgQueue._myDestination;
		String dest = msgDestination == null ? null : msgDestination.getDestinationID();
		
		_msgCollector.addMessage(msgQueue._brokerURI, msg, dest);
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				boolean doesItMatch = false;
				if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							msgQueue._brokerURI, BrokerEventType.DEQUEUE, MessageType.COMPOSITESUBSCRIPTION,
							((CompositeSubscriptionMessage)msg).getSubscription(),
							"" + msgQueue._myDestination);
				} else if(msgType.equals(MessageType.UNADVERTISEMENT)) {
					doesItMatch = expectedEvent.match(
							msgQueue._brokerURI, BrokerEventType.DEQUEUE, MessageType.UNADVERTISEMENT,
							getFullPredicates(msg),
							((UnadvertisementMessage) msg).getUnadvertisement().getAdvID(),
							"" + msgQueue._myDestination);
				} else if(msgType.equals(MessageType.UNSUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							msgQueue._brokerURI, BrokerEventType.DEQUEUE, MessageType.UNSUBSCRIPTION,
							getFullPredicates(msg),
							((UnsubscriptionMessage) msg).getUnsubscription().getSubID(),
							"" + msgQueue._myDestination);
				} else {
					doesItMatch = expectedEvent.match(
							msgQueue._brokerURI, BrokerEventType.DEQUEUE, msgType,
							getFullPredicates(msg),
							"" + msgQueue._myDestination);
				}
				
				if(doesItMatch) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + msg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	protected void report(String prefix, TesterMessageQueue msgQueue, Message msg) {
		if(!PRODUCE_PRINT_TRACES)
			return;
		
		if(lookAtPubs && msg.getType().equals(MessageType.PUBLICATION)) {
			Publication pub = ((PublicationMessage)msg).getPublication();
			if(classSpecificMessages == null || pub.getClassVal().equals(classSpecificMessages))
				System.out.println(
						prefix + "(" + msgQueue + ")" +
						">>>" + (msgQueue == null ? null : msgQueue.getMyDestination()) +
						">>>" + msg);
		}
		
		if(lookAtSubs && msg.getType().equals(MessageType.SUBSCRIPTION)) {
			Subscription sub = ((SubscriptionMessage)msg).getSubscription();
			if(classSpecificMessages == null || sub.getClassVal().equals(classSpecificMessages))
				System.out.println(
						prefix + "(" + msgQueue + ")" +
						">>>" + (msgQueue == null ? null : msgQueue.getMyDestination()) +
						">>>" + msg);
		}
		
		if(lookAtAdvs && msg.getType().equals(MessageType.ADVERTISEMENT)) {
			Advertisement adv = ((AdvertisementMessage)msg).getAdvertisement();
			if(classSpecificMessages == null || adv.getClassVal().equals(classSpecificMessages)) {
				System.out.println(
						prefix + "(" + msgQueue + ")" +
						">>>" + (msgQueue == null ? null : msgQueue.getMyDestination()) +
						">>>" + msg);
				
				if(prefix.equals("DQ"))
					return;
			}
		}
	}
	
	@Override
	public void msgEnqueued(TesterMessageQueue msgQueue, Message msg) {
		report("EQ", msgQueue, msg);
		
		MessageType msgType = msg.getType();
		
		if(msgType.equals(MessageType.SHUTDOWN))
			return;
		
		MessageDestination msgDestination = msgQueue._myDestination;
		String dest = msgDestination == null ? null : msgDestination.getDestinationID();
		_msgCollector.addMessage(msgQueue._brokerURI, msg, dest);
		
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				boolean doesItMatch = false;
				if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							msgQueue == null ? null : msgQueue._brokerURI,
							BrokerEventType.ENQUEUE, msgType,
							((CompositeSubscriptionMessage)msg).getSubscription(),
							"" + (msgQueue == null ? null : msgQueue._myDestination));
				} else if(msgType.equals(MessageType.UNADVERTISEMENT)) {
					doesItMatch = expectedEvent.match(
							msgQueue == null ? null : msgQueue._brokerURI,
							BrokerEventType.ENQUEUE, msgType,
							getFullPredicates(msg),
							((UnadvertisementMessage) msg).getUnadvertisement().getAdvID(),
							"" + (msgQueue == null ? null : msgQueue._myDestination));
				} else if(msgType.equals(MessageType.UNSUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							msgQueue == null ? null : msgQueue._brokerURI,
							BrokerEventType.ENQUEUE, msgType,
							getFullPredicates(msg),
							((UnsubscriptionMessage) msg).getUnsubscription().getSubID(),
							"" + (msgQueue == null ? null : msgQueue._myDestination));
				} else {
					doesItMatch = expectedEvent.match(
							msgQueue == null ? null : msgQueue._brokerURI,
							BrokerEventType.ENQUEUE, msgType,
							getFullPredicates(msg),
							"" + (msgQueue == null ? null : msgQueue._myDestination));
				}
				
				if(doesItMatch) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + msg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}
	
	protected TesterMessagePredicates getFullPredicates(Message msg) {
		MessageType msgType = msg.getType();
		if(msgType.equals(MessageType.PUBLICATION))
			return getFullEuqlityPredicates(
					((PublicationMessage)msg).getPublication().
					getPairMap());

		else if(msgType.equals(MessageType.SUBSCRIPTION))
			return getFullPredicates(
					((SubscriptionMessage)msg).getSubscription().
					getPredicateMap());
		
		else if(msgType.equals(MessageType.ADVERTISEMENT))
			return getFullPredicates(
					((AdvertisementMessage)msg).getAdvertisement().
					getPredicateMap());
		
		else if(msgType.equals(MessageType.UNADVERTISEMENT))
			return new TesterMessagePredicates();
		
		else if(msgType.equals(MessageType.UNSUBSCRIPTION))
			return new TesterMessagePredicates();

		else if(msgType.equals(MessageType.UNCOMPOSITESUBSCRIPTION))
			return new TesterMessagePredicates();
		
		else if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION))
			return new TesterMessagePredicates();
		else
			throw new UnsupportedOperationException(msgType.toString());
	}
	
	protected TesterMessagePredicates getFullPredicates(
			Map<String, Predicate> predMap) {
		TesterMessagePredicates predList = new TesterMessagePredicates();
		for(Entry<String, Predicate> entry : predMap.entrySet()) {
			Predicate pred = entry.getValue();
			Serializable val = (Serializable) pred.getValue();
			predList.addPredicate(entry.getKey(), pred.getOp(), val);
		}
		
		return predList;
	}
	
	protected TesterMessagePredicates getFullEuqlityPredicates(
			Map<String, Serializable> predMap) {
		TesterMessagePredicates predList = new TesterMessagePredicates();
		for(Entry<String, Serializable> entry : predMap.entrySet()) {
			Serializable value = entry.getValue();
			if(value.getClass().equals(String.class))
				predList.addPredicate(entry.getKey(), "eq", value);
			else
				predList.addPredicate(entry.getKey(), "=", value);
		}
		
		return predList;
	}

	@Override
	public void routerHandlingMessage(String brokerURI, Message msg) {
		MessageDestination msgDestination = msg.getLastHopID();
		MessageType msgType = msg.getType();
		
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				boolean doesItMatch = false;
				if(msgType.equals(MessageType.COMPOSITESUBSCRIPTION)) {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ROUTER_ADD_COMPOSITE_SUBSCRIPTION, msg.getType(),
							((CompositeSubscriptionMessage)msg).getSubscription(),
							msgDestination.getDestinationID());
				} else if(msgType.equals(MessageType.UNADVERTISEMENT)) {
//					System.out.println("HandingUnAdv: " + brokerURI + ":" + msg);
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ROUTER_ADD_UNADV, msg.getType(),
							getFullPredicates(msg),
							((UnadvertisementMessage) msg).getUnadvertisement().getAdvID(),
							msgDestination.getDestinationID());
				} else if(msgType.equals(MessageType.UNSUBSCRIPTION)) {
//					System.out.println("HandingUnSub: " + brokerURI + ":" + msg);
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ROUTER_ADD_UNSUB, msg.getType(),
							getFullPredicates(msg),
							((UnsubscriptionMessage) msg).getUnsubscription().getSubID(),
							msgDestination.getDestinationID());
				} else {
					doesItMatch = expectedEvent.match(
							brokerURI, BrokerEventType.ROUTER_HANDLE, msg.getType(),
							getFullPredicates(msg),
							msgDestination.getDestinationID());
				}
				
				if(doesItMatch) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + msg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public IBrokerTester expectRouterNotHandleUnsubscribe(
			String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String subId) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedUnsubscribeBrokerEvent(
							brokerURI,
							expectedMsgPredicates, subId,
							destination == null ? null : destination.getDestinationID(),
							true));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectRouterNotHandle(
			String brokerURI,
			MessageDestination destination,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_HANDLE, msgType,
							expectedMsgPredicates, destination.getDestinationID(), true));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterNotHandleUnAdvertise(
			String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String subId) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedUnadvertiseBrokerEvent(
							brokerURI,
							expectedMsgPredicates, subId,
							destination == null ? null : destination.getDestinationID(),
							true));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectRouterHandleUnAdvertise(
			String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String advId) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedUnadvertiseBrokerEvent(
							brokerURI,
							expectedMsgPredicates, advId,
							destination == null ? null : destination.getDestinationID(),
							false));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectRouterHandleUnsubscribe(
			String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String subId) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedUnsubscribeBrokerEvent(
							brokerURI,
							expectedMsgPredicates, subId,
							destination == null ? null : destination.getDestinationID(),
							false));
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public IBrokerTester expectRouterHandle(
			String brokerURI,
			MessageDestination destination,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_HANDLE, msgType,
							expectedMsgPredicates,
							destination == null ? null : destination.getDestinationID(),
							false));
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterHandle(
			String brokerURI,
			MessageDestination destination,
			MessageType msgType,
			String objectId) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_HANDLE, msgType,
							objectId, destination == null ? null : destination.getDestinationID(), false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterHandle(
			String brokerURI,
			MessageDestination destination,
			MessageType msgType,
			String objectId,
			TesterMessagePredicates expectedMsgPredicates) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
				new ExpectedBrokerEvent(
					brokerURI, BrokerEventType.ROUTER_HANDLE, msgType,
					objectId, expectedMsgPredicates,
					destination == null ? null : destination.getDestinationID(),
					false));
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public void clientReceivedPublication(String clientID,
			PublicationMessage pubMsg) {
//		if(pubMsg.getPublication().getClassVal().equals("historic"))
//			System.out.println(clientID + ":::" + pubMsg.getPublication());
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent.match(
						clientID, BrokerEventType.CLIENT_PUB_RECEIVE,
						pubMsg.getType(),
						getFullPredicates(pubMsg),
						null)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + pubMsg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}		
	}

	@Override
	public IBrokerTester expectClientReceivePublication(String clientID,
			TesterMessagePredicates expectedMsgPredicates) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							clientID,
							BrokerEventType.CLIENT_PUB_RECEIVE,
							MessageType.PUBLICATION,
							expectedMsgPredicates,
							null, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectClientNotReceivePublication(String clientID,
			TesterMessagePredicates expectedMsgPredicates) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							clientID,
							BrokerEventType.CLIENT_PUB_RECEIVE,
							MessageType.PUBLICATION,
							expectedMsgPredicates,
							null, true));
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterAddCompositeSubscription(
			String brokerURI, MessageDestination destination,
			CompositeSubscription cs) {
		synchronized (_expectedEvents) {
			String destinationId = destination == null ? null : destination.getDestinationID();
			_expectedEvents.add(
					new ExpectedCompositeSubscribeBrokerEvent(
							brokerURI, cs,
							destinationId, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}
	
	@Override
	public IBrokerTester expectRouterAddSubscription(String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates) {
		synchronized (_expectedEvents) {
			String destinationId = destination == null ? null : destination.getDestinationID();
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_ADD_SUB, MessageType.SUBSCRIPTION,
							expectedSubPredicates, destinationId, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterNotAddSubscription(String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates) {
		synchronized (_expectedEvents) {
			String destinationId = destination == null ? null : destination.getDestinationID();
			
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_ADD_SUB, MessageType.SUBSCRIPTION,
							expectedSubPredicates, destinationId, true));	
			_expectedEvents.notifyAll();
		}

		return this;
	}
	
	public IBrokerTester expectRouterAddAdvertisement(String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates) {
		synchronized (_expectedEvents) {
			String destinationId = destination == null ? null : destination.getDestinationID();
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_ADD_ADV, MessageType.ADVERTISEMENT,
							expectedSubPredicates, destinationId, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectRouterNotAddAdvertisement(String brokerURI,
			MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates) {
		synchronized (_expectedEvents) {
			String destinationId = destination == null ? null : destination.getDestinationID();
			
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.ROUTER_ADD_ADV, MessageType.ADVERTISEMENT,
							expectedSubPredicates, destinationId, true));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

//	Map<String, List<AdvertisementMessage>> _addedAdvertiments =
//		new HashMap<String, List<AdvertisementMessage>>();
	
	class MessageEventTypePair {
		final Message _msg;
		final BrokerEventType _type;
		
		MessageEventTypePair(Message msg, BrokerEventType type) {
			_msg = msg;
			_type = type;
		}
		
		@Override
		public String toString() {
			return _type + ":" + _msg;
		}
	}
	
	Map<String, List<MessageEventTypePair>> _addedSubscriptionsAdvertisements =
		new HashMap<String, List<MessageEventTypePair>>();
	
	@Override
	public void routerAddSubscription(String brokerURI, SubscriptionMessage subMsg) {
		Subscription sub = subMsg.getSubscription();
		String prefix = "ADDSUB";
		if(lookAtSubs)
			if(classSpecificMessages == null || sub.getClassVal().equals(classSpecificMessages))
				System.out.println(
						prefix + "(" + brokerURI + ")" +
						">>>" + subMsg);
		
		synchronized (_expectedEvents) {
			// Keep subscriptions that were inserted right after the broker starts and before
			// testcase declares its expected subscriptions.
			List<MessageEventTypePair> subMessages =
				_addedSubscriptionsAdvertisements.get(brokerURI);
			if(subMessages == null) {
				subMessages = new LinkedList<MessageEventTypePair>();
				_addedSubscriptionsAdvertisements.put(brokerURI, subMessages);
			}
			subMessages.add(new MessageEventTypePair(subMsg, BrokerEventType.ROUTER_ADD_SUB));
		
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				String msgDestination = subMsg == null ? null : subMsg.getLastHopID().getDestinationID();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.ROUTER_ADD_SUB, MessageType.SUBSCRIPTION,
						getFullPredicates(subMsg),
						msgDestination)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + subMsg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void routerAddCompositeSubscription(String brokerURI, CompositeSubscriptionMessage csMsg) {
		synchronized (_expectedEvents) {
			// Keep subscriptions that were inserted right after the broker starts and before
			// testcase declares its expected subscriptions.
			List<MessageEventTypePair> subMessages = _addedSubscriptionsAdvertisements.get(brokerURI);
			if(subMessages == null) {
				subMessages = new LinkedList<MessageEventTypePair>();
				_addedSubscriptionsAdvertisements.put(brokerURI, subMessages);
			}
			subMessages.add(
					new MessageEventTypePair(
							csMsg, BrokerEventType.ROUTER_ADD_COMPOSITE_SUBSCRIPTION));
		
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				String msgDestination = csMsg == null ? null : csMsg.getLastHopID().getDestinationID();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.ROUTER_ADD_COMPOSITE_SUBSCRIPTION,
						MessageType.COMPOSITESUBSCRIPTION,
						csMsg.getSubscription(),
						msgDestination)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + csMsg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void routerAddAdvertisement(String brokerURI, AdvertisementMessage advMsg) {
		synchronized (_expectedEvents) {
			// Keep advertisements that were inserted right after the broker starts and before
			// testcase declares its expected advertisement.
			List<MessageEventTypePair> advMessages = _addedSubscriptionsAdvertisements.get(brokerURI);
			if(advMessages == null) {
				advMessages = new LinkedList<MessageEventTypePair>();
				_addedSubscriptionsAdvertisements.put(brokerURI, advMessages);
			}
			advMessages.add(
					new MessageEventTypePair(
							advMsg, BrokerEventType.ROUTER_ADD_ADV));
		
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				String msgDestination = advMsg == null ? null : advMsg.getLastHopID().getDestinationID();
				if(expectedEvent.match(
						brokerURI,
						BrokerEventType.ROUTER_ADD_ADV,
						MessageType.ADVERTISEMENT,
						getFullPredicates(advMsg),
						msgDestination)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString() + " v. " + advMsg).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}		
	}
	
	protected void setError(boolean val) {
		_error = val;
	}

	@Override
	public IBrokerTester expectHandleResumeCommand(String brokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_RESUME,
							null, (String) null, null, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectHandleShutdownCommand(String brokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_SHUTDOWN,
							null, (String) null, null, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public IBrokerTester expectHandleStopCommand(String brokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_STOP,
							null, (String) null, null, false));	
			_expectedEvents.notifyAll();
		}

		return this;
	}

	@Override
	public void lifecyclemanagerHandleResume(String brokerURI) {
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_RESUME,
						null, null, (String) null, null)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString()).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void lifecyclemanagerHandleShutdown(String brokerURI) {
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_SHUTDOWN,
						null, null, (String) null, null)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString()).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}

	@Override
	public void lifecyclemanagerHandleStop(String brokerURI) {
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.LIFECICLEMANAGER_HANDLE_STOP,
						null, null, (String) null, null)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString()).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}
	
	@Override
	public void dbHandlerInitComplete(String brokerURI) {
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent.match(
						brokerURI, BrokerEventType.DB_HANLDER_INIT,
						null, null, (String) null, null)) {
					if(expectedEvent._negative) {
						setError(true);
						new IllegalStateException("Unexpected event happened: " + expectedEvent.toString()).printStackTrace();
					} else
						expectedEventsIt.remove();
				}
			}
			
			_expectedEvents.notifyAll();
		}
	}
	
	@Override
	public IBrokerTester expectDBHandlerInit(String brokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedBrokerEvent(
							brokerURI, BrokerEventType.DB_HANLDER_INIT,
							null, (String) null, null, false));	
			_expectedEvents.notifyAll();
		}
		
		return this;
	}

	@Override
	public IBrokerTester expectConnectionFail(String brokerURI, String toBrokerURI) {
		synchronized (_expectedEvents) {
			_expectedEvents.add(
					new ExpectedConnectFailedBrokerEvent(
							brokerURI, toBrokerURI, false));	
			_expectedEvents.notifyAll();
		}
		
		return this;
	}
	
	@Override
	public void connectFailed(String brokerURI, String toBrokerURI) {
		synchronized (_expectedEvents) {
			Iterator<ExpectedBrokerEvent> expectedEventsIt =
				_expectedEvents.iterator();
			while(expectedEventsIt.hasNext()) {
				ExpectedBrokerEvent expectedEvent = expectedEventsIt.next();
				if(expectedEvent._direction == BrokerEventType.CONNECT_FAILED) {
					if(((ExpectedConnectFailedBrokerEvent)expectedEvent).match(brokerURI, toBrokerURI)) {
						if(expectedEvent._negative) {
							setError(true);
							new IllegalStateException(
									"Unexpected event happened: " +
									expectedEvent.toString()).printStackTrace();
						} else {
							expectedEventsIt.remove();
						}
					}
				}
			}
			
			_expectedEvents.notifyAll();
		}	
	}

	@Override
	public void queueHandlerShutdown(String brokerURI, MessageDestination dest) {
		boolean matched = false;
		synchronized (_expectedEvents) {
			for(Iterator<SimpleExpectedEvent> expectedEventIt = _simpleExpectedEvents.iterator() ;
				expectedEventIt.hasNext() ; ) {
				SimpleExpectedEvent expectedEvent = expectedEventIt.next();
				if(!expectedEvent.match(SimpleExpectedEventType.QUEUE_HANDLER_SHUTDOWN, brokerURI))
					continue;
				
				ExpectedQueueHandlerShutdownEvent queueHandlerShutdownEvent =
					(ExpectedQueueHandlerShutdownEvent) expectedEvent;
				if(queueHandlerShutdownEvent.match(SimpleExpectedEventType.QUEUE_HANDLER_SHUTDOWN, brokerURI, dest)) {
					expectedEventIt.remove();
					matched = true;
				}
			}
		}
		if(!matched)
			System.err.println("No matching simple event: " + "queueHandlerShutdown::" + brokerURI + ":" + dest);
	}
	
	@Override
	public void queueHandlerStarted(String brokerURI, MessageDestination dest) {
		synchronized (_expectedEvents) {
			synchronized (_expectedEvents) {
				ExpectedQueueHandlerEvent queueHandlerEvent =
					new ExpectedQueueHandlerShutdownEvent(brokerURI, dest);
				_simpleExpectedEvents.add(queueHandlerEvent);
			}
		}
	}

	@Override
	public IBrokerTester expectCommSystemShutdown(String brokerURI, String serverAddress) {
		throw new UnsupportedOperationException("Expected shutdowns are automatically added when commSystem starts.");
	}
	
	@Override
	public IBrokerTester expectCommSystemStart(String brokerURI, String serverAddress) {
		synchronized (_expectedEvents) {
			SimpleExpectedEvent queueHandlerEvent =
				new ExpectedCommSystemStartEvent(brokerURI, serverAddress);
			_simpleExpectedEvents.add(queueHandlerEvent);
		}			

		return this;
	}

	
	@Override
	public void commSystemStarted(String brokerURI, String serverAddress) {
		synchronized (_expectedEvents) {
			for(Iterator<SimpleExpectedEvent> expectedEventIt = _simpleExpectedEvents.iterator() ;
				expectedEventIt.hasNext() ; )
			{
				SimpleExpectedEvent expectedEvent = expectedEventIt.next();
				if(!expectedEvent.match(SimpleExpectedEventType.COMM_SYSTEM_START, brokerURI))
					continue;
				
				ExpectedCommSystemStartEvent commSystemStartEvent =
					(ExpectedCommSystemStartEvent) expectedEvent;
				if(commSystemStartEvent.match(
						SimpleExpectedEventType.COMM_SYSTEM_START, brokerURI, serverAddress))
					expectedEventIt.remove();
			}
			
			SimpleExpectedEvent queueHandlerEvent =
				new ExpectedCommSystemShutdownEvent(brokerURI, serverAddress);
			_simpleExpectedEvents.add(queueHandlerEvent);
		}
	}
	
	@Override	
	public void commSystemShutdown(String brokerURI, String serverAddress) {
		boolean matched = false;
		synchronized (_expectedEvents) {
			for(Iterator<SimpleExpectedEvent> expectedEventIt = _simpleExpectedEvents.iterator() ;
				expectedEventIt.hasNext() ; ) {
				SimpleExpectedEvent expectedEvent = expectedEventIt.next();
				if(!expectedEvent.match(SimpleExpectedEventType.COMM_SYSTEM_SHUTDOWN, brokerURI))
					continue;
				
				ExpectedCommSystemShutdownEvent commSystemShutdownEvent =
					(ExpectedCommSystemShutdownEvent) expectedEvent;
				if(commSystemShutdownEvent.match(SimpleExpectedEventType.COMM_SYSTEM_SHUTDOWN, brokerURI, serverAddress)) {
					expectedEventIt.remove();
					matched = true;
				}
			}
		}
		
		if(!matched)
			System.err.println("No matching simple event: " + "queueHandlerShutdown::" + brokerURI + ":" + serverAddress);
	}
	
	public boolean waitUntilExpectedStartsHappen(
			int waitMillis, boolean printErrors) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		
		synchronized (_expectedEvents) {
			do {
				boolean startPending = false;
				for(SimpleExpectedEvent sEvent : _simpleExpectedEvents) {
					switch(sEvent._type) {
					case COMM_SYSTEM_START:
					case QUEUE_HANDLER_START:
						startPending = true;
						break;
					}
				}
				if(startPending)
					_expectedEvents.wait(500);
				else
					return true;
			} while((waitMillis - (System.currentTimeMillis() - startTime) > 0));
			
			throw new IllegalStateException("Some start events did not happen: " + _simpleExpectedEvents);
		}
	}
	
	public boolean waitUntilExpectedShutdownHappen(
			int waitMillis, boolean printErrors) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		
		synchronized (_expectedEvents) {
			while((_simpleExpectedEvents.size() != 0) &&
					(waitMillis - (System.currentTimeMillis() - startTime) > 0)) {
				_expectedEvents.wait(500);
			}
			
			if(_simpleExpectedEvents.size() != 0)
				throw new IllegalStateException("Some simple events did not happen: " + _simpleExpectedEvents);

			return true;
		}
	}
}
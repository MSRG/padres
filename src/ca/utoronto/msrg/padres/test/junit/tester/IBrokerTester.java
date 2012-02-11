package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;

/**
 * The main interface for Padres test framework. Implementing classes are
 * responsible to examine expected events/unexpected that must/must not take
 * place during a given testcase. An "expected event" is treated positively,
 * i.e., test must fail if the event does not occur. An "unexpected event"
 * is treated negatively, i.e., test must succeed only if the tester did not
 * receive a call (after a timeout) indicating occurrence of the event.
 * 
 * Examples of these events are send/receipt of a given message at a broker
 * or client, updates to routing tables of brokers, or enqueue/dequeue
 * operation on various message queues.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 * 
 */

public interface IBrokerTester {
	
	/**
	 * Called by testcase to examine status of expected/unexpected events.
	 * 
	 * @param wait time in milliseconds
	 * @return true for a successful taking place of expected events or false
	 * indicating failure (i.e., some expected events did not occur or some
	 * unexpected event did occur).
	 * @see waitUntilExpectedEventsHappen()
	 * 
	 */
	public boolean waitUntilExpectedEventsHappen(int waitMillis)
		throws InterruptedException;

	/**
	 * Called by testcase to examine status of expected/unexpected events.
	 * Similar to waitUntilExpectedEventsHappen() but uses a default timeout.
	 * 
	 * @return true for a successful taking place of expected events or false
	 * indicating failure (i.e., some expected events did not occur or some
	 * unexpected event did occur).
	 * @see waitUntilExpectedEventsHappen(int)
	 * 
	 */
	public boolean waitUntilExpectedEventsHappen() throws InterruptedException;

	
	/**
	 * Called by testcase to examine start status of brokers. Call returns
	 * when either a default timeout value is passed or when the registered
	 * brokers all indicate that they have successfully started.
	 * 
	 * @return true if all registered brokers are started and false if such
	 * events do not occur within the default timeout.
	 * 
	 */
	public boolean waitUntilExpectedStartsHappen() throws InterruptedException;

	
	/**
	 * Called by testcase to examine shutdown status of brokers. Call returns
	 * when either a default timeout value is passed or when the registered
	 * brokers all indicate that they have successfully shutdown.
	 * 
	 * @return true if all registered brokers are started and false if such
	 * events do not occur within the default timeout.
	 * @see expectHandleShutdownCommand
	 * 
	 */
	public boolean waitUntilExpectedShutdownHappen()
		throws InterruptedException;

	
	/**
	 * Registers an expected event for broker LifeCycleManager component
	 * regarding handling of a shutdown command.
	 * 
	 * @param URI string of the broker tested
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectHandleShutdownCommand(String brokerURI);

	
	/**
	 * Registers an expected event for broker LifeCycleManager component 
	 * regarding handling of a resume command.
	 * 
	 * @param URI string of the broker tested
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectHandleResumeCommand(String brokerURI);
	
	
	/**
	 * Registers an expected event for broker LifeCycleManager component
	 * regarding handling of a stop command.
	 * 
	 * @param URI string of the broker tested
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectHandleStopCommand(String brokerURI);
	
	
	/**
	 * Called from within the LifeCycleManager component upon handling of a
	 * shutdown command.
	 * 
	 * @param URI string of the broker tested
	 * 
	 */
	public void lifecyclemanagerHandleShutdown(String brokerURI);

	
	/**
	 * Called from within the LifeCycleManager component upon handling of a
	 * stop command.
	 * 
	 * @param URI string of the broker tested
	 * 
	 */
	public void lifecyclemanagerHandleStop(String brokerURI);
	
	
	/**
	 * Called from within the LifeCycleManager component upon handling of a
	 * resume command.
	 * 
	 * @param URI string of the broker tested
	 * 
	 */
	public void lifecyclemanagerHandleResume(String brokerURI);
	
	/**
	 * Registers an expected event for broker's database registration.
	 * 
	 * @param URI string of the broker tested
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectDBHandlerInit(String brokerURI);
	
	
	/**
	 * Called from within a broker upon completion of initialization of its
	 * database.
	 * 
	 * @param URI string of the broker being tested
	 * 
	 */
	public void dbHandlerInitComplete(String brokerURI);
	

	/**
	 * Registers an expected event for failure of a broker's connection.
	 * 
	 * @param URI string of the broker tested
	 * @param URI string of the remote endpoint of the connection
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectConnectionFail(
			String brokerURI, String toBrokerURI);

	
	/**
	 * Called from within a broker's communication layer upon failure of its
	 * connection to a remote URI.
	 * 
	 * @param URI string of the broker being tested
	 * @param URI string of the remote endpoint of the connection
	 * 
	 */
	public void connectFailed(String brokerURI, String toURI);
	
	
	/**
	 * Called from within a broker's QueueHandler component upon shutdown.
	 * 
	 * @param URI string of the broker being tested
	 * @param message destination of the queue
	 * 
	 */
	public void queueHandlerShutdown(
			String brokerURI, MessageDestination dest);

	
	/**
	 * Called from within a broker's QueueHandler component upon start.
	 * 
	 * @param URI string of the broker being tested
	 * @param message destination of the queue
	 * 
	 */
	public void queueHandlerStarted(
			String brokerURI, MessageDestination dest);
	

	/**
	 * Registers an expected event for broker's communication layer shutdown.
	 * 
	 * @param URI string of the broker tested
	 * @param the communication layer's listening address
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectCommSystemShutdown(
			String brokerURI, String serverAddress);

	
	/**
	 * Registers an expected event for broker's communication layer start.
	 * 
	 * @param URI string of the broker tested
	 * @param the communication layer's listening address
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectCommSystemStart(
			String brokerURI, String serverAddress);
	
	
	/**
	 * Called from within a broker's communication layer upon shutdown.
	 * 
	 * @param URI string of the broker being tested
	 * @param the communication layer's listening address
	 * 
	 */
	public void commSystemShutdown(String brokerURI, String serverAddress);

	
	/**
	 * Called from within a broker's communication layer upon start.
	 * 
	 * @param URI string of the broker being tested
	 * @param the communication layer's listening address
	 * 
	 */
	public void commSystemStarted(String brokerURI, String serverAddress);
	
	
	/**
	 * Registers an expected event for broker MessageQueue component regarding
	 * receipt of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param destination queue
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectReceipt(
			String brokerURI, MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String queueDestination);
	
	
	/**
	 * Registers an expected event for broker MessageQueue component regarding
	 * receipt of a message. Used for unsubscriptions.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param identifier of the message (e.g., unubscription)
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectReceipt(String brokerURI, MessageType msgType,
			String objectId, String queueDestination);
	
	
	/**
	 * Registers an expected event for broker MessageQueue component regarding
	 * sending of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param destination queue
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectSend(String brokerURI, MessageType msgType,
		TesterMessagePredicates expectedMsgPredicates, String remoteBrokerURI);

	
	/**
	 * Registers an expected event for broker MessageQueue component regarding
	 * sending of a message. Used for unsubscriptions.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param identifier of the message (e.g., unubscription)
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectSend(String brokerURI, MessageType msgType,
			String objectId, String remoteBrokerURI);
	
	
	/**
	 * Registers unexpected event for broker MessageQueue component regarding
	 * receipt of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param destination queue
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectNegativeReceipt(
			String brokerURI, MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String queueDestination);
	
	/**
	 * Registers unexpected event for broker MessageQueue component regarding
	 * receipt of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param type of message
	 * @param predicates in the message
	 * @param destination queue
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectNegativeSend(
			String brokerURI, MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates,
			String remoteBrokerURI);


	/**
	 * Called from within the MessageQueue component upon enqueue of a message.
	 * 
	 * @param TesterMessageQueue object executing the enqueue operation
	 * @param message that is being enqueued
	 * 
	 */
	public void msgEnqueued(TesterMessageQueue msgQueue, Message msg);

	
	/**
	 * Called from within the MessageQueue component upon dequeue of a message.
	 * 
	 * @param TesterMessageQueue object executing the dequeue operation
	 * @param message that is being dequeued
	 * 
	 */
	public void msgDequeued(TesterMessageQueue msgQueue, Message msg);
	
	
	/**
	 * Called from within the MessageSender component upon sending of a message.
	 * 
	 * @param ITestMessageSender object executing the send operation
	 * @param message that is sent
	 * 
	 */
	public void msgSent(ITestMessageSender messageSender, Message msg);

	
	/**
	 * Called upon receipt of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param message that is received
	 * 
	 */
	public void msgReceived(String brokerURI, Message msg);


	/**
	 * Registers an expected event for broker's Router component regarding
	 * handling of an unadvertisement.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param predicates in the message
	 * @param identifier of the unadvertisement message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterHandleUnAdvertise(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String advId);

	
	/**
	 * Registers an unexpected event for broker's Router component regarding
	 * handling of an unadvertisement.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param predicates in the message
	 * @param identifier of the unadvertisement message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterNotHandleUnAdvertise(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String advId);

	/**
	 * Registers an expected event for broker's Router component regarding
	 * handling of an unsubscription.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param predicates in the message
	 * @param identifier of the unsubscription message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterHandleUnsubscribe(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String subId);

	
	/**
	 * Registers an unexpected event for broker's Router component regarding
	 * handling of an unsubscription.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param predicates in the message
	 * @param identifier of the unsubscription message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterNotHandleUnsubscribe(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedMsgPredicates,
			String subId);

	/**
	 * Registers an expected event for broker's Router component regarding
	 * handling of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param type of the message being handled
	 * @param predicates in the message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterHandle(
			String brokerURI, MessageDestination destination,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates);
	
	/**
	 * Registers an expected event for broker's Router component regarding
	 * handling of an unsubscription/unadvertisement message.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param type of the message being handled
	 * @param identifier of the unsubscription/unadvertisement message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterHandle(
			String brokerURI, MessageDestination destination,
			MessageType msgType,
			String objectId);
	
	/**
	 * Registers an expected event for broker's Router component regarding
	 * handling of an unsubscription/unadvertisement message.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param type of the message being handled
	 * @param identifier of the unsubscription/unadvertisement message
	 * @param predicates in the message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterHandle(
			String brokerURI, MessageDestination destination,
			MessageType msgType,
			String objectId,
			TesterMessagePredicates expectedMsgPredicates);
	
	
	/**
	 * Registers an unexpected event for broker's Router component regarding
	 * handling of an unsubscription/unadvertisement message.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param type of the message being handled
	 * @param identifier of the unsubscription/unadvertisement message
	 * @param predicates in the message
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterNotHandle(
			String brokerURI, MessageDestination destination,
			MessageType msgType,
			TesterMessagePredicates expectedMsgPredicates);
	
	
	/**
	 * Registers an expected event for broker's Router component regarding
	 * addition of a composite subscription.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the message
	 * @param the CompositeSubscription
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterAddCompositeSubscription(
			String brokerURI, MessageDestination destination,
			CompositeSubscription cs);

	
	/**
	 * Registers an expected event for broker's Router component regarding
	 * addition of a subscription.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the subscription
	 * @param predicates in the subscription
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterAddSubscription(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates);

	
	/**
	 * Registers an unexpected event for broker's Router component regarding
	 * addition of a subscription.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the subscription
	 * @param predicates in the subscription
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterNotAddSubscription(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates);
	
	/**
	 * Registers an expected event for broker's Router component regarding
	 * addition of an advertisement.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the advertisement
	 * @param predicates in the advertisement
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterAddAdvertisement(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedAdvPredicates);

	
	/**
	 * Registers an unexpected event for broker's Router component regarding
	 * addition of an advertisement.
	 * 
	 * @param URI string of the broker tested
	 * @param destination of the advertisement
	 * @param predicates in the advertisement
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectRouterNotAddAdvertisement(
			String brokerURI, MessageDestination destination,
			TesterMessagePredicates expectedSubPredicates);

	
	/**
	 * Called from within the Router component upon handling of a message.
	 * 
	 * @param URI string of the broker tested
	 * @param message being handled
	 * 
	 */
	public void routerHandlingMessage(String brokerURI, Message msg);

	
	/**
	 * Called from within the Router component upon adding of an
	 * advertisement message.
	 * 
	 * @param URI string of the broker tested
	 * @param the AdvertisementMessage being handled
	 * 
	 */
	public void routerAddAdvertisement(
			String brokerURI, AdvertisementMessage advMsg);

	
	/**
	 * Called from within the Router component upon adding of an
	 * subscription message.
	 * 
	 * @param URI string of the broker tested
	 * @param the SubscriptionMessage being handled
	 * 
	 */
	public void routerAddSubscription(
			String brokerURI, SubscriptionMessage subMsg);
	
	
	/**
	 * Called from within the Router component upon adding of an
	 * composite subscription message.
	 * 
	 * @param URI string of the broker tested
	 * @param the CompositeSubscriptionMessage being handled
	 * 
	 */
	public void routerAddCompositeSubscription(
			String brokerURI, CompositeSubscriptionMessage csMsg);
	

	/**
	 * Registers an expected event for a client regarding receipt of a
	 * publication.
	 * 
	 * @param URI string of the client being tested
	 * @param predicates in the publication
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectClientReceivePublication(
			String clientURI, TesterMessagePredicates expectedMsgPredicates);

	
	/**
	 * Registers an unexpected event for a client regarding receipt of a
	 * publication.
	 * 
	 * @param URI string of the client being tested
	 * @param predicates in the publication
	 * @return the IBrokerTester (this) object
	 * 
	 */
	public IBrokerTester expectClientNotReceivePublication(
			String clientURI, TesterMessagePredicates expectedMsgPredicates);
	
	/**
	 * Called from within a client upon receipt of a publication.
	 * 
	 * @param URI string of the client being tested
	 * @param predicates in the publication
	 * 
	 */
	public void clientReceivedPublication(
			String clientURI, PublicationMessage pubMsg);
}
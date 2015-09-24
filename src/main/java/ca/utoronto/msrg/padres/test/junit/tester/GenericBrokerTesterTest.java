package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import junit.framework.TestCase;

/**
 * A testcase for the Padres test framework! 
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class GenericBrokerTesterTest extends TestCase {

	static final int DEFAULT_WAIT_TIME = 1000;
	
	GenericBrokerTester _brokerTester;
	String brokerURI0 = "socket://127.0.0.1:8000/broker0";
	String brokerURI1 = "socket://127.0.0.1:8000/broker1";
	String brokerDestStr = "broker2";
	MessageDestination brokerDest2 = new MessageDestination(brokerDestStr);
	String brokerDestStr3 = "broker3";
	MessageDestination brokerDest3 = new MessageDestination(brokerDestStr3);
	
	TesterMessagePredicates messagePredicates100 =
		new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price", "=", 100L);
	TesterMessagePredicates messagePredicates200 =
		new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("price", "=", 200L);
	
	Subscription sub100 =
		new Subscription().
			addPredicate("class", new Predicate("eq", "stock")).
			addPredicate("price", new Predicate("=", 100L));
	Subscription sub200 =
		new Subscription().
			addPredicate("class", new Predicate("eq", "stock")).
			addPredicate("price", new Predicate("=", 200L));
	Advertisement adv100 =
		new Advertisement().
			addPredicate("class", new Predicate("eq", "stock")).
			addPredicate("price", new Predicate("=", 100L));
	Advertisement adv200 =
		new Advertisement().
			addPredicate("class", new Predicate("eq", "stock")).
			addPredicate("price", new Predicate("=", 200L));
	Publication pub100 =
		new Publication().
			addPair("class", "stock").
			addPair("price", 100L);
	Publication pub200 =
		new Publication().
			addPair("class", "stock").
			addPair("price", 200L);

	@Override
	public void setUp() {
		_brokerTester = new GenericBrokerTester();
	}
	
	@Override
	public void tearDown() {
		_brokerTester = null;
	}
	
	public void testExpectRouterAddSubscription() throws InterruptedException {
		// Wait for subscription with price 100
		_brokerTester.
			expectRouterAddSubscription(
				brokerURI1,
				brokerDest2,
				messagePredicates100);
		
		// No events happened yet.
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));
		
		// Only an advertisement with equal predicates added.
		_brokerTester.
			routerAddAdvertisement(
				brokerURI1,
				new AdvertisementMessage(adv100, "ADV100"));
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));
		
		// An advertisement with unequal predicates added.
		_brokerTester.
		routerAddAdvertisement(
			brokerURI1,
			new AdvertisementMessage(adv200, "ADV200"));
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));
		
		// A subscription with different predicates added.
		_brokerTester.
			routerAddSubscription(
				brokerURI1,
				new SubscriptionMessage(sub200, "SUB200"));
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));

		// A subscription with equal predicates added but different destination.
		_brokerTester.
			routerAddSubscription(
				brokerURI1,
				(SubscriptionMessage) new SubscriptionMessage(sub100, "SUB100").
					setLastHopID(brokerDest3));
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));
		
		// A subscription with equal predicates added but different broker.
		_brokerTester.
		routerAddSubscription(
			brokerURI0,
			(SubscriptionMessage) new SubscriptionMessage(sub100, "SUB100").
				setLastHopID(brokerDest2));
		assertFalse(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME, false));	
		
		// A subscription with equal predicates added to the right broker.
		_brokerTester.
		routerAddSubscription(
			brokerURI1,
			(SubscriptionMessage) new SubscriptionMessage(sub100, "SUB100").
				setLastHopID(brokerDest2));
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TIME));	
	}
	
}

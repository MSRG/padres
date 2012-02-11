package ca.utoronto.msrg.padres.test.junit;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.controller.OverlayRoutingTable;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Unadvertisement;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Unsubscription;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterClient;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test in the scenario of one broker with multiple Clients.
 * 
 * @author Shuang Hou, Bala Maniymaran, and Vinod Muthusamy
 */

public class TestClients extends TestCase {

	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore brokerCore;

	protected Client clientA;

	protected Client clientB;

	protected Client clientC;

	private MessageWatchAppender messageWatcher;

	private PatternFilter msgFilter;
	
	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// start the broker
		brokerCore = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore.initialize();
		// start the clientA
		clientA = createNewClient(AllTests.clientConfigA);
		clientA.connect(brokerCore.getBrokerURI());
		clientB = createNewClient(AllTests.clientConfigB);
		clientB.connect(brokerCore.getBrokerURI());
		clientC = createNewClient(AllTests.clientConfigC);
		clientC.connect(brokerCore.getBrokerURI());
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(Client.class.getName());
		msgFilter.setPattern(".*Client " + clientA.getClientID() + ".+Publication.+stock.+");
		// msgFilter.setPattern(".*Publication.+stock.+");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		brokerCore.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}
	
	protected Client createNewClient(ClientConfig newConfig) throws ClientException {
		return new TesterClient(_brokerTester, newConfig);
	}

	/**
	 * Test wingRmiClient connects to the broker correctly.
	 */
	public void testConnectionWithClients() {
		OverlayRoutingTable ort = brokerCore.getOverlayManager().getORT();
		MessageDestination expectedMdA = clientA.getClientDest();
		MessageDestination expectedMdB = clientB.getClientDest();
		assertTrue("The SwingClientA is not connected to the broker correctly",
				ort.isClient(expectedMdA));
		assertTrue("The SwingClientB is not connected to the broker correctly",
				ort.isClient(expectedMdB));
	}

	/**
	 * Test pub/sub matched on one client.
	 * @throws ParseException 
	 */
	public void testPubSubMatchOnOneClient() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100.3]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100.3]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100.3]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched, but not",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test pub/sub not matched on one client.
	 * @throws ParseException 
	 */
	public void testPubSubNotMatchOnOneClient() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication [class,'stock'],[price,120] should not be matched ",
				expectedPub);
	}

	/**
	 * Test pub/sub matched on two clients.
	 * @throws ParseException 
	 */
	public void testPubSubMatchOnTwoClients() throws ParseException {
		// clientB is publisher, clientA is subscriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,100] should be matched at clientB, but not",
				expectedPub.equalVals(pub));
	}

	/**
	 * Test pub/sub not matched on two clients.
	 * @throws ParseException 
	 */
	public void testPubSubNotMatchOnTwoClients() throws ParseException {
		// clientB is publisher, clientA is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication [class,'stock'],[price,120] should not be matched at clientB ",
				expectedPub);
	}

	/**
	 * Test pub/sub match with messages injected from swingClient, not from broker.
	 * @throws ParseException 
	 */
	public void testPubSubMatchInjectedFromSwingClients() throws ParseException {
		// clientA is publisher, clientB is subscriber
		clientB.handleCommand("a [class,eq,'stock'],[price,=,100]");
		clientA.handleCommand("s [class,eq,'stock'],[price,=,100]");
		clientB.handleCommand("p [class,'stock'],[price,100]");

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,100] should be matched at clientB, but not",
				expectedPub.equalVals(pub));
	}

	/**
	 * Test publication has same attribute set with the advertisement.
	 * @throws ParseException 
	 */
	public void testPubHavingSameAttributeSetWithAdv() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,90]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched, but not",
				expectedPub.equalVals(pub));
	}

	/**
	 * Test publication has less attribute set than the advertisement
	 * @throws ParseException 
	 */
	public void testPubHavingLessAttributeSetThanAdv() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100],[amount,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched, but not",
				expectedPub.equalVals(pub));
	}

	/**
	 * Test subscription has the same attribute set with the publication.
	 * @throws ParseException 
	 */
	public void testSubHavingSameAttributeSetWithPub() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched, but not",
				expectedPub.equalVals(pub1));

		// get not matched
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,80] should not be matched ",
				expectedPub.equalVals(pub2));
	}

	/**
	 * Test subscription has more attribute set than the publication.
	 * @throws ParseException 
	 */
	public void testSubHavingMoreAttributeSetThanPub() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100],[amount,>,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched", expectedPub);
	}

	/**
	 * Test subscription has less attribute set than the publication.
	 * @throws ParseException 
	 */
	public void testSubHavingLessAttributeSetThanPub() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[price,isPresent,100],[amount,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120],[amount,100]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,120],[amount,100] should be matched, but not",
				expectedPub.equalVals(pub1));

		// get not matched
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80],[amount,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientA.getCurrentPub();
		assertFalse(
				"The publication:[class,'stock'],[price,80],[amount,100] should not be matched ",
				expectedPub.equalVals(pub2));
	}

	/**
	 * Test all subscription's predicates have overlap with the advertisement's predicates.
	 * @throws ParseException 
	 */

	public void testAllSubPredicatesHavingOverlapWithAdv() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100],[amount,<,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,60],[amount,<,60]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[amount,40]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,40],[amount,40] should be matched, but not",
				expectedPub.equalVals(pub1));

		// get not matched
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80],[amount,80]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientA.getCurrentPub();
		assertFalse(
				"The publication:[class,'stock'],[price,80],[amount,80] should not be matched ",
				expectedPub.equalVals(pub2));
	}

	/**
	 * Test not all subscription's predicates have overlap with the advertisement's predicates, and
	 * one non-overlap predicate exists.
	 * @throws ParseException 
	 */
	public void testNotAllSubPredicatesHavingOverlapWithAdv() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100],[amount,<,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100],[amount,<,60]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[amount,40]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,40], [amount,40] should not be matched",
				expectedPub);
	}

	/**
	 * Test all subscription's predicates do not have overlap with the advertisement's predicates at
	 * all.
	 * @throws ParseException 
	 */
	public void testAllSubPredicatesNotHavingOverlapWithAdv() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100],[amount,<,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'ibm'],[price,>,100],[amount,>,100]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[amount,40]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,40], [amount,40] should not be matched",
				expectedPub);
	}

	/**
	 * Test adv with "<" and sub with "<".
	 * @throws ParseException 
	 * 
	 * 
	 */
	public void testAdvWithLessThanOperatorSubWithLessThanOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,60] should be matched",
				expectedPub.equalVals(pub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client " + clientB.getClientID() + ".+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get matched on clientB, no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				expectedPub1.equalVals(pub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,80] should not be matched",
				expectedPub2.equalVals(pub1));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get matched on clientC,B, no match on clientA
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		boolean checkB = false, checkC = false;
		int waitCount = 0;
		while (!(checkB && checkC) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkB)
					checkB = msg.contains("Client " + clientB.getClientID());
				if (!checkC)
					checkC = msg.contains("Client " + clientC.getClientID());
			}
		}
		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				expectedPub3.equalVals(pub2));
		Publication expectedPub4 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub2.equalVals(expectedPub4));
		Publication expectedPub5 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,90] should not be matched",
				expectedPub5.equalVals(pub2));
	}

	/**
	 * Test adv with "<" and sub with "<=.
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithLessThanOrEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// setup filter
		msgFilter.setPattern(".*" + clientB.getClientID() + ".+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get matched on clientB, no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub1.equalVals(expectedPub1));
		Publication expectedPub2 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,90] should not be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// setup filter
		msgFilter.setPattern(".*Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get matched on clientB,C , no match on clientA
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,95]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		boolean checkB = false, checkC = false;
		int waitCount = 0;
		while (!(checkB && checkC) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkB)
					checkB = msg.contains("Client " + clientB.getClientID());
				if (!checkC)
					checkC = msg.contains("Client " + clientC.getClientID());
			}
			waitCount++;
		}
		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,95] should be matched",
				pub2.equalVals(expectedPub3));
		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,95] should be matched",
				pub2.equalVals(expectedPub4));
		Publication expectedPub5 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,95] should not be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<" and sub with ">".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithGreaterThanOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,80] should not be matched",
				pub1.equalVals(expectedPub1));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub2 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,60] should not be matched",
				pub2.equalVals(expectedPub2));
	}

	/**
	 * Test adv with "<" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithGreaterThanOrEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clintB, get matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub1.equalVals(expectedPub2));
		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub1);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on both client
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub3 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,60] should not be matched",
				pub2.equalVals(expectedPub3));
		Publication expectedPub4 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,60] should not be matched", expectedPub4);
	}

	/**
	 * Test adv with "<" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,90] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub1.equalVals(expectedPub1));
		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub2);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on both
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub3 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,60] should not be matched",
				pub2.equalVals(expectedPub3));
		Publication expectedPub4 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,60] should not be matched", expectedPub4);
	}

	/**
	 * Test adv with "<" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithNotEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client " + clientB.getClientID() + ".+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get not matched on clientA, get matched on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub1.equalVals(expectedPub1));
		Publication expectedPub2 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,80] should not be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		messageWatcher.clearFilters();
		messageWatcher.addFilter(msgFilter);
		// get matched on clientA,B,C
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		boolean checkA = false, checkB = false, checkC = false;
		int waitCount = 0;
		while (!(checkA && checkB && checkC) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkA)
					checkA = msg.contains("Client " + clientA.getClientID());
				if (!checkB)
					checkB = msg.contains("Client " + clientB.getClientID());
				if (!checkC)
					checkC = msg.contains("Client " + clientC.getClientID());
			}
			waitCount++;
		}
		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,60] should be matched",
				pub2.equalVals(expectedPub3));
		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,60] should be matched",
				pub2.equalVals(expectedPub4));
		Publication expectedPub5 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,60] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "<=" and sub with "<".
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithLessThanOperator() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		_brokerTester.expectRouterAddAdvertisement(
				brokerCore.getBrokerURI(),
				mdA,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<=", 100L));
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(),
				mdA,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 80L));
		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// get not matched on clientA
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock"));
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		assertTrue("The publication:[class,'stock'],[price,80] should not be matched", _brokerTester.waitUntilExpectedEventsHappen());

		
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(),
				mdB,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 100L));
		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// get not matched on clientA, get matched on clientB
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 90L)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock"));
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		
		_brokerTester.clearAll().
			expectRouterAddSubscription(
				brokerCore.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "<", 120L));
		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// get matched on clientC, get not matched on others
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectClientNotReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L)).
			expectClientReceivePublication(
				clientC.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 100L));
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv with "<=" and sub with "<=".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithLessThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));
		messageWatcher.clear();

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client " + clientB.getClientID() + ".+Publication.+stock.+");
		// get not matched on clientA, get matched on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub1.equalVals(expectedPub1));
		Publication expectedPub2 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,90] should not be matched",
				pub1.equalVals(expectedPub2));
		messageWatcher.clear();

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset the filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get not matched on clientA, get matched on others
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub3));
		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,100] should not be matched",
				pub2.equalVals(expectedPub4));
		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<=" and sub with ">".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithGreaterThanOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientB, get matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub1.equalVals(expectedPub));

		expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,90] should not be matched", expectedPub);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on others
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub4);

		Publication expectedPub5 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should be matched", expectedPub5);
	}

	/**
	 * Test adv with "<=" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithGreaterThanEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientB, get matched on clientA
		pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub.equalVals(expectedPub));

		expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,90] should not be matched", expectedPub);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientA,B, no match on clientC
		pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		expectedPub = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched", expectedPub);
	}

	/**
	 * Test adv with "<=" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,90] should not be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,90] should not be matched", expectedPub2);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-setup message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,100] should not be matched",
				pub2.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "<=" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithNotEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,90]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,90] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get ont matched on clientB, match on clientA,C
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,100] should not be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<=" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithLessThanEqualToOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<=,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with ">" and sub with "<".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithLessThanOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,130]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,130] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get ont matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub1);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-setup message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get not matched on clientA, match on clientB
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched",
				expectedPub4);
	}

	/**
	 * Test adv with ">" and sub with "<="
	 * @throws ParseException.
	 */
	public void testAdvWithGreaterThanOperatorSubWithLessThanEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,115]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,115] should not be matched",
				expectedPub1);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get not matched on clientA, match on clientB
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub4);
	}

	/**
	 * Test adv with ">" and sub with ">".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithGreaterThanOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,115]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,115] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,115] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on clientC, match on clientA,B
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub5);
	}

	/**
	 * Test adv with ">" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithGreaterThanEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on all
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,130]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,130] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,130] should be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,130] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with ">" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,115]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,115] should not be matched",
				expectedPub1);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-setup message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientC
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub3));
	}

	/**
	 * Test adv with ">" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithNotEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,115]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,115] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,115] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA,B, no match on clientC
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with ">" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with ">=" and sub with "<".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithLessThanOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched",
				expectedPub1);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-setup message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get not matched on clientA, match on clientB
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub2.equalVals(expectedPub4));
	}

	/**
	 * Test adv with ">=" and sub with "<=".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithLessThanEqualToOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-setup message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get not matched on clientA, match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on both
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,130]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,130] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,130] should not be matched",
				pub2.equalVals(expectedPub4));
	}

	/**
	 * Test adv with ">=" and sub with ">".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithGreaterThanOperator() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on clientC, match on clientA,B
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub5));
	}

	/**
	 * Test adv with ">=" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithGreaterThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA,B, no match no clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub2);

		// get matched on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub5));
	}

	/**
	 * Test adv with ">=" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get not matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,110] should not be matched", expectedPub);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNotNull("The publication:[class,'stock'],[price,100] should be matched", expectedPub1);
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub1.equalVals(expectedPub1));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub2.equalVals(expectedPub4));
	}

	/**
	 * Test adv with ">=" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithNotEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub1.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,110] should be matched",
				pub1.equalVals(expectedPub2));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA,B, no match on clientC
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub2.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub5);
	}

	/**
	 * Test adv with ">=" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithGreaterThanEqualToOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>=,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "=" and sub with "<".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithLessThanOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get no matched on clientA, match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);
	}

	/**
	 * Test adv with "=" and sub with "<=".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithLessThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get no matched on clientA, match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub2));
	}

	/**
	 * Test adv with "=" and sub with ">".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithGreaterThanOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no matched on clientB, match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);
	}

	/**
	 * Test adv with "=" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithGreaterThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get no matched on clientC, match on clientA,B
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub2));
	}

	/**
	 * Tests if the bug with advertisement conformance checking is fixed. This bug does not happen
	 * for all publications, but is reproduceable.
	 * @throws ParseException 
	 */
	public void testAdvConformanceChecking() throws ParseException {
		int noOfPubsToTest = 20;
		String stockSymbol = "YHOO";
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'STOCK'],[open,isPresent,37.85],[symbol,eq,'YHOO'],"
						+ "[volume,isPresent,11291000],[high,isPresent,37.99],[low,isPresent,37.65],"
						+ "[date,isPresent,'28-Dec-04'],[close,isPresent,37.9]");

		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'STOCK']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+STOCK.+");
		// Read publications from stockquote file
		String line = null;
		String stockquotePath = BrokerConfig.PADRES_HOME + "demo/data/stockquotes/" + stockSymbol;

		try {
			FileReader fr = new FileReader(stockquotePath);
			BufferedReader in = new BufferedReader(fr);

			for (int i = 0; i < noOfPubsToTest && (line = in.readLine()) != null; i++) {
				line = line.trim();

				// Create the publication object
				Publication pub = MessageFactory.createPublicationFromString(line);
				PublicationMessage pubMsg = new PublicationMessage(pub,
						brokerCore.getNewMessageID(), mdA);
				brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

				// waiting for routing finished
				messageWatcher.getMessage(5);

				Publication expectedPub = clientB.getCurrentPub();
				assertTrue("The publication: " + pub + " should be matched",
						pub.equalVals(expectedPub));
			}

			in.close();
			fr.close();

		} catch (FileNotFoundException e1) {
			System.out.println("Failed: Stockquote file '" + stockquotePath + "' not found.");
			assertTrue(e1.getMessage(), false);
		} catch (IOException e2) {
			System.out.println("Failed: Exception is:\n" + e2.getMessage() + "\n");
			assertTrue(e2.getMessage(), false);
		}
	}

	/**
	 * Test adv with "=" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get no matched on clientA, match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);
	}

	/**
	 * Test adv with "=" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithNotEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get no matched on clientB, match on clientA,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,100] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub2));
	}

	/**
	 * Test adv with "=" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithEqualToOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,=,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "<>" and sub with "<".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithLessThanOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get not matched on clientA, match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub2));

		// get not matched on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<>" and sub with "<=".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithLessThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub2));

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get matched on clientC, no match clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<>" and sub with ">".
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithGreaterThanOperator() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get not matched on all
		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(), null).
			expectClientNotReceivePublication(
				clientB.getClientID(), null).
			expectClientNotReceivePublication(
				clientC.getClientID(), null);
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		// get matched on clientA,B, no match clientC
		_brokerTester.clearAll().
			expectClientReceivePublication(
				clientA.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L)).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", "=", 120L)).
			expectClientNotReceivePublication(
				clientC.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock"));
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv with "<>" and sub with ">=".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithGreaterThanEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub1);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub2);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<>" and sub with "=".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,=,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub1);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub2);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get matched on clientC, no match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,120] should not be matched",
				expectedPub5);
	}

	/**
	 * Test adv with "<>" and sub with "<>".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithNotEqualToOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,100]");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<>,120]");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get not matched on clientA, match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,80] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[price,80] should not be matched", expectedPub2);

		// get no matched on clientC, match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,120] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub1.equalVals(expectedPub5));
	}

	/**
	 * Test adv with "<>" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithNotEqualToOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<>,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "isPresent" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithIsPresentOperatorSubWithIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,isPresent,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,120] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'a']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'a']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-lt" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ab']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ab] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ab] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ab] should be matched",
				pub.equalVals(expectedPub3));

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,C, no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,abc] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// get matched on clientB,C, no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'def']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,def] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ab']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ab] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ab] should not be matched",
				expectedPub2);

		// get matched on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-lt" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		// get matched on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'def']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,def] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-lt" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		// get no match on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'def']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,def] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,def] should not be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-lt" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLtOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// get matched on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'def']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,def] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-le" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get no match on clientB, match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-le" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'i']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-le" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'m']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-le" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-le" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get match on clientC, no match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub4 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub4);

		Publication expectedPub5 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-le" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// get match on clientB,C, no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-le" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientB,A, no match on clientC
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "str-le" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no match on both
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		// get match on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub3);
	}

	/**
	 * Test adv with "str-le" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get match on clientB, no match on clientC,A
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "str-le" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrLeOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get no match on clientA, match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// get no match on clientB, match on clientC,A
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get no match on clientB, match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'i']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'lm']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get matched on clientC, no match on clientB,A
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'lmn']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,lmn] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,lmn] should not be matched",
				pub1.equalVals(expectedPub3));

		Publication expectedPub4 = clientC.getCurrentPub();
		assertNotNull("The publication:[class,'stock'],[attribute,lmn] should be matched",
				expectedPub4);
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'m']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-ge" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub2));

		// get no match on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-le".
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrLeOperator() throws ParseException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(),
				null).
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", "eq", "ibm")).
			expectClientReceivePublication(
				clientC.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", "eq", "ibm"));
		// get matched on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());

		_brokerTester.clearAll().
			expectClientNotReceivePublication(
				clientA.getClientID(),
				null).
			expectClientNotReceivePublication(
				clientB.getClientID(),
				null).
			expectClientReceivePublication(
				clientC.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("attribute", "eq", "xyz"));

		// get no match on clientA,B, match on clientC
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test adv with "str-ge" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,A, no match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));

		// get match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-ge" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientA,B, no match on C
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "str-ge" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get match on clientC, no match on clientA,B
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get no match on clientA,C, match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "str-ge" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGeOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientA,B, no match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		// get match on clientA,C, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'x']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNotNull("The publication:[class,'stock'],[attribute,xyz] should be matched",
				expectedPub1);
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'x']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-gt" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'lmn']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,lmn] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub.equalVals(expectedPub2));

		// get no match on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no matched on both
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'yy']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,yy] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,yy] should not be matched",
				expectedPub2);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get no match on clientA, match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub4));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub3));

		// get no match on clientC, match on clientB,A
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'lmn']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,lmn] should not be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-gt" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientA,B, no match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		// get match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'yy']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,yy] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,yy] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,yy] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-gt" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get no match on clientA,B, match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		// get no match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'yy']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub4 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,yy] should not be matched",
				expectedPub4);

		Publication expectedPub5 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,yy] should not be matched",
				expectedPub5);

		Publication expectedPub6 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,yy] should not be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-gt" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrGtOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientA,B, no match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub3);

		// get match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'lmn']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub4 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub5 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,lmn] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "eq" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "eq" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'i']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "eq" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'m']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "eq" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "eq" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get matched on clientC, no match on clientB,A
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "eq" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "eq" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on clientB,A, no match on clientC
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "eq" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "eq" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");

		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "eq" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrEqOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);

		Publication expectedPub3 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "neq" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ib']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "neq" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'a']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "neq" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'m']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'bc']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "neq" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "neq" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get matched on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// get no match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub5 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub5);

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "neq" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get matched on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub3));

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		// get match on clientC, no match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub5 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "neq" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on all
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "neq" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get no match on all
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub1);

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientA,B, no match on clientC
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "neq" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB,C
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");

		// get match on clientC, no match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub5 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				expectedPub6);
	}

	/**
	 * Test adv with "neq" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrNeqOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'abc']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		Subscription sub2 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'xyz']");
		SubscriptionMessage subMsg2 = new SubscriptionMessage(sub2, brokerCore.getNewMessageID(),
				mdC);
		brokerCore.routeMessage(subMsg2, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");
		// get match on clientB,C, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abc']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub1));

		Publication expectedPub2 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abc] should be matched",
				pub.equalVals(expectedPub2));

		Publication expectedPub3 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abc] should not be matched",
				expectedPub3);

		// get no match on clientC, match on clientA,B
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'xyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub5 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub5));

		Publication expectedPub4 = clientC.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,xyz] should not be matched",
				pub1.equalVals(expectedPub4));

		Publication expectedPub6 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,xyz] should be matched",
				pub1.equalVals(expectedPub6));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'st']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'yz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'st']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get matched on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'ck']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// re-set message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		// get matched on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'e']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get matched on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abcibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abcibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abcibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abcibm] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-contains" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrContainsOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'xyz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub1);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmxyz']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null)
				msgCount++;
			waitCount++;
		}

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmxyz] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmxyz] should be matched",
				pub1.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'xyz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub1);

		// get match on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub3);
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'stock']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+Publication.+stock.+");

		// get match on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub1);

		// get match on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "str-prefix" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equals(expectedPub));
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub1);
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-prefix" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		// get not match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-prefix" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibmstock] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-prefix" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPrefixOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-contains".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrContainsOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-contains,'xyz']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub1);

		// get match on both
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub3);
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-prefix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrPrefixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-prefix,'st']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get match on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub1);

		// get match on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientA.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub3));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-postfix".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrPostfixOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Subscription sub1 = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-postfix,'stock']");
		SubscriptionMessage subMsg1 = new SubscriptionMessage(sub1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(subMsg1, MessageDestination.INPUTQUEUE);

		// get match on clientA, no match on clientB
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));

		Publication expectedPub1 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub1);

		// get match on clientA, no match on clientB
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub2));

		Publication expectedPub3 = clientB.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				expectedPub3);
	}

	/**
	 * Test adv with "str-postfix" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-lt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrLtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-lt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abcibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,abcibm] should be matched",
				pub.equalVals(expectedPub));

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-le".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrLeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-le,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-ge".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrGeOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-ge,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'abcibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,abcibm] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "str-gt".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrGtOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,str-gt,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));

		// get not match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "eq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrEqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,eq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub = clientA.getCurrentPub();
		assertNull("The publication:[class,'stock'],[attribute,stockibm] should not be matched",
				expectedPub);

		// get match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibm] should be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "str-postfix" and sub with "neq".
	 * @throws ParseException 
	 */
	public void testAdvWithStrPostfixOperatorSubWithStrNeqOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,str-postfix,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,neq,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'stockibm']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,stockibm] should be matched",
				pub.equalVals(expectedPub));

		// get no match on clientA
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibm']");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientA.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[attribute,ibm] should not be matched",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test adv with "isPresent" and sub with "isPresent".
	 * @throws ParseException 
	 */
	public void testAdvWithStrIsPresentOperatorSubWithStrIsPresentOperator() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,'ibm']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[attribute,isPresent,'ibm']");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// get match on clientB, no match on clientA
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,'ibmstock']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientA.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[attribute,ibmstock] should be matched",
				pub.equalVals(expectedPub));
	}

	/**
	 * Test unsubscribe a subscription.
	 * @throws ParseException 
	 */
	public void testUnsubscribe() throws ParseException {
		// clientA is publisher, clientB is subscriber
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		String subId = brokerCore.getNewMessageID();
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched at clientB",
				pub.equalVals(expectedPub));

		Unsubscription unsub = new Unsubscription(subId);
		UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);

		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,100] should not be matched at clientB",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test unadvertisement
	 * @throws ParseException 
	 */
	public void testUnadvertise() throws ParseException {
		// clientA is publisher, clientB is subscriber
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		String advId = brokerCore.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientB.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientB.getCurrentPub();
		assertTrue("The publication:[class,'stock'],[price,100] should be matched at clientB",
				pub.equalVals(expectedPub));

		Unadvertisement unadv = new Unadvertisement(advId);
		UnadvertisementMessage unadvMsg = new UnadvertisementMessage(unadv,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(unadvMsg, MessageDestination.INPUTQUEUE);

		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[price,110]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientB.getCurrentPub();
		assertFalse("The publication:[class,'stock'],[price,110] should not be matched at clientB",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test composite subscription: s1 AND s2.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAnd() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match both of them
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				pub.equalVals(expectedPub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				pub1.equalVals(expectedPub1));

		// this pub match one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		boolean countPub = false;
		boolean countPub2 = false;
		int waitCount = 0, msgCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub)
					countPub = clientC.checkForReceivedPub(pub);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue("The publication [class,'stock'],[price,70] should be matched at clientC",
				countPub2);
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC",
				countPub);
	}

	/**
	 * Test composite subscription: s1 AND s2. Composite subscription is issued first.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAndWithCSBeforeAdv() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		CompositeSubscription sub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		// this pub match both of them
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				pub.equalVals(expectedPub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				pub1.equalVals(expectedPub1));

		// this pub match one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// Both pub and pub2 should be matched here
		boolean countPub = false;
		boolean countPub2 = false;
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 7) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub)
					countPub = clientC.checkForReceivedPub(pub);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue("The publication [class,'stock'],[price,70] should be matched at clientC",
				countPub2);
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC",
				countPub);
	}

	/**
	 * Test composite subscription: s1 OR s2.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithOr() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}||{[class,eq,'stock'],[price,<,80]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		// this pub match both of them
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				pub.equalVals(expectedPub));

		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		// this pub match none of them
		Publication expectedPub1 = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				pub1.equalVals(expectedPub1));

		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		// this pub match one of them
		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[price,70] should be matched at clientC ",
				pub2.equalVals(expectedPub2));
	}

	/**
	 * Test composite subscription: {{s1 AND s2} AND s3}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAndAnd() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100],[attribute,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		Advertisement adv3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		AdvertisementMessage advMsg3 = new AdvertisementMessage(adv3, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}&{[class,eq,'stock'],[attribute,>,100]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[number,140] should not be matched at clientC ",
				expectedPub2);

		// this pub only match the last sub,
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,120] should not be matched at clientC ",
				expectedPub3);

		// this pub match the first two subs
		Publication pub4 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pub4, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// pub2, pub3 and pub4 should be matched here
		boolean countPub2 = false;
		boolean countPub3 = false;
		boolean countPub4 = false;
		int msgCount = 0, waitCount = 0;
		while (msgCount < 3 && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// pub, pub2, pub3 and pub4 are all matched here
		boolean countPub = false;
		countPub2 = false;
		countPub3 = false;
		countPub4 = false;
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 4 && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub)
					countPub = clientC.checkForReceivedPub(pub);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC",
				countPub);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);
	}

	/**
	 * Test composite subscription: {{s1 OR s2} OR s3}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithOrOr() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100],[attribute,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		Advertisement adv3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		AdvertisementMessage advMsg3 = new AdvertisementMessage(adv3, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{{[class,eq,'stock'],[number,>,120]}||{[class,eq,'stock'],[price,<,80]}}||{[class,eq,'stock'],[attribute,>,100]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC ",
				pub.equalVals(expectedPub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				pub1.equalVals(expectedPub1));

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC ",
				pub2.equalVals(expectedPub2));

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC ",
				pub3.equalVals(expectedPub3));
	}

	/**
	 * Test composite subscription: {{s1 AND s2} OR s3}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAndOr() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100],[attribute,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		Advertisement adv3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		AdvertisementMessage advMsg3 = new AdvertisementMessage(adv3, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{{[class,eq,'stock'],[number,>,120]}&{[class,eq,'stock'],[price,<,80]}}||{[class,eq,'stock'],[attribute,>,100]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub2);

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC ",
				pub3.equalVals(expectedPub3));

		// this pub match the first two subs
		Publication pub4 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pub4, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// Both pub2 and pub4 should be matched here
		boolean countPub2 = false, countPub4 = false;
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);

		// this pub match the first and the last subs
		Publication pub5 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,130],[number,130]");
		PublicationMessage pubMsg5 = new PublicationMessage(pub5, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg5, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// both pub4 and pub5 should be matched here
		countPub4 = false;
		boolean countPub5 = false;
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub5)
					countPub5 = clientC.checkForReceivedPub(pub5);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				countPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// pub2, pub4, pub5 and pub should be matched here
		boolean countPub = false;
		countPub2 = false;
		countPub4 = false;
		countPub5 = false;
		waitCount = 0;
		msgCount = 0;
		while (msgCount < 4 && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub5)
					countPub5 = clientC.checkForReceivedPub(pub5);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				if (!countPub)
					countPub = clientC.checkForReceivedPub(pub);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				countPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC",
				countPub);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
	}

	/**
	 * Test composite subscription: {{s1 OR s2} AND s3}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithOrAnd() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100],[attribute,isPresent,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);

		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);

		Advertisement adv3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		AdvertisementMessage advMsg3 = new AdvertisementMessage(adv3, brokerCore.getNewMessageID(),
				mdB);
		brokerCore.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{{[class,eq,'stock'],[number,>,120]}||{[class,eq,'stock'],[price,<,80]}}&{[class,eq,'stock'],[attribute,>,100]}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+stock.+");

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);

		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub2);

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// both pub2 and pub3 should be matched here
		// waiting for routing finished
		boolean countPub3 = false;
		boolean countPub2 = false;
		int msgCount = 0, waitCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);

		// this pub match the first two subs
		Publication pub4 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pub4, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		boolean countPub4 = false;
		countPub3 = false;
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 2 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				msgCount++;
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);

		// this pub match the first and the last subs
		Publication pub5 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,130],[number,130]");
		PublicationMessage pubMsg5 = new PublicationMessage(pub5, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg5, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// both pub2,pub3,pub4 and pub5 should be matched here
		countPub4 = false;
		countPub2 = false;
		countPub3 = false;
		boolean countPub5 = false;
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 4 && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub5)
					countPub5 = clientC.checkForReceivedPub(pub5);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				countPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		// pub2,pub3, pub4, pub5 and pub should be matched here
		boolean countPub = false;
		countPub2 = false;
		countPub3 = false;
		countPub4 = false;
		countPub5 = false;
		msgCount = 0;
		waitCount = 0;
		while (msgCount < 5 && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub5)
					countPub5 = clientC.checkForReceivedPub(pub5);
				if (!countPub4)
					countPub4 = clientC.checkForReceivedPub(pub4);
				if (!countPub)
					countPub = clientC.checkForReceivedPub(pub);
				if (!countPub2)
					countPub2 = clientC.checkForReceivedPub(pub2);
				if (!countPub3)
					countPub3 = clientC.checkForReceivedPub(pub3);
				msgCount++;
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				countPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				countPub4);
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC",
				countPub);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				countPub2);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				countPub3);
	}

	/**
	 * Test composite subscription: s1 AND s2, both s1 and s2 have string variable.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithStringVariableWithAnd() throws ParseException {
		// varibale test for CS is only "eq" right now.
		// clientA,B is publisher, clientC is susbcriber
		clientA.handleCommand("a [class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		clientB.handleCommand("a [class,eq,'trade'],[buyer,eq,'ibm'],[bargainor,isPresent,'string']");
		clientC.handleCommand("cs {{[class,eq,'stock'],[comp,eq,$S$X],[price,>,100]}&{[class,eq,'trade'],[buyer,eq,$S$X],[bargainor,eq,'MS']}}");

		// reset message filter
		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+");

		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'trade'],[buyer,'ibm'],[bargainor,'MS']");

		// waiting for routing finished
		boolean countPub = false;
		boolean countPub1 = false;
		int waitCount = 0;
		while (!(countPub && countPub1) && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub) {
					countPub = clientC.checkForReceivedPub(pub);
				}
				if (!countPub1) {
					countPub1 = clientC.checkForReceivedPub(pub1);
				}
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be sent to ClientC",
				countPub);
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'] should be sent to ClientC",
				countPub1);
	}

	/**
	 * Test composite subscription: s1 OR s2, both s1 and s2 have string variable.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithStringVariableWithOr() throws ParseException {
		// varibale test for CS is only "eq" right now.
		// clientA,B is publisher, clientC is susbcriber
		clientA.handleCommand("a [class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		clientB.handleCommand("a [class,eq,'trade'],[buyer,eq,'ibm'],[bargainor,isPresent,'string']");
		clientC.handleCommand("cs {{[class,eq,'stock'],[comp,eq,$S$X],[price,>,100]}||{[class,eq,'trade'],[buyer,eq,$S$X],[bargainor,eq,'MS']}}");

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+");

		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be matched at clientC ",
				pub.equalVals(expectedPub));

		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'] should be matched at clientC ",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test composite subscription: s1 AND s2, both s1 and s2 have integer variable
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithNumberVariableWithAnd() throws ParseException {
		// varibale test for CS is only "=" right now.
		// clientA,B is publisher, clientC is susbcriber
		clientA.handleCommand("a [class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		clientB.handleCommand("a [class,eq,'trade'],[buyer,eq,'ibm'],[bargainor,isPresent,'string'],[price,isPresent,100]");
		clientC.handleCommand("cs {{[class,eq,'stock'],[comp,eq,ibm],[price,=,$I$X]}&{[class,eq,'trade'],[buyer,eq,ibm],[bargainor,eq,'MS'],[price,=,$I$X]}}");

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+");

		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,110]");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,110]");
		Publication pub1 = MessageFactory.createPublicationFromString(
				"[class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");

		// waiting for routing finished
		boolean countPub = false;
		boolean countPub1 = false;
		int waitCount = 0;
		while (!(countPub && countPub1) && waitCount < 5) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPub) {
					countPub = clientC.checkForReceivedPub(pub);
				}
				if (!countPub1) {
					countPub1 = clientC.checkForReceivedPub(pub1);
				}
			}
			waitCount++;
		}

		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be sent to ClientC",
				countPub);
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,120] should be sent to ClientC",
				countPub1);
	}

	/**
	 * Test composite subscription: s1 OR s2, both s1 and s2 have integer variable
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithNumberVariableWithOr() throws ParseException {
		// varibale test for CS is only "=" right now.
		// clientA,B is publisher, clientC is susbcriber
		clientA.handleCommand("a [class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		clientB.handleCommand("a [class,eq,'trade'],[buyer,eq,'ibm'],[bargainor,isPresent,'string'],[price,isPresent,100]");
		clientC.handleCommand("cs {{[class,eq,'stock'],[comp,eq,ibm],[price,=,$I$X]}||{[class,eq,'trade'],[buyer,eq,ibm],[bargainor,eq,'MS'],[price,=,$I$X]}}");

		// reset message filter
		msgFilter.setPattern(".*Client.+" + clientC.getClientID() + ".+Publication.+");

		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be matched at clientC ",
				pub.equalVals(expectedPub));

		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");
		Publication pub1 = MessageFactory.createPublicationFromString(
				"[class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");

		// waiting for routing finished
		messageWatcher.getMessage();

		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110] should be matched at clientC ",
				pub1.equalVals(expectedPub1));
	}

	/**
	 * Test payload attached with publication.
	 */
	public void testPubPayload() {

	}

}

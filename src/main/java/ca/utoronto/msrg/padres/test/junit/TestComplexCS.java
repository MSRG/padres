package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

/**
 * This class provides a way to test in the scenario of one broker with swingRmiClient. In order to
 * run this class, rmiregistry 1099 need to be done first.
 * 
 * @author Shuang Hou
 */

public class TestComplexCS extends TestCase {

	private BrokerCore brokerCore;

	private Client clientA;

	private Client clientB;

	private Client clientC;

	private MessageWatchAppender messageWatcher;

	private PatternFilter msgFilter;

	protected void setUp() throws Exception {
		brokerCore = new BrokerCore(AllTests.brokerConfig01);
		brokerCore.initialize();
		clientA = new Client(AllTests.clientConfigA);
		clientA.connect(brokerCore.getBrokerURI());
		clientB = new Client(AllTests.clientConfigB);
		clientB.connect(brokerCore.getBrokerURI());
		clientC = new Client(AllTests.clientConfigC);
		clientC.connect(brokerCore.getBrokerURI());
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(Client.class.getName());
		msgFilter.setPattern(".*Client " + clientC.getClientID() + ".+Publication.+");
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		brokerCore.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
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
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				expectedPub.equalVals(pub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub.equalVals(pub1));

		// this pub match one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// Both pub and pub2 should be matched here
		boolean countPub = false;
		boolean countPub2 = false;
		int msgCount = 0;
		int waitCount = 0;
		while (msgCount < 2 && waitCount < 10) {
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
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				expectedPub.equalVals(pub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub.equalVals(pub1));

		// this pub match one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// Both pub and pub2 should be matched here
		boolean countPub = false;
		boolean countPub2 = false;
		int msgCount = 0;
		int waitCount = 0;
		while (msgCount < 2 && waitCount < 10) {
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
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		// this pub match both of them
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60] should be matched at clientC ",
				expectedPub.equalVals(pub));

		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		// this pub match none of them
		expectedPub = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub.equalVals(pub1));

		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[price,70]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		// this pub match one of them
		expectedPub = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[price,70] should be matched at clientC ",
				expectedPub.equalVals(pub2));
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
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		Publication expectedPub = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[number,140] should not be matched at clientC ",
				expectedPub);

		// this pub only match the last sub,
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,120] should not be matched at clientC ",
				expectedPub);

		// this pub match the first two subs
		Publication pub4 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pub4, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// pub2, pub3 and pub4 should be matched here
		// waiting for the message to be received
		boolean countPub2 = false;
		boolean countPub3 = false;
		boolean countPub4 = false;
		int msgCount = 0;
		int waitCount = 0;
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

		// waiting for the message to be received
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
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC ",
				expectedPub.equalVals(pub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub.equalVals(pub1));

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		expectedPub = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub.equalVals(pub2));

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		expectedPub = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC ",
				expectedPub.equalVals(pub3));
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
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// wait until routed
		messageWatcher.getMessage(2);
		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// wait until routed
		messageWatcher.getMessage(2);
		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub2);

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);
		// wait until routed
		messageWatcher.getMessage();
		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC ",
				expectedPub3.equalVals(pub3));

		// this pub match the first two subs
		Publication pub4 = MessageFactory.createPublicationFromString("[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pub4, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// Both pub2 and pub4 should be matched here
		boolean checkPub2 = false;
		boolean checkPub4 = false;
		int waitCount = 0;
		while (!(checkPub2 && checkPub4) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPub2)
					checkPub2 = clientC.checkForReceivedPub(pub2);
				if (!checkPub4)
					checkPub4 = clientC.checkForReceivedPub(pub4);
			}
			waitCount++;
		}
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				checkPub2);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				checkPub4);

		// this pub match the first and the last subs
		Publication pub5 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,130],[number,130]");
		PublicationMessage pubMsg5 = new PublicationMessage(pub5, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg5, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// both pub4 and pub5 should be matched here
		checkPub4 = false;
		boolean checkPub5 = false;
		waitCount = 0;
		while (!(checkPub4 && checkPub5) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPub5)
					checkPub5 = clientC.checkForReceivedPub(pub5);
				if (!checkPub4)
					checkPub4 = clientC.checkForReceivedPub(pub4);
			}
			waitCount++;
		}
		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				checkPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				checkPub4);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// pub2, pub4, pub5 and pub should be matched here
		boolean checkPub = false;
		checkPub2 = false;
		checkPub4 = false;
		checkPub5 = false;
		waitCount = 0;
		while (!(checkPub && checkPub2 && checkPub4 && checkPub5) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPub5)
					checkPub5 = clientC.checkForReceivedPub(pub5);
				if (!checkPub4)
					checkPub4 = clientC.checkForReceivedPub(pub4);
				if (!checkPub)
					checkPub = clientC.checkForReceivedPub(pub);
				if (!checkPub2)
					checkPub2 = clientC.checkForReceivedPub(pub2);
			}
			waitCount++;
		}
		assertTrue(
				"The publication [class,'stock'],[attribute,130],[number,130] should be matched at clientC",
				checkPub5);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				checkPub4);
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC",
				checkPub2);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				checkPub);
	}

	/**
	 * Test composite subscription: {{s1 OR s2} AND s3}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithOrAnd() throws ParseException {
		// clientA,B is publisher, clientC is subscriber
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

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);
		// wait until routed
		messageWatcher.getMessage(2);
		Publication expectedPub = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub);

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);
		// wait until routed
		messageWatcher.getMessage(2);
		expectedPub = clientC.getCurrentPub();
		assertNull("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub);

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// both pub2 and pub3 should be matched here
		boolean countPub3 = false;
		boolean countPub2 = false;
		int waitCount = 0;
		while (!(countPub2 && countPub3) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				Publication receivedPub = clientC.getCurrentPub();
				if (!countPub3)
					countPub3 = pub3.equalVals(receivedPub);
				if (!countPub2)
					countPub2 = pub2.equalVals(receivedPub);
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

		// waiting for the message to be received
		// both pub3 and pub4 are matched here
		boolean countPub4 = false;
		countPub3 = false;
		waitCount = 0;
		while (!(countPub3 && countPub4) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
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

		// waiting for the message to be received
		// both pub2,pub3,pub4 and pub5 should be matched here
		countPub4 = false;
		countPub2 = false;
		countPub3 = false;
		boolean countPub5 = false;
		waitCount = 0;
		while (!(countPub2 && countPub3 && countPub4 && countPub5) && waitCount < 10) {
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

		// waiting for the message to be received
		// pub2,pub3, pub4, pub5 and pub should be matched here
		boolean countPub = false;
		countPub2 = false;
		countPub3 = false;
		countPub4 = false;
		countPub5 = false;
		waitCount = 0;
		while (!(countPub && countPub2 && countPub3 && countPub4 && countPub5) && waitCount < 10) {
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
		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'trade'],[buyer,'ibm'],[bargainor,'MS']");

		// waiting for the message to be received
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
		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");
		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be matched at clientC ",
				expectedPub.equalVals(pub));

		msgFilter.setPattern(".*Client " + clientC.getClientID() + ".+Publication.+trade.+");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'trade'],[buyer,'ibm'],[bargainor,'MS']");
		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub1 = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'] should be matched at clientC ",
				expectedPub1.equalVals(pub1));
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
		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,110]");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,110]");
		Publication pub1 = MessageFactory.createPublicationFromString(
				"[class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");

		// waiting for the message to be received
		boolean countPub = false;
		boolean countPub1 = false;
		int waitCount = 0;
		while (!(countPub && countPub1) && waitCount < 10) {
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
		// this pub only match the first one of them
		clientA.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[comp,'ibm'],[price,120]");
		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[comp,'ibm'],[price,120] should be matched at clientC ",
				expectedPub.equalVals(pub));

		// change filter
		msgFilter.setPattern(".*Client " + clientC.getClientID() + ".+Publication.+trade.+");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");
		Publication pub1 = MessageFactory.createPublicationFromString(
				"[class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110]");
		// waiting for routing finished
		messageWatcher.getMessage();
		expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'trade'],[buyer,'ibm'],[bargainor,'MS'],[price,110] should be matched at clientC ",
				expectedPub.equalVals(pub1));
	}

	/**
	 * Test composite subscription: {s1 AND {s2 AND s3}}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAndAndLast() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientA.getClientDest();
		MessageDestination mdB = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement advWithPriceAndAttribute = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[number,isPresent,100],[price,isPresent,100],[attribute,isPresent,100]");
		AdvertisementMessage advMsgWithPriceAndAttribute = new AdvertisementMessage(
				advWithPriceAndAttribute, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsgWithPriceAndAttribute, MessageDestination.INPUTQUEUE);

		Advertisement advWithAttribute = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[attribute,isPresent,100]");
		AdvertisementMessage advMsgWithAttribute = new AdvertisementMessage(advWithAttribute,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(advMsgWithAttribute, MessageDestination.INPUTQUEUE);

		Advertisement advWithPrice = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		AdvertisementMessage advMsgWithPrice = new AdvertisementMessage(advWithPrice,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(advMsgWithPrice, MessageDestination.INPUTQUEUE);

		Advertisement advWithNumber = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[number,isPresent,100]");
		AdvertisementMessage advMsg3 = new AdvertisementMessage(advWithNumber,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(advMsg3, MessageDestination.INPUTQUEUE);

		CompositeSubscription subForPriceAttributeAndNumber = new CompositeSubscription(
				"{{[class,eq,'stock'],[number,>,120]}&{{[class,eq,'stock'],[price,<,80]}&{[class,eq,'stock'],[attribute,>,100]}}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(
				subForPriceAttributeAndNumber, brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match none of them
		Publication pubWithAttribute60 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsgWithAttribute60 = new PublicationMessage(pubWithAttribute60,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsgWithAttribute60, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);
		Publication expectedPub1 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1);

		// this pub only match the first one of them
		Publication pubWithNumber = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsgWithNumber = new PublicationMessage(pubWithNumber,
				brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsgWithNumber, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);
		Publication expectedPub2 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[number,140] should not be matched at clientC ",
				expectedPub2);

		// this pub only match the last sub,
		Publication pubWithAttribute120 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsgWithAttribute120 = new PublicationMessage(pubWithAttribute120,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsgWithAttribute120, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);
		Publication expectedPub3 = clientC.getCurrentPub();
		assertNull(
				"The publication [class,'stock'],[attribute,120] should not be matched at clientC ",
				expectedPub3);

		// this pub match the first two subs
		Publication pubWithPriceAndNumber = MessageFactory.createPublicationFromString(
				"[class,'stock'],[price,40],[number,130]");
		PublicationMessage pubMsg4 = new PublicationMessage(pubWithPriceAndNumber,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg4, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// pub2, pub3 and pub4 should be matched here
		boolean checkPubWithNumber = false;
		boolean checkPubWithAttribute120 = false;
		boolean checkPubWithPriceAndNumber = false;
		int waitCount = 0;
		while (!(checkPubWithNumber && checkPubWithAttribute120 && checkPubWithPriceAndNumber)
				&& waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPubWithNumber)
					checkPubWithNumber = clientC.checkForReceivedPub(pubWithNumber);
				if (!checkPubWithAttribute120)
					checkPubWithAttribute120 = clientC.checkForReceivedPub(pubWithAttribute120);
				if (!checkPubWithPriceAndNumber)
					checkPubWithPriceAndNumber = clientC.checkForReceivedPub(pubWithPriceAndNumber);
			}
			waitCount++;
		}
		messageWatcher.clear();
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				checkPubWithNumber);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				checkPubWithAttribute120);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				checkPubWithPriceAndNumber);

		// this pub match the all three subs
		Publication pubWithNumberPriceAndAttribute = MessageFactory.createPublicationFromString(
				"[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pubWithNumberPriceAndAttribute,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// pub, pub2, pub3 and pub4 are all matched here
		boolean checkPubWithNumberPriceAndAttribute = false;
		checkPubWithNumber = false;
		checkPubWithAttribute120 = false;
		checkPubWithPriceAndNumber = false;
		waitCount = 0;
		while (!(checkPubWithNumberPriceAndAttribute && checkPubWithNumber
				&& checkPubWithAttribute120 && checkPubWithPriceAndNumber)
				&& waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPubWithNumberPriceAndAttribute)
					checkPubWithNumberPriceAndAttribute = clientC.checkForReceivedPub(pubWithNumberPriceAndAttribute);
				if (!checkPubWithNumber)
					checkPubWithNumber = clientC.checkForReceivedPub(pubWithNumber);
				if (!checkPubWithAttribute120)
					checkPubWithAttribute120 = clientC.checkForReceivedPub(pubWithAttribute120);
				if (!checkPubWithPriceAndNumber)
					checkPubWithPriceAndNumber = clientC.checkForReceivedPub(pubWithPriceAndNumber);
			}
			waitCount++;
		}
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC",
				checkPubWithNumberPriceAndAttribute);
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC",
				checkPubWithNumber);
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC",
				checkPubWithAttribute120);
		assertTrue(
				"The publication [class,'stock'],[price,40],[number,130] should be matched at clientC",
				checkPubWithPriceAndNumber);
	}

	/**
	 * Test composite subscription: {s1 OR { s2 OR s3}}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithOrOrLast() throws ParseException {
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
				"{{[class,eq,'stock'],[number,>,120]}||{{[class,eq,'stock'],[price,<,80]}||{[class,eq,'stock'],[attribute,>,100]}}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match the all three subs
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[number,130],[price,60],[attribute,120]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub = clientC.getCurrentPub();
		assertTrue(
				"The publication [class,'stock'],[number,130],[price,60],[attribute,120] should be matched at clientC ",
				expectedPub.equalVals(pub));

		// this pub match none of them
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,60]");
		PublicationMessage pubMsg1 = new PublicationMessage(pub1, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg1, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);
		Publication expectedPub1 = clientC.getCurrentPub();
		assertFalse(
				"The publication [class,'stock'],[attribute,60] should not be matched at clientC ",
				expectedPub1.equalVals(pub1));

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,140]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub2 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[number,140] should be matched at clientC ",
				expectedPub2.equalVals(pub2));

		// this pub only match the last sub
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,120]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage();
		Publication expectedPub3 = clientC.getCurrentPub();
		assertTrue("The publication [class,'stock'],[attribute,120] should be matched at clientC ",
				expectedPub3.equalVals(pub3));
	}

	/**
	 * Test composite subscription: {s1 AND {s2 AND s3}}.
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionWithAndAndLastWithVarible() throws ParseException {
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
				"{{[class,eq,'stock'],[number,=,$I$X]}&{{[class,eq,'stock'],[price,=,$I$X]}&{[class,eq,'stock'],[attribute,=,$I$X]}}}");
		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

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

		// this pub only match the last sub,
		Publication pub3 = MessageFactory.createPublicationFromString("[class,'stock'],[price,60]");
		PublicationMessage pubMsg3 = new PublicationMessage(pub3, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg3, MessageDestination.INPUTQUEUE);

		// waiting for routing finished
		messageWatcher.getMessage(2);
		Publication expectedPub3 = clientC.getCurrentPub();
		assertFalse("The publication [class,'stock'],[price,60] should not be matched at clientC ",
				pub3.equalVals(expectedPub3));

		// this pub only match the first one of them
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'stock'],[number,60]");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdB);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		// pub2, pub3 and pub4 should be matched here
		boolean checkPub2 = false;
		boolean checkPub3 = false;
		boolean checkPub4 = false;
		int waitCount = 0;
		while (!(checkPub2 && checkPub3 && checkPub4) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPub2)
					checkPub2 = clientC.checkForReceivedPub(pub2);
				if (!checkPub3)
					checkPub3 = clientC.checkForReceivedPub(pub3);
				if (!checkPub4)
					checkPub4 = clientC.checkForReceivedPub(pub1);
			}
			waitCount++;
		}
		assertTrue("The publication [class,'stock'],[number,60] should be matched at clientC",
				checkPub2);
		assertTrue("The publication [class,'stock'],[attribute,60] should be matched at clientC",
				checkPub3);
		assertTrue("The publication [class,'stock'],[price,60] should be matched at clientC",
				checkPub4);

	}

	/**
	 * "{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,SUCCESS]}&"
	 * +
	 * "{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,SUCCESS]}&"
	 * + "{[class,eq,Trigger1],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}}");
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionFromWorkflowDemo() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement advJobStatus = MessageFactory.createAdvertisementFromString(
				"[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,isPresent,JobB],[GID,isPresent,g001],"
						+ "[status,isPresent,SUCCESS],[detail,isPresent,ANYSTRING]");
		AdvertisementMessage advMsgJobStatus = new AdvertisementMessage(advJobStatus,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsgJobStatus, MessageDestination.INPUTQUEUE);

		Advertisement advTrigger = MessageFactory.createAdvertisementFromString(
				"[class,eq,Trigger1],[applname,eq,PAYROLL],[GID,isPresent,g001],[schedule,isPresent,DAILY]");
		AdvertisementMessage advMsgTrigger = new AdvertisementMessage(advTrigger,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsgTrigger, MessageDestination.INPUTQUEUE);

		CompositeSubscription csTwoJobStatusAndTrigger = new CompositeSubscription(
				"{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,SUCCESS]}&"
						+ "{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,SUCCESS]}&"
						+ "{[class,eq,Trigger1],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}}");

		CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(
				csTwoJobStatusAndTrigger, brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(csMsg, MessageDestination.INPUTQUEUE);

		// this pub match both of them
		Publication pubTrigger = MessageFactory.createPublicationFromString(
				"[class,Trigger1],[schedule,'2009.5.4'],[GID,g001],[applname,PAYROLL]");
		PublicationMessage pubMsgTrigger = new PublicationMessage(pubTrigger,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsgTrigger, MessageDestination.INPUTQUEUE);

		Publication pubJobB = MessageFactory.createPublicationFromString(
				"[class,'JOBSTATUS1'],[detail,'null'],[jobname,'JobB'],[status,'SUCCESS'],[GID,'g001'],[applname,'PAYROLL']");
		PublicationMessage pubJobBMsg = new PublicationMessage(pubJobB,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubJobBMsg, MessageDestination.INPUTQUEUE);

		// this pub match one of them
		Publication pubJobC = MessageFactory.createPublicationFromString(
				"[class,'JOBSTATUS1'],[detail,'null'],[jobname,'JobC'],[status,'SUCCESS'],[GID,'g001'],[applname,'PAYROLL']");
		PublicationMessage pubJobCMsg = new PublicationMessage(pubJobC,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubJobCMsg, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		boolean checkPubTrigger = false;
		boolean checkPubJobB = false;
		boolean checkPubJobC = false;
		int waitCount = 0;
		while (!(checkPubJobB && checkPubJobC && checkPubTrigger) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!checkPubTrigger)
					checkPubTrigger = clientC.checkForReceivedPub(pubTrigger);
				if (!checkPubJobB)
					checkPubJobB = clientC.checkForReceivedPub(pubJobB);
				if (!checkPubJobC)
					checkPubJobC = clientC.checkForReceivedPub(pubJobC);
			}
			waitCount++;
		}
		assertTrue(
				"The publication [class,Trigger1],[schedule,2009.5.4],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				checkPubTrigger);
		assertTrue(
				"The publication [class,JOBSTATUS1],[detail,null],[jobname,JobB],[status,SUCCESS],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				checkPubJobB);
		assertTrue(
				"The publication [class,JOBSTATUS1],[detail,null],[jobname,JobC],[status,SUCCESS],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				checkPubJobC);
	}

	/**
	 * Test composite subscription: s1 AND s2.
	 * {{{{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname
	 * ,eq,JobB],[GID,eq,$S$X],[status,eq,SUCCESS
	 * ]}||{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname
	 * ,eq,JobB],[GID,eq,$S$X],[status,eq,NORUN
	 * ]}}&{{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname
	 * ,eq,JobC],[GID,eq,$S$X],[status,eq,SUCCESS
	 * ]}||{[class,eq,JOBSTATUS],[applname,eq,PAYROLL],[jobname
	 * ,eq,JobC],[GID,eq,$S$X],[status,eq,NORUN
	 * ]}}}&{[class,eq,Trigger],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}
	 * @throws ParseException 
	 */
	public void testCompositeSubscriptionFromWorkflowDemoComplex() throws ParseException {
		// clientA,B is publisher, clientC is susbcriber
		MessageDestination mdA = clientB.getClientDest();
		MessageDestination mdC = clientC.getClientDest();

		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,isPresent,JobB],[GID,isPresent,g001],"
						+ "[status,isPresent,SUCCESS],[detail,isPresent,ANYSTRING]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Advertisement advTrigger = MessageFactory.createAdvertisementFromString(
				"[class,eq,Trigger1],[applname,eq,PAYROLL],[GID,isPresent,g001],[schedule,isPresent,DAILY]");
		AdvertisementMessage advMsgTrigger = new AdvertisementMessage(advTrigger,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(advMsgTrigger, MessageDestination.INPUTQUEUE);

		CompositeSubscription sub = new CompositeSubscription(
				"{{{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,SUCCESS]}||"
						+ "{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobB],[GID,eq,$S$X],[status,eq,NORUN]}}&"
						+ "{{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,SUCCESS]}||"
						+ "{[class,eq,JOBSTATUS1],[applname,eq,PAYROLL],[jobname,eq,JobC],[GID,eq,$S$X],[status,eq,NORUN]}}}&"
						+ "{[class,eq,Trigger1],[applname,eq,PAYROLL],[GID,eq,$S$X],[schedule,isPresent,DAILY]}}");

		CompositeSubscriptionMessage subMsg = new CompositeSubscriptionMessage(sub,
				brokerCore.getNewMessageID(), mdC);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// this pub match both of them
		Publication pubTrigger = MessageFactory.createPublicationFromString(
				"[class,Trigger1],[schedule,'2009.5.4'],[GID,g001],[applname,PAYROLL]");
		PublicationMessage pubMsgTrigger = new PublicationMessage(pubTrigger,
				brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsgTrigger, MessageDestination.INPUTQUEUE);

		Publication pub = MessageFactory.createPublicationFromString(
				"[class,'JOBSTATUS1'],[detail,'null'],[jobname,'JobB'],[status,'SUCCESS'],[GID,'g001'],[applname,'PAYROLL']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);

		// this pub match one of them
		Publication pub2 = MessageFactory.createPublicationFromString(
				"[class,'JOBSTATUS1'],[detail,'null'],[jobname,'JobC'],[status,'SUCCESS'],[GID,'g001'],[applname,'PAYROLL']");
		PublicationMessage pubMsg2 = new PublicationMessage(pub2, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg2, MessageDestination.INPUTQUEUE);

		// waiting for the message to be received
		boolean countPubTrigger = false;
		boolean countPubJobB = false;
		boolean countPubJobC = false;
		int waitCount = 0;
		while (!(countPubJobB && countPubJobC && countPubTrigger) && waitCount < 10) {
			String msg = messageWatcher.getMessage();
			if (msg != null) {
				if (!countPubTrigger)
					countPubTrigger = clientC.checkForReceivedPub(pubTrigger);
				if (!countPubJobB)
					countPubJobB = clientC.checkForReceivedPub(pub);
				if (!countPubJobC)
					countPubJobC = clientC.checkForReceivedPub(pub2);
			}
			waitCount++;
		}
		assertTrue(
				"The publication [class,Trigger1],[schedule,2009.5.4],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				countPubTrigger);
		assertTrue(
				"The publication [class,JOBSTATUS1],[detail,null],[jobname,JobB],[status,SUCCESS],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				countPubJobB);
		assertTrue(
				"The publication [class,JOBSTATUS1],[detail,null],[jobname,JobC],[status,SUCCESS],[GID,g001],[applname,PAYROLL] should be matched at clientC ",
				countPubJobC);
	}

}

package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;

import org.apache.log4j.Level;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.broker.router.matching.AdvMsgNotFoundException;
import ca.utoronto.msrg.padres.broker.router.matching.PubMsgNotConformedException;
import ca.utoronto.msrg.padres.broker.router.matching.SubMsgNotFoundException;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
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

/**
 * This class provides a way for exception handling test in the scenario of one broker with
 * swingRmiClient. In order to run this class, rmiregistry 1099 need to be done first.
 * 
 * @author Shuang Hou, Bala Maniymaran
 */
public class TestClientsException extends TestCase {
	
	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "1");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}
	
	protected GenericBrokerTester _brokerTester;
	
	protected BrokerCore otherBroker = null;

	protected BrokerCore brokerCore;

	protected Client clientA;

	protected Client clientB;

	protected MessageWatchAppender exceptionAppender;

	protected MessageWatchAppender messageWatcher;

	protected PatternFilter msgPatternFilter;

	protected PatternFilter exceptionPatternFilter;

	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		// start the broker
		brokerCore = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore.initialize();
		clientA = new Client(AllTests.clientConfigA);
		clientA.connect(brokerCore.getBrokerURI());
		exceptionAppender = new MessageWatchAppender();
		exceptionPatternFilter = new PatternFilter(Subscription.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + ParseException.class.getName() + ".+(\\s*.*)*");
		exceptionAppender.addFilter(exceptionPatternFilter);
		LogSetup.addAppender("Exception", exceptionAppender);
		messageWatcher = new MessageWatchAppender();
		msgPatternFilter = new PatternFilter(Client.class.getName());
		messageWatcher.addFilter(msgPatternFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		LogSetup.removeAppender("Exception", exceptionAppender);
		LogSetup.removeAppender("MessagePath", messageWatcher);
		clientA.shutdown();
		
		if(clientB != null)
			clientB.shutdown();
		
		brokerCore.shutdown();
		_brokerTester = null;
	}
	
	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}
	
    protected Client createNewClient(String clientId) throws ClientException {
    	return new TesterClient(_brokerTester, clientId);
	}

	/**
	 * Test for exception that the connection arguments for swingClientB is fake.
	 * @throws ClientException 
	 * @throws InterruptedException 
	 */
	public void testFakeConnectionArgsWithSwingClient() throws ClientException, InterruptedException {
		/* TODO: REZA (NEW-DONE) */
		// start the ClientB and give a fake URI to connect to
		clientB = createNewClient("B");
		String tempAddress =
			NodeAddress.makeURI(AllTests.commProtocol, "localhost", 1500, "BrokerNull");
		_brokerTester.clearAll().
			expectConnectionFail(clientB.getClientID(), tempAddress);
		try { clientB.connect(tempAddress); } catch (ClientException e) { }
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}

	/**
	 * Test for exception that unrecognized publication, where the advertisement has the same
	 * attribute set with publication, however, the value of publication is unrecognized.
	 * @throws ParseException 
	 */
	public void testPubWithUnrecognizedValue() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// setup message filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + PubMsgNotConformedException.class.getName()
				+ ".+\\s*.+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		if (brokerCore.getBrokerConfig().isPubConformCheck()) {
			boolean checkReceived = exceptionAppender.getMessage() != null;
			assertTrue(
					"Matcher should throw an exception since you did not advertise correctly before you published.",
					checkReceived);
		}
	}

	/**
	 * Test for exception that publication with string value in number predicate.
	 * @throws ParseException 
	 */
	public void testPubWithStringValueInNumberPredicate() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,<,100]");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,'IBM']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// setup message filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + PubMsgNotConformedException.class.getName()
				+ ".+\\s*.+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the exception to be thrown
		if (brokerCore.getBrokerConfig().isPubConformCheck()) {
			boolean checkReceived = exceptionAppender.getMessage() != null;
			assertTrue(
					"Retematcher should throw an exception since you did not advertise correctly before you published.",
					checkReceived);
		}
	}

	/**
	 * Test for exception that publication with number value in string predicate.
	 * @throws ParseException 
	 */
	public void testPubWithNumberValueInStringPredicate() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[attribute,eq,'IBM']");
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[attribute,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// setup message filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + PubMsgNotConformedException.class.getName()
				+ ".+\\s*.+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the exception to be thrown
		if (brokerCore.getBrokerConfig().isPubConformCheck()) {
			boolean checkReceived = exceptionAppender.getMessage() != null;
			assertTrue(
					"Retematcher should throw an exception since you did not advertise correctly before you published.",
					checkReceived);
		}
	}

	/**
	 * Test for exception that unrecognized publication, where the advertisement has less attribute
	 * set than publication. That is, some attribute name of publication is unrecognized.
	 * @throws ParseException 
	 */
	public void testPubWithUnrecognizedAttribute() throws ParseException {
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,isPresent,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,120],[amount,100]");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		// setup message filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + PubMsgNotConformedException.class.getName()
				+ ".+\\s*.+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for routing finished
		if (brokerCore.getBrokerConfig().isPubConformCheck()) {
			boolean checkReceived = exceptionAppender.getMessage() != null;
			assertTrue(
					"Retematcher should throw an exception since you did not advertise correctly before you published.",
					checkReceived);
		}
	}

	/**
	 * Test for exception that advertisement has duplicate attributes.
	 */
	public void testAdvWithDuplicateAttribute() {
		// For now padres just keep only one of them in mind, fail right now
		try {
			MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100],[price,<,30]");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that publication has duplicate attributes.
	 */
	public void testPubWithDuplicateAttribute() {
		// For now padres just keep only one of them in mind,fail right now
		try {
			MessageFactory.createPublicationFromString("[class,'stock'],[price,120],[price,130]");
		} catch (ParseException e) {
			return;
		}

		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription has duplicate attributes.
	 */
	public void testSubWithDuplicateAttribute() {
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100],[price,>,120]");
		} catch (ParseException e) {
			return;
		}

		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without comma between predicates with space instead
	 */
	public void testSubWithoutCommaWithSpaceBetweenPredicates() {
		// for now padres just add comma between predicates, and do not throw exception
		// matching can be executed correctly
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'] [price,<,100]");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);	
	}

	/**
	 * Test for exception that subscription without comma between predicates.
	 */
	public void testSubWithoutCommaAndSpaceBetweenPredicates() {
		// for now padres just add comma between predicates, and do not throw exception
		// matching can be excuted correctly
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'][price,<,100]");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without single quotes in string predicate.
	 * @throws ParseException 
	 */
	public void testSubWithoutSingleQuotesInStringPredicates() throws ParseException {
		// for now padres do not throw exception, and matching can be excuted correctly
		// this message format is allowed in Padres.
		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[price,<,100],[attribute,eq,'high']");
		MessageDestination mdA = clientA.getClientDest();
		String advId = brokerCore.getNewMessageID();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, advId, mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,stock],[price,<,100],[attribute,eq,high]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// setup the filter
		msgPatternFilter.setPattern(".*Publication.+stock.+");
		// route messages
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80],[attribute,'high']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,80],[attribute,'high'] should be matched",
				pub.equalVals(expectedPub));

		// Our message parser allows the message without single quotes.
	}

	/**
	 * Test for exception that subscription with double quotes in string predicate.
	 * @throws ParseException 
	 */
	public void testSubWithoutSingleQuotesWithDoubleQuotesInStringPredicates() throws ParseException {
		// for now padres do not throw exception, and matching can be excuted correctly
		// this message format is allowed in Padres.
		Advertisement adv = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[price,<,100],[attribute,eq,'high']");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Subscription sub = MessageFactory.createSubscriptionFromString(
				"[class,eq,\"stock\"],[price,<,100],[attribute,eq,\"high\"]");
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);

		// setup the filter
		msgPatternFilter.setPattern(".*Publication.+stock.+");
		// route the messages
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80],[attribute,'high']");
		PublicationMessage pubMsg = new PublicationMessage(pub, brokerCore.getNewMessageID(), mdA);
		brokerCore.routeMessage(pubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the message to be received
		messageWatcher.getMessage();
		Publication expectedPub = clientA.getCurrentPub();
		assertTrue(
				"The publication:[class,'stock'],[price,80],[attribute,'high'] should be matched",
				pub.equalVals(expectedPub));

		// Our message parser allows the message with double quotes.
	}

	/**
	 * Test for exception that subscription without brackets between predicates.
	 */
	public void testSubWithoutBracketBetweenPredicates() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("class,eq,'stock',price,<,100,attribute,eq,'high'");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without commas in predicate.
	 */
	public void testSubWithoutCommaInPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString(
				"[class eq 'stock'],[price < 100],[attribute eq 'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription with number operator in string predicate.
	 */
	public void testSubWithNumberOperatorInStringPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100],[attribute,=,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription with string operator in number predicate.
	 */
	public void testSubWithStringOperatorInNumberPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString(
				"[class,eq,'stock'],[price,str-lt,100],[attribute,eq,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without string operator in string predicate.
	 */
	public void testSubWithoutOperatorInStringPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100],[attribute,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without number operator in number predicate.
	 */
	public void testSubWithoutOperatorInNumberPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,100],[attribute,eq,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without string value in string predicate.
	 */
	public void testSubWithoutValueInStringPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100],[attribute,eq]");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without number value in number predicate.
	 */
	public void testSubWithoutValueInNumberPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<],[attribute,eq,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without string attribute in string predicate.
	 */
	public void testSubWithoutAttributeInStringPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,<,100],[eq,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that subscription without number attribute in number predicate.
	 */
	public void testSubWithoutAttributeInNumberPredicate() {
		// for now padres throws a malformed exception by message parser
		try {
			MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[<,100],[attribute,eq,'high']");
		} catch (ParseException e) {
			return;
		}
		
		assertTrue("No parse exception has been thrown.", false);
	}

	/**
	 * Test for exception that unsubscribe an unrecognized subscription.
	 * @throws ParseException 
	 */
	public void testUnsubscribeUnrecognizedSub() throws ParseException {
		MessageDestination mdA = clientA.getClientDest();
		Subscription sub = MessageFactory.createSubscriptionFromString("[class,eq,'stock'],[price,>,80]");
		String subId = brokerCore.getNewMessageID();
		SubscriptionMessage subMsg = new SubscriptionMessage(sub, subId, mdA);
		brokerCore.routeMessage(subMsg, MessageDestination.INPUTQUEUE);
		Unsubscription unsub = new Unsubscription(brokerCore.getBrokerID() + "-M100");
		UnsubscriptionMessage unsubMsg = new UnsubscriptionMessage(unsub,
				brokerCore.getNewMessageID(), mdA);
		// setup the filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + SubMsgNotFoundException.class.getName() + ".+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(unsubMsg, MessageDestination.INPUTQUEUE);
		// waiting for the exception to be thrown
		boolean checkReceived = exceptionAppender.getMessage() != null;
		assertTrue("An unrecognized subscription exception should be thrown.", checkReceived);
	}

	/**
	 * Test for exception that unadvertise an unrecognized advertisement.
	 * @throws ParseException 
	 */
	public void testUnadvertiseUnrecognizedAdv() throws ParseException {
		// for now padres did not implement unadvertisement, so did not throw
		// any exception. fail right now
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		MessageDestination mdA = clientA.getClientDest();
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
				mdA);
		brokerCore.routeMessage(advMsg, MessageDestination.INPUTQUEUE);

		Unadvertisement unadv = new Unadvertisement(brokerCore.getBrokerID() + "-M100");
		UnadvertisementMessage unadvMsg = new UnadvertisementMessage(unadv,
				brokerCore.getNewMessageID(), mdA);
		// setup the filter
		exceptionPatternFilter = new PatternFilter(Router.class.getName());
		exceptionPatternFilter.setLevel(Level.ERROR);
		exceptionPatternFilter.setPattern(".*" + AdvMsgNotFoundException.class.getName() + ".+");
		exceptionAppender.clearFilters();
		exceptionAppender.addFilter(exceptionPatternFilter);
		// route messages
		brokerCore.routeMessage(unadvMsg, MessageDestination.INPUTQUEUE);
		// waiting for the exception to be thrown
		boolean checkReceived = exceptionAppender.getMessage() != null;
		assertTrue("An unrecognized advertisement exception should be thrown.", checkReceived);
	}

}

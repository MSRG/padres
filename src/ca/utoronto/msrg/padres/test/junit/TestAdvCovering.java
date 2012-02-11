package ca.utoronto.msrg.padres.test.junit;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterClient;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test subscription covering function with ACTIVE strategy.
 * 
 * @author Chen Chen, Bala Maniymaran
 */

public class TestAdvCovering extends TestCase {

	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "4");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "rmi");
	}

	protected GenericBrokerTester _brokerTester;

	protected BrokerCore brokerCore1;

	protected BrokerCore brokerCore2;

	protected BrokerCore brokerCore3;

	protected BrokerCore brokerCore4;

	protected Client clientA;

	protected Client clientB;

	protected Client clientC;

	MessageWatchAppender messageWatcher;

	PatternFilter msgFilter;

	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		AllTests.setupStarNetwork01();

		brokerCore1 = createNewBrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		brokerCore2 = createNewBrokerCore(AllTests.brokerConfig02);
		brokerCore2.initialize();
		brokerCore3 = createNewBrokerCore(AllTests.brokerConfig03);
		brokerCore3.initialize();
		brokerCore4 = createNewBrokerCore(AllTests.brokerConfig04);
		brokerCore4.initialize();

		// start swingClientA for Broker2
		clientA = createNewClient("A");
		clientA.connect(brokerCore2.getBrokerURI());

		// start swingClientB for Broker3
		clientB = createNewClient("B");
		clientB.connect(brokerCore3.getBrokerURI());

		// start swingClientC for Broker4
		clientC = createNewClient("C");
		clientC.connect(brokerCore4.getBrokerURI());

		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);
	}

	protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, brokerConfig);
	}

	protected Client createNewClient(String clientId) throws ClientException {
		return new TesterClient(_brokerTester, clientId);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		brokerCore3.shutdown();
		brokerCore4.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
		_brokerTester = null;
	}

	/**
	 * Test covering with multibrokers, where broker1 is the core, broker2,3,4 connect to broker1
	 * seperately. clientA,B,C connect to broker2,3,4 respectively. clientA is publisher, clientB,C
	 * are subscribers. adv1 from clientB covers/equals to adv2 from clientC
	 * 
	 * @throws BrokerCoreException
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testAdv1CoversOrEqualsToAdv2() throws BrokerCoreException, ClientException, ParseException, InterruptedException {
		/* TODO: REZA (NEW-) */
		_brokerTester.clearAll().
			expectRouterAddAdvertisement(
				brokerCore4.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore3.getBrokerURI(),
				brokerCore1.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L)).
			expectRouterAddAdvertisement(
				brokerCore1.getBrokerURI(),
				brokerCore2.getBrokerDestination(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 100L));
		
		// adv1 from client A
		Advertisement adv1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,100]");
		AdvertisementMessage advMsg1 = new AdvertisementMessage(adv1,
				brokerCore2.getNewMessageID(), clientA.getClientDest());
		brokerCore2.routeMessage(advMsg1, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
		
		
		_brokerTester.clearAll().
			expectRouterNotAddAdvertisement(
				brokerCore1.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L)).
			expectRouterNotAddAdvertisement(
				brokerCore3.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L)).
			expectRouterNotAddAdvertisement(
				brokerCore4.getBrokerURI(),
				null,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "stock").
					addPredicate("price", ">", 150L));

		// adv2 from clientA ( adv1 covers adv2 )
		Advertisement adv2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[price,>,150]");
		AdvertisementMessage advMsg2 = new AdvertisementMessage(adv2,
				brokerCore2.getNewMessageID(), clientA.getClientDest());
		brokerCore2.routeMessage(advMsg2, MessageDestination.INPUTQUEUE);
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen());
	}
}

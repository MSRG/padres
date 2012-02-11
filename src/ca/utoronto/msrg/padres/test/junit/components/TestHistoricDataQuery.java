package ca.utoronto.msrg.padres.test.junit.components;

import java.io.File;
import java.sql.SQLException;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.MessageType;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.AllTests;
import ca.utoronto.msrg.padres.test.junit.MessageWatchAppender;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;
import ca.utoronto.msrg.padres.test.junit.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.test.junit.tester.TesterBrokerCore;
import ca.utoronto.msrg.padres.test.junit.tester.TesterClient;
import ca.utoronto.msrg.padres.test.junit.tester.TesterMessagePredicates;

/**
 * This class provides a way to test historic data query function. In order to run this class,
 * database postgresql need to be set up first, and related tables need to be created. Please refer
 * src/main/binding/dbbinding/*.sql to create tables.
 * 
 * You need to create two databases: postgres and postgresB and create the necessary tables in them
 * (execute src/main/binding/dbbinding/create_table.sql in each database).
 * 
 * @author shou
 */
public class TestHistoricDataQuery extends TestCase {

	static {
		if(System.getProperty("test.version") == null)
			System.setProperty("test.version", "1");
		if(System.getProperty("test.comm_protocol") == null)
			System.setProperty("test.comm_protocol", "socket");
	}
	
	protected GenericBrokerTester _brokerTester;
	
	protected final int DEFAULT_WAIT_TESTS = 25000;
	
	protected BrokerCore brokerCore1;

	protected BrokerCore brokerCore2;

	private String propsFileName1;

	private String propsFileName2;

	protected Client clientA;

	protected Client clientB;

	protected Client clientC;

	protected MessageWatchAppender messageWatcher;

	PatternFilter msgFilter;

	protected MessageWatchAppender clientMessageWatcher;

	protected PatternFilter clientMsgFilter;

	@Override
	protected void setUp() throws Exception {
		_brokerTester = new GenericBrokerTester();
		
		propsFileName1 = BrokerConfig.PADRES_HOME + "etc" + File.separator + "db" + File.separator
				+ "padresHD.properties";
		// start the broker
		String[] brokerArgs = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, propsFileName1 };
		brokerCore1 = createNewBrokerCore(brokerArgs);

		// start the broker2
		propsFileName2 = BrokerConfig.PADRES_HOME + "etc" + File.separator + "db" + File.separator
				+ "padresHDBrokerB.properties";
		String[] brokerArgs2 = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, propsFileName2 };
		brokerCore2 = createNewBrokerCore(brokerArgs2);
		clientA = createNewClient(AllTests.clientConfigA);
		clientB = createNewClient(AllTests.clientConfigB);
		clientC = createNewClient(AllTests.clientConfigC);

		_brokerTester.expectCommSystemStart(brokerCore1.getBrokerURI(), null).
			expectCommSystemStart(brokerCore2.getBrokerURI(), null);
			
		initializeBrokers();

		// start the swing clientA
		clientA.connect(brokerCore1.getBrokerURI());
		// start the swing clientB
		clientB.connect(brokerCore1.getBrokerURI());
		// start the swing clientB
		
		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);

		clientMessageWatcher = new MessageWatchAppender();
		clientMsgFilter = new PatternFilter(Client.class.getName());
		clientMessageWatcher.addFilter(clientMsgFilter);
		LogSetup.addAppender("MessagePath", clientMessageWatcher);
		
		try { Thread.sleep(3000); } catch (InterruptedException itx) {}
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
		
		_brokerTester.waitUntilExpectedStartsHappen();
	}

	protected void initializeBrokers() throws BrokerCoreException {
		_brokerTester.
			expectDBHandlerInit(brokerCore1.getBrokerURI()).
			expectDBHandlerInit(brokerCore2.getBrokerURI());
		brokerCore1.initialize();
		brokerCore2.initialize();
	}
	
	protected Client createNewClient(ClientConfig newConfig) throws ClientException {
		return new TesterClient(_brokerTester, newConfig);
	}

	protected BrokerCore createNewBrokerCore(String[] args) throws BrokerCoreException {
		return new TesterBrokerCore(_brokerTester, args);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		_brokerTester = null;
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		brokerCore1.shutdown();
		brokerCore2.shutdown();
		LogSetup.removeAppender("MessagePath", messageWatcher);
		LogSetup.removeAppender("MessagePath", clientMessageWatcher);
	}

	/**
	 * Test historic data query function with multiple brokers, where broker1 and broker2 connect
	 * each other. They have their own database respectively. clientA is a DB client, and connects
	 * to broker1. clientB,C connect to broker1,2 respectively. clientB is a publisher, and clientC
	 * is a subscriber.
	 * 
	 * @throws SQLException
	 * @throws BrokerCoreException
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHistoricDataQueryWithMultipleBrokers() throws SQLException, BrokerCoreException, ClientException, ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		clientC.connect(brokerCore2.getBrokerURI());

		brokerCore1.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();
		brokerCore2.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();

		//
		// Setup databases.
		//
		
		// Send DB_CONTROL adv and wait for matching sub from Broker2 to get back.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.ADVERTISEMENT,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "DB_CONTROL").
					addPredicate("command", "isPresent", "any").
					addPredicate("content_spec", "isPresent", "any").
					addPredicate("database_id", "isPresent", "any"),			
				"INPUTQUEUE").
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "DB_CONTROL").
					addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"),			
				"INPUTQUEUE").
			expectRouterHandle(
				brokerCore1.getBrokerURI(),
				null,
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "DB_CONTROL").
					addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"));
		clientA.handleCommand("a [class,eq,'DB_CONTROL'],[command,isPresent,'any'],[content_spec,isPresent,'any'],[database_id,isPresent,'any']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Make sure DB_CONTROL sub from Broker2 exists in the routing table. 
		Subscription dbsub = MessageFactory.createSubscriptionFromString("[class,eq,DB_CONTROL],[database_id,eq,'" + brokerCore2.getBrokerDestination() + "-DB']");
		boolean dbsubstate = brokerCore1.getRouter().checkStateForSubscription(brokerCore2.getBrokerDestination(), dbsub);
		assertTrue("The DB_CONTROL subscription should be sent to Broker1", dbsubstate);

		// Configure Broker1-DB to store some IBM pubs.
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "DB_CONTROL").
					addPredicate("command", "eq", "STORE").
					addPredicate("content_spec", "eq", "[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]").
					addPredicate("database_id", "eq", "" + brokerCore1.getBrokerDestination() + "-DB"),			
				"INPUTQUEUE");
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Configure Broker2-DB to store some MS pubs.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "eq", "STORE").
			addPredicate("content_spec", "eq", "[class,eq,stock],[symbol,eq,MS],[price,>,23.45],[volume,<,100]").
			addPredicate("database_id", "eq", "" + brokerCore2.getBrokerDestination() + "-DB"),			
			"INPUTQUEUE");
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[symbol,eq,MS],[price,>,23.45],[volume,<,100]'],[database_id,'"	+ brokerCore2.getBrokerDestination() + "-DB']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send publications.
		//
		
		// Send IBM stock adv and wait for adv to propagate to Broker2.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 150.02d).
			addPredicate("volume", ">", 0L),			
			"INPUTQUEUE");
		clientB.handleCommand("a [class,eq,'stock'],[symbol,eq,'IBM'],[price,<,150.02],[volume,>,0]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send an IBM stock pub and wait for it to reach Broker1.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "=", 10.01d).
			addPredicate("volume", "=", 110L),			
			"INPUTQUEUE");
		clientB.handleCommand("p [class,'stock'],[symbol,'IBM'],[price,10.01],[volume,110]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send MS stock adv and wait for matching sub from Broker2 to get back.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "MS").
			addPredicate("price", "<", 150.02d).
			addPredicate("volume", ">", 0L),			
			"INPUTQUEUE").
		expectRouterHandle(
				brokerCore1.getBrokerURI(),
				null,
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("symbol", "eq", "MS").
				addPredicate("price", ">", 23.45d).
				addPredicate("volume", "<", 100L)			
				);
		clientB.handleCommand("a [class,eq,'stock'],[symbol,eq,'MS'],[price,<,150.02],[volume,>,0]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send an MS stock pub and wait for it to propagate to Broker2.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "MS").
			addPredicate("price", "=", 40.01d).
			addPredicate("volume", "=", 50L),			
			"INPUTQUEUE").
		expectRouterHandle(
				brokerCore2.getBrokerURI(),
				null,
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock").
				addPredicate("symbol", "eq", "MS").
				addPredicate("price", "=", 40.01d).
				addPredicate("volume", "=", 50L));
		clientB.handleCommand("p [class,'stock'],[symbol,'MS'],[price,40.01],[volume,50]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
		
		//
		// Send historic subscriptions.
		//

		// Send a historic sub for IBM pub and wait for client C to get matching pub. 
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "historic").
			addPredicate("subclass", "eq", "stock").
			addPredicate("_query_id", "eq", "q123").
			addPredicate("price", "<", 123.45d).
			addPredicate("volume", ">", 100L).
			addPredicate("symbol", "eq", "IBM"),
			"INPUTQUEUE").
		expectClientReceivePublication(
			clientC.getClientID(),
			new TesterMessagePredicates().
				addPredicate("subclass", "eq", "stock").
				addPredicate("_query_id", "eq", "q123").
				addPredicate("price", "=", 10.01d).
				addPredicate("volume", "=", 110L).
				addPredicate("symbol", "eq", "IBM"));
		clientC.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q123'],[volume,>,100],[price,<,123.45],[symbol,eq,IBM]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
		
		// Send a historic sub for MS pub and wait for client B to get matching pub. 
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "historic").
				addPredicate("subclass", "eq", "stock").
				addPredicate("_query_id", "eq", "q123").
				addPredicate("price", "<", 50.45d).
				addPredicate("volume", "<", 100L).
				addPredicate("symbol", "eq", "MS"),
				"INPUTQUEUE").
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "historic").
					addPredicate("subclass", "eq", "stock").
					addPredicate("_query_id", "eq", "q123").
					addPredicate("price", "=", 40.01d).
					addPredicate("volume", "=", 50L).
					addPredicate("symbol", "eq", "MS"));
		clientB.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q123'],[volume,<,100],[price,<,50.45],[symbol,eq,MS]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send a historic sub for IBM pub and wait for client B to get matching pub. 
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
					addPredicate("class", "eq", "historic").
					addPredicate("subclass", "eq", "stock").
					addPredicate("_query_id", "eq", "q5").
					addPredicate("symbol", "eq", "IBM").
					addPredicate("price", "<", 200.01d).
					addPredicate("volume", ">", 10L),
				"INPUTQUEUE").
			expectClientReceivePublication(
				clientB.getClientID(),
				new TesterMessagePredicates().
					addPredicate("class", "eq", "historic").
					addPredicate("subclass", "eq", "stock").
					addPredicate("_query_id", "eq", "q5").
					addPredicate("price", "=", 10.01d).
					addPredicate("volume", "=", 110L).
					addPredicate("symbol", "eq", "IBM"));
		clientB.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q5'],[symbol,eq,'IBM'],[price,<,200.01],[volume,>,10],[_start_time,eq,'Tue Feb 20 22:23:59 EDT 2007'],[_end_time,eq,'Thu Mar 22 22:23:59 EDT 2018']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
	}

	/**
	 * Test historic data query function with one broker, where clientA,B,C connect to broker1.
	 * clientA is a DB client, clientB is a publisher, and clientC is a subscriber.
	 * 
	 * @throws SQLException
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHistoricDataQueryWithOneBroker() throws SQLException, ClientException, ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		clientC.connect(brokerCore1.getBrokerURI());

		brokerCore1.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();

		//
		// Setup databases.
		//
		
		// Send DB_CONTROL adv and wait for matching sub from Broker2 to get back.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "isPresent", "any").
			addPredicate("content_spec", "isPresent", "any").
			addPredicate("database_id", "isPresent", "any"),			
			"INPUTQUEUE").
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"),			
			"INPUTQUEUE").
		expectRouterHandle(
			brokerCore1.getBrokerURI(),
			null,
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"));
		clientA.handleCommand("a [class,eq,'DB_CONTROL'],[command,isPresent,'any'],[content_spec,isPresent,'any'],[database_id,isPresent,'any']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Make sure DB_CONTROL sub from Broker2 exists in the routing table.
		/*
		Subscription dbsub = MessageFactory.createSubscriptionFromString("[class,eq,DB_CONTROL],[database_id,eq,'" + brokerCore2.getBrokerDestination() + "-DB']");
		boolean dbsubstate = brokerCore1.getRouter().checkStateForSubscription(brokerCore2.getBrokerDestination(), dbsub);
		assertTrue("The DB_CONTROL subscription should be sent to Broker1", dbsubstate);
		*/
		// Configure Broker1-DB to store some IBM pubs.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "eq", "STORE").
			addPredicate("content_spec", "eq", "[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]").
			addPredicate("database_id", "eq", "" + brokerCore1.getBrokerDestination() + "-DB"),			
			"INPUTQUEUE");
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send publications.
		//
		
		// Send IBM stock adv and wait for adv to propagate to Broker2.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 150.02d).
			addPredicate("volume", ">", 0L),			
			"INPUTQUEUE");
		clientB.handleCommand("a [class,eq,'stock'],[symbol,eq,'IBM'],[price,<,150.02],[volume,>,0]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send an IBM stock pub and wait for it to reach Broker1.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "=", 10.01d).
			addPredicate("volume", "=", 110L),			
			"INPUTQUEUE");
		clientB.handleCommand("p [class,'stock'],[symbol,'IBM'],[price,10.01],[volume,110]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send historic subscriptions.
		//

		// Send a historic sub for IBM pub and wait for client C to get matching pub. 
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "historic").
			addPredicate("subclass", "eq", "stock").
			addPredicate("_query_id", "eq", "q123").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 123.45d).
			addPredicate("volume", ">", 100L),
			"INPUTQUEUE").
		expectClientReceivePublication(
			clientC.getClientID(),			
			new TesterMessagePredicates().
			addPredicate("class", "eq", "historic").
			addPredicate("subclass", "eq", "stock").
			addPredicate("_query_id", "eq", "q123").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "=", 10.01d).
			addPredicate("volume", "=", 110L));
		clientC.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q123'],[volume,>,100],[price,<,123.45],[symbol,eq,IBM]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
	}

	/**
	 * Test historic data query function with one broker, where clientA,B,C connect to broker1.
	 * clientA is a DB client, clientB is a publisher, and clientC is a subscriber.
	 * 
	 * @throws SQLException
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHistoricDataQueryWithOneBrokerWithRegularSub() throws SQLException,	ClientException, ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		clientC.connect(brokerCore1.getBrokerURI());

		brokerCore1.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();

		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "isPresent", "any").
			addPredicate("content_spec", "isPresent", "any").
			addPredicate("database_id", "isPresent", "any"),			
			"INPUTQUEUE").
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"),			
			"INPUTQUEUE").
		expectRouterHandle(
			brokerCore1.getBrokerURI(),
			null,
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"));
		// Send DB_CONTROL adv and wait for matching sub from Broker2 to get back.
		clientA.handleCommand("a [class,eq,'DB_CONTROL'],[command,isPresent,'any'],[content_spec,isPresent,'any'],[database_id,isPresent,'any']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Make sure DB_CONTROL sub from Broker2 exists in the routing table. 
		Subscription dbsub = MessageFactory.createSubscriptionFromString("[class,eq,DB_CONTROL],[database_id,eq,'" + brokerCore2.getBrokerDestination() + "-DB']");
		boolean dbsubstate = brokerCore1.getRouter().checkStateForSubscription(brokerCore2.getBrokerDestination(), dbsub);
		assertTrue("The DB_CONTROL subscription should be sent to Broker1", dbsubstate);

		// Configure Broker1-DB to store some IBM pubs.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "eq", "STORE").
			addPredicate("content_spec", "eq", "[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]").
			addPredicate("database_id", "eq", "" + brokerCore1.getBrokerDestination() + "-DB"),			
			"INPUTQUEUE");
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[symbol,eq,IBM],[price,<,123.45],[volume,>,100]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send publications.
		//
		
		// Send IBM stock adv and wait for adv to propagate to Broker2.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 150.02d).
			addPredicate("volume", ">", 0L),			
			"INPUTQUEUE");
		clientB.handleCommand("a [class,eq,'stock'],[symbol,eq,'IBM'],[price,<,150.02],[volume,>,0]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send an IBM stock pub and wait for it to reach Broker1.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "=", 10.01d).
			addPredicate("volume", "=", 110L),			
			"INPUTQUEUE");
		clientB.handleCommand("p [class,'stock'],[symbol,'IBM'],[price,10.01],[volume,110]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send regular subscriptions.
		//

		// Send a regular sub for IBM pub and wait for it to reach Broker1. 
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 45.45d).
			addPredicate("volume", ">", 100L),			
			"INPUTQUEUE").
		expectNegativeReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock"),
			"INPUTQUEUE");			
		clientB.handleCommand("s [class,eq,'stock'],[volume,>,100],[price,<,45.45],[symbol,eq,IBM]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
		
		//
		// Send historic subscriptions.
		//

		// Send a historic sub for IBM pub and wait for client C to get matching pub. 
		_brokerTester.clearAll().
			expectReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.SUBSCRIPTION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "historic").
				addPredicate("subclass", "eq", "stock").
				addPredicate("_query_id", "eq", "q123").
				addPredicate("symbol", "eq", "IBM").
				addPredicate("price", "<", 123.45d).
				addPredicate("volume", ">", 100L),
				"INPUTQUEUE").
			expectNegativeReceipt(
				brokerCore1.getBrokerURI(),
				MessageType.PUBLICATION,
				new TesterMessagePredicates().
				addPredicate("class", "eq", "stock"),
				"INPUTQUEUE");	
		clientC.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q123'],[volume,>,100],[price,<,123.45],[symbol,eq,IBM]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
	}

	/**
	 * Test historic data query function with one broker, where clientA,B,C connect to broker1.
	 * clientA is a DB client, clientB is a publisher, and clientC is a subscriber.
	 * 
	 * @throws SQLException
	 * @throws ClientException
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public void testHistoricDataQueryWithOneBrokerWithSubHavingMoreAttributes() throws SQLException, ClientException, ParseException, InterruptedException {
		/* TODO: YOUNG (DONE) */
		clientC.connect(brokerCore1.getBrokerURI());

		brokerCore1.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();

		//
		// Setup databases.
		//
				
		
		// Send DB_CONTROL adv and wait for matching sub from Broker2 to get back.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "isPresent", "any").
			addPredicate("content_spec", "isPresent", "any").
			addPredicate("database_id", "isPresent", "any"),			
			"INPUTQUEUE").
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"),			
			"INPUTQUEUE").
		expectRouterHandle(
			brokerCore1.getBrokerURI(),
			null,
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("database_id", "eq", brokerCore2.getBrokerURI() + "-DB"));
		clientA.handleCommand("a [class,eq,'DB_CONTROL'],[command,isPresent,'any'],[content_spec,isPresent,'any'],[database_id,isPresent,'any']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Make sure DB_CONTROL sub from Broker2 exists in the routing table. 
		Subscription dbsub = MessageFactory.createSubscriptionFromString("[class,eq,DB_CONTROL],[database_id,eq,'" + brokerCore2.getBrokerDestination() + "-DB']");
		boolean dbsubstate = brokerCore1.getRouter().checkStateForSubscription(brokerCore2.getBrokerDestination(), dbsub);
		assertTrue("The DB_CONTROL subscription should be sent to Broker1", dbsubstate);

		// Configure Broker1-DB to store some IBM pubs.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "DB_CONTROL").
			addPredicate("command", "eq", "STORE").
			addPredicate("content_spec", "eq", "[class,eq,stock],[symbol,eq,IBM],[price,<,123.45]").
			addPredicate("database_id", "eq", "" + brokerCore1.getBrokerDestination() + "-DB"),			
			"INPUTQUEUE");
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[symbol,eq,IBM],[price,<,123.45]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		//
		// Send publications.
		//
		
		// Send IBM stock adv and wait for adv to propagate to Broker2.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.ADVERTISEMENT,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 150.02d).
			addPredicate("volume", ">", 0L),			
			"INPUTQUEUE");
		clientB.handleCommand("a [class,eq,'stock'],[symbol,eq,'IBM'],[price,<,150.02],[volume,>,0]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));

		// Send an IBM stock pub and wait for it to reach Broker1.
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.PUBLICATION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "stock").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "=", 10.01d).
			addPredicate("volume", "=", 110L),			
			"INPUTQUEUE");
		clientB.handleCommand("p [class,'stock'],[symbol,'IBM'],[price,10.01],[volume,110]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
		//
		// Send historic subscriptions.
		//

		// Send a historic sub for IBM pub and wait for it to reach Broker1. 
		// This subscription has more attributes than the historic DB is configured for,
		// so even though there's a matching publication in the DB, it will not match.
		// This is either a bug in padres or in the historic DB implementation. 
		_brokerTester.clearAll().
		expectReceipt(
			brokerCore1.getBrokerURI(),
			MessageType.SUBSCRIPTION,
			new TesterMessagePredicates().
			addPredicate("class", "eq", "historic").
			addPredicate("subclass", "eq", "stock").
			addPredicate("_query_id", "eq", "q123").
			addPredicate("symbol", "eq", "IBM").
			addPredicate("price", "<", 123.45d).
			addPredicate("volume", ">", 100L),			
			"INPUTQUEUE");
		clientC.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[_query_id,eq,'q123'],[volume,>,100],[price,<,123.45],[symbol,eq,IBM]");
		assertTrue(_brokerTester.waitUntilExpectedEventsHappen(DEFAULT_WAIT_TESTS));
	}	
}

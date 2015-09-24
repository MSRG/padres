package ca.utoronto.msrg.padres.test.junit.components;

import java.sql.SQLException;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.test.junit.AllTests;
import ca.utoronto.msrg.padres.test.junit.MessageWatchAppender;
import ca.utoronto.msrg.padres.test.junit.PatternFilter;

/**
 * This class provides a way to test historic data query function intersecting with other features.
 * In order to run this class, database postgresql need to be set up first, and related tables need
 * to be created. Please refer src/main/binding/dbbinding/*.sql to create tables.
 * 
 * @author shou, Bala Maniymaran
 */

public class TestHistoricDataQueryWithCyclicRouting extends TestCase {

	private BrokerCore brokerCore1;

	private Client clientA;

	private Client clientB;

	private Client clientC;

	MessageWatchAppender messageWatcher;

	PatternFilter msgFilter;

	MessageWatchAppender clientMessageWatcher;

	PatternFilter clientMsgFilter;


	protected void setUp() throws Exception {
		// start a broker
		String[] managers = { "DB" };
		AllTests.brokerConfig01.setManagers(managers);
		AllTests.brokerConfig01.setDbPropertyFileName("etc/db/db.properties");
		brokerCore1 = new BrokerCore(AllTests.brokerConfig01);
		brokerCore1.initialize();
		String brokerAddress = brokerCore1.getBrokerURI();

		// start the clientA
		clientA = new Client(AllTests.clientConfigA);
		clientA.connect(brokerAddress);
		// start the clientB
		clientB = new Client(AllTests.clientConfigB);
		clientB.connect(brokerAddress);
		// start the clientC
		clientC = new Client(AllTests.clientConfigC);
		clientC.connect(brokerAddress);

		messageWatcher = new MessageWatchAppender();
		msgFilter = new PatternFilter(InputQueueHandler.class.getName());
		messageWatcher.addFilter(msgFilter);
		LogSetup.addAppender("MessagePath", messageWatcher);

		clientMessageWatcher = new MessageWatchAppender();
		clientMsgFilter = new PatternFilter(Client.class.getName());
		clientMessageWatcher.addFilter(clientMsgFilter);
		LogSetup.addAppender("MessagePath", clientMessageWatcher);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	
		clientA.shutdown();
		clientB.shutdown();
		clientC.shutdown();
		
		if (brokerCore1 != null)
			brokerCore1.shutdown();
		
		LogSetup.removeAppender("MessagePath", messageWatcher);
		LogSetup.removeAppender("MessagePath", clientMessageWatcher);
	}

	/**
	 * Test historic data query function is intersecting with cyclic routing, where clientA,B,C
	 * connect to broker1. clientA is a DB client, clientB is a publisher, and clientC is a
	 * subscriber.
	 * 
	 * @throws SQLException
	 * @throws BrokerCoreException
	 * @throws ClientException
	 * @throws ParseException 
	 */
	public void testHistoricDataQueryIntersectCyclicRouting() throws SQLException,
			BrokerCoreException, ClientException, ParseException {
		brokerCore1.getController().getLifeCycleManager().getDBHandler().getDBConnector().clearTables();

		//
		// Setup databases.
		//
		
		// Send DB_CONTROL adv and wait for it to reach Broker1.
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Advertisement.+DB_CONTROL.+");
		messageWatcher.clear();
		clientA.handleCommand("a [class,eq,'DB_CONTROL'],[command,isPresent,'any'],[content_spec,isPresent,'any'],[database_id,isPresent,'any']");
		String watcherMsg = messageWatcher.getMessage(); // wait for matching subscription to propagate back.
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);

		// Configure Broker1-DB to store some IBM pubs and wait for DB to issue sub.
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Subscription.+stock.+");
		messageWatcher.clear();
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,stock],[comp,eq,ibm],[price,isPresent,100]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		watcherMsg = messageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);

		//
		// Send publications.
		//
		
		// Directly inject an IBM stock adv and wait for adv to reach Broker1.
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Advertisement.+stock.+");
		messageWatcher.clear();
		Advertisement adv = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		String tid = brokerCore1.getNewMessageID();
		int index = tid.indexOf("M");
		int newNum = Integer.parseInt(tid.substring(index + 1));
		newNum--;
		AdvertisementMessage advMsg = new AdvertisementMessage(adv, tid, clientB.getClientDest());
		brokerCore1.routeMessage(advMsg, MessageDestination.INPUTQUEUE);
		watcherMsg = messageWatcher.getMessage(); // probably not necessary because there are no subs are propagated back. 
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);
		
		// Send an IBM stock pub and wait for it to reach Broker1.
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Publication.+stock.+");
		messageWatcher.clear();
		clientB.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		watcherMsg = messageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);

		//
		// Send historic subscriptions.
		//

		// Send a historic sub for IBM pub and wait for client C to get matching pub. 
		clientMsgFilter.setPattern(".*Message received at Client " + clientC.getClientID() + ".+Publication.+stock.+");
		clientMessageWatcher.clear();
		clientC.handleCommand("s [class,eq,'historic'],[subclass,eq,'stock'],[comp,eq,'ibm'],[price,>,100],[_query_id,eq,'q123']");
		watcherMsg = clientMessageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);
		
		// Make sure client C got the pub.
		Publication expectedPub = MessageFactory.createPublicationFromString("[class,'historic'],[subclass,'stock'],[_query_id,'q123'],[comp,'ibm'],[price,120],[tid,'" + brokerCore1.getBrokerDestination() + "-M" + +newNum + "']");
		Publication foundPub = clientC.getCurrentPub();
		assertTrue("The publication: " + expectedPub + " should be matched", foundPub.equalVals(expectedPub));
	}
}

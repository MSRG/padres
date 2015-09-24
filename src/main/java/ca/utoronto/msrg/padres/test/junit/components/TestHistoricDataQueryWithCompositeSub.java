package ca.utoronto.msrg.padres.test.junit.components;

import java.sql.SQLException;
import java.util.Set;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.brokercore.InputQueueHandler;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
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

public class TestHistoricDataQueryWithCompositeSub extends TestCase {

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
	}

	/**
	 * Test historic data query function is intersecting with composite subscription having string
	 * variable, where clientA,B,C connect to broker1. clientA is a DB client, clientB is a
	 * publisher, and clientC is a subscriber.
	 * 
	 * @throws SQLException
	 * @throws BrokerCoreException
	 * @throws ClientException
	 * @throws ParseException 
	 */
	public void testHistoricDataQueryIntersectCompositeSubscriptionWithVariable()
			throws SQLException, BrokerCoreException, ClientException, ParseException {
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

		// Configure Broker1-DB to store some IBM pubs and wait for DB to issue sub.
		msgFilter.setPattern(".*" + brokerCore1.getBrokerURI() + ".+got message.+Subscription.+trade.+");
		messageWatcher.clear();
		clientA.handleCommand("p [class,'DB_CONTROL'],[command,'STORE'],[content_spec,'[class,eq,trade],[buyer,isPresent,ibm],[bargainor,isPresent,MS]'],[database_id,'" + brokerCore1.getBrokerDestination() + "-DB']");
		watcherMsg = messageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);

		//
		// Send publications.
		//

		// Note: We don't need to wait for these to propagate because there's only
		//       one broker in this test.
		clientB.handleCommand("a [class,eq,'stock'],[comp,eq,'ibm'],[price,isPresent,100]");
		clientB.handleCommand("p [class,'stock'],[comp,'ibm'],[price,120]");
		clientB.handleCommand("a [class,eq,'trade'],[buyer,isPresent,'ibm'],[bargainor,isPresent,'MS']");
		clientB.handleCommand("p [class,'trade'],[buyer,'ibm'],[bargainor,'MS']");

		//
		// Send historic subscriptions.
		//
		
		// Send a composite historic sub. 
		clientMsgFilter.setPattern(".*Message received at Client " + clientC.getClientID() + ".+Publication.+_query_id.+");
		clientMessageWatcher.clear();
		clientC.handleCommand("cs {{[class,eq,'historic'],[subclass,eq,'stock'],[comp,eq,$S$X],[price,>,100],[_query_id,eq,'q123']}"
				+ "&{[class,eq,'historic'],[subclass,eq,'trade'],[buyer,eq,$S$X],[bargainor,eq,'MS'],[_query_id,eq,'q12']}}");

		// Unfortunately, the Client only records the most recent received publication,
		// whereas for this test we need to check for the last two publications.  So, we:
		// (1) Look for the pubs at the client's logs.
		//     This is deterministic (e.g., heartbeats won't interfere) but not reliable
		//     (we just do partial string match on the log message so there may be false positives).
		// (2) Look for the pubs in the last set of messages the broker forwarded.
		//     This is not deterministic but is reliable.
		// TODO: Find a better way to test this.

		// (1) Look for the pubs at the client's logs.
		watcherMsg = clientMessageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);
		watcherMsg = clientMessageWatcher.getMessage();
		assertTrue("Timeout waiting for message to propagate.", watcherMsg != null);
		
		// (2) Look for the pubs in the last set of messages the broker forwarded.
		Set<Message> messages = brokerCore1.getInputQueue().getCurrentMessagesToRoute();
		Publication pub1 = MessageFactory.createPublicationFromString("[class,'historic'],[subclass,'stock'],[_query_id,'q123'],[comp,'ibm'],[price,120]");
		Publication pub2 = MessageFactory.createPublicationFromString("[class,'historic'],[subclass,'trade'],[_query_id,'q12'],[buyer,'ibm'],[bargainor,'MS']");
		boolean pub1Found = false;
		boolean pub2Found = false;
		assertTrue("There should have two pubs routed out on Broker1", messages.size() == 2);
		for (Message m : messages) {
			if (m.getLastHopID().equals(brokerCore1.getBrokerDestination())	&& m.getNextHopID().equals(clientC.getClientDest())) {
				Publication p = ((PublicationMessage)m).getPublication();
				if (!pub1Found && p.equalVals(pub1))
					pub1Found = true;
				if (!pub2Found && p.equalVals(pub2))
					pub2Found = true;
			}
		}
		assertTrue("The publication " + pub1 + " should be sent to ClientC", pub1Found);
		assertTrue("The publication " + pub2 + " should be sent to ClientC", pub2Found);
	}
}

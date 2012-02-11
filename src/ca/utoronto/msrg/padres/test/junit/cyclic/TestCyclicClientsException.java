package ca.utoronto.msrg.padres.test.junit.cyclic;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.test.junit.TestClientsException;

/**
 * This class provides a way for exception handling test in the scenario of one broker with
 * swingRmiClient. In order to run this class, rmiregistry 1099 need to be done first.
 * 
 * @author Bala Maniymaran
 */
public class TestCyclicClientsException extends TestClientsException {

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
		String tidPredicate = ",[tid,'" + advId + "']";
		Publication pub = MessageFactory.createPublicationFromString("[class,'stock'],[price,80],[attribute,'high']"
				+ tidPredicate);
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

}

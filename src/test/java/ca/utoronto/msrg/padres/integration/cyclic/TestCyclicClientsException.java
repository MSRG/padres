package ca.utoronto.msrg.padres.integration.cyclic;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.integration.tester.GenericBrokerTester;
import ca.utoronto.msrg.padres.integration.tester.TesterBrokerCore;
import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.integration.TestClientsException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * This class provides a way for exception handling test in the scenario of one broker with
 * swingRmiClient. In order to run this class, rmiregistry 1099 need to be done first.
 *
 * @author Bala Maniymaran
 */
@RunWith(Parameterized.class)
public class TestCyclicClientsException extends Assert {
    @Parameterized.Parameter(value = 0)
    public int configuration;

    @Parameterized.Parameter(value = 1)
    public String method;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {6, "socket"}, {6, "rmi"},
                {7, "socket"}, {7, "rmi"}
        });
    }

    protected GenericBrokerTester _brokerTester;

    protected BrokerCore brokerCore;

    protected Client clientA;

    protected Client clientB;

    protected MessageWatchAppender exceptionAppender;

    protected MessageWatchAppender messageWatcher;

    protected PatternFilter msgPatternFilter;

    protected PatternFilter exceptionPatternFilter;
    private String commProtocol;

    @Before
    public void setUp() throws Exception {
        commProtocol = method;
        setupConfigurations(configuration, method);

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

    protected BrokerCore createNewBrokerCore(BrokerConfig brokerConfig) throws BrokerCoreException {
        return new TesterBrokerCore(_brokerTester, brokerConfig);
    }


    @After
    public void tearDown() throws Exception {

        LogSetup.removeAppender("Exception", exceptionAppender);
        LogSetup.removeAppender("MessagePath", messageWatcher);
        clientA.shutdown();

        if (clientB != null)
            clientB.shutdown();

        brokerCore.shutdown();
        _brokerTester = null;
    }

    /**
     * Test for exception that subscription without single quotes in string predicate.
     *
     * @throws ParseException
     */
    @Test
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

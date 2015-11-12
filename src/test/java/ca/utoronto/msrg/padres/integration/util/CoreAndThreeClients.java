package ca.utoronto.msrg.padres.integration.util;

import ca.utoronto.msrg.padres.AllTests;
import ca.utoronto.msrg.padres.MessageWatchAppender;
import ca.utoronto.msrg.padres.PatternFilter;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.util.LogSetup;

import static ca.utoronto.msrg.padres.AllTests.setupConfigurations;

/**
 * Created by chris on 12.11.15.
 */
public class CoreAndThreeClients {
    public BrokerCore brokerCore;

    public Client clientA;

    public Client clientB;

    public Client clientC;

    public MessageWatchAppender messageWatcher;

    public PatternFilter msgFilter;

    public CoreAndThreeClients(int config, String method) throws BrokerCoreException, ClientException {
        setupConfigurations(config, method);


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

    public void shutdown() throws ClientException {
        clientA.shutdown();
        clientB.shutdown();
        clientC.shutdown();
        brokerCore.shutdown();
        LogSetup.removeAppender("MessagePath", messageWatcher);
    }
}

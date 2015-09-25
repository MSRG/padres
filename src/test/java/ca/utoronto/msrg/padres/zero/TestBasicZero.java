package ca.utoronto.msrg.padres.zero;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by chris on 25.09.15.
 */
public class TestBasicZero extends TestCase  {

    public void testStartBrokerWithZero() throws BrokerCoreException {
        BrokerConfig cfg = new BrokerConfig();
        cfg.setBrokerURI("zero-tcp://127.0.0.1:5555");

        BrokerCore bc = new BrokerCore(cfg);
        bc.initialize();

    }
}

package de.tum.msrg.itt;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.common.comm.CommSystem;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by pxsalehi on 30.06.16.
 */
public class TestNodeURI extends Assert {
    @Test (expected = BrokerCoreException.class)
    public void testNodeURIParser() throws BrokerCoreException {
        String rmi = "rmi://localhost:1234/broker1";
        NodeURI uri = NodeURI.parse(rmi);
        assertNotNull(uri);
        assertEquals(uri.getType(), CommSystem.CommSystemType.RMI);
        assertEquals(uri.getHost(), "localhost");
        assertEquals(uri.getPort(), 1234);
        assertEquals(uri.getID(), "broker1");
        // socket
        String socket = "socket://125.24.147.33:8000/b100";
        uri = NodeURI.parse(socket);
        assertNotNull(uri);
        assertEquals(uri.getType(), CommSystem.CommSystemType.SOCKET);
        assertEquals(uri.getHost(), "125.24.147.33");
        assertEquals(uri.getPort(), 8000);
        assertEquals(uri.getID(), "b100");
        // socket
        String wrong = "125.24.147.33:8000/b100";
        uri = NodeURI.parse(wrong);
    }
}

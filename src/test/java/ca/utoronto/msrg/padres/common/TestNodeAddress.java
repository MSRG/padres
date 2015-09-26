package ca.utoronto.msrg.padres.common;

import org.junit.Test;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.ConnectionHelper;
import ca.utoronto.msrg.padres.common.comm.INodeAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.socket.SocketAddress;
import ca.utoronto.msrg.padres.common.comm.zero.ZeroAddress;
import org.junit.Assert;

import static ca.utoronto.msrg.padres.common.comm.ConnectionHelper.getLocalIPAddr;

/**
 * Created by chris on 26.09.15.
 */
public class TestNodeAddress extends Assert {

   @Test
    public void test_parsing_rmi_address() throws CommunicationException {
        String uri = "rmi://127.0.0.1:8080/rmi1";

        INodeAddress address = ConnectionHelper.getAddress(uri);

        assertEquals("rmi1", address.getNodeID());
        assertEquals(CommSystem.CommSystemType.RMI, address.getType());
        assertEquals(RMIAddress.class, address.getClass());
        assertEquals(8080, address.getPort());
    }

   @Test
    public void test_parsing_socket_address() throws CommunicationException {
        String uri = "socket://127.0.0.1:8080/sock1";

        INodeAddress address = ConnectionHelper.getAddress(uri);

        assertEquals("sock1", address.getNodeID());
        assertEquals(CommSystem.CommSystemType.SOCKET, address.getType());
        assertEquals(SocketAddress.class, address.getClass());
        assertEquals(8080, address.getPort());
    }

   @Test
    public void test_parsing_zero_address() throws CommunicationException {
        String uri = "zero-tcp://127.0.0.1:8080/zerotcp";

        INodeAddress address = ConnectionHelper.getAddress(uri);

        assertEquals("zerotcp", address.getNodeID());
        assertEquals(CommSystem.CommSystemType.ZERO, address.getType());
        assertEquals(ZeroAddress.class, address.getClass());
        assertEquals(8080, address.getPort());
    }
}

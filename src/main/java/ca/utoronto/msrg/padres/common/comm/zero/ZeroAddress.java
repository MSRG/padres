package ca.utoronto.msrg.padres.common.comm.zero;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chris on 25.09.15.
 */
public class ZeroAddress extends NodeAddress {
    private static final String ZERO_REG_EXP = "zero-tcp://([^:/]+)(:(\\d+))";;


    /**
     * Constructor to create the node address from the given protocol specific URI string
     *
     * @param nodeURI protocol-specific URI string
     * @throws CommunicationException when there is a problem parsing the URI
     */
    public ZeroAddress(String nodeURI) throws CommunicationException {
        super(nodeURI);
        type = CommSystem.CommSystemType.ZERO;
    }

    @Override
    protected void parseURI(String nodeURI) throws CommunicationException {
        //zero-tcp://*:5555

        host = "should not be used"; //actually we don't have that here
        port = 123456;
        remoteID = null;
        Matcher socketMatcher = Pattern.compile(ZERO_REG_EXP).matcher(nodeURI);
        if (socketMatcher.find()) {
            host = socketMatcher.group(1);
            if (socketMatcher.group(3) != null) {
                port = Integer.parseInt(socketMatcher.group(3));
            }
            remoteID = ""; //we don't have that
        } else {
            throw new CommunicationException("Malformed remote broker socket URI: " + nodeURI);
        }
    }

    @Override
    public boolean isEqual(String nodeURI) throws CommunicationException {
        try {
            ZeroAddress checkAddr = new ZeroAddress(nodeURI);
            InetAddress checkHost = InetAddress.getByName(checkAddr.host);
            nodeURI = String.format("socket://%s:%d/%s", checkHost.getHostAddress(),
                    checkAddr.port, checkAddr.remoteID);
            InetAddress thisHost = InetAddress.getByName(checkAddr.host);
            String thisURI = String.format("socket://%s:%d/%s", thisHost.getHostAddress(), port,
                    remoteID);
            return thisURI.equalsIgnoreCase(nodeURI);
        } catch (UnknownHostException e) {
            throw new CommunicationException(e);
        }
    }

    @Override
    public String toString() {
        return "ZeroAddress{}";
    }
}

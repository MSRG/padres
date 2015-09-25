package ca.utoronto.msrg.padres.common.comm.zero;

import ca.utoronto.msrg.padres.common.comm.CommSystem;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.INodeAddress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.utoronto.msrg.padres.common.comm.ConnectionHelper.getLocalIPAddr;

/**
 * Created by chris on 25.09.15.
 */
public class ZeroAddress implements INodeAddress {
    private static final String ZERO_REG_EXP = "zero-tcp://([^:/]+)(:(\\d+))?/(.+)";;
    private final CommSystem.CommSystemType type;
    private String host;
    private int port;
    private String remoteID;


    /**
     * Constructor to create the node address from the given protocol specific URI string
     *
     * @param nodeURI protocol-specific URI string
     * @throws CommunicationException when there is a problem parsing the URI
     */
    public ZeroAddress(String nodeURI) throws CommunicationException {
        type = CommSystem.CommSystemType.ZERO;
        parseURI(nodeURI);
    }

    void parseURI(String nodeURI) throws CommunicationException {
        Matcher socketMatcher = Pattern.compile(ZERO_REG_EXP).matcher(nodeURI);
        if (socketMatcher.find()) {
            host = getLocalIPAddr(socketMatcher.group(1));
            if (socketMatcher.group(3) != null) {
                port = Integer.parseInt(socketMatcher.group(3));
            }
            remoteID = socketMatcher.group(4); //we don't have that
        } else {
            throw new CommunicationException("Malformed ZeroAddress URI: " + nodeURI);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZeroAddress that = (ZeroAddress) o;

        if (port != that.port) return false;
        if (type != that.type) return false;
        if (!host.equals(that.host)) return false;
        return remoteID.equals(that.remoteID);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + remoteID.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("zero-tcp://%s:%d/%s", host, port, remoteID);
    }

    @Override
    public CommSystem.CommSystemType getType() {
        return null;
    }

    @Override
    public String getNodeURI() {
        return this.toString();
    }

    @Override
    public String getNodeID() {
        return this.remoteID;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getPort() {
        return this.port;
    }
}

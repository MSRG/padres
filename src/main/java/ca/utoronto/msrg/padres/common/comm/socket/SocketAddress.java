package ca.utoronto.msrg.padres.common.comm.socket;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.utoronto.msrg.padres.common.comm.ConnectionHelper.getLocalIPAddr;

public class SocketAddress implements NodeAddress {

	private static final String SOCKET_REG_EXP = "socket://([^:/]+)(:(\\d+))?/(.+)";
    private final CommSystemType type;
    private String host;
    private int port;
    private String remoteID;

    public SocketAddress(String nodeURI) throws CommunicationException {
		type = CommSystemType.SOCKET;
        parseURI(nodeURI);
	}

    void parseURI(String nodeURI) throws CommunicationException {
        // set the default values
        host = "localhost";
        port = 1099;
        remoteID = null;
        // get the actual values from the input string
        Matcher socketMatcher = Pattern.compile(SOCKET_REG_EXP).matcher(nodeURI);
        if (socketMatcher.find()) {
            host = getLocalIPAddr(socketMatcher.group(1));
            if (socketMatcher.group(3) != null) {
                port = Integer.parseInt(socketMatcher.group(3));
            }
            remoteID = socketMatcher.group(4);
        } else {
            throw new CommunicationException("Malformed remote broker socket URI: " + nodeURI);
        }
    }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SocketAddress))
			return false;
		SocketAddress tempAddr = (SocketAddress) o;
		if (tempAddr.host.equals(host) && tempAddr.port == port)
			return true;
		return false;
	}

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + remoteID.hashCode();
        return result;
    }

    public String toString() {

        return String.format("socket://%s:%d/%s", host, port, remoteID);
	}

    @Override
    public CommSystemType getType() {
        return this.type;
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

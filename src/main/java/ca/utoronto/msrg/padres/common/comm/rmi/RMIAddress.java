package ca.utoronto.msrg.padres.common.comm.rmi;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ca.utoronto.msrg.padres.common.comm.ConnectionHelper.getLocalIPAddr;

public class RMIAddress implements NodeAddress {

	public static final String RMI_REG_EXP = "rmi://([^:/]+)(:(\\d+))?/(.+)";
    private final CommSystemType type;
    private String host;
    private int port;
    private String remoteID;

    public RMIAddress(String nodeURI) throws CommunicationException { //TODO: Constructors should not throw exceptions
		type = CommSystemType.RMI;
        parseURI(nodeURI);
	}

	private void parseURI(String nodeURI) throws CommunicationException {
		// set the default values
		host = "localhost";
		port = 1099;
		remoteID = null;
		// get the actual values from the input string
		Matcher rmiMatcher = Pattern.compile(RMI_REG_EXP).matcher(nodeURI);
		if (rmiMatcher.find()) {
			host = getLocalIPAddr(rmiMatcher.group(1));
			if (rmiMatcher.group(3) != null) {
				port = Integer.parseInt(rmiMatcher.group(3));
			}
			remoteID = rmiMatcher.group(4);
		} else {
			throw new CommunicationException("Malformed remote broker URI: " + nodeURI);
		}
	}

    /*
	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getNodeID() {
		return remoteID;
	}*/

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + remoteID.hashCode();
        return result;
    }

    @Override
	public boolean equals(Object o) {
		if (!(o instanceof RMIAddress))
			return false;
		RMIAddress tempAddr = (RMIAddress) o;
		return (tempAddr.host.equals(host) && tempAddr.port == port && remoteID.equals(tempAddr.remoteID));
	}

	@Override
	public String toString() {
		return String.format("rmi://%s:%d/%s", host, port, remoteID);
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

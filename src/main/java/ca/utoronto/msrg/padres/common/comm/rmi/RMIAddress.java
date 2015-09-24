package ca.utoronto.msrg.padres.common.comm.rmi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

public class RMIAddress extends NodeAddress {

	public static final String RMI_REG_EXP = "rmi://([^:/]+)(:(\\d+))?/(.+)";

	public RMIAddress(String nodeURI) throws CommunicationException {
		super(nodeURI);
		type = CommSystemType.RMI;
	}

	@Override
	protected void parseURI(String nodeURI) throws CommunicationException {
		// set the default values
		host = "localhost";
		port = 1099;
		remoteID = null;
		// get the actual values from the input string
		Matcher rmiMatcher = getMatch(nodeURI);
		if (rmiMatcher.find()) {
			host = rmiMatcher.group(1);
			if (rmiMatcher.group(3) != null) {
				port = Integer.parseInt(rmiMatcher.group(3));
			}
			remoteID = rmiMatcher.group(4);
		} else {
			throw new CommunicationException("Malformed remote broker URI: " + nodeURI);
		}
	}

	@Override
	public boolean isEqual(String checkURI) throws CommunicationException {
		try {
			RMIAddress checkAddr = new RMIAddress(checkURI);
			InetAddress checkHost = InetAddress.getByName(checkAddr.host);
			checkURI = String.format("rmi://%s:%d/%s", checkHost.getHostAddress(), checkAddr.port,
					checkAddr.remoteID);
			InetAddress thisHost = InetAddress.getByName(checkAddr.host);
			String thisURI = String.format("rmi://%s:%d/%s", thisHost.getHostAddress(), port,
					remoteID);
			return thisURI.equalsIgnoreCase(checkURI);
		} catch (UnknownHostException e) {
			throw new CommunicationException(e);
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getNodeID() {
		return remoteID;
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

}

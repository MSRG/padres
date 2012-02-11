package ca.utoronto.msrg.padres.common.comm.socket;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;

import ca.utoronto.msrg.padres.common.comm.CommSystem.CommSystemType;
import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.NodeAddress;

public class SocketAddress extends NodeAddress {

	public static final String SOCKET_REG_EXP = "socket://([^:/]+)(:(\\d+))?/(.+)";

	public SocketAddress(String nodeURI) throws CommunicationException {
		super(nodeURI);
		type = CommSystemType.SOCKET;
	}

	@Override
	protected void parseURI(String nodeURI) throws CommunicationException {
		// set the default values
		host = "localhost";
		port = 1099;
		remoteID = null;
		// get the actual values from the input string
		Matcher socketMatcher = getMatch(nodeURI);
		if (socketMatcher.find()) {
			host = socketMatcher.group(1);
			if (socketMatcher.group(3) != null) {
				port = Integer.parseInt(socketMatcher.group(3));
			}
			remoteID = socketMatcher.group(4);
		} else {
			throw new CommunicationException("Malformed remote broker socket URI: " + nodeURI);
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public String getNodeID() {
		return remoteID;
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
	public boolean isEqual(String checkURI) throws CommunicationException {
		try {
			SocketAddress checkAddr = new SocketAddress(checkURI);
			InetAddress checkHost = InetAddress.getByName(checkAddr.host);
			checkURI = String.format("socket://%s:%d/%s", checkHost.getHostAddress(),
					checkAddr.port, checkAddr.remoteID);
			InetAddress thisHost = InetAddress.getByName(checkAddr.host);
			String thisURI = String.format("socket://%s:%d/%s", thisHost.getHostAddress(), port,
					remoteID);
			return thisURI.equalsIgnoreCase(checkURI);
		} catch (UnknownHostException e) {
			throw new CommunicationException(e);
		}
	}

	public String toString() {
		return String.format("socket://%s:%d/%s", host, port, remoteID);
	}

}

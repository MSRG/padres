package ca.utoronto.msrg.padres.common.comm.socket.message;

public class ConnectReplyMessage extends SocketMessage {

	private static final long serialVersionUID = 1L;

	protected String serverID;

	public ConnectReplyMessage(String serverID) {
		super(SocketMessageType.CONNECT_REPLY);
		this.serverID = serverID;
	}

	public String getServerID() {
		return serverID;
	}

	public String toString() {
		return String.format("[%s] %s", msgType.toString(), serverID);
	}

}

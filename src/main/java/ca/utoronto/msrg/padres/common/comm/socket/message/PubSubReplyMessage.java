package ca.utoronto.msrg.padres.common.comm.socket.message;

public class PubSubReplyMessage extends SocketMessage {

	private static final long serialVersionUID = 1L;

	protected String msgID;

	public PubSubReplyMessage(String msgID) {
		super(SocketMessageType.PUB_SUB_REPLY);
		this.msgID = msgID;
	}

	public String getMessageID() {
		return msgID;
	}

	public String toString() {
		return String.format("[%s] %s", msgType, msgID);
	}

}

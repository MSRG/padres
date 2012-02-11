package ca.utoronto.msrg.padres.common.comm.socket.message;

import java.io.Serializable;

public class SocketMessage implements Serializable {

	private static final long serialVersionUID = 5122229262760141130L;

	public enum SocketMessageType {
		CONNECT, CONNECT_REPLY, PUB_SUB, PUB_SUB_REPLY;
	}

	protected SocketMessageType msgType;

	public SocketMessage(SocketMessageType msgType) {
		this.msgType = msgType;
	}

	public SocketMessageType getMessageType() {
		return msgType;
	}
	
	public String toString() {
		return msgType.toString();
	}

}

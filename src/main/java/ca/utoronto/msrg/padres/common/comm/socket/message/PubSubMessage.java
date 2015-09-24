package ca.utoronto.msrg.padres.common.comm.socket.message;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.Message;

public class PubSubMessage extends SocketMessage {

	private static final long serialVersionUID = 1L;

	protected HostType sourceHostType;

	protected Message message;

	public PubSubMessage(Message message, HostType hostType) {
		super(SocketMessageType.PUB_SUB);
		this.sourceHostType = hostType;
		this.message = message;
	}

	public HostType getHostType() {
		return sourceHostType;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public String toString() {
		return String.format("[%s] from %s:  %s", msgType.toString(), sourceHostType.toString(),
				message);
	}

}

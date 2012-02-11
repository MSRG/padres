package ca.utoronto.msrg.padres.common.comm.socket.message;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.message.MessageDestination;

public class ConnectMessage extends SocketMessage {

	private static final long serialVersionUID = 1L;

	protected HostType sourceHostType;

	protected MessageDestination sourceDestination;

	public ConnectMessage(HostType sourceHostType, MessageDestination sourceDestination) {
		super(SocketMessageType.CONNECT);
		this.sourceHostType = sourceHostType;
		this.sourceDestination = sourceDestination;
	}

	public HostType getSourceType() {
		return sourceHostType;
	}

	public MessageDestination getSourceDestination() {
		return sourceDestination;
	}

	public String toString() {
		return String.format("[%s] %s (%s)", msgType.toString(), sourceDestination.toString(),
				sourceHostType.toString());
	}

}

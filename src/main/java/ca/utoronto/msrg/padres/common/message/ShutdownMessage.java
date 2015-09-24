package ca.utoronto.msrg.padres.common.message;

public class ShutdownMessage extends Message {

	private static final long serialVersionUID = 2596881313861408487L;

	public ShutdownMessage(){
		super(MessageType.SHUTDOWN);
	}
	
	@Override
	public Message duplicate() {
		return new ShutdownMessage();
	}

}

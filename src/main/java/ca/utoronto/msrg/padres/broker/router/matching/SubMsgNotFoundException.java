package ca.utoronto.msrg.padres.broker.router.matching;

public class SubMsgNotFoundException extends MessageNotFoundException {

	private static final long serialVersionUID = 4474411654736461814L;

	public SubMsgNotFoundException() {
		super();
	}

	public SubMsgNotFoundException(String msg) {
		super(msg);
	}
}

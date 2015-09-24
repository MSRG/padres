package ca.utoronto.msrg.padres.broker.router.matching;

public class MessageNotFoundException extends Exception {

	private static final long serialVersionUID = -1896988477733229145L;

	public MessageNotFoundException() {
		super();
	}

	public MessageNotFoundException(String msg) {
		super(msg);
	}

}

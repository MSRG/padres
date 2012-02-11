package ca.utoronto.msrg.padres.broker.router.matching;

public class PubMsgNotConformedException extends Exception {

	private static final long serialVersionUID = 5415734136405923701L;

	public PubMsgNotConformedException() {
		super();
	}

	public PubMsgNotConformedException(String msg) {
		super(msg);
	}

}

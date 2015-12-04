package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import ca.utoronto.msrg.padres.broker.router.matching.MatcherException;

public class ReteException extends MatcherException {

	private static final long serialVersionUID = 1L;

	public ReteException() {
		super();
	}

	public ReteException(String arg0) {
		super("Rete: " + arg0);
	}

	public ReteException(Throwable arg0) {
		super("Rete: " + arg0);
	}

	public ReteException(String arg0, Throwable arg1) {
		super("Rete: " + arg0, arg1);
	}

}

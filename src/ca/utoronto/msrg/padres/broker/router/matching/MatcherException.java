package ca.utoronto.msrg.padres.broker.router.matching;

public class MatcherException extends Exception {

	private static final long serialVersionUID = 1L;

	public MatcherException() {
	}

	public MatcherException(String message) {
		super(message);
	}

	public MatcherException(Throwable cause) {
		super(cause);
	}

	public MatcherException(String message, Throwable cause) {
		super(message, cause);
	}

}

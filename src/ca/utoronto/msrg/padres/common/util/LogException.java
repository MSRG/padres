package ca.utoronto.msrg.padres.common.util;

public class LogException extends Exception {

	private static final long serialVersionUID = 1L;

	public LogException() {
	}

	public LogException(String message) {
		super(message);
	}

	public LogException(Throwable cause) {
		super(cause);
	}

	public LogException(String message, Throwable cause) {
		super(message, cause);
	}

}

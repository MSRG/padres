package ca.utoronto.msrg.padres.client;

public class ClientException extends Exception {

	private static final long serialVersionUID = -2870589682660763961L;

	public ClientException() {
	}

	public ClientException(String message) {
		super(message);
	}

	public ClientException(Throwable cause) {
		super(cause);
	}

	public ClientException(String message, Throwable cause) {
		super(message, cause);
	}

}

package ca.utoronto.msrg.padres.demo.webclient.client;

/*
 * Exceptions of this type (and only this type) are returned as a friendly
 * "info" dialog box. Other exceptions will result in "Internal Server Error"
 * error dialogs.
 */
public class WebClientException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8992644978925696214L;

	/**
	 * 
	 * @param string The friendly error message to be popped up to the user
	 */
	public WebClientException(String string) {
		super(string);
	}

}

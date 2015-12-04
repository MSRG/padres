package ca.utoronto.msrg.padres.common.comm;

/**
 * @author Bala Maniymaran
 * 
 *         Created: 2010-08-11
 * 
 *         Exception class for the communication layer
 * 
 */
public class CommunicationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3373936066940861009L;

	public CommunicationException() {
	}

	public CommunicationException(String arg0) {
		super(arg0);
	}

	public CommunicationException(Throwable arg0) {
		super(arg0);
	}

	public CommunicationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}

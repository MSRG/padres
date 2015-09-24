/*
 * Created on May 6, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.util.datastructure;

/**
 * @author cheung
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ParseException extends RuntimeException {

	private static final long serialVersionUID = 14396549331670671L;

	/**
	 * Constructor
	 */
	public ParseException() {
		super();
	}

	/**
	 * @param s
	 *            the detail message.
	 */
	public ParseException(String s) {
		super(s);
	}
}

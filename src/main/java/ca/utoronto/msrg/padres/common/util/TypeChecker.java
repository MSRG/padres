/*
 * Created on Dec 22, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.util;

/**
 * @author Mr. Biggy
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */


public class TypeChecker {

	/*
	 *Just to check that an input parameter is well-formed
	 */
	public static boolean isString(String input)
	{
		if (input != null)
			if (input != "")
				if (input.length() > 0)
					return true;
		return false;
	}
	
		
	/*
	 * Check to see if an input parameter is really a valid number.  Decimals are ok.
	 */
	public static boolean isNumeric(String input)
	{
		try {
			Double.parseDouble(input);
		} catch (Exception goodone) {
			return false;
		}
		return true;
	}
}

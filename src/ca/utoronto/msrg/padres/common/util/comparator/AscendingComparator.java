/*
 * Created on May 7, 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.util.comparator;

/**
 * @author Alex
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.Comparator;

import ca.utoronto.msrg.padres.common.util.TypeChecker;

public class AscendingComparator<T> implements Comparator<T> {
	public int compare(Object a, Object b) {
		return staticCompare(a, b);
	}

	public static int staticCompare(Object a, Object b) {
		if (TypeChecker.isNumeric(a.toString()) && TypeChecker.isNumeric(b.toString())) {
			double da = Double.parseDouble(a.toString());
			double db = Double.parseDouble(b.toString());

			if (da == db)
				return 0;
			else
				return (da > db) ? 1 : -1;

		} else {
			// assume both are strings
			return a.toString().compareTo(b.toString());
		}
	}
}

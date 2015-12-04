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

public class DescendingComparator<T> implements Comparator<T> {
	public int compare(Object a, Object b) {
		return -1 * AscendingComparator.staticCompare(a, b);
	}
}

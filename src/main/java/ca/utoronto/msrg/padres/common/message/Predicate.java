// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on Jul 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author efidler
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public class Predicate implements Serializable {
	public static final long serialVersionUID = 1;

	// Subscriptions consist of HashMaps of Predictes, so that attributes are
	// the hash keys
	private String op;

	private Serializable value;

	// Each value type is allowed certain operators. These are chosen to match
	// Jess function calls

	// Numbers (Integers, Shorts, Longs, Floats, or Doubles)
	// Allowed: less than ("<"), less than or equal to ("<="), equal to ("="),
	// greater than or equal to (">="),
	// greater than (">"), not equal to ("<>")
	static private String[] validNumberOperatorsArray = new String[] { "<", "<=", "=", ">=", ">",
			"<>", "isPresent" };

	static private List<String> validNumberOperators = Arrays.asList(validNumberOperatorsArray);

	// Strings (all comparisons are lexographic)
	// Allowed: less than ("str-lt"), less than or equal to ("str-le"), equal to
	// ("eq"), greater than or equal to ("str-ge"),
	// greater than ("str-gt"), not equal to ("neq"), contains ("str-contains"),
	// is within ("str-index")
	static private String[] validStringOperatorsArray = new String[] { "str-lt", "str-le", "eq",
			"str-ge", "str-gt", "neq", "str-contains", "str-prefix", "str-postfix", "isPresent" };

	static private List<String> validStringOperators = Arrays.asList(validStringOperatorsArray);

	static private String[] validDateOperatorsArray = new String[] { "before", "after" };

	static private List<String> validDateOperators = Arrays.asList(validDateOperatorsArray);

	/**
	 * Default constructor
	 */
	public Predicate() {
	}

	/**
	 * Constructor with initializers
	 * 
	 * @param o
	 *            refer to valid operators above to see what is allowed
	 * @param val
	 */
	public Predicate(String o, Serializable val) {
		op = o;
		value = val;
	}

	/**
	 * 
	 */
	public String toString() {
		return op + " " + value.toString();
	}

	/**
	 * @return
	 */
	public String getOp() {
		return op;
	}

	/**
	 * @return
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param object
	 */
	public void setValue(Serializable object) {
		value = object;
	}

	/**
	 * @param string
	 */
	public void setOp(String string) {
		op = string;
	}

	/**
	 * 
	 * @return true if operator is allowed for that value, false otherwise
	 */
	public boolean isValid() {
		Class<? extends Serializable> valueClass = value.getClass();
		if (valueClass == Integer.class || valueClass == Double.class || valueClass == Float.class
				|| valueClass == Long.class || valueClass == Short.class) {
			return validNumberOperators.contains(op);
		} else if (valueClass == String.class) {
			return validStringOperators.contains(op);
		} else if (valueClass.equals(Date.class)) {
			return validDateOperators.contains(op);
		}
		return false;
	}

	public Predicate duplicate() {
		Predicate newPredicate = new Predicate();
		newPredicate.setOp(this.getOp());
		newPredicate.setValue((Serializable) this.getValue());
		return newPredicate;
	}

	public static boolean isNumericOperator(String op) {
		return validNumberOperators.contains(op);
	}

	public static boolean isStringOperator(String op) {
		return validStringOperators.contains(op);
	}

	/**
	 * <code>
	public boolean equals(Map m1, Map m2) {
		if (m1.size() != m2.size()) {
			return false;
		} else {
			for (Iterator i = (m1.keySet()).iterator(); i.hasNext();) {
				String attribute = (String) i.next();
				if (!m2.containsKey(attribute)) {
					return false;
				} else {
					if (m1.get(attribute).getClass().equals(Predicate.class)) {
						// deal with predicateMap for adv and sub
						Predicate p1 = (Predicate) m1.get(attribute);
						Predicate p2 = (Predicate) m2.get(attribute);
						if (!equals(p1, p2)) {
							return false;
						}
					} else {
						// deal with pairMap for pub
						Object v1 = (Object) m1.get(attribute);
						Object v2 = (Object) m2.get(attribute);
						if (!v1.equals(v2)) {
							return false;
						}
					}
				}
			}
			return true;
		}
	}
	 * </code>
	 */

	/**
	 * @param p1
	 *            p2
	 * @return true if their op and value are equal seperately false if not
	 */

	public boolean equals(Predicate p1, Predicate p2) {
		if (p1.getOp().equals(p2.getOp()) && (p1.getValue().equals(p2.getValue()))) {
			return true;
		} else {
			return false;
		}
	}
}

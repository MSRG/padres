/*
 * Created on Mar 7, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scout;

/**
 * @author alex
 * getNumericRelation() and getStringRelation() were originally authored by Shuang
 *
 * Hint: "Sub" is an abbreviation for "Subscription" for all variable names
 */
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.util.TypeChecker;

public class RelationIdentifier {

	/**
	 * 
	 * 
	 * Assumptions: 1. Two subscriptions with the same attribute name must have
	 * the same data type
	 * 
	 * @param ourSub
	 * @param theirSub
	 * @return
	 */
	public static Relation getRelation(Map<String, Predicate> ourPredMap,
			Map<String, Predicate> theirPredMap) {
		Relation relation = Relation.EQUAL; // assume equal first

		// Walkthrough each and every single attribute from ourSub that is also found
		// in otherSub and test for their relationship
		for (String ourAttrName : ourPredMap.keySet()) {
			// Test for relation if both subscriptions have the same attribute name
			// This can be EQUAL, SUBSET, SUPERSET, INTERSECT, or EMPTY (all possibility)
			if (theirPredMap.containsKey(ourAttrName)) {
				Predicate ourPredicate = ourPredMap.get(ourAttrName);
				Predicate theirPredicate = theirPredMap.get(ourAttrName);

				relation = relation.AND(getRelation(ourPredicate, theirPredicate));
			}
		}

		// Now that we have checked all of our attributes with theirs, now see if
		// they have more attributes that we don't have.
		if (ourPredMap.keySet().containsAll(theirPredMap.keySet())
				&& theirPredMap.keySet().containsAll(ourPredMap.keySet())) {
			// no need to do anything, we have tested all of their attributes too
			// We have more attributes than them
		} else if (ourPredMap.keySet().containsAll(theirPredMap.keySet())
				&& !theirPredMap.keySet().containsAll(ourPredMap.keySet())) {
			relation = relation.AND(Relation.SUBSET);
			// Both subscriptions have attributes not found in the other
			// subscription
		} else if (!ourPredMap.keySet().containsAll(theirPredMap.keySet())
				&& !theirPredMap.keySet().containsAll(ourPredMap.keySet())) {
			relation = relation.AND(Relation.INTERSECT);
		} else {
			relation = relation.AND(Relation.SUPERSET);
		}

		return relation;
	}

	/*
	 * Get the relation of ourPredicate compared to theirPredicate Order of
	 * predicates is important!
	 */
	private static Relation getRelation(Predicate ourPredicate, Predicate theirPredicate) {
		// First find out whether it is a String or numbers by looking at the
		// operator type. However, if the operator is "isPresent", we must look
		// to see
		// if the value is numeric. If so, treat the predicate as numeric.
		// Otherwise,
		// a string
		String operator = ourPredicate.getOp();
		if (operator.equalsIgnoreCase("isPresent")) {
			if (TypeChecker.isNumeric(ourPredicate.getValue().toString())) {
				return getNumericRelation(ourPredicate, theirPredicate);
			} else {
				return getStringRelation(ourPredicate, theirPredicate);
			}
		} else if (Predicate.isNumericOperator(operator)) {
			return getNumericRelation(ourPredicate, theirPredicate);

		} else if (Predicate.isStringOperator(operator)) {
			return getStringRelation(ourPredicate, theirPredicate);

			// Unknown operator, don't know how to compare
		} else {
			System.err.println("RelationIdentifier: Invalid operator in "
					+ "subscription detected: " + operator);
			return Relation.EMPTY; // arbitrary answer, we were warned!
		}
	}

	/*
	 * Tests the relation of a given predicate (ourPred) with another predicate
	 * (theirPred)
	 */
	private static Relation getNumericRelation(Predicate ourPred, Predicate theirPred) {
		String op1 = ourPred.getOp();
		String op2 = theirPred.getOp();
		double v1 = Double.parseDouble(ourPred.getValue().toString());
		double v2 = Double.parseDouble(theirPred.getValue().toString());

		// Rule 1 : if op1=op1,v1=v2 then f1=f2
		if (op1.equals(op2) && (v1 == v2)) {
			return Relation.EQUAL;
		}

		// Rule 5 : if op1="isPresent" then f1 cover f2
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			if (op1.equals("isPresent") && op2.equals("isPresent")) {
				return Relation.EQUAL;
			} else if (op1.equals("isPresent")) {
				return Relation.SUPERSET;
			} else if (op2.equals("isPresent")) {
				return Relation.SUBSET;
			}
		}

		// Rule 2 : if op1='=', op2(v1,v2) then f1 is covered by f2
		if (op1.equals("=") || op2.equals("=")) {
			if (op1.equals("=") && op2.equals("=")) {
				if (v1 == v2) {
					return Relation.EQUAL;
				} else {
					return Relation.EMPTY;
				}
			} else if (op1.equals("=")) {
				if (operation(op2, v1, v2)) {
					return Relation.SUBSET;
				} else {
					return Relation.EMPTY;
				}
			} else if (op2.equals("=")) {
				if (operation(op1, v2, v1)) {
					return Relation.SUPERSET;
				} else {
					return Relation.EMPTY;
				}
			}
		}

		// Rule 3 : if op1='<>' , op2(v1,v2) then f1 covers f2
		// op1 and op2 cannot have "=" or "isPresent" at this point
		if ((op1.equals("<>")) || (op2.equals("<>"))) {
			if (op1.equals("<>") && op2.equals("<>")) {
				if (v1 == v2) {
					return Relation.EQUAL;
				} else {
					return Relation.INTERSECT;
				}
			} else if (op1.equals("<>")) {
				if (operation(op2, v1, v2)) {
					return Relation.INTERSECT;
				} else {
					return Relation.SUPERSET;
				}
			} else if (op2.equals("<>")) {
				if (operation(op1, v2, v1)) {
					return Relation.INTERSECT;
				} else {
					return Relation.SUBSET;
				}
			}
		}

		// Replace the inequality operators with no equal signs
		if (op1.equals("<")) {
			op1 = "<=";
			v1--;
		} else if (op1.equals(">")) {
			op1 = ">=";
			v1++;
		}

		if (op2.equals("<")) {
			op2 = "<=";
			v2--;
		} else if (op2.equals(">")) {
			op2 = ">=";
			v2++;
		}

		// By now, all operators are only inequalities (>= and <=)
		// Rule 4 :
		if ((op1.equals("<=") || op1.equals(">=")) && (op2.equals("<=") || op2.equals(">="))) {
			// For the case with (>,>) and (<,<)
			if (op1.equals(op2)) {
				if (v1 == v2) {
					return Relation.EQUAL;
				}
				if (operation(op2, v1, v2)) {
					return Relation.SUBSET;
				} else {
					return Relation.SUPERSET;
				}
				// For the case with (>,<) or (<,>)
			} else {
				if (operation(op2, v1, v2)) {
					return Relation.INTERSECT;
				} else {
					return Relation.EMPTY;
				}
			}
		}

		System.err.println("RelationIdentifier: Function getNumericRelation() did not handle all cases!");
		System.err.println("(" + op1 + "," + v1 + ") and (" + op2 + "," + v2 + ")");
		return Relation.EMPTY; // this is wrong, but oh well, we were warned
		// above
	}

	/*
	 * Evaluates the expression "v1 op v2" (ie. 3 < 1)
	 */
	private static boolean operation(String op, double v1, double v2) {
		if (op.equals("=") && (v1 == v2)) {
			return true;
		}
		if (op.equals("<=") && (v1 <= v2)) {
			return true;
		}
		if (op.equals("<") && (v1 < v2)) {
			return true;
		}
		if (op.equals(">=") && (v1 >= v2)) {
			return true;
		}
		if (op.equals(">") && (v1 > v2)) {
			return true;
		}
		if (op.equals("<>") && (v1 != v2)) {
			return true;
		}

		return false;
	}

	/*
	 * Gets the relationship between 2 string predicates
	 */
	private static Relation getStringRelation(Predicate ourPred, Predicate theirPred) {
		String op1 = ourPred.getOp();
		String op2 = theirPred.getOp();
		String s1 = ourPred.getValue().toString();
		String s2 = theirPred.getValue().toString();

		// When everything is equal
		if (op1.equals(op2) && s1.equals(s2)) {
			return Relation.EQUAL;
		}

		// Solve all combinations with isPresent in either predicate
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			if (op1.equals("isPresent") && op2.equals("isPresent")) {
				return Relation.EQUAL;
			} else if (op1.equals("isPresent")) {
				return Relation.SUPERSET;
			} else if (op2.equals("isPresent")) {
				return Relation.SUBSET;
			}
		}

		// Solve all combinations with "eq" in either predicate
		if (op1.equals("eq") || op2.equals("eq")) {
			if (op1.equals("eq")) {
				if (op2.equals("eq")) {
					if (s1.equals(s2)) {
						return Relation.EQUAL;
					} else {
						return Relation.EMPTY;
					}
				} else if (op2.equals("neq")) {
					if (s1.equals(s2)) {
						return Relation.EMPTY;
					} else {
						return Relation.SUBSET;
					}
				} else if (op2.equals("str-contains")) {
					if (s1.indexOf(s2) > -1) {
						return Relation.SUBSET;
					} else {
						return Relation.EMPTY;
					}
				} else if (op2.equals("str-prefix")) {
					if (s1.startsWith(s2)) {
						return Relation.SUBSET;
					} else {
						return Relation.EMPTY;
					}
				} else if (op2.equals("str-postfix")) {
					if (s1.endsWith(s2)) {
						return Relation.SUBSET;
					} else {
						return Relation.EMPTY;
					}
				}
			}

			if (op2.equals("eq")) {
				if (op1.equals("neq")) {
					if (s1.equals(s2)) {
						return Relation.EMPTY;
					} else {
						return Relation.SUPERSET;
					}
				} else if (op1.equals("str-contains")) {
					if (s2.indexOf(s1) > -1) {
						return Relation.SUPERSET;
					} else {
						return Relation.EMPTY;
					}
				} else if (op1.equals("str-prefix")) {
					if (s2.startsWith(s1)) {
						return Relation.SUPERSET;
					} else {
						return Relation.EMPTY;
					}
				} else if (op1.equals("str-postfix")) {
					if (s2.endsWith(s1)) {
						return Relation.SUPERSET;
					} else {
						return Relation.EMPTY;
					}
				}
			}
		}

		// Rule 6: if op1=op2="str-prefix", and s1 is the prefix of s2 then f1
		// covers f2
		// and s2 is the prefix of s1 then f2 covers f1
		if (op1.equals("neq") || op2.equals("neq")) {
			if (op1.equals("neq")) {
				if (op2.equals("neq")) {
					if (s1.equals(s2)) {
						return Relation.EQUAL;
					} else {
						return Relation.INTERSECT;
					}
				} else if (op2.equals("str-contains") || op2.equals("str-prefix")
						|| op2.equals("str-postfix")) {
					if (s1.indexOf(s2) > -1) {
						return Relation.INTERSECT;
					} else if (s2.indexOf(s1) > -1) {
						return Relation.EMPTY;
					} else if (!s1.equals(s2)) {
						return Relation.INTERSECT;
					}
				}
			} else if (op2.equals("neq")) {
				if (op1.equals("str-contains") || op1.equals("str-prefix")
						|| op1.equals("str-postfix")) {
					if (s2.indexOf(s1) > -1) {
						return Relation.INTERSECT;
					} else if (s1.indexOf(s2) > -1) {
						return Relation.EMPTY;
					} else if (!s1.equals(s2)) {
						return Relation.INTERSECT;
					}
				}
			}
		}

		// Left with str-contains, str-prefix, and str-postfix combos
		if (op1.equals("str-contains") || op2.equals("str-contains")) {
			if (op1.equals("str-contains")) {
				if (op2.equals("str-contains")) {
					if (s1.equals(s2)) {
						return Relation.EQUAL;
					} else if (s1.indexOf(s2) > -1) {
						return Relation.SUBSET;
					} else if (s2.indexOf(s1) > -1) {
						return Relation.SUPERSET;
					} else if (!s1.equals(s2)) {
						return Relation.INTERSECT;
					}
				} else if (op2.equals("str-prefix") || op2.equals("str-postfix")) {
					if (s1.indexOf(s2) > -1) {
						return Relation.INTERSECT;
					} else if (s2.indexOf(s1) > -1) {
						return Relation.SUPERSET;
					} else if (!s1.equals(s2)) {
						return Relation.INTERSECT;
					}
				}
				// This is the reverse of the above conditions
			} else if (op2.equals("str-contains")) {
				if (op1.equals("str-contains")) {
					if (s2.indexOf(s1) > -1) {
						return Relation.SUPERSET;
					} else if (s1.indexOf(s2) > -1) {
						return Relation.SUBSET;
					} else if (!s2.equals(s1)) {
						return Relation.INTERSECT;
					}
				} else if (op1.equals("str-prefix") || op1.equals("str-postfix")) {
					if (s2.indexOf(s1) > -1) {
						return Relation.INTERSECT;
					} else if (s1.indexOf(s2) > -1) {
						return Relation.SUBSET;
					} else if (!s2.equals(s1)) {
						return Relation.INTERSECT;
					}
				}
			}
		}

		// Now we just have to check all combos of prefix and suffix
		if (op1.equals("str-prefix") || op2.equals("str-prefix")) {
			if (op1.equals("str-prefix")) {
				if (op2.equals("str-prefix")) {
					if (s1.equals(s2)) {
						return Relation.EQUAL;
					} else if (s2.startsWith(s1)) {
						return Relation.SUPERSET;
					} else if (s1.startsWith(s2)) {
						return Relation.SUBSET;
					} else if (!s1.equals(s2)) {
						return Relation.EMPTY;
					}
				} else if (op2.equals("str-postfix")) {
					return Relation.INTERSECT;
				}
			} else if (op2.equals("str-prefix")) {
				if (op1.equals("str-postfix")) {
					return Relation.INTERSECT;
				}
			}
		}

		if (op1.equals("str-postfix") && op2.equals("str-postfix")) {
			if (s1.equals(s2)) {
				return Relation.EQUAL;
			} else if (s2.endsWith(s1)) {
				return Relation.SUPERSET;
			} else if (s1.endsWith(s2)) {
				return Relation.SUBSET;
			} else if (!s1.equals(s2)) {
				return Relation.EMPTY;
			}
		}

		System.err.println("RelationIdentifier: Function getStringRelation() did not handle all cases!");
		System.err.println("(" + op1 + "," + s1 + ") and (" + op2 + "," + s2 + ")");
		return Relation.EMPTY; // this is wrong, but oh well, we were warned
		// above
	}

	public static boolean isCovered(Map<String, Predicate> testPredMap,
			Set<Map<String, Predicate>> predicateSet) {
		Relation relation = Relation.EQUAL;
		for (Map<String, Predicate> basePredMap : predicateSet) {
			relation = getRelation(testPredMap, basePredMap);
			if (relation == Relation.EQUAL || relation == Relation.SUBSET)
				return true;
		}

		return false;
	}
}

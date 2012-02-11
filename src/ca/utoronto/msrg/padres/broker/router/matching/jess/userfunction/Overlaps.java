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
 * Created on 14-Aug-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.matching.jess.userfunction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.router.matching.rete.ReteMatcher;

import jess.Context;
import jess.JessException;
import jess.RU;
import jess.Userfunction;
import jess.Value;
import jess.ValueVector;

/**
 * @author strangelove
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class Overlaps implements Userfunction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see jess.Userfunction#getName()
	 */
	static Logger reteMatcherLogger = Logger.getLogger(ReteMatcher.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public String getName() {
		return "overlaps";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jess.Userfunction#call(jess.ValueVector, jess.Context)
	 */
	public Value call(ValueVector functionArguments, Context context) throws JessException {
		// TODO Auto-generated method stub
		if (functionArguments.size() != 5) {
			if (reteMatcherLogger.isDebugEnabled())
				reteMatcherLogger.debug("Function overlaps takes four arguments: "
						+ functionArguments + ".  That's " + functionArguments.size()
						+ " arguments.");
		} else {

			Value op1Value = functionArguments.get(1).resolveValue(context);
			Value val1Value = functionArguments.get(2).resolveValue(context);
			Value op2Value = functionArguments.get(3).resolveValue(context);
			Value val2Value = functionArguments.get(4).resolveValue(context);

			int val1Type = val1Value.type();
			int val2Type = val2Value.type();

			// This is just a check to see if we're dealing with a float value. If so, make sure
			// that all numbers are treated as floats. If you want two sets of different types to
			// always return FALSE from overlaps, just take out these lines
			if ((val1Type == RU.INTEGER && val2Type == RU.FLOAT)
					|| (val1Type == RU.FLOAT && val2Type == RU.INTEGER)) {
				val1Type = RU.FLOAT;
				val2Type = RU.FLOAT;
			}

			// The sets cannot overlap if their types are different
			if (val1Type != val2Type) {
				return new Value(false);
			} else {
				String op1 = op1Value.toString().replaceAll("\"", "");
				String op2 = op2Value.toString().replaceAll("\"", "");

				switch (val1Type) {
				// Note that, for integers, the allowed operators are <, <=, =, >=, >, and isPresent
				case RU.INTEGER:
					int val1int = val1Value.intValue(context);
					int val2int = val2Value.intValue(context);
					return integerOverlaps(op1, val1int, op2, val2int);
				case RU.FLOAT:
					double val1double = val1Value.floatValue(context);
					double val2double = val2Value.floatValue(context);
					return doubleOverlaps(op1, val1double, op2, val2double);
				case RU.STRING:
					String val1String = val1Value.stringValue(context);
					String val2String = val2Value.stringValue(context);
					return stringOverlaps(op1, val1String, op2, val2String);
				case RU.ATOM:
					String val1atom = val1Value.stringValue(context);
					String val2atom = val2Value.stringValue(context);

					if (!isAJessBoolean(val1atom) || !isAJessBoolean(val1atom)) {
						return new Value(false);
					} else {
						return booleanOverlaps(op1, val1atom, op2, val2atom);
					}
					// break;
				}
			}
		}

		return new Value(false);
	}

	private Value integerOverlaps(String op1, int val1, String op2, int val2) {
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			return new Value(true);
		} else if (op1.equals(op2)) {
			if (op1.equals("<>")) {
				return new Value(true);
				// If both operators are "=", then the sets only overlap if the values are the same
			} else if (op1.equals("=")) {
				return new Value((val1 == val2));
				// If both operators are "<" (or "<="), or ">" (or ">="), then the sets overlap
				// regardless of value
			} else if (op1.equals("<") || op1.equals("<=") || op1.equals(">") || op1.equals(">=")) {
				return new Value(true);
			} else {
				reteMatcherLogger.warn("Invalid operator for integer.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for integer: " + op1));
			}
		} else {
			if (op1.equals("<")) {
				if (op2.equals("<=") || op2.equals("<>")) {
					return new Value(true);
				} else if (op2.equals("=") || op2.equals(">") || op2.equals(">=")) {
					return new Value((val2 < val1));
				} else {
					reteMatcherLogger.warn("Invalid operator for integer.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for integer: " + op2));
				}
			} else if (op1.equals("<=")) {
				if (op2.equals("<") || op2.equals("<>")) {
					return new Value(true);
				} else if (op2.equals("=") || op2.equals(">=")) {
					return new Value((val2 <= val1));
				} else if (op2.equals(">")) {
					return new Value((val2 < val1));
				} else {
					reteMatcherLogger.warn("Invalid operator for integer.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for integer: " + op2));
				}
			} else if (op1.equals("=")) {
				if (op2.equals("<")) {
					return new Value((val1 < val2));
				} else if (op2.equals("<=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals(">")) {
					return new Value((val1 > val2));
				} else if (op2.equals(">=")) {
					return new Value((val1 >= val2));
				} else if (op2.equals("<>")) {
					return new Value((val1 != val2));
				} else {
					reteMatcherLogger.warn("Invalid operator for integer.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for integer: " + op2));
				}
			} else if (op1.equals(">=")) {
				// change by shuang, for ">=" and "=" comparision
				if (op2.equals("<")) {
					return new Value((val1 < val2));
				} else if (op2.equals("=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals("<=") || op2.equals("=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals(">") || op2.equals("<>")) {
					return new Value((true));
				} else {
					reteMatcherLogger.warn("Invalid operator for integer.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for integer: " + op2));
				}
			} else if (op1.equals(">")) {
				if (op2.equals("<") || op2.equals("<=") || op2.equals("=")) {
					return new Value((val1 < val2));
				} else if (op2.equals(">=") || op2.equals("<>")) {
					return new Value(true);
				} else {
					reteMatcherLogger.warn("Invalid operator for integer.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for integer: " + op2));
				}
			} else if (op1.equals("<>")) {
				if (op2.equals("=")) {
					return new Value(!(val1 == val2));
				} else {
					return new Value(true);
				}
			} else {
				reteMatcherLogger.warn("Invalid operator for integer.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for integer: " + op1));
			}
		}

		return new Value(false);
	}

	private Value doubleOverlaps(String op1, double val1, String op2, double val2) {

		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			return new Value(true);
		} else if (op1.equals(op2)) {
			// If both operators are "=", then the sets only overlap if the values are the same
			if (op1.equals("<>")) {
				return new Value(true);
			} else if (op1.equals("=")) {
				return new Value((val1 == val2));
				// If both operators are "<" (or "<="), or ">" (or ">="), then the sets overlap
				// regardless of value
			} else if (op1.equals("<") || op1.equals("<=") || op1.equals(">") || op1.equals(">=")) {
				return new Value(true);
			} else {
				reteMatcherLogger.warn("Invalid operator for double.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for double: " + op2));
			}
		} else {
			if (op1.equals("<")) {
				if (op2.equals("<=") || op2.equals("<>")) {
					return new Value(true);
				} else if (op2.equals("=") || op2.equals(">") || op2.equals(">=")) {
					return new Value((val2 < val1));
				} else {
					reteMatcherLogger.warn("Invalid operator for double.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for double: " + op2));
				}
			} else if (op1.equals("<=")) {
				if (op2.equals("<") || op2.equals("<>")) {
					return new Value(true);
				} else if (op2.equals("=") || op2.equals(">=")) {
					return new Value((val2 <= val1));
				} else if (op2.equals(">")) {
					return new Value((val2 < val1));
				} else {
					reteMatcherLogger.warn("Invalid operator for double.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for double: " + op2));
				}
			} else if (op1.equals("=")) {
				if (op2.equals("<")) {
					return new Value((val1 < val2));
				} else if (op2.equals("<=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals(">")) {
					return new Value((val1 > val2));
				} else if (op2.equals(">=")) {
					return new Value((val1 >= val2));
				} else if (op2.equals("<>")) {
					return new Value(!(val1 == val2));
				} else {
					reteMatcherLogger.warn("Invalid operator for double.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for double: " + op2));
				}
			} else if (op1.equals(">=")) {
				// change by shuang, for ">=" and "=" comparision
				if (op2.equals("<")) {
					return new Value((val1 < val2));
				} else if (op2.equals("=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals("<=") || op2.equals("=")) {
					return new Value((val1 <= val2));
				} else if (op2.equals(">") || op2.equals("<>")) {
					return new Value((true));
				} else {
					reteMatcherLogger.warn("Invalid operator for double.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for double: " + op2));
				}
			} else if (op1.equals(">")) {
				if (op2.equals("<") || op2.equals("<=") || op2.equals("=")) {
					return new Value((val1 < val2));
				} else if (op2.equals(">=") || op2.equals("<>")) {
					return new Value(true);
				} else {
					reteMatcherLogger.warn("Invalid operator for double.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for double: " + op2));
				}
			} else if (op1.equals("<>")) {
				if (op2.equals("=")) {
					return new Value(!(val1 == val2));
				} else {
					return new Value(true);
				}
			} else {
				reteMatcherLogger.warn("Invalid operator for double.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for double: " + op1));
			}
		}

		return new Value(false);
	}

	private Value stringOverlaps(String op1, String val1, String op2, String val2) {
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			return new Value(true);
		} else if (op1.equals(op2)) {
			if (op1.equals("neq")) {
				return new Value(true);
				// If both operators are "eq", then the sets only overlap if the values are the same
			} else if (op1.equals("eq")) {
				return new Value((val1.equals(val2)));
				// If both operators are "str-lt" (or "str-le"), or "str-gt" (or "str-ge"), then the
				// sets overlap regardless of value
			} else if (op1.equals("str-lt") || op1.equals("str-le") || op1.equals("str-gt")
					|| op1.equals("str-ge") || op1.equals("str-contains")) {
				return new Value(true);
			} else if (op1.equals("str-prefix")) {
				return new Value(val1.startsWith(val2) || val2.startsWith(val1));
			} else if (op1.equals("str-postfix")) {
				return new Value(val1.endsWith(val2) || val2.endsWith(val1));
			} else if (op1.equals("before") || op1.equals("after")) {
				return new Value(true);
			} else if (op1.equals("str-index")) {
				// Trying not to use this operator
			} else {
				reteMatcherLogger.warn("Invalid operator for string.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for string: " + op2));
			}
		} else {
			if (op1.equals("str-lt")) {
				if (op2.equals("str-le") || op2.equals("str-postfix") || op2.equals("str-contains")
						|| op2.equals("neq")) {
					return new Value(true);
				} else if (op2.equals("eq") || op2.equals("str-gt") || op2.equals("str-ge")
						|| op2.equals("str-prefix")) {
					return new Value((val2.compareTo(val1) < 0));
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("str-le")) {
				if (op2.equals("str-lt") || op2.equals("str-postfix") || op2.equals("str-contains")
						|| op2.equals("neq")) {
					return new Value(true);
				} else if (op2.equals("eq") || op2.equals("str-ge") || op2.equals("str-prefix")) {
					return new Value((val2.compareTo(val1) <= 0));
				} else if (op2.equals("str-gt")) {
					return new Value((val2.compareTo(val1) < 0));
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("eq")) {
				if (op2.equals("str-lt")) {
					return new Value((val1.compareTo(val2) < 0));
				} else if (op2.equals("str-le")) {
					return new Value((val1.compareTo(val2) <= 0));
				} else if (op2.equals("str-gt")) {
					return new Value((val1.compareTo(val2) > 0));
				} else if (op2.equals("str-ge")) {
					return new Value((val1.compareTo(val2) >= 0));
				} else if (op2.equals("str-prefix")) {
					return new Value(val1.startsWith(val2));
				} else if (op2.equals("str-postfix")) {
					return new Value(val1.endsWith(val2));
				} else if (op2.equals("str-contains")) {
					return new Value(val1.indexOf(val2) != -1);
				} else if (op2.equals("neq")) {
					return new Value(val1.compareTo(val2) != 0);
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("str-ge")) {
				if (op2.equals("str-lt")) {
					return new Value((val1.compareTo(val2) < 0));
				} else if (op2.equals("str-le") || op2.equals("eq")) {
					return new Value((val1.compareTo(val2) <= 0));
				} else if (op2.equals("str-prefix")) {
					// return new Value((val1.startsWith(val2)));
					// change by shuang, for "str-ge" and "str-prefix" comparision
					return new Value(val1.startsWith(val2) || (val1.compareTo(val2) <= 0));
				} else if (op2.equals("str-gt") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return new Value((true));
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("str-gt")) {
				if (op2.equals("str-lt") || op2.equals("str-le") || op2.equals("eq")) {
					return new Value((val1.compareTo(val2) < 0));
				} else if (op2.equals("str-prefix")) {
					// return new Value((val1.startsWith(val2)));
					// change by shuang, for "str-gt" and "str-prefix" comparision
					return new Value(val1.startsWith(val2) || (val1.compareTo(val2) < 0));
				} else if (op2.equals("str-ge") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return new Value(true);
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("str-prefix")) {
				if (op2.equals("str-postfix") || op2.equals("str-contains") || op2.equals("neq")) {
					return new Value(true);
				} else if (op2.equals("str-lt") || op2.equals("str-le") || op2.equals("eq")
						|| op2.equals("str-ge") || op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				}
			} else if (op1.equals("str-postfix")) {
				if (op2.equals("str-prefix") || op2.equals("str-contains") || op2.equals("neq")) {
					return new Value(true);
				} else if (op2.equals("str-lt") || op2.equals("str-le") || op2.equals("eq")
						|| op2.equals("str-ge") || op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("str-contains")) {
				if (op2.equals("str-prefix") || op2.equals("str-postfix") || op2.equals("neq")) {
					return new Value(true);
				} else if (op2.equals("str-lt") || op2.equals("str-le") || op2.equals("eq")
						|| op2.equals("str-ge") || op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				} else {
					reteMatcherLogger.warn("Invalid operator for string.");
					exceptionLogger.warn("Here is an exception, ", new Exception(
							"Invalid operator for string: " + op2));
				}
			} else if (op1.equals("neq")) {
				if (op2.equals("eq")) {
					return new Value(!val1.equals(val2));
				} else {
					return new Value(true);
				}
			} else if (op1.equals("before")) {
				if (op2.equals("after")) {
					SimpleDateFormat timeFormat = new SimpleDateFormat(
							"EEE MMM dd HH:mm:ss zzz yyyy");
					Date time1 = new Date();
					Date time2 = new Date();
					try {
						time1 = timeFormat.parse(val1);
						time2 = timeFormat.parse(val2);
					} catch (ParseException pe) {
						reteMatcherLogger.error("Failed to parse time: " + pe);
						exceptionLogger.error("Failed to parse time: " + pe);
						return new Value(false);
					}
					return new Value(time2.before(time1));
				}
			} else if (op1.equals("after")) {
				if (op2.equals("before")) {
					SimpleDateFormat timeFormat = new SimpleDateFormat(
							"EEE MMM dd HH:mm:ss zzz yyyy");
					Date time1 = new Date();
					Date time2 = new Date();
					try {
						time1 = timeFormat.parse(val1);
						time2 = timeFormat.parse(val2);
					} catch (ParseException pe) {
						reteMatcherLogger.error("Failed to parse time: " + pe);
						exceptionLogger.error("Failed to parse time: " + pe);
						return new Value(false);
					}
					return new Value(time1.before(time2));
				}
			} else {
				reteMatcherLogger.warn("Invalid operator for string.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for string: " + op1));
			}
		}
		return new Value(false);
	}

	private Value booleanOverlaps(String op1, String val1, String op2, String val2) {
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			return new Value(true);
		} else if (op1.equals("eq")) {
			if (op2.equals("eq")) {
				return new Value(val1.equals(val2));
			} else if (op2.equals("neq")) {
				return new Value(!val1.equals(val2));
			} else {
				reteMatcherLogger.warn("Invalid operator for boolean.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for boolean: " + op2));
			}
		} else if (op1.equals("neq")) {
			if (op2.equals("eq")) {
				return new Value(!val1.equals(val2));
			} else if (op2.equals("neq")) {
				return new Value(val1.equals(val2));
			} else {
				reteMatcherLogger.warn("Invalid operator for boolean.");
				exceptionLogger.warn("Here is an exception, ", new Exception(
						"Invalid operator for boolean: " + op2));
			}
		} else {
			reteMatcherLogger.warn("Invalid operator for boolean.");
			exceptionLogger.warn("Here is an exception, ", new Exception(
					"Invalid operator for boolean: " + op1));
		}

		return null;
	}

	private boolean isAJessBoolean(String jessAtom) {
		if (jessAtom.equals("TRUE") || jessAtom.equals("FALSE")) {
			return true;
		} else {
			return false;
		}
	}

}

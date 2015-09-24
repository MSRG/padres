package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestString extends TestBase {
	public TestString(String op, String v) {
		super(op, v);
	}
	
	public boolean doTest(String op, Object val) {
		if (val == null || !(val instanceof String))
			return false;

		String value = (String) val;
		// constructor guarantees that constraint is a String
		String _constraint = (String) constraint;

		return stringOverlaps(operator, _constraint, op, value);
	}

	/*
	 * Taken from D. Matheson's original implementation
	 */
	private boolean stringOverlaps(String op1, String val1, String op2,
			String val2) {
		if (op1.equals("isPresent") || op2.equals("isPresent")) {
			return true;
		} else if (op1.equals(op2)) {
			if (op1.equals("neq")) {
				return true;
			} else if (op1.equals("eq")) {
				// if the value being matched in a variable return true
				if (val1.startsWith("$S$") || val2.startsWith("$S$"))
					return true;
				// If both ops are "eq", then the sets only overlap if the vals are the same
				return (val1.equals(val2));
			} else if (op1.equals("str-lt") || op1.equals("str-le")
					|| op1.equals("str-gt") || op1.equals("str-ge")
					|| op1.equals("str-contains")) {
				// If both op1s are "str-lt" (or "str-le"), or "str-gt" (or
				// "str-ge"), then the sets overlap regardless of val2
				return true;
			} else if (op1.equals("str-prefix")) {
				return val1.startsWith(val2) || val2.startsWith(val1);
			} else if (op1.equals("str-postfix")) {
				return val1.endsWith(val2) || val2.endsWith(val1);
			} else if (op1.equals("before") || op1.equals("after")) {
				return true;
			} else if (op1.equals("str-index")) {
				// Trying not to use this op1
			}
		} else {
			if (op1.equals("str-lt")) {
				if (op2.equals("str-le") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return true;
				} else if (op2.equals("eq") || op2.equals("str-gt")
						|| op2.equals("str-ge") || op2.equals("str-prefix")) {
					return (val2.compareTo(val1) < 0);
				}
			} else if (op1.equals("str-le")) {
				if (op2.equals("str-lt") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return true;
				} else if (op2.equals("eq") || op2.equals("str-ge")
						|| op2.equals("str-prefix")) {
					return (val2.compareTo(val1) <= 0);
				} else if (op2.equals("str-gt")) {
					return (val2.compareTo(val1) < 0);
				}
			} else if (op1.equals("eq")) {
				if (op2.equals("str-lt")) {
					return (val1.compareTo(val2) < 0);
				} else if (op2.equals("str-le")) {
					return (val1.compareTo(val2) <= 0);
				} else if (op2.equals("str-gt")) {
					return (val1.compareTo(val2) > 0);
				} else if (op2.equals("str-ge")) {
					return (val1.compareTo(val2) >= 0);
				} else if (op2.equals("str-prefix")) {
					return val1.startsWith(val2);
				} else if (op2.equals("str-postfix")) {
					return val1.endsWith(val2);
				} else if (op2.equals("str-contains")) {
					return val1.indexOf(val2) != -1;
				} else if (op2.equals("neq")) {
					return val1.compareTo(val2) != 0;
				}
			} else if (op1.equals("str-ge")) {
				if (op2.equals("str-lt")) {
					return (val1.compareTo(val2) < 0);
				} else if (op2.equals("str-le") || op2.equals("eq")) {
					return (val1.compareTo(val2) <= 0);
				} else if (op2.equals("str-prefix")) {
					// return (val1.startsWith(val2));
					// change by shuang, for "str-ge" and "str-prefix"
					// comparision
					return val1.startsWith(val2) || (val1.compareTo(val2) <= 0);
				} else if (op2.equals("str-gt") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return (true);
				}
			} else if (op1.equals("str-gt")) {
				if (op2.equals("str-lt") || op2.equals("str-le")
						|| op2.equals("eq")) {
					return (val1.compareTo(val2) < 0);
				} else if (op2.equals("str-prefix")) {
					// return (val1.startsWith(val2));
					// change by shuang, for "str-gt" and "str-prefix"
					// comparision
					return val1.startsWith(val2) || (val1.compareTo(val2) < 0);
				} else if (op2.equals("str-ge") || op2.equals("str-postfix")
						|| op2.equals("str-contains") || op2.equals("neq")) {
					return true;
				}
			} else if (op1.equals("str-prefix")) {
				if (op2.equals("str-postfix") || op2.equals("str-contains")
						|| op2.equals("neq")) {
					return true;
				} else if (op2.equals("str-lt") || op2.equals("str-le")
						|| op2.equals("eq") || op2.equals("str-ge")
						|| op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				}
			} else if (op1.equals("str-postfix")) {
				if (op2.equals("str-prefix") || op2.equals("str-contains")
						|| op2.equals("neq")) {
					return true;
				} else if (op2.equals("str-lt") || op2.equals("str-le")
						|| op2.equals("eq") || op2.equals("str-ge")
						|| op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				}
			} else if (op1.equals("str-contains")) {
				if (op2.equals("str-prefix") || op2.equals("str-postfix")
						|| op2.equals("neq")) {
					return true;
				} else if (op2.equals("str-lt") || op2.equals("str-le")
						|| op2.equals("eq") || op2.equals("str-ge")
						|| op2.equals("str-gt")) {
					return stringOverlaps(op2, val2, op1, val1);
				}
			} else if (op1.equals("neq")) {
				if (op2.equals("eq")) {
					return !val1.equals(val2);
				} else {
					return true;
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
						return false;
					}
					return time2.before(time1);
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
						return false;
					}
					return time1.before(time2);
				}
			}
		}
		return false;
	}
}

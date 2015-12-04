package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

/*
 * Should unify this class with TestLong since they do pretty much the
 * same thing. But we'll keep the convention that's already been setup.
 * 
 * This separate class *might* be useful if the Long tests diverge from the
 * Double tests for some reason. 
 * 
 */
public class TestLong extends TestBase {
	public TestLong(String op, Long val) {
		super(op, val);
	}

	public boolean doTest(String op2, Object val) {
		if (val == null || !(val instanceof Long))
			return false;
		
		long value = ((Long)val).longValue();
		// constructor guarantees constraint is a Long
		long _constraint = ((Long)constraint).longValue();
		
		if(operator.equals("isPresent") || op2.equals("isPresent")) {
			return true;
		} else if (operator.equals("<")) {
			if (op2.equals("<") || op2.equals("<=") || op2.equals("<>"))
				return true;
			else if (op2.equals(">") || op2.equals(">=") || op2.equals("="))
				return (value < _constraint);
		} else if (operator.equals("<=")) {
			if (op2.equals("<") || op2.equals("<=") || op2.equals("<>"))
				return true;
			else if (op2.equals(">"))
				return (value < _constraint);
			else if (op2.equals(">=") || op2.equals("="))
		 	  return (value <= _constraint);
		} else if (operator.equals("=")) {
			if (op2.equals("="))
		 		return (value == _constraint);
			else if (op2.equals("<"))
				return (value > _constraint);
			else if (op2.equals("<="))
				return (value >= _constraint);
			else if (op2.equals("<>"))
				return (value != _constraint);
			else if (op2.equals(">"))
				return (value < _constraint);
			else if (op2.equals(">="))
				return (value <= _constraint);
		} else if (operator.equals(">=")) {
			if (op2.equals(">") || op2.equals(">=") || op2.equals("<>"))
				return true;
			else if (op2.equals("<"))
				return (value > _constraint);
			else if (op2.equals("<=") || op2.equals("=")) 
		 		return (value >= _constraint);
		} else if (operator.equals(">")) {
			if (op2.equals(">") || op2.equals(">=") || op2.equals("<>"))
				return true;
			else if (op2.equals("<") || op2.equals("<=") || op2.equals("=")) 
		 		return (value > _constraint);
		} else if (operator.equals("<>") && value != _constraint)
			if (op2.equals("=")) 
				return (value != _constraint);
			// we explicit test the other operator to catch unrecognized operators
			else if (op2.equals(">") || op2.equals(">=") || op2.equals("<>") || op2.equals("<") || op2.equals("<="))
				return true;
		else
			// will reach this line if op1 is not recognized
			return false;

		// will reach this line if op2 is not recognized
		return false;
	}
}

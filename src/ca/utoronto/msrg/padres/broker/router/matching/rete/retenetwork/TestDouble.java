package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

public class TestDouble extends TestBase {
	public TestDouble(String op, Double val) {
		super(op, val);
	}

	public boolean doTest(String op2, Object val) {
		if (val == null || !(val instanceof Double || val instanceof Long))
			return false;

		double value = val instanceof Double ? ((Double) val).doubleValue()
				: ((Long) val).doubleValue();
		// constructor guarantees constraint is a Double
		double _constraint = ((Double) constraint).doubleValue();

		if (operator.equals("isPresent") || op2.equals("isPresent")) {
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
		} else if (operator.equals("<>") && value != _constraint) {
			if (op2.equals("="))
				return (value != _constraint);
			// we explicit test the other operator to catch unrecognized operators
			else if (op2.equals(">") || op2.equals(">=") || op2.equals("<>") || op2.equals("<")
					|| op2.equals("<="))
				return true;
		} else {
			// will reach this line if op1 is not recognized
			return false;
		}

		// will reach this line if op2 is not recognized
		return false;
	}
}

package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

public abstract class TestBase implements Test {
	protected String operator;

	protected Object constraint;

	public TestBase(String op, Object v) {
		operator = op;
		constraint = v;
	}

	public String getTestOp() {
		return operator;
	}

	public Object getTestValue() {
		return constraint;
	}

	public String toString() {
		return "Test: (" + operator + "; " + constraint + ")"; 
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (this.getClass() != o.getClass())
			return false;

		TestBase n = (TestBase) o;
		return (operator.equals(n.getTestOp()) && constraint.equals(n
				.getTestValue()));
	}
}

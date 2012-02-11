package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

public abstract class DefaultPolicy implements Policy {

	protected String policyName;

	protected ReteNetwork reteNW;

	public DefaultPolicy(String pName, ReteNetwork rn) {
		policyName = pName;
		reteNW = rn;
	}

	public String getPolicyName() {
		return policyName;
	}

}

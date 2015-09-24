package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

public class NodeTRight extends Node1 {

	private static final long serialVersionUID = 1L;

	public NodeTRight(ReteNetwork rn) {
		super(rn);
	}

	public boolean callNodeRight(Object p, int matchCount) {
		passAlong(p, matchCount);
		return true;
	}

}

package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.io.Serializable;

/**
 * Single input, mulitple output node in Rete network.
 *
 */
public class Node1 extends Node implements Serializable {

	private static final long serialVersionUID = 1378386668245070246L;

	public Node1(ReteNetwork rn) {
		super(rn);
	}

	public Node getParent() {
		return super.getParent("L");
	}

}

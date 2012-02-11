package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

/**
 * This is 1-input attribute test node in the Rete network. It consists of the attribute name and a
 * instance of the {@link Test} class to perform node-level match.
 */
public class NodeTAttr extends Node1 {

	private static final long serialVersionUID = 1L;

	private String attrName;

	private Test tt;

	public NodeTAttr(String name, Test t, ReteNetwork rn) {
		super(rn);
		attrName = name;
		tt = t;
	}

	public String getAttrName() {
		return attrName;
	}

	public Test getTest() {
		return tt;
	}

	public boolean callNodeRight(Object obj, int matchCount) {
		String operator = null;
		Object value = null;
		if (obj instanceof Publication) {
			value = ((Publication) obj).getPairMap().get(attrName);
			if (value != null) {
				if (value.getClass().equals(String.class))
					operator = "eq";
				else
					operator = "=";
			}
		} else if (obj instanceof Subscription) {
			Predicate pred = (Predicate) ((Subscription) obj).getPredicateMap().get(attrName);
			if (pred != null) {
				value = pred.getValue();
				operator = pred.getOp();
			}
		} else if (obj instanceof Advertisement) {
			Predicate pred = (Predicate) ((Advertisement) obj).getPredicateMap().get(attrName);
			if (pred != null) {
				value = pred.getValue();
				operator = pred.getOp();
			}
		}

		boolean result = false;
		if (operator != null && value != null) {
			result = tt.doTest(operator, value);
		}

		if (result) {
			passAlong(obj, matchCount + 1);
		} else if (reteNW.getNWType() == ReteNetwork.NWType.ADV_TREE) {
			passAlong(obj, matchCount);
		}

		return result;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (this.getClass() != o.getClass())
			return false;

		NodeTAttr n = (NodeTAttr) o;
		return (attrName.equals(n.getAttrName()) && tt.equals(n.getTest()));
	}

	public String toString() {
		return this.getClass().getSimpleName() + " Attr: " + this.attrName + " " + this.tt;
	}

}

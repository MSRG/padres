package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.io.Serializable;
import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

/**
 * The class node in Rete network. It is the first level node in the node
 * hierarchy in the network. It is a 1-input node with the root node as its left
 * parent.
 */
public class NodeTClass extends Node1 {

	private static final long serialVersionUID = 1L;

	private String class_name;

	private Test tt;

	public NodeTClass(String name, Test t, ReteNetwork rn) {
		super(rn);
		class_name = name;
		tt = t;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.rete.Node#callNodeRight(java.lang.Object)
	 */
	public boolean callNodeRight(Object p, int matchCount) {
		boolean result = false;
		String operator = null;
		Object value = null;
		if (p instanceof Publication) {
			Map<String, Serializable> pairMap = ((Publication) p).getPairMap();
			value = (Object) pairMap.get("class");
			operator = "eq";
		} else if (p instanceof Advertisement) {
			Map<String, Predicate> pairMap = ((Advertisement) p).getPredicateMap();
			Predicate pre = pairMap.get("class");
			if (pre != null) {
				value = pre.getValue();
				operator = pre.getOp();
			}
		} else if (p instanceof Subscription) {
			Map<String, Predicate> pairMap = ((Subscription) p).getPredicateMap();
			Predicate pre = pairMap.get("class");
			if (pre != null) {
				value = pre.getValue();
				operator = pre.getOp();
			}
		}

		if (operator != null && value != null) {
			result = tt.doTest(operator, value);
		}
		if (result) {
			passAlong(p, matchCount + 1);
		}

		return result;
	}

	public String getClassName() {
		return class_name;
	}

	/**
	 * NodeTClass equals only if the class name matches.
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (this.getClass() != o.getClass())
			return false;

		NodeTClass n = (NodeTClass) o;
		return (class_name.equals(n.getClassName()));
	}

	public String toString() {
		return this.getClass().getSimpleName() + " " + getClassName();
	}
}

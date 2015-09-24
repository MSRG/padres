package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;

/**
 * <p>
 * This is the dummy node in Rete network. It is a 1-input node acting as an intermediate point
 * between {@link NodeTAttr} and {@link NodeTerminal} or {@link NodeJoin} node.
 * </p>
 * <p>
 * It holds the variable name -> attributes map to facilate the function of NodeJoin
 * </p>
 */
public class NodeTLeft extends Node1 {

	private static final long serialVersionUID = 1L;

	private Map<String, Set<String>> variableMap = new HashMap<String, Set<String>>();

	private boolean hasVariable = false;

	/**
	 * Creates the variable -> attributes map from the provides predicateMap
	 * 
	 * @param predicateMap
	 */
	public NodeTLeft(Map<String, Predicate> predicateMap, ReteNetwork rn) {
		super(rn);
		// create/update the variable -> attributes map
		for (String attribute : predicateMap.keySet()) {
			if (attribute.equalsIgnoreCase("class"))
				continue;
			Predicate p = predicateMap.get(attribute);
			Object v = (Object) p.getValue();
			if (v.toString().startsWith("$S$") || v.toString().startsWith("$I$")) {
				hasVariable = true;
				String variableName = v.toString().substring(3);
				if (variableMap.containsKey(variableName)) {
					Set<String> attrSet = variableMap.get(variableName);
					attrSet.add(attribute);
				} else {
					Set<String> attrSet = new HashSet<String>();
					attrSet.add(attribute);
					variableMap.put(variableName, attrSet);
				}
			}
		}
	}

	public Map<String, Set<String>> getVariableMap() {
		return variableMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see matching.rete.Node#callNodeRight(java.lang.Object)
	 */
	public boolean callNodeRight(Object p, int matchCount) {
		// TODO: double check the "p instanceof Publication" condition
		if (p instanceof Publication && hasVariable) {
			if (evaluateVariable((Publication) p)) {
				// create result for passing along
				reteNW.setPartialMatchFound();
				MemoryUnit passAlongObj = generatePassAlongResult(p);
				return callNodeLeft(passAlongObj, matchCount);
			}
		} else {
			// NodeTLeft is a dummy node (non-variable)
			return callNodeLeft(p, matchCount);
		}
		return false;
	}

	private MemoryUnit generatePassAlongResult(Object p) {
		MemoryUnit memUnit = new MemoryUnit();
		memUnit.addPid(((Publication) p).getPubID());
		for (String varName : variableMap.keySet()) {
			Set<String> attrSet = variableMap.get(varName);
			String attr = (String) attrSet.iterator().next();
			memUnit.addVarValue(varName, ((Publication) p).getPairMap().get(attr));
		}

		return memUnit;
	}

	public boolean callNodeLeft(Object p, int matchCount) {
		passAlong(p, matchCount);
		return true;
	}

	public void passAlong(Object p, int matchCount) {
		for (int j = 0; j < nSucc; j++) {
			Node s = succ[j];

			if (s.rightParent == this) {
				s.callNodeRight(p, matchCount);
			} else if (s.leftParent == this) {
				s.callNodeLeft(p, matchCount);
			} else {
				System.out.println("Not Left or Right Parents");
			}

		}
	}

	/**
	 * Evaluates whether a Publication object matches all the variables defined in this node.
	 * 
	 * @param p
	 *            The publication object.
	 * @return {@code true} if the publication matches; {@code false} otherwise.
	 */
	private boolean evaluateVariable(Publication p) {
		boolean passEvaluation = true;
		for (String varName : variableMap.keySet()) {
			Set<String> attrSet = variableMap.get(varName);
			String sameValue = null;
			for (String attrName : attrSet) {
				Object value = p.getPairMap().get(attrName);
				if (sameValue == null) {
					sameValue = value.toString();
				} else {
					passEvaluation = sameValue.equals(value.toString());
					if (!passEvaluation)
						return passEvaluation;
				}
			}
		}
		return passEvaluation;
	}

	public String toString() {
		return this.getClass().getSimpleName() + " Parent: " + leftParent;
	}
}

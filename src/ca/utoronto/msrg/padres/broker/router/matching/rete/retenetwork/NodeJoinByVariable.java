package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;

@Deprecated
public class NodeJoinByVariable extends Node2 {

	private static final long serialVersionUID = 1L;

	String m_op;

	Node m_right;

	Node m_left;

	Map<String, Predicate> predicateMapRight;

	Map<String, Map<String, Set<String>>> variableMap = new HashMap<String, Map<String, Set<String>>>();

	Map<String, Set<String>> rightVariableMap = new HashMap<String, Set<String>>();

	boolean rightPartWithVariable = false;

	private ArrayList<Object> mem_left = new ArrayList<Object>();

	private ArrayList<Map<String, Object>> mem_right = new ArrayList<Map<String, Object>>();

	// TODO: remove after testing
	// private ReteListener re;

	private ConsumeAndRemain eventConsumer;

	public NodeJoinByVariable(Node left, String op, Node right, Map<String, Predicate> rightMap,
			ReteNetwork rb) {
		super(rb);
		eventConsumer = new ConsumeAndRemain("DEFAULT", rb);
		m_op = op;
		m_right = right;
		m_left = left;
		// TODO: you don't really have to pass the predicate map separately
		predicateMapRight = rightMap;
		// TODO: remove after testing
		// re = new ReteListener(rb);
		// this.addReteListener(re);
		buildVaribleMap(left);
	}

	private void buildVaribleMap(Node left) {
		// add variables from left parent (dummy node or join node)
		if (left instanceof NodeTLeft) {
			Map<String, Set<String>> leftVariableMap = ((NodeTLeft) left).getVariableMap();
			for (Iterator<String> i = leftVariableMap.keySet().iterator(); i.hasNext();) {
				String variableName = i.next();
				Map<String, Set<String>> leftMap = new HashMap<String, Set<String>>();
				leftMap.put("LEFT", leftVariableMap.get(variableName));
				variableMap.put(variableName, leftMap);
			}
		} else if (left instanceof NodeJoin) {
			// do nothing
		} else if (left instanceof NodeJoinByVariable) {
			Map<String, Map<String, Set<String>>> leftVariableMap = ((NodeJoinByVariable) left).getVariableMap();
			for (Iterator<String> i = leftVariableMap.keySet().iterator(); i.hasNext();) {
				String variableName = i.next();
				Map<String, Set<String>> tmpMap = leftVariableMap.get(variableName);
				Map<String, Set<String>> leftMap = new HashMap<String, Set<String>>();
				if (tmpMap.containsKey("LEFT")) {
					leftMap.put("LEFT", tmpMap.get("LEFT"));
				}
				if (tmpMap.containsKey("RIGHT")) {
					leftMap.put("LEFT", tmpMap.get("RIGHT"));
				}
				variableMap.put(variableName, leftMap);
			}
		}
		// add variables from right parent
		for (String attribute : predicateMapRight.keySet()) {
			if (attribute.equalsIgnoreCase("class"))
				continue;
			Predicate p = (Predicate) predicateMapRight.get(attribute);
			Object v = (Object) p.getValue();
			if (v.toString().startsWith("$S$") || v.toString().startsWith("$I$")) {
				String variableName = v.toString().substring(3);
				if (variableMap.containsKey(variableName)) {
					Map<String, Set<String>> tmpMap = variableMap.get(variableName);
					if (tmpMap.containsKey("RIGHT")) {
						Set<String> attrSet = tmpMap.get("RIGHT");
						attrSet.add(attribute);
					} else {
						Set<String> attrSet = new HashSet<String>();
						attrSet.add(attribute);
						tmpMap.put("RIGHT", attrSet);
					}
				} else {
					Set<String> attrSet = new HashSet<String>();
					attrSet.add(attribute);
					Map<String, Set<String>> tmpMap = new HashMap<String, Set<String>>();
					tmpMap.put("RIGHT", attrSet);
					variableMap.put(variableName, tmpMap);
				}
			}
		}
		// generate rightVariableMap
		for (Iterator<String> j = variableMap.keySet().iterator(); j.hasNext();) {
			String variableName = j.next();
			Map<String, Set<String>> tmpMap = variableMap.get(variableName);
			if (tmpMap.containsKey("RIGHT")) {
				rightPartWithVariable = true;
				rightVariableMap.put(variableName, tmpMap.get("RIGHT"));
			}
		}
	}

	public boolean hasRightPartWithVariable() {
		return rightPartWithVariable;
	}

	public Map<String, Set<String>> getRightVariableMap() {
		return rightVariableMap;
	}

	public Map<String, Map<String, Set<String>>> getVariableMap() {
		return variableMap;
	}

	public String getOp() {
		return m_op;
	}

	public Node getRightNode() {
		return m_right;
	}

	public Node getLeftNode() {
		return m_left;
	}

	public boolean callNodeRight(Object p, int matchCount) {
		if (p instanceof Publication) {
			Map<String, Object> tmpMap = new HashMap<String, Object>();
			if (rightPartWithVariable) {
				tmpMap.put(((Publication) p).getPubID(), rightVariableMap);
			} else {
				tmpMap.put(((Publication) p).getPubID(), p);
			}
			mem_right.add(tmpMap);
		} else {
			// TODO: produce exception?
			// p can be only the publication.
			// mem_right.add(p);
		}
		boolean result = eventConsumer.consumeEventWithVariable(this, "LEFT", p, matchCount,
				variableMap);
		return result;
	}

	public boolean callNodeLeft(Object p, int matchCount) {
		boolean result;
		Map<String, Object> tmpMap = new HashMap<String, Object>();
		if (p instanceof Publication) {
			// then p has no variable
			tmpMap.put(((Publication) p).getPubID(), p);
			mem_left.add(tmpMap);
			result = eventConsumer.consumeEventWithVariable(this, "RIGHT", tmpMap, matchCount,
					variableMap);
		} else if (p instanceof Advertisement) {
			// drop it
			result = true;
		} else {
			// what we get here is a map, with pid/variable/attr set info.
			// mem_left.add(generateMemResult(p));
			mem_left.add(p);
			result = eventConsumer.consumeEventWithVariable(this, "RIGHT", p, matchCount,
					variableMap);
		}
		return result;
	}

	public void passAlong(Object p, int matchCount) {
		for (int j = 0; j < nSucc; j++) {
			Node s = succ[j];
			s.callNodeLeft(p, matchCount);
		}
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (this.getClass() != o.getClass())
			return false;

		NodeJoinByVariable n = (NodeJoinByVariable) o;
		return (m_op.equals(n.getOp()) && m_right.equals(n.getRightNode()) && m_left.equals(n.getLeftNode()));
	}

	public void flushMemory() {
		mem_left.clear();
		mem_right.clear();
	}

	public String toString() {
		return this.getClass().getSimpleName() + " " + this.m_op;
	}

	public ArrayList<Object> getLeftMem() {
		return mem_left;
	}

	public ArrayList<Map<String, Object>> getRightMem() {
		return mem_right;
	}
}

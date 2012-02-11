package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

public class NodeJoinUnify extends Node2 {

	private static final long serialVersionUID = 1L;

	String m_op;

	Node m_right;

	Node m_left;

	private ConsumeAndRemain eventConsumer;

	private ArrayList<MemoryUnit> mem_left = new ArrayList<MemoryUnit>();

	private ArrayList<MemoryUnit> mem_right = new ArrayList<MemoryUnit>();

	private Map<String, String> leftVarMap = new HashMap<String, String>();

	private Map<String, String> rightVarMap = new HashMap<String, String>();

	private Set<String> correlationVars = new HashSet<String>();

	public NodeJoinUnify(Node left, String op, Node right, ReteNetwork rn) {
		super(rn);

		m_op = op;
		m_right = right;
		m_left = left;

		eventConsumer = new ConsumeAndRemain("DEFAULT", rn);

		buildVariableMap(left, leftVarMap);
		buildVariableMap(right, rightVarMap);
		buildCorrelationVariableMap();
	}

	private void buildCorrelationVariableMap() {
		for (String var : leftVarMap.keySet()) {
			if (rightVarMap.keySet().contains(var)) {
				correlationVars.add(var);
			}
		}
	}

	private void buildVariableMap(Node parent, Map<String, String> varMap) {
		if (parent instanceof NodeTLeft) {
			Map<String, Set<String>> parentVarMap = ((NodeTLeft) parent).getVariableMap();
			for (String varName : parentVarMap.keySet()) {
				Set<String> attrs = parentVarMap.get(varName);
				String attr = (String) attrs.iterator().next();
				varMap.put(varName, attr);
			}
		} else if (parent instanceof NodeJoinUnify) {
			Map<String, String> parentVarMap = ((NodeJoinUnify) parent).getVariableMap();
			varMap.putAll(parentVarMap);
		}
	}

	public ArrayList<MemoryUnit> getLeftMemory() {
		return mem_left;
	}

	public ArrayList<MemoryUnit> getRightMemory() {
		return mem_right;
	}

	public String getOp() {
		return m_op;
	}

	public Map<String, String> getLeftVarMap() {
		return leftVarMap;
	}

	public Map<String, String> getRightVarMap() {
		return rightVarMap;
	}

	public Map<String, String> getVariableMap() {
		Map<String, String> allVarMap = new HashMap<String, String>();
		allVarMap.putAll(leftVarMap);
		for (String var : rightVarMap.keySet()) {
			if (!correlationVars.contains(var)) {
				allVarMap.put(var, rightVarMap.get(var));
			}
		}
		return allVarMap;
	}

	public Set<String> getCorrelationVars() {
		return correlationVars;
	}

	public boolean callNodeLeft(Object p, int matchCount) {
		boolean result;
		if (p instanceof Publication) {
			MemoryUnit mem = new MemoryUnit();
			mem.addPid(((Publication) p).getPubID());
			reteNW.setPartialMatchFound();
			mem_left.add(mem);
			// TODO consume event in right memory
			result = eventConsumer.consumeEvent(this, "RIGHT", mem, matchCount);
		} else if (p instanceof Advertisement) {
			// drop it
			result = true;
		} else if (p instanceof MemoryUnit) {
			mem_left.add((MemoryUnit) p);
			result = eventConsumer.consumeEvent(this, "RIGHT", (MemoryUnit) p, matchCount);
		} else if (p instanceof Subscription) {
			result = true;
		} else {
			System.err.println("wrong objects received at NodeJoinUnify left parent");
			result = false;
		}
		return result;
	}

	public boolean callNodeRight(Object p, int matchCount) {
		boolean result;
		if (p instanceof Publication) {
			MemoryUnit mem = new MemoryUnit();
			mem.addPid(((Publication) p).getPubID());
			reteNW.setPartialMatchFound();
			mem_right.add(mem);
			// TODO consume event in right memory
			result = eventConsumer.consumeEvent(this, "LEFT", mem, matchCount);
		} else if (p instanceof Advertisement) {
			// drop it
			result = true;
		} else if (p instanceof MemoryUnit) {
			mem_right.add((MemoryUnit) p);
			result = eventConsumer.consumeEvent(this, "LEFT", (MemoryUnit) p, matchCount);
		} else if (p instanceof Subscription) {
			result = true;
		} else {
			System.err.println("wrong objects received at NodeJoinUnify right parent");
			result = false;
		}
		return result;
	}

	public void passAlong(Object p, int matchCount) {
		// TODO: callNodeLeft or callNodeRight?
		for (int j = 0; j < nSucc; j++) {
			Node s = succ[j];
			if (s.rightParent == this) {
				s.callNodeRight(p, matchCount);
			} else if (s.leftParent == this) {
				s.callNodeLeft(p, matchCount);
			} else {
				System.err.println("Not Left or Right Parents");
			}
		}
	}

	public boolean isInMemory(String pubID) {
		System.out.println("checking left memory");
		for (MemoryUnit mem : mem_left) {
			if (mem.hasPID(pubID))
				return true;
		}
		System.out.println("checking right memory");
		for (MemoryUnit mem : mem_right) {
			if (mem.hasPID(pubID))
				return true;
		}
		return false;
	}

	public void flushMemory() {
		mem_left.clear();
		mem_right.clear();
	}

	public void printMemory(ArrayList<MemoryUnit> memeoryList) {
		for (int i = 0; i < memeoryList.size(); i++) {
			MemoryUnit m = memeoryList.get(i);
			System.out.println(i);
			printMemoryUnit(m);
		}
	}

	private void printMemoryUnit(MemoryUnit mem) {
		System.out.println("=Start==========");
		if (mem.getPids() != null) {
			System.out.println(mem.getPids());
		}

		if (mem.getVarValues() != null) {
			System.out.println(mem.getVarValues());
		}
		System.out.println("=End==========");
	}
}

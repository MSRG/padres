package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.Publication;

@Deprecated
public class NodeJoin extends Node2 {

	private static final long serialVersionUID = 1L;

	String m_op;

	// TODO: do we need this? we already have a right parent from Node class.
	Node m_right;

	private ArrayList<Object> mem_left = new ArrayList<Object>();

	private ArrayList<Map<String, Publication>> mem_right = new ArrayList<Map<String, Publication>>();

	private ConsumeAndRemain eventConsumer;

	public NodeJoin(String op, Node right, ReteNetwork rb) {
		super(rb);
		eventConsumer = new ConsumeAndRemain("DEFAULT", rb);
		m_op = op;
		m_right = right;
	}

	public String getOp() {
		return m_op;
	}

	public Node getRightNode() {
		return m_right;
	}

	public boolean callNodeRight(Object p, int matchCount) {
		if (p instanceof Publication) {
			Map<String, Publication> tmpMap = new HashMap<String, Publication>();
			tmpMap.put(((Publication) p).getPubID(), (Publication) p);
			mem_right.add(tmpMap);
		} else if (p instanceof Advertisement) {
			// drop it
			return true;
		} else {
			// TODO: check whether this should raise an exception
			// it can be a map??
		}
		return eventConsumer.consumeEvent(this, "LEFT", p, matchCount);
	}

	public boolean callNodeLeft(Object p, int matchCount) {
		boolean result;
		if (p instanceof Publication) {
			Map<String, Object> tmpMap = new HashMap<String, Object>();
			tmpMap.put(((Publication) p).getPubID(), p);
			mem_left.add(tmpMap);
			result = eventConsumer.consumeEvent(this, "RIGHT", tmpMap, matchCount);
		} else if (p instanceof Advertisement) {
			// drop it
			result = true;
		} else {
			mem_left.add(p);
			result = eventConsumer.consumeEvent(this, "RIGHT", p, matchCount);
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

		NodeJoin n = (NodeJoin) o;
		return (m_op.equals(n.getOp()) && m_right.equals(n.getRightNode()));
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

	public ArrayList<Map<String, Publication>> getRightMem() {
		return mem_right;
	}
}

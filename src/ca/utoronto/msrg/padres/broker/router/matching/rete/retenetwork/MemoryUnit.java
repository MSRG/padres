package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemoryUnit {

	private Set<String> pidSet;

	private Map<String, Object> varValues;

	public MemoryUnit() {
		pidSet = null;
		varValues = null;
	}

	void addPid(String pid) {
		if (pidSet == null)
			pidSet = new HashSet<String>();
		pidSet.add(pid);
	}

	void addPids(Set<String> name) {
		if (pidSet == null)
			pidSet = new HashSet<String>();
		pidSet.addAll(name);
	}

	void addVarValue(String varName, Object val) {
		if (varValues == null)
			varValues = new HashMap<String, Object>();
		varValues.put(varName, val);
	}

	void addVarValues(Map<String, Object> varValMap) {
		if (varValues == null)
			varValues = new HashMap<String, Object>();
		varValues.putAll(varValMap);
	}

	Set<String> getPids() {
		return pidSet;
	}

	public boolean hasPID(String pid) {
		return pidSet.contains(pid);
	}

	Map<String, Object> getVarValues() {
		return varValues;
	}
}

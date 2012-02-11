package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionOPs;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;

public class ConsumeAndRemain extends DefaultPolicy {

	public ConsumeAndRemain(String pName, ReteNetwork rn) {
		super(pName, rn);
	}

	public boolean consumeEvent(NodeJoinUnify node, String otherMem, MemoryUnit mem, int matchCount) {
		// get the memory from the other parent
		String mOp = node.getOp();
		ArrayList<MemoryUnit> memory = null;

		if (otherMem.equals("LEFT")) {
			memory = node.getLeftMemory();
		} else if (otherMem.equals("RIGHT")) {
			memory = node.getRightMemory();
		}
		Map<String, Object> varAndValues = mem.getVarValues();
		if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)) {
			// combine both the parents memories and broadcast it all the children
			for (int i = 0; i < memory.size(); i++) {
				MemoryUnit resultMem = new MemoryUnit();
				MemoryUnit curMem = memory.get(i);
				Map<String, Object> curVarAndValues = curMem.getVarValues();
				boolean correlated = true;
				for (String var : node.getCorrelationVars()) {
					if (!varAndValues.get(var).equals(curVarAndValues.get(var)))
						correlated = false;
				}
				if (correlated) {
					resultMem = mergeMemoryUnits(mem, curMem);
					node.passAlong(resultMem, matchCount);
				}

			}
		} else if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {
			node.passAlong(mem, matchCount);
		}

		return true;
	}

	@Deprecated
	public boolean consumeEvent(NodeJoin node, String otherMem, Object p, int matchCount) {
		// get the memory from the other parent
		String mOp = node.getOp();
		ArrayList mem = null;
		if (otherMem.equals("LEFT")) {
			mem = node.getLeftMem();
		} else if (otherMem.equals("RIGHT")) {
			mem = node.getRightMem();
		}
		if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)) {
			// combine both the parents memories and broadcast it all the children
			for (int i = 0; i < mem.size(); i++) {
				Map passObj = (Map) mem.get(i);
				if (p instanceof Publication) {
					passObj.put(((Publication) p).getPubID(), p);
				} else {
					passObj.putAll((Map) p);
				}
				node.passAlong(passObj, matchCount);
			}
		} else if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {
			Map passObj = new HashMap();
			if (p instanceof Publication) {
				passObj.put(((Publication) p).getPubID(), p);
			} else {
				// TODO: check - seems we never get here!
				passObj.putAll((Map) p);
			}
			node.passAlong(passObj, matchCount);
		}
		return true;
	}

	@Deprecated
	public boolean consumeEventWithVariable(NodeJoinByVariable joinNode, String otherMem, Object p,
			int matchCount, Map<String, Map<String, Set<String>>> variableMap) {
		String mOp = joinNode.getOp();
		ArrayList<Object> leftMem = null;
		ArrayList<Map<String, Object>> rightMem = null;
		if (otherMem.equals("LEFT")) {
			leftMem = joinNode.getLeftMem();
		} else if (otherMem.equals("RIGHT")) {
			rightMem = joinNode.getRightMem();
		}
		if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)) {
			if (p instanceof Publication) {
				Publication pub = (Publication) p;
				// publication must come from right side
				if (joinNode.hasRightPartWithVariable()) {
					if (!checkRightPart(pub, variableMap))
						return false;
					// need to agree on some value for each variable
					if (leftMem != null) {
						for (int j = 0; j < leftMem.size(); j++) {
							Map passObj = (Map) leftMem.get(j);
							if (checkLeftMem(passObj, pub, variableMap)) {
								passObj.put(pub.getPubID(), joinNode.getRightVariableMap());
								joinNode.passAlong(passObj, matchCount);
							}
						}
					}
				} else {
					// do not need correlate
					if (leftMem != null) {
						for (int j = 0; j < leftMem.size(); j++) {
							Map passObj = (Map) leftMem.get(j);
							passObj.put(pub.getPubID(), pub);
							joinNode.passAlong(passObj, matchCount);
						}
					}

				}
			} else {
				// from the left side, it must alreday agree with some value for each variable.
				if (rightMem != null) {
					for (int j = 0; j < rightMem.size(); j++) {
						Map<String, Object> passObj = rightMem.get(j);
						if (checkRightMem(passObj, (Map) p, variableMap)) {
							passObj.putAll((Map) p);
							joinNode.passAlong(passObj, matchCount);
						}
					}
				}
			}

		} else if (mOp.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {
			Map passObj = new HashMap();
			if (p instanceof Publication) {
				Publication pub = (Publication) p;
				if (joinNode.hasRightPartWithVariable()) {
					if (!checkRightPart(pub, variableMap))
						return false;
					passObj.put(pub.getPubID(), joinNode.getRightVariableMap());
				} else {
					passObj.put(pub.getPubID(), p);
				}

			} else {
				passObj.putAll((Map) p);
			}
			joinNode.passAlong(passObj, matchCount);
		}

		return true;
	}

	private boolean checkRightPart(Publication p, Map<String, Map<String, Set<String>>> variableMap) {
		Map pubAttrValMap = p.getPairMap();
		for (Iterator<String> i = variableMap.keySet().iterator(); i.hasNext();) {
			// for every variable
			Map<String, Set<String>> tmpMap = variableMap.get(i.next());
			if (tmpMap.containsKey("RIGHT")) {
				// check whether the variable is required for the right side
				Set<String> attrSet = tmpMap.get("RIGHT");
				String sameValue = null;
				for (Iterator<String> j = attrSet.iterator(); j.hasNext();) {
					// for every attribute that is marked by the variable look for the same
					// attribute in the publication
					Object valInPub = pubAttrValMap.get(j.next());
					if (sameValue == null) {
						sameValue = valInPub.toString();
					} else {
						if (!sameValue.equals(valInPub.toString()))
							return false;
					}
				}
			}
		}
		return true;
	}

	private MemoryUnit mergeMemoryUnits(MemoryUnit left, MemoryUnit right) {
		MemoryUnit tmpMem = new MemoryUnit();
		if (left.getPids() != null)
			tmpMem.addPids(left.getPids());
		if (right.getPids() != null)
			tmpMem.addPids(right.getPids());
		if (left.getVarValues() != null)
			tmpMem.addVarValues(left.getVarValues());

		if (right.getVarValues() != null)
			tmpMem.addVarValues(right.getVarValues());
		return tmpMem;
	}

	/**
	 * When the node is activated from right node, check the left memory for matching publications.
	 * 
	 * @param passObj
	 *            pubID->pubMsg map from left memory.
	 * @param p
	 *            Publication passed from the right parent.
	 * @param variableMap
	 *            variable map in the join node.
	 * @param reteEng
	 * @return {@code true} if all the defined variables are matched between the passed publication
	 *         and at least one of the publication from the left memory; {@code false} otherwise.
	 */
	private boolean checkLeftMem(Map passObj, Publication p,
			Map<String, Map<String, Set<String>>> variableMap) {
		for (Iterator<String> i = variableMap.keySet().iterator(); i.hasNext();) {
			String variableName = i.next();
			Map<String, Set<String>> sideAttrMap = variableMap.get(variableName);
			if (sideAttrMap.containsKey("RIGHT") && sideAttrMap.containsKey("LEFT")) {
				Set<String> rightAttrSet = sideAttrMap.get("RIGHT");
				String rightFirstAttr = rightAttrSet.iterator().next();
				String rightValue = p.getPairMap().get(rightFirstAttr).toString();
				boolean variableMatched = false;
				Set<String> leftAttrSet = sideAttrMap.get("LEFT");
				String leftFirstAttr = leftAttrSet.iterator().next();
				String leftValue = null;
				for (Iterator k = passObj.keySet().iterator(); k.hasNext();) {
					String pubid = (String) k.next();
					Object tmp = passObj.get(pubid);
					if (tmp instanceof Publication) {
						// leftvalue is not in this pub, since this pub does not
						// have variable at all.
					} else {
						Map variableAttrMap = (Map) tmp;
						if (variableAttrMap.containsKey(variableName)) {
							Set attributeSet = (Set) variableAttrMap.get(variableName);
							if (attributeSet.contains(leftFirstAttr)) {
								// we do find it on the left side
								PublicationMessage pubMsg = reteNW.getRouter().getPublicationMessage(
										pubid);
								leftValue = pubMsg.getPublication().getPairMap().get(leftFirstAttr).toString();
								if (leftValue.equals(rightValue)) {
									variableMatched = true;
									break;
								}
							}
						}
					}
				}
				if (!variableMatched) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkRightMem(Map<String, Object> passObj, Map p,
			Map<String, Map<String, Set<String>>> variableMap) {
		for (Iterator<String> i = variableMap.keySet().iterator(); i.hasNext();) {
			String variableName = i.next();
			Map<String, Set<String>> tmpMap = variableMap.get(variableName);
			if (tmpMap.containsKey("RIGHT") && tmpMap.containsKey("LEFT")) {
				Set<String> rightAttrSet = tmpMap.get("RIGHT");
				String firstAttr = rightAttrSet.iterator().next();
				// passObj should have only one publication, the firstAttr must belong to this pub
				String rightPubID = passObj.keySet().iterator().next();
				PublicationMessage rightPubMsg = reteNW.getRouter().getPublicationMessage(
						rightPubID);
				String rightValue = rightPubMsg.getPublication().getPairMap().get(firstAttr).toString();
				boolean variableMatched = false;
				String leftValue = null;
				Set<String> leftAttrSet = tmpMap.get("LEFT");
				String tmpAttrName = leftAttrSet.iterator().next();
				for (Iterator k = p.keySet().iterator(); k.hasNext();) {
					String pubid = (String) k.next();
					Object tmp = p.get(pubid);
					if (tmp instanceof Publication) {
						// leftvalue is not in this pub, since this pub does not have variable at
						// all.
					} else {
						Map variableAttrMap = (Map) tmp;
						if (variableAttrMap.containsKey(variableName)) {
							Set attributeSet = (Set) variableAttrMap.get(variableName);
							if (attributeSet.contains(tmpAttrName)) {
								// we do find it on the left side
								PublicationMessage pubMsg = reteNW.getRouter().getPublicationMessage(
										pubid);
								leftValue = pubMsg.getPublication().getPairMap().get(tmpAttrName).toString();
								if (leftValue.equals(rightValue)) {
									variableMatched = true;
									break;
								}
							}
						}
					}
				}
				if (!variableMatched)
					return false;
			}
		}
		return true;
	}
}

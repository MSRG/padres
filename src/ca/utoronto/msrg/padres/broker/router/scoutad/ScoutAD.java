/*
 * Created on April 06, 2010
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scoutad;

/**
 * @author Chen
 *
 * Stores the subscriptions in a hierarchical tree where subscriptions higher
 * up the tree cover subscriptions below it.
 * 
 * Properties:  
 * - Intersecting subscriptions are 
 *   inserted at the same level as nodes that it intersects with.  
 * - Nodes that are covered by more than 2 nodes at a level are inserted 
 *   under those nodes.  Therefore a child node can have more than one parent
 */
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.UnadvertisementMessage;
import ca.utoronto.msrg.padres.common.util.datastructure.HashMapSet;

public class ScoutAD {

	// This is the head of the tree. It is not a real subscription
	private ScoutNodeAD headNode = new ScoutNodeAD();

	// For fast retrieval of a SCOUT node given the subscription id. Subscriptions with tid
	// attribute will have their sub Id appended with tid together to reference a scout node
	private HashMap<String, ScoutNodeAD> advIDNodeMap;

	// Since it is possible for two subscriptions with unique tid predicate to have exactly the same
	// subscription ID, we need to map a subscription Id to their "subscription id + tree id".
	private HashMapSet advIDToAdvIDtreeIDMap;

	// Maps a predicate map to a SCOUT node
	private HashMapSet advNodeMap;

	// Counts the number of relationship tests done (also the number of non-unique scout nodes
	// traversed). Just an optional benchmark thingy requested by Songlin.
	private int relationTestCount = 0;

	/**
	 * Constructor
	 * 
	 */
	public ScoutAD() {
		advIDNodeMap = new HashMap<String, ScoutNodeAD>();
		advIDToAdvIDtreeIDMap = new HashMapSet();
		advNodeMap = new HashMapSet();
	}

	/**
	 * Inserts a new subscription message as a new/existing node in the SCOUT tree
	 * 
	 * @param subMsg
	 */
	public int insert(AdvertisementMessage advMsg) {
		// reset this benchmark variable
		relationTestCount = 0;

		Advertisement adv = advMsg.getAdvertisement();
		String advID = advMsg.getMessageID();
		String rootAdvID = advID.split("_")[0];

		// String subIDtreeID = getSubIdTreeId(subID, sub.getPredicateMap());

		// Assume we never see a subscription twice because the matching engine should have already
		// filtered it.
		// ScoutNode newNode = new ScoutNode(sub.getPredicateMap(), subID);
		ScoutNodeAD newNodeAD = new ScoutNodeAD(adv.getPredicateMap(), rootAdvID);
		advIDNodeMap.put(advID, newNodeAD);
		// subIDToSubIDtreeIDMap.put(subID, subIDtreeID);
		advIDToAdvIDtreeIDMap.put(rootAdvID, advID);

		// insert it using recursive method
		recursiveInsert(newNodeAD, headNode,advID);

		// Now update our subNodeMap.
		// We need to get it from the hashmap again because an equal subscription could be
		// reinserted for use in recursiveInsert()
		// Map<String, Predicate> equivalentPredicateMap =
		// subIDNodeMap.get(subIDtreeID).predicateMap;
		// subNodeMap.put(equivalentPredicateMap.toString(), subIDNodeMap.get(subIDtreeID));
		Map<String, Predicate> equivalentPredicateMap = advIDNodeMap.get(advID).predicateMap;
		advNodeMap.put(equivalentPredicateMap.toString(), advIDNodeMap.get(advID));
		// Reuse old subscriptions with equal subscription space to reduce duplicates
		advMsg.getAdvertisement().setPredicateMap(equivalentPredicateMap);

		return relationTestCount;
	}

	/**
	 * 
	 * @param unsubMsg
	 * @return
	 */
	public Set<ScoutNodeAD> remove(UnadvertisementMessage unadvMsg) {
		return remove(unadvMsg.getUnadvertisement().getAdvID());
	}

	public Set<ScoutNodeAD> remove(String unadvID) {
		Set<Object> advIdTreeIdSet = advIDToAdvIDtreeIDMap.getSet(unadvID);
		Set<ScoutNodeAD> nodesRemovedSet = new HashSet<ScoutNodeAD>();
		for (Object obj : advIdTreeIdSet) {
			String advIdTreeId = obj.toString();
			if (advIDNodeMap.containsKey(advIdTreeId)) {
				ScoutNodeAD nodeAD = advIDNodeMap.remove(advIdTreeId);
				advNodeMap.getSet(nodeAD.predicateMap.toString()).remove(nodeAD);
				nodeAD.removeAdvertisementID(unadvID);

				// Remove the node if it does not represent other subscriptions
				if (nodeAD.advertiserCount() == 0) {
					removeNodeAD(nodeAD);
					advNodeMap.removeAll(nodeAD.predicateMap.toString());
				}

				nodesRemovedSet.add(nodeAD);
			}
		}

		advIDToAdvIDtreeIDMap.removeAll(unadvID);
		return nodesRemovedSet;
	}

	/*
	 * Remove the given node from the tree by updating its parents' and children's pointers.
	 * 
	 * Special case: If our parent leaves, inherit the parent's position only if none of the
	 * parent's neighbors have a superset over us
	 */
	private void removeNodeAD(ScoutNodeAD nodeAD) {
		for (ScoutNodeAD parent : nodeAD.parentSet) {
			parent.childSet.remove(nodeAD);
		}

		// Retain children nodes that do not intersect with any of this node's
		// same-level neighbors
		if (!nodeAD.childSet.isEmpty()) {
			for (ScoutNodeAD child : nodeAD.childSet) {
				child.parentSet.remove(nodeAD);
				// Inherit the parent's position only if
				// none of the parent's neighbors have a superset over us
				for (ScoutNodeAD newParent : nodeAD.parentSet) {
					Set<ScoutNodeAD> neighborSet = newParent.childSet;
					if (!child.isCovered(neighborSet)) {
						child.parentSet.add(newParent);
						newParent.childSet.add(child);
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return the set of nodes at the top of the scout tree
	 */
	public Set<ScoutNodeAD> coveringAdvertisementSet() {
		return headNode.childSet;
	}

	/**
	 * 
	 * @return the cloned set of nodes at the top of the scout tree
	 */
	public Set clonedCoveringAdvertisementSet() {
		return (Set) ((HashSet<ScoutNodeAD>) coveringAdvertisementSet()).clone();
	}

	/**
	 * 
	 * @param subID
	 * @return the set of scout children nodes of the given subscription id
	 */
	public Set<ScoutNodeAD> childSet(String advID) {
		return getNodeAD(advID).childSet;
	}

	/**
	 * Returns all the descendant nodes of the given scout node
	 * 
	 * @param subID
	 * @param resultSet
	 */
	public void descendantSet(String advID, Set<ScoutNodeAD> resultSet) {
		ScoutNodeAD nodeAD = getNodeAD(advID);

		getAllDescendants(nodeAD, resultSet);

		// getAllDescendants will add ourself to the set, so remove it
		resultSet.remove(nodeAD);
	}

	/*
	 * Do not directly call this function. This is used privately by descendentSet() and this
	 * function
	 */
	private void getAllDescendants(ScoutNodeAD nodeAD, Set<ScoutNodeAD> resultSet) {
		if (nodeAD.childSet.size() > 0) {
			for (ScoutNodeAD child : nodeAD.childSet) {
				getAllDescendants(child, resultSet);
			}
		}
		resultSet.add(nodeAD);
	}

	
	/*
	 * Returns the node that is inserted or the node that was updated
	 */
	private void recursiveInsert(ScoutNodeAD newNodeAD, ScoutNodeAD startNodeAD, String newAdvID) {
		// add by Mingwen
		// If newsubID equals the currentsubID in the startNode, the startNode
		// has been traversed
		// there is no need to recursive the current node.
		if (newAdvID.equals(startNodeAD.getCurrentAdvID())) {
			return;
		} else
			startNodeAD.currentAdvID = newAdvID;

		// New node is covered by existing node, and existing node has no children, so add new node
		// under existing node
		if (startNodeAD.childSet.size() == 0) {
			insertNodeADAt(newNodeAD, startNodeAD, null);
			return;
		}
	
		
		LinkedList<ScoutNodeAD> ourNewChildNodes = new LinkedList<ScoutNodeAD>();
		LinkedList<ScoutNodeAD> intersectNodes = new LinkedList<ScoutNodeAD>();
		boolean hasEmptyRelation = false;
		boolean hasSupersetRelation = false;
		boolean hasSubsetRelation = false;
		boolean hasIntersectRelation = false;
		for (ScoutNodeAD childNode : startNodeAD.childSet) {
			RelationAD relationAD = RelationIdentifierAD.getRelation(newNodeAD.predicateMap,
					childNode.predicateMap);
			relationTestCount++;
//			System.out.println("now judge:newNode"+newNode.predicateMap+"childNode"+childNode.predicateMap);
//			System.out.println(relation);

			if (relationAD == RelationAD.EMPTY) {
				hasEmptyRelation = true;
			} else if (relationAD == RelationAD.INTERSECT) {
				hasIntersectRelation = true;
				intersectNodes.add(childNode); // need this for the
				// INTERSECT/EMPTY case below
			} else if (relationAD == RelationAD.SUBSET) {
				recursiveInsert(newNodeAD, childNode,newAdvID);
				hasSubsetRelation = true;
			} else if (relationAD == RelationAD.SUPERSET) {
				ourNewChildNodes.add(childNode);
				hasSupersetRelation = true;
				// this can happen if subscription attributes are out of order
				// We should ditch the new node object and use the existing one
			} else if (relationAD == RelationAD.EQUAL) {
				String advID = newNodeAD.getAdvertisementIDs().iterator().next();
				String rootAdvID = advID.split("_")[0];
				// childNode.addSubscriptionID(subID);
				childNode.addAdvertisementID(rootAdvID);
				// String subIdTreeId = getSubIdTreeId(subID, childNode.predicateMap);
				// subIDNodeMap.put(subIdTreeId, childNode);
				advIDNodeMap.put(advID, childNode);

				if (hasSubsetRelation || hasSupersetRelation) {
					System.err.println("SCOUT has EQUAL relation along with "
							+ (hasSupersetRelation ? "SUPERSET " : "")
							+ ((hasSupersetRelation && hasSubsetRelation) ? "and " : "")
							+ (hasSubsetRelation ? "SUBSET " : ""));
					System.err.println("Happened while inserting " + newNodeAD + " with ids: "
							+ newNodeAD.getAdvertisementIDs());
					System.err.println("Superset over:\n" + ourNewChildNodes);
					System.err.flush();
					showTree();
					System.exit(0);
				}
				return;
			}
		}

		// Check for inconsistencies in the tree

		// After examining all child nodes, we should insert ourself between the parent (startNode)
		// and the set of nodes under ourNewChildNodes which our subscription covers, if there is
		// any SUBSET. if we are a subset of a child node, we should not be added at this level
		// anyway
		if (hasSubsetRelation) {
			// if (hasSupersetRelation) {
			// System.err.println("SCOUT insertion error!  Has subset and superset relation at same level!");
			// System.err.println("Happened while inserting " +
			// newNode.predicateMap);
			// showTree();
			// System.out.flush();
			// } else {
			// this case includes subset + empty/intersect combos, or subset of
			// all child nodes
			return;
			// }
			// INTERSECT/EMPTY. we have an intersect or empty relation with all
			// child nodes. We should insert ourselve
			// below the startNode and be neighbors with the existing child
			// nodes
		} else if (ourNewChildNodes.size() == 0 && (hasEmptyRelation || hasIntersectRelation)
				&& !hasSupersetRelation) {
			insertNodeADAt(newNodeAD, startNodeAD, null);

			// Find top-most level nodes that are descendants of nodes which
			// intersect us
			if (hasIntersectRelation) {
				LinkedList<ScoutNodeAD> extraChildNodeSet = findFirstCoveredChildren(newNodeAD, intersectNodes);
				while (!extraChildNodeSet.isEmpty()) {
					ScoutNodeAD extraChildNode = (ScoutNodeAD) extraChildNodeSet.iterator().next();
					extraChildNodeSet.remove(extraChildNode);
					// Only insert it as our child if our existing children
					// nodes do not
					// cover it
					if (!extraChildNode.isCovered(newNodeAD.childSet)
							&& !extraChildNode.isCovered(extraChildNodeSet)) {
						insertNodeADAt(newNodeAD, startNodeAD, extraChildNode);
					}
				}
			}
			// SUPERSET. we won't affect the intersecting/empty relation
			// children, just the ones we cover
		} else if (ourNewChildNodes.size() > 0) {
			for (ScoutNodeAD childNode : ourNewChildNodes) {
				insertNodeADAt(newNodeAD, startNodeAD, childNode);
			}

			// Find top-most level nodes that are descendants of nodes which
			// intersect us
			if (hasIntersectRelation) {
				LinkedList<ScoutNodeAD> extraChildNodeSet = findFirstCoveredChildren(newNodeAD, intersectNodes);
				while (!extraChildNodeSet.isEmpty()) {
					ScoutNodeAD extraChildNode = (ScoutNodeAD) extraChildNodeSet.iterator().next();
					extraChildNodeSet.remove(extraChildNode);
					// Only insert it as our child if our existing children
					// nodes do not
					// cover it
					if (!extraChildNode.isCovered(newNodeAD.childSet)
							&& !extraChildNode.isCovered(extraChildNodeSet)) {
						insertNodeADAt(newNodeAD, startNodeAD, extraChildNode);
					}
				}
			}
		} else {
			System.out.println("SCOUT: Weird case found: ourNewChildNodes size is "
					+ ourNewChildNodes.size() + " and hasEmptyRelation=" + hasEmptyRelation
					+ " and hasIntersectRelation=" + hasIntersectRelation
					+ " and hasSubsetRelation=" + hasSubsetRelation + " and hasSupersetRelation="
					+ hasSupersetRelation);
		}
	}

	/*
	 * Inserts the new node between the parent node and the childnode by updating the all three
	 * entities' parent/children pointers
	 */
	private void insertNodeADAt(ScoutNodeAD newNodeAD, ScoutNodeAD parentNodeAD, ScoutNodeAD childNodeAD) {
		parentNodeAD.childSet.add(newNodeAD);
		newNodeAD.parentSet.add(parentNodeAD);
		if (childNodeAD != null) {
			parentNodeAD.childSet.remove(childNodeAD);
			newNodeAD.childSet.add(childNodeAD);
			childNodeAD.parentSet.remove(parentNodeAD);
			childNodeAD.parentSet.add(newNodeAD);
		}
	}

	/*
	 * Finds the first childNode descendants under the given intersecting relation nodes
	 */
	private LinkedList<ScoutNodeAD> findFirstCoveredChildren(ScoutNodeAD newNodeAD,
			LinkedList<ScoutNodeAD> nodesToExamineSet) {
		String newAdvID = newNodeAD.getAdvertisementIDs().toString();
		LinkedList<ScoutNodeAD> childSet = new LinkedList<ScoutNodeAD>();
		while (!nodesToExamineSet.isEmpty()) {
			ScoutNodeAD parentNode = nodesToExamineSet.iterator().next();
			nodesToExamineSet.remove(parentNode);
			for (ScoutNodeAD childNodeAD : parentNode.childSet) {
				if (newAdvID.equals(childNodeAD.getAdvChildID())) {
					continue;
				} else
					{
					childNodeAD.advChildID = newAdvID;
					}
				RelationAD relationAD = RelationIdentifierAD.getRelation(newNodeAD.predicateMap,
						childNodeAD.predicateMap);
				relationTestCount++;
				if (relationAD == RelationAD.SUPERSET) {
					childSet.add(childNodeAD);
					mark(childNodeAD, newAdvID);

				} else if (relationAD == RelationAD.INTERSECT) {
					nodesToExamineSet.add(childNodeAD);
				}
				// its not possible to find another subscription here that
				// give us a SUBSET or EQUAL relation. For EMPTY relation
				// just ignore that child and its children
			}
		}
		return childSet;
	}

	void mark(ScoutNodeAD ParentNodeAD, String newAdvID) {
		LinkedList<ScoutNodeAD> nodesToMarklist = new LinkedList<ScoutNodeAD>();
		nodesToMarklist.add(ParentNodeAD);
		while (!nodesToMarklist.isEmpty()) {
			ScoutNodeAD parentNode = nodesToMarklist.removeFirst();
			for (ScoutNodeAD childNode : parentNode.childSet) {
				if (newAdvID.equals(childNode.getAdvChildID())) {
					continue;
				} else {
					childNode.advChildID = newAdvID;
					nodesToMarklist.add(childNode);
				}
			}
		}
	}
	/**
	 * Displays all nodes in the scout tree. You'll have to look at the amount of indenting to see
	 * at what level is the node in
	 * 
	 */
	public void showTree() {
		System.out.println("HEAD:");
		if (headNode.childSet.size() > 0) {
			for (ScoutNodeAD child : headNode.childSet) {
				printTree(child, 1);
			}
		}
	}

	private void printTree(ScoutNodeAD node, int level) {
		// Indentation indicates the level of this subscription
		for (int i = 0; i < level; i++)
			System.out.print("\t");

		// display the node itself. putting the predicates into a subscription
		// removes the '=' inherent in Map's toString() built-in method
		Advertisement adv = new Advertisement();
		adv.setPredicateMap(node.predicateMap);
		System.out.println(adv + " " + node.getAdvertisementIDs().toString());

		// display the node's children
		if (node.childSet.size() > 0) {
			level++;
			for (ScoutNodeAD child : node.childSet) {
				printTree(child, level);
			}
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Advertisement adv = new Advertisement();
		if (headNode.childSet.size() > 0) {
			for (ScoutNodeAD child : headNode.childSet) {
				getStringRep(child, 1, buf, adv);
			}
		}

		adv = null;
		return buf.toString();
	}

	private void getStringRep(ScoutNodeAD node, int level, StringBuffer buf, Advertisement adv) {
		// Indentation indicates the level of this subscription
		for (int i = 0; i < level; i++)
			buf.append("\t");

		// display the node itself. putting the predicates into a subscription
		// removes the '=' inherent in Map's toString() built-in method
		adv.setPredicateMap(node.predicateMap);
		buf.append(adv.toString() + " " + node.getAdvertisementIDs().toString() + "\n");

		// display the node's children
		if (node.childSet.size() > 0) {
			level++;
			for (ScoutNodeAD child : node.childSet) {
				getStringRep(child, level, buf, adv);
			}
		}
	}

	/**
	 * Returns a set of unique subscription predicate maps where no two subscriptions have equal
	 * subscription space
	 * 
	 * @return
	 */
	public Set<Object> predicateSet() {
		return advNodeMap.keySet();
	}

	/**
	 * This function only returns one of the subscription nodes if there are multiple subscriptions
	 * with same subID but different tid.
	 * 
	 * @param subID
	 * @return
	 */
	public ScoutNodeAD getNodeAD(String advID) {
		// if this subscription has a tree id thingy, then look up the first
		// subscription and return its child set. All child sets of other
		// equal subscriptions (with different tid) should have the same child
		// set.
		if (!advIDNodeMap.containsKey(advID)) {
			// Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getClonedSet(subID);
			Set<Object> advIdTreeIdSet = advIDToAdvIDtreeIDMap.getSet(advID);
			if (advIdTreeIdSet.isEmpty())
				return null;

			String firstAdvIdTreeId = advIdTreeIdSet.iterator().next().toString();
			// subIdTreeIdSet.clear();
			// subIdTreeIdSet = null;

			if (!advIDNodeMap.containsKey(firstAdvIdTreeId))
				return null;

			return advIDNodeMap.get(firstAdvIdTreeId);

			// otherwise, its either a regular subId or a subIdTreeId
		} else {
			return advIDNodeMap.get(advID);
		}
	}

	/**
	 * With cycles ON, it is possible to have multiple subscriptions with the same subID and their
	 * only difference is their tid in the predicate map.
	 * 
	 * @param subID
	 * @return
	 */
	public Set<ScoutNodeAD> getNodes(String advID) {
		HashSet<ScoutNodeAD> nodeSet = new HashSet<ScoutNodeAD>();
		// if this subscription has a tree id thingy, and the subId does not have the treeId part,
		// then look it up
		if (!advIDNodeMap.containsKey(advID)) {
			// Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getClonedSet(subID);
			Set<Object> advIdTreeIdSet = advIDToAdvIDtreeIDMap.getSet(advID);
			if (advIdTreeIdSet.isEmpty())
				return null;
			for (Object obj : advIdTreeIdSet) {
				String advIdTreeId = obj.toString();
				nodeSet.add(advIDNodeMap.get(advIdTreeId));
			}
			// subIdTreeIdSet.clear();
			// subIdTreeIdSet = null;

			// If the input is the regular subId or is actually a subIdTreeId
		} else {
			nodeSet.add(advIDNodeMap.get(advID));
		}

		return nodeSet;
	}

	public Set<ScoutNodeAD> nodeSet() {
		return new HashSet<ScoutNodeAD>(advIDNodeMap.values());
	}

	public int size() {
		return advIDNodeMap.size();
	}

	public boolean isEmpty() {
		return advIDNodeMap.isEmpty();
	}

	/**
	 * <code>
	public String getSubIdTreeId(String subId, Map<String, Predicate> predicateMap) {
		String treeId = predicateMap.containsKey("tid") ? predicateMap.get("tid").getValue().toString()
				: null;
		String subIdTreeId = (treeId == null) ? subId : subId + "_" + treeId;

		return subIdTreeId;
	}
	</code>
	 */
}

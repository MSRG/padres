/*
 * Created on Mar 10, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scout;

/**
 * @author alex
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

import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.UnsubscriptionMessage;
import ca.utoronto.msrg.padres.common.util.datastructure.HashMapSet;

public class Scout {

	// This is the head of the tree. It is not a real subscription
	private ScoutNode headNode = new ScoutNode();

	// For fast retrieval of a SCOUT node given the subscription id. Subscriptions with tid
	// attribute will have their sub Id appended with tid together to reference a scout node
	private HashMap<String, ScoutNode> subIDNodeMap;

	// Since it is possible for two subscriptions with unique tid predicate to have exactly the same
	// subscription ID, we need to map a subscription Id to their "subscription id + tree id".
	private HashMapSet subIDToSubIDtreeIDMap;

	// Maps a predicate map to a SCOUT node
	private HashMapSet subNodeMap;

	// Counts the number of relationship tests done (also the number of non-unique scout nodes
	// traversed). Just an optional benchmark thingy requested by Songlin.
	private int relationTestCount = 0;

	/**
	 * Constructor
	 * 
	 */
	public Scout() {
		subIDNodeMap = new HashMap<String, ScoutNode>();
		subIDToSubIDtreeIDMap = new HashMapSet();
		subNodeMap = new HashMapSet();
	}

	/**
	 * Inserts a new subscription message as a new/existing node in the SCOUT tree
	 * 
	 * @param subMsg
	 */
	public int insert(SubscriptionMessage subMsg) {
		// reset this benchmark variable
		relationTestCount = 0;

		Subscription sub = subMsg.getSubscription();
		String subID = subMsg.getMessageID();
		String rootSubID = subID.split("_")[0];

		// String subIDtreeID = getSubIdTreeId(subID, sub.getPredicateMap());

		// Assume we never see a subscription twice because the matching engine should have already
		// filtered it.
		// ScoutNode newNode = new ScoutNode(sub.getPredicateMap(), subID);
		ScoutNode newNode = new ScoutNode(sub.getPredicateMap(), rootSubID);
		subIDNodeMap.put(subID, newNode);
		// subIDToSubIDtreeIDMap.put(subID, subIDtreeID);
		subIDToSubIDtreeIDMap.put(rootSubID, subID);

		// insert it using recursive method
		recursiveInsert(newNode, headNode,subID);

		// Now update our subNodeMap.
		// We need to get it from the hashmap again because an equal subscription could be
		// reinserted for use in recursiveInsert()
		// Map<String, Predicate> equivalentPredicateMap =
		// subIDNodeMap.get(subIDtreeID).predicateMap;
		// subNodeMap.put(equivalentPredicateMap.toString(), subIDNodeMap.get(subIDtreeID));
		Map<String, Predicate> equivalentPredicateMap = subIDNodeMap.get(subID).predicateMap;
		subNodeMap.put(equivalentPredicateMap.toString(), subIDNodeMap.get(subID));
		// Reuse old subscriptions with equal subscription space to reduce duplicates
		subMsg.getSubscription().setPredicateMap(equivalentPredicateMap);

		return relationTestCount;
	}

	/**
	 * 
	 * @param unsubMsg
	 * @return
	 */
	public Set<ScoutNode> remove(UnsubscriptionMessage unsubMsg) {
		return remove(unsubMsg.getUnsubscription().getSubID());
	}

	public Set<ScoutNode> remove(String unsubID) {
		
		Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getSet(unsubID);
		Set<ScoutNode> nodesRemovedSet = new HashSet<ScoutNode>();
		for (Object obj : subIdTreeIdSet) {
			String subIdTreeId = obj.toString();
			if (subIDNodeMap.containsKey(subIdTreeId)) {
				ScoutNode node = subIDNodeMap.remove(subIdTreeId);
				subNodeMap.getSet(node.predicateMap.toString()).remove(node);
				node.removeSubscriptionID(unsubID);

				// Remove the node if it does not represent other subscriptions
				if (node.subscriberCount() == 0) {
					removeNode(node);
					subNodeMap.removeAll(node.predicateMap.toString());
				}

				nodesRemovedSet.add(node);
			}
		}

		subIDToSubIDtreeIDMap.removeAll(unsubID);
		return nodesRemovedSet;
	}

	/*
	 * Remove the given node from the tree by updating its parents' and children's pointers.
	 * 
	 * Special case: If our parent leaves, inherit the parent's position only if none of the
	 * parent's neighbors have a superset over us
	 */
	private void removeNode(ScoutNode node) {
		for (ScoutNode parent : node.parentSet) {
			parent.childSet.remove(node);
		}

		// Retain children nodes that do not intersect with any of this node's
		// same-level neighbors
		if (!node.childSet.isEmpty()) {
			for (ScoutNode child : node.childSet) {
				child.parentSet.remove(node);
				/*add by cmw for simplefied covering tree. we need to re-insert children */
				// reinsert the child to scout tree
				if(child.parentSet.size()==0){
					for (ScoutNode parent : node.parentSet) {
						recursiveInsert(child, parent, "u"+child.getNextSubscriptionID()) ;
					}
				}
				else
					{
					//do nothing 
					}
				/*end */
				
			}
		}
	}

	/**
	 * 
	 * @return the set of nodes at the top of the scout tree
	 */
	public Set<ScoutNode> coveringSubscriptionSet() {
		return headNode.childSet;
	}

	/**
	 * 
	 * @return the cloned set of nodes at the top of the scout tree
	 */
	public Set clonedCoveringSubscriptionSet() {
		return (Set) ((HashSet<ScoutNode>) coveringSubscriptionSet()).clone();
	}

	/**
	 * 
	 * @param subID
	 * @return the set of scout children nodes of the given subscription id
	 */
	public Set<ScoutNode> childSet(String subID) {
		return getNode(subID).childSet;
	}

	/**
	 * Returns all the descendant nodes of the given scout node
	 * 
	 * @param subID
	 * @param resultSet
	 */
	public void descendantSet(String subID, Set<ScoutNode> resultSet) {
		ScoutNode node = getNode(subID);

		getAllDescendants(node, resultSet);

		// getAllDescendants will add ourself to the set, so remove it
		resultSet.remove(node);
	}

	/*
	 * Do not directly call this function. This is used privately by descendentSet() and this
	 * function
	 */
	private void getAllDescendants(ScoutNode node, Set<ScoutNode> resultSet) {
		if (node.childSet.size() > 0) {
			for (ScoutNode child : node.childSet) {
				getAllDescendants(child, resultSet);
			}
		}
		resultSet.add(node);
	}
	
	
	/**@author cmw
	 * simplified covering 
	 */
	private void recursiveInsert(ScoutNode newNode, ScoutNode startNode,String newsubID) {

		LinkedList<ScoutNode> newChildrenList =new LinkedList<ScoutNode>();
		if (newsubID.equals(startNode.getCurrentSubID())) {
			return;
		} else
			startNode.currentSubID = newsubID;
		// New node is covered by existing node, and existing node has no
		// children, so add new node
		// under existing node
		if (startNode.childSet.size() == 0) {
			insertNodeAt(newNode, startNode, null);
			return;
		}
		boolean hasEmptyRelation = true;
		boolean hasChild = false;
		for (ScoutNode childNode : startNode.childSet) {
			Relation relation = RelationIdentifier.getRelation(
					newNode.predicateMap, childNode.predicateMap);
			 if (relation == Relation.SUBSET) {
				hasEmptyRelation = false;
				recursiveInsert(newNode, childNode,newsubID);
				return;
			} else if (relation == Relation.SUPERSET) {
				hasEmptyRelation = false;
				hasChild = true;
				newChildrenList.add(childNode);
			} else if (relation == Relation.EQUAL) {
				hasEmptyRelation = false;
				hasChild = false;
				String subID = newNode.getSubscriptionIDs().iterator().next();
				String rootSubID = subID.split("_")[0];
				// childNode.addSubscriptionID(subID);
				childNode.addSubscriptionID(rootSubID);
				// String subIdTreeId = getSubIdTreeId(subID,
				// childNode.predicateMap);
				// subIDNodeMap.put(subIdTreeId, childNode);
				subIDNodeMap.put(subID, childNode);
				return;
			}
		}

		if (hasEmptyRelation) {
			insertNodeAt(newNode, startNode, null);
			return;
		}
		if (hasChild) {
			for (ScoutNode newChildren : newChildrenList){
				insertNodeAt(newNode, startNode, newChildren);
			}	
			return;
		}
	
	}
	
	
	/*
	 * Returns the node that is inserted or the node that was updated
	 */
//	private void recursiveInsert(ScoutNode newNode, ScoutNode startNode) {
//		// New node is covered by existing node, and existing node has no children, so add new node
//		// under existing node
//		if (startNode.childSet.size() == 0) {
//			insertNodeAt(newNode, startNode, null);
//			return;
//		}
//
//		Set<ScoutNode> ourNewChildNodes = new HashSet<ScoutNode>();
//		Set<ScoutNode> intersectNodes = new HashSet<ScoutNode>();
//		boolean hasEmptyRelation = false;
//		boolean hasSupersetRelation = false;
//		boolean hasSubsetRelation = false;
//		boolean hasIntersectRelation = false;
//		for (ScoutNode childNode : startNode.childSet) {
//			Relation relation = RelationIdentifier.getRelation(newNode.predicateMap,
//					childNode.predicateMap);
//			relationTestCount++;
//
//			if (relation == Relation.EMPTY) {
//				hasEmptyRelation = true;
//			} else if (relation == Relation.INTERSECT) {
//				hasIntersectRelation = true;
//				intersectNodes.add(childNode); // need this for the
//				// INTERSECT/EMPTY case below
//			} else if (relation == Relation.SUBSET) {
//				recursiveInsert(newNode, childNode);
//				hasSubsetRelation = true;
//			} else if (relation == Relation.SUPERSET) {
//				ourNewChildNodes.add(childNode);
//				hasSupersetRelation = true;
//				// this can happen if subscription attributes are out of order
//				// We should ditch the new node object and use the existing one
//			} else if (relation == Relation.EQUAL) {
//				String subID = newNode.getSubscriptionIDs().iterator().next();
//				String rootSubID = subID.split("_")[0];
//				// childNode.addSubscriptionID(subID);
//				childNode.addSubscriptionID(rootSubID);
//				// String subIdTreeId = getSubIdTreeId(subID, childNode.predicateMap);
//				// subIDNodeMap.put(subIdTreeId, childNode);
//				subIDNodeMap.put(subID, childNode);
//
//				if (hasSubsetRelation || hasSupersetRelation) {
//					System.err.println("SCOUT has EQUAL relation along with "
//							+ (hasSupersetRelation ? "SUPERSET " : "")
//							+ ((hasSupersetRelation && hasSubsetRelation) ? "and " : "")
//							+ (hasSubsetRelation ? "SUBSET " : ""));
//					System.err.println("Happened while inserting " + newNode + " with ids: "
//							+ newNode.getSubscriptionIDs());
//					System.err.println("Superset over:\n" + ourNewChildNodes);
//					System.err.flush();
//					showTree();
//					System.exit(0);
//				}
//				return;
//			}
//		}
//
//		// Check for inconsistencies in the tree
//
//		// After examining all child nodes, we should insert ourself between the parent (startNode)
//		// and the set of nodes under ourNewChildNodes which our subscription covers, if there is
//		// any SUBSET. if we are a subset of a child node, we should not be added at this level
//		// anyway
//		if (hasSubsetRelation) {
//			// if (hasSupersetRelation) {
//			// System.err.println("SCOUT insertion error!  Has subset and superset relation at same level!");
//			// System.err.println("Happened while inserting " +
//			// newNode.predicateMap);
//			// showTree();
//			// System.out.flush();
//			// } else {
//			// this case includes subset + empty/intersect combos, or subset of
//			// all child nodes
//			return;
//			// }
//			// INTERSECT/EMPTY. we have an intersect or empty relation with all
//			// child nodes. We should insert ourselve
//			// below the startNode and be neighbors with the existing child
//			// nodes
//		} else if (ourNewChildNodes.size() == 0 && (hasEmptyRelation || hasIntersectRelation)
//				&& !hasSupersetRelation) {
//			insertNodeAt(newNode, startNode, null);
//
//			// Find top-most level nodes that are descendants of nodes which
//			// intersect us
//			if (hasIntersectRelation) {
//				Set<ScoutNode> extraChildNodeSet = findFirstCoveredChildren(newNode, intersectNodes);
//				while (!extraChildNodeSet.isEmpty()) {
//					ScoutNode extraChildNode = (ScoutNode) extraChildNodeSet.iterator().next();
//					extraChildNodeSet.remove(extraChildNode);
//					// Only insert it as our child if our existing children
//					// nodes do not
//					// cover it
//					if (!extraChildNode.isCovered(newNode.childSet)
//							&& !extraChildNode.isCovered(extraChildNodeSet)) {
//						insertNodeAt(newNode, startNode, extraChildNode);
//					}
//				}
//			}
//			// SUPERSET. we won't affect the intersecting/empty relation
//			// children, just the ones we cover
//		} else if (ourNewChildNodes.size() > 0) {
//			for (ScoutNode childNode : ourNewChildNodes) {
//				insertNodeAt(newNode, startNode, childNode);
//			}
//
//			// Find top-most level nodes that are descendants of nodes which
//			// intersect us
//			if (hasIntersectRelation) {
//				Set<ScoutNode> extraChildNodeSet = findFirstCoveredChildren(newNode, intersectNodes);
//				while (!extraChildNodeSet.isEmpty()) {
//					ScoutNode extraChildNode = (ScoutNode) extraChildNodeSet.iterator().next();
//					extraChildNodeSet.remove(extraChildNode);
//					// Only insert it as our child if our existing children
//					// nodes do not
//					// cover it
//					if (!extraChildNode.isCovered(newNode.childSet)
//							&& !extraChildNode.isCovered(extraChildNodeSet)) {
//						insertNodeAt(newNode, startNode, extraChildNode);
//					}
//				}
//			}
//		} else {
//			System.out.println("SCOUT: Weird case found: ourNewChildNodes size is "
//					+ ourNewChildNodes.size() + " and hasEmptyRelation=" + hasEmptyRelation
//					+ " and hasIntersectRelation=" + hasIntersectRelation
//					+ " and hasSubsetRelation=" + hasSubsetRelation + " and hasSupersetRelation="
//					+ hasSupersetRelation);
//		}
//	}

	/*
	 * Inserts the new node between the parent node and the childnode by updating the all three
	 * entities' parent/children pointers
	 */
	private void insertNodeAt(ScoutNode newNode, ScoutNode parentNode, ScoutNode childNode) {
		parentNode.childSet.add(newNode);
		newNode.parentSet.add(parentNode);
		if (childNode != null) {
			parentNode.childSet.remove(childNode);
			newNode.childSet.add(childNode);
			childNode.parentSet.remove(parentNode);
			childNode.parentSet.add(newNode);
		}
	}

	/*
	 * Finds the first childNode descendants under the given intersecting relation nodes
	 */
	private Set<ScoutNode> findFirstCoveredChildren(ScoutNode newNode,
			Set<ScoutNode> nodesToExamineSet) {
		HashSet<ScoutNode> childSet = new HashSet<ScoutNode>();
		while (!nodesToExamineSet.isEmpty()) {
			ScoutNode parentNode = nodesToExamineSet.iterator().next();
			nodesToExamineSet.remove(parentNode);
			for (ScoutNode childNode : parentNode.childSet) {
				Relation relation = RelationIdentifier.getRelation(newNode.predicateMap,
						childNode.predicateMap);
				relationTestCount++;
				if (relation == Relation.SUPERSET) {
					childSet.add(childNode);
				} else if (relation == Relation.INTERSECT) {
					nodesToExamineSet.add(childNode);
				}
				// its not possible to find another subscription here that
				// give us a SUBSET or EQUAL relation. For EMPTY relation
				// just ignore that child and its children
			}
		}
		return childSet;
	}

	/**
	 * Displays all nodes in the scout tree. You'll have to look at the amount of indenting to see
	 * at what level is the node in
	 * 
	 */
	public void showTree() {
		System.out.println("HEAD:");
		if (headNode.childSet.size() > 0) {
			for (ScoutNode child : headNode.childSet) {
				printTree(child, 1);
			}
		}
	}

	private void printTree(ScoutNode node, int level) {
		// Indentation indicates the level of this subscription
		for (int i = 0; i < level; i++)
			System.out.print("\t");

		// display the node itself. putting the predicates into a subscription
		// removes the '=' inherent in Map's toString() built-in method
		Subscription sub = new Subscription();
		sub.setPredicateMap(node.predicateMap);
		System.out.println(sub + " " + node.getSubscriptionIDs().toString());

		// display the node's children
		if (node.childSet.size() > 0) {
			level++;
			for (ScoutNode child : node.childSet) {
				printTree(child, level);
			}
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		Subscription sub = new Subscription();
		if (headNode.childSet.size() > 0) {
			for (ScoutNode child : headNode.childSet) {
				getStringRep(child, 1, buf, sub);
			}
		}

		sub = null;
		return buf.toString();
	}

	private void getStringRep(ScoutNode node, int level, StringBuffer buf, Subscription sub) {
		// Indentation indicates the level of this subscription
		for (int i = 0; i < level; i++)
			buf.append("\t");

		// display the node itself. putting the predicates into a subscription
		// removes the '=' inherent in Map's toString() built-in method
		sub.setPredicateMap(node.predicateMap);
		buf.append(sub.toString() + " " + node.getSubscriptionIDs().toString() + "\n");

		// display the node's children
		if (node.childSet.size() > 0) {
			level++;
			for (ScoutNode child : node.childSet) {
				getStringRep(child, level, buf, sub);
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
		return subNodeMap.keySet();
	}

	/**
	 * This function only returns one of the subscription nodes if there are multiple subscriptions
	 * with same subID but different tid.
	 * 
	 * @param subID
	 * @return
	 */
	public ScoutNode getNode(String subID) {
		// if this subscription has a tree id thingy, then look up the first
		// subscription and return its child set. All child sets of other
		// equal subscriptions (with different tid) should have the same child
		// set.
		if (!subIDNodeMap.containsKey(subID)) {
			// Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getClonedSet(subID);
			Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getSet(subID);
			if (subIdTreeIdSet.isEmpty())
				return null;

			String firstSubIdTreeId = subIdTreeIdSet.iterator().next().toString();
			// subIdTreeIdSet.clear();
			// subIdTreeIdSet = null;

			if (!subIDNodeMap.containsKey(firstSubIdTreeId))
				return null;

			return subIDNodeMap.get(firstSubIdTreeId);

			// otherwise, its either a regular subId or a subIdTreeId
		} else {
			return subIDNodeMap.get(subID);
		}
	}

	/**
	 * With cycles ON, it is possible to have multiple subscriptions with the same subID and their
	 * only difference is their tid in the predicate map.
	 * 
	 * @param subID
	 * @return
	 */
	public Set<ScoutNode> getNodes(String subID) {
		HashSet<ScoutNode> nodeSet = new HashSet<ScoutNode>();
		// if this subscription has a tree id thingy, and the subId does not have the treeId part,
		// then look it up
		if (!subIDNodeMap.containsKey(subID)) {
			// Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getClonedSet(subID);
			Set<Object> subIdTreeIdSet = subIDToSubIDtreeIDMap.getSet(subID);
			if (subIdTreeIdSet.isEmpty())
				return null;
			for (Object obj : subIdTreeIdSet) {
				String subIdTreeId = obj.toString();
				nodeSet.add(subIDNodeMap.get(subIdTreeId));
			}
			// subIdTreeIdSet.clear();
			// subIdTreeIdSet = null;

			// If the input is the regular subId or is actually a subIdTreeId
		} else {
			nodeSet.add(subIDNodeMap.get(subID));
		}

		return nodeSet;
	}

	public Set<ScoutNode> nodeSet() {
		return new HashSet<ScoutNode>(subIDNodeMap.values());
	}

	public int size() {
		return subIDNodeMap.size();
	}

	public boolean isEmpty() {
		return subIDNodeMap.isEmpty();
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

/*
 * Created on Mar 10, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scout;

/**
 * @author Alex
 *
 * This represents a particular node on a SCOUT tree
 */
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Subscription;

public class ScoutNode implements Serializable {

	private static final long serialVersionUID = 6510527527004402838L;

	// Holds the predicate representing the subscription
	public Map<String, Predicate> predicateMap;

	// Counts the total number of subscribers represented by this scout node
	private LinkedHashSet<String> subscriptionIDs;

	// parent of this node.
	public transient HashSet<ScoutNode> parentSet;

	// set of children SCOUT nodes of this node
	public transient HashSet<ScoutNode> childSet;

	// not related to SCOUT. Used by SubscriptionFilter to mark which sub was sent
	public transient boolean flag;
	
	public String currentSubID;
	

	public ScoutNode() {
		parentSet = new HashSet<ScoutNode>(1);
		childSet = new HashSet<ScoutNode>();
		predicateMap = new HashMap<String, Predicate>(2);
		predicateMap.put("class", new Predicate("eq", "SCOUT_ROOT_NODE"));
		subscriptionIDs = null;
		flag = false;
		currentSubID=null;
	}

	/**
	 * Constructor
	 * 
	 * @param sub
	 * @param parent
	 * @param child
	 */
	public ScoutNode(Map<String, Predicate> predMap, String subID) {
		parentSet = new HashSet<ScoutNode>();
		childSet = new HashSet<ScoutNode>();
		predicateMap = predMap;
		subscriptionIDs = new LinkedHashSet<String>();
		subscriptionIDs.add(subID);
		flag = false;
		currentSubID=null;
	}

	public int subscriberCount() {
		return subscriptionIDs.size();
	}

	public void addSubscriptionID(String subID) {
		subscriptionIDs.add(subID);
	}

	public void removeSubscriptionID(String subID) {
		subscriptionIDs.remove(subID);
	}

	public LinkedHashSet<String> getSubscriptionIDs() {
		return subscriptionIDs;
	}

	public String getNextSubscriptionID() {
		return subscriptionIDs.iterator().next().toString();
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String toString() {
		Subscription sub = new Subscription();
		sub.setPredicateMap(predicateMap);
		String buf = sub.toString();
		sub = null;

		return buf;
	}

	/**
	 * Tests if this node is covered by any one of the nodes in the given set of
	 * Scout nodes
	 * 
	 * @param scoutNodeSet
	 * @return
	 */
	public boolean isCovered(Set<ScoutNode> scoutNodeSet) {
		Relation relation = Relation.EQUAL;
		for (ScoutNode node : scoutNodeSet) {
			Map<String, Predicate> otherPredicateMap = node.predicateMap;
			relation = RelationIdentifier.getRelation(predicateMap, otherPredicateMap);
			if (relation == Relation.EQUAL || relation == Relation.SUBSET)
				return true;
		}

		return false;
	}
	public String getCurrentSubID() {
		return currentSubID;
	}
	
	
}
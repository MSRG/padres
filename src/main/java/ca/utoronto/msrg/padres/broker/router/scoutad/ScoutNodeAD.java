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
 * This represents a particular node on a SCOUT tree
 */
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Advertisement;

public class ScoutNodeAD implements Serializable {

	private static final long serialVersionUID = 6510527527004402838L;

	// Holds the predicate representing the adv
	public Map<String, Predicate> predicateMap;

	// Counts the total number of advertisers represented by this scout node
	private LinkedHashSet<String> advertisementIDs;

	// parent of this node.
	public transient HashSet<ScoutNodeAD> parentSet;

	// set of children SCOUT nodes of this node
	public transient HashSet<ScoutNodeAD> childSet;

	// not related to SCOUT. Used by SubscriptionFilter to mark which sub was sent
	public transient boolean flag;
	// add by Mingwen
	// currentSubID is a flag to avoid reiteration while inserting node;
	public String currentAdvID;
	
	// add by Mingwen
	// SubchildID is another flag to avoid reiteration while nodes have INTERSECT relation;
	public String advChildID;

	public ScoutNodeAD() {
		parentSet = new HashSet<ScoutNodeAD>(1);
		childSet = new HashSet<ScoutNodeAD>();
		predicateMap = new HashMap<String, Predicate>(2);
		predicateMap.put("class", new Predicate("eq", "SCOUT_ROOT_NODE"));
		advertisementIDs = null;
		flag = false;
		currentAdvID=null;
	}

	/**
	 * Constructor
	 * 
	 * @param sub
	 * @param parent
	 * @param child
	 */
	public ScoutNodeAD(Map<String, Predicate> predMap, String advID) {
		parentSet = new HashSet<ScoutNodeAD>();
		childSet = new HashSet<ScoutNodeAD>();
		predicateMap = predMap;
		advertisementIDs = new LinkedHashSet<String>();
		advertisementIDs.add(advID);
		flag = false;
		currentAdvID=null;
	}

	public int advertiserCount() {
		return advertisementIDs.size();
	}

	public void addAdvertisementID(String advID) {
		advertisementIDs.add(advID);
	}

	public void removeAdvertisementID(String advID) {
		advertisementIDs.remove(advID);
	}

	public LinkedHashSet<String> getAdvertisementIDs() {
		return advertisementIDs;
	}

	public String getNextAdvertisementID() {
		return advertisementIDs.iterator().next().toString();
	}

	public int hashCode() {
		return toString().hashCode();
	}

	public String toString() {
		Advertisement adv = new Advertisement();
		adv.setPredicateMap(predicateMap);
		String buf = adv.toString();
		adv = null;

		return buf;
	}

	public String getCurrentAdvID() {
		return currentAdvID;
	}
	
	public String getAdvChildID() {
		return advChildID;
	}
	
	/**
	 * Tests if this node is covered by any one of the nodes in the given set of
	 * Scout nodes
	 * 
	 * @param scoutNodeSet
	 * @return
	 */
	public boolean isCovered(Set<ScoutNodeAD> scoutNodeADSet) {
		RelationAD relationAD = RelationAD.EQUAL;
		for (ScoutNodeAD nodeAD : scoutNodeADSet) {
			Map<String, Predicate> otherPredicateMap = nodeAD.predicateMap;
			relationAD = RelationIdentifierAD.getRelationAD(predicateMap, otherPredicateMap);
			if (relationAD == RelationAD.EQUAL || relationAD == RelationAD.SUBSET)
				return true;
		}

		return false;
	}
	public boolean isCovered(LinkedList<ScoutNodeAD> scoutNodeADSet) {
		RelationAD relationAD = RelationAD.UNCERTAIN;
		for (ScoutNodeAD nodeAD : scoutNodeADSet) {
			Map<String, Predicate> otherPredicateMap = nodeAD.predicateMap;
			relationAD = RelationIdentifierAD.getRelationAD(predicateMap, otherPredicateMap);
			if (relationAD == RelationAD.EQUAL || relationAD == RelationAD.SUBSET) {
				return true;
			}
		}

		return false;
	}
}
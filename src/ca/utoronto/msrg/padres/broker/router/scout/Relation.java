/*
 * Created on Mar 7, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scout;

/**
 * @author alex
 *
 */
public class Relation {
	private String relation;
	
	public static final Relation EQUAL = new Relation("equal");
	public static final Relation SUPERSET = new Relation("superset");	// same as cover
	public static final Relation SUBSET = new Relation("subset");
	public static final Relation INTERSECT = new Relation("intersect");
	public static final Relation EMPTY = new Relation("empty");
	
	
	private Relation(String relationStr) {
		relation = relationStr;
	}
	
	public Relation AND(Relation newRelation) {
		// Regardless of any new condition, EQUAL is the weakest and can be
		// overwritten by any other relation
		if (this == EQUAL) {
			return newRelation;
			
		// SUPERSET cannot go back to EQUAL nor be SUBSET
		} else if (this == SUPERSET) {
			if (newRelation == EQUAL || newRelation == SUPERSET)
				return this;	// no change
			else if (newRelation == SUBSET || newRelation == INTERSECT)
				return INTERSECT;
			else if (newRelation == EMPTY)
				return newRelation;
		
		// SUBSET is like SUPERSET
		} else if (this == SUBSET) {
			if (newRelation == EQUAL || newRelation == SUBSET) 
				return this;	// no change
			else if (newRelation == SUPERSET || newRelation == INTERSECT)
				return INTERSECT;
			else if (newRelation == EMPTY)
				return newRelation;
		
		// INTERSECT can only become EMPTY or UNKNOWN
		} else if (this == INTERSECT) {
			if (newRelation == EQUAL || newRelation == SUBSET || 
				newRelation == SUPERSET || newRelation == INTERSECT)
				return INTERSECT;
			else if (newRelation == EMPTY)
				return newRelation;
		
		} 
		// EMPTY set is the king.  No one can over-rule it
		return EMPTY;	
	}
	
	public String toString() {
		return relation;
	}
	
}

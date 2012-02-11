/*
 * Created on April 6, 2010
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.broker.router.scoutad;

/**
 * @author Chen 
 *
 */
public class RelationAD {
	private String relation;
	
	public static final RelationAD EQUAL = new RelationAD("equal");
	public static final RelationAD SUPERSET = new RelationAD("superset");	// same as cover
	public static final RelationAD SUBSET = new RelationAD("subset");
	public static final RelationAD INTERSECT = new RelationAD("intersect");
	public static final RelationAD EMPTY = new RelationAD("empty");
	
	public static final RelationAD UNCERTAIN = new RelationAD("uncertain");
	
	private RelationAD(String relationStr) {
		relation = relationStr;
	}
	
	public RelationAD AND(RelationAD newRelation) {
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
	
	public RelationAD OR(RelationAD newRelation){
		
		if ( this == UNCERTAIN ) {
			return newRelation;
		}
		
		if ( this == EMPTY ) {
			return EMPTY;
		} else if ( this == EQUAL) {
			if ( newRelation == EMPTY) {
				return EMPTY;
			} else if ( newRelation == EQUAL ) {
				return EQUAL;
			} else if ( newRelation == INTERSECT ) {
				return INTERSECT;
			} else if ( newRelation == SUBSET ) {
				return SUBSET;
			} else if ( newRelation == SUPERSET ) {
				return SUPERSET;
			}
		} else if ( this == INTERSECT) {
			if ( newRelation == EMPTY) {
				return EMPTY;
			} else if ( newRelation == EQUAL ) {
				return INTERSECT;
			} else if ( newRelation == INTERSECT ) {
				return INTERSECT;
			} else if ( newRelation == SUBSET ) {
				return INTERSECT;
			} else if ( newRelation == SUPERSET ) {
				return INTERSECT;
			}
		} else if ( this == SUBSET ){
			if ( newRelation == EMPTY) {
				return EMPTY;
			} else if ( newRelation == EQUAL ) {
				return SUBSET;
			} else if ( newRelation == INTERSECT ) {
				return INTERSECT;
			} else if ( newRelation == SUBSET ) {
				return SUBSET;
			} else if ( newRelation == SUPERSET ) {
				return INTERSECT;
			}
		} else if ( this == SUPERSET ){
			if ( newRelation == EMPTY) {
				return EMPTY;
			} else if ( newRelation == EQUAL ) {
				return SUPERSET;
			} else if ( newRelation == INTERSECT ) {
				return INTERSECT;
			} else if ( newRelation == SUBSET ) {
				return INTERSECT;
			} else if ( newRelation == SUPERSET ) {
				return SUPERSET;
			}
		}
		
		return null;
	}
	
	public String toString() {
		return relation;
	}
}

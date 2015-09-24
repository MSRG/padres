/*
 * Created on Apr 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.scout;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.router.scout.Relation;

/**
 * @author cheung
 * 
 */
public class RelationTest extends TestCase {

	Relation rel;

	/**
	 * Constructor for RelationTest.
	 * 
	 * @param arg0
	 */
	public RelationTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(RelationTest.class);
	}

	public void testEqualAND() {
		rel = Relation.EQUAL;
		assertTrue(rel.AND(Relation.EQUAL) == Relation.EQUAL);
		rel = Relation.EQUAL;
		assertTrue(rel.AND(Relation.SUBSET) == Relation.SUBSET);
		rel = Relation.EQUAL;
		assertTrue(rel.AND(Relation.SUPERSET) == Relation.SUPERSET);
		rel = Relation.EQUAL;
		assertTrue(rel.AND(Relation.EMPTY) == Relation.EMPTY);
		rel = Relation.EQUAL;
		assertTrue(rel.AND(Relation.INTERSECT) == Relation.INTERSECT);
	}

	public void testSubsetAND() {
		rel = Relation.SUBSET;
		assertTrue(rel.AND(Relation.EQUAL) == Relation.SUBSET);
		rel = Relation.SUBSET;
		assertTrue(rel.AND(Relation.SUBSET) == Relation.SUBSET);
		rel = Relation.SUBSET;
		assertTrue(rel.AND(Relation.SUPERSET) == Relation.INTERSECT);
		rel = Relation.SUBSET;
		assertTrue(rel.AND(Relation.EMPTY) == Relation.EMPTY);
		rel = Relation.SUBSET;
		assertTrue(rel.AND(Relation.INTERSECT) == Relation.INTERSECT);
	}

	public void testSupersetAND() {
		rel = Relation.SUPERSET;
		assertTrue(rel.AND(Relation.EQUAL) == Relation.SUPERSET);
		rel = Relation.SUPERSET;
		assertTrue(rel.AND(Relation.SUBSET) == Relation.INTERSECT);
		rel = Relation.SUPERSET;
		assertTrue(rel.AND(Relation.SUPERSET) == Relation.SUPERSET);
		rel = Relation.SUPERSET;
		assertTrue(rel.AND(Relation.EMPTY) == Relation.EMPTY);
		rel = Relation.SUPERSET;
		assertTrue(rel.AND(Relation.INTERSECT) == Relation.INTERSECT);
	}

	public void testIntersectAND() {
		rel = Relation.INTERSECT;
		assertTrue(rel.AND(Relation.EQUAL) == Relation.INTERSECT);
		rel = Relation.INTERSECT;
		assertTrue(rel.AND(Relation.SUBSET) == Relation.INTERSECT);
		rel = Relation.INTERSECT;
		assertTrue(rel.AND(Relation.SUPERSET) == Relation.INTERSECT);
		rel = Relation.INTERSECT;
		assertTrue(rel.AND(Relation.EMPTY) == Relation.EMPTY);
		rel = Relation.INTERSECT;
		assertTrue(rel.AND(Relation.INTERSECT) == Relation.INTERSECT);
	}

	public void testEmptyAND() {
		rel = Relation.EMPTY;
		assertTrue(rel.AND(Relation.EQUAL) == Relation.EMPTY);
		rel = Relation.EMPTY;
		assertTrue(rel.AND(Relation.SUBSET) == Relation.EMPTY);
		rel = Relation.EMPTY;
		assertTrue(rel.AND(Relation.SUPERSET) == Relation.EMPTY);
		rel = Relation.EMPTY;
		assertTrue(rel.AND(Relation.EMPTY) == Relation.EMPTY);
		rel = Relation.EMPTY;
		assertTrue(rel.AND(Relation.INTERSECT) == Relation.EMPTY);
	}

}

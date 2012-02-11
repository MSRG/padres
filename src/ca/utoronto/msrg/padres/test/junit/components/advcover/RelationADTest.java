/*
 * Created on Apr 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.advcover;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.router.scoutad.RelationAD;

/**
 * @author Chen
 * 
 */
public class RelationADTest extends TestCase {

	RelationAD rel;

	/**
	 * Constructor for RelationTest.
	 * 
	 * @param arg0
	 */
	public RelationADTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(RelationADTest.class);
	}

	public void testEqualAND() {
		rel = RelationAD.EQUAL;
		assertTrue(rel.AND(RelationAD.EQUAL) == RelationAD.EQUAL);
		rel = RelationAD.EQUAL;
		assertTrue(rel.AND(RelationAD.SUBSET) == RelationAD.SUBSET);
		rel = RelationAD.EQUAL;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.SUPERSET);
		rel = RelationAD.EQUAL;
		assertTrue(rel.AND(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.EQUAL;
		assertTrue(rel.AND(RelationAD.INTERSECT) == RelationAD.INTERSECT);
	}

	public void testSubsetAND() {
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.EQUAL) == RelationAD.SUBSET);
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.SUBSET) == RelationAD.SUBSET);
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.INTERSECT);
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.INTERSECT) == RelationAD.INTERSECT);
	}

	public void testSupersetAND() {
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.EQUAL) == RelationAD.SUPERSET);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.SUBSET) == RelationAD.INTERSECT);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.SUPERSET);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.INTERSECT) == RelationAD.INTERSECT);
	}

	public void testIntersectAND() {
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.EQUAL) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.SUBSET) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.INTERSECT) == RelationAD.INTERSECT);
	}

	public void testEmptyAND() {
		rel = RelationAD.EMPTY;
		assertTrue(rel.AND(RelationAD.EQUAL) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.AND(RelationAD.SUBSET) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.AND(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.AND(RelationAD.INTERSECT) == RelationAD.EMPTY);
	}

	public void testEmptyOR() {
		rel = RelationAD.EMPTY;
		assertTrue(rel.OR(RelationAD.EQUAL) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.OR(RelationAD.SUBSET) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.OR(RelationAD.SUPERSET) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.OR(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.EMPTY;
		assertTrue(rel.OR(RelationAD.INTERSECT) == RelationAD.EMPTY);
	}

	public void testEqualOR() {
		rel = RelationAD.EQUAL;
		assertTrue(rel.OR(RelationAD.EQUAL) == RelationAD.EQUAL);
		rel = RelationAD.EQUAL;
		assertTrue(rel.OR(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.EQUAL;
		assertTrue(rel.OR(RelationAD.INTERSECT) == RelationAD.INTERSECT);
		rel = RelationAD.EQUAL;
		assertTrue(rel.OR(RelationAD.SUBSET) == RelationAD.SUBSET);
		rel = RelationAD.EQUAL;
		assertTrue(rel.OR(RelationAD.SUPERSET) == RelationAD.SUPERSET);
	}

	public void testIntersectOR() {
		rel = RelationAD.INTERSECT;
		assertTrue(rel.OR(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.OR(RelationAD.EQUAL) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.OR(RelationAD.INTERSECT) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.OR(RelationAD.SUBSET) == RelationAD.INTERSECT);
		rel = RelationAD.INTERSECT;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.INTERSECT);
	}

	public void testSubsetOR() {
		rel = RelationAD.SUBSET;
		assertTrue(rel.OR(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.SUBSET;
		assertTrue(rel.OR(RelationAD.EQUAL) == RelationAD.SUBSET);
		rel = RelationAD.SUBSET;
		assertTrue(rel.OR(RelationAD.INTERSECT) == RelationAD.INTERSECT);
		rel = RelationAD.SUBSET;
		assertTrue(rel.OR(RelationAD.SUBSET) == RelationAD.SUBSET);
		rel = RelationAD.SUBSET;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.INTERSECT);
	}

	public void testSupersetOR() {
		rel = RelationAD.SUPERSET;
		assertTrue(rel.OR(RelationAD.EMPTY) == RelationAD.EMPTY);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.OR(RelationAD.EQUAL) == RelationAD.SUPERSET);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.OR(RelationAD.INTERSECT) == RelationAD.INTERSECT);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.OR(RelationAD.SUBSET) == RelationAD.INTERSECT);
		rel = RelationAD.SUPERSET;
		assertTrue(rel.AND(RelationAD.SUPERSET) == RelationAD.SUPERSET);
	}
}

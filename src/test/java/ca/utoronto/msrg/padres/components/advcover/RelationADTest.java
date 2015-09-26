/*
 * Created on Apr 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.components.advcover;

import org.junit.Test;

import org.junit.Assert;
import ca.utoronto.msrg.padres.broker.router.scoutad.RelationAD;

/**
 * @author Chen
 */
public class RelationADTest extends Assert {

    RelationAD rel;

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

    @Test
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

/*
 * Created on Apr 23, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.scout;

import java.util.Map;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.router.scout.Relation;
import ca.utoronto.msrg.padres.broker.router.scout.RelationIdentifier;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
/**
 * @author cheung
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RelationIdentifierTest extends TestCase {
	
	// Allows us to easily compose a predicate map
	Map[] predIBM = new Map[14];
	Map[] predATI = new Map[2];
	Map<String, Predicate> predAll;
	Map[] predStr = new Map[14];


	/**
	 * Constructor for RelationIdentifierTest.
	 * @param arg0
	 */
	public RelationIdentifierTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(RelationIdentifierTest.class);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		predAll = MessageFactory.createSubscriptionFromString("[class,eq,'stock']").getPredicateMap();
		predIBM[0] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm']").getPredicateMap();
		predIBM[1] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>,100]").getPredicateMap();
		predIBM[2] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>=,100]").getPredicateMap();
		predIBM[3] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[volume,<,101]").getPredicateMap();
		predIBM[4] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,50]").getPredicateMap();
		predIBM[5] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,51]").getPredicateMap();
		predIBM[6] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[volume,<=,100]").getPredicateMap();
			
			
		predIBM[7] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,50],[low,<,35]").getPredicateMap();
		predIBM[8] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,<,70],[low,>,15]").getPredicateMap();
		predIBM[9] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,30],[low,<,55]").getPredicateMap();
		predIBM[10] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,31],[low,<=,54]").getPredicateMap();
		predIBM[11] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,30],[low,<,30]").getPredicateMap();
		predIBM[12] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,31],[low,<=,54],[volume,>,10000]").getPredicateMap();	
		predIBM[13] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ibm'],[low,<=,54],[volume,>=,10001]").getPredicateMap();
		
		predATI[0] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ati']").getPredicateMap();
		predATI[1] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'stock'],[symbol,eq,'ati'],[high,>=,31],[low,<=,54],[volume,>,10000]").getPredicateMap();	
	
		predStr[0] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,eq,'padres']").getPredicateMap();
		predStr[1] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,eq,'duke']").getPredicateMap();
		predStr[2] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-prefix,'pad']").getPredicateMap();
		predStr[3] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-prefix,'padres']").getPredicateMap();
		predStr[4] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-prefix,'padress']").getPredicateMap();
		predStr[5] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-postfix,'es']").getPredicateMap();
		predStr[6] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-postfix,'padres']").getPredicateMap();
		predStr[7] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-postfix,'dress']").getPredicateMap();
		predStr[8] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,neq,'padres']").getPredicateMap();
		predStr[9] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,neq,'no']").getPredicateMap();
		predStr[10] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-contains,'ad']").getPredicateMap();
		predStr[11] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-contains,'padres']").getPredicateMap();
		predStr[12] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-contains,'jeff']").getPredicateMap();		
		predStr[13] = MessageFactory.createSubscriptionFromString(
			"[class,eq,'string'],[string,str-prefix,'jeff']").getPredicateMap();		
		
	}


	public void testGetRelationStringSingleAttr() {
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[0]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[1]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[1], predStr[0]) == Relation.EMPTY);
		// test prefix
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[2]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[2], predStr[0]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[3]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[3], predStr[0]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[4]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[4], predStr[0]) == Relation.EMPTY);
		
		assertTrue(RelationIdentifier.getRelation(predStr[3], predStr[13]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[13], predStr[3]) == Relation.EMPTY);
		
		assertTrue(RelationIdentifier.getRelation(predStr[3], predStr[2]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[2], predStr[3]) == Relation.SUPERSET);

		// test suffix
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[5]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[5], predStr[0]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[6]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[6], predStr[0]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[7]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[7], predStr[0]) == Relation.EMPTY);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[8]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[8], predStr[0]) == Relation.EMPTY);
		
		assertTrue(RelationIdentifier.getRelation(predStr[6], predStr[7]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[7], predStr[6]) == Relation.EMPTY);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[9]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[9], predStr[0]) == Relation.SUPERSET);
		// test contains
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[10]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[10], predStr[0]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[11]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predStr[11], predStr[0]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predStr[0], predStr[12]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predStr[12], predStr[0]) == Relation.EMPTY);
		
		// misc
		assertTrue(RelationIdentifier.getRelation(predStr[4], predStr[5]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[5], predStr[4]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predStr[9], predStr[6]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[6], predStr[9]) == Relation.INTERSECT);

		assertTrue(RelationIdentifier.getRelation(predStr[9], predStr[3]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[3], predStr[9]) == Relation.INTERSECT);

		assertTrue(RelationIdentifier.getRelation(predStr[12], predStr[3]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[3], predStr[12]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predStr[11], predStr[8]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[8], predStr[11]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predStr[12], predStr[13]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predStr[13], predStr[12]) == Relation.INTERSECT);

	}


//	public void testGetRelationStringMultiAttr() {
//	}
	

	public void testGetRelationNumericSingleAttr() {
		assertTrue(RelationIdentifier.getRelation(predIBM[1], predIBM[1]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[4], predIBM[5]) == Relation.EQUAL);
		assertTrue(RelationIdentifier.getRelation(predIBM[5], predIBM[4]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[6], predIBM[3]) == Relation.EQUAL);
		assertTrue(RelationIdentifier.getRelation(predIBM[3], predIBM[6]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[1], predIBM[0]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[0], predIBM[1]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[0], predAll) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predAll, predIBM[0]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[1], predIBM[2]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[2], predIBM[1]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[1], predIBM[3]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predIBM[3], predIBM[1]) == Relation.EMPTY);	
		
		assertTrue(RelationIdentifier.getRelation(predIBM[2], predIBM[3]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[3], predIBM[2]) == Relation.INTERSECT);	
		
		assertTrue(RelationIdentifier.getRelation(predIBM[4], predIBM[3]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[3], predIBM[4]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[6], predIBM[3]) == Relation.EQUAL);
		assertTrue(RelationIdentifier.getRelation(predIBM[3], predIBM[6]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[2], predIBM[8]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[8], predIBM[2]) == Relation.INTERSECT);
	}


	public void testGetRelationNumericMultiAttr() {
		assertTrue(RelationIdentifier.getRelation(predIBM[7], predIBM[8]) == Relation.INTERSECT);	
		assertTrue(RelationIdentifier.getRelation(predIBM[8], predIBM[7]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[7], predIBM[9]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[9], predIBM[7]) == Relation.SUPERSET);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[9], predIBM[10]) == Relation.EQUAL);
		assertTrue(RelationIdentifier.getRelation(predIBM[10], predIBM[9]) == Relation.EQUAL);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[11], predIBM[7]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[7], predIBM[11]) == Relation.INTERSECT);

		assertTrue(RelationIdentifier.getRelation(predIBM[4], predIBM[7]) == Relation.SUPERSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[7], predIBM[4]) == Relation.SUBSET);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[4], predIBM[9]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[9], predIBM[4]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[12], predIBM[10]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[10], predIBM[12]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predIBM[11], predIBM[10]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[10], predIBM[11]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predIBM[4], predIBM[12]) == Relation.INTERSECT);
		assertTrue(RelationIdentifier.getRelation(predIBM[12], predIBM[4]) == Relation.INTERSECT);
		
		assertTrue(RelationIdentifier.getRelation(predIBM[12], predIBM[13]) == Relation.SUBSET);
		assertTrue(RelationIdentifier.getRelation(predIBM[13], predIBM[12]) == Relation.SUPERSET);

		assertTrue(RelationIdentifier.getRelation(predIBM[12], predATI[0]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predATI[0], predIBM[12]) == Relation.EMPTY);

		assertTrue(RelationIdentifier.getRelation(predIBM[12], predATI[1]) == Relation.EMPTY);
		assertTrue(RelationIdentifier.getRelation(predATI[1], predIBM[12]) == Relation.EMPTY);
	}

}

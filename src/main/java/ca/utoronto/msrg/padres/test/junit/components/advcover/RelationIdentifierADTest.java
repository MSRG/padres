/*
 * Created on Nov 30, 2010
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.advcover;

import java.util.Map;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.router.scoutad.RelationAD;
import ca.utoronto.msrg.padres.broker.router.scoutad.RelationIdentifierAD;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author Chen
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RelationIdentifierADTest extends TestCase {

	// Allows us to easily compose a predicate map
	Map[] predIBM = new Map[14];

	Map[] predATI = new Map[2];

	Map predAll;

	Map[] predStr = new Map[14];

	/**
	 * Constructor for RelationIdentifierTest.
	 * 
	 * @param arg0
	 */
	public RelationIdentifierADTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(RelationIdentifierADTest.class);
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		predAll = MessageFactory.createAdvertisementFromString("[class,eq,'stock']").getPredicateMap();
		predIBM[0] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm']").getPredicateMap();
		predIBM[1] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>,100]").getPredicateMap();
		predIBM[2] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>=,100]").getPredicateMap();
		predIBM[3] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,<,101]").getPredicateMap();
		predIBM[4] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,50]").getPredicateMap();
		predIBM[5] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,51]").getPredicateMap();
		predIBM[6] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,<=,100]").getPredicateMap();

		predIBM[7] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,50],[low,<,35]").getPredicateMap();
		predIBM[8] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,<,70],[low,>,15]").getPredicateMap();
		predIBM[9] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,30],[low,<,55]").getPredicateMap();
		predIBM[10] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,31],[low,<=,54]").getPredicateMap();
		predIBM[11] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,30],[low,<,30]").getPredicateMap();
		predIBM[12] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,31],[low,<=,54],[volume,>,10000]").getPredicateMap();
		predIBM[13] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[low,<=,54],[volume,>=,10001]").getPredicateMap();

		predATI[0] = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ati']").getPredicateMap();
		predATI[1] = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ati'],[high,>=,31],[low,<=,54],[volume,>,10000]").getPredicateMap();

		predStr[0] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,eq,'padres']").getPredicateMap();
		predStr[1] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,eq,'duke']").getPredicateMap();
		predStr[2] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-prefix,'pad']").getPredicateMap();
		predStr[3] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-prefix,'padres']").getPredicateMap();
		predStr[4] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-prefix,'padress']").getPredicateMap();
		predStr[5] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-postfix,'es']").getPredicateMap();
		predStr[6] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-postfix,'padres']").getPredicateMap();
		predStr[7] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-postfix,'dress']").getPredicateMap();
		predStr[8] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,neq,'padres']").getPredicateMap();
		predStr[9] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,neq,'no']").getPredicateMap();
		predStr[10] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-contains,'ad']").getPredicateMap();
		predStr[11] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-contains,'padres']").getPredicateMap();
		predStr[12] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-contains,'jeff']").getPredicateMap();
		predStr[13] = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-prefix,'jeff']").getPredicateMap();

	}

	public void testGetRelationStringSingleAttr() {

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[0]) == RelationAD.EQUAL);

		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[1]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[1], predStr[0]) ==
		// RelationAD.EMPTY);
		// test prefix
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[2]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[2], predStr[0]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[3]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[3], predStr[0]) == RelationAD.SUPERSET);

		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[4]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[4], predStr[0]) ==
		// RelationAD.EMPTY);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[3], predStr[13]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[13], predStr[3]) ==
		// RelationAD.EMPTY);

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[3], predStr[2]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[2], predStr[3]) == RelationAD.SUPERSET);

		// test suffix
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[5]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[5], predStr[0]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[6]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[6], predStr[0]) == RelationAD.SUPERSET);

		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[7]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[7], predStr[0]) ==
		// RelationAD.EMPTY);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[8]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[8], predStr[0]) ==
		// RelationAD.EMPTY);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[6], predStr[7]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[7], predStr[6]) ==
		// RelationAD.EMPTY);

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[9]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[9], predStr[0]) == RelationAD.SUPERSET);
		// test contains
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[10]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[10], predStr[0]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[11]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predStr[11], predStr[0]) == RelationAD.SUPERSET);

		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[0], predStr[12]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[12], predStr[0]) ==
		// RelationAD.EMPTY);
		//
		// // misc
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[4], predStr[5]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[5], predStr[4]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[9], predStr[6]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[6], predStr[9]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[9], predStr[3]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[3], predStr[9]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[12], predStr[3]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[3], predStr[12]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[11], predStr[8]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[8], predStr[11]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[12], predStr[13]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predStr[13], predStr[12]) ==
		// RelationAD.INTERSECT);

	}

	// public void testGetRelationStringMultiAttr() {
	// }

	public void testGetRelationNumericSingleAttr() {
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[1], predIBM[1]) == RelationAD.EQUAL);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[4], predIBM[5]) == RelationAD.EQUAL);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[5], predIBM[4]) == RelationAD.EQUAL);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[6], predIBM[3]) == RelationAD.EQUAL);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[3], predIBM[6]) == RelationAD.EQUAL);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[1], predIBM[0]) == RelationAD.SUPERSET);

		System.out.println(RelationIdentifierAD.getRelationAD(predIBM[0], predIBM[1]));
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[0], predIBM[1]) == RelationAD.SUBSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[0], predAll) == RelationAD.SUPERSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predAll, predIBM[0]) == RelationAD.SUBSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[1], predIBM[2]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[2], predIBM[1]) == RelationAD.SUPERSET);

		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[1], predIBM[3]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[3], predIBM[1]) ==
		// RelationAD.EMPTY);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[2], predIBM[3]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[3], predIBM[2]) ==
		// RelationAD.INTERSECT);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[4], predIBM[3]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[3], predIBM[4]) ==
		// RelationAD.INTERSECT);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[6], predIBM[3]) == RelationAD.EQUAL);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[3], predIBM[6]) == RelationAD.EQUAL);

		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[2], predIBM[8]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[8], predIBM[2]) ==
		// RelationAD.INTERSECT);
	}

	public void testGetRelationNumericMultiAttr() {
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[7], predIBM[8]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[8], predIBM[7]) ==
		// RelationAD.INTERSECT);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[7], predIBM[9]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[9], predIBM[7]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[9], predIBM[10]) == RelationAD.EQUAL);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[10], predIBM[9]) == RelationAD.EQUAL);

		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[11], predIBM[7]) ==
		// RelationAD.INTERSECT);
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[7], predIBM[11]) ==
		// RelationAD.INTERSECT);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[4], predIBM[7]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[7], predIBM[4]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[4], predIBM[9]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[9], predIBM[4]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[12], predIBM[10]) == RelationAD.SUPERSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[10], predIBM[12]) == RelationAD.SUBSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[11], predIBM[10]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[10], predIBM[11]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[4], predIBM[12]) == RelationAD.SUBSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[12], predIBM[4]) == RelationAD.SUPERSET);

		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[12], predIBM[13]) == RelationAD.SUPERSET);
		assertTrue(RelationIdentifierAD.getRelationAD(predIBM[13], predIBM[12]) == RelationAD.SUBSET);

		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[12], predATI[0]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predATI[0], predIBM[12]) ==
		// RelationAD.EMPTY);
		//
		// assertTrue(RelationIdentifierAD.getRelationAD(predIBM[12], predATI[1]) ==
		// RelationAD.EMPTY);
		// assertTrue(RelationIdentifierAD.getRelationAD(predATI[1], predIBM[12]) ==
		// RelationAD.EMPTY);
	}

}

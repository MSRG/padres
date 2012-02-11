/*
 * Created on Nov 30, 2010
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.advcover;

import java.util.LinkedList;
import java.util.Map;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.router.scoutad.ScoutNodeAD;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Chen
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class IsCoveredADTest extends TestCase {

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
	public IsCoveredADTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(IsCoveredADTest.class);
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

	public void testIsCoveredIBM() throws ParseException {
		LinkedList<ScoutNodeAD> scoutNodeADList = new LinkedList();

		for (int i = 0; i < predIBM.length; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predIBM[i], idStr);
			scoutNodeADList.add(scoutNodeAD);
		}

		Map predNew1 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>,150]").getPredicateMap();
		ScoutNodeAD newNode1 = new ScoutNodeAD(predNew1, "101");
		assertTrue(newNode1.isCovered(scoutNodeADList) == true);

		Map predNew2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[volume,>=,150]").getPredicateMap();
		ScoutNodeAD newNode2 = new ScoutNodeAD(predNew2, "102");
		assertTrue(newNode2.isCovered(scoutNodeADList) == true);

		Map predNew3 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm'],[high,>=,80]").getPredicateMap();
		ScoutNodeAD newNode3 = new ScoutNodeAD(predNew3, "103");
		assertTrue(newNode3.isCovered(scoutNodeADList) == true);

		Map predNew4 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ibm1']").getPredicateMap();
		ScoutNodeAD newNode4 = new ScoutNodeAD(predNew4, "104");
		assertTrue(newNode4.isCovered(scoutNodeADList) == false);

		Map predNew5 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,55],[low,<,30]").getPredicateMap();
		ScoutNodeAD newNode5 = new ScoutNodeAD(predNew5, "105");
		assertTrue(newNode5.isCovered(scoutNodeADList) == true);

		Map predNew6 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ibm'],[high,>,20],[low,<,30]").getPredicateMap();
		ScoutNodeAD newNode6 = new ScoutNodeAD(predNew6, "106");
		assertTrue(newNode6.isCovered(scoutNodeADList) == false);
	}

	public void testIsCoveredATI() throws ParseException {
		LinkedList<ScoutNodeAD> scoutNodeADList = new LinkedList();

		for (int i = 0; i < predATI.length; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predATI[i], idStr);
			scoutNodeADList.add(scoutNodeAD);
		}

		Map predNew1 = MessageFactory.createAdvertisementFromString(
				"[class,eq,'stock'],[symbol,eq,'ati'],[high,>=,31],[low,<=,54]").getPredicateMap();
		ScoutNodeAD newNode1 = new ScoutNodeAD(predNew1, "101");
		assertTrue(newNode1.isCovered(scoutNodeADList) == true);

		Map predNew2 = MessageFactory.createAdvertisementFromString("[class,eq,'stock'],[symbol,eq,'ati'],[highhigh,>=,35]").getPredicateMap();
		ScoutNodeAD newNode2 = new ScoutNodeAD(predNew2, "102");
		newNode2.isCovered(scoutNodeADList);
		assertTrue(newNode2.isCovered(scoutNodeADList) == false);
	}

	public void testIsCoveredStr() throws ParseException {
		// test eq
		LinkedList<ScoutNodeAD> scoutNodeADList1 = new LinkedList();

		for (int i = 0; i < 2; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predStr[i], idStr);
			scoutNodeADList1.add(scoutNodeAD);
		}

		Map predNew1 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,eq,'padres']").getPredicateMap();
		ScoutNodeAD newNode1 = new ScoutNodeAD(predNew1, "101");
		assertTrue(newNode1.isCovered(scoutNodeADList1) == true);

		// test str-prefix
		LinkedList<ScoutNodeAD> scoutNodeADList2 = new LinkedList();

		for (int i = 2; i < 5; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predStr[i], idStr);
			scoutNodeADList2.add(scoutNodeAD);
		}

		Map predNew2 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-prefix,'padressss']").getPredicateMap();
		ScoutNodeAD newNode2 = new ScoutNodeAD(predNew2, "102");
		assertTrue(newNode2.isCovered(scoutNodeADList2) == true);

		assertTrue(newNode1.isCovered(scoutNodeADList2) == true);
		assertTrue(newNode2.isCovered(scoutNodeADList1) == false);

		// test str-postfix
		LinkedList<ScoutNodeAD> scoutNodeADList3 = new LinkedList();

		for (int i = 5; i < 8; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predStr[i], idStr);
			scoutNodeADList3.add(scoutNodeAD);
		}

		Map predNew3 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-postfix,'ppadres']").getPredicateMap();
		ScoutNodeAD newNode3 = new ScoutNodeAD(predNew3, "103");
		assertTrue(newNode3.isCovered(scoutNodeADList3) == true);

		assertTrue(newNode1.isCovered(scoutNodeADList3) == true);

		// test neq
		LinkedList<ScoutNodeAD> scoutNodeADList4 = new LinkedList();

		for (int i = 8; i < 9; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predStr[i], idStr);
			scoutNodeADList4.add(scoutNodeAD);
		}

		Map predNew4 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,eq,'ppadres']").getPredicateMap();
		ScoutNodeAD newNode4 = new ScoutNodeAD(predNew4, "104");
		assertTrue(newNode4.isCovered(scoutNodeADList4) == true);

		Map predNew41 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,eq,'padres']").getPredicateMap();
		ScoutNodeAD newNode41 = new ScoutNodeAD(predNew41, "104");
		assertTrue(newNode41.isCovered(scoutNodeADList4) == false);

		// test str-contains
		LinkedList<ScoutNodeAD> scoutNodeADList5 = new LinkedList();

		for (int i = 10; i < 13; i++) {
			String idStr = Integer.toString(i);
			ScoutNodeAD scoutNodeAD = new ScoutNodeAD(predStr[i], idStr);
			scoutNodeADList5.add(scoutNodeAD);
		}

		Map predNew5 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-contains,'ppadress']").getPredicateMap();
		ScoutNodeAD newNode5 = new ScoutNodeAD(predNew5, "105");
		assertTrue(newNode5.isCovered(scoutNodeADList5) == true);

		Map predNew6 = MessageFactory.createAdvertisementFromString("[class,eq,'string'],[string,str-contains,'xxx']").getPredicateMap();
		ScoutNodeAD newNode6 = new ScoutNodeAD(predNew6, "105");
		assertTrue(newNode6.isCovered(scoutNodeADList5) == false);
	}

}

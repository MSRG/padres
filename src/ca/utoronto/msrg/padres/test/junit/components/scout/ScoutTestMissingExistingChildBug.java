/*
 * Created on Apr 24, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.scout;

import junit.framework.TestCase;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.router.scout.*;
import ca.utoronto.msrg.padres.common.message.*;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author cheung
 * 
 *         Tests to see that a newly inserted node has to search for its children to add to its
 *         children set. If this is not done, then the supposedly child node can become a neighbor
 *         of its parent when the child's last parent is removed
 */
public class ScoutTestMissingExistingChildBug extends TestCase {

	private static final String subscriptionFile = BrokerConfig.PADRES_HOME
			+ "/etc/test/junit/matching/scout/ScoutTestMissingExistingChild.txt";

	private Scout scout;

	/**
	 * Constructor for ScoutTestRemoveDupChildBug.
	 * 
	 * @param arg0
	 */
	public ScoutTestMissingExistingChildBug(String arg0) {
		super(arg0);
		scout = new Scout();
		loadScout();
	}

	public static void main(String[] args) {
		junit.swingui.TestRunner.run(ScoutTestMissingExistingChildBug.class);
	}

	private void loadScout() {
		int id = 0;
		String line;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(subscriptionFile));
			while ((line = reader.readLine()) != null) {
				String idStr = Integer.toString(id++);
				Subscription sub = MessageFactory.createSubscriptionFromString(line);
				sub.setSubscriptionID(idStr);
				SubscriptionMessage subMsg = new SubscriptionMessage(sub, idStr, null);
				scout.insert(subMsg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// scout.showTree();
	}

	public void testMissingExistingChildBug() {
		HashSet<ScoutNode> expectedCoveringSet = new HashSet<ScoutNode>();
		expectedCoveringSet.add(scout.getNode("0"));
		expectedCoveringSet.add(scout.getNode("2"));
		expectedCoveringSet.add(scout.getNode("3"));
		expectedCoveringSet.add(scout.getNode("5"));
		// System.out.println(expectedCoveringSet.toString() + "\n");

		assertTrue(scout.coveringSubscriptionSet().equals(expectedCoveringSet));

		Set<ScoutNode> expectedParentSet = new HashSet<ScoutNode>();
		expectedParentSet.add(scout.getNode("2"));
		expectedParentSet.add(scout.getNode("3"));
		expectedParentSet.add(scout.getNode("4"));
		expectedParentSet.add(scout.getNode("5"));

		ScoutNode bottomNode = (ScoutNode) scout.getNode("1");
//		assertTrue(bottomNode.parentSet.equals(expectedParentSet));
		assertTrue(expectedParentSet.containsAll(bottomNode.parentSet));

	}
}

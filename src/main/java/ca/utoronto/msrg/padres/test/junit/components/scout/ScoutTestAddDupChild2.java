/*
 * Created on Apr 24, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.test.junit.components.scout;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.router.scout.Scout;
import ca.utoronto.msrg.padres.broker.router.scout.ScoutNode;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author cheung
 * 
 *         Tests to see that a newly inserted node has to search for its children to add to its
 *         children set. If this is not done, then the supposedly child node can become a neighbor
 *         of its parent when the child's last parent is removed
 */
public class ScoutTestAddDupChild2 extends TestCase {

	private static final String subscriptionFile = BrokerConfig.PADRES_HOME
			+ "/etc/test/junit/matching/scout/ScoutTestAddDupChild2.txt";

	private Scout scout;

	/**
	 * Constructor for ScoutTestRemoveDupChildBug.
	 * 
	 * @param arg0
	 */
	public ScoutTestAddDupChild2(String arg0) {
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

	public void testSupersetSubset() {
		Set<ScoutNode> expectedParentSet = new HashSet<ScoutNode>();
		expectedParentSet.add(scout.getNode("2562"));
		expectedParentSet.add(scout.getNode("2793"));
		// System.out.println(scout.getNode("2722").parentSet.toString());
//		assertTrue(scout.getNode("2722").parentSet.containsAll(expectedParentSet));
		assertTrue(expectedParentSet.containsAll(scout.getNode("2722").parentSet));

	}
}

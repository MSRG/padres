package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ca.utoronto.msrg.padres.broker.router.Router;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.CompositeNode;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionOPs;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;

/**
 * This is the Rete network data structure.
 * 
 */
public class ReteNetwork implements Iterable<Node> {

	public enum NWType {
		ADV_TREE, SUB_TREE
	};

	private Router router;

	private final NWType type;

	/**
	 * The roots of the pattern network
	 */
	private RootNode m_root;

	private Map<String, Set<NodeTerminal>> reteTerminalMap = new HashMap<String, Set<NodeTerminal>>();

	private boolean partialMatchFound = false;

	private Set<String> matchingResultSubID = new HashSet<String>();

	private Set<String> matchingResultAdvID = new HashSet<String>();

	private HashMap<Publication, Set<String>> matchingPubSubID = new HashMap<Publication, Set<String>>();

	public final Node getRoot() {
		return m_root;
	}

	public ReteNetwork(NWType t, Router r) {
		type = t;
		router = r;
		m_root = new RootNode(this);
	}

	public NWType getNWType() {
		return type;
	}

	public boolean isSubTree() {
		return type.equals(NWType.SUB_TREE);
	}

	public boolean isAdvTree() {
		return type.equals(NWType.ADV_TREE);
	}

	public Router getRouter() {
		return router;
	}

	public String dataSizeToString() {
		return String.format("%d\t%d\t%d\t%d", reteTerminalMap.size(), matchingResultAdvID.size(),
				matchingResultSubID.size(), matchingPubSubID.size());
	}

	public Map<String, Set<NodeTerminal>> getTerminals() {
		return reteTerminalMap;
	}

	public HashMap<Publication, Set<String>> getMatchingPubSubs() {
		return matchingPubSubID;
	}

	public void printMatchingPubSubs() {
		System.out.println(matchingPubSubID);
	}

	/**
	 * Return the class node in the network for the specified class, or null if no such node is
	 * found.
	 */
	public NodeTClass getClassNode(String className) {
		// Iterate over the root node's successors.
		// TODO: Optimize this with a hash table lookup.
		for (Node n : m_root.getSuccessors()) {
			if (n == null)
				break;
			assert n instanceof NodeTClass;
			NodeTClass classNode = (NodeTClass) n;
			if (classNode.getClassName().equals(className)) {
				return classNode;
			}
		}
		return null;
	}

	/**
	 * Adds part of the subscription - from class node to the final attribute node. Attributes are
	 * added alphabetically.
	 * 
	 * @param s
	 * @param sid
	 * @return
	 * @throws ReteException
	 */
	private synchronized Node addPartialSub(Subscription s, String sid) throws ReteException {
		// for each subscription, add class node and attribute nodes.
		Map<String, Predicate> predicateMap = s.getPredicateMap();

		Node last = addPredicateMap(predicateMap, sid);

		// return the last attribute node
		return last;
	}

	/**
	 * This is the method mainly used by {@link #addPartialSub(Subscription, String)} method. It
	 * creates and inserts class and attribute nodes relavent to the subscription.
	 * 
	 * @param predicateMap
	 * @param sid
	 * @return
	 * @throws ReteException
	 */
	private synchronized Node addPredicateMap(Map<String, Predicate> predicateMap, String sid)
			throws ReteException {
		// first, add the class node under the root node.
		Predicate classPredicate = predicateMap.get("class");
		String subscriptionClass = null;
		if (classPredicate != null)
			subscriptionClass = (String) classPredicate.getValue();
		else
			return null;
		Test classTest = new TestString("eq", subscriptionClass);
		Node last = m_root.mergeSuccessor(new NodeTClass(subscriptionClass, classTest, this), "L");
		String[] sortedArray = predicateMap.keySet().toArray(new String[0]);
		Arrays.sort(sortedArray);

		// add other attribute nodes below the class node.
		for (int i = 0; i < sortedArray.length; i++) {
			String attrName = (String) sortedArray[i];
			// we have dealt with the class node separately.
			if (!attrName.equals("class")) {
				// collect data for constructing a new attribute node
				Predicate p = predicateMap.get(attrName);
				String op = (String) p.getOp();
				Object v = (Object) p.getValue();
				Test test = null;
				if (v.toString().startsWith("$S$")) {
					op = "isPresent";
					test = new TestString(op, v.toString());
				} else if (v.toString().startsWith("$I$")) {
					op = "isPresent";
					test = new TestLong(op, 0l);
				} else if (v.getClass().equals(String.class)) {
					test = new TestString(op, v.toString());
				} else if (v.getClass().equals(Long.class)) {
					test = new TestLong(op, (Long) v);
				} else if (v.getClass().equals(Double.class)) {
					test = new TestDouble(op, (Double) v);
				}
				// add new attribute node
				last = addSimpleTest(last, test, attrName, sid);
			}
		}
		// return the last attribute node added
		return last;
	}

	/**
	 * Adds a subscription to the Rete network.
	 * 
	 * @param s
	 * @param sid
	 * @return
	 * @throws ReteException
	 */
	public synchronized Node addSub(Subscription s, String sid) throws ReteException {
		// insert the class node and other attribute nodes into the Rete network
		Node last = addPartialSub(s, sid);
		if (last == null)
			return null;
		// insert the dummy node that interfaces to terminal and join nodes
		Map<String, Predicate> subPredicateMap = s.getPredicateMap();
		last = last.mergeSuccessor(new NodeTLeft(subPredicateMap, this), "L");
		// create the terminal node
		Node terminal = last.mergeSuccessor(new NodeTerminal(this, sid), "L");
		if (reteTerminalMap.containsKey(sid)) {
			reteTerminalMap.get(sid).add((NodeTerminal) terminal);
		} else {
			Set<NodeTerminal> newSet = new HashSet<NodeTerminal>();
			newSet.add((NodeTerminal) terminal);
			reteTerminalMap.put(sid, newSet);
		}

		return terminal;
	}

	/**
	 * Adds a composite subscription to the Rete network.
	 * 
	 * @param cs
	 * @param sid
	 * @throws ReteException
	 */
	public synchronized void addCompositeSub(CompositeSubscription cs, String sid)
			throws ReteException {
		CompositeNode csRoot = cs.getRoot();
		Map<String, Subscription> subMap = cs.getSubscriptionMap();
		// add first part of the composite subscription.
		Node last = addPartialCS(csRoot, subMap, sid);
		// complete it by adding the terminal node.
		Node terminal = last.mergeSuccessor(new NodeTerminal(this, sid), "L");

		if (reteTerminalMap.containsKey(sid)) {
			reteTerminalMap.get(sid).add((NodeTerminal) terminal);
		} else {
			Set<NodeTerminal> newSet = new HashSet<NodeTerminal>();
			newSet.add((NodeTerminal) terminal);
			reteTerminalMap.put(sid, newSet);
		}
	}

	private synchronized Node addPartialCS(CompositeNode root, Map<String, Subscription> subMap,
			String sid) throws ReteException {

		Node sublast = null;
		// boolean joinNodeHasVariable = false;
		if (root.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)
				|| root.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {
			// it is a non-leave node (operator node)
			// recursively add the left and right subscriptions
			Node tmpLeft = addPartialCS(root.leftNode, subMap, sid);
			Node tmpRight = addPartialCS(root.rightNode, subMap, sid);
			Node joinNode = null;
			Node lastLeft = tmpLeft;
			Node lastRight = tmpRight;

			if (tmpLeft instanceof NodeTAttr) {
				Map<String, Predicate> predicateMapLeft = subMap.get(root.leftNode.content).getPredicateMap();
				lastLeft = tmpLeft.mergeSuccessor(new NodeTLeft(predicateMapLeft, this), "L");
			}

			if (tmpRight instanceof NodeTAttr) {
				Map<String, Predicate> predicateMapRight = subMap.get(root.rightNode.content).getPredicateMap();
				lastRight = tmpRight.mergeSuccessor(new NodeTLeft(predicateMapRight, this), "L");
			}

			joinNode = addJoinNodeUnify(lastLeft, lastRight, root.content);

			return joinNode;
		} else {
			// reached a leave node (subscription node)
			// insert the subscription into the terminal node
			Subscription sub = subMap.get(root.content);
			String subSid = sid + "-" + root.content;
			sublast = addPartialSub(sub, subSid);
		}

		return sublast;
	}

	/**
	 * Checks whether a predicate map has variable definitions inside.
	 * 
	 * @param predicateMap
	 * @return
	 */
	@Deprecated
	private boolean predicateMapHasVariable(Map<String, Predicate> predicateMap) {
		for (String attribute : predicateMap.keySet()) {
			if (attribute.equalsIgnoreCase("class"))
				continue;
			Predicate p = (Predicate) predicateMap.get(attribute);
			Object v = (Object) p.getValue();
			if (v.toString().startsWith("$S$") || v.toString().startsWith("$I$")) {
				return true;
			}
		}
		return false;
	}

	public synchronized void addPub(Publication p) {
		m_root.callNodeRight(p, 0);
	}

	public synchronized Node addAdv(Advertisement a, String aid) throws ReteException {
		Map<String, Predicate> predicateMap = a.getPredicateMap();

		Node last = addPredicateMap(predicateMap, aid);
		last = last.mergeSuccessor(new NodeTLeft(predicateMap, this), "L");

		// crate terminal nodes
		Node terminal = last.mergeSuccessor(new NodeTerminal(this, aid), "L");

		if (reteTerminalMap.containsKey(aid)) {
			reteTerminalMap.get(aid).add((NodeTerminal) terminal);
		} else {
			Set<NodeTerminal> newSet = new HashSet<NodeTerminal>();
			newSet.add((NodeTerminal) terminal);
			reteTerminalMap.put(aid, newSet);
		}

		return terminal;
	}

	public synchronized void collectSubs(String subMsgID) {
		matchingResultSubID.add(subMsgID);
	}

	public synchronized void collectAdvs(String advMsgID) {
		matchingResultAdvID.add(advMsgID);
	}

	public synchronized void collectPubMatchingSubs(Publication pub, String sid) {
		if (sid.contains("-s")) {
			String csID = sid.split("-s")[0];
			Set<String> csMsgIDs = router.getCompositeSubscriptions().keySet();
			if (csMsgIDs.contains(csID))
				return;
		}
		if (!matchingPubSubID.containsKey(pub)) {
			matchingPubSubID.put(pub, new HashSet<String>());
		}
		matchingPubSubID.get(pub).add(sid);
	}

	public synchronized void collectPubMatchingSubs(Set<String> pubMsgIDs, String subMsgID) {
		for (String msgID : pubMsgIDs) {
			Publication pub = router.getPublicationMessage(msgID).getPublication();
			collectPubMatchingSubs(pub, subMsgID);
		}
	}

	/**
	 * @return IDs of advertisements that intersect sub.
	 */
	public synchronized Set<String> getMatches(Subscription sub) {
		matchingResultAdvID.clear();
		m_root.callNodeRight(sub, 0);
		Set<String> tmpResult = matchingResultAdvID;
		matchingResultAdvID = new HashSet<String>();
		return tmpResult;
	}

	/**
	 * @return IDs of subscriptions that intersect adv.
	 */
	public synchronized Set<String> getMatches(Advertisement adv) {
		matchingResultSubID.clear();
		m_root.callNodeRight(adv, 0);
		Set<String> tmpResult = matchingResultSubID;
		return tmpResult;
	}

	public synchronized Map<Publication, Set<String>> getMatches(Publication pub) {
		matchingPubSubID.clear();
		partialMatchFound = false;
		m_root.callNodeRight(pub, 0);
		Map<Publication, Set<String>> tmpResult = matchingPubSubID;
		return tmpResult;
	}

	public void setPartialMatchFound() {
		partialMatchFound = true;
	}

	public boolean isPartialMatch() {
		return partialMatchFound;
	}

	public synchronized void removeAdv(String aid) {
		Set<NodeTerminal> terminalSet = reteTerminalMap.get(aid);
		for (NodeTerminal terminal : terminalSet) {
			Node parent = terminal.getParent("L");
			Node child = terminal;

			while (parent != null) {
				parent.removeSuccessor(child);
				if (parent.nSucc > 0) {
					parent = null;
				} else {
					child = parent;
					parent = parent.getParent("L");
				}
			}
		}
		reteTerminalMap.remove(aid);
	}

	public synchronized void removeSub(String sid) {
		Set<NodeTerminal> terminalSet = reteTerminalMap.get(sid);
		if (terminalSet != null) {
			for (NodeTerminal terminal : terminalSet) {
				Node parent = terminal.getParent("L");
				Node child = terminal;
				removeChild(parent, child);
			}
			terminalSet.clear();
			reteTerminalMap.remove(sid);
		}
	}

	private void removeChild(Node parent, Node child) {
		parent.removeSuccessor(child);
		if (parent instanceof Node1) {
			if (parent.nSucc < 1) {
				child = parent;
				parent = parent.getParent("L");
				removeChild(parent, child);

			}
		} else if (parent instanceof Node2) {
			if (parent.nSucc < 1) {
				child = parent;
				removeChild(parent.getParent("L"), child);
				removeChild(parent.getParent("R"), child);
			}
		}
	}

	public boolean isInMemory(String pubID) {
		boolean result = m_root.isInMemory(pubID);
		return result;
	}

	/**
	 * Delete partial matching state from all Join nodes in the network, and flush the working
	 * memory of all publications.
	 */
	public void flushPartialState() {
		flushPartialState(m_root);
	}

	/**
	 * Delete partial matching state from all Join nodes in the network that are decendent of the
	 * specified node.
	 */
	public void flushPartialState(Node root) {
		for (Node n : new DepthFirstIterator(root)) {
			if (n instanceof NodeJoinUnify)
				((NodeJoinUnify) n).flushMemory();
		}
	}

	/**
	 * Delete partial matching state related to the specified class.
	 * 
	 * @deprecated This method is not supported since we don't know what its semantics should be.
	 * 
	 * @param className
	 *            The class who state to flush.
	 */
	@Deprecated
	public void flushPartialState(String className) {
		throw new UnsupportedOperationException();
		// Node classNode = m_builder.getClassNode(className);
		// if (classNode != null)
		// flushPartialState(classNode);
	}

	public DepthFirstIterator iterator() {
		return new DepthFirstIterator(m_root);
	}

	private Node addJoinNodeUnify(Node left, Node right, String op) throws ReteException {
		NodeJoinUnify n = new NodeJoinUnify(left, op, right, this);
		left.mergeSuccessor(n, "L");
		right.mergeSuccessor(n, "R");
		return n;

	}

	@Deprecated
	private Node addJoinNode(Node left, Node right, String op, String sid) throws ReteException {
		NodeJoin n = new NodeJoin(op, right, this);
		left.mergeSuccessor(n, "L");
		right.mergeSuccessor(n, "R");
		return n;
	}

	/**
	 * Creates and adds a join node that can handle variables in the composite subscriptions.
	 * 
	 * @param left
	 *            left parent - A nodeTLeft node
	 * @param right
	 *            right parent - A nodeTAttr node
	 * @param predicateMapRight
	 *            map of predicates from the right parent
	 * @param op
	 *            operation of the composite subscription (either AND or OR).
	 * @param sid
	 *            ID of the CS
	 * @return the newly added join node.
	 * @throws ReteException
	 */
	@Deprecated
	private Node addJoinNodeWithVariable(Node left, Node right,
			Map<String, Predicate> predicateMapRight, String op, String sid) throws ReteException {
		// create the join node
		NodeJoinByVariable n = new NodeJoinByVariable(left, op, right, predicateMapRight, this);
		// set the left and right parents
		left.mergeSuccessor(n, "L");
		right.mergeSuccessor(n, "R");
		return n;
	}

	/**
	 * Create a new attribute node and add it to the Rete network.
	 * 
	 * @param last
	 * @param test
	 * @param attr
	 * @param sid
	 * @return
	 * @throws ReteException
	 */
	private Node addSimpleTest(Node last, Test test, String attr, String sid) throws ReteException {
		Node1 node = new NodeTAttr(attr, test, this);
		return last.mergeSuccessor(node, "L");
	}

}

/**
 * The root node of the Rete network.
 * 
 */
class RootNode extends Node1 {

	private static final long serialVersionUID = 1L;

	public RootNode(ReteNetwork rn) {
		super(rn);
	}

	/**
	 * Calls the {@code passAlong} method.
	 * 
	 * @see matching.rete.Node#passAlong(java.lang.Object)
	 */
	public boolean callNodeRight(Object p, int matchCount) {
		passAlong(p, matchCount);
		return true;
	}

	public String toString() {
		return this.getClass().getSimpleName();
	}
}

class Pair {

	String attr_name;

	Object value;

	Pair(String a, Object v) {
		attr_name = a;
		value = v;
	}

	public String getAttrName() {
		return attr_name;
	}

	public Object getValue() {
		return value;
	}
}

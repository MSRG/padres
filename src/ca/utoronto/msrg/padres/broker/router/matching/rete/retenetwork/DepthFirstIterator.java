/**
 * 
 */
package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Collection;

/**
 * Depth first iterator of the rete network.
 * Note: Modifying the network while iterating over it is undefined.
 * Note: Nodes may be visited multiple times. (!)
 */
public class DepthFirstIterator implements Iterator<Node>, Iterable<Node> {

	Node nextNode;	// the next node to return.
	LinkedList<NodeChildPair> ancestors;	// the ancestors of the nextNode in the rete network.
	Collection<Node> visited;	// visited nodes
	
	/**
	 * @param node The node to start iterating from.
	 */
	public DepthFirstIterator(Node node) {
		nextNode = node;
		ancestors = new LinkedList<NodeChildPair>();
		visited = new LinkedList<Node>();
	}

	public int getNumAncestors() {
		return ancestors.size();
	}
	
	public boolean hasNext() {
		return nextNode != null;
	}

	/**
	 * @return The next node in the network.
	 */
	public Node next() {
		if (!hasNext())
			throw new NoSuchElementException();
		
		// Remember nextNode which is what we're going to return this time.
		Node returnNode = nextNode;
		visited.add(returnNode);

		// Increment nextNode for next time. 
		ancestors.add(new NodeChildPair(returnNode));
		nextNode = null;	// we may not find a next node.
		while (!ancestors.isEmpty()) {
			// Get the parent's next child.
			Node child = ancestors.getLast().nextChild();
			if (child != null && !haveVisited(child)) {
				nextNode = child;
				break;
			}

			// Parent has no more children so look at grandparent's children next.
			ancestors.removeLast();
		}
		
		// Return the nextNode value when the function was called.
		return returnNode;
	}

	/**
	 * Not supported.
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return true if node has been visited by this iterator, false otherwise.
	 */
	boolean haveVisited(Node node) {
		// TODO: Optimize this function.

		// We check the nodes' object references, since there's no other 
		// way to uniquely identify a node in the rete.

		// I'm not sure if Object.hashcode() returns unique hash values, so
		// we can't use a hash table lookup of object references.

		for (Node n : this.visited) {
			if (n == node)
				return true;
		}
		return false;
	}
	
	/**
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Node> iterator() {
		return this;
	}

	/**
	 * A helper class to store a Rete Node and a next child index for the node.
	 */
	class NodeChildPair {
		Node node;
		int nextChildIndex;

		public NodeChildPair(Node node) {
			this(node, 0);
		}

		public NodeChildPair(Node node, int nextChildIndex) {
			this.node = node;
			this.nextChildIndex = nextChildIndex;
		}

		public Node getNode() {
			return node;
		}
		
		/**
		 * @return The next child of the current node, 
		 *         null if there's no next child.
		 */
		public Node nextChild() {
			Node[] children = node.getSuccessors();
			if (children != null && nextChildIndex < children.length)
				return node.getSuccessors()[nextChildIndex++];
			else
				return null;
		}
	}

}

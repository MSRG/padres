package ca.utoronto.msrg.padres.broker.router.matching.rete.retenetwork;

import java.io.Serializable;

import ca.utoronto.msrg.padres.common.message.Advertisement;

public class Node implements Serializable {

	private static final long serialVersionUID = 1L;

	protected final ReteNetwork reteNW;

	protected Node leftParent; // default parent for 1-input node

	protected Node rightParent;

	protected Node[] succ; // list of children nodes

	protected int nSucc; // no of children

	public Node(ReteNetwork rn) {
		reteNW = rn;
		rightParent = null;
		leftParent = null;
	}

	/**
	 * Returns the list of child nodes of the node.
	 * 
	 * @return The list of child nodes.
	 */
	public Node[] getSuccessors() {
		return succ;
	}

	/**
	 * Add a node into the children list. This method does not update anything on the provided child
	 * node {@code n}. Also, this method does not check whether the node is already in the children
	 * list. Do the test before calling this method.
	 * 
	 * @param n
	 *            Node to be added into the list.
	 */
	public void addSuccessor(Node n) {
		if (succ == null || nSucc == succ.length) {
			// increment the children list memory by 5 elements when it is full
			Node[] temp = succ;
			succ = new Node[nSucc + 5];
			if (temp != null)
				System.arraycopy(temp, 0, succ, 0, nSucc);
		}
		succ[nSucc++] = n;
	}

	/**
	 * Remove a children node from the children list of a node.
	 * 
	 * @param n
	 *            The node to be removed.
	 */
	public boolean removeSuccessor(Node n) {
		if (nSucc == 1) {
			if (succ[0].equals(n)) {
				succ = null;
			} else {
				return false;
			}
		} else {
			boolean found = false;
			int i = 0;
			while (i < nSucc & !found) {
				if (succ[i].equals(n)) {
					found = true;
				} else {
					i++;
				}
			}
			if (found) {
				Node[] temp = succ;
				succ = new Node[temp.length - 1];
				System.arraycopy(temp, 0, succ, 0, i);
				System.arraycopy(temp, i + 1, succ, i, temp.length - i - 1);
			} else {
				return false;
			}
		}
		nSucc--;
		return true;
	}

	/**
	 * Add a node into the children list if it is not already there. It is different from the
	 * {@code addSuccess} method, because the parent of the given node is set to the current node in
	 * this method.
	 * 
	 * @param n
	 *            The child node to be added.
	 * @param lOr
	 *            The index string which denotes whether this node should be the left parent or
	 *            right parent of the given child node. It should be either "L" or "R".
	 * @return If {@code n} is already in the children list, the existing entry is returned.
	 *         Otherwise {@code n} is returned.
	 * @throws ReteException
	 *             throws exception of {@code lor} string is not either "L" or "R"
	 */
	public Node mergeSuccessor(Node n, String lOr) throws ReteException {
		for (int j = 0; j < nSucc; j++) {
			Node test = succ[j];
			if (n.equals(test)) {
				return test;
			}
		}
		// confirmed that n is a new child
		// set its parent pointer
		n.setParent(this, lOr);
		// add it to the children list
		addSuccessor(n);
		return n;
	}

	/**
	 * Right activating all the children nodes passing the given message object.
	 * 
	 * @param p
	 *            The object to be passed to the children nodes. It can be either
	 *            {@link message.Publication}, {@link message.Subscription}, or
	 *            {@link Advertisement}.
	 */
	public void passAlong(Object p, int matchCount) {
		for (int j = 0; j < nSucc; j++) {
			Node s = succ[j];
			s.callNodeRight(p, matchCount);
		}
	}

	/**
	 * Left activate the node passing the message object {@code p}. This method is better extended
	 * in the children nodes, because, if not, it will always return {@code false}.
	 * 
	 * @param p
	 *            The object to be passed to the children nodes. It can be either
	 *            {@link message.Publication}, {@link message.Subscription}, or
	 *            {@link Advertisement}.
	 * @return {@code true} if success; {@code false} if not successful.
	 */
	public boolean callNodeLeft(Object p, int matchCount) {
		return false;
	}

	/**
	 * Right activate the node passing the message object {@code p}. This method is better extended
	 * in the children nodes, because, if not, it will always return {@code false}.
	 * 
	 * @param p
	 *            The object to be passed to the children nodes. It can be either
	 *            {@link message.Publication}, {@link message.Subscription}, or
	 *            {@link Advertisement}.
	 * @return {@code true} if success; {@code false} if not successful.
	 */
	public boolean callNodeRight(Object p, int matchCount) {
		return false;
	}

	/**
	 * Sets the left or right parent of the node depending on the given index string.
	 * 
	 * @param n
	 *            The new parent node.
	 * @param lOr
	 *            The index string. It can be either "L" or "R".
	 * @throws ReteException
	 *             throws exception if the index string not either "L" or "R".
	 */
	public void setParent(Node n, String lOr) throws ReteException {
		if (lOr.equals("L")) {
			leftParent = n;
		} else {
			if (lOr.equals("R")) {
				rightParent = n;
			} else {
				throw new ReteException("Error in setting parent; bad index string!");
			}
		}
	}

	/**
	 * Returns the left or right parent specified by the {@code lor} string.
	 * 
	 * @param lor
	 *            Either "L" or "R" specifying the interested parent node.
	 * @return The parent node from the specified side.
	 */
	public Node getParent(String lor) {
		if (lor.equals("L")) {
			return leftParent;
		}

		if (lor.equals("R")) {
			return rightParent;
		}

		return null;
	}

	public boolean isInMemory(String pubID) {
		if (succ != null) {
			for (int i = 0; i < nSucc; i++) {
				if (succ[i].isInMemory(pubID))
					return true;
			}
		}
		return false;
	}

	public String toString() {
		return this.getClass().getSimpleName();
	}

}

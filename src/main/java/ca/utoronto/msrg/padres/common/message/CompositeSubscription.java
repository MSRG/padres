// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 23-Jul-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author gli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CompositeSubscription implements Serializable {
	public static final long serialVersionUID = 1;

	private static Logger exceptionLogger = Logger.getLogger("Exception");

	private String csString = "";

	private String subscriptionID;

	private Serializable payload;
	
	private CompositeNode treeRoot;

	private Map<String, Subscription> subMap;

	private int sNum;

	private CompositeNode proPointer;

	// TODO: figure out how to store AND, OR, NOT between predicates
	// private Map predicateMap;

	/**
	 * Constructor. Initialize predicate map and set expiry to none.
	 */
	public CompositeSubscription() {
		subscriptionID = "";

		treeRoot = null;
		subMap = new HashMap<String, Subscription>();
		// desMap = new HashMap();
		sNum = 0;
		proPointer = null;
	}

	
	public void addSubscription(String key, Subscription atomicSubscription) {
		subMap.put(key, atomicSubscription);
	}
	
	/**
	 * Constructor for building Subscription from stringified version
	 * 
	 * @param stringRep
	 *            The string representation of the Subscription
	 */
	public CompositeSubscription(String stringRep) {
		this();
		try {
			// MessageParser mp = new MessageParser(stringRep);
			new MessageParser(MessageParser.ParserType.SUB_PARSER, stringRep);

			csString = stringRep;

			CompositeNode newNode;
			int pre_index = 0;
			int post_index = 0;
			String stringRepPro = stringRep.trim();
			if (treeRoot == null) {
				treeRoot = new CompositeNode();
				proPointer = treeRoot;
			}
			pre_index = stringRepPro.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_LEFT_PARENTHSIS);
			stringRepPro = stringRepPro.substring(pre_index + 1);
			while (!(stringRepPro.equals(""))) {

				pre_index = stringRepPro.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_LEFT_PARENTHSIS);
				post_index = stringRepPro.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_RIGHT_PARENTHSIS);
				int next_pre_index = stringRepPro.indexOf(
						CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_LEFT_PARENTHSIS,
						pre_index + 1);

				if (pre_index < post_index) {

					newNode = new CompositeNode();
					if (proPointer.leftNode == null) {
						proPointer.leftNode = newNode;
					} else if (proPointer.rightNode == null) {
						proPointer.rightNode = newNode;
					}

					newNode.setParentNode(proPointer);
					proPointer = newNode;

					if (next_pre_index > post_index) {

						String sub = stringRepPro.substring(pre_index + 1, post_index);
						String op;
						op = stringRepPro.substring(post_index + 1, next_pre_index);
						Subscription atomicSubscription = MessageFactory.createSubscriptionFromString(sub);
						sNum++;
						String key = "s" + sNum;
						subMap.put(key, atomicSubscription);
						proPointer.content = key;

						proPointer = proPointer.parentNode;

						if (op.trim().equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)
								|| op.trim().equals(
										CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)) {

							proPointer.setContent(op);
						} else {
							int count = 0;
							while (!(op.trim().equals(
									CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)
									|| op.trim().equals(
											CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND) || op.equals(""))) {
								count++;
								int index = op.indexOf(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_RIGHT_PARENTHSIS);
								op = op.substring(index + 1);
							}
							for (int i = 0; i < count; i++) {
								proPointer = proPointer.parentNode;

							}
							if (proPointer.content == null) {
								proPointer.setContent(op);
							} else {
								proPointer = proPointer.parentNode;
								proPointer.setContent(op);
							}

						}

					}
					if (next_pre_index == -1) {
						String sub = stringRepPro.substring(pre_index + 1, post_index);

						Subscription atomicSubscription = MessageFactory.createSubscriptionFromString(sub);
						sNum++;
						String key = "s" + sNum;
						subMap.put(key, atomicSubscription);
						proPointer.content = key;
						stringRepPro = "";
					} else {
						stringRepPro = stringRepPro.substring(next_pre_index);
					}

				}
			}
		} catch (ParseException e) {
			exceptionLogger.error("Failed to parse the composite subscription: " + e);
		}
	}

	/**
	 * @return
	 */
	public Serializable getPayload() {
		return payload;
	}

	/**
	 * @param object
	 */
	public void setPayload(Serializable object) {
		payload = object;
	}
	
	/**
	 * @return
	 */
	public String getSubscriptionID() {
		return subscriptionID;
	}

	/**
	 * @param subscriptionID
	 */
	public void setSubscriptionID(String subscriptionID) {
		this.subscriptionID = subscriptionID;
	}

	public int getAtomicSubscriptionNumber() {
		return sNum;
	}

	public Subscription getAtomicSubscription(String subName) {
		return subMap.get(subName);
	}

	public Map<String, Subscription> getSubscriptionMap() {
		return subMap;
	}

	// public Set getDestinationMap(String subName) {
	// return (Set) desMap.get(subName);
	// }
	//
	// public void setDestinationMap(String subName, Set desSet) {
	// desMap.put(subName, desSet);
	// }
	//
	public CompositeNode getRoot() {
		return treeRoot;
	}
	
	public void setRoot(CompositeNode treeRoot) {
		this.treeRoot = treeRoot;
	}

	/*
	 * Returns a string representation of the Subscription in the following
	 * form: "[attr1,op1,value1][attr2,op2,value2]..."
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */

	private String rootToString(CompositeNode root) {

		String rootToString = "";

		if (root.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_AND)
				|| root.content.equals(CompositeSubscriptionOPs.COMPOSIT_SUBSCRIPTION_OR)) {

			rootToString = rootToString(root.leftNode) + " " + root.content + " "
					+ rootToString(root.rightNode);
		} else {
			Subscription sub = subMap.get(root.content);
			rootToString = sub.toString();
		}

		return rootToString;

	}

	public CompositeSubscription duplicate() {
		CompositeSubscription newCS = new CompositeSubscription();
		newCS.csString = this.csString;
		newCS.subscriptionID = this.subscriptionID;
		newCS.treeRoot = this.treeRoot.duplicate();
		for (Entry<String, Subscription> subEntry : subMap.entrySet()) {
			newCS.subMap.put(subEntry.getKey(), subEntry.getValue().duplicate());
		}
		newCS.sNum = this.sNum;
		newCS.proPointer = this.proPointer.duplicate();
		return newCS;
	}

	public String toString() {
		CompositeNode root = this.treeRoot;

		//rootToString(root);

		//return csString;
		
		return rootToString(root);
	}

}

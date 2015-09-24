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

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Subscription implements Serializable {

	public static final long serialVersionUID = 1;

	private static Logger messageLogger = Logger.getLogger("Message");

	private static Logger exceptionLogger = Logger.getLogger("Exception");

	private String subscriptionID;

	// TODO: figure out how to store AND, OR, NOT between predicates
	private Map<String, Predicate> predicateMap;

	/**
	 * Constructor. Initialize predicate map and set expiry to none.
	 */
	public Subscription() {
		subscriptionID = "";
		predicateMap = Collections.synchronizedMap(new HashMap<String, Predicate>());
	}

	/**
	 * Constructor for building Subscription from stringified version
	 * 
	 * @param stringRep
	 *            The string representation of the Subscription
	 */
	private Subscription(String stringRep) {
		this();
		try {
			// MessageParser mp = new MessageParser(stringRep + ";");
			new MessageParser(stringRep + ";");
			StreamTokenizer st = new StreamTokenizer(new StringReader(stringRep));

			st.quoteChar('"');
			st.parseNumbers();
			st.whitespaceChars('[', '[');
			st.whitespaceChars(']', ']');
			st.whitespaceChars(',', ',');
			st.wordChars('<', '<');
			st.wordChars('=', '=');
			st.wordChars('>', '>');
			st.wordChars('_', '_');
			st.wordChars('-', '-');
			st.wordChars(':', ':');
			st.wordChars('$', '$');

			// parse string into predicates and insert into predicateMap
			st.nextToken();
			while (st.ttype != StreamTokenizer.TT_EOF) {
				Predicate p = new Predicate();
				String attribute = st.sval;
				st.nextToken();
				p.setOp(st.sval);
				st.nextToken();
				switch (st.ttype) {
				case StreamTokenizer.TT_WORD:
				case '"':
				case '\'':
				case '$':
					p.setValue(st.sval);
					break;
				case StreamTokenizer.TT_NUMBER:
					Long num = new Long(Math.round(st.nval));
					if (st.nval == num.doubleValue()) {
						// number is a integer
						p.setValue(num);
					} else {
						p.setValue(new Double(st.nval));
					}
					break;
				}
				addPredicate(attribute, p);
				st.nextToken();
			}
		} catch (IOException e) {
			messageLogger.error("Failed to convert a message to a subscription: " + e);
			exceptionLogger.error("Failed to convert a message to a subscription: " + e);
		} catch (ParseException e) {
			messageLogger.error("Failed to parse the subscription: " + e);
			exceptionLogger.error("Failed to parse the subscription: " + e);
		}
	}

	/**
	 * @return
	 */
	public Map<String, Predicate> getPredicateMap() {
		return predicateMap;
	}

	public String getClassVal() {
		Predicate p = predicateMap.get("class");
		if (p == null)
			return null;
		return (String) p.getValue();
	}

	/**
	 * 
	 * @param predicateMap
	 */
	public void setPredicateMap(Map<String, Predicate> predicateMap) {
		this.predicateMap = predicateMap;
	}

	/**
	 * @return
	 */
	public Subscription addPredicate(String attribute, Predicate p) {
		predicateMap.put(attribute, p);
		return this;
	}

	/**
	 * @param set
	 */
	public Predicate removePredicate(String attribute) {
		return predicateMap.remove(attribute);
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

	public boolean isValid() {
		for (Predicate testPredicate : predicateMap.values()) {
			if (!testPredicate.isValid()) {
				return false;
			}
		}

		return true;
	}

	public Subscription duplicate() {
		Subscription newSub = new Subscription();
		newSub.subscriptionID = this.subscriptionID;
		for (Entry<String, Predicate> predicateEntry : predicateMap.entrySet()) {
			newSub.predicateMap.put(predicateEntry.getKey(), predicateEntry.getValue().duplicate());
		}
		return newSub;
	}

	/*
	 * Returns a string representation of the Subscription in the following form:
	 * "[attr1,op1,value1][attr2,op2,value2]..."
	 * 
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String stringRep = "[class,eq," + getClassVal() + "]";
		synchronized (predicateMap) {
			for (String attribute : predicateMap.keySet()) {
				if (attribute.equalsIgnoreCase("class"))
					continue;

				Predicate p = predicateMap.get(attribute);

				stringRep += ",[" + attribute + "," + p.getOp() + ",";
				if ((p.getValue().getClass()).equals(String.class)
						|| (p.getValue().getClass()).equals(Date.class)) {
					stringRep += "\"" + p.getValue() + "\"";
				} else {
					stringRep += p.getValue();
				}
				stringRep += "]";
			}
		}

		return stringRep;
	}

	public boolean equalsPredicates(Object o) {
		if (!(o instanceof Subscription))
			return false;
		Subscription anotherSub = (Subscription) o;
		// check the sizes of the predicate maps
		if (predicateMap.size() != anotherSub.predicateMap.size()) {
			return false;
		} else {
			for (String attribute : predicateMap.keySet()) {
				// check the existence of the same attribute in the predicate maps
				if (!anotherSub.predicateMap.containsKey(attribute)) {
					return false;
				} else {
					// check for the equivalence of the predicates
					Predicate p1 = predicateMap.get(attribute);
					Predicate p2 = anotherSub.predicateMap.get(attribute);
					if (!p1.getOp().equals(p2.getOp()) || !p1.getValue().equals(p2.getValue()))
						return false;
				}
			}
		}
		return true;
	}

}

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
 * A PADRES Advertisement.
 * 
 * @author eli
 */
public class Advertisement implements Serializable {

	public static final long serialVersionUID = 1;

	private String advID;

	private Map<String, Predicate> predicateMap;

	private static Logger messageLogger = Logger.getLogger("Message");

	private static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Constructor. Initialize predicate map.
	 */
	public Advertisement() {
		advID = "";
		predicateMap = Collections.synchronizedMap(new HashMap<String, Predicate>());
	}

	/**
	 * @return The ID of this Advertisement.
	 */
	public String getAdvID() {
		return advID;
	}

	/**
	 * @param advID
	 *            The new ID for this Advertisement.
	 */
	public void setAdvID(String advID) {
		this.advID = advID;
	}

	/**
	 * Constructor for building Advertisement from stringified version
	 * 
	 * @param stringRep
	 *            The string representation of the Advertisement
	 * @throws ParseException
	 */
	public Advertisement(String stringRep) {
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

			// parse string into predicates and insert into predicateMap
			while (st.ttype != StreamTokenizer.TT_EOF) {
				Predicate p = new Predicate();
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;
				String attribute = st.sval;
				st.nextToken();
				p.setOp(st.sval);
				st.nextToken();
				switch (st.ttype) {
				case StreamTokenizer.TT_WORD:
				case '"':
				case '\'':
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
			}
		} catch (IOException e) {
			messageLogger.error("Failed to convert a message to a advertisement: " + e);
			exceptionLogger.error("Failed to convert a message to a advertisement: " + e);
		} catch (ParseException e) {
			messageLogger.error("Failed to parse the advertisement: " + e);
			exceptionLogger.error("Failed to parse the advertisement: " + e);
		}
	}

	/**
	 * @return The set of Predicates in the Advertisement.
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
	 * Set the predicates to the supplied Map.
	 * 
	 * @param predicateMap
	 *            The new set of Predicates for the Advertisement.
	 */
	public void setPredicateMap(Map<String, Predicate> predicateMap) {
		this.predicateMap = predicateMap;
	}

	/**
	 * Add a predicate to the Advertisement.
	 * 
	 * @param attribute
	 *            The attribute for the Predicate.
	 * @param p
	 *            The Predicate to add.
	 */
	public Advertisement addPredicate(String attribute, Predicate p) {
		predicateMap.put(attribute, p);
		return this;
	}

	/**
	 * Remove a Predicate from the Advertisement.
	 * 
	 * @param attribute
	 *            The attribute identifying the Predicate to remove.
	 * @return The removed Predicate. null if the Predicate is not found.
	 */
	public Predicate removePredicate(String attribute) {
		return predicateMap.remove(attribute);
	}

	/**
	 * Check if the Advertisement is valid.
	 * 
	 * @return True if the Advertisement is valid. False otherwise.
	 */
	public boolean isValid() {
		for (Predicate testPredicate : predicateMap.values()) {
			if (!testPredicate.isValid()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This is a method that is meant to test if an advertisement string is well formed
	 * 
	 * @param wfString
	 * @return
	 */
	/**
	 * <code>
	 private boolean wellFormedString(String wfString) {
	 if (wfString.indexOf("class") != 1)
	 return false;
	 return false;
	 }
	 </code>
	 */

	/**
	 * Returns a string representation of the Advertisement in the following form:
	 * "[attr1,op1,value1],[attr2,op2,value2],..."
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

	public boolean equalPredicates(Object o) {
		if (!(o instanceof Advertisement))
			return false;
		Advertisement anotherAdv = (Advertisement) o;
		// check the sizes of the predicate maps
		if (predicateMap.size() != anotherAdv.predicateMap.size()) {
			return false;
		} else {
			for (String attribute : predicateMap.keySet()) {
				// check the existence of the same attribute in the predicate maps
				if (!anotherAdv.predicateMap.containsKey(attribute)) {
					return false;
				} else {
					// check for the equivalence of the predicates
					Predicate p1 = predicateMap.get(attribute);
					Predicate p2 = anotherAdv.predicateMap.get(attribute);
					if (!p1.getOp().equals(p2.getOp()) || !p1.getValue().equals(p2.getValue()))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Construct a duplicate of the current Advertisement.
	 * 
	 * @return A duplicate of the current Advertisement.
	 */
	public Advertisement duplicateOld() {
		Advertisement newAdvertisement = new Advertisement(this.toString());
		return newAdvertisement;
	}

	public Advertisement duplicate() {
		Advertisement newAdv = new Advertisement();
		newAdv.advID = this.advID;
		for (Entry<String, Predicate> predicateEntry : predicateMap.entrySet()) {
			newAdv.predicateMap.put(predicateEntry.getKey(), predicateEntry.getValue().duplicate());
		}
		return newAdv;
	}

	/**
	 * Returns a string representation of an advertisement given a string representation of a
	 * publication, without the header information, such as: [class,STOCK],[symbol,YHOO],[value,1]
	 * 
	 * Assumes user does not use "'" within text predicates
	 */
	public static Advertisement toAdvertisement(String publication) {
		boolean insideQuote = false;
		boolean insideSquareBrackets = false;
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < publication.length(); i++) {
			char currentChar = publication.charAt(i);

			if (currentChar == '[' && !insideQuote) {
				insideSquareBrackets = true;
			} else if (currentChar == ']' && !insideQuote) {
				insideSquareBrackets = false;
			} else if (currentChar == '\'') {
				insideQuote = insideQuote ? false : true;
			} else if (currentChar == ',' && !insideQuote && insideSquareBrackets) {
				result.append(",isPresent");
			}

			result.append(currentChar);
		}

		// make [class,isPresent,.... to [class,eq,.....
		String classEqResult = result.toString();
		classEqResult = classEqResult.replaceAll("class,isPresent,", "class,eq,");

		return new Advertisement(classEqResult);
	}
}
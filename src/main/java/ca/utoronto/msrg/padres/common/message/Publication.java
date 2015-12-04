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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.parser.MessageParser;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Publication implements Serializable {

	public static final long serialVersionUID = 1;

	private String pubID;

	private Date timeStamp;

	private Map<String, Serializable> pairMap;

	private Serializable payload;

	private static final String PRE_PUB_TIME_STAMP_PATTERN = "];";

	static Logger messageLogger = Logger.getLogger("Message");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public Publication() {
		pubID = "";
		timeStamp = new Date();
		pairMap = Collections.synchronizedMap(new HashMap<String, Serializable>());
		payload = null;
	}

	public Publication duplicate() {
		Publication newPublication = new Publication();
		newPublication.pubID = this.pubID;
		newPublication.timeStamp = this.timeStamp;
		newPublication.pairMap.putAll(this.pairMap);
		newPublication.payload = this.payload;
		return newPublication;
	}

	/**
	 * @return
	 */
	public String getPubID() {
		return pubID;
	}

	/**
	 * @param pubID
	 */
	public void setPubID(String pubID) {
		this.pubID = pubID;
	}

	public void setTimeStamp(Date t) {
		timeStamp = t;
	}

	/**
	 * Constructor for building Subscription from stringified version.
	 * 
	 * Note that the payload is not included in the string representation.
	 * 
	 * @param stringRep
	 *            The string representation of the Publication
	 */
	private Publication(String stringRep) {
		this();
		try {
			// MessageParser mp = new MessageParser(stringRep + ";");
			new MessageParser(stringRep + ";");
			StreamTokenizer st = new StreamTokenizer(new StringReader(stringRep));
			st.quoteChar('"');
			st.parseNumbers();
			// st.ordinaryChars('0', '9');
			st.whitespaceChars('[', '[');
			st.whitespaceChars(']', ']');
			st.whitespaceChars(',', ',');
			st.ordinaryChar(';');
			st.wordChars('_', '_');
			st.wordChars('(', '(');
			st.wordChars(')', ')');
			st.wordChars('{', '{');
			st.wordChars('}', '}');

			// st.wordChars('0', '9');
			// parse string into predicates and insert into predicateMap
			while (st.ttype != StreamTokenizer.TT_EOF) {
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF || st.ttype == ';')
					break;
				String attribute = st.sval;
				st.nextToken();
				Serializable value = null;
				switch (st.ttype) {
				case StreamTokenizer.TT_WORD:
				case '"':
				case '\'':
					value = st.sval;
					break;
				case StreamTokenizer.TT_NUMBER:
					Long num = new Long(Math.round(st.nval));
					if (st.nval == num.doubleValue()) {
						// number is a integer
						value = num;
					} else {
						value = new Double(st.nval);
					}
					break;
				}
				addPair(attribute, value);
			}
		} catch (IOException e) {
			messageLogger.error("Failed to convert a message to a publication: " + e);
			exceptionLogger.error("Failed to convert a message to a publication: " + e);
		} catch (ParseException e) {
			messageLogger.error("Failed to parse the publication: " + e);
			exceptionLogger.error("Failed to parse the publication: " + e);
		}

		// Overwrite the date that we have just initialized for timeStamp with the provided value in
		// stringRep if it exists. stringRep will not have the timeStamp in it if the publication is
		// created by a client publisher
		if (stringRep.indexOf(PRE_PUB_TIME_STAMP_PATTERN) > 0) {
			String strTimeStamp = stringRep.substring(stringRep.indexOf(PRE_PUB_TIME_STAMP_PATTERN)
					+ PRE_PUB_TIME_STAMP_PATTERN.length());
			SimpleDateFormat timeStampParser = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			try {
				if (strTimeStamp != null && strTimeStamp.length() > 0)
					timeStamp = timeStampParser.parse(strTimeStamp);
			} catch (Exception e) {
				System.err.println("Publication's time stamp String is unparseable. "
						+ "Using current time as new value for timeStamp");
			}
		}
	}

	/*
	 * Returns a string representation of the Publication in the following form:
	 * "[attr1,value1],[attr2,value2];Sat Jul 08 18:38:47 EDT 2006"
	 * 
	 * Note that the payload is not included in the string representation.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		// 1. Show the predicates
		String stringRep = "[class," + getClassVal() + "]";

		synchronized (pairMap) {
			for (String attribute : pairMap.keySet()) {
				if (attribute == null)
					break;
				if (attribute.equalsIgnoreCase("class"))
					continue;

				Object value = pairMap.get(attribute);

				stringRep += ",[" + attribute + ",";
				if ((value.getClass()).equals(String.class)
						|| (value.getClass()).equals(Date.class)) {
					stringRep += "\"" + value + "\"";
				} else {
					stringRep += value;
				}
				stringRep += "]";
			}
		}

		// 2. Show the publication's time stamp
		stringRep += ";" + timeStamp;

		return stringRep;
	}

	public boolean equalVals(Object o) {
		if (!(o instanceof Publication))
			return false;
		Publication anotherPub = (Publication) o;
		// check the sizes of the pair maps
		if (pairMap.size() != anotherPub.pairMap.size()) {
			return false;
		} else {
			for (String attribute : pairMap.keySet()) {
				// check the existence of the same attribute in the pair maps
				if (!anotherPub.pairMap.containsKey(attribute)) {
					return false;
				} else {
					// check for the equivalence of the predicates
					Serializable value1 = pairMap.get(attribute);
					Serializable value2 = anotherPub.pairMap.get(attribute);
					if (!value1.equals(value2))
						return false;
				}
			}
		}
		return true;
	}

	public boolean equals(Publication otherPub) {
		return ((Publication) otherPub).toString().equals(this.toString());
	}

	/**
	 * @return
	 */
	public Map<String, Serializable> getPairMap() {
		return pairMap;
	}

	public String getClassVal() {
		return (String) pairMap.get("class");
	}

	/**
	 * @param set
	 */
	public void setPairMap(Map<String, Serializable> map) {
		pairMap = map;
	}

	/**
	 * 
	 * @param attribute
	 * @param value
	 */
	public Publication addPair(String attribute, Serializable value) {
		pairMap.put(attribute, value);
		
		return this;
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
	 * Useful for putting this publication object in a hashmap
	 */
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * 
	 * @return
	 */
	public Date getTimeStamp() {
		return timeStamp;
	}

}

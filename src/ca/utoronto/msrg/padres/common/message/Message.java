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
 * Created on Jul 7, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.IOException;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public abstract class Message implements Serializable, Comparable<Message> {

	public static final long serialVersionUID = 1;

	// for nextHopId if unset.
	protected final static MessageDestination DEFAULT_DEST = new MessageDestination("none",
			DestinationType.NULL);

	protected static final int UNLIMITED_TTL = -999;

	protected MessageType type;

	protected String messageID;

	protected MessageDestination lastHopID, nextHopID;

	protected int ttl; // number of neighbors this message to reach down a chain

	protected Date messageTime;

	protected short priority;

	protected String traceRouteID;

	// The last borker that touch this message before it got to this broker
	protected String previousBrokerID;

	// The last client that touch this message before it got to this broker
	protected String previousClientID;

	protected static Logger messageLogger = Logger.getLogger("Message");

	protected static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Constructor with no default values
	 * 
	 * @param type
	 * @param messageID
	 * @param lastHopID
	 * @param priority
	 */
	protected Message(MessageType type, String messageID, MessageDestination lastHopID,
			short priority) {
		this.type = type;
		this.messageID = messageID;
		this.lastHopID = lastHopID;
		this.nextHopID = DEFAULT_DEST;
		ttl = UNLIMITED_TTL;
		this.priority = priority;
		this.messageTime = new Date();
		traceRouteID = "dummy";
		previousBrokerID = "dummy";
		previousClientID = "dummy";
	}

	/**
	 * Constructor with default priority=0
	 * 
	 * @param type
	 * @param messageID
	 * @param lastHopID
	 */
	protected Message(MessageType type, String messageID, MessageDestination lastHopID) {
		this(type, messageID, lastHopID, (short) 0);
	}

	/**
	 * Constructor with default priority=0
	 * 
	 * @param type
	 * @param messageID
	 */
	protected Message(MessageType type, String messageID) {
		this(type, messageID, new MessageDestination("none", DestinationType.NULL), (short) 0);
	}

	/**
	 * Constructor with default values
	 * 
	 * @param type
	 */
	protected Message(MessageType type) {
		this(type, "");
	}
	
	protected Message(String message) {
		traceRouteID = "dummy";
		previousBrokerID = "dummy";
		previousClientID = "dummy";

		StreamTokenizer messageTokenizer = new StreamTokenizer(new StringReader(message));
		messageTokenizer.whitespaceChars('(', '(');
		messageTokenizer.whitespaceChars(')', ')');
		messageTokenizer.whitespaceChars('=', '=');
		messageTokenizer.whitespaceChars(',', ',');
		messageTokenizer.whitespaceChars(':', ':');
		messageTokenizer.wordChars('-', '-');
		messageTokenizer.wordChars('_', '_');
		messageTokenizer.wordChars('@', '@');
		messageTokenizer.ordinaryChars('0', '9');
		messageTokenizer.wordChars('0', '9');
		messageTokenizer.quoteChar('\'');
		messageTokenizer.parseNumbers();

		try {
			messageTokenizer.nextToken();
			type = MessageType.getMessageType(messageTokenizer.sval);

			messageTokenizer.nextToken();

			// This is to catch if the message ID is entirely numerical, which it shouldn't be, as
			// we parse numbers as characters
			if (messageTokenizer.ttype == StreamTokenizer.TT_NUMBER) {
				Double numberValue = new Double(messageTokenizer.nval);
				messageID = numberValue.toString();
			} else if (messageTokenizer.ttype == StreamTokenizer.TT_WORD) {
				messageID = messageTokenizer.sval;
			}

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			lastHopID = new MessageDestination(messageTokenizer.sval);

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			nextHopID = new MessageDestination(messageTokenizer.sval);

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			ttl = (int) messageTokenizer.nval;

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			SimpleDateFormat messageTimeTemp = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			ParsePosition pos = new ParsePosition(0);
			messageTime = messageTimeTemp.parse(messageTokenizer.sval, pos);

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			priority = new Double(messageTokenizer.nval).shortValue();

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			traceRouteID = messageTokenizer.sval;

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			previousBrokerID = messageTokenizer.sval;

			messageTokenizer.nextToken();
			messageTokenizer.nextToken();
			previousClientID = messageTokenizer.sval;

		} catch (IOException e) {
			messageLogger.error("Failed to create a message from a string: " + e);
			exceptionLogger.error("Failed to create a message from a string: " + e);
		}
	}

	/**
	 * @return
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * @return
	 */
	public MessageDestination getLastHopID() {
		return lastHopID;
	}

	/**
	 * @return
	 */
	public String getMessageID() {
		return messageID;
	}

	/**
	 * @return
	 */
	public Date getMessageTime() {
		return messageTime;
	}

	public String getTraceRouteID() {
		return traceRouteID;
	}

	public String getPrevBrokerID() {
		return previousBrokerID;
	}

	public String getPrevClientID() {
		return previousClientID;
	}

	/**
	 * @return
	 */
	public MessageDestination getNextHopID() {
		return nextHopID;
	}

	/**
	 * @param nextHopID
	 */
	public void setNextHopID(MessageDestination nextHopID) {
		this.nextHopID = nextHopID;
	}

	/**
	 * @param string
	 */
	public Message setLastHopID(MessageDestination lastHopID) {
		this.lastHopID = lastHopID;
		
		return this;
	}

	/**
	 * @param string
	 */
	public void setMessageID(String string) {
		messageID = string;
	}

	/**
	 * @param date
	 */
	public void setMessageTime(Date date) {
		messageTime = date;
	}

	/**
	 * @return
	 */
	public short getPriority() {
		return priority;
	}

	/**
	 * @param s
	 */
	public void setPriority(short s) {
		priority = s;
	}

	public void setTraceRouteID(String id) {
		traceRouteID = id;
	}

	public Message setPrevBrokerID(String id) {
		previousBrokerID = id;
		
		return this;
	}

	public void setPrevClientID(String id) {
		previousClientID = id;
	}

	/**
	 * Compare Message's by comparing priority, messageTime, messageID.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Message msg) throws ClassCastException {
		if (msg == this)
			return 0;

		if (priority < msg.priority) {
			return -1;
		} else if (priority > msg.priority) {
			return 1;
		} else {
			int timeCompare = messageTime.compareTo(msg.messageTime);
			if (timeCompare != 0)
				return timeCompare;

			int IDCompare = messageID.compareTo(msg.messageID);
			if (IDCompare != 0)
				return IDCompare;
		}
		return 1; // arbitrary default
	}

	public String toString() {
		return type + "(" + messageID + "):lasthop=" + lastHopID + ",nexthop=" + nextHopID
				+ ",TTL=" + ttl + ",time='" + messageTime + "',priority=" + priority
				+ ",TraceRouteID='" + traceRouteID + "',PreviousBroker='" + previousBrokerID
				+ "',PreviousClient='" + previousClientID + "'";
	}

	public abstract Message duplicate();

	public HashMap<String, String> getAllHeaderFields() {
		HashMap<String, String> allHeaderFields = new HashMap<String, String>();
		// ConcurrentHashMap does not accept nulls
		String lasthopString = "";
		String nexthopString = "";
		String messageTimeString = "";
		String priorityString = new Integer(getPriority()).toString();
		String traceRouteIDString = "";
		String prevBrokerIDString = "";
		// String ttlString = new Integer(getTTL()).toString();
		String ttlString = "0";

		if (getLastHopID() != null)
			lasthopString = getLastHopID().toString();
		if (getNextHopID() != null)
			nexthopString = getNextHopID().toString();
		if (getMessageTime() != null)
			messageTimeString = getMessageTime().toString();
		// if (getPriority() != null) priorityString = new
		// Integer(getPriority()).toString();
		if (getTraceRouteID() != null)
			traceRouteIDString = getTraceRouteID();
		if (getPrevBrokerID() != null)
			prevBrokerIDString = getPrevBrokerID();

		allHeaderFields.put(getType().toString(), getMessageID());
		allHeaderFields.put(MessageResources.F_LAST_HOP, lasthopString);
		allHeaderFields.put(MessageResources.F_NEXT_HOP, nexthopString);
		allHeaderFields.put(MessageResources.F_TIME, messageTimeString);
		allHeaderFields.put(MessageResources.F_PRIORITY, priorityString);
		allHeaderFields.put(MessageResources.F_TRACE_ROUTE_ID, traceRouteIDString);
		allHeaderFields.put(MessageResources.F_PREVIOUS_BROKER_ID, prevBrokerIDString);
		allHeaderFields.put(MessageResources.F_TTL, ttlString);
		return allHeaderFields;
	}

	/**
	 * @return the TTL
	 */
	public int getTTL() {
		return ttl;
	}

	/**
	 * @param ttl
	 *            the TTL to set
	 */
	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	/**
	 * Decrement TTL and check if it has expired. Returns true if TTL expired.
	 * 
	 * @return
	 */
	public boolean updateTTLAndExpired() {
		// unlimited TTL messages never expire
		if (ttl == UNLIMITED_TTL) {
			return false;
		} else {
			// Decrement TTL only if the message is forwarded to another broker. Messages routed to
			// an internal broker component, such as Controller should not have TTL decremented.
			if (nextHopID.isNeighborBroker())
				ttl--;
			return (ttl < 0);
		}
	}

	// Retrieves the broker's id given a message id
	public static String getBrokerIdFromMsgId(String msgId) {
		return (msgId.indexOf("-M") > 0) ? msgId.substring(0, msgId.indexOf("-M")) : "";
	}
}

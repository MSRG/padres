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
 * Created on 15-Jul-2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class SubscriptionMessage extends Message {

	private static final long serialVersionUID = -2859047078468681068L;

	private Subscription sub;

	private int numNotifications;

	private Date expireTime;

	private int expireNotifications;

	private boolean timedExpiry;

	private Date startTime, endTime; // for historic queries
	
	private MessageDestination sender = null; // for Total Order, Top-k

	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public SubscriptionMessage(Subscription subscription, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.SUBSCRIPTION, messageID, lastHopID);
		this.sub = subscription;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * @param messageID
	 */
	public SubscriptionMessage(Subscription subscription, String messageID) {
		super(MessageType.SUBSCRIPTION, messageID);
		this.sub = subscription;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	public SubscriptionMessage(String subMessage) throws ca.utoronto.msrg.padres.common.message.parser.ParseException {
		super(subMessage);
		this.sub = MessageFactory.createSubscriptionFromString(subMessage.substring(subMessage.lastIndexOf(":[") + 1));

		try {
			String expireTimeString = subMessage.substring(subMessage.indexOf("expiry='") + 8,
					subMessage.lastIndexOf("':["));
			SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			expireTime = timeFormat.parse(expireTimeString);
		} catch (ParseException pe) {
			this.setTimedExpiry(0);
		}
		try {
			String start = subMessage.substring(subMessage.indexOf("startTime='") + 11,
					subMessage.lastIndexOf("',endTime='"));
			SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			startTime = timeFormat.parse(start);
		} catch (ParseException pe) {
		}
		try {
			String end = subMessage.substring(subMessage.indexOf("endTime='") + 9,
					subMessage.lastIndexOf("',expiry='"));
			SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			endTime = timeFormat.parse(end);
		} catch (ParseException pe) {

		}
		expireNotifications = 0; // default is no expiry
		timedExpiry = false;
		numNotifications = 0;
	}

	public void setInterestTime(Date start, Date end) {
		startTime = start;
		endTime = end;
	}

	public void setStartTime(Date start) {
		startTime = start;
	}

	public void setEndTime(Date end) {
		endTime = end;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	/**
	 * @return
	 */
	public Subscription getSubscription() {
		return sub;
	}

	/**
	 * @param sub
	 */
	public void setSubscription(Subscription sub) {
		this.sub = sub;
	}

	/**
	 * Get the expiry time of the subscription message
	 * 
	 * @return The expiry time, as a Date
	 */
	public Date getTimedExpiry() {
		return expireTime;
	}

	/**
	 * @param expireTime
	 */
	public void setTimedExpiry(Date expireTime) {
		this.expireTime = expireTime;
		timedExpiry = true;
	}

	/**
	 * If you want to set the expire time with a milliseconds time value, you can
	 * 
	 * @param expireTime
	 */
	public void setTimedExpiry(long expireTime) {
		this.expireTime = new Date(expireTime);
		timedExpiry = true;
	}

	/**
	 * @param notificationLimit
	 */
	public void setNotificationsExpiry(int notificationLimit) {
		expireNotifications = notificationLimit;
		timedExpiry = false;
	}

	/**
	 * @param i
	 */
	public void incrementNotificationCount(int i) {
		numNotifications += i;
	}

	/**
	 * @return
	 */
	public boolean isExpired() {
		if (!timedExpiry) {
			// notification count expiry
			if (expireNotifications == 0 || numNotifications < expireNotifications)
				return false;
			else
				return true;
		} else {
			// timed expiry
			Date currentTime = new Date();
			if (expireTime.after(currentTime))
				return false;
			else
				return true;
		}
	}

	public String toString() {
		return super.toString() + ",startTime='" + getStartTime() + "',endTime='" + getEndTime()
				+ "',expiry='" + expireTime + "':" + sub.toString();
	}

	public SubscriptionMessage duplicate() {
		SubscriptionMessage newSubscriptionMessage = new SubscriptionMessage(this.sub.duplicate(),
				this.messageID, this.lastHopID);
		// copy general message-specific fields
		newSubscriptionMessage.nextHopID = this.nextHopID;
		newSubscriptionMessage.ttl = this.ttl;
		newSubscriptionMessage.messageTime = (Date) this.messageTime.clone();
		newSubscriptionMessage.priority = this.priority;
		newSubscriptionMessage.traceRouteID = this.traceRouteID;
		newSubscriptionMessage.previousBrokerID = this.previousBrokerID;
		newSubscriptionMessage.previousClientID = this.previousClientID;
		// copy subscription message-specific fields
		newSubscriptionMessage.numNotifications = this.numNotifications;
		newSubscriptionMessage.expireTime = (Date) this.expireTime.clone();
		newSubscriptionMessage.expireNotifications = this.expireNotifications;
		newSubscriptionMessage.timedExpiry = this.timedExpiry;
		newSubscriptionMessage.startTime = this.startTime == null ? null
				: (Date) this.startTime.clone();
		newSubscriptionMessage.endTime = this.endTime == null ? null : (Date) this.endTime.clone();
		newSubscriptionMessage.sender = this.sender;
		return newSubscriptionMessage;
	}

	public boolean equals(SubscriptionMessage subMessage) {
		return (subMessage.toString()).equals(this.toString());
	}

	public HashMap<String, String> getAllHeaderFields() {
		// ConcurrentHashMap allHeaderFields = new ConcurrentHashMap();
		HashMap<String, String> allHeaderFields = new HashMap<String, String>();
		String startTime = "";
		String endTime = "";
		String expireTime = "";

		// ConcurrentHashMap can not accept nulls
		if (getStartTime() != null)
			startTime = getStartTime().toString();
		if (getEndTime() != null)
			endTime = getEndTime().toString();
		if (getTimedExpiry() != null)
			expireTime = getTimedExpiry().toString();

		allHeaderFields.putAll(super.getAllHeaderFields());
		allHeaderFields.put(MessageResources.F_START_TIME, startTime);
		allHeaderFields.put(MessageResources.F_END_TIME, endTime);
		allHeaderFields.put(MessageResources.F_EXPIRY, expireTime);
		return allHeaderFields;
	}

	public MessageDestination getSender() {
		return sender;
	}

	public void setSender(MessageDestination sender) {
		this.sender = sender;
	}

	public boolean isControl() {
		String val = sub.getClassVal();
		return val.equals("BROKER_MONITOR") || val.equals("NETWORK_DISCOVERY") || val.equals("GLOBAL_FD") || val.equals("BROKER_CONTROL") || val.equals("HEARTBEAT_MANAGER");
	}
}

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

import java.util.Date;

/**
 * @author gli
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class CompositeSubscriptionMessage extends Message {

	private static final long serialVersionUID = 6309904202763113094L;

	private CompositeSubscription sub;

	private int numNotifications;

	private Date expireTime;

	private int expireNotifications;

	private boolean timedExpiry;

	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public CompositeSubscriptionMessage(CompositeSubscription subscription, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.COMPOSITESUBSCRIPTION, messageID, lastHopID);
		this.sub = subscription;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * @param messageID
	 */
	public CompositeSubscriptionMessage(CompositeSubscription subscription, String messageID) {
		super(MessageType.COMPOSITESUBSCRIPTION, messageID);
		this.sub = subscription;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * <code>
	public CompositeSubscriptionMessage(String subMessage) {
		super(subMessage);
		this.sub = new CompositeSubscription(subMessage.substring(subMessage.lastIndexOf(":{") + 1));

		try {
			String expireTimeString = subMessage.substring(subMessage.indexOf("expiry='") + 8,
					subMessage.lastIndexOf("':{"));
			SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			expireTime = timeFormat.parse(expireTimeString);
		} catch (ParseException pe) {
			this.setTimedExpiry(0);
		}
		expireNotifications = 0; // default is no expiry
		timedExpiry = false;
		numNotifications = 0;
	}
	</code>
	 */

	/**
	 * @return
	 */
	public CompositeSubscription getSubscription() {
		return sub;
	}

	/**
	 * @param sub
	 */
	public void setSubscription(CompositeSubscription sub) {
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
		return super.toString() + ",expiry='" + expireTime + "':" + sub.toString();
	}

	public CompositeSubscriptionMessage duplicate() {
		CompositeSubscriptionMessage newSubscriptionMessage = new CompositeSubscriptionMessage(
				this.sub.duplicate(), this.messageID, this.lastHopID.duplicate());
		// copy general message-specific fields
		newSubscriptionMessage.nextHopID = this.nextHopID;
		newSubscriptionMessage.ttl = this.ttl;
		newSubscriptionMessage.messageTime = (Date) this.messageTime.clone();
		newSubscriptionMessage.priority = this.priority;
		newSubscriptionMessage.traceRouteID = this.traceRouteID;
		newSubscriptionMessage.previousBrokerID = this.previousBrokerID;
		newSubscriptionMessage.previousClientID = this.previousClientID;
		// copy composite subscription message-specific fields
		newSubscriptionMessage.numNotifications = this.numNotifications;
		newSubscriptionMessage.expireTime = (Date) this.expireTime.clone();
		newSubscriptionMessage.expireNotifications = this.expireNotifications;
		newSubscriptionMessage.timedExpiry = this.timedExpiry;
		return newSubscriptionMessage;
	}

	public boolean equals(SubscriptionMessage subMessage) {
		return (subMessage.toString()).equals(this.toString());
	}

}

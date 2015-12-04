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
 */
package ca.utoronto.msrg.padres.common.message;

import java.util.Date;

/**
 * A PADRES Message containing an Advertisement.
 * 
 * @author eli
 */
public class AdvertisementMessage extends Message {

	private static final long serialVersionUID = -6320552460774972324L;

	private Advertisement advertisement;

	private int numNotifications;

	private Date expireTime;

	private int expireNotifications;

	private boolean timedExpiry;

	/**
	 * Constructor.
	 * 
	 * @param advertisement
	 *            The Advertisement contained in the Message.
	 * @param messageID
	 *            The messageID for this Message.
	 * @param lastHopID
	 *            The lastHopID for this Message.
	 */
	public AdvertisementMessage(Advertisement advertisement, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.ADVERTISEMENT, messageID, lastHopID);
		this.advertisement = advertisement;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * Constructor. lastHopID is set to the default.
	 * 
	 * @param advertisement
	 * @param messageID
	 */
	public AdvertisementMessage(Advertisement advertisement, String messageID) {
		super(MessageType.ADVERTISEMENT, messageID);
		this.advertisement = advertisement;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * Constructor. Build the AdvertisementMessage from a stringified version.
	 * 
	 * @param advMessage
	 *            The stringified AdvertisementMessage.
	 * 
	 *            <code>
	public AdvertisementMessage(String advMessage) {
		super(advMessage);
		advertisement = new Advertisement(advMessage.substring(advMessage.lastIndexOf(":[") + 1));

		try {
			String expireTimeString = advMessage.substring(advMessage.indexOf("expiry='") + 8,
					advMessage.lastIndexOf("':["));
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
	public Advertisement getAdvertisement() {
		return advertisement;
	}

	/**
	 * @param advertisement
	 */
	public void setAdvertisement(Advertisement advertisement) {
		this.advertisement = advertisement;
	}

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
	 * Check if the AdvertisementMessage has expired.
	 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return super.toString() + ",expiry='" + expireTime + "':" + advertisement.toString();
	}

	/**
	 * Build a duplicate of the current AdvertisementMessage.
	 * 
	 * @return A duplicate of the current AdvertisementMessage.
	 */
	public AdvertisementMessage duplicate() {
		AdvertisementMessage newAdvertisementMessage = new AdvertisementMessage(
				this.advertisement.duplicate(), this.messageID, this.lastHopID);
		// copy general message-specific fields
		newAdvertisementMessage.nextHopID = this.nextHopID;
		newAdvertisementMessage.ttl = this.ttl;
		newAdvertisementMessage.messageTime = (Date) this.messageTime.clone();
		newAdvertisementMessage.priority = this.priority;
		newAdvertisementMessage.traceRouteID = this.traceRouteID;
		newAdvertisementMessage.previousBrokerID = this.previousBrokerID;
		newAdvertisementMessage.previousClientID = this.previousClientID;
		// copy advertisement message-specific fields
		newAdvertisementMessage.numNotifications = this.numNotifications;
		newAdvertisementMessage.expireTime = (Date) this.expireTime.clone();
		newAdvertisementMessage.expireNotifications = this.expireNotifications;
		newAdvertisementMessage.timedExpiry = this.timedExpiry;
		return newAdvertisementMessage;
	}

	public boolean equals(AdvertisementMessage advMessage) {
		return (advMessage.toString()).equals(this.toString());
	}
}
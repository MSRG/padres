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
import java.util.List;
import java.util.Vector;

import ca.utoronto.msrg.padres.broker.order.TotalOrderConflictInfo;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * @author eli
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class PublicationMessage extends Message {

	private static final long serialVersionUID = -122482881067627608L;

	protected Publication publication;

	protected int numNotifications;

	// WARNING: This expiry time stuff doesn't make much sense for publications in our system! It's
	// mainly here for the JMS binding.
	// TODO: JMS binding is removed from PADRES. Should we remove this as well?
	protected Date expireTime;

	protected int expireNotifications;

	protected boolean timedExpiry;

	// Total Order
	protected TotalOrderConflictInfo conflict = null;
	
	protected boolean processed = false;
	
	protected List<String> topkSubIds = null;

	private String chunkId = null;
	
	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public PublicationMessage(Publication publication, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.PUBLICATION, messageID, lastHopID);
		publication.setPubID(messageID);
		this.publication = publication;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * @param messageID
	 */
	public PublicationMessage(Publication publication, String messageID) {
		super(MessageType.PUBLICATION, messageID);
		publication.setPubID(messageID);
		this.publication = publication;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * 
	 * @param publication
	 */
	public PublicationMessage(Publication publication) {
		super(MessageType.PUBLICATION);
		this.publication = publication;

		expireNotifications = 0; // default is no expiry
		this.setTimedExpiry(0);
		timedExpiry = false;
		numNotifications = 0;
	}

	/**
	 * 
	 * @param pubMessage
	 * @throws ca.utoronto.msrg.padres.common.message.parser.ParseException 
	 */
	public PublicationMessage(String pubMessage) throws ca.utoronto.msrg.padres.common.message.parser.ParseException {
		super(pubMessage);
		publication = MessageFactory.createPublicationFromString(pubMessage.substring(pubMessage.lastIndexOf(":[") + 1));

		try {
			String expireTimeString = pubMessage.substring(pubMessage.indexOf("expiry='") + 8,
					pubMessage.lastIndexOf("':["));
			SimpleDateFormat timeFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
			expireTime = timeFormat.parse(expireTimeString);
		} catch (ParseException pe) {
			this.setTimedExpiry(0);
		}
		expireNotifications = 0; // default is no expiry
		timedExpiry = false;
		numNotifications = 0;

	}

	/**
	 * @return
	 */
	public Publication getPublication() {
		return publication;
	}

	/**
	 * @param publication
	 */
	public void setPublication(Publication publication) {
		this.publication = publication;
	}

	/**
	 * Get the expiry time of the publication message
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

	public void setMessageID(String id) {
		super.setMessageID(id);
		publication.setPubID(id);
	}

	public String toString() {
		return super.toString() + ",expiry='" + expireTime + "':" + publication.toString();
	}

	public PublicationMessage duplicate() {
		PublicationMessage newPublicationMessage = new PublicationMessage(
				this.publication.duplicate(), this.messageID, this.lastHopID.duplicate());
		// copy general message-specific fields
		newPublicationMessage.nextHopID = this.nextHopID;
		newPublicationMessage.ttl = this.ttl;
		newPublicationMessage.messageTime = (Date) this.messageTime.clone();
		newPublicationMessage.priority = this.priority;
		newPublicationMessage.traceRouteID = this.traceRouteID;
		newPublicationMessage.previousBrokerID = this.previousBrokerID;
		newPublicationMessage.previousClientID = this.previousClientID;
		// copy publication message-specific fields
		newPublicationMessage.numNotifications = this.numNotifications;
		newPublicationMessage.expireTime = (Date) this.expireTime.clone();
		newPublicationMessage.expireNotifications = this.expireNotifications;
		newPublicationMessage.timedExpiry = this.timedExpiry;
		// copy total order fields
		if(this.conflict != null)
			newPublicationMessage.conflict = this.conflict.duplicate();
		else
			newPublicationMessage.conflict = null;
		newPublicationMessage.processed = this.processed;

		if(this.topkSubIds != null)
			newPublicationMessage.topkSubIds = new Vector<String>(this.topkSubIds);
		
		newPublicationMessage.chunkId = this.chunkId;
		
		return newPublicationMessage;
	}

	public boolean equals(PublicationMessage pubMessage) {
		return (pubMessage.toString()).equals(this.toString());
	}

	public boolean hasConflict() {
		return (conflict != null);
	}

	public TotalOrderConflictInfo getConflict() {
		return conflict;
	}
	
	public void setConflict(TotalOrderConflictInfo conflict) {
		this.conflict = conflict;
	}
	
	public void clearConflict(){
		this.conflict = null;
	}

	public boolean isControl() {
		String val = publication.getClassVal();
		return val.equals("BROKER_MONITOR") || val.equals("NETWORK_DISCOVERY") || val.equals("GLOBAL_FD") || val.equals("BROKER_CONTROL") || val.equals("HEARTBEAT_MANAGER");
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}
	
	public void addTopkSubId(String subId){
		if(topkSubIds == null){
			topkSubIds = new Vector<String>();
		}
		
		topkSubIds.add(subId);
	}
	
	public void setChunkId(String chunkId){
		this.chunkId  = chunkId;
	}
	
	public String getChunkId(){
		return this.chunkId;
	}
	
	public boolean hasTopk(){
		return topkSubIds != null;
	}
	
	public List<String> getTopkSubIds(){
		return topkSubIds;
	}

}

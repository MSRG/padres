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
 * @author eli
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */
public class UnsubscriptionMessage extends Message {

	private static final long serialVersionUID = -2079350081865536713L;

	private Unsubscription unsubscription;

	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public UnsubscriptionMessage(Unsubscription unsubscription, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.UNSUBSCRIPTION, messageID, lastHopID);
		this.unsubscription = unsubscription;
	}

	/**
	 * @param messageID
	 */
	public UnsubscriptionMessage(Unsubscription unsubscription, String messageID) {
		super(MessageType.UNSUBSCRIPTION, messageID);
		this.unsubscription = unsubscription;
	}

	/**
	 * <code>
	public UnsubscriptionMessage(String unsubStr) {
		super(unsubStr);
		unsubscription = new Unsubscription(unsubStr.substring(unsubStr.lastIndexOf(":") + 1));
	}
	</code>
	 */

	/**
	 * @return
	 */
	public Unsubscription getUnsubscription() {
		return unsubscription;
	}

	/**
	 * @param unsubscription
	 */
	public void setUnsubscription(Unsubscription unsubscription) {
		this.unsubscription = unsubscription;
	}

	public String toString() {
		return super.toString() + ":" + unsubscription.toString();
	}

	public boolean equals(UnsubscriptionMessage unsubMessage) {
		return (unsubMessage.toString()).equals(this.toString());
	}

	public UnsubscriptionMessage duplicate() {
		UnsubscriptionMessage newUnSubMsg = new UnsubscriptionMessage(
				this.unsubscription.duplicate(), this.messageID, this.lastHopID.duplicate());
		// copy general message-specific fields
		newUnSubMsg.nextHopID = this.nextHopID;
		newUnSubMsg.ttl = this.ttl;
		newUnSubMsg.messageTime = (Date) this.messageTime.clone();
		newUnSubMsg.priority = this.priority;
		newUnSubMsg.traceRouteID = this.traceRouteID;
		newUnSubMsg.previousBrokerID = this.previousBrokerID;
		newUnSubMsg.previousClientID = this.previousClientID;
		return newUnSubMsg;
	}
}

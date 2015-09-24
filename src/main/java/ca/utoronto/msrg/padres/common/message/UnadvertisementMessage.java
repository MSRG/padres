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
public class UnadvertisementMessage extends Message {

	private static final long serialVersionUID = -3057976663775101270L;

	private Unadvertisement unadvertisement;

	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public UnadvertisementMessage(Unadvertisement unadvertisement, String messageID,
			MessageDestination lastHopID) {
		super(MessageType.UNADVERTISEMENT, messageID, lastHopID);
		this.unadvertisement = unadvertisement;
	}

	/**
	 * @param messageID
	 */
	public UnadvertisementMessage(Unadvertisement unadvertisement, String messageID) {
		super(MessageType.UNADVERTISEMENT, messageID);
		this.unadvertisement = unadvertisement;
	}

	/**
	 * <code>
	public UnadvertisementMessage(String unadvStr) {
		super(unadvStr);
		unadvertisement = new Unadvertisement(unadvStr.substring(unadvStr.lastIndexOf(":") + 1));
	}
	</code>
	 */

	/**
	 * @return
	 */
	public Unadvertisement getUnadvertisement() {
		return unadvertisement;
	}

	/**
	 * @param unadvertisement
	 */
	public void setUnadvertisement(Unadvertisement unadvertisement) {
		this.unadvertisement = unadvertisement;
	}

	public String toString() {
		return super.toString() + ":" + unadvertisement.toString();
	}

	public UnadvertisementMessage duplicate() {
		UnadvertisementMessage newUnAdvertisementMessage = new UnadvertisementMessage(
				this.unadvertisement.duplicate(), this.messageID, this.lastHopID.duplicate());
		// copy general message-specific fields
		newUnAdvertisementMessage.nextHopID = this.nextHopID;
		newUnAdvertisementMessage.ttl = this.ttl;
		newUnAdvertisementMessage.messageTime = (Date) this.messageTime.clone();
		newUnAdvertisementMessage.priority = this.priority;
		newUnAdvertisementMessage.traceRouteID = this.traceRouteID;
		newUnAdvertisementMessage.previousBrokerID = this.previousBrokerID;
		newUnAdvertisementMessage.previousClientID = this.previousClientID;
		return newUnAdvertisementMessage;
	}

	public boolean equals(UnadvertisementMessage unadvMessage) {
		return (unadvMessage.toString()).equals(this.toString());
	}

}

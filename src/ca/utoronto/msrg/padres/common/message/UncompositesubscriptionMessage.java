package ca.utoronto.msrg.padres.common.message;

import java.util.Date;

/**
 * @author shuang
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code
 *         Generation>Code and Comments
 */

public class UncompositesubscriptionMessage extends Message {

	private static final long serialVersionUID = 210080570674616631L;

	private Uncompositesubscription uncompositesubscription;

	/**
	 * @param messageID
	 * @param lastHopID
	 */
	public UncompositesubscriptionMessage(Uncompositesubscription uncompositesubscription,
			String messageID, MessageDestination lastHopID) {
		super(MessageType.UNCOMPOSITESUBSCRIPTION, messageID, lastHopID);
		this.uncompositesubscription = uncompositesubscription;
	}

	/**
	 * @param messageID
	 */
	public UncompositesubscriptionMessage(Uncompositesubscription uncompositesubscription,
			String messageID) {
		super(MessageType.UNCOMPOSITESUBSCRIPTION, messageID);
		this.uncompositesubscription = uncompositesubscription;
	}

	/**
	 * @return
	 */
	public Uncompositesubscription getUncompositesubscription() {
		return uncompositesubscription;
	}

	/**
	 * @param uncompositesubscription
	 */
	public void setUncompositesubscription(Uncompositesubscription uncompositesubscription) {
		this.uncompositesubscription = uncompositesubscription;
	}

	public String toString() {
		return super.toString() + ":" + uncompositesubscription.toString();
	}

	public boolean equals(UncompositesubscriptionMessage uncsubMessage) {
		return (uncsubMessage.toString()).equals(this.toString());
	}

	public UncompositesubscriptionMessage duplicate() {
		UncompositesubscriptionMessage newUnCSMessage = new UncompositesubscriptionMessage(
				this.uncompositesubscription.duplicate(), this.messageID,
				this.lastHopID.duplicate());
		// copy general message-specific fields
		newUnCSMessage.nextHopID = this.nextHopID;
		newUnCSMessage.ttl = this.ttl;
		newUnCSMessage.messageTime = (Date) this.messageTime.clone();
		newUnCSMessage.priority = this.priority;
		newUnCSMessage.traceRouteID = this.traceRouteID;
		newUnCSMessage.previousBrokerID = this.previousBrokerID;
		newUnCSMessage.previousClientID = this.previousClientID;
		return newUnCSMessage;
	}
}

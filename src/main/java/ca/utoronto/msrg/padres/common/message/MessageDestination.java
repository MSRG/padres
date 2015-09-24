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
 * Created on 17-Jul-2003
 *
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * A destination for a PADRES message. There are some static destinations for common components.
 * 
 * @author eli
 */
public class MessageDestination implements Serializable {

	public enum DestinationType {
		NULL, BROKER, CLIENT, INTERNAL, DB;
	}

	public static final long serialVersionUID = 1;

	public static final MessageDestination INPUTQUEUE = new MessageDestination("INPUTQUEUE",
			EnumSet.of(DestinationType.BROKER, DestinationType.INTERNAL));

	public static final MessageDestination CONTROLLER = new MessageDestination("CONTROLLER",
			EnumSet.of(DestinationType.BROKER, DestinationType.INTERNAL));

	public static final MessageDestination SYSTEM_MONITOR = new MessageDestination(
			"SYSTEM_MONITOR", EnumSet.of(DestinationType.BROKER, DestinationType.INTERNAL));

	public static final MessageDestination HEARTBEAT_MANAGER = new MessageDestination(
			"HEARTBEAT_MANAGER", EnumSet.of(DestinationType.BROKER, DestinationType.INTERNAL));

	protected String destinationID;

	protected EnumSet<DestinationType> destinationTypes;

	// added for heartbeat support
	protected int failCount = 0;

	/**
	 * TODO: Eventually this constructor must be removed alongside with "removeComponent()" method
	 * and DestinationType must be exclusively used. Currently this constructor can not be removed
	 * because there are places where MessageDestination is constructed from a tokenizer created
	 * from Message.toString(). In order to remove this constructor, message construction from
	 * string should be avoided. - Maniy
	 * 
	 * @param destinationID
	 */
	public MessageDestination(String destinationID) {
		this.destinationID = destinationID.trim();
		destinationTypes = EnumSet.noneOf(DestinationType.class);
	}

	public MessageDestination(String destinationID, DestinationType destType) {
		this.destinationID = destinationID.trim();
		destinationTypes = EnumSet.of(destType);
	}

	public MessageDestination(String destinationID, EnumSet<DestinationType> destTypes) {
		this.destinationID = destinationID.trim();
		destinationTypes = destTypes.clone();
	}

	public String getDestinationID() {
		return destinationID;
	}

	public void addDestinationType(DestinationType type) {
		destinationTypes.add(type);
	}

	public void addDestinationTypes(EnumSet<DestinationType> typeSet) {
		this.destinationTypes.addAll(typeSet);
	}

	public EnumSet<DestinationType> getDestinationType() {
		return destinationTypes;
	}

	public String toString() {
		return destinationID;
	}

	/**
	 * Get the destination with one less component (hyphenated substring) than the current one.
	 * TODO: remove this method along with the MessageDestination(String id) constructor to remove
	 * the "-" character constraint on the brokerID string
	 * 
	 * @return The MessageDestination for the desired destination.
	 */
	public MessageDestination removeComponent() {
		int lastHyphen = destinationID.lastIndexOf('-');
		if (lastHyphen == -1)
			return null;
		String newDestID = destinationID.substring(0, destinationID.lastIndexOf('-'));
		return new MessageDestination(newDestID, destinationTypes);
	}

	/**
	 * Determine if this destination is a broker.
	 * 
	 * TODO: this has to be changed to use the DestinationType instead of string searching
	 * 
	 * @return True if this destination is a broker, false otherwise.
	 */
	public boolean isBroker() {
		return (destinationID.indexOf('-') == -1);
	}

	/**
	 * Different from the above isBroker(), this limits to a remote broker only. This is mainly used
	 * by the TTL
	 * 
	 * TODO: this has to be changed to use the DestinationType instead of string searching
	 * 
	 * @return
	 */
	public boolean isNeighborBroker() {
		return (destinationID.indexOf('-') == -1) && !destinationID.equals("INPUTQUEUE")
				&& !destinationID.equals("CONTROLLER") && !destinationID.equals("SYSTEM_MONITOR")
				&& !destinationID.equals("HEARTBEAT_MANAGER");
	}

	// TODO: this has to be changed to use the DestinationType instead of string searching
	public boolean isInternalQueue() {
		return (destinationID.equals("INPUTQUEUE") || destinationID.equals("CONTROLLER")
				|| destinationID.equals("SYSTEM_MONITOR") || destinationID.equals("HEARTBEAT_MANAGER"));

	}

	// TODO: this has to be changed to use the DestinationType instead of string searching
	public boolean isDB() {
		return (destinationID.indexOf("-DB") != -1);
	}

	public int hashCode() {
		return destinationID.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof MessageDestination))
			return false;
		MessageDestination checkDest = (MessageDestination) o;
		return destinationID.equals(checkDest.destinationID);
	}

	public int incrementFailCount() {
		return ++failCount;
	}

	public int decrementFailCount() {
		return --failCount;
	}

	public int getFailCount() {
		return failCount;
	}

	public int setFailCount(int failCount) {
		int oldCount = this.failCount;
		this.failCount = failCount;
		return oldCount;
	}

	/**
	 * Returns the brokerID corresponding to this destination
	 * 
	 * @return
	 */
	public String getBrokerId() {
		return isBroker() ? destinationID : destinationID.substring(0, destinationID.indexOf('-'));
	}

	public MessageDestination duplicate() {
		MessageDestination newMessageDestination = new MessageDestination(this.destinationID);
		newMessageDestination.destinationTypes = this.destinationTypes.clone();
		newMessageDestination.failCount = this.failCount;
		return newMessageDestination;
	}

	public static MessageDestination formatClientDestination(String clientID, String brokerID) {
		return new MessageDestination(String.format("%s-%s", brokerID, clientID),
				DestinationType.CLIENT);
	}
}

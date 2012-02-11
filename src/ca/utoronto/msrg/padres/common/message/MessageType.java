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
 * Created on Jul 8, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.Serializable;

/**
 * @author dmatheson
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class MessageType implements Serializable {

	private static final long serialVersionUID = -9150045721173468572L;

	private int i;

	private static final int _UNDEFINED = 0;
	
	public static final MessageType UNDEFINED = new MessageType(_UNDEFINED);

	private static final int _PUBLICATION = 1;

	public static final MessageType PUBLICATION = new MessageType(_PUBLICATION);

	private static final int _SUBSCRIPTION = 2;

	public static final MessageType SUBSCRIPTION = new MessageType(
			_SUBSCRIPTION);

	private static final int _ADVERTISEMENT = 3;

	public static final MessageType ADVERTISEMENT = new MessageType(
			_ADVERTISEMENT);

	private static final int _UNSUBSCRIPTION = 4;

	public static final MessageType UNSUBSCRIPTION = new MessageType(
			_UNSUBSCRIPTION);

	private static final int _UNADVERTISEMENT = 5;

	public static final MessageType UNADVERTISEMENT = new MessageType(
			_UNADVERTISEMENT);

	// added by gli
	private static final int _COMPOSITESUBSCRIPTION = 6;

	public static final MessageType COMPOSITESUBSCRIPTION = new MessageType(
			_COMPOSITESUBSCRIPTION);

	// added by shuang
	private static final int _UNCOMPOSITESUBSCRIPTION = 7;

	public static final MessageType UNCOMPOSITESUBSCRIPTION = new MessageType(
			_UNCOMPOSITESUBSCRIPTION);

	// added by kzhang
	private static final int _SHUTDOWN = 8;
	
	public static final MessageType SHUTDOWN = new MessageType(_SHUTDOWN);
	
	public static MessageType getMessageType(String type) {
		if (type.equalsIgnoreCase("PublicationMessage")) {
			return PUBLICATION;
		} else if (type.equalsIgnoreCase("SubscriptionMessage")) {
			return SUBSCRIPTION;
		} else if (type.equalsIgnoreCase("AdvertisementMessage")) {
			return ADVERTISEMENT;
		} else if (type.equalsIgnoreCase("UnsubscriptionMessage")) {
			return UNSUBSCRIPTION;
		} else if (type.equalsIgnoreCase("UnadvertisementMessage")) {
			return UNADVERTISEMENT;
		} else if (type.equalsIgnoreCase("CompositeSubscriptionMessage")) {
			return COMPOSITESUBSCRIPTION;
		} else if (type.equalsIgnoreCase("UncompositesubscriptionMessage")) {
			return UNCOMPOSITESUBSCRIPTION;
		} else if (type.equalsIgnoreCase("ShutdownMessage")) {
			return SHUTDOWN;
		} else {
			return UNDEFINED;
		}

	}

	public int value() {
		return i;
	}

	private MessageType(int i) {
		this.i = i;
	}

	public String toString() {
		switch (i) {
		case 1:
			return "PublicationMessage";
		case 2:
			return "SubscriptionMessage";
		case 3:
			return "AdvertisementMessage";
		case 4:
			return "UnsubscriptionMessage";
		case 5:
			return "UnadvertisementMessage";
		case 6:
			return "CompositeSubscriptionMessage";
		case 7:
			return "UncompositesubscriptionMessage";
		case 8:
			return "ShutdownMessage";
		default:
			return "UndefinedMessage";
		}
	}

	public boolean equals(MessageType mt) {
		return (i == mt.i);
	}
}

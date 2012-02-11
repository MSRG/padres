/*
 * Created on Apr 8, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

/**
 * @author Alex
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.Comparator;

public class MessageComparator implements Comparator<Message> {
	public int compare(Message a, Message b) {
		return staticCompare(a, b);
	}

	public static int staticCompare(Message a, Message b) {
		// This is necessary in order for remove() in TreeMap/TreeSet to work
		if (a.equals(b))
			return 0;

		int rankA = getMessageRanking(a);
		int rankB = getMessageRanking(b);

		// never return 0
		if (rankA != rankB)
			return (rankA > rankB) ? 1 : -1;

		return -1;
	}

	private static int getMessageRanking(Message obj) {
		Class<? extends Message> c = obj.getClass();

		if (c == SubscriptionMessage.class) {
			return 1;
		} else if (c == CompositeSubscriptionMessage.class) {
			return 2;
		} else if (c == AdvertisementMessage.class) {
			return 3;
		} else if (c == UnsubscriptionMessage.class) {
			return 4;
		} else if (c == UncompositesubscriptionMessage.class) {
			return 5;
		} else if (c == UnadvertisementMessage.class) {
			return 6;
		} else if (c == PublicationMessage.class) {
			return 7;
		} else {
			return 8;
		}
	}

}

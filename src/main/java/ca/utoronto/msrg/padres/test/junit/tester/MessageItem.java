package ca.utoronto.msrg.padres.test.junit.tester;

import ca.utoronto.msrg.padres.common.message.Message;

/**
 * Auxiliary class used as part of test framework. The class encapsulates
 * a Message object and additional information regarding its creation time, 
 * destination and uri.
 * 
 * @author Reza Sherafat Kazemzadeh (sherafat@gmail.com)
 * Created: July 26, 2011
 *
 */
public class MessageItem {
	
	public final long _time;
	public final String _uri;
	public final Message _msg;
	public final String _destination;
	
	MessageItem(String uri, Message msg, String destination) {
		this(System.currentTimeMillis(), uri, msg, destination);
	}
	
	MessageItem(long time, String uri, Message msg, String destination) {
		_time = time;
		_uri = uri;
		_msg = msg;
		_destination = destination;
	}
	
	public String toString() {
		return "MSGITEM:" +
				" @" + _time +
				" URI:" + _uri +
				" DEST:" + _destination +
				" MSG:" + _msg;
	}
}
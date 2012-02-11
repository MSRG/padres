/*
 * Created on 2004-3-15
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.common.message;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author Guoli Li
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ValidMessage {
	public boolean msgContentFlag = true;

	public boolean msgTypeFlag = true;

	public String msgType;

	public Set<String> opSet;

	public String command;

	static Logger messageLogger = Logger.getLogger("Message");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	public ValidMessage(String userCommand) {

		opSet = new HashSet<String>();

		opSet.add(">");
		opSet.add("<");
		opSet.add("<>");
		opSet.add("=");
		opSet.add(">=");
		opSet.add("<=");
		opSet.add("isPresent");
		opSet.add("eq");
		opSet.add("neq");
		opSet.add("str-contains");
		opSet.add("str-prefix");
		opSet.add("str-postfix");
		opSet.add("str-lt");
		opSet.add("str-le");
		opSet.add("str-gt");
		opSet.add("str-ge");

		command = trimMessage(userCommand);

		if (msgTypeFlag && msgContentFlag) {

			if (msgType.equalsIgnoreCase("publish") || msgType.equalsIgnoreCase("p")) {
				msgContentFlag = isPublication(command);

			} else if (msgType.equalsIgnoreCase("advertise") || msgType.equalsIgnoreCase("a")
					|| msgType.equalsIgnoreCase("subscribe") || msgType.equalsIgnoreCase("s")) {

				msgContentFlag = isSubscription(command);
			}
		} else {
			if (msgTypeFlag) {
				if (msgContentFlag) {
					messageLogger.debug("The user command is " + command);
				} else {
					messageLogger.warn("Invalid command format.");
					exceptionLogger.warn("Here is an exception: ", new Exception(
							"Invalid command format."));
				}
			} else {
				messageLogger.warn("Invalid command type.");
				exceptionLogger.warn("Here is an exception: ", new Exception(
						"Invalid command type."));
			}
		}
	}

	public String trimMessage(String msgString) {
		String msgString_new = "";
		msgContentFlag = true;
		msgTypeFlag = true;

		StreamTokenizer st_tmp = new StreamTokenizer(new StringReader(msgString));
		st_tmp.quoteChar('"');

		st_tmp.wordChars('=', '=');
		st_tmp.wordChars('>', '>');
		st_tmp.wordChars('<', '<');
		st_tmp.wordChars('_', '_');
		st_tmp.wordChars('[', ']');
		st_tmp.wordChars(',', ',');
		st_tmp.wordChars('.', '.');
		st_tmp.wordChars('\'', '\'');
		st_tmp.wordChars('$', '$');
		st_tmp.wordChars(0, 255);
		st_tmp.wordChars('0', '9');
		st_tmp.whitespaceChars(' ', ' ');

		if (st_tmp.ttype != StreamTokenizer.TT_EOF) {

			try {
				st_tmp.nextToken();
				if (st_tmp.ttype != StreamTokenizer.TT_EOF) {
					msgType = st_tmp.sval;
					if (!(msgType.equalsIgnoreCase("publish") || msgType.equalsIgnoreCase("p")
							|| msgType.equalsIgnoreCase("advertise")
							|| msgType.equalsIgnoreCase("a")
							|| msgType.equalsIgnoreCase("subscribe")
							|| msgType.equalsIgnoreCase("s") || msgType.equalsIgnoreCase("cs"))) {

						msgTypeFlag = false;
						msgType = null;
						msgContentFlag = false;
						msgString_new = "";
						return msgString_new;
					}
				} else {
					msgType = null;
					msgContentFlag = false;
					msgTypeFlag = false;
					msgString_new = "";
					return msgString_new;
				}

			} catch (IOException msg_type_err) {
				msgTypeFlag = false;
				msgType = null;
				msgContentFlag = false;
				msgString_new = "";
				return msgString_new;
			}
			try {
				st_tmp.nextToken();
				while (st_tmp.ttype != StreamTokenizer.TT_EOF) {
					String s = "";
					if (st_tmp.ttype == StreamTokenizer.TT_NUMBER) {
						double st_int = st_tmp.nval;
						s = String.valueOf(st_int);
					} else if (st_tmp.ttype == StreamTokenizer.TT_WORD) {
						s = st_tmp.sval;

					}

					msgString_new = msgString_new.concat(s.trim());
					st_tmp.nextToken();
				}
			} catch (IOException s_null) {
				msgContentFlag = false;
				msgString_new = "";
				return msgString_new;
			}

		}

		if (msgTypeFlag && msgContentFlag) {
			msgString_new = msgType + " " + msgString_new;
			return msgString_new;
		} else {
			msgString_new = "";
			return msgString_new;
		}

	}

	public boolean isAdvertisement(String msgString) {
		boolean msgAdsFlag = true;

		return msgAdsFlag;

	}

	public boolean isSubscription(String msgString) {
		boolean msgSubFlag = true;
		String st_tmp = "";
		int index_tmp = 0;
		String attr = "";
		String op = "";
		String val = "";

		StreamTokenizer st = new StreamTokenizer(new StringReader(msgString));

		st.whitespaceChars(']', ']');
		st.whitespaceChars('[', '[');

		st.wordChars(',', ',');
		st.wordChars('\'', '\'');
		st.wordChars('\"', '\"');
		st.wordChars('0', '9');
		st.wordChars('>', '>');
		st.wordChars('=', '=');
		st.wordChars('<', '<');
		st.wordChars('$', '$');

		// for the [class,type_value] part
		try {
			if (st.ttype != StreamTokenizer.TT_EOF) {
				st.nextToken();

				if (st.ttype == StreamTokenizer.TT_WORD) {

					String head = st.sval;
					index_tmp = head.indexOf(',');
					attr = head.substring(0, index_tmp);
					head = head.substring(index_tmp + 1);
					index_tmp = head.indexOf(',');
					op = head.substring(0, index_tmp);
					val = head.substring(index_tmp + 1);

					if (!attr.equals("class") || !opSet.contains(op) || val.equals("")) {
						msgSubFlag = false;
						return msgSubFlag;
					}
				} else {
					msgSubFlag = false;
					return msgSubFlag;
				}
			}

			// for the body part of a publication
			while (st.ttype != StreamTokenizer.TT_EOF) {

				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;

				if (st.ttype == StreamTokenizer.TT_WORD) {
					st_tmp = st.sval;

				}

				if (!st_tmp.equals(",")) {
					msgSubFlag = false;
					return msgSubFlag;
				} else {
					st.nextToken();
				}

				if (st.ttype == StreamTokenizer.TT_WORD) {
					String pair = st.sval;
					index_tmp = pair.indexOf(',');
					attr = pair.substring(0, index_tmp);
					pair = pair.substring(index_tmp + 1);
					index_tmp = pair.indexOf(',');
					op = pair.substring(0, index_tmp);
					val = pair.substring(index_tmp + 1);

					if (attr.equals("") || !opSet.contains(op) || val.equals("")) {
						msgSubFlag = false;
						return msgSubFlag;
					}
				}
			}

		} catch (Exception e) {
			msgSubFlag = false;
		}

		return msgSubFlag;

	}

	public boolean isPublication(String msgString) {
		boolean msgPubFlag = true;
		String st_tmp = "";
		int index_tmp = 0;
		StreamTokenizer st = new StreamTokenizer(new StringReader(msgString));

		// st.quoteChar('\"');

		st.whitespaceChars(']', ']');
		// st.whitespaceChars(',',',');
		st.whitespaceChars('[', '[');

		st.wordChars(',', ',');
		st.wordChars('\'', '\'');
		st.wordChars('\"', '\"');
		st.wordChars('0', '9');

		// for the [class,type_value] part
		try {
			if (st.ttype != StreamTokenizer.TT_EOF) {
				st.nextToken();

				if (st.ttype == StreamTokenizer.TT_WORD) {

					String head = st.sval;

					if (!head.startsWith("class")) {
						msgPubFlag = false;
						return msgPubFlag;
					} else {
						// st.nextToken();
						index_tmp = head.indexOf(',');
						head = head.substring(index_tmp + 1);
						if (head.equals(null)) {
							msgPubFlag = false;
							return msgPubFlag;
						}
					}
				} else {
					msgPubFlag = false;
					return msgPubFlag;
				}
			}
			// for the body part of a publication
			while (st.ttype != StreamTokenizer.TT_EOF) {

				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;

				if (st.ttype == StreamTokenizer.TT_WORD) {
					st_tmp = st.sval;

				}

				if (!st_tmp.equals(",")) {
					msgPubFlag = false;
					return msgPubFlag;
				} else {
					st.nextToken();
				}

				if (st.ttype == StreamTokenizer.TT_WORD) {
					String pair = st.sval;
					index_tmp = pair.indexOf(',');
					st_tmp = pair.substring(index_tmp + 1);
					pair = pair.substring(0, index_tmp);

					if (st_tmp.equals("") || pair.equals("")) {
						msgPubFlag = false;
						return msgPubFlag;
					}
				}
			}

		} catch (Exception e) {
			msgPubFlag = false;
		}

		return msgPubFlag;

	}

}

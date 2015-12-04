/*
 * Created on Sep 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/* Author: Gerald Chan
 * Thie class will read from the file and send message according to the content of the file
 */

package ca.utoronto.msrg.padres.broker.management.console;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Gerald Chan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class BatchMsgProcessor {

	private BrokerCore brokerCore;

	private String fileName;

	static Logger brokerCoreLogger = Logger.getLogger(BrokerCore.class);

	static Logger exceptionLogger = Logger.getLogger("Exception");

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	public BatchMsgProcessor(String fileName, BrokerCore broker) {
		brokerCore = broker;
		this.fileName = fileName;

	}

	public void run() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			handlefile(in);
		} catch (Exception e) {
			brokerCoreLogger.error("Failed to initiate fileReader: " + e);
			exceptionLogger.error("Failed to initiate fileReader: " + e);
		}
	}

	public void handlefile(BufferedReader in) throws ParseException {
		StreamTokenizer st = new StreamTokenizer(in);
		st.quoteChar('"');
		st.wordChars('<', '<');
		st.wordChars('=', '=');
		st.wordChars('>', '>');
		st.wordChars('_', '_');
		st.wordChars('[', ']');
		st.wordChars(',', ',');
		st.wordChars('.', '.');
		st.wordChars('\'', '\'');

		while (st.ttype != StreamTokenizer.TT_EOF) {
			try {
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;
				String command = st.sval;
				brokerCoreLogger.debug("The batchMsgProcessor receives command: " + command);

				if (command.equalsIgnoreCase("publish") || command.equalsIgnoreCase("p")) {
					st.nextToken();
					Publication pub = MessageFactory.createPublicationFromString(st.sval);
					PublicationMessage pubmsg = new PublicationMessage(pub,
							brokerCore.getNewMessageID());
					brokerCoreLogger.debug("The batchMsgProcessor is sending publication: "
							+ pubmsg.toString());
					messagePathLogger.debug("The batchMsgProcessor is sending publication: "
							+ pubmsg.toString());
					brokerCore.routeMessage(pubmsg, MessageDestination.INPUTQUEUE);
				} else if (command.equalsIgnoreCase("subscribe") || command.equalsIgnoreCase("s")) {
					st.nextToken();
					Subscription sub = MessageFactory.createSubscriptionFromString(st.sval);
					SubscriptionMessage submsg = new SubscriptionMessage(sub,
							brokerCore.getNewMessageID());
					brokerCoreLogger.debug("The batchMsgProcessor is sending subscription: "
							+ submsg.toString());
					messagePathLogger.debug("The batchMsgProcessor is sending subscription: "
							+ submsg.toString());
					brokerCore.routeMessage(submsg, MessageDestination.INPUTQUEUE);
				} else if (command.equalsIgnoreCase("advertise") || command.equalsIgnoreCase("a")) {
					st.nextToken();
					Advertisement adv = MessageFactory.createAdvertisementFromString(st.sval);
					AdvertisementMessage advmsg = new AdvertisementMessage(adv,
							brokerCore.getNewMessageID());
					brokerCoreLogger.debug("The batchMsgProcessor is sending advertisement: "
							+ advmsg.toString());
					messagePathLogger.debug("The batchMsgProcessor is sending advertisement: "
							+ advmsg.toString());
					brokerCore.routeMessage(advmsg, MessageDestination.INPUTQUEUE);
				} else if (command.equalsIgnoreCase("unsubscribe")
						|| command.equalsIgnoreCase("us")) {
					st.nextToken();
					// TODO: implement unsubscribe
				} else if (command.equalsIgnoreCase("unadvertise")
						|| command.equalsIgnoreCase("ua")) {
					st.nextToken();
					// TODO: implement unadvertise
				} else if (command.equalsIgnoreCase("subscriptions")
						|| command.equalsIgnoreCase("subs")) {
					if (brokerCoreLogger.isDebugEnabled())
						brokerCoreLogger.debug("Got all subscriptions in the matchingEngine: "
								+ brokerCore.getSubscriptions());
				} else if (command.equalsIgnoreCase("advertisements")
						|| command.equalsIgnoreCase("ads")) {
					if (brokerCoreLogger.isDebugEnabled())
						brokerCoreLogger.debug("Got all advertisements in the matchingEngine: "
								+ brokerCore.getAdvertisements());
				}
			} catch (IOException e) {
				brokerCoreLogger.error("Failed to handle inputfile: " + e);
				exceptionLogger.error("Failed to handle inputfile: " + e);
			}
		}

	}
}

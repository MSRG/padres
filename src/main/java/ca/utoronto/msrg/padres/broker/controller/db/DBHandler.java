// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2004 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
package ca.utoronto.msrg.padres.broker.controller.db;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.common.comm.QueueHandler;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.MessageDestination.DestinationType;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * The DBBinding attaches a historic database to the publish/subscribe federation.
 * 
 * @author Eli Fidler
 * @version 2.0
 */
public class DBHandler extends QueueHandler {

	private BrokerCore brokerCore;

	/**
	 * Database ID will be assigned when binding.
	 */
	private String databaseID;

	private DBConnector dbConnector;

	private SQLConverter sqlConverter;

	private final String HISTORIC_CLASS_NAME = "historic";

	private final String SUB_CLASS = "subclass";

	static Logger dbbindingLogger = Logger.getLogger("DBBinding");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	static Logger messagePathLogger = Logger.getLogger("MessagePath");

	/**
	 * Constructor
	 * 
	 * @param databaseID
	 *            Assigned by BrokerCore
	 */
	public DBHandler(String databaseID, BrokerCore broker) {
		super(databaseID, DestinationType.INTERNAL);
		brokerCore = broker;
		dbConnector = new DBConnector(broker.getDBPropertiesFile());
		sqlConverter = SQLConverter.getInstance();
		this.databaseID = databaseID;
	}

	/**
	 * This must be called before database client can query or store.
	 */
	public void init() {
		// Startup this database and make connection
		dbConnector.startup();

		// Subscribe to database control messages
		Subscription sub;
		try {
			sub = MessageFactory.createSubscriptionFromString("[class,eq,'DB_CONTROL'],[database_id,eq,'" + databaseID
					+ "']");
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			return;
		}
		SubscriptionMessage smsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(),
				myDestination);
		if (dbbindingLogger.isDebugEnabled())
			dbbindingLogger.debug("DBBinding is sending DBControlSubMsg : "
					+ smsg.getSubscription().toString());
		brokerCore.routeMessage(smsg, MessageDestination.INPUTQUEUE);
		dbbindingLogger.info("DBBinding is started.");
	}

	/**
	 * Get the current database ID
	 * 
	 * @return current database ID
	 */
	public String getDatabaseID() {
		return databaseID;
	}

	/**
	 * @return a string representation of the database client in the form: "Database ID: ####"
	 */
	public String toString() {
		return "Database ID:" + databaseID;
	}

	/**
	 * Handle the incoming message from the queue.
	 * 
	 * @param msg
	 *            Incoming message
	 */
	public void processMessage(Message msg) {

		dbbindingLogger.debug("DBBinding receives message : " + msg.toString());
		messagePathLogger.debug("DBBinding receives message : " + msg.toString());

		if (msg instanceof PublicationMessage) {
			Publication pub = ((PublicationMessage) msg).getPublication();
			Map<String, Serializable> pairMap = pub.getPairMap();
			String msgClass = (String) pairMap.get("class");

			if (msgClass.equalsIgnoreCase("DB_CONTROL")) {
				try {
					handleDBControlMessage(pub);
				} catch (ParseException e) {
					exceptionLogger.error(e.getMessage());
					return;
				}
			} else {
				// insert message into the database
				if (pairMap.get("_query_id") == null) { // avoid sinking
					// republished pubs
					// --shou, March 14,2007. For the bug of regular sub also
					// receiving historic data.
					// this is normal publication message which has format:
					// [class,"HISTORIC"][subclass,"stock"][symbol,"IBM"][price,123.45][volume,100]
					String className = (String) pub.getPairMap().get("class");
					pub.addPair(SUB_CLASS, className);
					pub.addPair("class", HISTORIC_CLASS_NAME);
					if (pub.getPairMap().containsKey("tid"))
						pub.getPairMap().remove("tid"); // hack so DB works with
					// cycles
					sqlConverter.convertPub((PublicationMessage) msg, dbConnector);
				}
			}
		} else if (msg instanceof SubscriptionMessage) {
			// this is normal subscription message which has format:
			// [_query_id,=,12][class,=,"stock"][symbol,=,"IBM"][price,<,123.45][volume,>,100]
			SubscriptionMessage smsg = (SubscriptionMessage) msg;
			Subscription sub = smsg.getSubscription();
			Subscription tempSub;
			try {
				tempSub = MessageFactory.createSubscriptionFromString(sub.toString());
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
				return;
			}
			if (tempSub.getPredicateMap().containsKey("tid"))
				tempSub.removePredicate("tid"); // hack so DB works with cycles
			if (sub.toString().contains("$S$")) {
				Map<String, Predicate> preMap = tempSub.getPredicateMap();
				for (Predicate p : preMap.values()) {
					if (p.getValue().toString().contains("$S$")) {
						String value = "dummy";
						p.setOp("isPresent");
						p.setValue((Serializable) value);
					}
				}
			}
			handleHistoricQuery(tempSub, smsg.getStartTime(), smsg.getEndTime());
		} else if (msg instanceof AdvertisementMessage) {
			dbbindingLogger.warn("Invalid message for DBBinding.");
			exceptionLogger.warn("Here is an exception : ", new Exception(
					"Invalid message for DBBinding."));
		}
	}

	/**
	 * Handle DB_CONTROL publication messages
	 * 
	 * @param pub
	 *            Publication from DB_CONTROL message
	 * @throws ParseException 
	 */
	public void handleDBControlMessage(Publication pub) throws ParseException {
		// the format of this publication is:
		// <class,"DB_CONTROL"><database_id,23><command,"STORE"><content_spec,"">
		Map<String, Serializable> pairMap = pub.getPairMap();

		String command = (String) pairMap.get("command");
		if (command.equalsIgnoreCase("STORE")) {
			// parse the pairs after [command=STORE] and construct
			// the correct subscription and advertisement
			String content = (String) pairMap.get("content_spec");

			// Create a subscription for sinking data
			Subscription sub;
			try {
				sub = MessageFactory.createSubscriptionFromString(content);
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
				return;
			}
			SubscriptionMessage smsg = new SubscriptionMessage(sub, brokerCore.getNewMessageID(),
					myDestination);
			if (dbbindingLogger.isDebugEnabled())
				dbbindingLogger.debug("DBBinding is sending subscription for sinking data : "
						+ smsg.getSubscription().toString());
			brokerCore.routeMessage(smsg, MessageDestination.INPUTQUEUE);

			// Create an advertisement for re-publishing data
			Advertisement adv = MessageFactory.createAdvertisementFromString(content);
			adv.addPredicate("_query_id", new Predicate("isPresent", ""));
			// --shou, March 14,2007. For the bug of regular sub also receiving
			// historic data.
			// this is normal advertisement message which has format:
			// [_query_id,eq,'12'][class,eq,"HISTORIC"][subclass,eq,"stock"][symbol,eq,"IBM"][price,<,123.45][volume,>,100]
			String className = (String) ((Predicate) adv.getPredicateMap().get("class")).getValue();
			adv.addPredicate(SUB_CLASS, new Predicate("eq", className));
			adv.addPredicate("class", new Predicate("eq", HISTORIC_CLASS_NAME));

			AdvertisementMessage amsg = new AdvertisementMessage(adv, brokerCore.getNewMessageID(),
					myDestination);
			if (dbbindingLogger.isDebugEnabled())
				dbbindingLogger.debug("DBBinding is sending advertisement for re-publishing data : "
						+ amsg.getAdvertisement().toString());
			brokerCore.routeMessage(amsg, MessageDestination.INPUTQUEUE);
		}
	}

	/**
	 * Handle historic query (subscription)
	 * 
	 * @param sub
	 *            Historic query subscription
	 */
	public void handleHistoricQuery(Subscription sub, Date start, Date end) {
		String queryID = null;
		boolean removeQueryIDPre = false;
		Map<String, Predicate> pairs = sub.getPredicateMap();
		Predicate predicate = pairs.get("_query_id");
		if (predicate == null) {
			return;
		} else { // There is a _query_id in this message
			queryID = (String) predicate.getValue();
			sub.removePredicate("_query_id");
			removeQueryIDPre = true;
		}

		// get matching publication from database
		Vector<PublicationMessage> pubEvents = sqlConverter.getAllPubs(sub, start, end, dbConnector);
		if (removeQueryIDPre) {
			sub.addPredicate("_query_id", predicate);
		}
		// publish all the results
		if (pubEvents != null) {
			for (PublicationMessage pmsg : pubEvents) {
				if (queryID != null) {
					Publication publication = pmsg.getPublication();
					publication.addPair("_query_id", queryID);
					pmsg.setPublication(publication);
					pmsg.setLastHopID(myDestination);
				}
				if (dbbindingLogger.isDebugEnabled())
					dbbindingLogger.debug("DBBinding is sending matching publications : "
							+ pmsg.getPublication().toString());
				brokerCore.routeMessage(pmsg, MessageDestination.INPUTQUEUE);
			}
		}
	}

	public DBConnector getDBConnector() {
		return dbConnector;
	}

	@Override
	public void shutdown() {
		if(shutdown)
			return;
		
		try {
			dbConnector.shutdown();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		super.shutdown();
	}
	
	
}

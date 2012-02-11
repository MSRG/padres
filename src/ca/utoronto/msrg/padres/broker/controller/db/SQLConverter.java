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
 * Created on Jul 28, 2003
 *
 * Copywriter (c) 2003 Cybermation & University of Toronto All rights reserved.
 * 
 */

//TODO: Don't insert duplicate values into tables
package ca.utoronto.msrg.padres.broker.controller.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Predicate;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;

/**
 * This class is used to convert the message into normal sql92 sentences.
 * 
 * @author Pengcheng Wan
 * @version 1.0
 */
public class SQLConverter {

	/**
	 * Define the singleton instance of the sql converter.
	 */
	private static SQLConverter instance;

	private static Map<String, String> OPERATOR_MAP;

	static Logger dbbindingLogger = Logger.getLogger("DBBinding");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	private static void initOpMap() {
		OPERATOR_MAP = new HashMap<String, String>();
		OPERATOR_MAP.put("<", "<");
		OPERATOR_MAP.put("<=", "<=");
		OPERATOR_MAP.put("=", "=");
		OPERATOR_MAP.put(">=", ">=");
		OPERATOR_MAP.put(">", ">");
		OPERATOR_MAP.put("<>", "<>");
		OPERATOR_MAP.put("str-lt", "");
		OPERATOR_MAP.put("str-le", "");
		OPERATOR_MAP.put("eq", "=");
		OPERATOR_MAP.put("str-ge", "");
		OPERATOR_MAP.put("str-gt", "");
		OPERATOR_MAP.put("neq", "<>");
		OPERATOR_MAP.put("str-contains", "");
		OPERATOR_MAP.put("str-prefix", "");
		OPERATOR_MAP.put("str-postfix", "");
		OPERATOR_MAP.put("after", ">");
		OPERATOR_MAP.put("before", "<");
	}

	/**
	 * Create and return an instance of the singleton.
	 * 
	 * @return SQLConverter - the singleton instance
	 */
	public static SQLConverter getInstance() {
		if (instance == null) {
			initOpMap();
			instance = new SQLConverter();
		}
		return instance;
	}

	/**
	 * Constructor
	 */
	private SQLConverter() {
	}

	/**
	 * Parse the publication message and insert into padres tables
	 * 
	 * @param pmsg
	 *            Publication Message
	 * @param db
	 *            Database Connector
	 * @return
	 */
	public void convertPub(PublicationMessage pmsg, DBConnector db) {
		// String queryID = null; // unique query identifier
		String className = null;
		String sql = null;
		String attributeType = "String"; // default for String

		// map database field
		int classID = 0;
		int dataID = 0; // the last dataID in database
		int pairID = 0; // the last pairID in database
		Vector<Integer> attributeIDs = new Vector<Integer>();
		Vector<String> attributeTypes = new Vector<String>();
		Vector<Integer> valueIDs = new Vector<Integer>();

		Publication pub = pmsg.getPublication();
		Map<String, Serializable> pairs = pub.getPairMap();
		for (String attribute : pairs.keySet()) {
			Object value = pairs.get(attribute);
			// Get the correct query id, class, symbol
			if (attribute.equalsIgnoreCase("query_id")) {
				// queryID = (String) value;
			} else if (attribute.equalsIgnoreCase("class")) {
				className = (String) value;
				// Search database if it has the record in table 'classes'
				sql = "SELECT * FROM classes WHERE Class_Name = '" + className + "'";
				// System.out.println("sql: " + sql);
				try {
					ResultSet rs = db.executeQuery(sql);
					if (!rs.isBeforeFirst()) { // empty result set
						String inSql = "INSERT INTO classes (Class_Name) VALUES ('" + className
								+ "')";
						// System.out.println(inSql);
						db.executeUpdate(inSql);
						sql = "SELECT * FROM classes WHERE Class_Name = '" + className + "'";
						// System.out.println("sql: " + sql);
						rs = db.executeQuery(sql);
						while (rs.next()) {
							classID = rs.getInt("Class_ID");
						}
					} else {
						while (rs.next()) {
							classID = rs.getInt("Class_ID");
						}
					}
					rs.close();
				} catch (SQLException e) {
					dbbindingLogger.error("Failed to operate database : " + e);
					exceptionLogger.error("Failed to operate database : " + e);
				}
			} else { // find if the attributes are in the database
				sql = "SELECT * FROM attributes WHERE Attribute_Name = '" + attribute + "'";
				// System.out.println("sql: " + sql);
				try {
					ResultSet rs = db.executeQuery(sql);
					if (!rs.isBeforeFirst()) { // empty result set
						if (value instanceof Long) {
							attributeType = "Long";
						} else if (value instanceof Double) {
							attributeType = "Double";
						} else {
							attributeType = "String";
						}
						String inSql = "INSERT INTO attributes (Attribute_Name, Attribute_Type) VALUES ('"
								+ attribute + "','" + attributeType + "')";
						// System.out.println(inSql);
						db.executeUpdate(inSql);
					}
					sql = "SELECT Attribute_ID, Attribute_Type FROM attributes "
							+ "WHERE Attribute_Name = '" + attribute + "'";
					// System.out.println("sql: " + sql);
					rs = db.executeQuery(sql);
					while (rs.next()) {
						attributeIDs.addElement(new Integer(rs.getInt("Attribute_ID")));
						attributeTypes.addElement(rs.getString("Attribute_Type").trim());
					}
					rs.close();
				} catch (SQLException e) {
					dbbindingLogger.error("Failed to operate database : " + e);
					exceptionLogger.error("Failed to operate database : " + e);
				}
				try {
					// insert value into value tables
					if (value instanceof Long) {
						sql = "INSERT INTO longvalues (Long_Value) Values ("
								+ ((Long) value).intValue() + ")";
					} else if (value instanceof Double) {
						sql = "INSERT INTO doublevalues (Double_Value) Values ("
								+ ((Double) value).doubleValue() + ")";
					} else {
						sql = "INSERT INTO stringvalues (String_Value) Values ('"
								+ value.toString() + "')";
					}
					// System.out.println("sql: " + sql);
					db.executeUpdate(sql);

					// get the value id from database
					if (value instanceof Long) {
						sql = "SELECT Value_ID FROM longvalues";
					} else if (value instanceof Double) {
						sql = "SELECT Value_ID FROM doublevalues";
					} else {
						sql = "SELECT Value_ID FROM stringvalues";
					}
					// System.out.println("sql: " + sql);
					ResultSet rs = db.executeQuery(sql);
					if (rs.last()) { // move to the last record
						valueIDs.addElement(new Integer(rs.getInt("Value_ID")));
					}
					rs.close();
				} catch (SQLException e) {
					dbbindingLogger.error("Failed to operate database : " + e);
					exceptionLogger.error("Failed to operate database : " + e);
				}
			}// end if
		}// end while

		// Now insert into pairs, eventdata, events table
		// Get the last dataID, pairID firstly
		try {
			sql = "SELECT Data_ID FROM eventdata";
			ResultSet rs = db.executeQuery(sql);
			if (rs.last()) { // move to the last record
				dataID = rs.getInt("Data_ID");
			}
			rs.close();
			sql = "SELECT Pair_ID FROM pairs";
			rs = db.executeQuery(sql);
			if (rs.last()) { // move to the last record
				pairID = rs.getInt("Pair_ID");
			}
			rs.close();
		} catch (SQLException e) {
			dbbindingLogger.error("Failed to operate database : " + e);
			exceptionLogger.error("Failed to operate database : " + e);
		}

		// Now insert pairs table and eventdata table
		Iterator<Integer> itOfAtt = attributeIDs.iterator();
		Iterator<String> itOfType = attributeTypes.iterator();
		Iterator<Integer> itOfVal = valueIDs.iterator();
		int curDataID = dataID + 1;
		int curPairID = pairID + 1;
		// System.out.println(attributeTypes);
		while (itOfAtt.hasNext() && itOfType.hasNext() && itOfVal.hasNext()) {
			int attribute_id = itOfAtt.next().intValue();
			String attribute_type = itOfType.next().trim();
			int value_id = itOfVal.next().intValue();
			try {
				if (attribute_type.equalsIgnoreCase("Long")) {
					sql = "INSERT INTO pairs (Pair_ID, Attribute_ID, Long_ValueID) " + "VALUES("
							+ curPairID + "," + attribute_id + "," + value_id + ")";
				} else if (attribute_type.equalsIgnoreCase("Double")) {
					sql = "INSERT INTO pairs (Pair_ID, Attribute_ID, Double_ValueID) " + "VALUES("
							+ curPairID + "," + attribute_id + "," + value_id + ")";
				} else {
					sql = "INSERT INTO pairs (Pair_ID, Attribute_ID, String_ValueID) " + "VALUES("
							+ curPairID + "," + attribute_id + "," + value_id + ")";
				}
				// System.out.println("sql: " + sql);
				db.executeUpdate(sql);
				sql = "INSERT INTO eventdata VALUES(" + curDataID + "," + curPairID + ")";
				// System.out.println("sql: " + sql);
				db.executeUpdate(sql);
				curPairID++;
			} catch (SQLException e) {
				dbbindingLogger.error("Failed to operate database : " + e);
				exceptionLogger.error("Failed to operate database : " + e);
			}
		}

		// Insert the record into events table
		String eventID = pmsg.getMessageID();
		MessageDestination lastHop = pmsg.getLastHopID();
		Serializable payLoad = pmsg.getPublication().getPayload();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
			
		short priority = pmsg.getPriority();
		long time = pmsg.getMessageTime().getTime();
		try {
			Connection conn = db.getConnection();
			PreparedStatement pst = conn.prepareStatement("INSERT INTO events VALUES (?,?,?,?,?,?,?)");
			
			oos = new ObjectOutputStream(baos);
			oos.writeObject(payLoad);
			
			pst.setString(1, eventID);
			pst.setInt(2, classID);
			pst.setInt(3, curDataID);
			pst.setString(4, lastHop.getDestinationID());
			pst.setObject(5, baos.toByteArray());
			pst.setShort(6, priority);
			// pst.setDate(7, new java.sql.Date(time));
			pst.setTimestamp(7, new java.sql.Timestamp(time));
			pst.executeUpdate();
			pst.close();
		} catch (SQLException ej) {
			dbbindingLogger.error("Failed to operate database : " + ej);
			exceptionLogger.error("Failed to operate database : " + ej);
		}
		catch (IOException ej) {
			exceptionLogger.error("Failed to operate database : " + ej);
		}
	}

	/**
	 * Query the database to get the required event messages, then publish them
	 * 
	 * @param smsg
	 *            Subscription Message
	 * @param db
	 *            Database Connector
	 * @retun The collection of matched messages
	 */
	public Vector<PublicationMessage> getAllPubs(Subscription sub, Date start, Date end,
			DBConnector db) {
		String sql = null;
		String psql = "SELECT Event_ID FROM messages WHERE ";
		String interSQL = "";
		boolean ifAddIntersect = false;

		Set<String> eventIDs = new HashSet<String>();
		Vector<Integer> valueIDs = new Vector<Integer>();
		Vector<String> attributeNames = new Vector<String>();
		Vector<String> attributeTypes = new Vector<String>();
		Vector<PublicationMessage> events = new Vector<PublicationMessage>(); // the
		// events

		// Subscription sub = smsg.getSubscription();
		Map<String, Predicate> pairs = sub.getPredicateMap();

		// Firstly, get the class name and the key is always "class"
		Predicate classPre = (Predicate) pairs.get("class");
		psql = psql + "Class_Name = '" + (String) classPre.getValue() + "'";
		// System.out.println(psql);

		for (String key : pairs.keySet()) {
			Predicate value = pairs.get(key);
			// System.out.println("Key: " + key + " Operator: " + value.getOp()
			// + " Value: " + value.getValue());
			if (key.equalsIgnoreCase("class")) {
				// Do nothing
			} else {
				String sqlAttPart = " AND Attribute_Name = '" + key + "'";
				sql = "SELECT * FROM attributes WHERE Attribute_Name = '" + key + "'";
				// System.out.println(sql);
				try {
					ResultSet rs = db.executeQuery(sql);
					if (!rs.isBeforeFirst()) { // empty result set
						rs.close();
						return null;
					} else {
						while (rs.next()) {
							String attType = rs.getString("Attribute_Type").trim();
							// convert to SQL operator
							String SQLOp = (String) OPERATOR_MAP.get(value.getOp()); 
							if (SQLOp != null) {
								if (attType.equalsIgnoreCase("Long")) {
									sqlAttPart = sqlAttPart + " AND Long_Value " + SQLOp + " "
											+ value.getValue();
								} else if (attType.equalsIgnoreCase("Double")) {
									sqlAttPart = sqlAttPart + " AND Double_Value " + SQLOp + " "
											+ value.getValue();
								} else {
									sqlAttPart = sqlAttPart + " AND String_Value " + SQLOp + " '"
											+ (String) value.getValue() + "'";
								}
							}
							if (ifAddIntersect) {
								interSQL = interSQL + " INTERSECT " + psql + sqlAttPart;
							} else {
								interSQL = psql + sqlAttPart;
								ifAddIntersect = true;
							}
						}
						rs.close();
					}
				} catch (SQLException e) {
					dbbindingLogger.error("Failed to operate database : " + e);
					exceptionLogger.error("Failed to operate database : " + e);
				}
			}
		}// end while
		// System.out.println(interSQL);

		// Find the possible event ids
		// String tsql0 =
		// "SELECT DISTINCT E.Event_ID, C.Class_Name, A.Attribute_Name," +
		// "A.Attribute_Type, P.Long_ValueID, P.Double_ValueID," +
		// "P.String_ValueID FROM events AS E, eventdata AS ED, classes AS C," +
		// "pairs AS P, attributes AS A, longvalues AS IV, doublevalues AS DV,"
		// +
		// "stringvalues AS SV WHERE E.Class_ID = C.Class_ID AND E.Data_ID = " +
		// "ED.Data_ID AND ED.Pair_ID = P.Pair_ID AND P.Attribute_ID = A.Attribute_ID ";

		// -- shou Mar 19,2007, fixed the bug for longvalues, doublevalues and
		// stringvalues tables do not have data.
		String tsql0 = "SELECT DISTINCT E.Event_ID, C.Class_Name, A.Attribute_Name,"
				+ "A.Attribute_Type, P.Long_ValueID, P.Double_ValueID,"
				+ "P.String_ValueID FROM events AS E, eventdata AS ED, classes AS C,"
				+ "pairs AS P, attributes AS A " + "WHERE E.Class_ID = C.Class_ID AND E.Data_ID = "
				+ "ED.Data_ID AND ED.Pair_ID = P.Pair_ID AND P.Attribute_ID = A.Attribute_ID ";

		try {
			ResultSet rs = db.executeQuery(interSQL);
			while (rs.next()) {
				String event_id = rs.getString("Event_ID");
				eventIDs.add(event_id);
			}
			rs.close();

			for (String event_id : eventIDs) {
				String tsql = tsql0 + "AND E.Event_ID = '" + event_id + "'";
				// System.out.println(tsql);

				Publication pub = MessageFactory.createEmptyPublication();

				ResultSet rset = db.executeQuery(tsql);
				String class_name = null;
				while (rset.next()) {
					if (class_name == null) {
						class_name = rset.getString("Class_Name");
					}
					String attr_name = rset.getString("Attribute_Name");
					String attr_type = rset.getString("Attribute_Type").trim();
					attributeNames.addElement(attr_name);
					attributeTypes.addElement(attr_type);
					int value_id = 0;
					if (attr_type.equalsIgnoreCase("Long")) {
						value_id = rset.getInt("Long_ValueID");
					} else if (attr_type.equalsIgnoreCase("Double")) {
						value_id = rset.getInt("Double_ValueID");
					} else {
						value_id = rset.getInt("String_ValueID");
					}
					valueIDs.addElement(new Integer(value_id));
				}
				rset.close();

				Iterator<String> itOfAttName = attributeNames.iterator();
				Iterator<String> itOfAttType = attributeTypes.iterator();
				Iterator<Integer> itOfValueID = valueIDs.iterator();
				while (itOfAttName.hasNext() && itOfAttType.hasNext() && itOfValueID.hasNext()) {
					String attr_name = itOfAttName.next();
					String attr_type = itOfAttType.next();
					int value_id = itOfValueID.next().intValue();
					if (attr_type.equalsIgnoreCase("Long")) {
						sql = "SELECT Long_Value FROM longvalues WHERE Value_ID = " + value_id;
					} else if (attr_type.equalsIgnoreCase("Double")) {
						sql = "SELECT Double_Value FROM doublevalues WHERE Value_ID = " + value_id;
					} else {
						sql = "SELECT String_Value FROM stringvalues WHERE Value_ID = " + value_id;
					}
					Serializable value = null;
					// System.out.println(sql);
					ResultSet result = db.executeQuery(sql);
					while (result.next()) {
						value = (Serializable) result.getObject(1);
						if (value instanceof Long) {
							value = (Long) value;
							pub.addPair(attr_name, value);
						} else if (value instanceof Integer) {
							value = new Long(((Integer) value).longValue());
							pub.addPair(attr_name, value);
						} else if (value instanceof Double) {
							value = (Double) value;
							pub.addPair(attr_name, value);
						} else if (value instanceof String) {
							value = ((String) value).trim();
							pub.addPair(attr_name, value);
						}
					}
					result.close();
				}
				pub.addPair("class", class_name); // Add class to publication

				PublicationMessage pmsg = new PublicationMessage(pub, event_id);
				sql = "SELECT * FROM events WHERE Event_ID = '" + event_id + "'";
				// System.out.println(sql);
				ResultSet eventset = db.executeQuery(sql);

				while (eventset.next()) {
					String destinationID = eventset.getString("LastHop_ID");
					pmsg.setLastHopID(new MessageDestination(destinationID));
					// Get the payload field					
					Blob payload = eventset.getBlob("PayLoad");
					InputStream is = payload.getBinaryStream();
				    ObjectInputStream oip;
					oip = new ObjectInputStream(is);
					Object object = oip.readObject();						
					pub.setPayload((Serializable) object);
					pmsg.setPublication(pub);
					// Get the priority field					
					short priority = eventset.getShort("Priority");
					pmsg.setPriority(priority);
					// Get the date field
					java.sql.Timestamp timestamp = eventset.getTimestamp("Time");
					java.util.Date time = new Date(timestamp.getTime());
					pmsg.setMessageTime(time);
				}
				eventset.close();

				// TODO: handle time selection properly
				if (start == null && end == null) {
					events.addElement(pmsg);
				} else if (start == null) {
					if (pmsg.getMessageTime().before(end))
						events.addElement(pmsg);
				} else if (end == null) {
					if (pmsg.getMessageTime().after(start))
						events.addElement(pmsg);
				} else {
					if (pmsg.getMessageTime().after(start) && pmsg.getMessageTime().before(end))
						events.addElement(pmsg);
				}
			}
		} catch (SQLException e) {
			dbbindingLogger.error("Failed to operate database : " + e);
			exceptionLogger.error("Failed to operate database : " + e);
		} catch (IOException e) {
			exceptionLogger.error("Failed to operate : " + e);
		} catch (ClassNotFoundException e) {
			exceptionLogger.error("Failed to operate database : " + e);
		}
		return events;
	}

}

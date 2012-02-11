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
 * Created on Jul 17, 2003
 *
 * Copywriter (c) 2003 Cybermation & University of Toronto All rights reserved.
 * 
 */
package ca.utoronto.msrg.padres.broker.controller.db;

import java.io.FileInputStream;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;

/**
 * This class provides convenience and flexibility for connecting to different JDBC compliant
 * database without changing the code. The database specific properties and other parameters are
 * specified in a Java properties file. By default, the properties file is named
 * <code>db.properties</code>. The properties files for the following database products are
 * provided.
 * <ul>
 * <li>MySQL (Linux and Windows)</li>
 * <li>PostgreSQL (Linux)</li>
 * </ul>
 * Add the logging feature
 * 
 * @author Pengcheng Wan
 * @version 1.1
 */
public class DBConnector {

	public static final String DEFAULT_DB_PROPS_FILE_PATH = BrokerConfig.PADRES_HOME
			+ "etc/db/db.properties";

	private enum DBType { MEMORY, EXTERNAL }
		
	/**
	 * The default name of the database properties file. The path is: build\etc\db\db.properties in
	 * window os OR build/etc/db/db.properties in Linux/Unix os
	 */
	private static String dbpropfile;

	/* instance fields */
	private DBType dbType = null;
	
	private String database = null;

	private String jdbcDriver = null;

	private String jdbcURL = null;

	private String dbHost = null;

	private String dbPort = null;

	private String dbName = null;

	private String username = null;

	private String password = null;

	private Connection conn = null;

	private Statement stmt = null;

	private PreparedStatement prestmt = null;

	static Logger dbbindingLogger = Logger.getLogger("DBBinding");

	static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * The tables and the SQL statements to create them.
	 * It's important the map elements are iterable in insertion order.
	 */
	static final Map<String, String> createTableStatements = new LinkedHashMap<String, String>() {
		private static final long serialVersionUID = -8501276788211462481L;
		{
			put("classes",
				"CREATE TABLE classes (\n" +
				"  Class_ID integer generated always as identity,                    -- autoincrement field\n" +
				"  Class_Name varchar(128) NOT NULL,\n" +
				"  PRIMARY KEY (Class_ID)\n" +
				")"
			);

			put("attributes",
				"CREATE TABLE attributes (\n" +
				"  Attribute_ID integer generated always as identity,                  -- autoincrement field\n" +
				"  Attribute_Name varchar(128) NOT NULL ,\n" +
				"  Attribute_Type char(6) NOT NULL,     -- enum from {Long, Double, String}\n" +
				"  PRIMARY KEY  (Attribute_ID)\n" +
				")"
			);

			put("longvalues",
				"CREATE TABLE longvalues (\n" +
				"  Value_ID integer generated always as identity,						-- autoincrement field         \n" +
				"  Long_Value integer NOT NULL,\n" +
				"  PRIMARY KEY  (Value_ID)\n" +
				")"
			);

			put("doublevalues",
				"CREATE TABLE doublevalues (\n" +
				"  Value_ID integer generated always as identity,	 					-- autoincrement field\n" +
				"  Double_Value float NOT NULL,\n" +
				"  PRIMARY KEY  (Value_ID)\n" +
				")"
			);

			put("stringvalues",
				"CREATE TABLE stringvalues (\n" +
				"  Value_ID integer generated always as identity,						-- autoincrement field\n" +
				"  String_Value char(128)   NOT NULL,\n" +
				"  PRIMARY KEY  (Value_ID)\n" +
				")"
			);

			put("pairs",
				"CREATE TABLE pairs (\n" +
				"  Pair_ID integer NOT NULL,\n" +
				"  Attribute_ID integer NOT NULL,\n" +
				"  Long_ValueID integer,\n" +
				"  Double_ValueID integer,\n" +
				"  String_ValueID integer,\n" +
				"  PRIMARY KEY (Pair_ID),\n" +
				"  FOREIGN KEY (Attribute_ID) REFERENCES attributes (Attribute_ID)\n" +
				")"
			);

			put("eventdata",
				"CREATE TABLE eventdata (\n" +
				"  Data_ID integer NOT NULL,\n" +
				"  Pair_ID integer NOT NULL,\n" +
				"  PRIMARY KEY (Data_ID, Pair_ID),\n" +
				"  FOREIGN KEY (Pair_ID) REFERENCES pairs (Pair_ID)\n" +
				")"
			);

			put("events",
				"CREATE TABLE events (\n" +
				"  Event_ID varchar(128) NOT NULL,\n" +
				"  Class_ID integer NOT NULL,\n" +
				"  Data_ID integer  NOT NULL,\n" +
				"  LastHop_ID varchar(1024) NOT NULL,\n" +
				"  Payload blob,	-- NOT NULL??\n" +
				"  Priority smallint NOT NULL,\n" +
				"  Time timestamp NOT NULL,\n" +
				"  PRIMARY KEY (Event_ID),\n" +
				"  FOREIGN KEY (Class_ID) REFERENCES classes (Class_ID) ON DELETE CASCADE\n" +
				")"
			);

			put("messages",
				"CREATE VIEW messages AS ( SELECT DISTINCT\n" +
				"E.Event_ID, C.Class_Name, A.Attribute_Name, A.Attribute_Type, IV.Long_Value, SV.String_Value, DV.Double_Value\n" +
				"FROM (events AS E JOIN classes AS C ON E.Class_ID = C.Class_ID) JOIN eventdata AS ED\n" +
				"     ON (E.Data_ID = ED.Data_ID) JOIN pairs AS P \n" +
				"     ON (ED.Pair_ID = P.Pair_ID) JOIN attributes AS A \n" +
				"     ON (P.Attribute_ID = A.Attribute_ID) LEFT OUTER JOIN stringvalues AS SV \n" +
				"     ON (P.String_ValueID = SV.Value_ID AND A.Attribute_Type = 'String') LEFT OUTER JOIN longvalues AS IV \n" +
				"     ON (P.Long_ValueID = IV.Value_ID AND A.Attribute_Type = 'Long') LEFT OUTER JOIN doublevalues AS DV\n" +
				"     ON (P.Double_ValueID = DV.Value_ID AND A.Attribute_Type = 'Double')\n" +
				")"
			);
		}
	};
	
	/**
	 * Default Constructor
	 */
	public DBConnector() {
		DBConnector.setDBPropertiesFileName(DEFAULT_DB_PROPS_FILE_PATH);
	}

	/**
	 * Constructor with given database properties name. This file should be put into build/etc
	 * directory. It is not default property file: db.properties.
	 * 
	 * @param dbpropname
	 *            The database properties file.
	 */
	public DBConnector(String propfile) {
		DBConnector.setDBPropertiesFileName(propfile);

	}

	/**
	 * Set the correct path of the database properties file if needed.
	 * 
	 * @param dbpropname
	 *            The database properties file
	 */
	public static void setDBPropertiesFileName(String dbpropname) {
		// String fileSeparator = System.getProperty("file.separator");
		// DBConnector.dbpropname = "build" + fileSeparator + fileSeparator + "etc" + fileSeparator
		// + fileSeparator + dbpropname;
		// DBConnector.dbpropfile = "etc" + fileSeparator + fileSeparator + dbpropname;
		DBConnector.dbpropfile = dbpropname;
	}

	/**
	 * Starts up the database. This must be called before calling methods to get connection of
	 * execute queries or updates.
	 */
	public void startup() {

		dbbindingLogger.debug("Begin to start up the database.");
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream(dbpropfile));

			try {
				dbType = DBType.MEMORY; // default value.
				dbType = DBType.valueOf(prop.getProperty("type"));
			} catch (IllegalArgumentException e) {
				dbbindingLogger.warn("Bad database type in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Bad database type in the property file."));
			} catch (NullPointerException e) {
				dbbindingLogger.warn("Missing database type in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database type in the property file."));
			}
			
			database = prop.getProperty("database");
			if (database == null) {
				dbbindingLogger.warn("Missing database key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database key in the property file."));
			}
			jdbcDriver = prop.getProperty("jdbc.driver");
			if (jdbcDriver == null) {
				dbbindingLogger.warn("Missing jdbcDriver key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing jdbcDriver key in the property file."));
			}
			jdbcURL = prop.getProperty("database.url");
			if (jdbcURL == null) {
				dbbindingLogger.warn("Missing database URL key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database URL key in the property file."));
			}
			dbHost = prop.getProperty("database.host");
			if (dbHost == null) {
				dbbindingLogger.warn("Missing database host key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database host key in the property file."));
			}
			dbPort = prop.getProperty("database.port");
			if (dbPort == null) {
				dbbindingLogger.warn("Missing database port key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database port key in the property file."));
			}
			dbName = prop.getProperty("database.name");
			if (dbName == null) {
				dbbindingLogger.warn("Missing database name key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database name key in the property file."));
			}
			username = prop.getProperty("username");
			if (username == null) {
				dbbindingLogger.warn("Missing database username key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database username key in the property file."));
			}
			password = prop.getProperty("password");
			if (password == null) {
				dbbindingLogger.warn("Missing database password key in the property file.");
				exceptionLogger.warn("Here is an exception : ", new Exception(
						"Missing database password key in the property file."));
			}

			if (dbbindingLogger.isDebugEnabled()) {
				dbbindingLogger.debug("Parse: " + database + "," + jdbcDriver + "," + jdbcURL
						+ dbHost + "," + dbPort + "," + dbName + "," + username + "," + password);
			}

			// Make connection to database and initialize the database.
			initDatabase();
			
			// TODO: support prepared statement if needed
		} catch (IOException ei) {
			dbbindingLogger.error("Failed to start up the database, IOException: " + ei);
			exceptionLogger.error("Failed to start up the database, IOException: " + ei);
		}
	}

	void initDatabase() {
		try {
			// Connect to the database.
			if (conn == null) {
				dbbindingLogger.debug("DBConnector: Connecting to " + dbName);
				if (dbType.equals(DBType.MEMORY)) {
					// In-memory embedded database.
					Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
					conn = DriverManager.getConnection("jdbc:derby:memory:" + dbName + ";create=true");
				} else {
					// External database.
					if (jdbcDriver != null)
						Class.forName(jdbcDriver).newInstance();
					conn = DriverManager.getConnection(makeURL(), username, password);
				}
			}
			
			// Create a JDBC statement.
			if (stmt == null) {
				stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			}
			
			// Create missing tables.
			DatabaseMetaData meta = conn.getMetaData();
			for (Entry<String, String> e : createTableStatements.entrySet()) {
				String table = e.getKey();
				// Check if table exists
				ResultSet rs = meta.getTables(null, null, table.toUpperCase(), null);
				if (!rs.next())
					stmt.execute(e.getValue());	// Table doesn't exist so create it.
			}
		} catch (IllegalAccessException ej) {
			dbbindingLogger.error("Failed to start up the database, IllgealAccessException: " + ej);
			exceptionLogger.error("Failed to start up the database, IllgealAccessException: " + ej);
		} catch (InstantiationException ek) {
			dbbindingLogger.error("Failed to start up the database, InstantiationException: " + ek);
			exceptionLogger.error("Failed to start up the database, InstantiationException: " + ek);
		} catch (ClassNotFoundException el) {
			dbbindingLogger.error("Failed to start up the database, ClassNotFoundException: " + el);
			exceptionLogger.error("Failed to start up the database, ClassNotFoundException: " + el);
		} catch (SQLException em) {
			dbbindingLogger.error("Failed to start up the database: " + em);
			exceptionLogger.error("Failed to start up the database: " + em);
		}
	}
	
	/**
	 * Shuts down the database.
	 * 
	 * @throws SQLException
	 */
	public void shutdown() throws SQLException {
		if (stmt != null)
			stmt.close();
		if (prestmt != null)
			prestmt.close();
		if (conn != null)
			conn.close();
//		DriverManager.getConnection("jdbc:derby:;shutdown=true");
		conn = null;
		prestmt = null;
		stmt = null;
	}

	/**
	 * Execute a SQL query.
	 * 
	 * @param sql
	 *            The normal SQL sentence
	 * @throws SQLException
	 */
	public synchronized ResultSet executeQuery(String sql) throws SQLException {
		if (conn == null || stmt == null)
			initDatabase();
		return stmt.executeQuery(sql);
	}

	/**
	 * Execute a SQL update.
	 * 
	 * @return The number of rows updated.
	 * @throws SQLException
	 */
	public synchronized int executeUpdate(String sql) throws SQLException {
		if (conn == null || stmt == null)
			initDatabase();
		return stmt.executeUpdate(sql);
	}

	/**
	 * To get a new connection to the database.
	 * 
	 * @return The JDBC Connection.
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		if (this.conn == null)
			initDatabase();
		return this.conn;
	}

	/**
	 * Inner method to create JDBC URL for MySQL format. The MySQL format is:
	 * "jdbc:mysql://[hostname][,failoverhost...]
	 * [:port]/[dbname][?param1=value1][&param2=value2]..." The PostgreSQl format is:
	 * "jdbc:postgresql://[hostname][:port]/[dbname]
	 * 
	 * @return The JDBC URL.
	 */
	protected String makeURL() {
		String url = jdbcURL;
		if (url != null) {
			if (dbHost != null)
				url = url.trim() + "//" + dbHost.trim();
			if (dbPort != null)
				url = url + ":" + dbPort.trim();
			if (dbName != null)
				url = url + "/" + dbName.trim();
			// if (username != null) url = url + "?user=" + username.trim();
			// if(password != null) url = url + "&password=" + password.trim();
			if (dbbindingLogger.isDebugEnabled())
				dbbindingLogger.debug("The URL is: " + url);
		}
		return url;
	}

	public void clearTables() throws SQLException {
		executeUpdate("delete from events");
		executeUpdate("delete from eventdata");
		executeUpdate("delete from classes");
		executeUpdate("delete from pairs");
		executeUpdate("delete from attributes");
		executeUpdate("delete from longvalues");
		executeUpdate("delete from doublevalues");
		executeUpdate("delete from stringvalues");
	}
}

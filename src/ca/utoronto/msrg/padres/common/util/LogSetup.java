package ca.utoronto.msrg.padres.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;

import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;

public class LogSetup {

	protected static final String DEFAULT_LOG_RELATIVE_PATH = System.getProperty("user.home")
			+ File.separator + ".padres" + File.separator + "logs" + File.separator;

	protected static final String DEFAULT_LOG_CONFIG_FILE_PATH = BrokerConfig.PADRES_HOME
			+ File.separator + "etc" + File.separator + File.separator + "log4j.properties";

	protected static final long EXPIRE_DATE = 30;

	protected String logsDir;

	protected String now;

	public LogSetup(String dirName) throws LogException {
		this(null, dirName);
	}

	public LogSetup(String logConfigFileName, String logDirName) throws LogException {
		// initialize
		initSetup(logConfigFileName, logDirName);

		// create and add file appenders depending on the log levels
		Level rootLevel = Logger.getRootLogger().getLevel();
		for (Enumeration<?> loggers = LogManager.getCurrentLoggers(); loggers.hasMoreElements();) {
			Logger classLogger = (Logger) loggers.nextElement();
			if (classLogger.getLevel().toInt() >= rootLevel.toInt()) {
				classLogger.removeAllAppenders();
				classLogger.setAdditivity(false);
				addFileAppender(classLogger);
			}
		}
	}

	private void initSetup(String logConfigFileName, String dirName) throws LogException {
		Properties log4jProps = new Properties();
		try {
			// read the log4j configurations
			if (logConfigFileName == null)
				logConfigFileName = DEFAULT_LOG_CONFIG_FILE_PATH;
			FileInputStream log4jPropsFile = new FileInputStream(logConfigFileName);
			log4jProps.load(log4jPropsFile);
			log4jPropsFile.close();
			// configure log4j
			new PropertyConfigurator().doConfigure(log4jProps, LogManager.getLoggerRepository());
		} catch (IOException e) {
			throw new LogException("Error in loading the log4j property file", e);
		}

		// get and create the log directory
		logsDir = log4jProps.getProperty("log.dir", DEFAULT_LOG_RELATIVE_PATH);
		if (dirName != null && !dirName.equals(""))
			logsDir = dirName;
		// create the log directory; if already exists clean the directory of old log files
		cleanLogDir(logsDir);

		// set the value of "now" to a formatted string
		now = "";
		String timeInFileName = log4jProps.getProperty("time.in.filename");
		if (timeInFileName != null && timeInFileName.equalsIgnoreCase("ON")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
			now = sdf.format(new Date());
		}
	}

	/**
	 * Create the log directory if it does not exists. If it already does, clean the directory of
	 * old log files.
	 * 
	 * @param logDir
	 * @throws LogException
	 */
	private void cleanLogDir(String logDir) throws LogException {
		File dir = new File(logDir);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new LogException("Unable to create log directory");
			}
		} else {
			long expireTime = System.currentTimeMillis() - (EXPIRE_DATE * 24 * 3600 * 1000);
			File[] fileList = dir.listFiles();
			for (File logFile : fileList) {
				if (logFile.lastModified() < expireTime) {
					if (!logFile.delete()) {
						throw new LogException("Unable to clean old log file: " + logFile);
					}
				}
			}
		}
	}

	/**
	 * Creates a RollingFileAppender and adds to the specified logger.
	 * 
	 * @param classLogger
	 *            The logger to which the appender is to be added.
	 * @throws BrokerCoreException
	 */
	private void addFileAppender(Logger classLogger) throws LogException {
		PatternLayout layout = new PatternLayout("%d %-5p %l %m%n");
		String[] classNameSplit = classLogger.getName().split("\\.");
		String className = classNameSplit[classNameSplit.length - 1];
		String fileName = String.format("%s/%s-%s.log", logsDir, className, now);
		try {
			RollingFileAppender appender = new RollingFileAppender(layout, fileName);
			appender.setMaxFileSize("1000KB");
			appender.setMaxBackupIndex(1);
			classLogger.addAppender(appender);
		} catch (IOException e) {
			throw new LogException("Error in adding log4j appender for " + classLogger.getName(), e);
		}
	}

	public void addFileAppender(Class<?> className) throws LogException {
		Logger classLogger = Logger.getLogger(className);
		addFileAppender(classLogger);
	}

	public void addFileAppender(String className) throws LogException {
		Logger classLogger = Logger.getLogger(className);
		addFileAppender(classLogger);
	}

	public static void addAppender(String className, Appender appender) {
		Logger classLogger = Logger.getLogger(className);
		classLogger.addAppender(appender);
	}

	public static void removeAppender(String className, Appender appender) {
		Logger classLogger = Logger.getLogger(className);
		classLogger.removeAppender(appender);
	}

	public String getLogDir() {
		return logsDir;
	}

	public String getExceptionLogFileName() {
		return String.format("%s/Exception-%s.log", logsDir, now);
	}

	public String getClassLogFileName(Class<?> className) {
		return String.format("%s/%s-%s.log", logsDir, className.getSimpleName(), now);
	}

	public String getClassLogFileName(String className) {
		String[] parts = className.split(".");
		className = parts.length > 0 ? parts[parts.length - 1] : className;
		return String.format("%s/%s-%s.log", logsDir, className, now);
	}
}

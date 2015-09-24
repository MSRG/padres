package ca.utoronto.msrg.padres.test.junit.components;

import java.io.IOException;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;

/**
 * This class provides a way for exception handling test with property file.
 * 
 * @author Shuang Hou
 */
public class TestPropertyFileException extends TestCase {

	private static final String FILE_SEP = System.getProperty("file.separator")
			+ System.getProperty("file.separator");

	private BrokerCore brokerCore;

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (brokerCore != null && brokerCore.isRunning()) {
			brokerCore.shutdown();
			brokerCore = null;
		}
	}

	/**
	 * Test for exception that the property file without padres.Port key. NOTE: This test is buggy
	 * since we now load the default property file first, and this file sets a default port.
	 * 
	 * @throws IOException
	 * @throws BrokerCoreException
	 */
	public void testPropertyFileMissingURIKey() throws IOException, BrokerCoreException {
		// For now, padres take the 1099 as defalt.
		String tempPropsFileName = BrokerConfig.PADRES_HOME + "etc" + FILE_SEP + "test" + FILE_SEP
				+ "junit" + FILE_SEP + "othersuite" + FILE_SEP + "missingPortKey.properties";
		String[] args = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, tempPropsFileName };
		brokerCore = new BrokerCore(args);
		// URI should be set to default
		BrokerConfig brokerProps = brokerCore.getBrokerConfig();
		assertTrue("The URI should be rmi://localhost:1099/Broker1",
				brokerProps.getBrokerURI().equals("rmi://localhost:1099/Broker1"));
	}

	/**
	 * Test for exception that the property file without padres.Port value.
	 * 
	 * @throws IOException
	 * @throws BrokerCoreException
	 */
	public void testPropertyFileMissingURIValue() throws IOException, BrokerCoreException {
		// For now, padres take the 1099 as defalt.
		String tempPropsFileName = BrokerConfig.PADRES_HOME + "etc" + FILE_SEP + "test" + FILE_SEP
				+ "junit" + FILE_SEP + "othersuite" + FILE_SEP + "missingPortValue.properties";
		String[] args = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, tempPropsFileName };
		try {
			brokerCore = new BrokerCore(args);
		} catch (BrokerCoreException e) {
			assertTrue(
					"BrokerException should be thrown since the properties file does not specify the URI.",
					e.getMessage().contains("Missing uri"));
		}
	}

	/**
	 * Test for exception that the property file without padres.heartbeat key. NOTE: This test is
	 * buggy since we now load the default property file first, and this file sets a default
	 * heartbeat value.
	 * 
	 * @throws IOException
	 * @throws BrokerCoreException
	 */
	public void testPropertyFileMissingHBKey() throws IOException, BrokerCoreException {
		// For now, padres take the HeartBeat=OFF as defalt.
		String tempPropsFileName = BrokerConfig.PADRES_HOME + "etc" + FILE_SEP + "test" + FILE_SEP
				+ "junit" + FILE_SEP + "othersuite" + FILE_SEP + "missingHBKey.properties";
		String[] args = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, tempPropsFileName };
		brokerCore = new BrokerCore(args);
		// heart beat should be set to the default value of "OFF"
		BrokerConfig brokerProps = brokerCore.getBrokerConfig();
		assertTrue("The heartbeat should be OFF", !brokerProps.isHeartBeat());

	}

	/**
	 * Test for exception that the property file without padres.heartbeat value.
	 * 
	 * @throws IOException
	 * @throws BrokerCoreException
	 */
	public void testPropertyFileMissingHBValue() throws IOException, BrokerCoreException {
		// For now, padres take the HeartBeat=OFF as defalt.
		String tempPropsFileName = BrokerConfig.PADRES_HOME + "etc" + FILE_SEP + "test" + FILE_SEP
				+ "junit" + FILE_SEP + "othersuite" + FILE_SEP + "missingHBValue.properties";
		String[] args = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, tempPropsFileName };
		brokerCore = new BrokerCore(args);
		BrokerConfig props = brokerCore.getBrokerConfig();
		assertTrue("Padres heartbeat is wrong", !props.isHeartBeat());
	}

	/**
	 * Test for exception that the property file does not exist.
	 * 
	 * @throws IOException
	 * @throws BrokerCoreException
	 */
	public void testNotExistingPropertyFile() throws IOException, BrokerCoreException {
		String tempPropsFileName = BrokerConfig.PADRES_HOME + "etc" + FILE_SEP + "test" + FILE_SEP
				+ "junit" + FILE_SEP + "othersuite" + FILE_SEP + "notExisting.properties";
		String[] args = { "-" + BrokerConfig.CMD_ARG_FLAG_CONFIG_PROPS, tempPropsFileName };
		try {
			brokerCore = new BrokerCore(args);
		} catch (BrokerCoreException e) {
			assertTrue(e.getMessage().contains("Cannot load broker properties file"));
		}
	}
}

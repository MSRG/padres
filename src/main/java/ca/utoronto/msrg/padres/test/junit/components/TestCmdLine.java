package ca.utoronto.msrg.padres.test.junit.components;

import junit.framework.TestCase;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerConfig.CycleType;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCore;
import ca.utoronto.msrg.padres.broker.brokercore.BrokerCoreException;
import ca.utoronto.msrg.padres.broker.router.AdvertisementFilter.AdvCoveringType;
import ca.utoronto.msrg.padres.broker.router.SubscriptionFilter.SubCoveringType;

public class TestCmdLine extends TestCase {

	private BrokerCore brokerCore;

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (brokerCore != null && brokerCore.isRunning())
			brokerCore.shutdown();
	}

	public void testCmdLineWithPropsFilename() throws BrokerCoreException {
		String propsFileName = BrokerConfig.PADRES_HOME + "etc"
				+ System.getProperty("file.separator")
				+ System.getProperty("file.separator") + "broker.properties";
		brokerCore = new BrokerCore("-c " + propsFileName);
		BrokerConfig props = brokerCore.getBrokerConfig();
		assertTrue("Broker URI is wrong",
				props.getBrokerURI().equals("rmi://localhost:1099/Broker1"));
		assertTrue("Broker heartbeat flag is wrong", !props.isHeartBeat());
		assertTrue("Broker neighbor count is wrong",
				props.getNeighborURIs().length == 0);
		assertTrue("Broker subscription covering flag is wrong",
				props.getSubCovering() == SubCoveringType.OFF);
		assertTrue("Broker advertisement covering flag is wrong",
				props.getAdvCovering() == AdvCoveringType.OFF);
		assertTrue("Broker cycles flag is wrong",
				props.getCycleOption() == CycleType.OFF);
		assertTrue("Broker db property file name is wrong", props
				.getDbPropertyFileName().equals("etc/db/db.properties"));
		assertTrue("Broker retry limit is wrong",
				props.getConnectionRetryLimit() == 30);
		assertTrue("Broker retry pause time is wrong",
				props.getConnectionRetryPause() == 10);
	}

	public void testCmdLineWithNoPropsAndAllArgs() throws BrokerCoreException {
		String[] args = "-uri rmi://localhost:1100/TestId -n rmi://10.0.0.1:1099/Test2,rmi://10.0.0.2:3333/Test3 -b DB -h ON -rl 30 -rp 10 -s ACTIVE -a ON -cy FIXED -d db/db.properties"
				.split("\\s+");
		brokerCore = new BrokerCore(args);
		BrokerConfig props = brokerCore.getBrokerConfig();
		assertTrue("Broker URI is wrong",
				props.getBrokerURI().equals("rmi://localhost:1100/TestId"));
		assertTrue("Broker manager is wrong",
				props.getManagers()[0].equals("DB"));
		assertTrue("Broker heartbeat flag is wrong", props.isHeartBeat());
		String[] neighborURIs = props.getNeighborURIs();
		assertTrue("Broker neighbors are wrong",
				neighborURIs[0].equals("rmi://10.0.0.1:1099/Test2")
						&& neighborURIs[1].equals("rmi://10.0.0.2:3333/Test3"));

		assertTrue("Broker subscription covering flag is wrong",
				props.getSubCovering() == SubCoveringType.ACTIVE);
		assertTrue("Broker advertisement covering flag is wrong",
				props.getAdvCovering() == AdvCoveringType.ON);
		assertTrue("Broker cycles flag is wrong",
				props.getCycleOption() == CycleType.FIXED);
		assertTrue("Broker db property file name is wrong", props
				.getDbPropertyFileName().equals("db/db.properties"));
		assertTrue("Broker retry limit is wrong",
				props.getConnectionRetryLimit() == 30);
		assertTrue("Broker retry pause time is wrong",
				props.getConnectionRetryPause() == 10);
	}

	public void testCmdLineWithPropsAndPartialArgs() throws BrokerCoreException {
		String arg = "-n rmi://10.0.0.1:1099/Broker1,rmi://10.0.0.2:3333/Broker3 -b DB -h ON -c "
				+ BrokerConfig.PADRES_HOME
				+ "etc/test/junit/othersuite/brokerB.properties";
		String args[] = arg.split("\\s+");
		brokerCore = new BrokerCore(args);
		BrokerConfig props = brokerCore.getBrokerConfig();
		assertTrue("Broker URI is wrong",
				props.getBrokerURI().equals("rmi://10.0.0.1:1100/Broker2"));
		assertTrue("Broker manager is wrong",
				props.getManagers()[0].equals("DB"));
		assertTrue("Broker heartbeat flag is wrong", props.isHeartBeat());
		String[] neighborURIs = props.getNeighborURIs();
		assertTrue(
				"Broker neighbors are wrong",
				neighborURIs[0].equals("rmi://10.0.0.1:1099/Broker1")
						&& neighborURIs[1]
								.equals("rmi://10.0.0.2:3333/Broker3"));

		assertTrue("Broker subscription covering flag is wrong",
				props.getSubCovering() == SubCoveringType.OFF);
		assertTrue("Broker advertisement covering flag is wrong",
				props.getAdvCovering() == AdvCoveringType.OFF);
		assertTrue("Broker cycles flag is wrong",
				props.getCycleOption() == CycleType.OFF);
		assertTrue("Broker db property file name is wrong", props
				.getDbPropertyFileName().equals("db.properties"));
		assertTrue("Broker retry limit is wrong",
				props.getConnectionRetryLimit() == 30);
		assertTrue("Broker retry pause time is wrong",
				props.getConnectionRetryPause() == 10);
	}

	/*
	 * Null values provided in command line should not overwrite default values
	 * given in properties file
	 */
	public void testCmdLineWithNullValues() {
		String[] args = "-n -b DB -c -i B1 -h".split("\\s+");
		try {
			brokerCore = new BrokerCore(args);
		} catch (BrokerCoreException e) {
			return;
		}
		assertTrue("This part of the code should not be reached", false);
	}

	public void testCmdLineWithEmptyArgs() throws BrokerCoreException {
		String[] args = new String[0];
		brokerCore = new BrokerCore(args);
		BrokerConfig props = brokerCore.getBrokerConfig();
		assertTrue("Broker URI is wrong",
				props.getBrokerURI().equals("rmi://localhost:1099/Broker1"));
		assertTrue("Broker heartbeat flag is wrong", !props.isHeartBeat());
		assertTrue("Broker neighbor count is wrong",
				props.getNeighborURIs().length == 0);
		assertTrue("Broker subscription covering flag is wrong",
				props.getSubCovering() == SubCoveringType.OFF);
		assertTrue("Broker advertisement covering flag is wrong",
				props.getAdvCovering() == AdvCoveringType.OFF);
		assertTrue("Broker cycles flag is wrong",
				props.getCycleOption() == CycleType.OFF);
		assertTrue("Broker db property file name is wrong", props
				.getDbPropertyFileName().equals("etc/db/db.properties"));
		assertTrue("Broker retry limit is wrong",
				props.getConnectionRetryLimit() == 30);
		assertTrue("Broker retry pause time is wrong",
				props.getConnectionRetryPause() == 10);
	}

	public void testCmdLineWithNullArgs() {
		String[] args = null;
		try {
			brokerCore = new BrokerCore(args);
		} catch (BrokerCoreException e) {
			return;
		}
		assertTrue("this part of the code should not be reached", false);
	}
}

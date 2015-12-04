package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * This dialog lets the user enter the necessary parameters to connect to the federation. The
 * connection is established through a single broker.
 */
// TODO: determine location for cache files and set programatically
// TODO: test without localhost
public class ConnectFederationDialog extends MonitorDialog {

	private static final long serialVersionUID = 9159396845312655152L;

	private JComboBox m_BrokerURLComboBox;

	/** Field for user to enter host name. */
	// private JComboBox m_HostComboBox;

	/** Field for user to enter port number. */
	// private JComboBox m_PortComboBox;

	// public final static String CACHE_HOSTS = "CACHE_HOSTS.txt";
	// public final static String CACHE_PORTS = "CACHE_PORTS.txt";

	public final static String BROKER_URLS = "broker_urls.txt";

	public final static String CACHE_DELIMITER = ";";

	// last used host (port) are placed between the below left and right enclosures
	public final static String LAST_USED_LEFT_ENCLOSURE = "{";

	public final static String LAST_USED_RIGHT_ENCLOSURE = "}";

	public StringBuffer qualifiedBrokerURLPath;

	// public StringBuffer qualifiedCacheHostsPath;
	//
	// public StringBuffer qualifiedCachePortsPath;

	public String userHome;

	List<String> urlsCache;

	// List<String> hostsCache;

	// List<String> portsCache;

	String lastUsedBrokerURL = "";

	// String lastUsedHost = "";
	//
	// String lastUsedPort = "";

	/**
	 * Construct a dialog that allows the user to enter connection parameters and connect to a
	 * federation of brokers.
	 * 
	 * @param owner
	 *            Hook to MonitorFrame.
	 */
	public ConnectFederationDialog(MonitorFrame owner) {
		super(owner, MonitorResources.T_CONNECT_FEDERATION);
	}

	/** @see TODO: MonitorDialog */
	public void buildContentPanel() {
		userHome = System.getProperty("user.home");

		// qualifiedCacheHostsPath = new StringBuffer(userHome);
		// qualifiedCacheHostsPath.append(System.getProperty("file.separator"));
		// // hardcoding tempfile directory for now... retrievable from somewhere?
		// qualifiedCacheHostsPath.append(".padres");
		// qualifiedCacheHostsPath.append(System.getProperty("file.separator"));
		// qualifiedCacheHostsPath.append(CACHE_HOSTS);
		//
		// qualifiedCachePortsPath = new StringBuffer(userHome);
		// qualifiedCachePortsPath.append(System.getProperty("file.separator"));
		// // hardcoding tempfile directory for now... retrievable from somewhere?
		// qualifiedCachePortsPath.append(".padres");
		// qualifiedCachePortsPath.append(System.getProperty("file.separator"));
		// qualifiedCachePortsPath.append(CACHE_PORTS);

		qualifiedBrokerURLPath = new StringBuffer(userHome);
		qualifiedBrokerURLPath.append(System.getProperty("file.separator"));
		// hardcoding tempfile directory for now... retrievable from somewhere?
		qualifiedBrokerURLPath.append(".padres");
		qualifiedBrokerURLPath.append(System.getProperty("file.separator"));
		qualifiedBrokerURLPath.append(BROKER_URLS);

		urlsCache = getCache(qualifiedBrokerURLPath.toString(), CACHE_DELIMITER);
		// hostsCache = getCache(qualifiedCacheHostsPath.toString(), CACHE_DELIMITER);
		// portsCache = getCache(qualifiedCachePortsPath.toString(), CACHE_DELIMITER);

		JLabel urlLabel = new JLabel(MonitorResources.L_BROKER_URL);
		// JLabel hostLabel = new JLabel(MonitorResources.L_HOSTNAME);
		// JLabel portLabel = new JLabel(MonitorResources.L_PORT);

		m_BrokerURLComboBox = new JComboBox(urlsCache.toArray());
		// m_HostComboBox = new JComboBox(hostsCache.toArray());
		// m_PortComboBox = new JComboBox(portsCache.toArray());

		m_BrokerURLComboBox.setEditable(true);
		// m_HostComboBox.setEditable(true);
		// m_PortComboBox.setEditable(true);

		add(urlLabel, 0, 0, 0.0, 0.0);
		add(m_BrokerURLComboBox, 1, 0, 1.0, 0.0);
		// add(hostLabel, 0, 0, 0.0, 0.0);
		// add(m_HostComboBox, 1, 0, 1.0, 0.0);
		// add(portLabel, 0, 1, 0.0, 0.0);
		// add(m_PortComboBox, 1, 1, 1.0, 0.0);
		add(new JLabel(), 0, 2, 2.0, 1.0); // padding
	}

	/**
	 * @see TODO: MonitorDialog
	 * @return The command identifier.
	 */
	public int getCommandID() {
		return MonitorResources.CMD_FEDERATION_CONNECT;
	}

	public String getBrokerURL() {
		String brokerURL = m_BrokerURLComboBox.getEditor().getItem().toString().trim();
		if (lastUsedBrokerURL.equals(brokerURL)) {
			// host already cached and labeled last used so do nothing
		} else {
			if (urlsCache.contains(brokerURL)) {
				storeInCache(qualifiedBrokerURLPath.toString(), CACHE_DELIMITER, urlsCache,
						brokerURL);
				lastUsedBrokerURL = brokerURL;
			} else {
				urlsCache.add(brokerURL);
				storeInCache(qualifiedBrokerURLPath.toString(), CACHE_DELIMITER, urlsCache,
						brokerURL);
				lastUsedBrokerURL = brokerURL;
			}
		}
		return brokerURL;
	}

	/**
	 * Return the contents of the host text field.
	 * 
	 * @return Host. <code>
	public String getHost() {
		String host = m_HostComboBox.getEditor().getItem().toString().trim();
		if (lastUsedHost.equals(host)) {
			// host already cached and labeled last used so do nothing
		} else {
			if (hostsCache.contains(host)) {
				storeInCache(qualifiedCacheHostsPath.toString(), CACHE_DELIMITER, hostsCache, host);
				lastUsedHost = host;
			} else {
				hostsCache.add(host);
				storeInCache(qualifiedCacheHostsPath.toString(), CACHE_DELIMITER, hostsCache, host);
				lastUsedHost = host;
			}
		}
		return host;
	}
	</code>
	 */

	/**
	 * Return the contents of the port text field.
	 * 
	 * @return Port. <code>
	public String getPort() {
		String port = m_PortComboBox.getEditor().getItem().toString().trim();
		if (lastUsedPort.equals(port)) {
			// port already cached and labeled last used so do nothing
		} else {
			if (portsCache.contains(port)) {
				storeInCache(qualifiedCachePortsPath.toString(), CACHE_DELIMITER, portsCache, port);
				lastUsedPort = port;
			} else {
				portsCache.add(port);
				storeInCache(qualifiedCachePortsPath.toString(), CACHE_DELIMITER, portsCache, port);
				lastUsedPort = port;
			}
		}
		return port;
	}
	</code>
	 */

	public void notify(Object o) {

	}

	private void storeInCache(String nameOfCacheFile, String delimiter, List<String> cache,
			String data) {
		BufferedWriter writer = null;
		File cacheFile = new File(nameOfCacheFile);
		if ((!cacheFile.exists()) || (!cacheFile.isFile())) {
			// LOG MESSAGE
			System.out.println("Cache file not found!");
			return;
		}
		StringBuffer lastUsedData = new StringBuffer();
		lastUsedData.append(LAST_USED_LEFT_ENCLOSURE);
		lastUsedData.append(data);
		lastUsedData.append(LAST_USED_RIGHT_ENCLOSURE);

		try {
			writer = new BufferedWriter(new FileWriter(nameOfCacheFile, false));
			for (int i = 0; i < cache.size(); i++) {
				String cacheData = cache.get(i);
				if (cacheData.equals(data)) {
					writer.write(CACHE_DELIMITER + lastUsedData.toString());
				} else {
					writer.write(CACHE_DELIMITER + cacheData);
				}
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ioe2) {
				ioe2.printStackTrace();
			}
		}
	}

	// where to put cache files?
	private List<String> getCache(String nameOfCacheFile, String delimiter) {
		List<String> cache = new ArrayList<String>();
		StringBuffer contents = new StringBuffer();
		BufferedReader reader = null;
		File cacheFile = new File(nameOfCacheFile);
		try {
			reader = new BufferedReader(new FileReader(cacheFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				contents.append(line);
			}
		} catch (FileNotFoundException fnfe) {
			if (!cacheFile.exists()) {
				try {
					File parentDir = new File(cacheFile.getParent());
					if (!parentDir.exists()) {
						parentDir.mkdir();
					}
					cacheFile.createNewFile();
					BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile, false));
					if (nameOfCacheFile == qualifiedBrokerURLPath.toString()) {
						writer.write(CACHE_DELIMITER + MonitorResources.D_BROKER_URL);
						contents.append(MonitorResources.D_BROKER_URL);
						// } else if (nameOfCacheFile == qualifiedCacheHostsPath.toString()) {
						// writer.write(CACHE_DELIMITER + MonitorResources.D_HOST);
						// contents.append(MonitorResources.D_HOST);
						// } else if (nameOfCacheFile == qualifiedCachePortsPath.toString()) {
						// writer.write(CACHE_DELIMITER + MonitorResources.D_PORT);
						// contents.append(MonitorResources.D_PORT);
					}
					writer.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			// return cache;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		StringTokenizer tok = new StringTokenizer(contents.toString(), delimiter);
		while (tok.hasMoreTokens()) {
			String data = tok.nextToken().toString().trim();
			if ((data.startsWith(LAST_USED_LEFT_ENCLOSURE))
					&& ((data.endsWith(LAST_USED_RIGHT_ENCLOSURE)))) {
				String lastUsedData = data.substring(LAST_USED_LEFT_ENCLOSURE.length(),
						data.indexOf(LAST_USED_RIGHT_ENCLOSURE));
				cache.add(0, lastUsedData);
				if (nameOfCacheFile == qualifiedBrokerURLPath.toString()) {
					lastUsedBrokerURL = lastUsedData;
					// } else if (nameOfCacheFile == qualifiedCacheHostsPath.toString()) {
					// lastUsedHost = lastUsedData;
					// } else if (nameOfCacheFile == qualifiedCachePortsPath.toString()) {
					// lastUsedPort = lastUsedData;
				}
			} else {
				cache.add(data);
			}
		}

		return cache;
	}

}

/*
 * Created on May 13, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.BrokerUI;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * The Diaologe for broker properties
 */
public class BrokerProperties extends MonitorDialog {
	
	/** JLabel for the broker id text */
	private JLabel m_brokerIDText;
	
	/** JLabel for the publication interval text */
	private JLabel m_pubIntervalText;
	
	/** JLabel for the input queue size text */
	private JLabel m_inputQueueSizeText;
	
	/** JLabel for the OS info */
	private JLabel m_osInfoText;
	
	/** JLabel for the Port */
	private JLabel m_portText;
	
	/** JLabel for the hostname */
	private JLabel m_hostnameText;
	
	/** JLabel for the version */
	private JLabel m_JvmVersionText;
	
	/** JLabel for the vendor */
	private JLabel m_JvmVendorText;
	
	/**
	* Construct a trace message dialog for the specified broker.
	* @param owner  Hook to MontiorFrame.
	* @param brokerID  The ID of the user selected broker.
	*/
	public BrokerProperties(MonitorFrame owner, BrokerUI broker) {
		super(owner, MonitorResources.T_BROKER_PROP);
	
		m_brokerIDText.setText(broker.getBrokerID());
		m_pubIntervalText.setText(broker.getPubInterval());
		m_inputQueueSizeText.setText(broker.getInputQueueSize());
		m_osInfoText.setText(broker.getOsInfo());
		m_portText.setText(broker.getPort());
		m_hostnameText.setText(broker.getHostName());
		m_JvmVersionText.setText(broker.getJVMVersion());
		m_JvmVendorText.setText(broker.getJVMVendor());
	
	}
	
	public void buildContentPanel() {
		JLabel brokerTitleLabel = new JLabel(MonitorResources.L_BROKER);
		JLabel pubIntervalTitleLable = new JLabel(MonitorResources.L_PUB_INTERVAL);
		JLabel inputQuquetitleLable = new JLabel(MonitorResources.L_INPUT_QUEUE_SIZE);
		JLabel neidhboursTitleLable = new JLabel(MonitorResources.L_NIGHBOURS);
		JLabel osInfoTitleLable = new JLabel(MonitorResources.L_OS_INFO);
		JLabel portLable = new JLabel(MonitorResources.L_PORT);
		JLabel hostLable = new JLabel(MonitorResources.L_HOSTNAME);
		JLabel jvmVersionLable = new JLabel(MonitorResources.L_JVM_VERSION);
		JLabel jvmVendorLable = new JLabel(MonitorResources.L_JVM_VENDOR);
		
		m_brokerIDText = new JLabel("");
		m_pubIntervalText = new JLabel("");
		m_inputQueueSizeText = new JLabel("");
		m_osInfoText = new JLabel("");
		m_portText = new JLabel("");
		m_hostnameText = new JLabel("");
		m_JvmVersionText = new JLabel("");
		m_JvmVendorText = new JLabel("");
	
		add(brokerTitleLabel, 0, 0, 0.0, 0.0);
		add(m_brokerIDText, 5, 0, 1.0,1.0);
		add(pubIntervalTitleLable, 0, 1, 0.0, 0.0);
		add(m_pubIntervalText, 5, 1, 1.0,1.0);
		add(inputQuquetitleLable, 0, 2, 0.0, 0.0);
		add(m_inputQueueSizeText, 5, 2, 1.0,1.0);
		add(osInfoTitleLable, 0, 3, 0.0, 0.0);
		add(m_osInfoText,5, 3, 1.0,1.0);
		add(portLable, 0, 4, 0.0, 0.0);
		add(m_portText,5, 4, 1.0,1.0);
		add(hostLable, 0, 5, 0.0, 0.0);
		add(m_hostnameText,5, 5, 1.0,1.0);
		add(jvmVersionLable, 0, 6, 0.0, 0.0);
		add(m_JvmVersionText,5, 6, 1.0,1.0);
		add(jvmVendorLable, 0, 7, 0.0, 0.0);
		add(m_JvmVendorText,5, 7, 1.0,1.0);
		
		pack(); // pack this dialog to its proper/preferred size
	}

	public int getCommandID() {
		return MonitorResources.CMD_PROPERTIES;
	}
	
	public void notify(Object o) {
		
	}

}

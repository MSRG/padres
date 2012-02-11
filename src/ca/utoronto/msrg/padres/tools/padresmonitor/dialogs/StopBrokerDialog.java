package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;



/**
* This dialog is a confirmation for the user when they want to stop a broker.
*/
public class StopBrokerDialog extends MonitorDialog
{
    /** ID of the broker to stop. */
    private String m_BrokerID;

    /** Label showing the ID of the selected broker. */
    private JLabel m_BrokerIDLabel;

    /**
    * Construct a stop broker dialog for the specified broker.
    * @param owner  Hook to MontiorFrame.
    * @param brokerID  The ID of the user selected broker.
    */
    public StopBrokerDialog(MonitorFrame owner, String brokerID) {
        super(owner, MonitorResources.T_STOP_BROKER);
        m_BrokerID = brokerID;
        m_BrokerIDLabel.setText(m_BrokerID);
    }

    
    /** @see TODO: MonitorDialog */
    public void buildContentPanel() {
        JLabel brokerLabel = new JLabel(MonitorResources.L_BROKER);
        m_BrokerIDLabel = new JLabel();

        add(brokerLabel, 0, 0, 0.0, 0.0);
        add(m_BrokerIDLabel, 1, 0, 1.0, 0.0);
        add(new JLabel(), 0, 1, 2.0, 1.0); // padding
    }

    /**
    * @see TODO: MonitorDialog
    * @return  The command identifier.
    */
    public int getCommandID() {
        return MonitorResources.CMD_BROKER_STOP;
    }

    /**
    * Return the broker ID of the selected broker.
    * @return  The broker ID.
    */
    public String getBrokerID() {
        return m_BrokerID;
    }
    
    public void notify(Object o) {
		
	}
    
}

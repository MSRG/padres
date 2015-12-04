package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

// is it a good idea to have monitor classes import brokercore classes?
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import ca.utoronto.msrg.padres.broker.brokercore.HeartbeatPublisher;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

public class FailureDetectionDialog extends MonitorDialog implements ActionListener
{
	private String m_BrokerID;
    private JLabel m_BrokerIDLabel;
    
    private JCheckBox m_HeartbeatEnableCheckbox;
    private JTextField m_HeartbeatIntervalField;
    private JTextField m_HeartbeatTimeoutField;
    private JTextField m_HeartbeatFailureThresholdField;
    
	public FailureDetectionDialog(MonitorFrame owner, String brokerID) {
        super(owner, MonitorResources.T_FAILURE_DETECTION);
        m_BrokerID = brokerID;
        m_BrokerIDLabel.setText(m_BrokerID);
        m_HeartbeatEnableCheckbox.setSelected(true);
        
        // set defaults
        m_HeartbeatIntervalField.setText(""+HeartbeatPublisher.DEFAULT_HEARTBEAT_INTERVAL);
        m_HeartbeatTimeoutField.setText(""+HeartbeatPublisher.DEFAULT_HEARTBEAT_TIMEOUT);
        m_HeartbeatFailureThresholdField.setText(""+HeartbeatPublisher.DEFAULT_FAILURE_THRESHOLD);
    }
    
    public void buildContentPanel() {
        JLabel brokerLabel = new JLabel(MonitorResources.L_BROKER);
        m_BrokerIDLabel = new JLabel();

        add(brokerLabel, 0, 0, 0.0, 0.0);
        add(m_BrokerIDLabel, 1, 0, 1.0, 0.0);
        
        JLabel enableLabel = new JLabel(MonitorResources.L_HEARTBEAT_ENABLED);
        add(enableLabel, 0, 1, 0.0, 0.0);

        m_HeartbeatEnableCheckbox = new JCheckBox();
        m_HeartbeatEnableCheckbox.addActionListener(this);
        add(m_HeartbeatEnableCheckbox, 1, 1, 1.0, 0.0);

        JLabel intervalLabel = new JLabel(MonitorResources.L_HEARTBEAT_INTERVAL);
        add(intervalLabel, 0, 2, 0.0, 0.0);

        m_HeartbeatIntervalField = new JTextField();
        add(m_HeartbeatIntervalField, 1, 2, 1.0, 0.0);

        JLabel timeoutLabel = new JLabel(MonitorResources.L_HEARTBEAT_TIMEOUT);
        add(timeoutLabel, 0, 3, 0.0, 0.0);
        
        m_HeartbeatTimeoutField = new JTextField();
        add(m_HeartbeatTimeoutField, 1, 3, 1.0, 0.0);
        
        JLabel failureThresholdLabel = new JLabel(MonitorResources.L_HEARTBEAT_FAILURE_THRESHOLD);
        add(failureThresholdLabel, 0, 4, 0.0, 0.0);
        
        m_HeartbeatFailureThresholdField = new JTextField();
        add(m_HeartbeatFailureThresholdField, 1, 4, 1.0, 0.0);

        add(new JLabel(), 0, 5, 2.0, 1.0); // padding
    }
    
    public int getCommandID() {
        return MonitorResources.CMD_FAILURE_DETECTION;
    }
    
    public String getBrokerID() {
        return m_BrokerID;
    }
    
    public void notify(Object o) {}
    
    public boolean validateFields() {
        // TODO: set error message if the fields are not valid
        return true;
    }

    public boolean getEnabled() {
        return m_HeartbeatEnableCheckbox.isSelected();
    }
    
    public long getInterval() {
        long interval;
        
        if (m_HeartbeatEnableCheckbox.isSelected())
            interval = Long.parseLong(m_HeartbeatIntervalField.getText());
        else
            interval = 0;

        return interval;
    }
    
    public long getTimeout() {
        long timeout;
        
        if (m_HeartbeatEnableCheckbox.isSelected())
            timeout = Long.parseLong(m_HeartbeatTimeoutField.getText());
        else
            timeout = 0;

        return timeout;
    }
    
    public int getThreshold() {
        int threshold;
        
        if (m_HeartbeatEnableCheckbox.isSelected())
            threshold = Integer.parseInt(m_HeartbeatFailureThresholdField.getText());
        else
            threshold = 0;

        return threshold;
    }
    
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == m_HeartbeatEnableCheckbox) {
            boolean enabled = m_HeartbeatEnableCheckbox.isSelected();
            
            m_HeartbeatIntervalField.setEnabled(enabled);
            m_HeartbeatTimeoutField.setEnabled(enabled);
            m_HeartbeatFailureThresholdField.setEnabled(enabled);
        }
        else {
            super.actionPerformed(event);
        }
    }
}
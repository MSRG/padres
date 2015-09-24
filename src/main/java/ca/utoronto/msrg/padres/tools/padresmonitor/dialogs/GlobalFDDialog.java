package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;


public class GlobalFDDialog extends MonitorDialog
{
	private JRadioButton m_EnabledRadioButton;
	private JRadioButton m_DisabledRadioButton;
	
    public GlobalFDDialog(MonitorFrame owner) {
        super(owner, MonitorResources.T_GLOBAL_FAILURE_DETECTION);
//        m_HeartbeatEnableCheckbox.setSelected(true);
    }

    public void buildContentPanel() {
    	JLabel label = new JLabel(MonitorResources.L_SET_GLOBAL_FD);
    	add(label, 0, 0, 0.0, 0.0);
    	
    	m_EnabledRadioButton = new JRadioButton(MonitorResources.B_ENABLED);
    	add(m_EnabledRadioButton, 0, 1, 0.0, 0.0);
    	
    	m_DisabledRadioButton = new JRadioButton(MonitorResources.B_DISABLED);
    	add(m_DisabledRadioButton, 1, 1, 0.0, 0.0);
    	
    	add(new JLabel(), 0, 2, 2.0, 1.0); // padding
    	
    	// set defaults for buttons and add them to a button group
    	ButtonGroup bg = new ButtonGroup();
    	bg.add(m_EnabledRadioButton);
    	bg.add(m_DisabledRadioButton);
    	m_EnabledRadioButton.setSelected(true);
    }
    
    public boolean enableFD() {
    	return m_EnabledRadioButton.isSelected();
    }
    
    public int getCommandID() {
    	return MonitorResources.CMD_GLOBAL_FAILURE_DETECTION;
    }
    
    public void notify(Object o) {}
}

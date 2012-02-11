/*
 * Created on May 19, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.awt.event.ActionEvent;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * @author Gerald
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class InjectMessageDialog extends MonitorDialog {
	
	public static final int INJ_TYPE_PUB=0;
	public static final int INJ_TYPE_SUB=1;
	public static final int INJ_TYPE_ADV=2;
	
	/** JLabel for the broker id text */
	private JLabel m_brokerIDText;
	
	private JRadioButton m_pubRadio;
		
	private JRadioButton m_subRadio;
	
	private JRadioButton m_advRadio;
	
	private JTextField m_AttributeTextField;
	
	private JTextField m_ValueTextField;
	
	private JTextField m_MsgTextField;
	
	private JSpinner m_OperatorSpinner;
	
	private JButton m_AddButton;
	
	private SpinnerListModel m_OperatorSpinnerModel;
	
	private ButtonGroup m_ButtonGroup;
	
	/**
	* Construct a inject message dialog for the specified broker.
	* @param owner  Hook to MontiorFrame.
	* @param brokerID  The ID of the user selected broker.
	*/
	public InjectMessageDialog(MonitorFrame owner, String brokerID) {
		super(owner, MonitorResources.T_INJECT_MESSAGE);
		m_brokerIDText.setText(brokerID);
			
	}

	
	public void buildContentPanel() {
		JLabel brokerTitleLabel = new JLabel(MonitorResources.L_BROKER);
		JLabel publication = new JLabel(MonitorResources.L_INJECT_MSG_PUB);
		JLabel subscription = new JLabel(MonitorResources.L_INJECT_MSG_SUB);
		JLabel advertisement = new JLabel(MonitorResources.L_INJECT_MSG_ADV);
		JLabel attribute = new JLabel(MonitorResources.L_INJECT_MSG_ATTRIBUTE);
		JLabel operator = new JLabel(MonitorResources.L_INJECT_MSG_OPERATOR);
		JLabel value = new JLabel(MonitorResources.L_INJECT_MSG_VALUE); 
		
		m_ButtonGroup = new ButtonGroup();
		m_pubRadio = new JRadioButton();
		m_ButtonGroup.add(m_pubRadio);
		
		m_subRadio = new JRadioButton();
		m_ButtonGroup.add(m_subRadio);
		
		m_advRadio =  new JRadioButton();
		m_ButtonGroup.add(m_advRadio);
				
		m_brokerIDText = new JLabel("");
		m_AttributeTextField = new JTextField("");
		m_ValueTextField = new JTextField("");
		m_MsgTextField = new JTextField("");
		m_OperatorSpinner = new JSpinner();
		
		m_OperatorSpinnerModel = new SpinnerListModel();
		Vector SpinnerList = new Vector();
		
		SpinnerList.add("eq");
		SpinnerList.add("isPresent");
		SpinnerList.add("=");
		SpinnerList.add("<");
		SpinnerList.add(">");
		
		m_OperatorSpinnerModel.setList(SpinnerList);
		m_OperatorSpinner.setModel(m_OperatorSpinnerModel);
		
		m_AddButton = new JButton(MonitorResources.B_ADD);
		m_AddButton.addActionListener(this);
		
		add(brokerTitleLabel, 0, 0, 0.0, 0.0);
		add(m_brokerIDText, 1, 0, 0.0, 0.0);
		add(m_pubRadio, 0,2,0.0,0.0);
		add(publication, 1,2,0.0,0.0);
		
		
		add(m_subRadio, 0,3,0.0,0.0);
		add(subscription, 1,3,0.0,0.0);
		
		
		add(m_advRadio, 0,4,0.0,0.0);
		add(advertisement, 1,4,0.0,0.0);
		add(attribute, 1,5,0.0,0.0);
		add(operator, 2,5,0.0,0.0);
		add(value, 3,5, 0.0,0.0);
		add(m_AttributeTextField, 1,6,0.0,0.0);
		add(m_ValueTextField, 3,6,0.0,0.0);
		add(m_OperatorSpinner, 2,6,0.0,0.0);
		add(m_AddButton, 3,7,0.0,0.0);
		add(m_MsgTextField, 1,8,5,3,0,40);

	}

	public int getCommandID() {
		return MonitorResources.CMD_INJECT_MESSAGE;
	}

	/* Add newly inputed value to the text area */
	public void actionPerformed(ActionEvent e) {
		
			super.actionPerformed(e);

			Object source = e.getSource();
			
			if (source == m_AddButton) {
				addToMsg();
			}
			
	}

	/**
	 * Function to add new selection to the end of the message
	 *
	 */
	private void addToMsg() {
		String attribute = m_AttributeTextField.getText();
		String operator = (String)m_OperatorSpinner.getValue(); 
		String value = m_ValueTextField.getText();
		String msgAddition = "";
		ButtonModel buttonModel;
		
		buttonModel = m_ButtonGroup.getSelection();
				
		if (!(attribute.equals("") && value.equals(""))) {
			
			if(m_pubRadio.isSelected()) {
				//	publication
				msgAddition = "["+attribute+","+"'"+value+"']";
				m_subRadio.setEnabled(false);
				m_advRadio.setEnabled(false);
			}
			else if (m_subRadio.isSelected() || m_advRadio.isSelected()){
				// Subscription and advertisement
				msgAddition = "["+attribute+","+operator+","+"'"+value+"']";
				m_pubRadio.setEnabled(false);
			}
			
			m_MsgTextField.setText(m_MsgTextField.getText()+msgAddition);
			
		}
	}
	
	public String getMsgText() {
		
		return m_MsgTextField.getText();
	}
	
	public int getMsgType() {
		
		int result = -1;
		if (m_pubRadio.isSelected()){
			result = INJ_TYPE_PUB;
		}
		else if (m_subRadio.isSelected()) {
			result = INJ_TYPE_SUB;
		}
		else if (m_advRadio.isSelected()){
			result = INJ_TYPE_ADV;
		}
		
		return result;
		
	}

	public String getBrokerID() {
		return m_brokerIDText.getText();
	}
	
	public void notify(Object o) {
		
	}

}

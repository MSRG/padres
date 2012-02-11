/*
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;

import ca.utoronto.msrg.padres.tools.padresmonitor.ClientInjectionManager;
import ca.utoronto.msrg.padres.tools.padresmonitor.InjectMessageStore;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;



/**
 * @author Gerald
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class UnInjectMessageDialog extends MonitorDialog {
	
	/** JLabel for the broker id text */
	private JLabel m_brokerIDText;
	
	private JSpinner m_InjectMsgSpinner;
	
	private SpinnerListModel m_InjectMsgSpinnerModel;
	
	private ClientInjectionManager m_InjectManager;
	
	private MonitorFrame m_owner;
	
	private String m_brokerID;
	
	public static final String BLANK_INJECT_MSG="No Inject Message"; 
		
	/**
	* Construct a uninject message dialog for the specified broker.
	* @param owner  Hook to MontiorFrame.
	* @param brokerID  The ID of the user selected broker.
	*/
	public UnInjectMessageDialog(MonitorFrame owner, String brokerID) {
		super(MonitorResources.T_UNINJECT_MESSAGE, owner);
		m_brokerIDText = new JLabel();
		m_brokerIDText.setText(brokerID);
		m_InjectManager = owner.getOverlayManager().getClientInjectionManager();
		m_brokerID = brokerID;
		super.MonitorDialogDraw(owner, MonitorResources.T_UNINJECT_MESSAGE);
	}

	public void buildContentPanel() {
		JLabel brokerTitleLabel = new JLabel(MonitorResources.L_BROKER);
		JLabel InjectedMassage = new JLabel(MonitorResources.L_INJECTED_MESSAGE);
		
		
		m_InjectMsgSpinner = new JSpinner();
		
		
		m_InjectMsgSpinnerModel = new SpinnerListModel();
		Vector injectMsgStoreList = m_InjectManager.getMsgStore(m_brokerID);
		if (injectMsgStoreList == null || injectMsgStoreList.size() == 0) {
			injectMsgStoreList = new Vector();
			injectMsgStoreList.add(BLANK_INJECT_MSG);
		}
		m_InjectMsgSpinnerModel.setList(injectMsgStoreList);
		m_InjectMsgSpinner.setModel(m_InjectMsgSpinnerModel);
		
		add(brokerTitleLabel, 0, 0, 0.0, 0.0);
		add(m_brokerIDText, 1, 0, 0.0, 0.0);
		add(m_InjectMsgSpinner, 0,2,0.0,0.0);
		
	}

	public int getCommandID() {
		return MonitorResources.CMD_UNINJECT_MESSAGE;
	}

	public String getBrokerID() {
		return m_brokerIDText.getText();
	}

	/**
	 * Return the inject message store
	 * @return The data structure that have all the inject message in it
	 */
	public InjectMessageStore getInjectMsgStore() {
		Object o = m_InjectMsgSpinner.getValue();
		InjectMessageStore store = null;
		if (o instanceof InjectMessageStore) {
			store = (InjectMessageStore)o;
		}
		return store;
	}
	
	public void notify(Object o) {
		
	}
}

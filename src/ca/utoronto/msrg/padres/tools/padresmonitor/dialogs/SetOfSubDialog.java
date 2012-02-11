/*
 * Created on Aug 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.tools.padresmonitor.ClientMonitorCommandManager;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;



/**
 * @author Gerald Chan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SetOfSubDialog extends MonitorDialog{
	
	private JLabel m_brokerIDText;
	
	private ClientMonitorCommandManager m_commManager;
	
	private JScrollPane m_ScrollPane;
	private JTextArea m_TextArea;
	
	public SetOfSubDialog(MonitorFrame owner, String brokerID, ClientMonitorCommandManager comm) {
		super(owner,MonitorResources.T_SET_SUB);
		
		m_brokerIDText.setText(brokerID);
		m_commManager = comm;
		m_commManager.sendGetMsgSetCommand(brokerID, ClientMonitorCommandManager.TYPE_SUB);
		m_commManager.setNotifyDialog(this);
	}
	
	/* (non-Javadoc)
	 * @see monitor.dialogs.MonitorDialog#buildContentPanel()
	 */
	public void buildContentPanel() {
		JLabel brokerTitleLabel = new JLabel(MonitorResources.L_BROKER);
		m_brokerIDText = new JLabel("");
		m_TextArea = new JTextArea();
		m_TextArea.setLineWrap(false);
		m_ScrollPane = new JScrollPane(m_TextArea);
	
		add(m_brokerIDText, 1, 0, 0.0, 0.0);
		add(m_ScrollPane, 0,3,1.0,1.0);
		add(new JLabel(" "), 0,5,0.0,0.0);
		pack();
		
	}
	/* (non-Javadoc)
	 * @see monitor.dialogs.MonitorDialog#getCommandID()
	 */
	public int getCommandID() {
		// TODO Auto-generated method stub
		return MonitorResources.CMD_SET_SUB;
	}

	/*
	 *  (non-Javadoc)
	 * @see monitor.dialogs.MonitorDialog#notify(java.lang.Object)
	 */
	public void notify(Object o) {
		//Map setMsg = (Map)o;
		Set setMsg = (Set)o;
		Iterator i;
		
		for (i = setMsg.iterator(); i.hasNext();) {
			SubscriptionMessage sMsg = (SubscriptionMessage) i.next();
			m_TextArea.append(sMsg.toString()+"\n");
		}
		setSize(640,480);
		//all the messages are retrieved from here
		//i need to parse the messages and add to the filter options list
		//and messages table
	}

}

/*
 * Created on Aug 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.tools.padresmonitor.ClientMonitorCommandManager;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

//formerly NewSetOfSubDialog
public class NewSetOfSubDialog extends MonitorDialog implements ListSelectionListener {

	public final static int EXPANDED_WIDTH = 200;
	public final static int COLLAPSED_WIDTH = 0;
	
	private String currentBroker;
	private Map<String, Set> filterCache;
	private Set<String> currentCache;

	private JLabel m_brokerIDText;
	private ClientMonitorCommandManager m_commManager;
	
	private JBrokerList brokerList;
	private JScrollPane brokerListScrollPane;

	private JFilterOptionsList filterOptionsList;
	private JScrollPane filterOptionsListScrollPane;
	
//	private MessagesTableModel messagesTableModel;
//	private JTable messagesTable;
	private JSubscriptionMessagesTable messagesTable;
	private JScrollPane messagesTableScrollPane;
	
	private JSplitPane listsSplitPane;
	private JSplitPane listsTableSplitPane;
	
	int clickCounter;
	
	public NewSetOfSubDialog(MonitorFrame owner, String brokerID, ClientMonitorCommandManager comm) {
		super(owner,MonitorResources.T_SET_SUB);
		
		currentBroker = brokerID;
		m_brokerIDText.setText(currentBroker);
		m_commManager = comm;
//		m_commManager.sendGetMsgSetCommand(brokerID, ClientMonitorCommandManager.TYPE_SUB);
//		m_commManager.setNotifyDialog(this);
		brokerList.setSelectedValue(currentBroker, true);
	}
	
	public void buildContentPanel() {
		filterCache = new HashMap<String, Set>();
		currentCache = new HashSet<String>();
		m_brokerIDText = new JLabel("");
		add(m_brokerIDText, 1, 0, 0.0, 0.0);
		
		brokerList = new JBrokerList(getMonitorFrame().getOverlayManager());
		brokerList.addListSelectionListener(this);
		brokerListScrollPane = new JScrollPane(brokerList);
		brokerListScrollPane.setColumnHeaderView(new JLabel("BROKER LIST"));

		filterOptionsList = new JFilterOptionsList();
		filterOptionsList.addListSelectionListener(this);
		filterOptionsListScrollPane = new JScrollPane(filterOptionsList);
		filterOptionsListScrollPane.setColumnHeaderView(new JLabel("FILTER OPTIONS"));
		
		listsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, brokerListScrollPane, filterOptionsListScrollPane);
		
//		messagesTableModel = new MessagesTableModel();
//		messagesTable = new JTable(messagesTableModel);
		messagesTable = new JSubscriptionMessagesTable();
		messagesTableScrollPane = new JScrollPane(messagesTable);
		listsTableSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listsSplitPane, messagesTableScrollPane);
		add(listsTableSplitPane, 0,3,1.0,1.0);
		pack();
		clickCounter = 0;
	}

	public int getCommandID() {
		return MonitorResources.CMD_SET_SUB;
	}

	public void notify(Object o) {
		//need this to get the messages and parse accordingly
		Set setMsgs = (Set) o;
		messagesTable.init();
		messagesTable.setMessages(setMsgs);
		
//		messagesTable.setAutoCreateColumnsFromModel(false);
//		messagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		//get messages
//		Set setMsgs = (Set) o;
//		Iterator it;
//		Set currentCache = (Set) filterCache.get(currentBroker);
//		messagesTableModel.setColumns(o);
//		Vector<TableColumn> allColumns = messagesTableModel.getAllColumns();
//		for (int i=0; i<allColumns.size(); i++) {
//			TableColumn col = allColumns.get(i); 
//			if ((currentCache != null) && (currentCache.contains(col.getHeaderValue().toString()))) {
//				col.setPreferredWidth(COLLAPSED_WIDTH);
//			} else {
//				col.setPreferredWidth(EXPANDED_WIDTH);
//			}
//			
//			messagesTable.getColumnModel().addColumn(col);
//			//add column to filter options list
//		}
//		
//		for (it = setMsgs.iterator(); it.hasNext();) {
//			SubscriptionMessage sMsg = (SubscriptionMessage) it.next();
//			messagesTableModel.addMessageToModel(sMsg);
//		}
//		messagesTableModel.fireTableDataChanged();
//		
//		//add filter options to panel and hide columns
//		
//		
//		filterOptionsList.addFilterOptions(messagesTableModel.getHeaderColumnNames(), messagesTableModel.getContentColumnNames(), (Set) filterCache.get(currentBroker));
		filterOptionsList.addFilterOptions(messagesTable.getHeaderColumnNames(), messagesTable.getContentColumnNames(), (Set) filterCache.get(currentBroker));

//		
//		
		listsSplitPane.resetToPreferredSizes();
		listsTableSplitPane.resetToPreferredSizes();
//		
	}
	
	public void valueChanged(ListSelectionEvent lse) {
		//are values still being changed? if so, return.
        if (lse.getValueIsAdjusting()) {
        	return;
        }
        Object source = lse.getSource();
        if (source == brokerList) {
        	filterCache.put(currentBroker, filterOptionsList.getCheckedColumnNames());        	
        	reset();
        	String newBroker = brokerList.getSelectedValue().toString();
        	currentBroker = newBroker;    	
            m_brokerIDText.setText(newBroker);
            m_commManager.sendGetMsgSetCommand(newBroker, ClientMonitorCommandManager.TYPE_SUB);
    		m_commManager.setNotifyDialog(this);        	
        } else if (source == filterOptionsList) {
        	int index = lse.getFirstIndex();
        	if (index != lse.getLastIndex()) {
        		return;
        	}
        	if (filterOptionsList.isChecked(index)) {
        		messagesTable.hideColumn(index);
//        		messagesTable.getColumnModel().getColumn(index).setPreferredWidth(COLLAPSED_WIDTH);
        	} else if (!filterOptionsList.isChecked(lse.getFirstIndex())) {
        		messagesTable.showColumn(index);
//        		messagesTable.getColumnModel().getColumn(index).setPreferredWidth(EXPANDED_WIDTH);        		
        	}
    		messagesTable.invalidate();
    		messagesTable.doLayout();
    		messagesTable.repaint();
        }
	}
	
//	public void itemStateChanged(ItemEvent e) {
//
//    	int selectedFilterOptionIndex = filterOptionsList.getSelectedIndex();
//
//    	FilterOptionsListCellRenderer effectedItem = (FilterOptionsListCellRenderer) e.getItem();
//    	System.out.println("Item effected by event: " + effectedItem.toString());
//    	if (e.getStateChange() == ItemEvent.SELECTED) {
//    		messagesTable.getColumnModel().getColumn(selectedFilterOptionIndex).setPreferredWidth(0);    		
//    	} else if (e.getStateChange() == ItemEvent.DESELECTED) {
//    		messagesTable.getColumnModel().getColumn(selectedFilterOptionIndex).setPreferredWidth(250);
//    	}
//    	clickCounter++;
//    	System.out.println("Get State: " + e.getStateChange());
//		messagesTable.invalidate();
//		messagesTable.doLayout();
//		messagesTable.repaint();
//	}
	
	private void reset() {
        filterOptionsList.clear();
        messagesTable.clear();
    	//remove all columns of the jtable
//        Vector<TableColumn> allColumns = messagesTableModel.getAllColumns();
//        
//        for (int i=0; i<allColumns.size(); i++) {
//        	TableColumn col = (TableColumn) allColumns.get(i);
//        	messagesTable.removeColumn(col);        	
//        }
//        //clear all data
//        messagesTableModel.clear();
		
	}
	
}


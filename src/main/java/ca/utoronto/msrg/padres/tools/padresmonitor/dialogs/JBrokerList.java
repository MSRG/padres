package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import ca.utoronto.msrg.padres.tools.padresmonitor.OverlayManager;

public class JBrokerList extends JList {
	//do I have to worry about serializable?
	
	private DefaultListModel brokerListModel;
	private OverlayManager m_overlayManager;
	
	//is passing in overlay manager a problem?
	//is it better to just pass in the broker list? I THINK SO
	//TODO pass in broker list instead of overlay manager
	public JBrokerList(OverlayManager overlayManager) {
		brokerListModel = new DefaultListModel();
		m_overlayManager = overlayManager;
		populateListModelWithBrokers();
		setListData(brokerListModel.toArray());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	private void populateListModelWithBrokers() {
		//TreeSet to sort the list of broker IDs
		Set brokerList = m_overlayManager.getBrokerList().keySet();
		TreeSet brokerListSorted = new TreeSet(brokerList);
		for (Iterator it = brokerListSorted.iterator(); it.hasNext();) {
			String brokerID = (String) it.next();
			System.out.println("brokerID: " + brokerID);
			brokerListModel.addElement(brokerID);
		}
		
	}
	
	public String getLabelAtIndex(int index) {
		return brokerListModel.get(index).toString();
	}
	
	public void clear() {
		brokerListModel.clear();
	}
	
}

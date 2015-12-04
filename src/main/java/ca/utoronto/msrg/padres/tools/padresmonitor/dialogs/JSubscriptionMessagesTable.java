package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;


public class JSubscriptionMessagesTable extends JTable {

	MessagesTableModel dataModel;
	Map<Integer, TableColumn> hiddenColumnModelIndexCache;
	
	public JSubscriptionMessagesTable() {
		init();
	}
	
	public void init() {
		dataModel = new MessagesTableModel();
		hiddenColumnModelIndexCache = new HashMap<Integer, TableColumn>();
		setModel(dataModel);
		setAutoCreateColumnsFromModel(false);
		setAutoResizeMode(AUTO_RESIZE_OFF);		
	}
	
	public void setMessages(Set<Message> messages) {
		dataModel.setColumns(messages);
		Vector<TableColumn> allColumns = dataModel.getAllColumns();
		for (int i=0; i<allColumns.size(); i++) {
			TableColumn col = allColumns.get(i); 			
			getColumnModel().addColumn(col);
		}
		
		for (Iterator<Message> it = messages.iterator(); it.hasNext();) {
			SubscriptionMessage sMsg = (SubscriptionMessage) it.next();
			dataModel.addMessageToModel(sMsg);
		}
		dataModel.fireTableDataChanged();
		
//		for (int i=0; i<getColumnModel().getColumnCount(); i++) {
//			System.out.println("Col [" + i + "]: " + getColumnModel().getColumn(i).getHeaderValue());
//		}

	}

	public String[] getHeaderColumnNames() {
		return dataModel.getHeaderColumnNames();
	}

	public String[] getContentColumnNames() {
		return dataModel.getContentColumnNames();
	}
	
	public void clear() {
		Vector<TableColumn> allColumns = dataModel.getAllColumns();
		for (int i=0; i<allColumns.size(); i++) {
			TableColumn col = allColumns.get(i); 			
			getColumnModel().removeColumn(col);
		}
		hiddenColumnModelIndexCache.clear();
		dataModel.clear();
	}
	
	public void hideColumn(int modelIndex) {
		int viewIndex = convertColumnIndexToView(modelIndex);
		TableColumn column = getColumnModel().getColumn(viewIndex);
		hiddenColumnModelIndexCache.put(new Integer(modelIndex), column);
		getColumnModel().removeColumn(column);
	}
	
	public void showColumn(int index) {
		Integer columnIndex = new Integer(index);
		if (hiddenColumnModelIndexCache.containsKey(columnIndex)) {
			int targetViewIndex = getAdjustedColumnViewLocation(columnIndex, getColumnModel().getColumnCount()-1);
			TableColumn column = hiddenColumnModelIndexCache.get(columnIndex);
			getColumnModel().addColumn(column);
			getColumnModel().moveColumn(getColumnModel().getColumnCount()-1, targetViewIndex);
			hiddenColumnModelIndexCache.remove(columnIndex);
		}		
	}
	
	private int getAdjustedColumnViewLocation(int newColumnModelIndex, int lastColumnViewIndex) {

		if (newColumnModelIndex >= convertColumnIndexToModel(lastColumnViewIndex)) {
			int newColumnViewIndex = lastColumnViewIndex+1;
			return (newColumnViewIndex);
		} else {
			int nextToLastColumnViewIndex = lastColumnViewIndex-1;
			return getAdjustedColumnViewLocation(newColumnModelIndex, nextToLastColumnViewIndex);
		}
		
	}
}

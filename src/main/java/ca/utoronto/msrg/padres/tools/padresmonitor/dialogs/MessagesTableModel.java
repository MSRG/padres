package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;


public class MessagesTableModel extends AbstractTableModel {

	public Map<String, Integer>headerColumnsIndices;
	public Map<String, Integer>contentColumnsIndices;
	public Vector<TableColumn>headerColumns;
	public Vector<TableColumn>contentColumns; 
	
	public Vector<RowData>allData;
	
	
	public MessagesTableModel() {
		headerColumnsIndices = new HashMap<String, Integer>();
		contentColumnsIndices = new HashMap<String, Integer>();
		headerColumns = new Vector<TableColumn>();
		contentColumns = new Vector<TableColumn>();	
		allData = new Vector<RowData>();
//		insertDefaultColumns();
		
	}
	
	public int getColumnCount() {
		// since I set setAutoCreateColumnsFromModel to false, I read
		// that this should be 0
		return 0;
	}

	public int getRowCount() {
		// TODO Auto-generated method stub
		return allData.size();
	}

	public Object getValueAt(int arg0, int arg1) {
		//arg0 = rowIndex
		//arg1 = columnIndex
		
		//get row 
		RowData row = allData.get(arg0);

		//find column value
		Object value = row.getDataForColumn(arg1);
		
		//int arg0 changes
		//int arg1 is always 0
		//System.out.println("row: " + arg0 + " col: " + arg1);

		return value;
	}

	//inner class to handle row data
	private class RowData {
		private Message msg;
		private Object[] dataPerColumn; 
//		private Object[] headerDataPerColumn;
//		private Object[] contentDataPerColumn;
		
		public RowData(Message msg) {
			this.msg = msg;
			initArrays();
			setMsg();
		}
		
		private void initArrays() {
			dataPerColumn = new Object[headerColumns.size() + contentColumns.size() + 1];
			for (int i=0; i<dataPerColumn.length; i++) {
				dataPerColumn[i] = "";
			}
//			headerDataPerColumn = new Object[headerColumns.size()];
//			for (int i=0; i<headerDataPerColumn.length; i++) {
//				headerDataPerColumn[i] = "";
//			}
//			contentDataPerColumn = new Object[contentColumns.size()];
//			for (int i=0; i<contentDataPerColumn.length; i++) {
//				contentDataPerColumn[i] = "";
//			}
			
		}
		
		public void setMsg() {
			Map headerData = msg.getAllHeaderFields();
			Iterator it;
			//set header data
			for (it=headerData.keySet().iterator(); it.hasNext();) {
				String fieldName = it.next().toString();

				Integer colIndex = headerColumnsIndices.get(fieldName);
				dataPerColumn[colIndex.intValue()] = headerData.get(fieldName);
//				headerDataPerColumn[colIndex.intValue()] = headerData.get(fieldName);
//				System.out.println(fieldName + " = " + headerData.get(fieldName));
			}
			//set content data
			if (msg instanceof AdvertisementMessage) {
				
			} else if (msg instanceof SubscriptionMessage) {
				SubscriptionMessage sMsg = (SubscriptionMessage) msg;
				Subscription sub = sMsg.getSubscription();
				Map predicateMap = sub.getPredicateMap();
				for (it = predicateMap.keySet().iterator(); it.hasNext();) {
					String attributeName = it.next().toString();
					Integer colIndex = contentColumnsIndices.get(attributeName)  ;
					dataPerColumn[headerColumns.size() + colIndex.intValue()] = predicateMap.get(attributeName);
//					contentDataPerColumn[headerColumns.size() + colIndex.intValue()] = predicateMap.get(attributeName);
				}
			}
			
		}
		
		public Object getDataForColumn(int columnIndex) {
			if ((columnIndex >= 0) && (columnIndex < dataPerColumn.length)) {
				return dataPerColumn[columnIndex];
			} else return "";
		}
		
		
	}
	
	public void addMessageToModel(Message msg) {
//		System.out.println("Message: " + msg.toString());
		
//		if (msg.toString().startsWith("SubscriptionMessage(A-M924):")) {
//			System.out.println("NOW");
//		}
		//insert the data into the model 
		RowData row = new RowData(msg);
		allData.add(row);
		
//		fireTableStructureChanged();
//		fireTableDataChanged();
	}
	
	public Vector<TableColumn> getAllColumns() {
		Vector<TableColumn> allColumns = new Vector<TableColumn>();
		for (int i=0; i<headerColumns.size(); i++) {
			allColumns.add(headerColumns.get(i)); 
		}
		for (int i=headerColumns.size(); i<headerColumns.size()+contentColumns.size(); i++) {
			allColumns.add(contentColumns.get(i-headerColumns.size()));
		}
			
//		for (int i=0; i<allColumns.size(); i++) {
//			System.out.println("ColumnName[" + i + "]: " + allColumns.get(i).getHeaderValue());
//		}
		return allColumns;
	}
	
	public String[] getHeaderColumnNames() {
		
		String[] headerColumnNames = new String[headerColumnsIndices.size()];
		for (Iterator it=headerColumnsIndices.keySet().iterator(); it.hasNext();) {
			String colName = it.next().toString();
			Integer colIndex = headerColumnsIndices.get(colName);
			headerColumnNames[colIndex] = colName;
		}
		return headerColumnNames;
		
	}

	public String[] getContentColumnNames() {
		
		String[] contentColumnNames = new String[contentColumnsIndices.size()];
		for (Iterator it=contentColumnsIndices.keySet().iterator(); it.hasNext();) {
			String colName = it.next().toString();
			Integer colIndex = contentColumnsIndices.get(colName);
			contentColumnNames[colIndex] = colName;
		}
		return contentColumnNames;
		
	}
	
	
//	public ArrayList<String> getHeaderColumnsNames() {
//		ArrayList<String> headerColumnsNames = new ArrayList<String>();
//		headerColumnsNames.ensureCapacity(headerColumnsIndices.size());
//		for (Iterator it=headerColumnsIndices.keySet().iterator(); it.hasNext();) {
//			String colName = it.next().toString();
//			headerColumnsNames.add(headerColumnsIndices.get(colName), colName);
//		}
//		
//		ArrayList<String> headerColumnsNames = new ArrayList<String>(headerColumnsIndices.keySet());
//		return headerColumnsNames;
//	}
	
//	public ArrayList<String> getContentColumnsNames() {
//		ArrayList<String> contentColumnsNames = new ArrayList<String>(contentColumnsIndices.size());
//		for (Iterator it=contentColumnsIndices.keySet().iterator(); it.hasNext();) {
//			String colName = it.next().toString();
//			contentColumnsNames.add(contentColumnsIndices.get(colName), colName);
//		}
//		return contentColumnsNames;
//	}
	
	public void setColumns(Object setOfMessages) {
		Set messagesMap = (Set) setOfMessages;
		Iterator it;
		//add "== HEADER == column
		insertDefaultHeaderColumn();
		//collect all header column names first
		for (it = messagesMap.iterator(); it.hasNext();) {
			Message msg = (Message) it.next();
			addAnyNewHeaderColumns(msg);
//			addAnyNewContentColumns(msg);
		}
		//set column index of "== CONTENT ==" column
		insertDefaultContentColumn(headerColumns.size()); 
		//collect all content column names next
		for (it = messagesMap.iterator(); it.hasNext();) {
			Message msg = (Message) it.next();
//			addAnyNewHeaderColumns(msg);
			addAnyNewContentColumns(msg);
		}
	}
	
	public void clear() {
		//clear all data structures
		allData.clear();
		headerColumns.clear();
		contentColumns.clear();
		headerColumnsIndices.clear();
		contentColumnsIndices.clear();
//		insertDefaultColumns();
	}
	
	private void addAnyNewHeaderColumns(Message msg) {
		SubscriptionMessage subMsg = (SubscriptionMessage) msg; 
		Map headerFields = subMsg.getAllHeaderFields();
		Iterator it;
		for(it = headerFields.keySet().iterator(); it.hasNext();) {
			String fieldName = it.next().toString();
			if (headerColumnsIndices.containsKey(fieldName)) {
				//do nothing
			} else {
				int colIndex = headerColumnsIndices.size();
				headerColumnsIndices.put(fieldName, new Integer(colIndex));
				TableColumn col = new TableColumn(colIndex);
				col.setHeaderValue(fieldName);
				headerColumns.add(col);
			}
		}
	}
	
	private void addAnyNewContentColumns(Message msg) {
		Iterator it;
		SubscriptionMessage subMsg = (SubscriptionMessage) msg;
		Subscription sub = subMsg.getSubscription();
    	Map predicateMap = sub.getPredicateMap();
    	for (it = predicateMap.keySet().iterator(); it.hasNext();) {
    		String fieldName = it.next().toString();
    		if (contentColumnsIndices.containsKey(fieldName)) {
    			//do nothing
    		} else {
    			int colIndex = contentColumnsIndices.size();
    			contentColumnsIndices.put(fieldName, new Integer(colIndex));
    			TableColumn col = new TableColumn(colIndex + headerColumns.size());
    			col.setHeaderValue(fieldName);
    			contentColumns.add(col);
    		}
    	}
	}
	
//	private void insertDefaultColumns() {
//		TableColumn headerCol = new TableColumn();
//		headerCol.setHeaderValue(" == HEADER == ");
//		headerColumnsIndices.put(" == HEADER == ", new Integer(0));
//		headerColumns.add(0, headerCol);
//		
//		TableColumn contentCol = new TableColumn();
//		contentCol.setHeaderValue(" == CONTENT == ");
//		contentColumnsIndices.put(" == CONTENT == ", new Integer(0));
//		contentColumns.add(0, contentCol);
//
//	}
	
	private void insertDefaultHeaderColumn() {
		TableColumn headerCol = new TableColumn();
		headerCol.setHeaderValue(" == HEADER == ");
		headerColumnsIndices.put(" == HEADER == ", new Integer(0));
		headerColumns.add(0, headerCol);		
	}
	
	private void insertDefaultContentColumn(int numberOfHeaderColumns) {
		TableColumn contentCol = new TableColumn(numberOfHeaderColumns);
		contentCol.setHeaderValue(" == CONTENT == ");
		contentColumnsIndices.put(" == CONTENT == ", 0);
		contentColumns.add(0, contentCol);

	}
}
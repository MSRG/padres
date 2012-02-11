package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

public class JFilterOptionsList extends JList {

	private DefaultListModel filterOptionsListModel;
	

	public JFilterOptionsList() {
		filterOptionsListModel = new DefaultListModel();
		setModel(filterOptionsListModel);
		FilterOptionsListCellRenderer cellRenderer = new FilterOptionsListCellRenderer();
		//cellRenderer.addMouseListener(l)
//		cellRenderer.addItemListener(listener);
		setCellRenderer(cellRenderer);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int index = locationToIndex(e.getPoint());
				Rectangle r = getCellBounds(index, index);
				CheckableListItem item = (CheckableListItem)filterOptionsListModel.getElementAt(index);
				item.setSelected(!item.isSelected());
				filterOptionsListModel.setElementAt(item, index);
				repaint(r);
				fireSelectionValueChanged(index, index, false); 
			}
		});
		
		
		
	}

	public void addFilterOptions(String[] headerFilterOptions, String[] contentFilterOptions, Set checkedItems) {
		boolean temp;
		if (checkedItems == null) {
			for (int i=0; i<headerFilterOptions.length; i++) {
				filterOptionsListModel.addElement(new CheckableListItem(headerFilterOptions[i], false));
			}
			for (int i=0; i<contentFilterOptions.length; i++) {
				filterOptionsListModel.addElement(new CheckableListItem(contentFilterOptions[i], false));
			}
		} else {
			for (int i=0; i<headerFilterOptions.length; i++) {
				filterOptionsListModel.addElement(new CheckableListItem(headerFilterOptions[i], checkedItems.contains(headerFilterOptions[i])));
			}
			for (int i=0; i<contentFilterOptions.length; i++) {
				filterOptionsListModel.addElement(new CheckableListItem(contentFilterOptions[i], checkedItems.contains(contentFilterOptions[i])));
			}			
		}
	}
	
	public class FilterOptionsListCellRenderer extends JCheckBox implements ListCellRenderer {
		
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
			
			CheckableListItem item = (CheckableListItem)value;
			this.setText(item.toString());
			this.setSelected(item.isSelected());

			if (hasFocus) {        
				 setBackground(list.getSelectionBackground());
	             setForeground(list.getSelectionForeground());
	         } else {
	             setBackground(list.getBackground());
	             setForeground(list.getForeground());
	         }
			return this;
		}
	}
	
	public void clear() {
		filterOptionsListModel.clear();
	}

	public boolean filterOptionIsSelected(int index) {
		CheckableListItem item = (CheckableListItem) filterOptionsListModel.get(index);

		if (item == null) {
			return false;
		}
		
		return item.isSelected();
		
	}
	
	public ListModel getListModel() {
		return filterOptionsListModel;
	}
	
	public boolean isChecked(int index) {
		if ((index < 0) || (index >= filterOptionsListModel.size())) {
			return false;
		}
		CheckableListItem item = (CheckableListItem)filterOptionsListModel.getElementAt(index);
		return item.isSelected();
	}
	
	public Set<String> getCheckedColumnNames() {
		Set<String> checkedColumns = new HashSet<String>();
		for (int i=0; i<filterOptionsListModel.size(); i++) {
			CheckableListItem item = (CheckableListItem) filterOptionsListModel.get(i);
			if (item.isSelected()) {
				checkedColumns.add(item.toString());
			}
		}
		
		return checkedColumns;
	}
	
	class CheckableListItem {
		private String label;
		private boolean isSelected;
		
		public CheckableListItem(String label, boolean isSelected) {
			this.label = label;
			this.isSelected = isSelected;
		}
		
		public void setSelected(boolean isSelected) {
			this.isSelected = isSelected;
		}
		
		public boolean isSelected() {
			return isSelected;
		}
		
		public String toString() {
			return label;
		}
	}
}
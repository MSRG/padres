/*
 * Created on 2004-8-26
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.guiclient.dialogs;

/**
 * @author Li Guoli
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;

public class ConstraintsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5906832090594660679L;

	DisplayAreaPanel dpanel = new DisplayAreaPanel();

	// PaddingPanel ppanel = new PaddingPanel();

	String dpaneltip = "display area attributes", afpaneltip = "component attributes",
			ppaneltip = "padding";

	public ConstraintsPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(dpanel);
		add(Box.createVerticalStrut(15));

		// add(Box.createVerticalStrut(15));
		// //add(ppanel);
		// add(Box.createVerticalStrut(15));
		//		
		// add(Box.createVerticalStrut(15));
	}

	public void setConstraints(GridBagConstraints gbc) {

		dpanel.setGridx(gbc.gridx);

	}

	public GridBagConstraints getConstraints() {
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = dpanel.getGridx();

		return gbc;
	}

	public String getApplName() {
		String name = dpanel.ipadxField.getText();
		return name;

	}

	public String getGID() {
		String gid = dpanel.ipadyField.getText();
		return gid;
	}

	public String getSchedule() {
		// String schedule = (String) dpanel.gridxCombo.getItemAt(0);
		String schedule = (String) dpanel.yearCombo.getSelectedItem();
		schedule += "." + (dpanel.monthCombo.getSelectedIndex() + 1);
		schedule += "." + (String) dpanel.dayCombo.getSelectedItem();
		return schedule;
	}
}

/*
 * class PaddingPanel extends JPanel { JButton issue_Button = new JButton("Issue"); JButton
 * cancel_Button = new JButton("Cancel");
 * 
 * 
 * public PaddingPanel() { GridBagLayout gbl = new GridBagLayout(); GridBagConstraints gbc = new
 * GridBagConstraints();
 * 
 * setLayout(gbl); gbc.anchor = GridBagConstraints.NORTHWEST;
 * 
 * add(issue_Button, gbc); add(Box.createHorizontalStrut(20), gbc); add(cancel_Button, gbc);
 * add(Box.createHorizontalStrut(20), gbc);
 * 
 * 
 * setBorder(new CompoundBorder( BorderFactory.createTitledBorder("To Do"),
 * BorderFactory.createEmptyBorder(10,10,10,10)));
 * 
 * issue_Button.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e) {
 * System.out.println("issue trigger...by button");
 * 
 * } });
 * 
 * cancel_Button.addActionListener(new ActionListener(){ public void actionPerformed(ActionEvent e)
 * { //dispose(); System.out.println("issue trigger...by button");
 * 
 * } }); }
 * 
 * }
 */
class DisplayAreaPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7728486218671604732L;

	public final static int START_YEAR = 2000;

	public final static int END_YEAR = 2010;

	public final static int FIRST_DAY = 1;

	public final static int LAST_DAY = 31;

	JLabel gridxLabel = new JLabel("Schedule:");

	JComboBox gridxCombo = new JComboBox();

	JLabel yearLabel = new JLabel("Year :");

	JLabel monthLabel = new JLabel("Month:");

	JLabel dayLabel = new JLabel("Day  :");

	JComboBox yearCombo = new JComboBox();

	JComboBox monthCombo = new JComboBox();

	JComboBox dayCombo = new JComboBox();

	JLabel ipadxLabel = new JLabel("APPL_NAME"), ipadyLabel = new JLabel("GID");

	JTextField ipadxField = new JTextField(3), ipadyField = new JTextField(3);

	int padX, padY;

	private void addToolTips() {
		gridxLabel.setToolTipText("Trigger Schedule");

		gridxCombo.setToolTipText("Date");

	}

	public DisplayAreaPanel() {
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();

		setLayout(gbl);
		gbc.anchor = GridBagConstraints.NORTHWEST;

		add(ipadxLabel, gbc);
		add(Box.createHorizontalStrut(10), gbc);
		add(ipadxField, gbc);
		add(Box.createHorizontalStrut(20), gbc);
		add(Box.createVerticalStrut(15));
		add(ipadyLabel, gbc);
		add(Box.createHorizontalStrut(10), gbc);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.5;
		add(ipadyField, gbc);

		setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Internal Padding"),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		add(gridxLabel, gbc);
		add(Box.createHorizontalStrut(10), gbc);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		// add(gridxCombo, gbc);
		add(yearLabel, gbc);
		add(yearCombo, gbc);
		add(monthLabel, gbc);
		add(monthCombo, gbc);
		add(dayLabel, gbc);
		add(dayCombo, gbc);

		gbc.gridwidth = 1;

		addToolTips();

		setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Define Trigger"),
				BorderFactory.createEmptyBorder(10, 10, 10, 10)));

		gridxCombo.addItem("DAILY");
		gridxCombo.addItem("MONDAY");
		gridxCombo.addItem("TUSEDAY");
		gridxCombo.addItem("WEDNESDAY");
		gridxCombo.addItem("THURSDAY");
		gridxCombo.addItem("FRIDAY");
		gridxCombo.addItem("WORKDAY");

		// Add year, month, day combo
		for (int i = START_YEAR; i <= END_YEAR; i++) {
			yearCombo.addItem("" + i);
		}
		monthCombo.addItem("January");
		monthCombo.addItem("Febrary");
		monthCombo.addItem("March");
		monthCombo.addItem("April");
		monthCombo.addItem("May");
		monthCombo.addItem("June");
		monthCombo.addItem("July");
		monthCombo.addItem("August");
		monthCombo.addItem("September");
		monthCombo.addItem("Octobor");
		monthCombo.addItem("November");
		monthCombo.addItem("December");
		for (int i = FIRST_DAY; i <= LAST_DAY; i++) {
			dayCombo.addItem("" + i);
		}

		int year = Calendar.getInstance().get(Calendar.YEAR);
		int month = Calendar.getInstance().get(Calendar.MONTH);
		int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
		// index of Combox is from 0
		//yearCombo.setSelectedIndex(year - START_YEAR);
		monthCombo.setSelectedIndex(month);
		dayCombo.setSelectedIndex(day - 1);
	}

	public void setGridx(int gridx) {
		if (gridx == GridBagConstraints.RELATIVE)
			gridxCombo.setSelectedItem("RELATIVE");
		else
			gridxCombo.setSelectedIndex(gridx + 1);
		repaint();
	}

	public int getGridx() {
		String x = (String) gridxCombo.getSelectedItem();
		int rv;

		if (x.equals("RELATIVE"))
			rv = GridBagConstraints.RELATIVE;
		else {
			rv = Integer.parseInt((String) gridxCombo.getSelectedItem());
		}
		return rv;
	}

}

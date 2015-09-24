package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * This is the (abstract) superclass of all dialogs in the PADRES system
 * monitor.
 */
public abstract class MonitorDialog extends JDialog implements ActionListener {
	/** Hook to parent MonitorFrame. */
	private MonitorFrame m_MonitorFrame;

	/** The OK button. */
	private JButton m_OkButton;

	/** The Cancel button. */
	private JButton m_CancelButton;

	/** Panel that subclasses will add their widgets to. */
	protected JPanel m_ContentPanel;

	/** Layout manager for this dialog. */
	protected GridBagLayout m_GridBagLayout;

	/** Constraints object for this dialog's layout manager. */
	protected GridBagConstraints m_GridBagConstraints;

	/**
	 * Construct a new MonitorDialog.
	 * 
	 * @param owner
	 *            Hook to Monitor frame.
	 * @param title
	 *            This dialog's title.
	 */
	public MonitorDialog(MonitorFrame owner, String title) {
		// changing from true to false so dialog boxes aren't modal
		super(owner, title, false);
		// setDefaultLookAndFeelDecorated(true);
		m_MonitorFrame = owner;
		init();
		buildContentPanel(); // let the subclass add its widgets
		getRootPane().setDefaultButton(m_OkButton); // set default button
		pack(); // pack this dialog to its proper/preferred size
		setLocationRelativeTo(m_MonitorFrame);
	}

	public MonitorDialog(String title, MonitorFrame owner) {
		// changing from true to false so dialog boxes aren't modal
		super(owner, title, false);
	}

	public void MonitorDialogDraw(MonitorFrame owner, String title) {
		m_MonitorFrame = owner;
		init();
		buildContentPanel(); // let the subclass add its widgets
		getRootPane().setDefaultButton(m_OkButton); // set default button
		pack(); // pack this dialog to its proper/preferred size
		setLocationRelativeTo(m_MonitorFrame);
	}

	/**
	 * Create layout manager, buttons, etc.
	 */
	private void init() {
		m_OkButton = new JButton(MonitorResources.B_OK);
		m_OkButton.addActionListener(this);
		m_CancelButton = new JButton(MonitorResources.B_CANCEL);
		m_CancelButton.addActionListener(this);
		m_ContentPanel = new JPanel(); // panel where subclasses add their
										// widgets
		JPanel buttonPanel = new JPanel();
		m_GridBagLayout = new GridBagLayout();
		m_GridBagConstraints = new GridBagConstraints();
		Container contentPane = getContentPane();

		// set the layout manager for this dialog
		contentPane.setLayout(m_GridBagLayout);
		buttonPanel.setLayout(m_GridBagLayout);
		m_ContentPanel.setLayout(m_GridBagLayout);

		// add the content panel (for sub classes) to the content pane
		m_GridBagConstraints.gridx = 0;
		m_GridBagConstraints.gridy = 0;
		m_GridBagConstraints.gridwidth = 1;
		m_GridBagConstraints.gridheight = 1;
		m_GridBagConstraints.weightx = 1.0;
		m_GridBagConstraints.weighty = 1.0;
		m_GridBagConstraints.fill = GridBagConstraints.BOTH;
		m_GridBagConstraints.insets = new Insets(4, 4, 4, 4);
		contentPane.add(m_ContentPanel, m_GridBagConstraints);

		// add the button panel to the content pane
		m_GridBagConstraints.gridx = 0;
		m_GridBagConstraints.gridy = 1;
		m_GridBagConstraints.gridwidth = 1;
		m_GridBagConstraints.gridheight = 1;
		m_GridBagConstraints.weightx = 0.0;
		m_GridBagConstraints.weighty = 0.0;
		m_GridBagConstraints.fill = GridBagConstraints.NONE;
		m_GridBagConstraints.anchor = GridBagConstraints.SOUTHEAST;
		contentPane.add(buttonPanel, m_GridBagConstraints);

		// add buttons to the button panel
		m_GridBagConstraints.gridx = 0;
		m_GridBagConstraints.gridy = 0;
		buttonPanel.add(m_OkButton, m_GridBagConstraints);
		m_GridBagConstraints.gridx = 1;
		m_GridBagConstraints.gridy = 0;
		buttonPanel.add(m_CancelButton, m_GridBagConstraints);
	}

	/**
	 * Invoked when an action occurs. Specified in java.awt.event.ActionListener
	 * interface. We only care about the OKand Cancel buttons here.
	 * 
	 * @param e
	 *            ActionEvent for action that occurred.
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();

		if (source == m_CancelButton) {
			m_MonitorFrame.cancelCommand();
		} else if (source == m_OkButton) {
			m_MonitorFrame.executeCommand(getCommandID());
		}
	}

	/**
	 * Get the ID of the command that this dialog should execute. This method
	 * must be implemented by subclasses.
	 * 
	 * @return The command ID.
	 */
	public abstract int getCommandID();

	/**
	 * Add widgets to the content panel (subclass portion) of this dialog.
	 * Subclasses implement this method to add their widgets to the content
	 * panel.
	 */
	public abstract void buildContentPanel();

	/**
	 * Call this method to add a widget to the subclass/content area of this
	 * dialog.
	 * 
	 * @param comp
	 *            The component (widget) to be added.
	 * @param gridx
	 *            The x location of the widget.
	 * @param gridy
	 *            The y location of the widget.
	 * @param weightx
	 *            Horizontal weight of the widget.
	 * @param weighty
	 *            Vertical weight of the widget.
	 */
	public void add(Component comp, int gridx, int gridy, double weightx,
			double weighty) {
		m_GridBagConstraints.gridx = gridx;
		m_GridBagConstraints.gridy = gridy;
		m_GridBagConstraints.weightx = weightx;
		m_GridBagConstraints.weighty = weighty;
		m_GridBagConstraints.fill = GridBagConstraints.BOTH;
		m_GridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

		m_ContentPanel.add(comp, m_GridBagConstraints);
	}

	/**
	 * Call this method to add a widget to the subclass/content area of this
	 * dialog.
	 * 
	 * @param comp
	 *            The component (widget) to be added.
	 * @param gridx
	 *            The x location of the widget.
	 * @param gridy
	 *            The y location of the widget.
	 * @param weightx
	 *            Horizontal weight of the widget.
	 * @param weighty
	 *            Vertical weight of the widget.
	 */
	public void add(Component comp, int gridx, int gridy, int gridwidth,
			int gridheight, int ipadx, int ipady) {
		m_GridBagConstraints.gridx = gridx;
		m_GridBagConstraints.gridy = gridy;
		m_GridBagConstraints.gridwidth = gridwidth;
		m_GridBagConstraints.gridheight = gridheight;
		m_GridBagConstraints.ipadx = ipadx;
		m_GridBagConstraints.ipady = ipady;
		m_GridBagConstraints.fill = GridBagConstraints.BOTH;
		m_GridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

		m_ContentPanel.add(comp, m_GridBagConstraints);
	}

	/**
	 * Call this method to add a widget to the subclass/content area of this
	 * dialog.
	 * 
	 * @param comp
	 *            The component (widget) to be added.
	 * @param gridx
	 *            The x location of the widget.
	 * @param gridy
	 *            The y location of the widget.
	 * @param weightx
	 *            Horizontal weight of the widget.
	 * @param weighty
	 *            Vertical weight of the widget.
	 */
	public void add(Component comp, int gridx, int gridy, int gridwidth,
			int gridheight, double weightx, double weighty) {
		m_GridBagConstraints.gridx = gridx;
		m_GridBagConstraints.gridy = gridy;
		m_GridBagConstraints.gridwidth = gridwidth;
		m_GridBagConstraints.gridheight = gridheight;
		m_GridBagConstraints.weightx = weightx;
		m_GridBagConstraints.weighty = weighty;
		m_GridBagConstraints.fill = GridBagConstraints.BOTH;
		m_GridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

		m_ContentPanel.add(comp, m_GridBagConstraints);
	}

	/**
	 * Function for the user of the dialog to pass message (or update)into the
	 * dialog
	 * 
	 * @param o
	 *            anything that the notify message needed to update the dialog
	 */
	public abstract void notify(Object o);

	public MonitorFrame getMonitorFrame() {
		return m_MonitorFrame;
	}
}

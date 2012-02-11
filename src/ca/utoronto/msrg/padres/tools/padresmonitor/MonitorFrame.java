package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.common.util.LogSetup;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.BatchMsgDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.BatchMsgIncrDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.BrokerProperties;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.BrokerStatistic;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.ConnectFederationDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.DisconnectFederationDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.FailureDetectionDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.GlobalFDDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.InjectMessageDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.MonitorDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.ResumeBrokerDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.SetOfAdvDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.SetOfSubDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.ShutdownBrokerDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.StopBrokerDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.TraceIDTraceDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.TracePubDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.TraceSubDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.UnInjectMessageDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.dialogs.UnTracePubDialog;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorVertex;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * Main window of the PADRES system monitor.
 */
public class MonitorFrame extends JFrame implements ActionListener, MenuListener {

	private static final long serialVersionUID = -2475747638105865292L;

	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	private String monitorID;

	private MonitorClient monitorClient;

	/** Overlay network manager. */
	private OverlayManager overlayManager;

	private ClientMonitorCommandManager cmdManager;

	/** Command dialog. */
	private MonitorDialog m_CommandDialog;

	/** Text description of a command error. */
	private String m_ErrorString;

	/** Status area at bottom of MonitorFrame. */
	private JLabel m_StatusLabel;

	/* menus */
	private JMenu m_MainMenu;

	private JMenu m_BrokerMenu;

	private JMenu m_FirstBrokerMenu;

	private JMenu m_ApplyLayoutSubmenu;

	private JMenu m_EdgeThroughputSubmenu;

	/* menu items that have associated actions */
	private JMenuItem m_ExitMenuItem;

	private JMenuItem m_FailureDetectionMenuItem;

	private JMenuItem m_FederationConnectMenuItem;

	private JMenuItem m_FederationDisconnectMenuItem;

	private JMenuItem m_GlobalFDMenuItem;

	private JMenuItem m_StopBrokerMenuItem;

	private JMenuItem m_ResumeBrokerMenuItem;

	private JMenuItem m_ShutdownBrokerMenuItem;

	private JMenuItem m_InjectMessageMenuItem;

	private JMenuItem m_UnInjectMessageMenuItem;

	private JMenuItem m_TracePublicationMenuItem;

	private JMenuItem m_TraceSubscriptionMenuItem;

	private JMenuItem m_UnTracePublicationMenuItem;

	private JMenuItem m_PropertiesMenuItem;

	private JMenuItem m_BrokerStatMenuItem;

	private JMenuItem m_SetAdvMenuItem;

	private JMenuItem m_SetSubMenuItem;

	private JMenuItem m_BatchMsgMenuItem;

	private JMenuItem m_BatchMsgIncrMenuItem;

	private JMenuItem m_TraceByTraceIDMenuItem;

	private JMenuItem m_RefreshFederationMenuItem;

	private JMenuItem m_ShowAllEdgeMessagesMenuItem;

	private JMenuItem m_HideAllEdgeMessagesMenuItem;

	private JCheckBoxMenuItem m_showFullLabelsMenuItem;

	/* layout submenu items */
	private JMenuItem m_CircleLayoutSubmenuItem;

	private JMenuItem m_FRLayoutSubmenuItem;

	private JMenuItem m_ISOMLayoutSubmenuItem;

	private JMenuItem m_KKLayoutSubmenuItem;

	private JMenuItem m_KKLayoutIntSubmenuItem;

	private JMenuItem m_SpringLayoutSubmenuItem;

	private JMenuItem m_StaticLayoutSubmenuItem;

	/* edge throughput submenu items */
	private JMenuItem m_EdgeThroughputOnSubmenuItem;

	private JMenuItem m_EdgeThroughputOffSubmenuItem;

	private JMenuItem m_EdgeThroughputResetSubmenuItem;

	/**
	 * Constructs a new MonitorFrame object.
	 * 
	 * @throws ClientException
	 */
	public MonitorFrame() throws ClientException {
		super(MonitorResources.T_MONITOR_FRAME);
		monitorID = "Monitor";
		// create the overlay manager
		monitorClient = new MonitorClient(monitorID);
		cmdManager = new ClientMonitorCommandManager(this, monitorClient);
		overlayManager = new OverlayManager(this, cmdManager, monitorClient);
		overlayManager.start();
		monitorClient.addMessageQueue(overlayManager.getMsgQueue());
		// build and layout the contents of this window
		buildContentPane();
		pack(); // pack the window to its preferred size
	}

	public String getMonitorID() {
		return monitorID;
	}

	/**
	 * Build and layout the contents of this window.
	 */
	private void buildContentPane() {
		// Step 1) setup the menu bar
		setJMenuBar(createMenuBar());

		GridBagLayout layout = new GridBagLayout();
		Container contentPane = getContentPane();
		contentPane.setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();

		// Step 2) setup the graph area
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		// contentPane.add(m_OverlayManager.getOverlayUI(), constraints);
		contentPane.add(overlayManager.getOverlayUI(), constraints);

		// Step 3) setup the status bar
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(layout);
		m_StatusLabel = new JLabel();
		m_StatusLabel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
		// initially we're not connected to anyone
		m_StatusLabel.setText(MonitorResources.L_NOT_CONNECTED);

		// add the status label to the status panel
		// constraints.gridx = 0;
		// constraints.gridy = 0;
		// constraints.weightx = 1.0;
		// constraints.weighty = 1.0;
		// constraints.fill = GridBagConstraints.BOTH;
		// constraints.anchor = GridBagConstraints.NORTHWEST;
		statusPanel.add(m_StatusLabel, constraints);

		// add the status panel to the window
		// constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.weightx = 0.0;
		constraints.weighty = 0.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		contentPane.add(statusPanel, constraints);
	}

	/**
	 * Create and return the menu bar for this MonitorFrame.
	 * 
	 * @return the main menu bar for this frame.
	 */
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// build the main menu
		m_MainMenu = new JMenu(MonitorResources.M_MAIN);
		m_MainMenu.addMenuListener(this);
		menuBar.add(m_MainMenu);

		m_FederationConnectMenuItem = new JMenuItem(MonitorResources.M_CONNECT_FEDERATION);
		m_FederationConnectMenuItem.addActionListener(this);
		m_FederationConnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
				ActionEvent.ALT_MASK));
		m_MainMenu.add(m_FederationConnectMenuItem);

		m_FederationDisconnectMenuItem = new JMenuItem(MonitorResources.M_DISCONNECT_FEDERATION);
		m_FederationDisconnectMenuItem.addActionListener(this);
		m_MainMenu.add(m_FederationDisconnectMenuItem);

		m_MainMenu.addSeparator();

		m_RefreshFederationMenuItem = new JMenuItem(MonitorResources.M_REFRESH);
		m_RefreshFederationMenuItem.addActionListener(this);
		m_RefreshFederationMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
		m_MainMenu.add(m_RefreshFederationMenuItem);

		// build layout submenu
		m_ApplyLayoutSubmenu = new JMenu(MonitorResources.M_APPLY_LAYOUT);
		m_ApplyLayoutSubmenu.addMenuListener(this);
		m_MainMenu.add(m_ApplyLayoutSubmenu);

		m_CircleLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_CIRCLE);
		m_CircleLayoutSubmenuItem.addActionListener(this);
		m_CircleLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0));
		m_ApplyLayoutSubmenu.add(m_CircleLayoutSubmenuItem);

		m_FRLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_FRUCHTERMAN_REINGOLD);
		m_FRLayoutSubmenuItem.addActionListener(this);
		m_FRLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0));
		m_ApplyLayoutSubmenu.add(m_FRLayoutSubmenuItem);

		m_ISOMLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_ISOM);
		m_ISOMLayoutSubmenuItem.addActionListener(this);
		m_ISOMLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0));
		m_ApplyLayoutSubmenu.add(m_ISOMLayoutSubmenuItem);

		m_KKLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_KAMADA_KAWAI);
		m_KKLayoutSubmenuItem.addActionListener(this);
		m_KKLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0));
		m_ApplyLayoutSubmenu.add(m_KKLayoutSubmenuItem);

		m_KKLayoutIntSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_KAMADA_KAWAI_INTEGER);
		m_KKLayoutIntSubmenuItem.addActionListener(this);
		m_KKLayoutIntSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, 0));
		m_ApplyLayoutSubmenu.add(m_KKLayoutIntSubmenuItem);

		m_SpringLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_SPRING);
		m_SpringLayoutSubmenuItem.addActionListener(this);
		m_SpringLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, 0));
		m_ApplyLayoutSubmenu.add(m_SpringLayoutSubmenuItem);

		m_StaticLayoutSubmenuItem = new JMenuItem(MonitorResources.M_LAYOUT_STATIC);
		m_StaticLayoutSubmenuItem.addActionListener(this);
		m_StaticLayoutSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, 0));
		m_ApplyLayoutSubmenu.add(m_StaticLayoutSubmenuItem);

		m_showFullLabelsMenuItem = new JCheckBoxMenuItem(MonitorResources.M_SHOW_FULL_LABELS);
		m_showFullLabelsMenuItem.addActionListener(this);
		m_showFullLabelsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0));
		m_MainMenu.add(m_showFullLabelsMenuItem);

		m_MainMenu.addSeparator();

		m_GlobalFDMenuItem = new JMenuItem(MonitorResources.M_GLOBAL_FAILURE_DETECTION);
		m_GlobalFDMenuItem.addActionListener(this);
		m_MainMenu.add(m_GlobalFDMenuItem);

		m_MainMenu.addSeparator();

		m_ShowAllEdgeMessagesMenuItem = new JMenuItem(
				MonitorResources.M_SHOW_ALL_EDGE_MESSAGE_COUNTER);
		m_ShowAllEdgeMessagesMenuItem.addActionListener(this);
		m_ShowAllEdgeMessagesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0));
		m_MainMenu.add(m_ShowAllEdgeMessagesMenuItem);

		m_HideAllEdgeMessagesMenuItem = new JMenuItem(
				MonitorResources.M_HIDE_ALL_EDGE_MESSAGE_COUNTER);
		m_HideAllEdgeMessagesMenuItem.addActionListener(this);
		m_HideAllEdgeMessagesMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, 0));
		m_MainMenu.add(m_HideAllEdgeMessagesMenuItem);

		m_MainMenu.addSeparator();

		// build edge throughput thickness submenu

		m_EdgeThroughputSubmenu = new JMenu(MonitorResources.M_EDGE_THROUGHPUT_INDICATOR);
		m_EdgeThroughputSubmenu.addMenuListener(this);
		m_MainMenu.add(m_EdgeThroughputSubmenu);

		m_EdgeThroughputOnSubmenuItem = new JMenuItem(
				MonitorResources.M_EDGE_THROUGHPUT_INDICATOR_ON);
		m_EdgeThroughputOnSubmenuItem.addActionListener(this);
		m_EdgeThroughputOnSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0));
		m_EdgeThroughputSubmenu.add(m_EdgeThroughputOnSubmenuItem);
		m_EdgeThroughputOnSubmenuItem.setEnabled(true);

		m_EdgeThroughputOffSubmenuItem = new JMenuItem(
				MonitorResources.M_EDGE_THROUGHPUT_INDICATOR_OFF);
		m_EdgeThroughputOffSubmenuItem.addActionListener(this);
		m_EdgeThroughputSubmenu.add(m_EdgeThroughputOffSubmenuItem);
		m_EdgeThroughputOffSubmenuItem.setEnabled(false);

		m_EdgeThroughputResetSubmenuItem = new JMenuItem(
				MonitorResources.M_EDGE_THROUGHPUT_INDICATOR_RESET);
		m_EdgeThroughputResetSubmenuItem.addActionListener(this);
		m_EdgeThroughputResetSubmenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
		m_EdgeThroughputSubmenu.add(m_EdgeThroughputResetSubmenuItem);

		m_MainMenu.addSeparator();

		m_ExitMenuItem = new JMenuItem(MonitorResources.M_EXIT);
		m_ExitMenuItem.addActionListener(this);
		m_ExitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		m_MainMenu.add(m_ExitMenuItem);

		// build the broker menu
		m_BrokerMenu = new JMenu(MonitorResources.M_BROKER);
		m_BrokerMenu.addMenuListener(this);
		menuBar.add(m_BrokerMenu);

		m_StopBrokerMenuItem = new JMenuItem(MonitorResources.M_STOP_BROKER);
		m_StopBrokerMenuItem.addActionListener(this);
		//m_BrokerMenu.add(m_StopBrokerMenuItem);

		m_ResumeBrokerMenuItem = new JMenuItem(MonitorResources.M_RESUME_BROKER);
		m_ResumeBrokerMenuItem.addActionListener(this);
		//m_BrokerMenu.add(m_ResumeBrokerMenuItem);

		m_ShutdownBrokerMenuItem = new JMenuItem(MonitorResources.M_SHUTDOWN_BROKER);
		m_ShutdownBrokerMenuItem.addActionListener(this);
		// Shutdown feature has not been implemented, so removing from menu
		m_BrokerMenu.add(m_ShutdownBrokerMenuItem);

		m_BrokerMenu.addSeparator();

		m_InjectMessageMenuItem = new JMenuItem(MonitorResources.M_INJECT_MESSAGE);
		m_InjectMessageMenuItem.addActionListener(this);
		// removing from menu bar for initial Padres release
		m_BrokerMenu.add(m_InjectMessageMenuItem);

		m_UnInjectMessageMenuItem = new JMenuItem(MonitorResources.M_UNINJECT_MESSAGE);
		m_UnInjectMessageMenuItem.addActionListener(this);
		// removing from menu bar for initial Padres release
		m_BrokerMenu.add(m_UnInjectMessageMenuItem);

		m_TracePublicationMenuItem = new JMenuItem(MonitorResources.M_TRACE_PUB_MESSAGE);
		m_TracePublicationMenuItem.addActionListener(this);
		//m_BrokerMenu.add(m_TracePublicationMenuItem);

		m_TraceSubscriptionMenuItem = new JMenuItem(MonitorResources.M_TRACE_SUB_MESSAGE);
		m_TraceSubscriptionMenuItem.addActionListener(this);
		//m_BrokerMenu.add(m_TraceSubscriptionMenuItem);

		m_UnTracePublicationMenuItem = new JMenuItem(MonitorResources.M_UNTRACE_MESSAGE);
		m_UnTracePublicationMenuItem.addActionListener(this);
		//m_BrokerMenu.add(m_UnTracePublicationMenuItem);

		m_SetAdvMenuItem = new JMenuItem(MonitorResources.M_SET_ADV);
		m_SetAdvMenuItem.addActionListener(this);
		m_BrokerMenu.add(m_SetAdvMenuItem);

		m_SetSubMenuItem = new JMenuItem(MonitorResources.M_SET_SUB);
		m_SetSubMenuItem.addActionListener(this);
		m_BrokerMenu.add(m_SetSubMenuItem);

		m_BrokerMenu.addSeparator();

		m_PropertiesMenuItem = new JMenuItem(MonitorResources.M_PROPERTIES);
		m_PropertiesMenuItem.addActionListener(this);
		m_BrokerMenu.add(m_PropertiesMenuItem);

		m_BrokerStatMenuItem = new JMenuItem(MonitorResources.M_STAT);
		m_BrokerStatMenuItem.addActionListener(this);
		// removing from menu bar for initial Padres release
		// m_BrokerMenu.add(m_BrokerStatMenuItem);

		m_FailureDetectionMenuItem = new JMenuItem(MonitorResources.M_FAILURE_DETECTION);
		m_FailureDetectionMenuItem.addActionListener(this);
		m_BrokerMenu.add(m_FailureDetectionMenuItem);

		m_TraceByTraceIDMenuItem = new JMenuItem(MonitorResources.M_TRACE_BY_TRACEID);
		m_TraceByTraceIDMenuItem.addActionListener(this);
		m_TraceByTraceIDMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0));
		m_BrokerMenu.add(m_TraceByTraceIDMenuItem);

		// build the first broker menu
		m_FirstBrokerMenu = new JMenu(MonitorResources.M_FIRST_BROKER);
		m_FirstBrokerMenu.addMenuListener(this);
		menuBar.add(m_FirstBrokerMenu);

		m_BatchMsgMenuItem = new JMenuItem(MonitorResources.M_BATCH_MSG);
		m_BatchMsgMenuItem.addActionListener(this);
		m_FirstBrokerMenu.add(m_BatchMsgMenuItem);

		m_BatchMsgIncrMenuItem = new JMenuItem(MonitorResources.M_BATCH_MSG_INCR);
		m_BatchMsgIncrMenuItem.addActionListener(this);
		m_FirstBrokerMenu.add(m_BatchMsgIncrMenuItem);

		return menuBar;
	}

	/**
	 * Enable or disable the menu bar (i.e., set whether it is clickable).
	 */
	private void enableMenuBar(boolean enable) {
		for (MenuElement e : this.getJMenuBar().getSubElements())
			e.getComponent().setEnabled(enable);
	}
	
	/**
	 * Override of setVisible to position this frame at the center of the display.
	 * 
	 * @param b
	 *            If true, display the monitor frame.
	 */
	public void setVisible(boolean b) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
		super.setVisible(b);
	}

	/**
	 * From MenuListener interface. Invoked when the user cancels a menu selection.
	 * 
	 * @param e
	 *            Menu cancellation event triggered by user.
	 */
	public void menuCanceled(MenuEvent e) {
	}

	/**
	 * From MenuListener interface. Invoked when the user deselects a menu.
	 * 
	 * @param e
	 *            Menu deselection event triggered by user.
	 */
	public void menuDeselected(MenuEvent e) {
	}

	/**
	 * From MenuListener interface. Invoked when the user selects a menu. This method
	 * enables/disables the appropriate menu items depending on the state of the monitor before
	 * displaying the menu to the user.
	 * 
	 * @param e
	 *            Menu selection event triggered by user.
	 */
	public void menuSelected(MenuEvent e) {
		Object source = e.getSource();

		if (source == m_MainMenu) {
			if (overlayManager.isConnected()) {
				m_FederationConnectMenuItem.setEnabled(false);
				m_FederationDisconnectMenuItem.setEnabled(true);
				m_RefreshFederationMenuItem.setEnabled(true);
				m_ApplyLayoutSubmenu.setEnabled(true);
				m_GlobalFDMenuItem.setEnabled(true);
				m_ShowAllEdgeMessagesMenuItem.setEnabled(true);
				m_HideAllEdgeMessagesMenuItem.setEnabled(true);
				m_EdgeThroughputSubmenu.setEnabled(true);
				m_showFullLabelsMenuItem.setEnabled(true);
			} else {
				m_FederationConnectMenuItem.setEnabled(true);
				m_FederationDisconnectMenuItem.setEnabled(false);
				m_RefreshFederationMenuItem.setEnabled(false);
				m_ApplyLayoutSubmenu.setEnabled(false);
				m_GlobalFDMenuItem.setEnabled(false);
				m_ShowAllEdgeMessagesMenuItem.setEnabled(false);
				m_HideAllEdgeMessagesMenuItem.setEnabled(false);
				m_EdgeThroughputSubmenu.setEnabled(false);
				m_showFullLabelsMenuItem.setEnabled(false);
			}
		} else if (source == m_BrokerMenu) {
			if (overlayManager.getSelectedBroker() != null) {
				m_StopBrokerMenuItem.setEnabled(true); // not working
				// TODO: make sure broker is not stopped
				m_ResumeBrokerMenuItem.setEnabled(true);
				// TODO: make sure broker is stopped // not working
				m_ShutdownBrokerMenuItem.setEnabled(true);
				m_InjectMessageMenuItem.setEnabled(true);
				m_UnInjectMessageMenuItem.setEnabled(true);
				m_TracePublicationMenuItem.setEnabled(true);
				m_TraceSubscriptionMenuItem.setEnabled(true);
				m_PropertiesMenuItem.setEnabled(true);
				m_SetSubMenuItem.setEnabled(true);
				m_SetAdvMenuItem.setEnabled(true);
				m_BrokerStatMenuItem.setEnabled(true);
				m_FailureDetectionMenuItem.setEnabled(true);
			} else { // no broker selected, disable the menu items
				m_StopBrokerMenuItem.setEnabled(false);
				m_ResumeBrokerMenuItem.setEnabled(false);
				m_ShutdownBrokerMenuItem.setEnabled(false);
				m_InjectMessageMenuItem.setEnabled(false);
				m_UnInjectMessageMenuItem.setEnabled(false);
				m_TracePublicationMenuItem.setEnabled(false);
				m_TraceSubscriptionMenuItem.setEnabled(false);
				m_PropertiesMenuItem.setEnabled(false);
				m_SetSubMenuItem.setEnabled(false);
				m_SetAdvMenuItem.setEnabled(false);
				m_BrokerStatMenuItem.setEnabled(false);
				m_FailureDetectionMenuItem.setEnabled(false);
			}
		} else if (source == m_FirstBrokerMenu) {
			if (overlayManager.isConnected()) {
				m_BatchMsgMenuItem.setEnabled(true);
				m_BatchMsgIncrMenuItem.setEnabled(true);
			} else {
				m_BatchMsgMenuItem.setEnabled(false);
				m_BatchMsgIncrMenuItem.setEnabled(false);
			}
		}

	}

	/**
	 * Invoked when an action occurs. Specified in java.awt.event.ActionListener interface.
	 * 
	 * @param e
	 *            ActionEvent for action that occurred.
	 */
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == m_ExitMenuItem) {
			// TODO: ask user if they really want to exit first?
			exitMonitor();
		} else if (source == m_FailureDetectionMenuItem) {
			m_CommandDialog = new FailureDetectionDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_FederationConnectMenuItem) {
			m_CommandDialog = new ConnectFederationDialog(this);
			m_CommandDialog.setVisible(true);
		} else if (source == m_FederationDisconnectMenuItem) {
			m_CommandDialog = new DisconnectFederationDialog(this);
			m_CommandDialog.setVisible(true);
		} else if (source == m_GlobalFDMenuItem) {
			m_CommandDialog = new GlobalFDDialog(this);
			m_CommandDialog.setVisible(true);
		} else if (source == m_StopBrokerMenuItem) {
			m_CommandDialog = new StopBrokerDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_ResumeBrokerMenuItem) {
			m_CommandDialog = new ResumeBrokerDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_ShutdownBrokerMenuItem) {
			m_CommandDialog = new ShutdownBrokerDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_InjectMessageMenuItem) {
			m_CommandDialog = new InjectMessageDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_UnInjectMessageMenuItem) {
			m_CommandDialog = new UnInjectMessageDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_TracePublicationMenuItem) {

			m_CommandDialog = new TracePubDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_TraceSubscriptionMenuItem) {
			m_CommandDialog = new TraceSubDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_UnTracePublicationMenuItem) {
			m_CommandDialog = new UnTracePubDialog(this, overlayManager.getSelectedBroker());
			m_CommandDialog.setVisible(true);
		} else if (source == m_PropertiesMenuItem) {
			m_CommandDialog = new BrokerProperties(this, overlayManager.getSelectedBrokerUI());
			m_CommandDialog.setVisible(true);
		} else if (source == m_BrokerStatMenuItem) {
			m_CommandDialog = new BrokerStatistic(this, overlayManager.getSelectedBrokerUI());
			m_CommandDialog.setVisible(true);
		} else if (source == m_BatchMsgMenuItem) {
			m_CommandDialog = new BatchMsgDialog(this, overlayManager.getMonitorClient());
			m_CommandDialog.setVisible(true);
		} else if (source == m_BatchMsgIncrMenuItem) {
			m_CommandDialog = new BatchMsgIncrDialog(this, overlayManager.getMonitorClient());
			m_CommandDialog.setVisible(true);
		} else if (source == m_TraceByTraceIDMenuItem) {
			m_CommandDialog = new TraceIDTraceDialog(this);
			m_CommandDialog.setVisible(true);
		} else if (source == m_RefreshFederationMenuItem) {
			try {
				overlayManager.publishNetDiscovery();
			} catch (ClientException exp) {
				setErrorString(exp.getMessage());
			}
		} else if (source == m_CircleLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_CIRCLE);
		} else if (source == m_FRLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_FRUCHTERMAN_REINGOLD);
		} else if (source == m_ISOMLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_ISOM);
		} else if (source == m_KKLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_KAMADA_KAWAI);
		} else if (source == m_KKLayoutIntSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_KAMADA_KAWAI_INTEGER);
		} else if (source == m_SpringLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_SPRING);
		} else if (source == m_StaticLayoutSubmenuItem) {
			overlayManager.applyLayout(MonitorResources.LAYOUT_STATIC);
		} else if (source == m_ShowAllEdgeMessagesMenuItem) {
			overlayManager.showAllEdgeMessages();
		} else if (source == m_HideAllEdgeMessagesMenuItem) {
			overlayManager.hideAllEdgeMessages();
		} else if (source == m_EdgeThroughputOnSubmenuItem) {
			overlayManager.setEdgeThroughputIndicator(MonitorResources.EDGE_THROUGHPUT_ON);
			m_EdgeThroughputOnSubmenuItem.setEnabled(false);
			m_EdgeThroughputOffSubmenuItem.setEnabled(true);
		} else if (source == m_EdgeThroughputOffSubmenuItem) {
			overlayManager.setEdgeThroughputIndicator(MonitorResources.EDGE_THROUGHPUT_OFF);
			m_EdgeThroughputOnSubmenuItem.setEnabled(true);
			m_EdgeThroughputOffSubmenuItem.setEnabled(false);
		} else if (source == m_EdgeThroughputResetSubmenuItem) {
			overlayManager.resetEdgeThroughputIndicator();
		} else if (source == m_showFullLabelsMenuItem) {
			if (m_showFullLabelsMenuItem.isSelected())
				overlayManager.useNodeLabelType(MonitorVertex.LabelType.LT_LONG);
			else
				overlayManager.useNodeLabelType(MonitorVertex.LabelType.LT_SHORT);
		}
		// extra condition added to account for right-click
		// really, an Action class should be created for each of these actions
		// so, for example, you could perform the same action through a
		// right-click,
		// through a toolbar button, and through the file menu without having to
		// repeat the same code or add a bunch of OR conditions... will require
		// major refactoring of MonitorFrame code
		else if (source == m_SetSubMenuItem) {
			m_CommandDialog = new SetOfSubDialog(this, overlayManager.getSelectedBroker(),
					cmdManager);
			// m_CommandDialog = new NewSetOfSubDialog(this,
			// m_OverlayManager.getSelectedBroker(), m_CommManager);
			m_CommandDialog.setVisible(true);
		} else if (source == m_SetAdvMenuItem) {
			m_CommandDialog = new SetOfAdvDialog(this, overlayManager.getSelectedBroker(),
					cmdManager);
			m_CommandDialog.setVisible(true);
		}
	}

	/**
	 * Called by the monitor's command dialog when a command is to be executed.
	 * 
	 * @param commandID
	 *            ID of the command to execute.
	 */
	public void executeCommand(int commandID) {
		boolean commandResult = false;

		// show a wait cursor while the command executes
		m_CommandDialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			switch (commandID) {
			case MonitorResources.CMD_FEDERATION_CONNECT:
				String brokerURL = ((ConnectFederationDialog) m_CommandDialog).getBrokerURL();
				try {
					overlayManager.connect(brokerURL);
					setStatusString(MonitorResources.L_CONNECTED_TO + brokerURL);
					commandResult = true;
				} catch (ClientException e) {
					setErrorString(e.getMessage());
					commandResult = false;
				}
				break;
			case MonitorResources.CMD_FEDERATION_DISCONNECT:
				commandResult = overlayManager.disconnect();
				if (commandResult) {
					setStatusString(MonitorResources.L_NOT_CONNECTED);
					overlayManager.clearUI();
				}
				break;
			case MonitorResources.CMD_BROKER_STOP:
				commandResult = overlayManager.stopBroker(((StopBrokerDialog) m_CommandDialog).getBrokerID());
				break;
			case MonitorResources.CMD_BROKER_RESUME:
				commandResult = overlayManager.resumeBroker(((ResumeBrokerDialog) m_CommandDialog).getBrokerID());
				break;
			case MonitorResources.CMD_BROKER_SHUTDOWN:
				commandResult = overlayManager.shutdownBroker(((ShutdownBrokerDialog) m_CommandDialog).getBrokerID());
				break;
			case MonitorResources.CMD_INJECT_MESSAGE:
				commandResult = overlayManager.injectMessage(
						((InjectMessageDialog) m_CommandDialog).getMsgType(),
						((InjectMessageDialog) m_CommandDialog).getMsgText(),
						((InjectMessageDialog) m_CommandDialog).getBrokerID());
				break;
			case MonitorResources.CMD_UNINJECT_MESSAGE:
				commandResult = overlayManager.uninjecMessage(
						((UnInjectMessageDialog) m_CommandDialog).getInjectMsgStore(),
						((UnInjectMessageDialog) m_CommandDialog).getBrokerID());
				break;
			case MonitorResources.CMD_TRACE_PUB_MESSAGE:
				commandResult = overlayManager.tracePublicationMessage(
						((TracePubDialog) m_CommandDialog).getBrokerID(),
						((TracePubDialog) m_CommandDialog).getMSg());
				break;
			case MonitorResources.CMD_TRACE_SUB_MESSAGE:
				commandResult = overlayManager.traceSubscriptionMessage(
						((TraceSubDialog) m_CommandDialog).getBrokerID(),
						((TraceSubDialog) m_CommandDialog).getMSg());
				break;
			case MonitorResources.CMD_UNTRACE_MESSAGE:
				boolean commandResult1 = overlayManager.untraceMessage();
				boolean commandResult2 = overlayManager.untraceByTraceID();
				commandResult = (commandResult1 || commandResult2);
				break;
			case MonitorResources.CMD_PROPERTIES:
				commandResult = true;
				break;
			case MonitorResources.CMD_BROKER_STAT:
				commandResult = true;
				break;
			case MonitorResources.CMD_SET_ADV:
				commandResult = true;
				break;
			case MonitorResources.CMD_SET_SUB:
				commandResult = true;
				break;
			case MonitorResources.CMD_BATCH_MSG:
				commandResult = true;
				break;
			case MonitorResources.CMD_BATCH_MSG_INCRE:
				commandResult = true;
				break;
			case MonitorResources.CMD_FAILURE_DETECTION:
				if (((FailureDetectionDialog) m_CommandDialog).validateFields()) {
					commandResult = overlayManager.setHeartbeatParameters(
							((FailureDetectionDialog) m_CommandDialog).getBrokerID(),
							((FailureDetectionDialog) m_CommandDialog).getEnabled(),
							((FailureDetectionDialog) m_CommandDialog).getInterval(),
							((FailureDetectionDialog) m_CommandDialog).getTimeout(),
							((FailureDetectionDialog) m_CommandDialog).getThreshold());
				}
				break;
			case MonitorResources.CMD_TRACE_BY_TRACEID:
				commandResult = overlayManager.traceByTraceID(((TraceIDTraceDialog) m_CommandDialog).getTraceID());
				break;
			case MonitorResources.CMD_GLOBAL_FAILURE_DETECTION:
				// TODO:
				boolean flag = ((GlobalFDDialog) m_CommandDialog).enableFD();
				try {
					overlayManager.publishGlobalFD(flag);
					commandResult = true;
				} catch (ClientException e) {
					setErrorString(e.getMessage());
					commandResult = false;
				}
				break;
			default:
				// should never get here!
				// TODO: what should I do now?
			}
		} catch (ParseException e) {
			exceptionLogger.error(e.getMessage());
			commandResult = false;
		}
		
		// Check the result of the command and take appropriate action.
		if (!commandResult) {
			// Command unsuccessful. Display the error message. Show the error message. Note that
			// this call blocks the monitor interface.
			JOptionPane.showMessageDialog(this, m_ErrorString, MonitorResources.T_ERROR,
					JOptionPane.ERROR_MESSAGE);
			m_CommandDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		} else {
			// Command successful. Get rid of the command dialog.
			m_CommandDialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			m_CommandDialog.dispose();
			m_CommandDialog = null;
		}
	}

	public OverlayManager getOverlayManager() {
		return overlayManager;
	}

	/**
	 * Called when the user chooses to cancel a command.
	 */
	public void cancelCommand() {
		m_ErrorString = null;
		m_CommandDialog.dispose();
		m_CommandDialog = null;
	}

	/**
	 * Set the text description of the last command error encountered. The class responsible for
	 * executing the command is required to call this method if an error occurred.
	 * 
	 * @param error
	 *            Text description of the error.
	 */
	public void setErrorString(String error) {
		m_ErrorString = error;
	}

	/**
	 * Set the text in this window's status bar.
	 * 
	 * @param status
	 *            Status text to display.
	 */
	public void setStatusString(String status) {
		m_StatusLabel.setText(status);
	}
	
	public void shutdown() throws ClientException {
	    overlayManager.disconnect();
	    overlayManager.shutdown();
	    monitorClient.shutdown();
	    this.dispose();
	}

	/**
	 * Close the monitor window and exit the application.
	 */
	// change private to public for testing, because not visible to other class
	public void exitMonitor() {
		try {
			shutdown();
		} catch (ClientException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Main entry point for PADRES monitor application.
	 * 
	 * @param args
	 *            Ignored.
	 */
	public static void main(String[] args) {
		try {
			CommandLine cmdLine = new CommandLine(ClientConfig.getCommandLineKeys());
			cmdLine.processCommandLine(args);
			new LogSetup(cmdLine.getOptionValue(ClientConfig.CLI_OPTION_LOG_LOCATION));

			final MonitorFrame monitorFrame = new MonitorFrame();

			monitorFrame.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent e) {
					monitorFrame.exitMonitor();
				}
			});

			monitorFrame.setSize(640, 480);
			monitorFrame.setVisible(true);
			
			// Automatically connect to broker if specified on command line.
			String brokerUri = cmdLine.getOptionValue(ClientConfig.CLI_OPTION_BROKER_LIST);
			if (brokerUri != null) {
				monitorFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				monitorFrame.enableMenuBar(false);
				monitorFrame.setStatusString("Attempting to connect to: " + brokerUri);
				try {
					monitorFrame.overlayManager.connect(brokerUri);
					monitorFrame.setStatusString(MonitorResources.L_CONNECTED_TO + brokerUri);
				} catch (ClientException e) {
					JOptionPane.showMessageDialog(monitorFrame, e.getMessage(), MonitorResources.T_ERROR, JOptionPane.ERROR_MESSAGE);
					monitorFrame.setStatusString(MonitorResources.L_NOT_CONNECTED);
				}
				monitorFrame.enableMenuBar(true);
				monitorFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
}

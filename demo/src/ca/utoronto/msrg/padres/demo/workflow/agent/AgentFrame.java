//=============================================================================
//This file is part of The PADRES Project.
//
//For more information, see http://www.msrg.utoronto.ca
//
//Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
//=============================================================================
//$Id$
//=============================================================================
package ca.utoronto.msrg.padres.demo.workflow.agent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Pengcheng Wan
 * 
 *         This frame needs two important class instance variables as passed in which are
 *         JobInstance and JobInfo. We need set the status of running jobinstance through press
 *         "Success" or "Failure" button, and show information about job in a JTable.
 */
public class AgentFrame extends JFrame implements ActionListener {

	public static final String PADRES = "PADRES";

	public static final String SUCCESS = "Success";

	public static final String FAILURE = "Failure";

	public static final String[] names = { "Agent Name", "Application", "Job Name",
			"Schedule Time", "Submission Time", "Dependency", "Detail" };

	private JPanel bottomPanel = null;

	private JSplitPane splitPane = null;

	private JTextArea statusField = null;

	private JPanel tablePanel = null;

	private JScrollPane scrollPane = null;

	private JPanel buttonPanel = null;

	private JTable jobTable = null;

	private TableModel dataModel = null;

	private JButton successButton = null;

	private JButton failureButton = null;

	private String agentName = null;

	private JobInstance job = null;

	private RMIServerInterface rmiConnection;

	private MessageDestination clientDest;

	// Create the dummy data (a few rows of job information)
	final Object[][] dummydata = { { "Agent-A", "Pay_Roll", "JobA", "Daily", "9:00AM", "NON",
			"Blabal" }, };

	/**
	 * Constructor
	 * 
	 * @param agentName
	 *            name of agent
	 */
	public AgentFrame(String agentName, JobInfo jobInfo, JobInstance job,
			RMIServerInterface rmiConnection, MessageDestination clientDest) {
		this.agentName = agentName;
		this.job = job;
		this.rmiConnection = rmiConnection;
		this.clientDest = clientDest;
		init(jobInfo, job.getDependency());

		WindowListener l = new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// this.setVisible(false);
				System.exit(0);
			}
			// public void windowDeiconified(WindowEvent e) { agent.start(); }
			// public void windowIconified(WindowEvent e) { agent.stop(); }
		};
		this.setTitle(PADRES + " " + agentName);
		this.addWindowListener(l);
		this.getContentPane().add(bottomPanel, BorderLayout.CENTER);
		this.pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int w = 535;
		int h = 190;
		this.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h / 2);
		this.setSize(w, h);
		this.setVisible(true);

	}

	/**
	 * Create all the components which will be put on this Frame
	 */
	private void init(JobInfo jobInfo, String dependency) {
		// build a panel to hold JSplitPane
		bottomPanel = new JPanel();

		// build a panel to hold a JTable and JButtons
		createTable(jobInfo, dependency);
		createTablePanel();

		// build a status JTextArea
		statusField = new JTextArea();
		statusField.setLineWrap(true);
		statusField.setMinimumSize(new Dimension(20, 20));
		statusField.setEditable(false);
		statusField.setBackground(new Color(0, 155, 100));
		statusField.setText(JobFields.APPLICATION_NAME + ":" + job.getApplName() + ", "
				+ JobFields.GENERATION_ID + ":" + job.getGenerationID());

		// build a JSplitPane to hold JTable and JTextField about status
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, statusField);
		splitPane.setDividerLocation(100);
		splitPane.setDividerSize(5);

		// set the bottomPanel properties
		BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
		bottomPanel.setBorder(bb);
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.setBackground(Color.gray);

		bottomPanel.add(splitPane);
	}

	private void createTable(JobInfo jobInfo, String dependency) {
		Vector vdata = this.createDataVector(jobInfo, dependency);
		final Object[] data = vdata.toArray();
		dataModel = new AbstractTableModel() {
			public int getColumnCount() {
				return names.length;
			}

			public int getRowCount() {
				return 1;
			}

			// public int getRowCount() { return dummydata.length;}
			// public Object getValueAt(int row, int col) {
			// return dummydata[row][col]; }
			public Object getValueAt(int row, int col) {
				return data[col];
			}

			public String getColumnName(int col) {
				return names[col];
			}

			public Class getColumnClass(int c) {
				return getValueAt(0, c).getClass();
			}

			// =================================================================
			/**
			 * Don't need to implement the following two methods unless we want set the table
			 * editable
			 */
			public boolean isCellEditable(int row, int col) {
				return false;
			}

			public void setValueAt(Object aValue, int row, int col) {
			}
			// =================================================================
		};

		jobTable = new JTable(dataModel);
		jobTable.setMinimumSize(new Dimension(20, 20));
		jobTable.doLayout();
	}

	private Vector createDataVector(JobInfo jobInfo, String dependency) {
		Vector v = new Vector();
		v.addElement(agentName);
		v.addElement(jobInfo.getApplName());
		v.addElement(jobInfo.getJobName());
		v.addElement(jobInfo.getSchedule());
		v.addElement(jobInfo.getSubmission());
		v.addElement(dependency);
		v.addElement(jobInfo.getUserID() + "-" + jobInfo.getCommand() + "-" + jobInfo.getArgs());
		return v;
	}

	private void createTablePanel() {
		scrollPane = new JScrollPane(jobTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		successButton = new JButton(SUCCESS);
		successButton.addActionListener(this);
		failureButton = new JButton(FAILURE);
		failureButton.addActionListener(this);

		buttonPanel.add(Box.createRigidArea(new Dimension(180, 1)));
		buttonPanel.add(successButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(40, 1)));
		buttonPanel.add(failureButton);

		tablePanel = new JPanel(new BorderLayout());
		tablePanel.add(scrollPane);
		tablePanel.add(buttonPanel, BorderLayout.SOUTH);
	}

	public void reset(String applname, String generationID) {
		statusField.setText(JobFields.APPLICATION_NAME + ":" + applname + ", "
				+ JobFields.GENERATION_ID + ":" + generationID);
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {
		try {
			if (event.getActionCommand().equalsIgnoreCase(SUCCESS)) {
				String status = JobFields.STATUS_SUCCESS;
				job.setStatus(status);

				statusField.setText(JobFields.STATUS_SUCCESS + ":\n" + job.toString());
				// successButton.setEnabled(false);
				// failureButton.setEnabled(false);
				publishStatus(job);

				System.out.println(JobFields.STATUS_SUCCESS + ":  " + job);
				// this.setVisible(false);
				this.dispose();
			} else if (event.getActionCommand().equalsIgnoreCase(FAILURE)) {
				String status = JobFields.STATUS_FAILURE;
				job.setStatus(status);
				statusField.setText(JobFields.STATUS_FAILURE + ":\n" + job.toString());
				// successButton.setEnabled(false);
				// failureButton.setEnabled(false);
				publishStatus(job);
				System.out.println(JobFields.STATUS_FAILURE + ":  " + job);
				// this.setVisible(false);
				this.dispose();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void publishStatus(JobInstance job) {
		try {
			String pubContent = "[class," + JobFields.JOB_STATUS + "]," + "["
					+ JobFields.APPLICATION_NAME + "," + job.getApplName() + "]," + "["
					+ JobFields.GENERATION_ID + "," + job.getGenerationID() + "]," + "["
					+ JobFields.STATUS + "," + job.getStatus() + "]," + "[" + JobFields.DETAIL
					+ "," + job.getDetail() + "]," + "[" + JobFields.JOB_NAME + ","
					+ job.getJobName() + "]";
			// System.out.println(pubContent);

			Publication pu = MessageFactory.createPublicationFromString(pubContent);
			PublicationMessage pubMsg = new PublicationMessage(pu, "-1", clientDest);
			rmiConnection.receiveMessage(pubMsg, HostType.CLIENT);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// =========================================================================
	// =================For Testing=============================================
	public static void main(String[] args) throws ParseException {
		JobInfo jobInfo = new JobInfo(
				"appl:PAYROLL;jobname:JobD;schedule:Daily;submission:9:00AM;userID:gli;command:ls;args:-l;isScript:N");
		Publication pub = MessageFactory.createPublicationFromString(
				"[class,JOBSTATUS],[applname,PAYROLL],[jobname,APPL_START],[GID,g001],[status,SUCCESS],[detail,'AAAA']");
		JobInstance job = new JobInstance(pub);
		job.setTriggered(true);
		// AgentFrame af = new AgentFrame("agentA", jobInfo, job);
	}
	// =========================================================================
}

/*
 * Created on 22-Sep-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.io.StringReader;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorClient;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * @author gerald
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BatchMsgIncrDialog extends MonitorDialog {

	protected static final long serialVersionUID = 1L;

	protected JFileChooser m_chooser = new JFileChooser();

	protected String m_fileName;

	protected MonitorFrame monitor;

	protected MonitorClient monitorClient;

	protected JButton m_ProcessNextButton;

	protected JButton m_SkipNextButton;

	protected JButton m_ProcessRemainingButton;

	protected JTextArea m_RemainingTextArea;

	protected JTextField m_NextField;

	protected JTextArea m_CompletedTextArea;

	/**
	 * Construct Batch Msg Inectmental Dialog
	 * 
	 * @param owner
	 *            the hook to monitor frame
	 * @param client
	 *            the connection to the federation
	 */
	public BatchMsgIncrDialog(MonitorFrame owner, MonitorClient client) {
		super(owner, MonitorResources.T_BATCH_MSG);
		monitor = owner;
		monitorClient = client;
		m_chooser.setCurrentDirectory(new File("."));

		m_chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {

			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return name.endsWith(".txt") || name.endsWith(".app") || f.isDirectory();
			}

			public String getDescription() {
				return null;
			}
		});

		int r = m_chooser.showOpenDialog(null);

		if (r == JFileChooser.APPROVE_OPTION) {
			// loadfile here now

			m_fileName = m_chooser.getSelectedFile().getAbsolutePath();

			processfile(m_fileName);

		} else {
			// user cancel the operation

		}
	}

	public void buildContentPanel() {

		m_ProcessNextButton = new JButton(MonitorResources.B_PROCESS_NEXT);
		m_ProcessNextButton.addActionListener(this);
		add(m_ProcessNextButton, 0, 0, 1.0, 0.0);

		m_SkipNextButton = new JButton(MonitorResources.B_SKIP_NEXT);
		m_SkipNextButton.addActionListener(this);
		add(m_SkipNextButton, 1, 0, 1.0, 0.0);

		m_ProcessRemainingButton = new JButton(MonitorResources.B_PROCESS_REMAINING);
		m_ProcessRemainingButton.addActionListener(this);
		add(m_ProcessRemainingButton, 2, 0, 1.0, 0.0);

		JLabel remainingLabel = new JLabel(MonitorResources.L_REMAINING);
		add(remainingLabel, 0, 1, 0.0, 0.0);

		m_RemainingTextArea = new JTextArea();
		m_RemainingTextArea.setLineWrap(false);
		m_RemainingTextArea.setEditable(false);
		JScrollPane remainingScrollPane = new JScrollPane(m_RemainingTextArea);
		remainingScrollPane.setPreferredSize(new java.awt.Dimension(100, 125));
		add(remainingScrollPane, 0, 2, 3, 1, 1.0, 1.0);

		JLabel nextLabel = new JLabel(MonitorResources.L_NEXT);
		add(nextLabel, 0, 3, 0.0, 0.0);

		m_NextField = new JTextField();
		m_NextField.setEditable(false);
		m_NextField.setOpaque(false);
		add(m_NextField, 0, 4, 3, 1, 0.0, 0.0);

		JLabel completedLabel = new JLabel(MonitorResources.L_COMPLETED);
		add(completedLabel, 0, 5, 0.0, 0.0);

		m_CompletedTextArea = new JTextArea();
		m_CompletedTextArea.setLineWrap(false);
		m_CompletedTextArea.setEditable(false);
		JScrollPane completedScrollPane = new JScrollPane(m_CompletedTextArea);
		completedScrollPane.setPreferredSize(new java.awt.Dimension(100, 125));
		add(completedScrollPane, 0, 6, 3, 1, 1.0, 1.0);

		pack();
	}

	public int getCommandID() {
		return MonitorResources.CMD_BATCH_MSG;
	}

	public void notify(Object o) {

	}

	private void handlecommand(String userCommand) {

		StreamTokenizer st = new StreamTokenizer(new StringReader(userCommand));
		BatchMsgDialog.handleCommandStream(st, monitorClient);
	}

	private void processfile(String fileName) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String line;
			boolean firstLine = true;
			while ((line = in.readLine()) != null) {
				if (firstLine) {
					m_NextField.setText(line);
					m_NextField.setCaretPosition(0);
					firstLine = false;
				} else {
					m_RemainingTextArea.append(line + "\n");
				}
				m_RemainingTextArea.setCaretPosition(0);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

	/* Add newly inputed value to the text area */
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		Object source = e.getSource();
		if (source == m_ProcessRemainingButton) {
			String remainingContent;
			remainingContent = m_NextField.getText() + m_RemainingTextArea.getText();
			handlecommand(remainingContent);
			m_CompletedTextArea.append(remainingContent);
			m_NextField.setText("");
			m_RemainingTextArea.setText("");

		} else if (source == m_SkipNextButton) {
			promoteString();
		} else if (source == m_ProcessNextButton) {
			String activeCommand = m_NextField.getText();
			handlecommand(activeCommand);
			promoteString();
			m_CompletedTextArea.append(activeCommand + "\n");
		}

		// TODO: HACK - reset the caret position so the user will see the start of the next command
		// and the head of the list of commands to execute
		m_NextField.setCaretPosition(0);
		m_RemainingTextArea.setCaretPosition(0);
		if (m_NextField.getText().equals("")) {
			// We are all done now
			m_ProcessRemainingButton.setEnabled(false);
			m_SkipNextButton.setEnabled(false);
			m_ProcessNextButton.setEnabled(false);
		}
	}

	/* Return the string of the pending area */
	/* Move 1 line of not done into pending area */
	private void promoteString() {

		try {

			int endofLine;
			endofLine = m_RemainingTextArea.getLineEndOffset(0);
			String notDoneStr = m_RemainingTextArea.getText();

			/* Extract first line */
			String firstLine = notDoneStr.substring(0, endofLine);

			/* Replace peding text */
			m_NextField.setText(firstLine);

			/* Remove first line from not done */
			m_RemainingTextArea.setText(notDoneStr.substring(endofLine));

		} catch (Exception e) {
		}

	}

}

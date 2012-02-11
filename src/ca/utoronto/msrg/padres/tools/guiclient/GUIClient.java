package ca.utoronto.msrg.padres.tools.guiclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import ca.utoronto.msrg.padres.client.BrokerState;
import ca.utoronto.msrg.padres.client.Client;
import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.client.CommandResult;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;

public class GUIClient extends Client implements ActionListener {

	protected static final String CONFIG_FILE_PATH = String.format(
			"%s/etc/guiclient/client.properties", ClientConfig.PADRES_HOME);

	protected static final String CONNECT_STRING = "Connect";

	protected static final String DISCONNECT_STRING = "Disconnect";

	protected static final String BATCH_STRING = "Batch";

	protected static final String ENTER_STRING = "Enter";

	protected static final String CLEAR_STRING = "Clear";

	protected JFrame mainWindow;

	protected JTextPane notifications;

	protected JTextPane history;

	protected JTextField input;

	protected int inputAreaStartPosition = 0;

	public GUIClient(String id) throws ClientException {
		super(id);
	}

	public GUIClient(ClientConfig newConfig) throws ClientException {
		super(newConfig);
	}

	public JFrame createJFrame() {
		mainWindow = new JFrame("PADRES GUI Client :: " + clientID);

		Container contents = mainWindow.getContentPane();
		contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS));

		JMenuBar buttonBox = new JMenuBar();
		buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.LINE_AXIS));
		buttonBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JButton connectButton = new JButton(CONNECT_STRING);
		connectButton.setMnemonic(KeyEvent.VK_C);
		connectButton.addActionListener(this);
		buttonBox.add(connectButton);
		buttonBox.add(Box.createRigidArea(new Dimension(5, 0)));
		JButton disconnectButton = new JButton(DISCONNECT_STRING);
		disconnectButton.setMnemonic(KeyEvent.VK_D);
		disconnectButton.addActionListener(this);
		buttonBox.add(disconnectButton);
		buttonBox.add(Box.createRigidArea(new Dimension(5, 0)));
		JButton batchButton = new JButton(BATCH_STRING);
		batchButton.setMnemonic(KeyEvent.VK_B);
		batchButton.addActionListener(this);
		buttonBox.add(batchButton);
		mainWindow.setJMenuBar(buttonBox);

		JPanel promptBox = new JPanel(new BorderLayout());
		promptBox.setBorder(BorderFactory.createTitledBorder("User Input"));
		input = new JTextField(55);
		input.addActionListener(this);
		promptBox.add(new JLabel("Command:"), BorderLayout.WEST);
		promptBox.add(input, BorderLayout.CENTER);
		JButton inputButton = new JButton(ENTER_STRING);
		inputButton.addActionListener(this);
		promptBox.add(inputButton, BorderLayout.EAST);
		contents.add(promptBox);

		JPanel historyBox = new JPanel(new BorderLayout());
		historyBox.setBorder(BorderFactory.createTitledBorder("Client Activity"));
		history = new JTextPane();
		history.setPreferredSize(new Dimension(200, 200));
		history.setEditable(false);
		JScrollPane historyPane = new JScrollPane(history);
		historyBox.add(historyPane);
		((DefaultCaret)history.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // auto-scroll
		contents.add(historyBox);

		JPanel notificationBox = new JPanel(new BorderLayout());
		notificationBox.setBorder(BorderFactory.createTitledBorder("Broker Notifications"));
		notifications = new JTextPane();
		notifications.setPreferredSize(new Dimension(200, 200));
		notifications.setEditable(false);
		JScrollPane notificationPane = new JScrollPane(notifications);
		notificationBox.add(notificationPane);
		JButton clearButton = new JButton(CLEAR_STRING);
		clearButton.addActionListener(this);
		notificationBox.add(clearButton, BorderLayout.PAGE_END);
		((DefaultCaret)notifications.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE); // auto-scroll
		contents.add(notificationBox);

		return mainWindow;
	}

	public void printActiveConnections() {
		Map<String, String> brokers = getBrokerConnections();
		if (brokers.size() != 0) {
			StringBuilder msg = new StringBuilder();
			msg.append("Connetcted to: ");
			for (String brokerID : brokers.keySet()) {
				msg.append(String.format("%s [%s] ", brokerID, brokers.get(brokerID)));
			}
			msg.append("\n");
			printClientAction(msg.toString(), Color.BLUE);
		}
	}

	public void processMessage(Message msg) {
		super.processMessage(msg);
		printNotification("Received: " + msg, Color.BLACK);
		
		// Print publications prettier.
		if (msg instanceof PublicationMessage) {
			PublicationMessage pubMsg = (PublicationMessage)msg;
			printNotification("  Pub: " + pubMsg.getPublication(), Color.BLUE);
		}
	}

	public void exitClient() {
		try {
			shutdown();
		} catch (ClientException e) {
			printClientAction("Error in shutting down client: " + e.getMessage(), Color.RED);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			if (e.getActionCommand().equals(CONNECT_STRING)) {
				connectToBroker();
			} else if (e.getActionCommand().equals(DISCONNECT_STRING)) {
				disconnectToBroker();
			} else if (e.getActionCommand().equals(BATCH_STRING)) {
				processBatch();
			} else if (e.getActionCommand().equals(ENTER_STRING)) {
				processUserInput();
			} else if (e.getActionCommand().equals(CLEAR_STRING)) {
				notifications.setText("");
			}
		} else if (e.getSource() instanceof JTextField) {
			processUserInput();
		}
		input.setText("");
	}

	protected void connectToBroker() {
		String brokerURI = JOptionPane.showInputDialog(mainWindow, "Connect to Broker",
				"rmi://localhost:1099/Broker1");
		if (brokerURI != null) {
			try {
				printClientAction("Connecting to broker: " + brokerURI + " ...", Color.BLACK);
				BrokerState brokerState = connect(brokerURI);
				String result = String.format("Connected to %s",
						brokerState.getBrokerAddress().getNodeURI());
				printClientAction(result, Color.GREEN);
			} catch (ClientException e) {
				printClientAction(e.getMessage(), Color.RED);
			}
		}
	}

	protected void disconnectToBroker() {
		try {
			String[] brokerList = getBrokerURIList();
			String brokerURI = (String) JOptionPane.showInputDialog(mainWindow,
					"Disconnect from: ", "Broker Disconnect", JOptionPane.PLAIN_MESSAGE, null,
					brokerList, brokerList[0]);
			if (brokerURI != null) {
				printClientAction("Disconnecting from broker: " + brokerURI + " ...", Color.BLACK);
				BrokerState brokerState = disconnect(brokerURI);
				String result = String.format("Connected to %s (%s)",
						brokerState.getBrokerAddress(), brokerState.getBrokerAddress().getNodeURI());
				printClientAction(result, Color.GREEN);
			}
		} catch (ClientException e) {
			printClientAction(e.getMessage(), Color.RED);
		}
	}

	protected void processUserInput() {
		CommandResult results;
		try {
			results = handleCommand(input.getText());
		} catch (ParseException e) {
			e.printStackTrace();
			exceptionLogger.error(e);
			return;
		}
		printClientAction(results.cmdString, Color.BLACK);
		if (results.isError()) {
			printClientAction(results.errMsg, Color.RED);
		} else {
			printClientAction(results.resString, Color.BLUE);
		}
	}

	protected void processBatch() {
		String fileName = chooseFile();
		if (fileName != null) {
			try {
				CommandResult result = handleCommand("batch " + fileName);
				printResults(result);
			} catch (ParseException e) {
				printClientAction("Error processing batch command.", Color.RED);
			}
		}
	}

	/**
	 * Use a dialog to choose a file in the file system.
	 */
	protected String chooseFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));

		chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return name.endsWith(".txt") || name.endsWith(".app") || f.isDirectory();
			}
			public String getDescription() {
				return null;
			}
		});

		int r = chooser.showOpenDialog(null);

		String fileName = null;
		if (r == JFileChooser.APPROVE_OPTION) {
			fileName = chooser.getSelectedFile().getAbsolutePath();
		} else {
			// User cancelled the operation.  Return null.
		}
		return fileName;
	}
	
	public void printResults(CommandResult results) {
		if (results.isError()) {
			printClientAction(results.errMsg, Color.RED);
		} else if (results.resString != null && !results.resString.equals("")) {
			printClientAction("Command: " + results.cmdString, Color.BLUE);
			printClientAction(results.resString, Color.BLACK);
		}
	}

	protected void printClientAction(String outString, Color txtColor) {
		try {
			Document notificationText = history.getDocument();
			SimpleAttributeSet attrSet = new SimpleAttributeSet();
			StyleConstants.setForeground(attrSet, txtColor);
			notificationText.insertString(notificationText.getLength(), outString + "\n", attrSet);
			mainWindow.repaint();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	protected void printNotification(String outString, Color txtColor) {
		try {
			Document notificationText = notifications.getDocument();
			SimpleAttributeSet attrSet = new SimpleAttributeSet();
			StyleConstants.setForeground(attrSet, txtColor);
			notificationText.insertString(notificationText.getLength(), outString + "\n", attrSet);
		} catch (BadLocationException e) {
			printClientAction(e.getMessage(), Color.RED);
		}
	}

	public static void main(String[] args) {
		try {
			// process the command line arguments
			CommandLine cmdLine = new CommandLine(ClientConfig.getCommandLineKeys());
			cmdLine.processCommandLine(args);
			String configFile = cmdLine.getOptionValue(ClientConfig.CLI_OPTION_CONFIG_FILE,
					CONFIG_FILE_PATH);
			// load the client configuration
			ClientConfig userConfig = new ClientConfig(configFile);
			// overwrite the client configurations from the config file with configuration
			// parameters from the command line
			userConfig.overwriteWithCmdLineArgs(cmdLine);

			// create the client
			final GUIClient guiClient = new GUIClient(userConfig);
			// create the GUI for the client
			JFrame clientWindow = guiClient.createJFrame();
			// add a WindowLister to the frame to gracefully shutdown the client
			clientWindow.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent e) {
					guiClient.exitClient();
					System.exit(0);
				}
			});
			// display the GUI
			clientWindow.pack();
			clientWindow.setVisible(true);
			guiClient.printActiveConnections();
		} catch (ClientException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

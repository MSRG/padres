package ca.utoronto.msrg.padres.tools.guiclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import ca.utoronto.msrg.padres.client.ClientConfig;
import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.common.util.CommandLine;
import ca.utoronto.msrg.padres.demo.workflow.resources.JobSchedulerResources;
import ca.utoronto.msrg.padres.tools.guiclient.dialogs.ConstraintsPanel;
import ca.utoronto.msrg.padres.tools.guiclient.dialogs.IssueTrigger;
import ca.utoronto.msrg.padres.tools.guiclient.dialogs.JobScheduleLoader;

public class SwingDeployer extends GUIClient{

	private String fileName = "";
	private ConstraintsPanel cp = new ConstraintsPanel();
	private HashMap<String, String> triggerMap = new HashMap<String, String>();
	
	public void jobScheduleLoad() {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));

			chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
				public boolean accept(File f) {
					String name = f.getName().toLowerCase();
					return name.endsWith(".txt") || name.endsWith(".app") || f.isDirectory();
				}

				public String getDescription() {
					// TODO Auto-generated method stub
					return null;
				}
			});

			int r = chooser.showOpenDialog(null);
			if (r == JFileChooser.APPROVE_OPTION) {
				fileName = chooser.getSelectedFile().getAbsolutePath();
			} else {
				fileName = "";
				printClientAction("Loading canceled!" + "\n", Color.RED);
				//SwingDeployer.output.append("Loading canceled!" + "\n");
				return;
			}
			try {
				BufferedReader in = new BufferedReader(new FileReader(fileName));
				printClientAction("file " + fileName + " is opened." + "\n", Color.BLACK);
				String line;
				try {
					while ((line = in.readLine()) != null) {
						if ((line.indexOf("p") != -1) || (line.indexOf("a") != -1)
								|| (line.indexOf("publication") != -1)) {
							try {
								handleCommand(line);
								printClientAction(line + "\n", Color.BLACK);
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}

						try {
							//SwingDeployer.output.paintImmediately(0, 0, 800, 800);
							Thread.sleep(500);
						} catch (InterruptedException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}

					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			} catch (FileNotFoundException ef) {
				printClientAction("file " + fileName + " is not found!" + "\n", Color.RED);
			}

	}
	
	public void issueTrigger() {
		JDialog dialog = new JDialog();
		Container dialogContentPane = dialog.getContentPane();
		JButton ok_button = new JButton("Issue");
		JButton cancel_button = new JButton("Cancel");
		dialogContentPane.add(cp, BorderLayout.CENTER);
		
		dialogContentPane.add(ok_button, BorderLayout.SOUTH);
		ok_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String trigger = "[class,Trigger],[applname,'" + cp.getApplName() + "'],[GID,'"
						+ cp.getGID() + "'],[schedule,'" + cp.getSchedule() + "']";
				if (cp.getApplName().equals("") || cp.getGID().equals("")) {
					printClientAction("Please specify application name and generation ID.\n", Color.RED);
					// dispose();
				} else {
					// Check the trigger generation id
					String key = cp.getApplName() + cp.getGID();
					if (triggerMap.containsKey(key)) {
						JOptionPane.showMessageDialog(mainWindow, "This generation ID has been used!",
								"Alert Information", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					triggerMap.put(key, cp.getGID());
					try {
						Publication newPub = MessageFactory.createPublicationFromString(trigger);
						if (newPub.getClassVal() == null) {
							throw new ClientException("Publication syntax error");
						}
						publish(newPub);
					} catch (ParseException e1) {
						try {
							throw new ClientException(e1);
						} catch (ClientException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					} catch (ClientException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					//publish(trigger);
					System.out.println("trigger is : " + trigger);
					printClientAction("p " + trigger + "\n", Color.BLACK);
					// frame.dispose();
				}


			}
		});
		cancel_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("issue trigger...by button");
				mainWindow.dispose();
				// exit(0);
				// this.

			}
		});		
		dialog.pack();
		dialog.show();
	}

		
	
	public SwingDeployer(ClientConfig newConfig) throws ClientException {
		super(newConfig);
	}
	
	public JFrame createJFrame() {
		mainWindow = new JFrame("PADRES Swing Deployer :: " + clientID);

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
		/*
		buttonBox.add(Box.createRigidArea(new Dimension(5, 0)));
		JButton appButton = new JButton(APP_STRING);
		appButton.setMnemonic(KeyEvent.VK_P);
		appButton.addActionListener(this);
		JPopupMenu deployAppl = new JPopupMenu("Deployment");
		// JMenuItem applControl = new JMenuItem("Control");
		appButton.add(deployAppl);
		//JMenuItem issueTriggerItem = new JMenuItem(JobSchedulerResources.M_TRIGGER_ISSUE);
		// applMenu.add(defTriggerItem);
		//appButton.addSeparator();
		//appButton.add(issueTriggerItem);
		buttonBox.add(appButton);
		*/
		//Create the popup menu.
        final JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(new AbstractAction("Deployment") {
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog(mainWindow, "Option 1 selected");
            	jobScheduleLoad();
            }
        }));
        popup.add(new JMenuItem(new AbstractAction("Issue Trigger") {
            public void actionPerformed(ActionEvent e) {
                //JOptionPane.showMessageDialog(mainWindow, "Option 2 selected");
            	System.out.println("issue trigger...");
				// IssueTrigger issueTriggerDialog = new
				// IssueTrigger("IssueTrigger");

				// issueTriggerDialog.show();
				// JFrame frame =
				//IssueTrigger dialog = new IssueTrigger(mainWindow, "Issue Trigger", true);

				//dialog.show();
            	issueTrigger();
            }
        }));
        final JButton appButton = new JButton("Application");
        appButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        appButton.setMnemonic(KeyEvent.VK_A);
        buttonBox.add(appButton);
        
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
		
		
		//final JMenu mainMenu = new JMenu(JobSchedulerResources.M_MAIN);
		
		final JMenu applMenu = new JMenu(JobSchedulerResources.M_APPLICATION);
		
/*
		//JMenuItem exitItem = new JMenuItem(JobSchedulerResources.M_EXIT);
		//mainMenu.add(exitItem);
		//buttonBox.add(mainMenu);
		//buttonBox.add(applMenu);
		JMenuItem deployAppl = new JMenuItem("Deployment");
		// JMenuItem applControl = new JMenuItem("Control");
		applMenu.add(deployAppl);
		JMenuItem issueTriggerItem = new JMenuItem(JobSchedulerResources.M_TRIGGER_ISSUE);
		// applMenu.add(defTriggerItem);
		applMenu.addSeparator();
		applMenu.add(issueTriggerItem);
		buttonBox.add(applMenu);
		// applMenu.add(applControl);
		// mb.add(applMenu);
		
		issueTriggerItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				System.out.println("issue trigger...");
				// IssueTrigger issueTriggerDialog = new
				// IssueTrigger("IssueTrigger");

				// issueTriggerDialog.show();
				// JFrame frame =
				IssueTrigger dialog = new IssueTrigger(mainWindow, "Issue Trigger", true);

				dialog.show();
			}
		});

		deployAppl.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// Merger merging = new Merger();
				// Merger.mergerFlag = true;
				JobScheduleLoader jobLoader = new JobScheduleLoader();
				jobLoader.start();
			}
			// }
		});
	*/	
		return mainWindow;
	}
	
	public void printClientAction(String outString, Color txtColor) {
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
			final SwingDeployer swingDeployer = new SwingDeployer(userConfig);
			// create the GUI for the client
			JFrame clientWindow = swingDeployer.createJFrame();
			// add a WindowLister to the frame to gracefully shutdown the client
			clientWindow.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent e) {
					swingDeployer.exitClient();
					System.exit(0);
				}
			});
			// display the GUI
			clientWindow.pack();
			clientWindow.setVisible(true);
			swingDeployer.printActiveConnections();
		} catch (ClientException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

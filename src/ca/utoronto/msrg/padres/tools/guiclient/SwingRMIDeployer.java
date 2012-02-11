// =============================================================================
// This file is part of The PADRES Project.
//
// For more information, see http://www.msrg.utoronto.ca
//
// Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
// =============================================================================
// $Id$
// =============================================================================
/*
 * Created on 06-Mar-2004
 *
 */
package ca.utoronto.msrg.padres.tools.guiclient;

/**
 * @author Guoli Li
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.common.comm.CommunicationException;
import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.AdvertisementMessage;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.CompositeSubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.Message;
import ca.utoronto.msrg.padres.common.message.MessageDestination;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.PublicationMessage;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.SubscriptionMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.demo.workflow.resources.JobSchedulerResources;
import ca.utoronto.msrg.padres.tools.guiclient.dialogs.IssueTrigger;
import ca.utoronto.msrg.padres.tools.guiclient.dialogs.JobScheduleLoader;

/**
 * @author gli
 */
public class SwingRMIDeployer extends UnicastRemoteObject implements RMIMessageListenerInterfce,
		ActionListener {

	private static final long serialVersionUID = -5254227997104387165L;

	public static HashMap<String, String> triggerMap = new HashMap<String, String>();
	static Logger exceptionLogger = Logger.getLogger("Exception");
	
	boolean isError = false;

	String fileName;

	private String clientID;

	private static RMIServerInterface rmiConnection = null;

	private static MessageDestination clientDest;

	JTextField input;

	public static JTextArea output;

	JFrame mainFrame = null;

	public SwingRMIDeployer(String clientID) throws RemoteException {
		super();
		this.clientID = clientID;
	}

	public void actionPerformed(ActionEvent e) {
		String command = input.getText();
		output.append(command + "\n");
		input.selectAll();
		// System.out.println("here is an action of input, and start handleCommand!"
		// + "\n");
		try {
			handleCommand(command);
		} catch (ParseException ex) {
			exceptionLogger.error(ex.getMessage());
		}
	}

	public static void handleCommand(String userCommand) throws ParseException {
		// add by gli
		StreamTokenizer st = new StreamTokenizer(new StringReader(userCommand));
		st.quoteChar('"');
		st.wordChars('<', '<');
		st.wordChars('=', '=');
		st.wordChars('>', '>');
		st.wordChars('_', '_');
		st.wordChars('[', ']');
		st.wordChars('{', '}');
		st.wordChars('(', ')');
		st.wordChars(',', ',');
		st.wordChars('.', '.');
		st.wordChars(';', ';');
		st.wordChars(':', ':');
		st.wordChars('$', '$');
		st.wordChars('&', '&');
		st.wordChars('|', '|');
		st.wordChars('\'', '\'');

		while (st.ttype != StreamTokenizer.TT_EOF) {
			try {
				st.nextToken();
				if (st.ttype == StreamTokenizer.TT_EOF)
					break;
				String command = st.sval;

				if (command.equalsIgnoreCase("publish") || command.equalsIgnoreCase("p")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					System.out.println(st.sval);
					Publication pub = MessageFactory.createPublicationFromString(st.sval);
					PublicationMessage pubMsg = new PublicationMessage(pub, "-1", clientDest);
					rmiConnection.receiveMessage(pubMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("subscribe") || command.equalsIgnoreCase("s")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					Subscription sub = MessageFactory.createSubscriptionFromString(st.sval);
					System.out.println(sub.toString());
					SubscriptionMessage subMsg = new SubscriptionMessage(sub, "-1", clientDest);
					rmiConnection.receiveMessage(subMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("cs")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					CompositeSubscription comSub = new CompositeSubscription(st.sval);
					CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(comSub,
							"-1", clientDest);
					rmiConnection.receiveMessage(csMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("advertise") || command.equalsIgnoreCase("a")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					Advertisement adv = MessageFactory.createAdvertisementFromString(st.sval);
					System.out.println(adv.toString());
					AdvertisementMessage advMsg = new AdvertisementMessage(adv, "-1", clientDest);
					rmiConnection.receiveMessage(advMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("unsubscribe")
						|| command.equalsIgnoreCase("us")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					// TODO: implement unsubscribe
				} else if (command.equalsIgnoreCase("unadvertise")
						|| command.equalsIgnoreCase("ua")) {
					st.wordChars(' ', ' ');
					st.nextToken();
					// TODO: implement unadvertise
				}
			} catch (IOException e) {
			}
		}
	}

	public static void publish(String trigger) {
		try {
			PublicationMessage pubMsg = new PublicationMessage(MessageFactory.createPublicationFromString(trigger), "-1",
					clientDest);
			rmiConnection.receiveMessage(pubMsg, HostType.CLIENT);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public String getID() throws RemoteException {
		return clientDest.getDestinationID();
	}

	public String receiveMessage(Message msg) {
		Publication pub = ((PublicationMessage) msg).getPublication();
		if (pub.getPairMap().containsKey("tid")) {
			pub.getPairMap().remove("tid");
		}
		output.append("Got Publication: " + pub + "\n");
		return pub.getPubID();
	}

	public void run(RMIAddress rmiAddress) {
		try {
			output.append("Making RMI connection to " + rmiAddress + "\n");
			Registry registry = LocateRegistry.getRegistry(rmiAddress.getHost(),
					rmiAddress.getPort());
			rmiConnection = (RMIServerInterface) registry.lookup(rmiAddress.getNodeID());
		} catch (Exception e) {
			output.append("ERROR: RMI connection failed: " + e + "\n");
			isError = true;
		}

		if (rmiConnection == null) {
			output.append("ERROR: RMI connection failed: rmiConnection is null" + "\n");
			isError = true;
		}

		if (!isError) {
			output.append("RMI connection successful" + "\n");
			try {
				clientDest = MessageDestination.formatClientDestination(clientID,
						rmiAddress.getNodeID());
				rmiConnection.registerMessageListener(clientDest, this);
				handleCommand("a [class,eq,AGENT_CTL],[agentname,isPresent,'agentA'],[command,isPresent,'ADVERTISE'],[content,isPresent,'any']");
				// handleCommand("a [class,eq,Trigger],[applname,eq,PAYROLL],[GID,isPresent,g001],[schedule,isPresent,DAILY]");
			} catch (Exception e) {

			}
		}
	}

	public JFrame createJFrame() {
		final JFrame frame = new JFrame(JobSchedulerResources.T_TRIGGER_DEFINITION);

		final JMenuBar mb = new JMenuBar();
		final JMenu mainMenu = new JMenu(JobSchedulerResources.M_MAIN);
		// final JMenu triggerMenu = new JMenu(JobSchedulerResources.M_TRIGGER);
		final JMenu applMenu = new JMenu(JobSchedulerResources.M_APPLICATION);
		// final JMenu postProMenu = new JMenu("Others");

		JMenuItem exitItem = new JMenuItem(JobSchedulerResources.M_EXIT);
		mainMenu.add(exitItem);
		mb.add(mainMenu);

		JMenuItem deployAppl = new JMenuItem("Deployment");
		// JMenuItem applControl = new JMenuItem("Control");
		applMenu.add(deployAppl);
		// applMenu.add(applControl);
		// mb.add(applMenu);

		// JMenuItem defTriggerItem = new
		// JMenuItem(JobSchedulerResources.M_TRIGGER_DEFINITION);
		JMenuItem issueTriggerItem = new JMenuItem(JobSchedulerResources.M_TRIGGER_ISSUE);
		// applMenu.add(defTriggerItem);
		applMenu.addSeparator();
		applMenu.add(issueTriggerItem);
		mb.add(applMenu);
		// JMenuItem postProItem = new JMenuItem("Post_Pro");
		// JMenuItem testItem = new JMenuItem("Test");
		// postProMenu.add(postProItem);
		// postProMenu.add(testItem);
		// mb.add(postProMenu);

		frame.getRootPane().setJMenuBar(mb);

		exitItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				frame.dispose();
				System.exit(0);
			}
		});

		/*
		 * defTriggerItem.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent e) { System.out.println("define trigger..."); } });
		 */

		issueTriggerItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				System.out.println("issue trigger...");
				// IssueTrigger issueTriggerDialog = new
				// IssueTrigger("IssueTrigger");

				// issueTriggerDialog.show();
				// JFrame frame =
				IssueTrigger dialog = new IssueTrigger(mainFrame, "Issue Trigger", true);

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

		/*
		 * applControl.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent e) { System.out.println("imperfectMerge enable..."); } });
		 */

		/*
		 * postProItem.addActionListener(new ActionListener() { public void
		 * actionPerformed(ActionEvent e) { System.out.println("post-Process enable..."); } });
		 * 
		 * testItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent
		 * e) { JFileChooser chooser = new JFileChooser(); chooser.setCurrentDirectory(new
		 * File("."));
		 * 
		 * chooser.setFileFilter(new javax.swing.filechooser.FileFilter() { public boolean
		 * accept(File f) { String name = f.getName().toLowerCase(); return name.endsWith(".log")||
		 * f.isDirectory(); }
		 * 
		 * public String getDescription() { // TODO Auto-generated method stub return null; } });
		 * 
		 * int r = chooser.showOpenDialog(null); if (r == JFileChooser.APPROVE_OPTION){ fileName =
		 * chooser.getSelectedFile().getAbsolutePath(); } try{ //FileInputStream dataFile = new
		 * FileInputStream(fileName); BufferedReader in = new BufferedReader(new
		 * FileReader(fileName)); output.append("file " + fileName + " is opened."); String line;
		 * try { while ( (line =in.readLine()) != null ){ String newLine; if
		 * (line.startsWith("Subscription")) { newLine = "s "; }else{ if
		 * (line.startsWith("Publication")){ newLine = "p "; }else{ newLine = "a "; } } int index =
		 * line.indexOf("["); String subLine = line.substring(index); newLine =
		 * newLine.concat(subLine);
		 * 
		 * output.append("\n" + newLine ); handleCommand(newLine); int p = 1000; while (p !=0){ p--;
		 * } } } catch (IOException e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
		 * 
		 * }catch (FileNotFoundException ef){ System.out.println("file " + fileName +
		 * " is not found!"); }
		 * 
		 * } });
		 */
		/*
		 * GJApp.launch(frame, "A Menu Bar",300,300,300,250);
		 */
		JPanel contents = new JPanel();
		contents.setLayout(new BorderLayout());

		output = new JTextArea(20, 50);
		output.setEditable(false);
		JScrollPane outputpane = new JScrollPane(output);
		contents.add(outputpane, BorderLayout.NORTH);

		input = new JTextField(50);

		input.setEditable(true);
		input.addActionListener(this);
		// JScrollPane inputpane = new JScrollPane(input);
		contents.add(input, BorderLayout.SOUTH);

		// frame.isValid();

		frame.setContentPane(contents);

		// frame.getContentPane().add(contents, BorderLayout.CENTER);
		mainFrame = frame;

		return frame;
	}

	public static JTextArea getOutput() {
		return output;
	}

	public RMIServerInterface getConnection() {
		return rmiConnection;
	}

	public static void main(String[] args) throws RemoteException {
		// System.out.println("My test start here!\n");

		// CompositeSubscription cs = new CompositeSubscription("test");
		/*
		 * String test = "$B$X=>kk"; int index_1 = test.indexOf("$"); String testTmp =
		 * test.substring(index_1 +1); int index_2 = test.lastIndexOf("=>");
		 * System.out.println("index_1 = " + index_1); System.out.println("index_2 = " + index_2);
		 * System.out.println(testTmp.substring(0,index_2));
		 * System.out.println(testTmp.substring(index_2 + 1));
		 */

		String usage = "Usage: SwingRMIClient <client_id> <broker_uri>";

		RMIAddress rmiAddress = null;
		if (args.length == 2) {
			try {
				rmiAddress = new RMIAddress(args[1]);
			} catch (CommunicationException e) {
				System.out.println(e.getMessage());
				System.out.println(usage);
				System.exit(1);
			}
		}

		if (rmiAddress == null) {
			System.out.println(usage);
			System.exit(1);
		}

		System.setSecurityManager(new RMISecurityManager() {

			public void checkConnect(String host, int port) {
			}

			public void checkConnect(String host, int port, Object context) {
			}
		});

		JFrame.setDefaultLookAndFeelDecorated(true);
		SwingRMIDeployer c = new SwingRMIDeployer(args[0]);
		JFrame frame = c.createJFrame();

		// Finish setting up the frame, and show it.
		frame.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.pack();
		frame.setVisible(true);

		c.run(rmiAddress);

	}
}

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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import ca.utoronto.msrg.padres.common.comm.CommSystem.HostType;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIAddress;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIServerInterface;
import ca.utoronto.msrg.padres.common.comm.rmi.RMIMessageListenerInterfce;
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
import ca.utoronto.msrg.padres.common.message.ValidMessage;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;

/**
 * @author Pengcheng Wan
 * 
 *         Read advertisement and publish from a deploy file. This is for agent client testing class
 */
public class SmallJobDeployer extends UnicastRemoteObject implements RMIMessageListenerInterfce {

	private static final long serialVersionUID = -4129271289896627315L;

	public static final String DEFAULT_FILE = "TESTCASE.TXT";

	private String clientID;

	private MessageDestination clientDest;

	private RMIServerInterface rmiConnection;

	private boolean isError = false;

	/**
     * 
     */
	public SmallJobDeployer(String id) throws RemoteException {
		clientID = id;
	}

	@Override
	public String getID() throws RemoteException {
		return clientDest.getDestinationID();
	}

	public String receiveMessage(Message msg) {
		Publication pub = ((PublicationMessage) msg).getPublication();
		System.out.println("Got Publication: " + pub + "\n");
		return msg.getMessageID();
	}

	public void handleCommand(String userCommand) throws ParseException {
		ValidMessage valid = new ValidMessage("");
		userCommand = valid.trimMessage(userCommand);
		System.out.println("\nuser commond is : " + userCommand);

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
					st.nextToken();
					System.out.println("PUBLISH:  " + st.sval);
					Publication pub = MessageFactory.createPublicationFromString(st.sval);
					PublicationMessage pubMsg = new PublicationMessage(pub, "-1", clientDest);
					rmiConnection.receiveMessage(pubMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("subscribe") || command.equalsIgnoreCase("s")) {
					st.nextToken();
					Subscription sub = MessageFactory.createSubscriptionFromString(st.sval);
					System.out.println("SUBSCRIBE: " + sub.toString());
					SubscriptionMessage subMsg = new SubscriptionMessage(sub, "-1", clientDest);
					rmiConnection.receiveMessage(subMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("compositesubscribe")
						|| command.equalsIgnoreCase("cs")) {
					st.nextToken();
					CompositeSubscription comSub = new CompositeSubscription(st.sval);
					CompositeSubscriptionMessage csMsg = new CompositeSubscriptionMessage(comSub,
							"-1", clientDest);
					rmiConnection.receiveMessage(csMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("advertise") || command.equalsIgnoreCase("a")) {
					st.nextToken();
					Advertisement adv = MessageFactory.createAdvertisementFromString(st.sval);
					System.out.println("ADVERTISE: " + adv.toString());
					AdvertisementMessage advMsg = new AdvertisementMessage(adv, "-1", clientDest);
					rmiConnection.receiveMessage(advMsg, HostType.CLIENT);
				} else if (command.equalsIgnoreCase("unsubscribe")
						|| command.equalsIgnoreCase("us")) {
					st.nextToken();
					// TODO: implement unsubscribe
				} else if (command.equalsIgnoreCase("unadvertise")
						|| command.equalsIgnoreCase("ua")) {
					st.nextToken();
					// TODO: implement unadvertise
				}
			} catch (IOException e) {
			}
		}
	}

	/**
	 * Make a connection to Padres infrastructure, the format should be rmi://[host of
	 * object]:[registry port]/[object name]
	 * 
	 * @param args
	 *            Host name or host name with designated port number
	 */
	public void run(String[] args) {
		try {
			RMIAddress rmiAddress = new RMIAddress(args[0]);
			clientDest = MessageDestination.formatClientDestination(clientID,
					rmiAddress.getNodeID());
			System.out.println("Making RMI connection to " + args[0] + " ...\n");
			rmiConnection = (RMIServerInterface) Naming.lookup(args[0]);
		} catch (Exception e) {
			System.out.println("ERROR: RMI connection failed: " + e + "\n");
			isError = true;
		}

		if (rmiConnection == null) {
			System.out.println("ERROR: RMI connection failed: rmiConnection is null" + "\n");
			isError = true;
		}

		if (!isError) {
			System.out.println("RMI connection successful..." + "\n");
			try {
				rmiConnection.registerMessageListener(clientDest, this);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		// DO the real deploy here
		this.handleDeploy(args);

	}

	private void handleDeploy(String[] args) {
		String deployFile = (args.length < 3) ? DEFAULT_FILE : args[2];
		try {
			BufferedReader br = new BufferedReader(new FileReader(deployFile));
			String temp = "";
			while ((temp = br.readLine()) != null) {
				System.out.println(temp);
				if (temp.length() > 0) {
					handleCommand(temp);
				}
				Thread.sleep(2000);
			}
			br.close();
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws RemoteException {
		if (args.length < 1) {
			System.out.println("Usage: SmallJobDeployert <broker URI> <fileName>");
			System.exit(1);
		}

		System.setSecurityManager(new RMISecurityManager() {

			public void checkConnect(String host, int port) {
			}

			public void checkConnect(String host, int port, Object context) {
			}
		});

		SmallJobDeployer jd = new SmallJobDeployer("deployer");
		jd.run(args);

	}
}

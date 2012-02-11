/*
 * Created on 22-Sep-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StreamTokenizer;

import javax.swing.JFileChooser;
import javax.swing.JLabel;

import org.apache.log4j.Logger;

import ca.utoronto.msrg.padres.client.ClientException;
import ca.utoronto.msrg.padres.common.message.Advertisement;
import ca.utoronto.msrg.padres.common.message.CompositeSubscription;
import ca.utoronto.msrg.padres.common.message.Publication;
import ca.utoronto.msrg.padres.common.message.Subscription;
import ca.utoronto.msrg.padres.common.message.parser.MessageFactory;
import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorClient;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * @author gerald
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BatchMsgDialog extends MonitorDialog {

	protected static final long serialVersionUID = 1L;

	protected JFileChooser m_chooser = new JFileChooser();

	protected String m_fileName;

	protected MonitorFrame monitor;

	protected MonitorClient monitorClient;

	protected JLabel m_ResultText; /* text showing user the result */

	static Logger exceptionLogger = Logger.getLogger("Exception");

	/**
	 * Contruct a new batch msg dialog
	 * 
	 * @param owner
	 *            the mointor frame of the
	 * @param client
	 *            the connection manager that hold the connection
	 */
	public BatchMsgDialog(MonitorFrame owner, MonitorClient client) {
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

			m_ResultText.setText(m_fileName + " processed");

		} else {
			// user cancel the operation
			m_ResultText.setText("User canceled action");
		}
	}

	public void buildContentPanel() {
		m_ResultText = new JLabel(" ");
		add(m_ResultText, 0, 0, 0.0, 0.0);
		pack();
	}

	public int getCommandID() {
		return MonitorResources.CMD_BATCH_MSG;
	}

	public void notify(Object o) {

	}

	private void handlefile(BufferedReader in) {
		StreamTokenizer st = new StreamTokenizer(in);
		handleCommandStream(st, monitorClient);
	}

	/**
	 * Handle the command in the file
	 * 
	 * @param st
	 *            streamtokenizer that have the file
	 * @param client
	 *            the connection to the federation
	 */
	public static void handleCommandStream(StreamTokenizer st, MonitorClient client) {
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
					// System.out.println(st.sval);
					Publication pub = MessageFactory.createPublicationFromString(st.sval);
					client.publish(pub);
				} else if (command.equalsIgnoreCase("subscribe") || command.equalsIgnoreCase("s")) {
					st.nextToken();
					Subscription sub = MessageFactory.createSubscriptionFromString(st.sval);
					client.subscribe(sub);
				} else if (command.equalsIgnoreCase("cs")) {
					st.nextToken();
					CompositeSubscription comSub = new CompositeSubscription(st.sval);
					client.subscribeCS(comSub);
				} else if (command.equalsIgnoreCase("advertise") || command.equalsIgnoreCase("a")) {
					st.nextToken();
					Advertisement adv = MessageFactory.createAdvertisementFromString(st.sval);
					client.advertise(adv);
				} else if (command.equalsIgnoreCase("unsubscribe")
						|| command.equalsIgnoreCase("us")) {
					st.nextToken();
					client.unSubscribe(st.sval);
				} else if (command.equalsIgnoreCase("unadvertise")
						|| command.equalsIgnoreCase("ua")) {
					st.nextToken();
					client.unAdvertise(st.sval);
				}
			} catch (ClientException e) {
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				exceptionLogger.error(e.getMessage());
			}
		}

	}

	private void processfile(String fileName) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			handlefile(in);
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

}

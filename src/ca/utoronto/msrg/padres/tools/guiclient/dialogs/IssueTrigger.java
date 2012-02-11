/*
 * Created on 2004-8-26
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.guiclient.dialogs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import ca.utoronto.msrg.padres.tools.guiclient.SwingRMIDeployer;

/**
 * @author Li Guoli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class IssueTrigger extends JDialog {

	// private HashMap triggerMap = new HashMap();

	/**
	 * 
	 */
	private static final long serialVersionUID = -6310759846732109359L;
	private ConstraintsPanel cp = new ConstraintsPanel();

	public IssueTrigger(final JFrame frame, String title, boolean f) {
		// Object BorderLayout = null;
		super(frame, title, f);
		Container dialogContentPane = this.getContentPane();
		JButton ok_button = new JButton("Issue");
		JButton cancel_button = new JButton("Cancel");
		dialogContentPane.add(cp, BorderLayout.CENTER);
		/*
		 * GridBagLayout gbl = new GridBagLayout(); GridBagConstraints gbc = new
		 * GridBagConstraints();
		 * 
		 * dialogContentPane.setLayout(gbl); gbc.anchor = GridBagConstraints.NORTHWEST;
		 * 
		 * dialogContentPane.add(ok_button,gbc); dialogContentPane.add(Box.createVerticalStrut(220),
		 * gbc); dialogContentPane.add(cancel_button,gbc);
		 * //dialogContentPane.add(Box.createHorizontalStrut(20), gbc);
		 */
		dialogContentPane.add(ok_button, BorderLayout.SOUTH);
		ok_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				// String trigger = "[class,Trigger],[applname,'" + cp.getApplName()+ "'],[GID,'" +
				// cp.getGID()+"'],[schedule,'2004.09.11']";
				String trigger = "[class,Trigger],[applname,'" + cp.getApplName() + "'],[GID,'"
						+ cp.getGID() + "'],[schedule,'" + cp.getSchedule() + "']";
				if (cp.getApplName().equals("") || cp.getGID().equals("")) {
					SwingRMIDeployer.output.append("Please specify application name and generation ID.\n");
					// dispose();
				} else {
					// Check the trigger generation id
					String key = cp.getApplName() + cp.getGID();
					if (SwingRMIDeployer.triggerMap.containsKey(key)) {
						JOptionPane.showMessageDialog(frame, "This generation ID has been used!",
								"Alert Information", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					SwingRMIDeployer.triggerMap.put(key, cp.getGID());

					SwingRMIDeployer.publish(trigger);
					System.out.println("trigger is : " + trigger);
					SwingRMIDeployer.output.append("p " + trigger + "\n");
					// frame.dispose();
				}

				// exit(0);
				// this.

			}
		});
		cancel_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("issue trigger...by button");
				frame.dispose();
				// exit(0);
				// this.

			}
		});
		// dialogContentPane.add(ok_button);
		// dialogContentPane.add(cancel_button);
		this.pack();

	}
}

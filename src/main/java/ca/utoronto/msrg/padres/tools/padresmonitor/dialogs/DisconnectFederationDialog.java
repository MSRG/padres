package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * This dialog allows the user to disconnect from the federation of brokers.
 */
public class DisconnectFederationDialog extends MonitorDialog {

	private static final long serialVersionUID = 5392504355117961448L;

	/**
	 * Construct a dialog that allows the user to disconnect from the federation
	 * of brokers.
	 * 
	 * @param owner
	 *            hook to MonitorFrame.
	 */
	public DisconnectFederationDialog(MonitorFrame owner) {
		super(owner, MonitorResources.T_DISCONNECT_FEDERATION);
	}

	/** @see TODO: MonitorDialog */
	public void buildContentPanel() {
		JLabel label = new JLabel(MonitorResources.L_ARE_YOU_SURE);
		add(label, 0, 0, 0.0, 0.0);
		add(new JLabel(), 0, 1, 1.0, 1.0); // padding
	}

	/**
	 * @see TODO: MonitorDialog
	 * @return The command identifier.
	 */
	public int getCommandID() {
		return MonitorResources.CMD_FEDERATION_DISCONNECT;
	}

	public void notify(Object o) {

	}
}

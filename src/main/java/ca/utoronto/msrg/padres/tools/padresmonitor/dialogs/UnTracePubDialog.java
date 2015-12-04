package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
* This dialog lets the user to untrace the message they have traced
* The connection is established through a single broker.
*/
public class UnTracePubDialog extends MonitorDialog {
		
		/** ID of the broker to stop. */
		private String m_BrokerID;
		
		private JLabel m_DialogLabel;

		/**
		* Construct a trace message dialog for the specified broker.
		* @param owner  Hook to MontiorFrame.
		* @param brokerID  The ID of the user selected broker.
		*/
		public UnTracePubDialog(MonitorFrame owner, String brokerID) {
			super(owner, MonitorResources.T_TRACE_PUB_MSG);
			m_DialogLabel.setText(MonitorResources.L_UNTRACE_MESSAGE);
			m_BrokerID = brokerID;		
		}

		
			
		
		
		/** @see TODO: MonitorDialog */
		public void buildContentPanel() {
			m_DialogLabel = new JLabel();
			add(m_DialogLabel, 0, 1, 30.0, 1.0); // padding
			add(new JLabel(), 0, 2, 30.0, 1.0); // padding
		}

		/**
		* @see TODO: MonitorDialog
		* @return  The command identifier.
		*/
		public int getCommandID() {
			return MonitorResources.CMD_UNTRACE_MESSAGE;
		}

		/**
		* Return the broker ID of the selected broker.
		* @return  The broker ID.
		*/
		public String getBrokerID() {
			return m_BrokerID;
		}
		
		public void notify(Object o) {
			
		}
		
}
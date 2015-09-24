package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import javax.swing.JLabel;
import javax.swing.JTextField;

import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;



/**
* This dialog lets the user enter the necessary parameters to 
* trace a message from the broker
* The connection is established through a single broker.
*/
public class TraceSubDialog extends MonitorDialog {
		
		/** ID of the broker to stop. */
		private String m_BrokerID;


		/** Field for user to enter the message to be trace. */
		private JTextField m_msgTextField;
		
		/**
		* Construct a trace message dialog for the specified broker.
		* @param owner  Hook to MontiorFrame.
		* @param brokerID  The ID of the user selected broker.
		*/
		public TraceSubDialog(MonitorFrame owner, String brokerID) {
			super(owner, MonitorResources.T_TRACE_SUB_MSG);
			m_BrokerID = brokerID;
			
		
		}

		
		/** @see TODO: trace route Dialog */
		public void buildContentPanel() {
			JLabel traceLabel = new JLabel(MonitorResources.L_TRACE_SUB_MSG);
			m_msgTextField = new JTextField();
			
			add(traceLabel, 0, 0, 0.0, 0.0);
			add(m_msgTextField, 1, 0, 30.0, 3.0);
			add(new JLabel(), 0, 1, 30.0, 1.0); // padding
		}

		/**
		* @see TODO: MonitorDialog
		* @return  The command identifier.
		*/
		public int getCommandID() {
			return MonitorResources.CMD_TRACE_SUB_MESSAGE;
		}

		/**
		* Return the broker ID of the selected broker.
		* @return  The broker ID.
		*/
		public String getBrokerID() {
			return m_BrokerID;
		}
		
		/**
		* Return the contents of the message text field.
		* @return  Host.
		*/
		public String getMSg() {
			return m_msgTextField.getText();
		}
		
		public void notify(Object o) {
			
		}

}
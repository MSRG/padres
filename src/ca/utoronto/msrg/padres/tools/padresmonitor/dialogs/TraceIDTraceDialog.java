/*
 * Created on 23-Sep-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

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
public class TraceIDTraceDialog extends MonitorDialog {
		
		/** Field for user to enter the trace id */
		private JTextField m_idTextField;
		
		/**
		* Construct a trace message dialog
		* @param owner  Hook to MontiorFrame.
		*/
		public TraceIDTraceDialog(MonitorFrame owner) {
			super(owner, MonitorResources.T_TRACE_BY_ID);		
		}

		
		/** @see TODO: trace route Dialog */
		public void buildContentPanel() {
			JLabel traceLabel = new JLabel(MonitorResources.L_TRACE_ID);
			m_idTextField = new JTextField();
			
			add(traceLabel, 0, 0, 0.0, 0.0);
			add(m_idTextField, 1, 0, 30.0, 3.0);
			add(new JLabel(), 0, 1, 30.0, 1.0); // padding
		}

		/**
		* @return  The command identifier.
		*/
		public int getCommandID() {
			return MonitorResources.CMD_TRACE_BY_TRACEID;
		}
		
		/**
		* Return tarce id
		* @return  traceid
		*/
		public String getTraceID() {
			return m_idTextField.getText();
		}

		public void notify(Object o) {
			
		}
}

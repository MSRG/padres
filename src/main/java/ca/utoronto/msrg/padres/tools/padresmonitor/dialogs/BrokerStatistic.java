/*
 * Created on May 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package ca.utoronto.msrg.padres.tools.padresmonitor.dialogs;

import java.util.Iterator;

import javax.swing.JLabel;

import ca.utoronto.msrg.padres.tools.padresmonitor.BrokerUI;
import ca.utoronto.msrg.padres.tools.padresmonitor.MonitorFrame;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;

/**
 * The Diaologe for broker properties
 */
public class BrokerStatistic extends MonitorDialog {
	
	/** JLabel for the broker id text */
	private JLabel m_brokerIDText;
	
	/** Incoming publication message rate */
	private JLabel m_InPubRate;
	
	/** Incoming control message rate */
	private JLabel m_InConRate;
	
	/** Queue time */
	private JLabel m_QueueTime;
	
	/** Match time */
	private JLabel m_MatchTime;
		
	/**
	* Construct a trace message dialog for the specified broker.
	* @param owner  Hook to MontiorFrame.
	* @param brokerID  The ID of the user selected broker.
	*/
	public BrokerStatistic(MonitorFrame owner, BrokerUI broker) {
		super(owner, MonitorResources.T_BROKER_STAT);
	
		m_brokerIDText.setText(broker.getBrokerID());
		m_InPubRate.setText(broker.getInPubRate());
		m_InConRate.setText(broker.getInConRate());
		m_QueueTime.setText(broker.getQueueTime());
		m_MatchTime.setText(broker.getMatchTime());
			
	}

	
	public void buildContentPanel() {
		JLabel brokerTitleLabel = new JLabel(MonitorResources.L_BROKER);
		JLabel inPubRate = new JLabel(MonitorResources.L_IN_PUB_RATE);
		JLabel inConRate = new JLabel(MonitorResources.L_IN_CON_RATE);
		JLabel queueTime = new JLabel(MonitorResources.L_QUEUE_TIME);
		JLabel matchTime = new JLabel(MonitorResources.L_MATCH_TIME);
		
		m_brokerIDText = new JLabel("");
		m_InPubRate = new JLabel("");
		m_InConRate = new JLabel("");
		m_QueueTime = new JLabel("");
		m_MatchTime = new JLabel("");
					
		add(brokerTitleLabel, 0, 0, 1.0, 1.0);
		add(m_brokerIDText, 1, 0, 1.0, 1.0);
		add(inPubRate, 0, 1, 1.0, 1.0);
		add(m_InPubRate, 1, 1, 1.0, 1.0);
		add(inConRate, 0, 2, 1.0, 1.0);
		add(m_InConRate, 1, 2, 1.0, 1.0);
		add(queueTime, 0, 3, 1.0, 1.0);
		add(m_QueueTime, 1, 3, 1.0, 1.0);
		add(matchTime, 0, 4, 1.0, 1.0);
		add(m_MatchTime, 1, 4, 1.0, 1.0);
		pack(); // pack this dialog to its proper/preferred size
	}

	public int getCommandID() {
		return MonitorResources.CMD_BROKER_STAT;
	}

	/**
	 * Function to reutrn a list of neighbours as String
	 * @param broker
	 * @return The list of neighbours
	 */
	private String getNeighbours(BrokerUI broker) {
		String neighboursList= "";
		Iterator i = broker.neighbourIterator();
		while(i.hasNext()) {
			neighboursList += ","+i.next().toString()+" ";
		}
		if (neighboursList.length()> 0) {
			// Take out the leading "," */
			neighboursList = neighboursList.substring(1);
		}
		
		return neighboursList;	
	}
	
	public void notify(Object o) {
		
	}
}

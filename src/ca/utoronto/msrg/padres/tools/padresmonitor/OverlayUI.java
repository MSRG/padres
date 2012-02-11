package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.util.Set;

import javax.swing.JComponent;

import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorVertex;

/**
 * Abstract superclass of a graphical component representing the overlay
 * network.
 */
public abstract class OverlayUI extends JComponent {

	private static final long serialVersionUID = 2619743890970469921L;

	/** Construct a new OverlayUI. */
	protected OverlayUI() {
		super();
	}

	/**
	 * Add a broker to the graphical overlay diagram.
	 * 
	 * @param broker
	 *            Broker to add.
	 * @return true if the broker was removed.
	 */
	public abstract boolean addBroker(BrokerUI broker);

	/**
	 * Remove a broker from the graphical overlay diagram.
	 * 
	 * @param broker
	 *            Broker to remove.
	 * @return true if the broker was removed.
	 */
	public abstract boolean removeBroker(BrokerUI broker);

	/**
	 * Show two brokers as neighbors in the graphical overlay diagram.
	 * 
	 * @param broker1
	 *            First broker.
	 * @param broker2
	 *            Second broker.
	 */
	public abstract void addNeighbour(BrokerUI broker1, BrokerUI broker2);

	/**
	 * Break the link between two brokers that were previously neighbors.
	 * 
	 * @param broker1
	 *            First broker.
	 * @param broker2
	 *            Second broker.
	 */
	public abstract void removeNeighbour(BrokerUI broker1, BrokerUI broker2);

	/** Remove all brokers from the graphical overlay diagram. */
	public abstract void clear();

	/**
	 * Determine if there are any brokers represented in the graphical overlay
	 * diagram.
	 * 
	 * @return true if the diagram contains no brokers.
	 */
	public abstract boolean isEmpty();

	/**
	 * Determine if the graphical overlay diagram contains the specified broker.
	 * 
	 * @param broker
	 *            The broker.
	 */
	public abstract boolean containsBroker(BrokerUI broker);

	/**
	 * Determine if two brokers are shown as neighbours in the graphical overlay
	 * diagram.
	 * 
	 * @param broker1
	 *            First broker.
	 * @param broker2
	 *            Second broker.
	 * @return true if the specified brokers are neighbours.
	 */
	public abstract boolean isNeighbour(BrokerUI broker1, BrokerUI broker2);

	/**
	 * Set the status of the specified broker in the diagram.
	 * 
	 * @param broker
	 *            The broker.
	 */
	public abstract void setBrokerStatus(BrokerUI broker, int status);

	/**
	 * Method to cause the graphic representation of the broker to be drawn as
	 * activated
	 * 
	 * @param broker
	 *            The broker that want to be activated
	 */
	public abstract void activeteBroker(BrokerUI broker);

	public abstract void activateEdge(BrokerUI broker1, BrokerUI broker2);

	public abstract void activeClientBrokerEdge(BrokerUI broker, String clientID);

	public abstract void deactivateAllEdge();

	/**
	 * Get the ID of the currently selected broker in the overlay diagram.
	 * 
	 * @return The ID of the selected broker. Returns null if no broker is
	 *         selected.
	 */
	public abstract BrokerUI getSelectedBrokerUI();

	public abstract void handleFailureClassMsg(String detectorBrokerID, String failureBrokerID,
			String type);

	public abstract void removeOldClient(BrokerUI broker, Set<String> clientSet);

	/**
	 * Layout the nodes in the overlay graph using the specified algorithm.
	 * 
	 */
	public abstract void applyLayout(int algorithm);

	/**
	 * Display message counter for all edges
	 * 
	 */
	public abstract void showAllEdgeMessages();

	/**
	 * Hide message counter for all edges
	 * 
	 */
	public abstract void hideAllEdgeMessages();

	/**
	 * Active/Deactivate/Reset edge relative thickness
	 * 
	 */
	public abstract void setEdgeThroughputIndicator(boolean state);

	public abstract void resetEdgeThroughputIndicator();

	public abstract void useNodeLabelType(MonitorVertex.LabelType type);
}
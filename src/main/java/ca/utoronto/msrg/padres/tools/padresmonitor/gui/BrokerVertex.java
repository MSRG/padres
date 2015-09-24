package ca.utoronto.msrg.padres.tools.padresmonitor.gui;

import ca.utoronto.msrg.padres.tools.padresmonitor.BrokerUI;

public class BrokerVertex extends MonitorVertex {

	private String vertexID;

	private BrokerUI broker;

	private final String TYPE = "BROKER_VERTEX_TYPE";

	private boolean isActive;

	// private boolean isInFailureState;
	private int temp_int;

	private String shortLabel;

	private String longLabel;
	
	private String label;

	public BrokerVertex(BrokerUI broker) {
		this.vertexID = broker.getBrokerID();
		this.broker = broker;
		this.isActive = false;
		// this.isInFailureState = false;
		this.temp_int = 0;
		this.longLabel = broker.getBrokerID();
		this.shortLabel = longLabel.replaceFirst("^.*/", "");
		this.label = this.shortLabel;
	}

	// public BrokerUI getBroker() {
	// return this.broker;
	// }

	public String getVertexID() {
		return this.vertexID;
	}

	public String getType() {
		return TYPE;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive() {
		this.isActive = true;

	}

	public void setInactive() {
		this.isActive = false;

	}

	// public void updateBroker(BrokerUI broker) {
	// this.broker = broker;
	// }

	public void update_int(int newval) {
		this.temp_int = newval;
	}

	public int get_int() {
		return this.temp_int;
	}

	public String getLabel() {
		return this.label;
	}

	@Override
	public String getLabel(LabelType type) {
		String label = null;
		switch (type) {
		case LT_SHORT:
			label = this.shortLabel;
			break;
		case LT_LONG:
			label = this.longLabel;
			break;
		}
		return label;
	}

	@Override
	public void useLabel(LabelType type) {
		switch (type) {
		case LT_SHORT:
			this.label = this.shortLabel;
			break;
		case LT_LONG:
			this.label = this.longLabel;
			break;
		}
	}

	// adding this in case vertex label is different from broker ID
	public String getBrokerID() {
		return this.broker.getBrokerID();
	}

}

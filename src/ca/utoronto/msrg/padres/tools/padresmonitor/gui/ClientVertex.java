package ca.utoronto.msrg.padres.tools.padresmonitor.gui;

import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorVertex.LabelType;

public class ClientVertex extends MonitorVertex {
	
	public String vertexID;
	public Object client; //need a client type similar to BrokerUI...
	public final String TYPE = "CLIENT_VERTEX_TYPE";
	public boolean isActive;
	public String longLabel;
	public String shortLabel;
	public String label;
	
	public ClientVertex(String vertexID, String brokerID) {
		this.vertexID = vertexID;
		this.longLabel = vertexID;
		this.shortLabel = vertexID.replaceFirst("^" + brokerID, ""); // remove common prefix with broker name
		this.shortLabel = this.shortLabel.replaceFirst("-", ""); // remove "-" prefix.
		this.label = this.shortLabel;
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
	
	public String getVertexID() {
		return this.vertexID;
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

}

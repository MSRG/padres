package ca.utoronto.msrg.padres.tools.padresmonitor.gui;

import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

public abstract class MonitorVertex extends UndirectedSparseVertex {
	
	private boolean isInFailureState;
	
	public abstract String getType();
	public abstract boolean isActive();
	public abstract void setActive();
	public abstract void setInactive();
	public abstract String getLabel();
	public abstract String getLabel(LabelType type);
	public abstract void useLabel(LabelType type);
	
	public enum LabelType {
		LT_LONG, LT_SHORT
	}

	public void setInFailureState() {
		//System.out.println("Is in failure state");
		this.isInFailureState = true;
	}
	
	public void clearFailureState() {
		//System.out.println("Is out of failure state");
		this.isInFailureState = false;
	}
	
	public boolean isInFailureState() {
		return this.isInFailureState;
	}
	
}

package ca.utoronto.msrg.padres.tools.padresmonitor.gui;

import java.awt.BasicStroke;
import java.awt.Stroke;

import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

public class MonitorEdge extends UndirectedSparseEdge {
	
	boolean active;
	int activationCount;
	boolean isInFailureState;
	boolean displayActivationCount;
	boolean throughputIndicatorActive;
	float edgeWidth;
	
	public MonitorEdge(UndirectedSparseVertex v1, UndirectedSparseVertex v2) {
		super(v1, v2);
		this.active = false;
		this.activationCount = 0;
		this.isInFailureState = false;
		this.displayActivationCount = false;
		this.throughputIndicatorActive = false;
		this.edgeWidth = MonitorResources.EDGE_DEFAULT_STROKE_WIDTH;
	}
		
	public MonitorEdge(UndirectedSparseVertex v1, UndirectedSparseVertex v2, boolean active) {
		this(v1,v2);
		this.active = active;
	}
	
	public boolean isActive() {
		return this.active;
	}
	
	public void incrementActivationCount() {
		this.activationCount++;
		this.active = true;
		if (throughputIndicatorActive == MonitorResources.EDGE_THROUGHPUT_ON) {
			this.edgeWidth++;
			
			if (edgeWidth > maxEdgeWidth) {
				maxEdgeWidth = edgeWidth;
			}
		} 
	}
	
	public int getActivationCount() {
		return this.activationCount;
	}
	
	public String getActivationCountMessage() {
		return new Integer(this.activationCount).toString();// + " Messages";
	}

	public void resetActivationCount() {
		this.activationCount = 0;
		this.active = false;
	}
	
	public void setActve() {
		this.active = true;
	}
	
	public void setInFailureState() {
		this.isInFailureState = true;
	}
	
	public void clearFailureState() {
		this.isInFailureState = false;
	}
	
	public boolean isInFailureState() {
		return this.isInFailureState;
	}
	
	public void toggleActivationCountMessage() {
		if (displayActivationCount == true) {
			displayActivationCount = false;
		} else {
			displayActivationCount = true;
		}
	}
	
	public boolean activationCountIsDisplayed() {
		return this.displayActivationCount;
	}
	
	public void displayActivationCountMessage(boolean displayActivationCount) {
		this.displayActivationCount = displayActivationCount;
	}
	
	public void enableThroughputIndicator(boolean state) {
		throughputIndicatorActive = state;
	}
	
	public void resetThroughputIndicator() {
		edgeWidth = MonitorResources.EDGE_DEFAULT_STROKE_WIDTH;
		resetActivationCount();
		maxEdgeWidth = edgeWidth;
	}
	
	public Stroke getStroke() {
		float width = applyFormula();
		return new BasicStroke(width);
	}
	
	
	private static final float OVERALL_MAX_EDGE_WIDTH = 10;
	private static float maxEdgeWidth = MonitorResources.EDGE_DEFAULT_STROKE_WIDTH;
	
	//currently all edges must use the same formula
	private float applyFormula() {
		// set formula here; use edgeWidth as the variable
		
		//formula to determine edge width; for now, just a linear y = x formula
//		float width = edgeWidth;

//		System.out.println(edgeWidth + "," + maxEdgeWidth);
		float width = edgeWidth / maxEdgeWidth * Math.min(maxEdgeWidth, OVERALL_MAX_EDGE_WIDTH);
		
		
		// after formula has been applied, resulting width should be no less
		// than MonitorResources.EDGE_DEFAULT_STROKE_WIDTH to edge thickness 
		// does not descrease from default when edge thickness feature is activated
		if (width < MonitorResources.EDGE_DEFAULT_STROKE_WIDTH) {
			width = MonitorResources.EDGE_DEFAULT_STROKE_WIDTH;
		}
		return width;
	}
}

package ca.utoronto.msrg.padres.tools.padresmonitor.gui;

import java.util.Iterator;
import java.util.Set;

import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.FRLayout;
import edu.uci.ics.jung.visualization.ISOMLayout;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.SpringLayout;
import edu.uci.ics.jung.visualization.StaticLayout;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayout;
import edu.uci.ics.jung.visualization.contrib.KKLayoutInt;

public class MonitorGraph extends UndirectedSparseGraph {

	Layout layout;
	
	public MonitorGraph() {
		super();
		// Since padres is a dynamic network, always start with a mutable layout -
		// use either SpringLayout or FRLayout

		// layout = new SpringLayout(this, new SpringLayout.UnitLengthFunction(100));
		layout = new FRLayout(this);

	}
	
	public MonitorEdge getEdge(MonitorVertex v1, MonitorVertex v2) {
		MonitorEdge e = null;
		boolean edgeFound = false;
		Set edgeSet = v1.getIncidentEdges();
		Iterator it = edgeSet.iterator();
		while (it.hasNext() && (edgeFound == false)) {
			e = (MonitorEdge) it.next();
			if (e.isIncident(v2)) {
				edgeFound = true;
			}
		}
		return e;
	}
	
	public void lockAllVertices() {
		Set allVertices = getVertices();
		for (Iterator it = allVertices.iterator(); it.hasNext();) {
			layout.lockVertex((Vertex) it.next());
		}
	}
	
	public void unlockAllVertices() {
		Set allVertices = getVertices();
		for (Iterator it = allVertices.iterator(); it.hasNext();) {
			layout.unlockVertex((Vertex) it.next());
		}
	}
	
	public Layout getLayout() {
		return this.layout;
	}
	
	public Layout applyLayout() {
		this.layout = new FRLayout(this);
		return this.layout;
	}
	
	public Layout applyLayout(int algorithm) {
		switch (algorithm) {
		case MonitorResources.LAYOUT_CIRCLE:
			this.layout = new CircleLayout(this);
			break;
		case MonitorResources.LAYOUT_FRUCHTERMAN_REINGOLD:
			this.layout = new FRLayout(this);
			break;
		case MonitorResources.LAYOUT_ISOM:
			this.layout = new ISOMLayout(this);
			break;
		case MonitorResources.LAYOUT_KAMADA_KAWAI:
			this.layout = new KKLayout(this);
			break;
		case MonitorResources.LAYOUT_KAMADA_KAWAI_INTEGER:
			this.layout = new KKLayoutInt(this);
			break;
		case MonitorResources.LAYOUT_SPRING:
			this.layout = new SpringLayout(this, new SpringLayout.UnitLengthFunction(100));
			break;
		case MonitorResources.LAYOUT_STATIC:
			this.layout = new StaticLayout(this);
			break;
		default:
			this.layout = new FRLayout(this);
			break;
		}
		return this.layout;
	}
	
	public void setEdgeThroughputIndicator(boolean state) {
		Set edgeSet = getEdges();
		for (Iterator it = edgeSet.iterator(); it.hasNext();) {
			MonitorEdge mEdge = (MonitorEdge) it.next();
			mEdge.enableThroughputIndicator(state);
		}
	}
	
	public void resetThroughputIndicator() {
		Set edgeSet = getEdges();
		for (Iterator it = edgeSet.iterator(); it.hasNext();) {
			MonitorEdge mEdge = (MonitorEdge) it.next();
			mEdge.resetThroughputIndicator();
		}
	}

	public void useNodeLabelType(MonitorVertex.LabelType type) {
		lockAllVertices();
		for (Object v : getVertices()) {
			if (v instanceof MonitorVertex)
				((MonitorVertex)v).useLabel(type);
		}
		unlockAllVertices();
	}
}

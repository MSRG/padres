package ca.utoronto.msrg.padres.tools.padresmonitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import ca.utoronto.msrg.padres.tools.padresmonitor.gui.BrokerVertex;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.ClientVertex;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorEdge;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorGraph;
import ca.utoronto.msrg.padres.tools.padresmonitor.gui.MonitorVertex;
import ca.utoronto.msrg.padres.tools.padresmonitor.resources.MonitorResources;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeFontFunction;
import edu.uci.ics.jung.graph.decorators.ConstantVertexFontFunction;
import edu.uci.ics.jung.graph.decorators.EdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeStringer;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.decorators.VertexStrokeFunction;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.LayoutMutable;
import edu.uci.ics.jung.visualization.PickSupport;
import edu.uci.ics.jung.visualization.PickedInfo;
import edu.uci.ics.jung.visualization.PickedState;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.ShapePickSupport;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;

public class JungOverlayUI extends OverlayUI {

	private static final long serialVersionUID = -3499997739869595927L;

	public static final boolean EDGE_INACTIVE = false;

	public static final boolean EDGE_ACTIVE = true;

	private MonitorFrame m_MonitorFrame;

	// private ClientMonitorCommandManager m_CommManager;

	boolean firstBrokerAdded = false;

	MonitorGraph m_graph;

	Map<String, BrokerVertex> vertexMap;

	Map<String, BrokerUI> brokerMap;

	// Map clientMap;
	Map<String, ClientVertex> clientVertexMap;

	// Map edgeMap;
	// Layout layout;
	VisualizationViewer vv;

	PluggableRenderer pr;

	VertexColour vcf;

	EdgePaint ep;

	EdgeWeightStrokeFunction ewsf;

	EdgeStringer es;

	BrokerVertex currentSelection;

	VertexStrokeHighlight vsh;

	VertexStringerFunction vsf;

	DefaultModalGraphMouse gm;

	protected boolean edgeThroughputIndicatorIsActive;

	public JungOverlayUI(MonitorFrame owner, ClientMonitorCommandManager comm) {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 1.0;
		constraints.weighty = 1.0;
		constraints.fill = GridBagConstraints.BOTH;

		m_MonitorFrame = owner;
		// m_CommManager = comm;

		currentSelection = null;
		m_graph = new MonitorGraph();
		vertexMap = new HashMap<String, BrokerVertex>();
		brokerMap = new HashMap<String, BrokerUI>();
		// clientMap = new HashMap();
		clientVertexMap = new HashMap<String, ClientVertex>();
		pr = new PluggableRenderer();
		// layout = new FRLayout(m_graph);

		es = new EdgeStringerFunction();
		vsf = new VertexStringerFunction();
		gm = new DefaultModalGraphMouse();

		ewsf = new EdgeWeightStrokeFunction();
		// vv = new VisualizationViewer(layout, pr);
		vv = new VisualizationViewer(m_graph.getLayout(), pr);
		vv.setBackground(Color.white);
		vv.setPreferredSize(new Dimension(640, 480));
		vv.setSize(vv.getPreferredSize());
		m_graph.getLayout().initialize(vv.getPreferredSize());
		// layout.initialize(vv.getPreferredSize());
		GraphZoomScrollPane scrollPane = new GraphZoomScrollPane(vv);
		vv.setPickSupport(new ShapePickSupport());
		PickedState ps = vv.getPickedState();
		vv.setGraphMouse(gm);
		gm.add(new PopupGraphMousePlugin());
		gm.setMode(ModalGraphMouse.Mode.PICKING);

		vcf = new VertexColour(ps);
		ep = new EdgePaint(ps);
		vsh = new VertexStrokeHighlight(ps);
		pr.setVertexPaintFunction(vcf);
		pr.setEdgeStrokeFunction(ewsf);
		pr.setEdgeStringer(es);
		pr.setVertexStrokeFunction(vsh);
		pr.setEdgePaintFunction(ep);
		pr.setVertexStringer(vsf);
		pr.setEdgeFontFunction(new ConstantEdgeFontFunction(new Font(
				MonitorResources.EDGE_LABEL_FONT_NAME, MonitorResources.EDGE_LABEL_FONT_STYLE,
				MonitorResources.EDGE_LABEL_FONT_SIZE)));
		pr.setVertexFontFunction(new ConstantVertexFontFunction(new Font(
				MonitorResources.VERTEX_LABEL_FONT_NAME, MonitorResources.VERTEX_LABEL_FONT_STYLE,
				MonitorResources.VERTEX_LABEL_FONT_SIZE)));

		add(scrollPane, constraints);

	}

	public void activateEdge(BrokerUI broker1, BrokerUI broker2) {
		BrokerVertex v1 = vertexMap.get(broker1.getBrokerID());
		BrokerVertex v2 = vertexMap.get(broker2.getBrokerID());
		MonitorEdge e = m_graph.getEdge(v1, v2);
		if (e != null) {
			e.incrementActivationCount();
			// do something with edge weights
			// vv.restart();
			vv.repaint();
		}
	}

	public void activeClientBrokerEdge(BrokerUI broker, String clientID) {
		BrokerVertex v1 = vertexMap.get(broker.getBrokerID());
		ClientVertex v2 = (ClientVertex) clientVertexMap.get(clientID);
		MonitorEdge e = m_graph.getEdge(v1, v2);
		if (e != null) {
			e.incrementActivationCount();
			// do something with edge weights
			// vv.restart();
			vv.repaint();
		}

	}

	public void activeteBroker(BrokerUI broker) {
		BrokerVertex v = vertexMap.get(broker.getBrokerID());
		if (v != null) {
			v.setActive();
			vertexMap.put(broker.getBrokerID(), v);
		}
	}

	public boolean addBroker(BrokerUI broker) {
		m_graph.lockAllVertices();
		boolean exist = vertexMap.containsKey(broker.getBrokerID());
		if (!exist) {
			BrokerVertex newVertex = (BrokerVertex) m_graph.addVertex(new BrokerVertex(broker));
			vertexMap.put(broker.getBrokerID(), newVertex);
			brokerMap.put(broker.getBrokerID(), broker);
			Set<String> clientList = broker.getClientSet();
			connectClients(newVertex, clientList);
			// vv.restart();
			updateVVWithNewNodes();
		} else {
			BrokerUI existingBroker = brokerMap.get(broker.getBrokerID());
			BrokerVertex existingVertex = vertexMap.get(existingBroker.getBrokerID());
			BrokerVertex newVertex = existingVertex;
			if (!existingBroker.hasFullInfo() && broker.hasFullInfo()) {
				m_graph.removeVertex(existingVertex);
				vertexMap.remove(existingBroker.getBrokerID());
				brokerMap.remove(existingBroker.getBrokerID());
				newVertex = (BrokerVertex) m_graph.addVertex(new BrokerVertex(broker));
				vertexMap.put(broker.getBrokerID(), newVertex);
				brokerMap.put(broker.getBrokerID(), broker);
			}
			Set<String> clientList = broker.getClientSet();
			connectClients(newVertex, clientList);
			// vv.restart();
			updateVVWithNewNodes();
		}
		m_graph.unlockAllVertices();

		// for now always return true
		return true;
	}

	public void addNeighbour(BrokerUI broker1, BrokerUI broker2) {
		BrokerVertex v1 = vertexMap.get(broker1.getBrokerID());
		BrokerVertex v2 = vertexMap.get(broker2.getBrokerID());

		// WHAT IF v1 OR v2 AREN'T IN THE GRAPH?
		// can i assume this function will be called correctly?
		boolean exists = v1.isNeighborOf(v2);
		if (!exists) {
			broker1.addNeighbour(broker2.getBrokerID());
			broker2.addNeighbour(broker1.getBrokerID());
			m_graph.addEdge(new MonitorEdge(v1, v2, EDGE_INACTIVE));
			m_graph.lockAllVertices();
			if (!vv.isVisRunnerRunning()) {
				vv.init();
			}
			vv.repaint();
			m_graph.unlockAllVertices();
		}
	}

	// public void applyLayout() {
	// // layout = new FRLayout(m_graph);
	// //layout = new SpringLayout(m_graph);
	// // vv.getModel().setGraphLayout(layout);
	// vv.getModel().setGraphLayout(m_graph.applyLayout());
	// vv.restart();
	// }

	public void applyLayout(int algorithm) {
		vv.getModel().setGraphLayout(m_graph.applyLayout(algorithm));
		vv.restart();
	}

	public void clear() {
		m_graph.removeAllVertices(); // removes all edges and vertices from
		// graph
		vertexMap.clear();
		brokerMap.clear();
		// clientMap.clear();
		clientVertexMap.clear();
		vv.repaint();
	}

	public boolean containsBroker(BrokerUI broker) {
		// TODO Auto-generated method stub
		return false;
	}

	public void deactivateAllEdge() {
		Set allEdges = m_graph.getEdges();
		for (Iterator it = allEdges.iterator(); it.hasNext();) {
			MonitorEdge e = (MonitorEdge) it.next();
			e.resetActivationCount();
		}
		// vv.restart();
		vv.repaint();
	}

	public BrokerUI getSelectedBrokerUI() {
		// assume for now that only one vertex will be fixed - need to address this issue later
		if (currentSelection != null) {
			return brokerMap.get(currentSelection.getBrokerID());
		}

		return null;
	}

	public void handleFailureClassMsg(String detectorBrokerID, String failureBrokerID, String type) {
		BrokerVertex failedBrokerVertex = vertexMap.get(failureBrokerID);
		BrokerVertex detectingBrokerVertex = vertexMap.get(detectorBrokerID);
		if (failedBrokerVertex == null || detectingBrokerVertex == null) {
			return;
		}
		MonitorEdge failedEdge = m_graph.getEdge(failedBrokerVertex, detectingBrokerVertex);

		if ((failedEdge == null) || (failedBrokerVertex == null) || (detectingBrokerVertex == null)) {
			// LOG MESSAGE
			// System.out.println("Can't handle failure message - brokers don't exist in topology");
			return;
		}

		if (type.equals("FAILURE_DETECTED")) {
			failedBrokerVertex.setInFailureState();
			failedEdge.setInFailureState();
			vv.repaint();
		} else if (type.equals("FAILURE_CLEARED")) {
			failedBrokerVertex.clearFailureState();
			failedEdge.clearFailureState();
			vv.repaint();
		}

	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isNeighbour(BrokerUI broker1, BrokerUI broker2) {
		BrokerVertex v1 = vertexMap.get(broker1.getBrokerID());
		BrokerVertex v2 = vertexMap.get(broker2.getBrokerID());
		boolean isIncident = v1.isNeighborOf(v2);
		return isIncident;
	}

	public boolean removeBroker(BrokerUI broker) {

		return false;
	}

	public void removeNeighbour(BrokerUI broker1, BrokerUI broker2) {
		// TODO Auto-generated method stub

	}

	public void removeOldClient(BrokerUI broker, Set<String> clientSet) {
		boolean differenceExists = false;
		vertexMap.get(broker.getBrokerID());
		BrokerUI existingBroker = brokerMap.get(broker.getBrokerID());
		Set<String> oldClientSet = existingBroker.getClientSet();
		Set<ClientVertex> removeSet = new HashSet<ClientVertex>();
		for (String client : oldClientSet) {
			if (!clientSet.contains(client)) {
				removeSet.add(clientVertexMap.get(client.toString()));
				differenceExists = true;
			}
		}
		if (differenceExists) {
			m_graph.removeVertices(removeSet);
			vv.repaint();
		}
		brokerMap.put(broker.getBrokerID(), broker);

	}

	public void setBrokerStatus(BrokerUI broker, int status) {
		// TODO Auto-generated method stub

	}

	private void connectClients(BrokerVertex v, Set<String> clientList) {
		for (String client : clientList) {
			// does this client already exist in the topology?
			if (!clientVertexMap.containsKey(client.toString())) {
				ClientVertex v1 = (ClientVertex) m_graph.addVertex(new ClientVertex(client, v.getLabel(MonitorVertex.LabelType.LT_LONG)));
				// clientMap.put(client.toString(), client);
				clientVertexMap.put(client, v1);
				m_graph.addEdge(new MonitorEdge(v, v1, EDGE_INACTIVE));
			}
		}
	}

	/**
	 * TODO: remove later <code>
	private void removeClient(Object client) {
		 System.out.println("RemoveClient is being called");
	}
	</code>
	 */

	public VisualizationViewer getVV() {
		return this.vv;
	}

	private void updateVVWithNewNodes() {
		Layout layout = m_graph.getLayout();
		if (layout instanceof LayoutMutable) {
			LayoutMutable mutableLayout = (LayoutMutable) layout;
			mutableLayout.update();
			if (!vv.isVisRunnerRunning()) {
				vv.init();
			}
			vv.repaint();
		} else {
			vv.restart();
		}
	}

	private final class VertexColour implements VertexPaintFunction {

		PickedState pi;

		public VertexColour(PickedState pi) {
			this.pi = pi;
		}

		public Paint getDrawPaint(Vertex v) {
			// vertex outline colour (draw => outline)
			return Color.BLACK;
		}

		public Paint getFillPaint(Vertex v) {
			// vertex interior (fill) colour (fill => interior)
			MonitorVertex v1 = (MonitorVertex) v;

			if (v1.getType() == "CLIENT_VERTEX_TYPE") {
				return Color.BLUE;
			} else {
				if (this.pi.isPicked(v1)) {
					currentSelection = (BrokerVertex) v1;
				}
				if (v1.isInFailureState()) {
					return Color.RED;
				} else if (v1.isActive() == true) {
					return Color.CYAN;
				} else {
					return VertexPaintFunction.TRANSPARENT;
				}
			}
		}
	}

	public void hideAllEdgeMessages() {
		Set edgeSet = m_graph.getEdges();
		for (Iterator it = edgeSet.iterator(); it.hasNext();) {
			MonitorEdge mEdge = (MonitorEdge) it.next();
			mEdge.displayActivationCountMessage(false);
		}
		vv.repaint();
	}

	public void showAllEdgeMessages() {
		Set edgeSet = m_graph.getEdges();
		for (Iterator it = edgeSet.iterator(); it.hasNext();) {
			MonitorEdge mEdge = (MonitorEdge) it.next();
			mEdge.displayActivationCountMessage(true);
		}
		vv.repaint();

	}

	public void setEdgeThroughputIndicator(boolean state) {
		m_graph.setEdgeThroughputIndicator(state);
	}

	public void resetEdgeThroughputIndicator() {
		m_graph.resetThroughputIndicator();
		vv.repaint();
	}

	public void useNodeLabelType(MonitorVertex.LabelType type) {
		m_graph.useNodeLabelType(type);
		vv.repaint();
	}
	
	private final static class VertexStrokeHighlight implements VertexStrokeFunction {

		protected Stroke heavy;

		protected Stroke medium;

		protected Stroke light;

		PickedInfo pi;

		boolean highlight;

		public VertexStrokeHighlight(PickedInfo pi) {
			this.pi = pi;
			heavy = new BasicStroke(5);
			light = new BasicStroke(1);
			highlight = true;
		}

		public void setHighlight(boolean highlight) {
			this.highlight = highlight;
		}

		public Stroke getStroke(Vertex v) {
			if (pi.isPicked(v)) {
				return heavy;
			} else {
				return light;
			}
		}

	}

	private final static class EdgeWeightStrokeFunction implements EdgeStrokeFunction {

		public Stroke weightedStroke;

		public EdgeWeightStrokeFunction() {
			weightedStroke = new BasicStroke(1);
		}

		public Stroke getStroke(Edge e) {
			MonitorEdge e1 = (MonitorEdge) e;
			if (e1.isInFailureState()) {
				return PluggableRenderer.DOTTED;
			} else {
				return e1.getStroke();
			}
		}

	}

	private final static class EdgeStringerFunction implements EdgeStringer {

		public String getLabel(ArchetypeEdge e) {
			MonitorEdge e1 = (MonitorEdge) e;
			if (e1.activationCountIsDisplayed()) {
				String text = e1.getActivationCountMessage();
				String formattedText = wrapHTMLColourTags(text,
						MonitorResources.EDGE_LABEL_FONT_COLOUR);
				// return e1.getActivationCountMessage();
				return formattedText.toString();
			} else {
				return "";
			}
		}

		private String wrapHTMLColourTags(String text, String colour) {
			StringBuffer formattedMessage = new StringBuffer();
			formattedMessage.append("<html><font color=");
			formattedMessage.append(colour.toString());
			formattedMessage.append(">");
			formattedMessage.append(text);
			formattedMessage.append("</font></html>");
			return formattedMessage.toString();
		}

	}

	private final static class EdgePaint implements EdgePaintFunction {

		PickedInfo pi;

		public EdgePaint(PickedInfo pi) {
			this.pi = pi;
		}

		public Paint getDrawPaint(Edge e) {
			MonitorEdge e1 = (MonitorEdge) e;
			if (e1.isInFailureState()) {
				return Color.RED;
			} else {
				if (pi.isPicked(e)) {
					return Color.GREEN;
				} else {
					return Color.BLACK;
				}
				// return Color.BLACK;
			}
		}

		public Paint getFillPaint(Edge e) {
			// if (pi.isPicked(e)) {
			// return Color.RED;
			// } else {
			// return Color.BLACK;
			// }
			return null;
		}

	}

	private final static class VertexStringerFunction implements VertexStringer {

		public String getLabel(ArchetypeVertex v) {
			MonitorVertex v1 = (MonitorVertex) v;
			String text = v1.getLabel();
			String formattedText = wrapHTMLColourTags(text,
					MonitorResources.VERTEX_LABEL_FONT_COLOUR);
			return formattedText;
		}

		private String wrapHTMLColourTags(String text, String colour) {
			StringBuffer formattedMessage = new StringBuffer();
			formattedMessage.append("<html><font color=");
			formattedMessage.append(colour.toString());
			formattedMessage.append(">");
			formattedMessage.append(text);
			formattedMessage.append("</font></html>");
			return formattedMessage.toString();
		}

	}

	private class PopupGraphMousePlugin extends AbstractPopupGraphMousePlugin implements
			MouseListener {

		public PopupGraphMousePlugin() {
			super(MouseEvent.BUTTON3_MASK);
		}

		protected void handlePopup(MouseEvent me) {
			final VisualizationViewer vv = (VisualizationViewer) me.getSource();
			Point2D p = vv.inverseViewTransform(me.getPoint());
			PickSupport pickSupport = vv.getPickSupport();
			if (pickSupport != null) {
				Vertex v = pickSupport.getVertex(p.getX(), p.getY());
				Edge e = pickSupport.getEdge(p.getX(), p.getY());
				if ((v != null) && (v instanceof BrokerVertex)) {
					JPopupMenu popup = new JPopupMenu();
					JMenuItem m_SetAdvMenuItem = new JMenuItem(MonitorResources.M_SET_ADV);
					m_SetAdvMenuItem.addActionListener(m_MonitorFrame);
					// removing advertisement and subscription window access
					// from right-click menu
					// popup.add(m_SetAdvMenuItem);
					JMenuItem m_SetSubMenuItem = new JMenuItem(MonitorResources.M_SET_SUB);
					m_SetSubMenuItem.addActionListener(m_MonitorFrame);
					// popup.add(m_SetSubMenuItem);
					JMenuItem m_centerVertex = new JMenuItem("Center Vertex");
					m_centerVertex.setAction(new CenterAction(m_graph.getLayout().getLocation(v),
							"Center Vertex"));
					popup.add(m_centerVertex);
					popup.show(vv, me.getX(), me.getY());
					popup.pack();
				} else if ((e != null) && (e instanceof MonitorEdge)) {
					MonitorEdge mEdge = (MonitorEdge) e;
					JPopupMenu popup = new JPopupMenu();
					boolean activationCountDisplayStatus = mEdge.activationCountIsDisplayed();
					String activationCountToggleMessage;
					if (activationCountDisplayStatus) {
						activationCountToggleMessage = "Hide Message Counter";
					} else {
						activationCountToggleMessage = "Show Message Counter";
					}
					JMenuItem m_activationCountMenuItem = new JMenuItem(
							activationCountToggleMessage);
					m_activationCountMenuItem.setAction(new ToggleEdgeActivationCountMessageAction(
							mEdge, activationCountToggleMessage));
					popup.add(m_activationCountMenuItem);
					popup.show(vv, me.getX(), me.getY());
					popup.pack();
				}
			}
		}

	}

	/**
	 * TODO: remove later <code>
	private final static class NamePredicate implements Predicate {
		String filterString;

		public NamePredicate(String name) {
			this.filterString = name;
		}

		public boolean evaluate(Object arg0) {
			MonitorVertex v = (MonitorVertex) arg0;
			String vertexName = v.getLabel();
			if (vertexName.indexOf(filterString) >= 0) {
				return true;
			}
			return false;
		}
	}
	</code>
	 * 
	 */

	public class ToggleEdgeActivationCountMessageAction extends AbstractAction {

		private static final long serialVersionUID = 6866256676785621274L;

		MonitorEdge mEdge;

		public ToggleEdgeActivationCountMessageAction(MonitorEdge mEdge, String name) {
			super(name);
			this.mEdge = mEdge;
		}

		public void actionPerformed(ActionEvent e) {
			mEdge.toggleActivationCountMessage();
		}
	}

	public class CenterAction extends AbstractAction {

		private static final long serialVersionUID = 1056926661591115011L;

		Thread animationThread;

		final Point2D center;

		public CenterAction(Point2D center, String name) {
			super(name);
			this.center = center;
		}

		public void actionPerformed(ActionEvent e) {
			moveCenter();
		}

		public void moveCenter() {
			if (animationThread != null) {
				animationThread.interrupt();
				animationThread = null;
			}
			animationThread = new Thread() {

				boolean keepGoing = true;

				static final int PIXEL_COUNT = 5;

				public void run() {
					Dimension d = vv.getSize();
					Point2D viewCenter = new Point2D.Float(d.width / 2, d.height / 2);
					viewCenter = vv.inverseTransform(viewCenter);
					double xdist = viewCenter.getX() - center.getX();
					double ydist = viewCenter.getY() - center.getY();
					// System.out.println("getx: " + viewCenter.getX());
					// System.out.println("gety: " + viewCenter.getY());
					// System.out.println("vvdimension: " + d.toString());
					double hdistance = Math.sqrt(xdist * xdist + ydist * ydist);
					if (hdistance > 200) {
						hdistance = 200.0;
					}
					int intervals = (int) (hdistance / PIXEL_COUNT);
					double dx = xdist / intervals;
					double dy = ydist / intervals;
					for (int i = 0; i < intervals && keepGoing; i++) {
						vv.getViewTransformer().translate(dx, dy);
						vv.repaint();
						try {
							sleep(10);
						} catch (InterruptedException e) {
						}
					}
					vv.repaint();
				}

				public void interrupt() {
					keepGoing = false;
					super.interrupt();
				}
			};
			animationThread.start();
		}

	}
}

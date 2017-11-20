package util;



import java.awt.BorderLayout;

import javax.swing.JFrame;

import com.mxgraph.layout.mxFastOrganicLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxMorphing;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

public class GraphRenderer {

	private Grafo g;
	private int[] coloreo;

	public static void main(String[] args) {
		
		Grafo g = GraphUtils.loadFromTxt("./rand/G_15_0.2");
		new GraphRenderer(g);
	}
	// Constructor
	public GraphRenderer(Grafo g) {
		this.g = g;
		mostrarSolucion();
		// actualizarSolucion();
	}

	// Constructor
	public GraphRenderer(Grafo g, int[] coloreo) {
		this.g = g;
		this.coloreo = coloreo;
		mostrarSolucion();
		// actualizarSolucion();
	}

	// Grafo asociado a la visualizaci�n
	private final mxGraph graph = new mxGraph();
	private mxGraphComponent graphComponent;

	// Vertices
	private static Object[] vertices;

	// Muestra la soluci�n por primera vez
	private void mostrarSolucion() {
		JFrame f = new JFrame();
		f.setSize(500, 500);
		f.setLocation(300, 200);

		graphComponent = new mxGraphComponent(graph);
		f.getContentPane().add(BorderLayout.CENTER, graphComponent);
		f.setVisible(true);

		graph.setCellsResizable(false);
		graph.setCellsDisconnectable(false);
		graph.setAllowDanglingEdges(false);

		Object parent = graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try {
			
			vertices = new Object[g.getVertices()];

			
			for (int v = 0; v < g.getVertices(); v++)
				vertices[v] = graph.insertVertex(parent, null, v + (coloreo == null ? "" : " = " + coloreo[v]), 100, 100, 30, 30);

			for (int v = 0; v < g.getVertices(); v++)
				for (int w = v + 1; w < g.getVertices(); w++)
					if (g.isArista(v, w))
							graph.insertEdge(parent, null, "", vertices[v],vertices[w], "startArrow=none;endArrow=none");

		} finally {
			graph.getModel().endUpdate();
		}

		// define layout
		mxIGraphLayout layout = new mxFastOrganicLayout(graph);

		// layout using morphing
		graph.getModel().beginUpdate();
		try {
			layout.execute(graph.getDefaultParent());
		} finally {
			mxMorphing morph = new mxMorphing(graphComponent, 20, 1.2, 20);
			morph.addListener(mxEvent.DONE, new mxIEventListener() {
				@Override
				public void invoke(Object arg0, mxEventObject arg1) {
					graph.getModel().endUpdate();
				}
			});

			morph.startAnimation();
		}
	}
}

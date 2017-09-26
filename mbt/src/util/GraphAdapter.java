package util;

import java.util.List;

import org.jgrapht.Graphs;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import util.Grafo.Builder;

public class GraphAdapter {

	/**
	 * Takes a graph in JGraphT format and converts it into a list of adjacencies.
	 * 
	 * @param graph
	 * @return
	 */
	public static Grafo convert(UndirectedGraph<Integer, DefaultEdge> graph) {

		int n = graph.vertexSet().size();

		Builder b = new Grafo.Builder(n);
		for (int j = 0; j < n; j++) {

			List<Integer> neighbours = Graphs.neighborListOf(graph, j);

			for (Integer v : neighbours)
				b.setArista(j, v);
		}

		return b.build();
	}

	/**
	 * Takes a graph in JGraphT format and converts it into a list of adjacencies.
	 * 
	 * @param graph
	 * @return
	 */
	public static UndirectedGraph<Integer, DefaultEdge> convert(Grafo g) {

		UndirectedGraph<Integer, DefaultEdge> graph = new SimpleGraph<Integer, DefaultEdge>(DefaultEdge.class);

		int n = g.getVertices();
		for (int j = 0; j < n; j++)
			graph.addVertex(j);

		for (int j = 0; j < n; j++)
			for (int k = j + 1; k < n; k++)
				if (g.isArista(j, k))
					graph.addEdge(j, k);

		return graph;
	}

}
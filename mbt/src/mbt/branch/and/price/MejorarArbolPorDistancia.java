package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;

/***
 * Implementación de solución exacta para el problema de pricing para un V0 de
 * varios vértices.
 */
public final class MejorarArbolPorDistancia {

	/** Mantenemos acá las variables de la función objetivo */
	double[] duals;

	private DataModel dataModel;

	/*** El árbol a mejorar */
	private Arbol T;

	double precision;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public MejorarArbolPorDistancia(DataModel dataModel, double[] duals, double precision, Arbol T) {

		this.dataModel = dataModel;
		this.duals = duals;
		this.precision = precision;
		this.T = T;
	}


	private void updateWeights(SimpleDirectedWeightedGraph<Integer, DefaultEdge> grafo, double coeficienteDeV0) {

		// el mínimo peso que me aparece (recordar que son negativos)
		double minWeight = Integer.MAX_VALUE;
		for (int i = dataModel.getV0().size(); i < duals.length; i++)
			if (grafo.containsVertex(i - dataModel.getV0().size()))
				minWeight = Math.min(minWeight, duals[i]);

		// una arista uv tiene peso igual al coeficiente de V0
		// más el peso de v más el valor absoluto del mínimo peso de antes
		// esto es para que todos los vértices tengan peso positivo o 0.
		for (DefaultEdge e : grafo.edgeSet()) {
			int v = grafo.getEdgeTarget(e);
			grafo.setEdgeWeight(e, coeficienteDeV0 + duals[dataModel.getV0().size() + v] + Math.abs(minWeight));
		}
	}

	/**
	 * Método principal que resuelve el problema de pricing.
	 * 
	 * @return List of columns (independent sets) with negative reduced cost.
	 * @throws TimeLimitExceededException
	 *             TimeLimitExceededException
	 */
	public Pair<Arbol, Double> mejorarArbol() {

		int i = 0;

		int v0 = T.getRoot();
		
		Arbol nuevo = T.clonar();		

		double coeficienteDeV0 = duals[i++];

		Set<Integer> noIncluidos = new HashSet<Integer>(this.dataModel.getV0());
		noIncluidos.remove(v0);
		SimpleDirectedWeightedGraph<Integer, DefaultEdge> grafo = dataModel.subgrafoSinVertices(noIncluidos);
		updateWeights(grafo, coeficienteDeV0);		

		DijkstraShortestPath<Integer, DefaultEdge> dijkstra = new DijkstraShortestPath<Integer, DefaultEdge>(grafo);

		ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultEdge> caminos = dijkstra.getPaths(v0);

		Set<GraphPath<Integer, DefaultEdge>> caminoSet = new HashSet<GraphPath<Integer, DefaultEdge>>();

		for (int v : grafo.vertexSet()) {
			GraphPath<Integer, DefaultEdge> camino = caminos.getPath(v);
			if (camino != null)
				caminoSet.add(camino);
		}

		boolean termine = false;
		double fObjOriginal = T.valorFuncionObjetivo(coeficienteDeV0, duals, dataModel);
		
		double mejorFuncionObjetivo = fObjOriginal;
		
		while (!termine) {
			List<Integer> mejorCamino = null;
			// Busco el mejor camino para agregar (o el menos peor)
			for (GraphPath<Integer, DefaultEdge> camino : caminoSet) {

				List<Integer> vertices = camino.getVertexList();

				List<Integer> verticesAAgregar = new ArrayList<Integer>();
				for (int j = vertices.size() - 1; j >= 0; j--) {
					int w = vertices.get(Integer.valueOf(j));
					verticesAAgregar.add(w);
					if (nuevo.contains(w))
						break;
				}

				if (verticesAAgregar.size() <= 1)
					continue;

				nuevo.addPath(verticesAAgregar);

				double fObj = nuevo.valorFuncionObjetivo(coeficienteDeV0, duals, dataModel);

				nuevo.removePath(verticesAAgregar);

				if (fObj < mejorFuncionObjetivo) {
					mejorFuncionObjetivo = fObj;
					mejorCamino = verticesAAgregar;
				}
			}

			if (mejorCamino != null) {

				nuevo.addPath(mejorCamino);				

			} else
				termine = true;
		}
		
		return new Pair<Arbol, Double>(nuevo,nuevo.valorFuncionObjetivo(coeficienteDeV0, duals, dataModel));
	}

}

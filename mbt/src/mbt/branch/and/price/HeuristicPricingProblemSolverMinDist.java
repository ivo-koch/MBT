package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

/***
 * Implementación de solución exacta para el problema de pricing para un V0 de
 * varios vértices.
 */
public final class HeuristicPricingProblemSolverMinDist
		extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	/** Mantenemos acá las variables de la función objetivo */
	double[] duals;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public HeuristicPricingProblemSolverMinDist(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "HeuristicPricingProblemSolverMinDist";
	}

	/***
	 * Devuelve el valor de la funcion objetivo para el arbol T y el coeficiente
	 * para t dado.
	 * 
	 * @param T
	 * @param coefT
	 * @return
	 */
	private double valorFuncionObjetivo(Arbol T, double coefT) {

		double sumaVertices = duals[dataModel.getV0().size() + T.getRoot()];

		for (int v : T.getInternalNodes())
			sumaVertices += duals[dataModel.getV0().size() + v];

		double fObj = coefT * (T.getCosto() + dataModel.getOffset()[T.getRoot()]) + sumaVertices;

		return fObj;
	}

	private void updateWeights(SimpleDirectedWeightedGraph<Integer, DefaultEdge> grafo , double coeficienteDeV0) {

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

	private void addPath(Arbol T, List<Integer> camino) {
		int padre = camino.get(camino.size() - 1);
		for (int j = camino.size() - 2; j >= 0; j--) {
			int v = camino.get(j);
			T.addVertex(v, padre);
			padre = v;
		}
	}

	private void removePath(Arbol T, List<Integer> camino) {
		for (int j = 0; j < camino.size() - 1; j++)
			T.removeVertex(camino.get(j));
	}

	private SimpleDirectedWeightedGraph<Integer, DefaultEdge> subgrafoSinVertices(
			SimpleDirectedWeightedGraph<Integer, DefaultEdge> grafo, Set<Integer> noIncluidos) {

		SimpleDirectedWeightedGraph<Integer, DefaultEdge> subgrafo = new SimpleDirectedWeightedGraph<Integer, DefaultEdge>(
				DefaultEdge.class);

		for (Integer v : grafo.vertexSet())
			if (!noIncluidos.contains(v))
				subgrafo.addVertex(v);

		for (DefaultEdge e : grafo.edgeSet()) {
			int u = grafo.getEdgeSource(e);
			int v = grafo.getEdgeTarget(e);

			if (!noIncluidos.contains(u) && !noIncluidos.contains(v))
				subgrafo.addEdge(u, v);
		}

		return subgrafo;
	}

	/**
	 * Método principal que resuelve el problema de pricing.
	 * 
	 * @return List of columns (independent sets) with negative reduced cost.
	 * @throws TimeLimitExceededException
	 *             TimeLimitExceededException
	 */
	@Override
	public List<MBTColumn> generateNewColumns() throws TimeLimitExceededException {
		List<MBTColumn> newPatterns = new ArrayList<>();

		logger.debug("Resolviendo heuristica distancia...");
		Estadisticas.llamadasDist++;
		int i = 0;

		for (int v0 : this.dataModel.getV0()) {

			int n = this.dataModel.getGrafo().getVertices();

			double coeficienteDeV0 = duals[i++];
			
			Set<Integer> noIncluidos  = new HashSet<Integer>(this.dataModel.getV0());
			noIncluidos.remove(v0);
			SimpleDirectedWeightedGraph<Integer, DefaultEdge> grafo = subgrafoSinVertices(dataModel.getGrafoPesado(), noIncluidos);
			updateWeights(grafo, coeficienteDeV0);
//
//			for (int z = 0; z + dataModel.getV0().size() < duals.length; z++)
//				logger.debug("Dual v" + z + " " + duals[dataModel.getV0().size() + z]);
//
//			for (DefaultEdge e : grafo.edgeSet())
//				logger.debug(
//						"Peso " + grafo.getEdgeSource(e) + "," + grafo.getEdgeTarget(e) + ":" + grafo.getEdgeWeight(e));

			Set<Arbol> candidatos = new HashSet<Arbol>();

			DijkstraShortestPath<Integer, DefaultEdge> dijkstra = new DijkstraShortestPath<Integer, DefaultEdge>(grafo);

			ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultEdge> caminos = dijkstra.getPaths(v0);

			Set<GraphPath<Integer, DefaultEdge>> caminoSet = new HashSet<GraphPath<Integer, DefaultEdge>>();

			for (int v : grafo.vertexSet()) {
				GraphPath<Integer, DefaultEdge> camino = caminos.getPath(v);
				if (camino != null)
					caminoSet.add(camino);
			}

			Arbol T = new Arbol(n, v0);

			// double sumaVertices = duals[dataModel.getV0().size() + v0];
			boolean termine = false;
			double mejorFuncionObjetivo = Double.MAX_VALUE;

			while (!termine) {
				List<Integer> mejorCamino = null;
				// Busco el mejor camino para agregar (o el menos peor)
				for (GraphPath<Integer, DefaultEdge> camino : caminoSet) {

					List<Integer> vertices = camino.getVertexList();

					List<Integer> verticesAAgregar = new ArrayList<Integer>();
					for (int j = vertices.size() - 1; j >= 0; j--) {
						int w = vertices.get(Integer.valueOf(j));
						verticesAAgregar.add(w);
						if (T.contains(w))
							break;
					}

					if (verticesAAgregar.size() <= 1)
						continue;

					addPath(T, verticesAAgregar);

					double fObj = valorFuncionObjetivo(T, coeficienteDeV0);

					removePath(T, verticesAAgregar);

					if (fObj < mejorFuncionObjetivo) {
						mejorFuncionObjetivo = fObj;
						mejorCamino = verticesAAgregar;
					}
				}

				if (mejorCamino != null) {

					addPath(T, mejorCamino);
					Arbol nuevo = T.clonar();

					double T_fobj = valorFuncionObjetivo(T, coeficienteDeV0);

					if (T_fobj < -config.PRECISION)
						candidatos.add(nuevo);

				} else {
					termine = true;
				}
			}

			if (candidatos.size() > 0)
				logger.debug("Agregando candidatos... ");

			for (Arbol T_cand : candidatos) {
				// if (T_best_fobj < -config.PRECISION)
				newPatterns.add(new MBTColumn(pricingProblem, false, "HeuristicPricingProblemSolverMinDist", T_cand));
				Estadisticas.columnasDist++;
			}

		}
		if (!newPatterns.isEmpty())
			Estadisticas.llamadasExitosasDist++;
		
		return newPatterns;
	}

	/**
	 * Actualizamos la función objetivo del problema con la nueva solución dual que
	 * viene del master.
	 */
	@Override
	public void setObjective() {
		duals = pricingProblem.dualCosts;
	}

	/**
	 * Cerrar el problema de pricing.
	 */
	@Override
	public void close() {

	}

	/**
	 * Aplicamos esta decisión de branching al problema
	 * 
	 * 
	 * @param bd
	 *            BranchingDecision
	 */
	@Override
	public void branchingDecisionPerformed(BranchingDecision bd) {

	}

	/**
	 * Volvemos atrás la decisión de branchear (cuando hacemos el backtracking)
	 * 
	 * 
	 * @param bd
	 *            BranchingDecision
	 */
	@Override
	public void branchingDecisionReversed(BranchingDecision bd) {
	}

	class Nodo {

		public Nodo anterior;
		public int v;
		public int nivel;

		Nodo(Nodo anterior, int v, int nivel) {
			this.anterior = anterior;
			this.v = v;
			this.nivel = nivel;
		}
	}

}

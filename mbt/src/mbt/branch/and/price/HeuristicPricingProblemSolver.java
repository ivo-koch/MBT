package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import util.Grafo.AristaDirigida;

/***
 * Implementación de solución exacta para el problema de pricing para un V0 de
 * varios vértices.
 */
public final class HeuristicPricingProblemSolver
		extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	/** Mantenemos acá las variables de la función objetivo */
	double[] duals;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public HeuristicPricingProblemSolver(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "HeuristicPricingProblemSolver";
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

		double fObj = coefT * (T.calcularCosto() + dataModel.getOffset()[T.getRoot()]) + sumaVertices;

		return fObj;
	}

	/**
	 * Actualiza el conjunto de aristas incidentes al árbol, realizando las
	 * siguientes acciones: a.- Agrega a aristasIncidentesAT todas las aristas
	 * incidentes a alguno de los vértices del camino b.- Elimina de
	 * aristasIncidentesAT todas las aristas incidentes a alguno de los vértices del
	 * camino PRECONDICION: camino debe ser un camino contenido en T
	 * 
	 * @param aristasIncidentesAT
	 * @param camino
	 * @param T
	 */
	private void actualizarAristas(Set<AristaDirigida> aristasIncidentesAT, List<Integer> camino, Arbol T) {

		// actualizamos la lista de aristas antes de iterar de nuevo, sino se calienta.
		// y agrego sus aristas incidentes para procesar.

		for (int w : camino) {

			for (AristaDirigida b : this.dataModel.getGrafo().getAristasIncidentes(w))
				if (!T.contains(b.getV2()) && !dataModel.getV0().contains(b.getV2()))
					aristasIncidentesAT.add(b);
			// y boleteamos las aristas que tengan a w como destino
			Set<AristaDirigida> aEliminar = new HashSet<AristaDirigida>();
			for (AristaDirigida c : aristasIncidentesAT)
				if (c.getV2() == w && !(T.parent(w) == c.getV1()))
					aEliminar.add(c);

			aristasIncidentesAT.removeAll(aEliminar);
		}
	}

	private Pair<Double, List<Integer>> findPath(Arbol T, int w, double coeficienteDeV0, double mejorFuncionObjetivo,
			int lookaheadSteps) {

		// lista en bfs de nodos. Esto representa el recorrido en bfs a partir de w,
		// para vértices no incluidos en T.
		// guardamos nodos y no vértices para tener el nivel y una referencia al padre
		// en el recorrido

		List<Nodo> bfsList = new LinkedList<Nodo>();
		bfsList.add(new Nodo(null, w, 0));

		LinkedList<Integer> caminoAExaminar = new LinkedList<Integer>();

		while (!bfsList.isEmpty()) {

			Nodo nodo = bfsList.remove(0);

			// reconstruimos el camino hasta la raíz.
			// y lo guardamos en caminoAExaminar.
			caminoAExaminar.clear();
			Nodo actual = nodo;
			while (actual != null) {
				caminoAExaminar.addFirst(actual.v);
				actual = actual.anterior;
			}
			// agregamos esos nodos al árbol
			for (int i = 1; i < caminoAExaminar.size(); i++) {
				T.addVertex(caminoAExaminar.get(i), caminoAExaminar.get(i - 1));
			}

			// evaluamos la función objetivo con esto
			double fObj = valorFuncionObjetivo(T, coeficienteDeV0);

			// el camino mejora la funcion objetivo, encontramos.
			if (fObj < mejorFuncionObjetivo)
				return new Pair<Double, List<Integer>>(fObj, caminoAExaminar);

			// si estamos en este punto, no sirvió ese camino.

			// eliminamos lo que agregamos, salvo a w
			for (int i = caminoAExaminar.size() - 1; i > 0; i--) {
				T.removeVertex(caminoAExaminar.get(i));
			}

			// y agregamos los vecinos del nodo a la lista, si corresponde por el nivel

			if (nodo.nivel < lookaheadSteps) {
				for (int z : dataModel.getGrafo().getVecinos(nodo.v))
					if (!T.contains(z) && !caminoAExaminar.contains(z))
						// notar que puedo agregar repetidos, si son de distinto padre. Son 2 casos
						// distintos que hay que analizar.
						bfsList.add(new Nodo(nodo, z, nodo.nivel + 1));
			}

		}

		return null;
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

		logger.debug("Resolviendo heuristica...");
		int i = 0;

		for (int v0 : this.dataModel.getV0()) {

			int n = this.dataModel.getGrafo().getVertices();

			double coeficienteDeV0 = duals[i++];

			Arbol T = new Arbol(n, v0);

			Set<AristaDirigida> aristasIncidentesAT = new HashSet<AristaDirigida>();
			aristasIncidentesAT.addAll(this.dataModel.getGrafo().getAristasIncidentes(v0));

			// double sumaVertices = duals[dataModel.getV0().size() + v0];
			boolean termine = false;
			double mejorFuncionObjetivo = 0;

			while (!termine) {				
				for (AristaDirigida a : aristasIncidentesAT) {
					// calculamos cuánto mejora la función objetivo.

					if (dataModel.getV0().contains(a.getV2()))
						continue;

					Pair<Double, List<Integer>> mejora = findPath(T, a.getV1(), coeficienteDeV0, mejorFuncionObjetivo,
							5);

					// la función desde mi arista mejora.
					if (mejora != null) {
						mejorFuncionObjetivo = mejora.getValue0();
						actualizarAristas(aristasIncidentesAT, mejora.getValue1(), T);
						break;
					} else 
						termine = true;
				}
				
			}
			// T.setCosto(T.calcularCosto());
			if (mejorFuncionObjetivo < -config.PRECISION)
				newPatterns.add(new MBTColumn(pricingProblem, false, "HeuristicPricingProblemSolver", T));

		}
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

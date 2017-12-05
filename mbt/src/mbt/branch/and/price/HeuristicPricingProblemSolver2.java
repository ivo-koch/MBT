package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import util.Grafo.AristaDirigida;

/***
 * Implementación de solución exacta para el problema de pricing para un V0 de
 * varios vértices.
 */
public final class HeuristicPricingProblemSolver2
		extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	/** Mantenemos acá las variables de la función objetivo */
	double[] duals;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public HeuristicPricingProblemSolver2(DataModel dataModel, MBTPricingProblem pricingProblem) {
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

		double fObj = coefT * (T.getCosto() + dataModel.getOffset()[T.getRoot()]) + sumaVertices;

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
//			Arbol T_best = T.clonar();
//			double T_best_fobj = valorFuncionObjetivo(T, coeficienteDeV0);
			
			HashMap<HashSet<Integer>, Arbol> candidatos = new HashMap<HashSet<Integer>, Arbol>();
			
			Set<AristaDirigida> aristasIncidentesAT = new HashSet<AristaDirigida>();
			aristasIncidentesAT.addAll(this.dataModel.getGrafo().getAristasIncidentes(v0));

			// double sumaVertices = duals[dataModel.getV0().size() + v0];
			boolean termine = false;

			while (!termine) 
			{				
				// Busco la mejor arista para agregar (o la menos peor)
				double mejorFuncionObjetivo = Double.MAX_VALUE;
				AristaDirigida a_best = null;
				for (AristaDirigida a : aristasIncidentesAT) 
				{
					if (T.contains(a.getV2()) || dataModel.getV0().contains(a.getV2()))
						continue;

					T.addVertex(a.getV2(), a.getV1());
					double fObj = valorFuncionObjetivo(T, coeficienteDeV0);
					T.removeVertex(a.getV2());

					if (fObj < mejorFuncionObjetivo)
					{
						mejorFuncionObjetivo = fObj;
						a_best = a;
					}
				}

				if (a_best != null)
				{
					T.addVertex(a_best.getV2(), a_best.getV1());
					actualizarAristas(aristasIncidentesAT, Collections.singletonList(a_best.getV2()), T);
					
					double T_fobj = valorFuncionObjetivo(T, coeficienteDeV0);
//					if (T_fobj < T_best_fobj)
//					{
//						T_best = T.clonar();
//						T_best_fobj = T_fobj;
//					}

					if (T_fobj >= -config.PRECISION)
					{
						T_fobj = mejorarPorAristasComplemento(T, coeficienteDeV0);
					}

					
					if (T_fobj < -config.PRECISION)
					{
						HashSet<Integer> set = new HashSet<Integer>(T.getInternalNodes());
						set.add(T.getRoot());
						
						if (!candidatos.containsKey(set) || candidatos.get(set).getCosto() > T.getCosto())
							candidatos.put(set, T.clonar());
					}
				}
				else
				{
					termine = true;
				}
			}

			if (candidatos.size() > 0)
				logger.debug("Agregando candidatos... ");

			for (Arbol T_cand : candidatos.values())
			{
//				if (T_best_fobj < -config.PRECISION)
					newPatterns.add(new MBTColumn(pricingProblem, false, "HeuristicPricingProblemSolver", T_cand));
			}

		}
		return newPatterns;
	}

	/**
	 * Procedimiento de mejora de un arbolito. Para cada arista no usada, si los 
	 * extremos est�n en el �rbol, agrega la arista e intenta cortar el ciclo de 
	 * manera que mejore el T. Si no mejora, no la agrega. 
	 * @param clonar
	 * @param coeficienteDeV0 
	 * @return 
	 */
	private double mejorarPorAristasComplemento(Arbol T, double coeficienteDeV0) 
	{
		List<Integer> nodos = T.bfs(T.getRoot());
//		Collections.reverse(nodos);
		double fobj = valorFuncionObjetivo(T, coeficienteDeV0);
		
//		logger.debug("--> Arbol: " + T);
//		logger.debug("--> BFS: " + nodos);
		
		for (Integer u : nodos)
			for (Integer v : nodos) if (u < v)
			{
				if (!T.isArista(u,v) && dataModel.getGrafo().isArista(u, v))
				{
//					logger.debug(">>> Probando arista " + u + " -- " + v);
					double fobj1 = Double.MAX_VALUE;
					int padre = T.parent(u);					
					if (T.cambiarParent(u, v))
					{
//						logger.debug(">>>>> cambi� el padre de " + u + " a " + v);
						// Tengo el T1
						fobj1 = valorFuncionObjetivo(T, coeficienteDeV0);
						T.cambiarParent(u, padre);
					}

					padre = T.parent(v);
					double fobj2 = Double.MAX_VALUE;
					if (T.cambiarParent(v, u))
					{
//						logger.debug(">>>>> cambi� el padre de " + v + " a " + u);
						// Tengo el T2
						fobj2 = valorFuncionObjetivo(T, coeficienteDeV0);
						T.cambiarParent(v, padre);
					}
					
					if (Math.min(fobj1, fobj2) < fobj)
					{
						logger.debug("**************** Mejoramos de " + fobj + " a " + Math.min(fobj1, fobj2));
						if (fobj1 < fobj2)
						{
							fobj = fobj1;
							T.cambiarParent(u, v);
						}
						else
						{
							fobj = fobj2;
							T.cambiarParent(v, u);
						}
					}
				}
			}
		
		return fobj;
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

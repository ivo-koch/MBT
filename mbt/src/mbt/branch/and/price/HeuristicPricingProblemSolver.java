package mbt.branch.and.price;

import java.util.ArrayList;
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

		int i = 0;
		for (int v0 : this.dataModel.getV0()) {

			int n = this.dataModel.getGrafo().getVertices();

			double coeficienteDev0 = duals[i++];

			Arbol T = new Arbol(n, v0);

			Set<AristaDirigida> aristasIncidentesAT = new HashSet<AristaDirigida>();
			aristasIncidentesAT.addAll(this.dataModel.getGrafo().getAristasIncidentes(v0));

			double sumaVertices = duals[dataModel.getV0().size() + v0];
			boolean termine = false;
			double mejorFuncionObjetivo = Double.MAX_VALUE;
			while (!termine) {
				AristaDirigida candidata = null;
			
				for (AristaDirigida a : aristasIncidentesAT) {
					// calculamos cuánto mejora la función objetivo.

					// cuánto costaría agregar a w?
					int w = a.getV2();
					// lo agregamos tentativamente a w, para calcular el costo.
					T.addVertex(w, a.getV1());
					double fObj = coeficienteDev0 * T.calcularCosto() + duals[dataModel.getV0().size() + w] + sumaVertices;

					// encontramos una arista que mejora mi función objetivo
					if (fObj < mejorFuncionObjetivo) {
						candidata = a;
						mejorFuncionObjetivo = fObj;

						sumaVertices += duals[dataModel.getV0().size() + w];

						// actualizamos la lista de aristas antes de iterar de nuevo, sino se calienta.
						// y agrego sus aristas incidentes para procesar.
						for (AristaDirigida b : this.dataModel.getGrafo().getAristasIncidentes(w))
							if (!T.contains(b.getV2()))
								aristasIncidentesAT.add(b);
						// y boleteamos las aristas que tengan a w como destino
						Set<AristaDirigida> aEliminar = new HashSet<AristaDirigida>();
						for (AristaDirigida c : aristasIncidentesAT)
							if (c.getV2() == w)
								aEliminar.add(c);

						aristasIncidentesAT.removeAll(aEliminar);
						break;
					} else
						T.removeVertex(w);
				}

				if (candidata == null)
					termine = true;
			}
			//T.setCosto(T.calcularCosto());
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

}

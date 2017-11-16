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
public final class RoutingPricingSolver extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	private ExactPricingProblemSolverMultipleV0 exactSolver;
	private HeuristicPricingProblemSolver heuristicSolver;
	
	private double timeLimit;
	

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public RoutingPricingSolver(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "RoutingPricingSolver";

		this.exactSolver = new ExactPricingProblemSolverMultipleV0(dataModel, pricingProblem);
		this.heuristicSolver = new HeuristicPricingProblemSolver(dataModel, pricingProblem);
		//timeLimit = exactSolver.
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

		List<MBTColumn> columnasHeuristica = heuristicSolver.generateNewColumns();

		if (!columnasHeuristica.isEmpty()) {
			logger.debug("Heuristica encontro cols");
			return columnasHeuristica;
		}
		//exactSolver.setTimeLimit(1510783352367l);		
		logger.debug("Ejecución del exacto");
		newPatterns = exactSolver.generateNewColumns();
		
		return newPatterns;
	}

	/**
	 * Actualizamos la función objetivo del problema con la nueva solución dual que
	 * viene del master.
	 */
	@Override
	public void setObjective() {
		exactSolver.setObjective();
		heuristicSolver.setObjective();
	}

	/**
	 * Cerrar el problema de pricing.
	 */
	@Override
	public void close() {
		exactSolver.close();
		heuristicSolver.close();
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

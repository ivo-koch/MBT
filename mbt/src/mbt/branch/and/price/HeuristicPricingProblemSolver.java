package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;
import util.Grafo;
import util.Grafo.AristaDirigida;

/***
 * Implementación de solución exacta para el problema de pricing para un V0 de
 * varios vértices.
 */
public final class HeuristicPricingProblemSolver
		extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	/*** Instancia de cplex **/
	private IloCplex cplex;
	/** Funcion objetivo */
	private IloObjective obj;

	/*** Variables del modelo **/
	private IloNumVar[][][] x;
	private IloNumVar[] z;
	private IloNumVar[] t;

	/** Mantenemos acá las variables de la función objetivo */
	IloNumVar[] varsEnFobj;



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
		try {
			// Límite de tiempo.
			double timeRemaining = Math.max(1, (timeLimit - System.currentTimeMillis()) / 1000.0);
			cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining);			
					
			// Resolvemos el problema
			if (!cplex.solve() || cplex.getStatus() != IloCplex.Status.Optimal) {
				if (cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim) {
					throw new TimeLimitExceededException();
				} else if (cplex.getStatus() == IloCplex.Status.Infeasible) {

					pricingProblemInfeasible = true;
					this.objective = Double.MAX_VALUE;
					throw new RuntimeException("Pricing problem infeasible");
				} else {
					throw new RuntimeException("Pricing problem solve failed! Status: " + cplex.getStatus());
				}
			} else { // Encontramos un óptimo
				
				this.pricingProblemInfeasible = false;
				this.objective = cplex.getObjValue();

				// podemos agregar el resultado a la base?
				if (objective < -config.PRECISION) { //- config.PRECISION) {
					// SI
					// si es así, agregamos una nueva columna representada por ese árbol a la base
					double t_v0 = 0;
					int v0 = -1;
					for (int v : dataModel.getV0())
						if (cplex.getValue(z[v]) > 1 - config.PRECISION) {
							t_v0 = cplex.getValue(t[v]);
							v0 = v;
						}

					// Creamos el árbol.
					Arbol.Builder builder = new Arbol.Builder(dataModel.getGrafo().getVertices(), v0);

					// recorremos en bfs según las variables x_ijk, para armar el árbol.
					LinkedList<Integer> vertices = new LinkedList<Integer>();
					vertices.add(v0);

					while (!vertices.isEmpty()) {
						int v = vertices.poll();
						for (int w : dataModel.getGrafo().getVecinos(v))
							for (int k = 0; k < dataModel.getGrafo().getVecinos(v).size(); k++)
								if (cplex.getValue(x[v][w][k]) > 1 - config.PRECISION && !builder.contains(w)) {
									builder.addVertex(w, v);
									vertices.add(w);
								}
					}
					logger.debug("Obj: " + objective);					

					MBTColumn columna = new MBTColumn(pricingProblem, false, this.getName(),
							builder.buildArbol());
					newPatterns.add(columna);

				}
			}
		} catch (IloException e) {
			e.printStackTrace();
		}
		return newPatterns;
	}

	/**
	 * Actualizamos la función objetivo del problema con la nueva solución dual que
	 * viene del master.
	 */
	@Override
	public void setObjective() {

		try {

			// nos fijamos las variables que tienen que ir en la función objetivo.
			IloNumVar[] varsEnFobj = new IloNumVar[dataModel.getGrafo().getVertices() + dataModel.getV0().size()];

			int l = 0;
			for (int v0 : dataModel.getV0())
				varsEnFobj[l++] = t[v0];

			for (int i = 0; i < dataModel.getGrafo().getVertices(); i++)
				varsEnFobj[l + i] = z[i];

			// y le ponemos las variables duales como costos.
			obj.setExpr(cplex.scalProd(pricingProblem.dualCosts, varsEnFobj));
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Cerrar el problema de pricing.
	 */
	@Override
	public void close() {
		cplex.end();
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
		try {

			// modificamos el V0 y el offser según la decisión del branch.
			MBTBranchingDecision decision = (MBTBranchingDecision) bd;
			int origen = decision.getArista().getV1();
			int destino = decision.getArista().getV2();


		} catch (Exception e) {
			e.printStackTrace();
		}

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
		try {
			MBTBranchingDecision decision = (MBTBranchingDecision) bd;
			int origen = decision.getArista().getV1();
			int destino = decision.getArista().getV2();



		} catch (Exception e) {
			e.printStackTrace();
		}
	}




}

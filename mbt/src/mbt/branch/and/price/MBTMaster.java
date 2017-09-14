package mbt.branch.and.price;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import util.Grafo;

/**
 * Esta clase define el problema master.
 * 
 * */
public final class MBTMaster extends AbstractMaster<DataModel, Arbol, MBTPricingProblem, MBTMasterData> {

	private IloObjective obj; // Función objetivo.
	private IloRange[] costLessThanH; // Constraint
	private IloRange[] vertexBelongsToOneTree; // Constraint
	
	private IloNumVar h;



	public MBTMaster(DataModel dataModel, Set<Integer> V0, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem, OptimizationSense.MINIMIZE);		
		System.out.println("Master constructor. Columns: " + masterData.getNrColumns());
	}

	/**
	 * Crea el modelo del problema master.
	 * 
	 * @return Devuelve un MBTMasterData. Esto es un contenedor para información
	 *         del master.
	 * 
	 */
	@Override
	protected MBTMasterData buildModel() {
		IloCplex cplex = null;

		try {
			cplex = new IloCplex(); // Nueva instancia de cplex.
			cplex.setOut(null); // Deshabilitamos el output.
			// Setea el máx. número de threads.
			cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS);

			// Función objetivo
			obj = cplex.addMinimize();

			// tenemos que agregar la variable h así
			// esto registra la columna de la h con la función objetivo, con coeficiente 1
			IloColumn iloColumn = masterData.cplex.column(obj, 1);

			// Constraints para la primera desigualdad del master.
			costLessThanH = new IloRange[dataModel.V0.size()];
			for (int i = 0; i < dataModel.V0.size(); i++) {
				// El addRange este funciona como el rango de la desigualdad. En
				// este caso, entre -infinito y 0
				costLessThanH[i] = cplex.addRange(Double.MIN_VALUE, 0, "costLessThanH");
				
				//y registro la variable h para esa desigualdad.
				iloColumn = iloColumn.and(masterData.cplex.column(this.costLessThanH[i], -1.0));
			}

			// Creamos la variable h que representa esa columna.
			h = masterData.cplex.numVar(iloColumn, 0, dataModel.maxT, "h");
			masterData.cplex.add(h);

			//y ahora creamos la segunda desigualdad, vacía
			vertexBelongsToOneTree = new IloRange[dataModel.grafo.getVertices()];
			for (int i = 0; i < dataModel.grafo.getVertices(); i++)
				// el range va así porque es una igualdad.
				vertexBelongsToOneTree[i] = cplex.addRange(1, 1, "vertexBelongsToOneTree");

		} catch (IloException e) {
			e.printStackTrace();
		}

		Map<MBTPricingProblem, OrderedBiMap<Arbol, IloNumVar>> varMap = new LinkedHashMap<>();
		MBTPricingProblem pricingProblem = this.pricingProblems.get(0);
		varMap.put(pricingProblem, new OrderedBiMap<Arbol, IloNumVar>());

		return new MBTMasterData(cplex, varMap);
	}

	/**
	 * Resolvemos el master.
	 * 
	 * @param timeLimit
	 * 
	 * @return true si se resolvió el modelo.
	 * @throws TimeLimitExceededException
	 *             TimeLimitExceededException
	 */
	@Override
	protected boolean solveMasterProblem(long timeLimit) throws TimeLimitExceededException {
		try {
			// Time limit
			double timeRemaining = Math.max(1, (timeLimit - System.currentTimeMillis()) / 1000.0);
			masterData.cplex.setParam(IloCplex.DoubleParam.TiLim, timeRemaining);

			// Exportación del modelo.
			if (config.EXPORT_MODEL)
				masterData.cplex.exportModel(config.EXPORT_MASTER_DIR + "master_" + this.getIterationCount() + ".lp");

			// Resolvemos el modelo.
			if (!masterData.cplex.solve() || masterData.cplex.getStatus() != IloCplex.Status.Optimal) {
				if (masterData.cplex.getCplexStatus() == IloCplex.CplexStatus.AbortTimeLim)
					throw new TimeLimitExceededException();
				else
					throw new RuntimeException("Master problem solve failed! Status: " + masterData.cplex.getStatus());
			} else {
				masterData.objectiveValue = masterData.cplex.getObjValue();
			}
		} catch (IloException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Le pasamos las variables duales al problema de pricing.
	 * 
	 * @param pricingProblem
	 *            pricing problem
	 */
	@Override
	public void initializePricingProblem(MBTPricingProblem pricingProblem) {
		try {

			
			// Duales para la primera restricción.
			double[] dualValuesRest1 = masterData.cplex.getDuals(this.costLessThanH);
			// Duales para la segunda restricción.
			double[] dualValuesRest2 = masterData.cplex.getDuals(this.vertexBelongsToOneTree);

			// ponemos todo en el arreglo dual values.
			double[] dualValues = Arrays.copyOf(dualValuesRest1, dualValuesRest1.length + dualValuesRest2.length);
			System.arraycopy(dualValuesRest2, 0, dualValues, dualValuesRest1.length, dualValuesRest2.length);

			// se lo tenemos que pasar todo junto al problema de pricing.
			pricingProblem.initPricingProblem(dualValues);

			// TODO: poner tamaños en el pricing problem para chequear que no
			// haya una cagada.
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Agregamos una columna al problema master.
	 * 
	 * @param column
	 * 
	 */
	@Override
	public void addColumn(Arbol column) {
		try {
			// Registramos una columna (un nuevo árbol T) con coeficiente 0 en
			// la función objetivo
			IloColumn iloColumn = masterData.cplex.column(obj, 0);

			// Registramos la columna con los primeros constraints, si este T
			// comienza en un v0
			if (dataModel.V0.contains(column.root))
				iloColumn = iloColumn.and(masterData.cplex.column(this.costLessThanH[column.root], column.cost));

			// Registramos la columna con los primeros constraints, para los
			// vértices que conforman a T
			for (Integer vertex : column.vertices)
				iloColumn = iloColumn.and(masterData.cplex.column(this.vertexBelongsToOneTree[vertex], 1.0));

			// Creamos la variable
			IloNumVar var = masterData.cplex.numVar(iloColumn, 0, Double.MAX_VALUE, "T_" + masterData.getNrColumns());
			masterData.cplex.add(var);
			masterData.addColumn(column, var);
		} catch (IloException e) {
		}
	}

	/**
	 * Obtiene la solución del master.
	 * 
	 * @return Devuelve todas las columnas distintas de 0
	 */
	@Override
	public List<Arbol> getSolution() {
		List<Arbol> solution = new ArrayList<>();
		try {
			Arbol[] arbolesEnLaSolucion = masterData.getColumnsForPricingProblemAsList().toArray(new Arbol[masterData.getNrColumns()]);
			IloNumVar[] vars = masterData.getVarMap().getValuesAsArray(new IloNumVar[masterData.getNrColumns()]);
			double[] values = masterData.cplex.getValues(vars);
			for (int i = 0; i < arbolesEnLaSolucion.length; i++) {
				arbolesEnLaSolucion[i].value = values[i];
				if (values[i] >= config.PRECISION)
					solution.add(arbolesEnLaSolucion[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return solution;
	}

	/**
	 * Solución por consola.
	 */
	@Override
	public void printSolution() {
		List<Arbol> solution = this.getSolution();
		for (Arbol is : solution)
			System.out.println(is);
	}

	/**
	 * Cierra el problema master.
	 */
	@Override
	public void close() {
		masterData.cplex.end();
	}

	/**
	 * Listen to branching decisions
	 * 
	 * @param bd
	 *            Branching decision
	 */
	@Override
	public void branchingDecisionPerformed(BranchingDecision bd) {
		// For simplicity, we simply destroy the master problem and rebuild it.
		// Of course, something more sophisticated may be used which retains the
		// master problem.
		this.close(); // Close the old cplex model
		masterData = this.buildModel(); // Create a new model without any
										// columns
	}

	/**
	 * Undo branching decisions during backtracking in the Branch-and-Price tree
	 * 
	 * @param bd
	 *            Branching decision
	 */
	@Override
	public void branchingDecisionReversed(BranchingDecision bd) {
		// No action required
	}
}

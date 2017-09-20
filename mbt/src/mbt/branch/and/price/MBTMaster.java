package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.master.AbstractMaster;
import org.jorlib.frameworks.columnGeneration.master.OptimizationSense;
import org.jorlib.frameworks.columnGeneration.util.OrderedBiMap;

import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 * Esta clase define el problema master.
 * 
 */
public final class MBTMaster extends AbstractMaster<DataModel, Arbol, MBTPricingProblem, MBTMasterData> {

	private IloObjective obj; // Función objetivo.
	private IloRange[] costLessThanH; // Constraint
	private IloRange[] vertexBelongsToOneTree; // Constraint

	private IloNumVar h;

	//El V0 y el offset. Esto va cambiando dinámicamente según el branching.
	//Lo vamos actualizando en estas variables.
	//private final Set<Integer> V0;
	//private final int[] offset;

	public MBTMaster(DataModel dataModel, Set<Integer> V0, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem, OptimizationSense.MINIMIZE);
		//this.dataModel.getV0() = new HashSet<Integer>(V0);
		//this.dataModel.getOffset() = new int[dataModel.getGrafo().getVertices()];
		System.out.println("Master constructor. Columns: " + masterData.getNrColumns());
	}

	/**
	 * Crea el modelo del problema master.
	 * 
	 * @return Devuelve un MBTMasterData. Esto es un contenedor para información del
	 *         master.
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
			costLessThanH = new IloRange[dataModel.getV0().size()];
			for (int i = 0; i < dataModel.getV0().size(); i++) {
				// El addRange este funciona como el rango de la desigualdad. En
				// este caso, entre -infinito y 0
				costLessThanH[i] = cplex.addRange(Double.MIN_VALUE, 0, "costLessThanH");

				// y registro la variable h para esa desigualdad.
				iloColumn = iloColumn.and(masterData.cplex.column(this.costLessThanH[i], -1.0));
			}

			// Creamos la variable h que representa esa columna.
			h = masterData.cplex.numVar(iloColumn, 0, dataModel.getMaxT(), "h");
			masterData.cplex.add(h);

			// y ahora creamos la segunda desigualdad, vacía
			vertexBelongsToOneTree = new IloRange[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); i++)
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
			if (dataModel.getV0().contains(column.getRoot()))
				iloColumn = iloColumn.and(masterData.cplex.column(this.costLessThanH[column.getRoot()], column.getT()));

			// Registramos la columna con los primeros constraints, para los
			// vértices que conforman a T
			for (Integer vertex : column.getVertices())
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
			Arbol[] arbolesEnLaSolucion = masterData.getColumnsForPricingProblemAsList()
					.toArray(new Arbol[masterData.getNrColumns()]);
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
	 * Listener para cuando se realizan las decisiones de branching
	 * 
	 * @param bd
	 *            Branching decision
	 */
	@Override
	public void branchingDecisionPerformed(BranchingDecision bd) {

		// acá cerramos el modelo de cplex y lo creamos otra vez.
		this.close();
		masterData = this.buildModel();
	}

	/**
	 * Listener para cuando se backtrackean las decisiones de branching.
	 * 
	 * @param bd
	 *            Branching decision
	 */
	@Override
	public void branchingDecisionReversed(BranchingDecision bd) {
		//No hacemos nada en el master si backtrackeamos.
	}
}

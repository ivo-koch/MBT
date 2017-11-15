package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
public final class MBTMaster extends AbstractMaster<DataModel, MBTColumn, MBTPricingProblem, MBTMasterData> {

	private IloObjective obj; // Función objetivo.
	private TreeMap<Integer, IloRange> costLessThanH; // Constraint
	private IloRange[] vertexBelongsToOneTree; // Constraint

	private IloNumVar h;

	private List<IloNumVar> vars = new ArrayList<IloNumVar>();

	public MBTMaster(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem, OptimizationSense.MAXIMIZE);
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
			if (vars != null) vars.clear();
			cplex = new IloCplex(); // Nueva instancia de cplex.
			cplex.setOut(null); // Deshabilitamos el output.
			// Setea el máx. número de threads.
			cplex.setParam(IloCplex.IntParam.Threads, config.MAXTHREADS);

			// Función objetivo
			obj = cplex.addMaximize();

			// tenemos que agregar la variable h así
			// esto registra la columna de la h con la función objetivo, con coeficiente 1
			IloColumn iloColumn = cplex.column(obj, -1);

			// Constraints para la primera desigualdad del master.
			costLessThanH = new TreeMap<Integer, IloRange>();
			for (int v0 : dataModel.getV0()) {
				// El addRange este funciona como el rango de la desigualdad. En
				// este caso, entre -infinito y 0
				costLessThanH.put(v0, cplex.addRange(-Double.MAX_VALUE, 0, "costLessThanH v" + v0));

				// y registro la variable h para esa desigualdad.
				iloColumn = iloColumn.and(cplex.column(this.costLessThanH.get(v0), -1.0));
			}

			// Creamos la variable h que representa esa columna.
			h = cplex.numVar(iloColumn, 0, dataModel.getMaxT(), "h");
			cplex.add(h);
				
			// y ahora creamos la segunda desigualdad, vacía
			vertexBelongsToOneTree = new IloRange[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); i++)
				// el range va así porque es una igualdad.
				vertexBelongsToOneTree[i] = cplex.addRange(1, 1, "vertexBelongsToOneTree v" + i);

		} catch (IloException e) {
			e.printStackTrace();
		}

		// esto es código técnico del framework, ni lo tocamos.
		Map<MBTPricingProblem, OrderedBiMap<MBTColumn, IloNumVar>> varMap = new LinkedHashMap<>();
		MBTPricingProblem pricingProblem = this.pricingProblems.get(0);
		varMap.put(pricingProblem, new OrderedBiMap<MBTColumn, IloNumVar>());

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
				masterData.cplex.exportModel("master_" + this.getIterationCount() + ".lp");
			// masterData.cplex.exportModel("master_" + this.getIterationCount() + ".lp");
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

			IloRange[] costLessThanHEnOrdInsercion = new IloRange[this.costLessThanH.size()];
			// Duales para la primera restricción.
			int i = 0;
			for (int v0 : this.costLessThanH.keySet()) {
				costLessThanHEnOrdInsercion[i++] = this.costLessThanH.get(v0);
				logger.debug("Dual costLessThanH v" + v0 + ":" + masterData.cplex.getDual(this.costLessThanH.get(v0)));
			}

			double[] dualValuesRest1 = masterData.cplex.getDuals(costLessThanHEnOrdInsercion);
			// Duales para la segunda restricción.
			double[] dualValuesRest2 = masterData.cplex.getDuals(this.vertexBelongsToOneTree);

			for (int j = 0; j < dataModel.getGrafo().getVertices(); j++)
				logger.debug("Dual vertexBelongsToOneTree v" + j + ":"
						+ masterData.cplex.getDual(this.vertexBelongsToOneTree[j]));

			logger.debug("Funcion objetivo " +  masterData.cplex.getObjValue());
			for (IloNumVar var : vars)
				logger.debug("Var " + var + "......" + masterData.cplex.getValue(var));

			// Hacemos la cuenta del costo reducido para cada columna, para verificar.
			// Todas tendrían que ser > 0
			// for (IloNumVar var : vars)
			// logger.debug("Reduced cost " + var + ":" +
			// masterData.cplex.getReducedCost(var));

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
	public void addColumn(MBTColumn column) {
		try {
			// Registramos una columna (un nuevo árbol T) con coeficiente 0 en
			// la función objetivo
			IloColumn iloColumn = masterData.cplex.column(obj, 0);

			// Registramos la columna con los primeros constraints, si este T
			// comienza en un v0
			if (dataModel.getV0().contains(column.getArbol().getRoot()))
				iloColumn = iloColumn.and(masterData.cplex.column(this.costLessThanH.get(column.getArbol().getRoot()),
						column.getArbol().calcularCosto() + dataModel.getOffset()[column.getArbol().getRoot()]));

			// Registramos la columna con los segundos constraints, para los
			// vértices que conforman a T
			for (Integer vertex : column.getArbol().getInternalNodes())
				iloColumn = iloColumn.and(masterData.cplex.column(this.vertexBelongsToOneTree[vertex], 1.0));

			iloColumn = iloColumn
					.and(masterData.cplex.column(this.vertexBelongsToOneTree[column.getArbol().getRoot()], 1.0));
			// Creamos la variable
			IloNumVar var = masterData.cplex.numVar(iloColumn, 0, Double.MAX_VALUE, "T_" + column);
			logger.debug("Offset:_" + Arrays.toString(dataModel.getOffset()));
			logger.debug("Agregando variable " + "T_" + column);
			masterData.cplex.add(var);
			masterData.addColumn(column, var);
			vars.add(var);
		} catch (IloException e) {
		}
	}

	/**
	 * Obtiene la solución del master.
	 * 
	 * @return Devuelve todas las columnas distintas de 0
	 */
	@Override
	public List<MBTColumn> getSolution() {
		List<MBTColumn> solution = new ArrayList<>();
		try {
			MBTColumn[] arbolesEnLaSolucion = masterData.getColumnsForPricingProblemAsList()
					.toArray(new MBTColumn[masterData.getNrColumns()]);
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
		List<MBTColumn> solution = this.getSolution();
		for (MBTColumn is : solution)
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

		// modificamos el V0 y el offser según la decisión del branch.
		MBTBranchingDecision decision = (MBTBranchingDecision) bd;
		int origen = decision.getArista().getV1();
		int destino = decision.getArista().getV2();

		// agregamos el destino a V0
		this.dataModel.getV0().add(destino);

		// y el offset del origen aumenta en uno
		this.dataModel.getOffset()[origen]++;

		if (this.dataModel.getOffset()[origen] > this.dataModel.getMaxT())
			throw new IllegalStateException("No podemos branchear con un t mayor a maxT!");

		// el offset del destino es del origen.
		this.dataModel.getOffset()[destino] = this.dataModel.getOffset()[origen];

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
		// No hacemos nada en el master si backtrackeamos.

		MBTBranchingDecision decision = (MBTBranchingDecision) bd;
		int origen = decision.getArista().getV1();
		int destino = decision.getArista().getV2();

		// el destino se va de V0, porque lo pusimos en la decisión de branching que
		// estamos revirtiendo.
		this.dataModel.getV0().remove(destino);
		this.dataModel.getOffset()[destino] = 0;

		// el origen va a tener el valor de offset que estaba en la decisión menos 1.
		this.dataModel.getOffset()[origen]--;
		if (this.dataModel.getOffset()[origen] < 0)
			throw new IllegalStateException("No puede ser que el valor de offset de un vértice sea negativo");

	}
}

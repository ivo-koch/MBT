package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashMap;
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
public final class ExactPricingProblemSolverMultipleV0
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

	/// Para la numeración de los constraints, ver el modelo en latex.

	/*** Diccionarios de Restricciones */
	// indexado por la arista v0,j
	private Map<Grafo.AristaDirigida, IloConstraint> constraints32 = new HashMap<Grafo.AristaDirigida, IloConstraint>();

	// indexado por v0
	private Map<Integer, IloConstraint> constraints33 = new HashMap<Integer, IloConstraint>();

	// indexado por v0
	private Map<Integer, Set<IloConstraint>> constraints35 = new HashMap<Integer, Set<IloConstraint>>();

	private IloConstraint constraint34;

	// indexado por v0
	private Map<Integer, IloConstraint> constraintsOffset = new HashMap<Integer, IloConstraint>();

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public ExactPricingProblemSolverMultipleV0(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "ExactPricingProblemSolver";
		this.buildModel();
	}

	/**
	 * Construye el problema de pricing: en nuestro caso, encontrar un árbol
	 * generador de mínimo peso, con pesos en los vértices.
	 */
	private void buildModel() {
		try {
			cplex = new IloCplex();
			cplex.setParam(IloCplex.IntParam.AdvInd, 0);
			cplex.setParam(IloCplex.IntParam.Threads, 1);
			cplex.setOut(null);

			// variable x
			x = new IloNumVar[dataModel.getGrafo().getVertices()][dataModel.getGrafo().getVertices()][];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
					if (i != j && dataModel.getGrafo().isArista(i, j)) {
						int Ni = dataModel.getGrafo().getVecinos(i).size();
						x[i][j] = new IloNumVar[Ni];
						for (int k = 0; k < Ni; k++)
							x[i][j][k] = cplex.boolVar("x[" + i + "," + j + "," + k + "]");
					}

			// variable t
			t = new IloNumVar[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				t[i] = cplex.numVar(0, dataModel.getMaxT(), "t" + i);

			// variable z
			z = new IloNumVar[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				z[i] = cplex.boolVar("z" + i);

			// función objetivo vacía
			// vamos a dar la expresión de la f.obj. en el método setObjective()
			obj = cplex.addMinimize();

			// constraints

			// restricción (30)
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++) {
					IloNumExpr lhs = cplex.linearIntExpr();
					for (int j : dataModel.getGrafo().getVecinos(i))
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

					cplex.addLe(lhs, 1, "rest30");
				}

			// restricción (31)
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
					if (i != j && dataModel.getGrafo().isArista(i, j)) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));

						for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
							lhs = cplex.sum(lhs, cplex.prod(-(k + 1), x[i][j][k]));

						lhs = cplex.sum(lhs, cplex.prod(-1.0, t[j]));

						for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
							lhs = cplex.sum(lhs, cplex.prod(-dataModel.getM(), x[i][j][k]));

						cplex.addGe(lhs, -dataModel.getM(), "rest31");
					}

			// restricción (32)
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				if (!dataModel.getV0().contains(i))
					for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
						if (i != j && !dataModel.getV0().contains(j) && dataModel.getGrafo().isArista(i, j))
							this.addConstraint32(i, j);

			// restricción (33)
			for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
				if (!dataModel.getV0().contains(j))
					this.addConstraint33(j);

			// restricción (34)
			this.updateConstraint34();

			// restricción (35)
			for (int v0 : dataModel.getV0())
				this.addConstraint35(v0);

			// restricción (36)
			for (int v0 : dataModel.getV0())
				this.addConstraintOffset(v0);
			
			for (int v0 : dataModel.getV0())
				this.addConstraintOffset2(v0);


		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	private String dumpModel() throws UnknownObjectException, IloException {
		StringBuilder sb = new StringBuilder();
		// costos duales:

		sb.append("Duales:\n");
		for (int i = 0; i < this.pricingProblem.dualCosts.length; i++)
			sb.append(this.pricingProblem.dualCosts[i] + ",");
		sb.append("\n");

		// variable x
		for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
			for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
				if (i != j && dataModel.getGrafo().isArista(i, j)) {
					int Ni = dataModel.getGrafo().getVecinos(i).size();
					for (int k = 0; k < Ni; k++)
						if (cplex.getValue(x[i][j][k]) > 1 - config.PRECISION)
							sb.append("x[" + i + ", " + j + ", " + k + "]\n");
				}
		// variable t
		sb.append("Variables t:\n");

		for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
			if (cplex.getValue(t[i]) > config.PRECISION)
				sb.append("t[" + i + "] = " + cplex.getValue(t[i]) + "\n");

		// variable z
		sb.append("Variables z:\n");
		for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
			if (cplex.getValue(z[i]) > 1 - config.PRECISION)
				sb.append("z[" + i + "]\n");
		return sb.toString();
	}

	private int activacion = 1;

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

			logger.debug("Resolviendo exacto...");
			
			if (config.EXPORT_MODEL)
				cplex.exportModel("pricingProblem" + activacion + ".lp");
			activacion++;
			logger.debug("Resolviendo pricing...");
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
				logger.debug("Pricing resuelto");
				this.pricingProblemInfeasible = false;
				this.objective = cplex.getObjValue();

				// podemos agregar el resultado a la base?
				if (objective < -config.PRECISION) { // - config.PRECISION) {
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
					logger.debug(dumpModel());

					MBTColumn columna = new MBTColumn(pricingProblem, false, this.getName(), builder.buildArbol());
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

			this.close();
			this.buildModel();
			//
			// // ahora destino pasa a estar en V0
			// // así que ahora, para todos sus vecinos fuera de V0, deja de aplicar el
			// // constraint 32
			// List<AristaDirigida> aristasIncidentes =
			// dataModel.getGrafo().getAristasIncidentes(destino);
			// for (AristaDirigida arista : aristasIncidentes)
			// if (!this.dataModel.getV0().contains(arista.getV2())) {
			// this.removeConstraint32(destino, arista.getV2());
			// this.removeConstraint32(arista.getV2(), destino);
			// }
			//
			// // lo mismo pasa con la desigualdad 33, antes aplicaba porque j=destino
			// // no estaba en V0 y
			// // ahora ya no.
			// this.removeConstraint33(destino);
			//
			// // actualizamos la desigualdad 34, porque cambió el V0
			// this.updateConstraint34();
			//
			// // y lo mismo la de offset
			// this.addConstraintOffset(destino);
			//
			// // actualizamos el constraint de offset para el origen, porque cambió.
			//
			// this.removeConstraintOffset(origen);
			// this.addConstraintOffset(origen);
			//
			// // para el nuevo v0 vale la rest. 35 ahora.
			// this.addConstraint35(destino);

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

			this.close();
			this.buildModel();
			//
			// // así que ahora, para todos sus vecinos fuera de V0, aplica el constraint 32
			// List<AristaDirigida> aristasIncidentes =
			// dataModel.getGrafo().getAristasIncidentes(destino);
			// // entonces se lo agregamos.
			// for (AristaDirigida arista : aristasIncidentes)
			// if (!this.dataModel.getV0().contains(arista.getV2())) {
			// this.addConstraint32(destino, arista.getV2());
			// this.addConstraint32(arista.getV2(), destino);
			// }
			//
			// // lo mismo pasa con la desigualdad 33, antes no aplicaba porque destino
			// // estaba en V0 y ahora sí porque deja de estar.
			// this.addConstraint33(destino);
			//
			// // y la desigualdad 34 deja de ser necesaria, ya no está en V0
			// this.updateConstraint34();
			//
			// // y lo mismo la de offset
			// this.removeConstraintOffset(destino);
			//
			//
			// this.removeConstraintOffset(origen);
			// this.addConstraintOffset(origen);
			// this.removeConstraint35(destino);
			//

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * Agrega el constraint 32 para la arista i, j, en esa dirección
	 * 
	 * @param i
	 * @param j
	 * @throws IloException
	 */
	private void addConstraint32(int i, int j) throws IloException {

		IloNumExpr lhs = cplex.linearIntExpr();

		for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
			lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

		for (int v : dataModel.getGrafo().getVecinos(i))
			for (int k = 0; k < dataModel.getGrafo().getVecinos(v).size(); k++)
				lhs = cplex.sum(lhs, cplex.prod(-1.0, x[v][i][k]));

		cplex.addLe(lhs, 0, "rest32");
		//
		// AristaDirigida arista = dataModel.getGrafo().getArista(i, j);
		// if (this.constraints32.containsKey(arista))
		// throw new RuntimeException("El constraint 32 ya existe para la arista " + i +
		// " " + j);
		//
		// this.constraints32.put(arista, nuevoConstraint);
	}

	/***
	 * Quito el constraint32 para i, j dirigido
	 * 
	 * @param i
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraint32(int i, int j) throws IloException {

		if (!this.constraints32.containsKey(dataModel.getGrafo().getArista(i, j)))
			throw new RuntimeException("El constraint 32 no existe para la arista " + i + " " + j);

		AristaDirigida arista = dataModel.getGrafo().getArista(i, j);
		cplex.remove(this.constraints32.get(arista));
		this.constraints32.remove(arista);
	}

	/***
	 * Agrego el constraint 35 para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void addConstraint35(int i) throws IloException {
		//
		// if (this.constraints35.containsKey(i))
		// throw new RuntimeException("El constraint 35 ya existe para el vértice " +
		// i);

		// Set<IloConstraint> rest = new HashSet<IloConstraint>();
		for (int j : dataModel.getGrafo().getVecinos(i)) {
			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, cplex.prod(-1.0, z[i]));

			for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
				lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

			cplex.addLe(lhs, 0, "rest35");
		}
		// this.constraints35.put(i, rest);
	}

	/***
	 * Quito el constraint 35 para el vértice i
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraint35(int i) throws IloException {

		if (!this.constraints35.containsKey(i))
			throw new RuntimeException("El constraint 35 no existe para el vértice  " + i);

		for (IloConstraint const35 : this.constraints35.get(i))
			cplex.remove(const35);
		this.constraints35.remove(i);
	}

	/***
	 * Quito el constraint 33 para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraint33(int j) throws IloException {

		if (!this.constraints33.containsKey(j))
			throw new RuntimeException("El constraint 33 no existe para el vértice  " + j);

		cplex.remove(this.constraints33.get(j));
		this.constraints33.remove(j);
	}

	/***
	 * Agrego el constraint 34 para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void updateConstraint34() throws IloException {

		if (this.constraint34 != null)
			cplex.remove(this.constraint34);

		IloNumExpr lhs = cplex.linearIntExpr();

		for (int v0 : dataModel.getV0())
			lhs = cplex.sum(lhs, cplex.prod(1.0, z[v0]));

		this.constraint34 = cplex.addLe(lhs, 1, "rest34");
	}

	/***
	 * Agrego el constraint 35 para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void addConstraint33(int j) throws IloException {

		IloNumExpr lhs = cplex.linearIntExpr();

		lhs = cplex.sum(lhs, cplex.prod(-1.0, z[j]));

		for (int i : dataModel.getGrafo().getVecinos(j))
			for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
				lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

		cplex.addEq(lhs, 0, "rest33");

		// if (this.constraints33.containsKey(j))
		// throw new RuntimeException("El constraint 33 ya existe para el vértice " +
		// j);
		//
		// this.constraints33.put(j, cplex.addEq(lhs, 0));
	}

	/***
	 * Agrego el constraint de offset para el vértice j, para el offset o
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void addConstraintOffset(int i) throws IloException {

		for (int k = 0; k < dataModel.getOffset()[i]; k++) {
			IloNumExpr lhs = cplex.linearIntExpr();
			for (int j : dataModel.getGrafo().getVecinos(i))
				lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));
			cplex.addEq(lhs, 0, "rest36");
		}

		// if (this.constraintsOffset.containsKey(i))
		// throw new RuntimeException("El constraint de offset ya existe para el vértice
		// " + i);
		//
		// this.constraintsOffset.put(i, cplex.addLe(lhs, 0));
	}

	private void addConstraintOffset2(int i) throws IloException {

		IloNumExpr lhs = cplex.linearIntExpr();
		lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));
		cplex.addGe(lhs, this.dataModel.getOffset()[i], "rest37");

		// if (this.constraintsOffset.containsKey(i))
		// throw new RuntimeException("El constraint de offset ya existe para el vértice
		// " + i);
		//
		// this.constraintsOffset.put(i, cplex.addLe(lhs, 0));
	}

	/***
	 * Quito el constraint de offset para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraintOffset(int j) throws IloException {

		if (this.constraintsOffset.containsKey(j)) {
			cplex.remove(this.constraintsOffset.get(j));
			this.constraintsOffset.remove(j);
		} else if (!this.dataModel.getInitialV0().contains(j))
			throw new RuntimeException("El constraint de offset no existe para el vértice  " + j);
	}
}

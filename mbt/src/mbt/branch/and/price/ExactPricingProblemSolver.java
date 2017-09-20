package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import util.Grafo;
import util.Grafo.AristaDirigida;

/***
 * Implementación de solución exacta para el problema de pricing.
 */
public final class ExactPricingProblemSolver extends AbstractPricingProblemSolver<DataModel, Arbol, MBTPricingProblem> {

	private IloCplex cplex; // Cplex instance.
	private IloObjective obj; // Objective function

	private IloNumVar[][][] x;
	private IloNumVar[] z;
	private IloNumVar[] t;
	private IloNumVar[] w;
	private int maxT;

	IloNumVar[] varsEnFobj;

	// Constraints del modelo de pricing que involucran un vértice de V0
	private Map<Grafo.AristaDirigida, IloConstraint> constraints32;

	private Map<Integer, IloConstraint> constraints33;

	private Map<Integer, IloConstraint> constraints34;

	private Map<Integer, IloConstraint> constraintsOffset;

	// tiene que salir de exactamente un v0 \in V0
	private IloConstraint desigualdad35;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public ExactPricingProblemSolver(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "ExactPricingProblemSolver";
		// this.V0 = new TreeSet<Integer>(dataModel.getV0());
		// this.offset = new int[dataModel.getGrafo().getVertices()];
		this.buildModel();
	}

	/**
	 * Construye el problema de pricing: en nuestro caso, encontrar un árbol
	 * generador de mínimo peso, con pesos
	 */
	private void buildModel() {
		try {
			cplex = new IloCplex();
			cplex.setParam(IloCplex.IntParam.AdvInd, 0);
			cplex.setParam(IloCplex.IntParam.Threads, 1);
			cplex.setOut(null);

			IloCplex cplex = new IloCplex();

			IloRange[][] rng = new IloRange[1][];

			// variable x
			x = new IloNumVar[dataModel.getGrafo().getVertices()][dataModel.getGrafo().getVertices()][];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
					if (i != j && dataModel.getGrafo().isArista(i, j)) {
						int Ni = dataModel.getGrafo().getVecinos(i).size();
						x[i][j] = new IloNumVar[Ni];
						for (int k = 0; k < Ni; k++)
							x[i][j][k] = cplex.boolVar();
					}

			// variable t
			t = new IloNumVar[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				t[i] = cplex.numVar(0, maxT);

			// variable z
			z = new IloNumVar[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				z[i] = cplex.boolVar();

			// variable w
			w = new IloNumVar[dataModel.getGrafo().getVertices()];
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				w[i] = cplex.boolVar();

			// función objetivo.
			// IloNumExpr fobj = cplex.linearIntExpr();
			// fobj = cplex.sum(fobj, cplex.prod(pesoT, t[v0]));

			// obj = cplex.linearIntExpr();
			obj = cplex.addMinimize();

			varsEnFobj = new IloNumVar[dataModel.getGrafo().getVertices() + dataModel.getV0().size()];

			int l = 0;
			for (int v0 : dataModel.getV0())
				varsEnFobj[l++] = t[v0];

			for (int i = 0; i < dataModel.getGrafo().getVertices(); i++)
				varsEnFobj[l + i] = z[i];

			// constraints
			ArrayList<IloRange> restricciones = new ArrayList<IloRange>();

			// restricción (30)
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++) {
					IloNumExpr lhs = cplex.linearIntExpr();
					for (int j : dataModel.getGrafo().getVecinos(i))
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

					restricciones.add(cplex.addLe(lhs, 1));
				}

			// restricción (31)
			double M = Math.pow(dataModel.getGrafo().getVertices(), 3);

			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
					if (i != j && dataModel.getGrafo().isArista(i, j)) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));

						for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
							lhs = cplex.sum(lhs, cplex.prod(-(k + 1), x[i][j][k]));

						lhs = cplex.sum(lhs, cplex.prod(-1.0, t[j]));

						for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
							lhs = cplex.sum(lhs, cplex.prod(-M, x[i][j][k]));

						// lhs = cplex.sum(lhs, cplex.prod(-M, y[i][j]));

						restricciones.add(cplex.addGe(lhs, -M));
					}

			// restricción (32)
			for (int i = 0; i < dataModel.getGrafo().getVertices(); ++i)
				if (!dataModel.getV0().contains(i))
					for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
						if (i != j && dataModel.getGrafo().isArista(i, j))
							this.addConstraint32(i, j);

			// IloNumExpr lhs = cplex.linearIntExpr();
			//
			// for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
			// lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));
			//
			// for (int v : dataModel.getGrafo().getVecinos(i))
			// for (int k = 0; k < dataModel.getGrafo().getVecinos(v).size(); k++)
			// lhs = cplex.sum(lhs, cplex.prod(-1.0, x[v][i][k]));
			//
			// restricciones.add(cplex.addLe(lhs, 0));

			// restricción (33)
			for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
				if (!dataModel.getV0().contains(j))
					this.addConstraint33(j);
			// IloNumExpr lhs = cplex.linearIntExpr();
			//
			// lhs = cplex.sum(lhs, cplex.prod(-1.0, z[j]));
			//
			// for (int i : dataModel.getGrafo().getVecinos(j))
			// for (int k = 0; k < dataModel.getGrafo().getVecinos(i).size(); k++)
			// lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));
			//
			// restricciones.add(cplex.addEq(lhs, 0));

			// restricción (34)
			for (int v0 : dataModel.getV0())
				this.addConstraint34(v0);
			// {
			// IloNumExpr lhs = cplex.linearIntExpr();
			// lhs = cplex.sum(lhs, cplex.prod(1.0, t[v0]));
			// lhs = cplex.sum(lhs, cplex.prod(-M, w[v0]));
			// restricciones.add(cplex.addLe(lhs, 0));
			// }

			// restricción (35)
			// lo hacemos para todos los vértices y no para los del V0 inicial así no hay
			// que modificarla
			// dinámicamente.
			IloNumExpr lhs = cplex.linearIntExpr();
			for (int v = 0; v < dataModel.getGrafo().getVertices(); v++)
				lhs = cplex.sum(lhs, cplex.prod(1.0, w[v]));
			restricciones.add(cplex.addEq(lhs, 1));

			rng[0] = new IloRange[restricciones.size()];

			int tr = 0;
			for (IloRange range : restricciones)
				rng[0][tr++] = range;

		} catch (IloException e) {
			e.printStackTrace();
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
	public List<Arbol> generateNewColumns() throws TimeLimitExceededException {
		List<Arbol> newPatterns = new ArrayList<>();
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
				if (objective > 0 + config.PRECISION) {

					// si es así, agregamos una nueva columna representada por ese árbol a la base

					double t_v0 = 0;
					int v0Raiz = -1;
					for (int v0 = 0; v0 < dataModel.getGrafo().getVertices(); v0++)
						if (cplex.getValue(w[v0]) > 1 - config.PRECISION) {
							t_v0 = cplex.getValue(t[v0]); // Get the variable
							v0Raiz = v0;
						}

					// Creamos el árbol.
					Set<Integer> vertices = new HashSet<Integer>();
					for (int v = 0; v < dataModel.getGrafo().getVertices(); v++)
						if (cplex.getValue(z[v]) > 1 - config.PRECISION)
							vertices.add(v);

					Arbol columna = new Arbol(pricingProblem, false, this.getName(), vertices, v0Raiz, t_v0);

					newPatterns.add(columna);
				}
			}

		} catch (IloException e) {
			e.printStackTrace();
		}
		return newPatterns;
	}

	/**
	 * Update the objective function of the pricing problem with the new dual
	 * information. The dual values are stored in the pricing problem.
	 */
	@Override
	public void setObjective() {

		try {
			obj.setExpr(cplex.scalProd(pricingProblem.dualCosts, this.varsEnFobj));
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Close the pricing problem
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

			// registramos en el master la decisión del branch.
			MBTBranchingDecision decision = (MBTBranchingDecision) bd;
			int origen = decision.getArista().getV1();
			int destino = decision.getArista().getV2();

			// agregamos el destino a V0
			this.dataModel.getV0().add(destino);

			// y el offset del origen aumenta en uno
			this.dataModel.getOffset()[origen]++;

			// el offset del destino es del origen.
			this.dataModel.getOffset()[destino] = this.dataModel.getOffset()[origen];

			// ahora destino pasa a estar en V0
			// así que ahora, para todos sus vecinos fuera de V0, deja de aplicar el
			// constraint 32
			Set<AristaDirigida> aristasIncidentes = dataModel.getGrafo().getAristasIncidentes(destino);
			// entonces se lo agregamos.
			for (AristaDirigida arista : aristasIncidentes)
				if (!this.dataModel.getV0().contains(arista.getV2()))
					this.removeConstraint32(destino, arista.getV2());

			// lo mismo pasa con la desigualdad 33, antes aplicaba porque j=destino
			// no estaba en V0 y
			// ahora ya no.
			this.removeConstraint33(destino);

			// y la desigualdad 34 pasa a valer, porque destino está en V0.
			this.addConstraint34(destino);

			// y lo mismo la de offset
			this.addConstraintOffset(destino);

			// actualizamos el constraint de offset para el origen, porque cambió.
			this.removeConstraintOffset(origen);
			this.addConstraintOffset(origen);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * When the Branch-and-Price algorithm backtracks, branching decisions are
	 * reversed.
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

			// el destino se va de V0, porque lo pusimos en la decisión de branching que
			// estamos revirtiendo.
			this.dataModel.getV0().remove(destino);
			this.dataModel.getOffset()[destino] = 0;

			// el origen va a tener el valor de offset que estaba en la decisión menos 1.
			this.dataModel.getOffset()[origen]--;
			if (this.dataModel.getOffset()[origen] < 0)
				throw new IllegalStateException("No puede ser que el valor de offset de un vértice sea negativo");

			// así que ahora, para todos sus vecinos fuera de V0, aplica el constraint 32
			Set<AristaDirigida> aristasIncidentes = dataModel.getGrafo().getAristasIncidentes(destino);
			// entonces se lo agregamos.
			for (AristaDirigida arista : aristasIncidentes)
				if (!this.dataModel.getV0().contains(arista.getV2()))
					this.addConstraint32(destino, arista.getV2());

			// lo mismo pasa con la desigualdad 33, antes no aplicaba porque destino
			// estaba en V0 y ahora sí porque deja de estar.
			this.addConstraint33(destino);

			// y la desigualdad 34 deja de ser necesaria, ya no está en V0
			this.removeConstraint34(destino);

			// y lo mismo la de offset
			this.removeConstraintOffset(destino);

			// ahora, el offset del origen, si revertimos el branch, tenemos que dejarlo en
			// 1 menos que el que tenía.
			// si el offset quedó en 0 y no está en el V0 original, lo sacamos.
			if (this.dataModel.getOffset()[origen] == 0 && !this.dataModel.getInitialV0().contains(origen)) {
				this.dataModel.getV0().remove(origen);
				// si sacamos el origen de V0, eso tiene impacto en las desigualdades.

				Set<AristaDirigida> aristasIncid = dataModel.getGrafo().getAristasIncidentes(origen);
				// entonces se lo agregamos.
				for (AristaDirigida arista : aristasIncid)
					if (!this.dataModel.getV0().contains(arista.getV2()))
						this.addConstraint32(origen, arista.getV2());

				// lo mismo pasa con la desigualdad 33, antes no aplicaba porque destino
				// estaba en V0 y ahora sí porque deja de estar.
				this.addConstraint33(origen);

				// y la desigualdad 34 deja de ser necesaria, ya no está en V0
				this.removeConstraint34(origen);

				// y lo mismo la de offset
				this.removeConstraintOffset(origen);
			}

			else {
				// actualizamos el offset del origen
				this.removeConstraintOffset(origen);
				this.addConstraintOffset(origen);
			}

		} catch (IloException e) {
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

		IloConstraint nuevoConstraint = cplex.addLe(lhs, 0);

		AristaDirigida arista = dataModel.getGrafo().getArista(i, j);
		if (this.constraints32.containsKey(arista))
			throw new RuntimeException("El constraint 32 ya existe para la arista " + i + " " + j);

		this.constraints32.put(arista, nuevoConstraint);
	}

	/***
	 * Quito el constraint32 para i, j dirigido
	 * 
	 * @param i
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraint32(int i, int j) throws IloException {

		if (!this.constraints32.containsKey(i))
			throw new RuntimeException("El constraint 32 no existe para la arista " + i + " " + j);

		AristaDirigida arista = dataModel.getGrafo().getArista(i, j);
		cplex.remove(this.constraints32.get(arista));
		this.constraints32.remove(arista);
	}

	/***
	 * Agrego el constraint 33 para el vértice j
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

		if (this.constraints33.containsKey(j))
			throw new RuntimeException("El constraint 33 ya existe para el vértice " + j);

		this.constraints33.put(j, cplex.addEq(lhs, 0));
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
	private void addConstraint34(int j) throws IloException {

		IloNumExpr lhs = cplex.linearIntExpr();
		lhs = cplex.sum(lhs, cplex.prod(1.0, t[j]));
		lhs = cplex.sum(lhs, cplex.prod(-dataModel.getM(), w[j]));

		if (this.constraints34.containsKey(j))
			throw new RuntimeException("El constraint 34 ya existe para el vértice " + j);

		this.constraints34.put(j, cplex.addLe(lhs, 0));
	}

	/***
	 * Quito el constraint 34 para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraint34(int j) throws IloException {

		if (!this.constraints34.containsKey(j))
			throw new RuntimeException("El constraint 34 no existe para el vértice  " + j);

		cplex.remove(this.constraints34.get(j));
		this.constraints34.remove(j);
	}

	/***
	 * Agrego el constraint de offset para el vértice j, para el offset o
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void addConstraintOffset(int i) throws IloException {

		IloNumExpr lhs = cplex.linearIntExpr();
		for (int k = 0; k < dataModel.getOffset()[i]; k++)
			for (int j : dataModel.getGrafo().getVecinos(i))
				lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

		if (this.constraintsOffset.containsKey(i))
			throw new RuntimeException("El constraint de offset ya existe para el vértice " + i);

		this.constraintsOffset.put(i, cplex.addLe(lhs, 0));
	}

	/***
	 * Quito el constraint de offset para el vértice j
	 * 
	 * @param j
	 * @throws IloException
	 */
	private void removeConstraintOffset(int j) throws IloException {

		if (!this.constraintsOffset.containsKey(j))
			throw new RuntimeException("El constraint de offset no existe para el vértice  " + j);

		cplex.remove(this.constraintsOffset.get(j));
		this.constraintsOffset.remove(j);
	}
}

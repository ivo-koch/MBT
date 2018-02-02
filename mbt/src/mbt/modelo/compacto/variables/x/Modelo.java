package mbt.modelo.compacto.variables.x;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;
import util.Grafo;

public class Modelo {

	private Grafo g;

	private Set<Integer> V0;

	private IloNumVar[][][] x;

	private IloNumVar[][] y;

	private IloNumVar[] w;

	private int maxT;

	private long[] solucion;

	private long sol;

	private long tiempoEjecucion;

	public Modelo(Grafo g, Set<Integer> V0) {

		this.g = g;
		this.V0 = V0;
		this.maxT = g.getVertices() - 1;
	}

	public void solve(OutputStream baos) throws Exception {

		// create CPLEX optimizer/modeler
		IloCplex cplex = new IloCplex();

		IloRange[][] rng = new IloRange[1][];

		// variables
		x = new IloNumVar[g.getVertices()][g.getVertices()][maxT];
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j))
					for (int t = 0; t < maxT; t++)
						x[i][j][t] = cplex.boolVar();

		y = new IloNumVar[g.getVertices()][maxT];
		for (int i = 0; i < g.getVertices(); ++i)
			for (int t = 0; t < maxT; t++)
				y[i][t] = cplex.boolVar();

		w = new IloNumVar[1];
		// for (int t = 0; t < maxT; t++)
		w[0] = cplex.numVar(0, maxT);

		// función objetivo
		IloNumExpr fobj = cplex.linearIntExpr();

		fobj = cplex.sum(fobj, cplex.prod(1, w[0]));

		cplex.addMinimize(fobj);

		// constraints
		ArrayList<IloRange> restricciones = new ArrayList<IloRange>();

		// preciso esto para arboricidad.

		IloNumExpr lhsV0 = cplex.linearIntExpr();
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j) && !V0.contains(i) && !V0.contains(j))

					if (i < j)
						lhsV0 = cplex.sum(lhsV0, cplex.prod(1.0, x[i][j][0]));
					else
						lhsV0 = cplex.sum(lhsV0, cplex.prod(1.0, x[j][i][0]));

		restricciones.add(cplex.addEq(lhsV0, 0));

		// restricción (2)
		for (int t = 1; t < maxT; t++)
			for (int i = 0; i < g.getVertices(); ++i)
				for (int j = i + 1; j < g.getVertices(); ++j)
					if (g.isArista(i, j)) {
						IloNumExpr lhs = cplex.linearIntExpr();
						if (i < j)
							lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][t]));
						else
							lhs = cplex.sum(lhs, cplex.prod(1.0, x[j][i][t]));

						for (int w2 : g.getVecinos(i))
							if (j != w2)
								// if (i < w2)
								// lhs = cplex.sum(lhs, cplex.prod(-1.0,
								// x[i][w2][t-1]));
								// else
								// lhs = cplex.sum(lhs, cplex.prod(-1.0,
								// x[w2][i][t-1]));

								for (int tPrima = 0; tPrima < t; tPrima++)
									if (i < w2)
										lhs = cplex.sum(lhs, cplex.prod(-1.0, x[i][w2][tPrima]));
									else
										lhs = cplex.sum(lhs, cplex.prod(-1.0, x[w2][i][tPrima]));

						for (int w3 : g.getVecinos(j))
							if (w3 != i)
								// if (j < w3)
								// lhs = cplex.sum(lhs, cplex.prod(-1.0,
								// x[j][w3][t-1]));
								// else
								// lhs = cplex.sum(lhs, cplex.prod(-1.0,
								// x[w3][j][t-1]));

								for (int tPrima = 0; tPrima < t; tPrima++)
									if (j < w3)
										lhs = cplex.sum(lhs, cplex.prod(-1.0, x[j][w3][tPrima]));
									else
										lhs = cplex.sum(lhs, cplex.prod(-1.0, x[w3][j][tPrima]));

						restricciones.add(cplex.addLe(lhs, 0));
					}

		// restricción (3)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					for (int t = 0; t < maxT; t++)
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][t]));

					restricciones.add(cplex.addLe(lhs, 1));
				}

		// restricción (4)
		for (int i = 0; i < g.getVertices(); ++i) {
			IloNumExpr lhs = cplex.linearIntExpr();
			for (int t = 0; t < maxT; t++)
				lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][t]));

			restricciones.add(cplex.addEq(lhs, 1));
		}

		// restricción (5)
		for (int i = 0; i < g.getVertices(); ++i) {
			//no usamos esta restricción si tengo un vértice de v0 aislado 
			if (!V0.contains(i) || g.getVecinos(i).size() > 1) {
				for (int t = 0; t < maxT; t++) {
					IloNumExpr lhs = cplex.linearIntExpr();

					lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][t]));

					for (int w : g.getVecinos(i))
						if (i < w)
							lhs = cplex.sum(lhs, cplex.prod(-1.0, x[i][w][t]));
						else
							lhs = cplex.sum(lhs, cplex.prod(-1.0, x[w][i][t]));

					restricciones.add(cplex.addLe(lhs, 0));
				}
			}
		}

		// restricción (6)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j))
					for (int t = 0; t < maxT; t++) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][t]));

						for (int tPrima = 0; tPrima < t; tPrima++)
							lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][tPrima]));

						for (int tPrima = 0; tPrima < t; tPrima++)
							lhs = cplex.sum(lhs, cplex.prod(1.0, y[j][tPrima]));

						restricciones.add(cplex.addLe(lhs, 2));
					}

		// restricción (7)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j))
					for (int t = 0; t < maxT; t++) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(t, x[i][j][t]));
						lhs = cplex.sum(lhs, cplex.prod(-1.0, w[0]));
						restricciones.add(cplex.addLe(lhs, 0));
					}

		// restricción de matching
		for (int i = 0; i < g.getVertices(); ++i)
			for (int t = 0; t < maxT; t++) {
				IloNumExpr lhs = cplex.linearIntExpr();

				for (int j : g.getVecinos(i))
					if (j != i) {
						if (i < j)
							lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][t]));
						else
							lhs = cplex.sum(lhs, cplex.prod(1.0, x[j][i][t]));
					}

				restricciones.add(cplex.addLe(lhs, 1));
			}

		// // usar los t en orden.
		// for (int t = 1; t < maxT; t++) {
		// IloNumExpr lhs = cplex.linearIntExpr();
		// lhs = cplex.sum(lhs, cplex.prod(1.0, w[t]));
		// lhs = cplex.sum(lhs, cplex.prod(-1.0, w[t - 1]));
		// restricciones.add(cplex.addLe(lhs, 0));
		// }

		rng[0] = new IloRange[restricciones.size()];

		int tr = 0;
		for (IloRange range : restricciones)
			rng[0][tr++] = range;

		cplex.setOut(baos);

		// cplex.exportModel("modelo.lp");
		// cplex.setParam(IloCplex.IntParam.NodeLim, 0);
		// // cplex.setParam(IloCplex.IntParam.BndStrenInd, 1);
		//
		// cplex.setParam(IloCplex.IntParam.Cliques, -1);
		// cplex.setParam(IloCplex.IntParam.ZeroHalfCuts, -1);
		// cplex.setParam(IloCplex.IntParam.FracCuts, -1);
		cplex.setParam(IloCplex.DoubleParam.TiLim, 900);
		// cplex.setParam(IloCplex.IntParam.AdvInd, 1);
		// cplex.setParam(IloCplex.IntParam.MIPDisplay, 5);
		// optimize and output solution information
		long start = System.currentTimeMillis();
		boolean ok = cplex.solve();
		tiempoEjecucion = System.currentTimeMillis() - start;

		System.out.println("Gap " + cplex.getMIPRelativeGap());
		System.out.println("Mejor sol entera " + cplex.getObjValue());
		System.out.println("Status " + cplex.getCplexStatus());
		System.out.println("Best bound " + cplex.getBestObjValue());
		System.out.print(baos.toString());

		if (ok) {

			solucion = new long[g.getVertices()];

			for (int v = 0; v < g.getVertices(); v++)
				for (int t = 0; t < maxT; t++) {
					if (cplex.getValue(this.y[v][t]) > 0.99) {
						solucion[v] = t;
						System.out.println("y[" + v + "] = " + solucion[v]);
					}
				}
			System.out.println();

			for (int i = 0; i < g.getVertices(); ++i)
				for (int j = i + 1; j < g.getVertices(); ++j)
					if (g.isArista(i, j))
						for (int t = 0; t < maxT; t++)
							// System.out.println("x[" + i + "," + j + ": " + t
							// + "] = " + cplex.getValue(this.x[i][j][t]));
							if (cplex.getValue(this.x[i][j][t]) > 0.99)
								System.out.println("x[" + i + "," + j + "] = " + t);

			// for (int t = maxT - 1; t >= 0; t--)
			// if (cplex.getValue(this.w[t]) > 0.99) {
			System.out.println("t = " + cplex.getValue(this.w[0]));
			sol = Math.round(cplex.getValue(this.w[0]));

		}
	}

	public Grafo getG() {
		return g;
	}

	public void setG(Grafo g) {
		this.g = g;
	}

	public long[] getVertSolucion() {
		return solucion;
	}

	public long getSolucion() {
		return sol;
	}

	public long getTiempoEjecucion() {
		return tiempoEjecucion;
	}

}

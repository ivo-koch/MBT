package mbt.modelo.arboles.modificado;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import util.Grafo;

public class Modelo {

	private Grafo g;

	private int v0;

	private double pesoT;

	private IloNumVar[][][] x;

	// private IloNumVar[][] y;

	private IloNumVar[] z;

	private IloNumVar[] t;

	private int maxT = 0;

	private long[] solucion;

	private long tiempoEjecucion;

	private boolean spanning;

	public Modelo(Grafo g, int v0, double pesoT, boolean spanning) {

		this.g = g;
		this.maxT = g.getVertices() - 1;
		this.v0 = v0;
		this.pesoT = pesoT;
		this.spanning = spanning;
	}

	public void solve(OutputStream baos) throws Exception {

		// create CPLEX optimizer/modeler
		IloCplex cplex = new IloCplex();

		IloRange[][] rng = new IloRange[1][];

		// variable x
		x = new IloNumVar[g.getVertices()][g.getVertices()][];
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = 0; j < g.getVertices(); ++j)
				if (i != j && g.isArista(i, j)) {
					int Ni = g.getVecinos(i).size();
					x[i][j] = new IloNumVar[Ni];
					for (int k = 0; k < Ni; k++)
						x[i][j][k] = cplex.boolVar();
				}

		// variable y e yDist
		// y = new IloNumVar[g.getVertices()][g.getVertices()];

		// for (int i = 0; i < g.getVertices(); ++i)
		// for (int j = i + 1; j < g.getVertices(); ++j)
		// if (g.isArista(i, j)) {
		// y[i][j] = cplex.boolVar();
		// y[j][i] = cplex.boolVar();
		// }

		// variable t
		t = new IloNumVar[g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			t[i] = cplex.numVar(0, maxT);

		// variable z
		z = new IloNumVar[g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			z[i] = cplex.boolVar();

		// función objetivo.
		IloNumExpr fobj = cplex.linearIntExpr();
		fobj = cplex.sum(fobj, cplex.prod(pesoT, t[v0]));
		if (!spanning)
			for (int i = 0; i < g.getVertices(); ++i)
				fobj = cplex.sum(fobj, cplex.prod(g.getPeso(i), z[i]));

		cplex.addMinimize(fobj);

		// constraints
		ArrayList<IloRange> restricciones = new ArrayList<IloRange>();

		// restricción (25)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int k = 0; k < g.getVecinos(i).size(); k++) {
				IloNumExpr lhs = cplex.linearIntExpr();
				for (int j : g.getVecinos(i))
					lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

				restricciones.add(cplex.addLe(lhs, 1));
			}

		// restricción (26)
		double M = Math.pow(g.getVertices(), 3);

		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = 0; j < g.getVertices(); ++j)
				if (i != j && g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));

					for (int k = 0; k < g.getVecinos(i).size(); k++)
						lhs = cplex.sum(lhs, cplex.prod(-(k + 1), x[i][j][k]));

					lhs = cplex.sum(lhs, cplex.prod(-1.0, t[j]));

					for (int k = 0; k < g.getVecinos(i).size(); k++)
						lhs = cplex.sum(lhs, cplex.prod(-M, x[i][j][k]));

					// lhs = cplex.sum(lhs, cplex.prod(-M, y[i][j]));

					restricciones.add(cplex.addGe(lhs, -M));
				}

		// restricción (27)
		for (int i = 0; i < g.getVertices(); ++i)
			if (i != v0)
				for (int j = 0; j < g.getVertices(); ++j)
					if (i != j && g.isArista(i, j)) {
						IloNumExpr lhs = cplex.linearIntExpr();

						for (int k = 0; k < g.getVecinos(i).size(); k++)
							lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

						// lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][j]));

						for (int v : g.getVecinos(i))
							for (int k = 0; k < g.getVecinos(v).size(); k++)
								lhs = cplex.sum(lhs, cplex.prod(-1.0, x[v][i][k]));

						// lhs = cplex.sum(lhs, cplex.prod(-1.0, y[v][i]));

						restricciones.add(cplex.addLe(lhs, 0));
					}

		// restricción (28)
		for (int j = 0; j < g.getVertices(); ++j) {
			if (j != v0) {
				IloNumExpr lhs = cplex.linearIntExpr();

				lhs = cplex.sum(lhs, cplex.prod(-1.0, z[j]));

				for (int i : g.getVecinos(j))
					for (int k = 0; k < g.getVecinos(i).size(); k++)
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

				restricciones.add(cplex.addEq(lhs, 0));
			}
		}

		if (spanning)
			for (int j = 0; j < g.getVertices(); ++j) {
				if (j != v0) {
					IloNumExpr lhs = cplex.linearIntExpr();

					lhs = cplex.sum(lhs, cplex.prod(1.0, z[j]));

					restricciones.add(cplex.addEq(lhs, 1));
				}
			}

		// // restricción (37)
		// for (int i = 0; i < g.getVertices(); ++i)
		// for (int j = i + 1; j < g.getVertices(); ++j)
		// if (g.isArista(i, j)) {
		// IloNumExpr lhs = cplex.linearIntExpr();
		// lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][j]));
		// lhs = cplex.sum(lhs, cplex.prod(1.0, y[j][i]));
		//
		// restricciones.add(cplex.addLe(lhs, 1));
		// }

		// restricción (39)
		// for (int i = 0; i < g.getVertices(); ++i)
		// for (int j = 0; j < g.getVertices(); ++j)
		// if (i != j && g.isArista(i, j)) {
		// IloNumExpr lhs = cplex.linearIntExpr();
		// lhs = cplex.sum(lhs, cplex.prod(-1.0, y[i][j]));
		// for (int k = 0; k < g.getVecinos(i).size(); k++)
		// lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));
		//
		// restricciones.add(cplex.addEq(lhs, 0));
		// }

		// restricción (40)
		// for (int i = 0; i < g.getVertices(); ++i)
		// for (int k = 0; k < g.getVecinos(i).size() - 1; k++) {
		// IloNumExpr lhs = cplex.linearIntExpr();
		// for (int j : g.getVecinos(i))
		// lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k + 1]));
		//
		// for (int j : g.getVecinos(i))
		// lhs = cplex.sum(lhs, cplex.prod(-1.0, x[i][j][k]));
		//
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

			for (int v = 0; v < g.getVertices(); v++) {
				try {
					if (cplex.getValue(z[v]) > 0.99)
						System.out.println("z[" + v + "]");
				} catch (Exception e) {
				}
			}

			// for (int v = 0; v < g.getVertices(); v++) {
			// for (int w = 0; w < g.getVertices(); w++) {
			// if (v != w && g.isArista(v, w) && cplex.getValue(y[v][w]) > 0.99)
			// System.out.println("y[" + v + ", " + w + "]");
			// }
			// }

			solucion = new long[g.getVertices()];
			double[][] t_e = new double[g.getVertices()][g.getVertices()];

			for (int v = 0; v < g.getVertices(); v++) {
				solucion[v] = Math.round(cplex.getValue(this.t[v]));
				System.out.println("t[" + v + "] = " + solucion[v] + " (" + cplex.getValue(this.t[v]) + ")");
				for (int w : g.getVecinos(v)) {
					for (int k = 0; k < g.getVecinos(v).size(); k++)
						try {
							if (cplex.getValue(x[v][w][k]) > 0.99) {
								t_e[v][w] = k;
								System.out.println("t_e[" + v + "," + w + "] = " + k);
							}
						} catch (Exception e) {}
				}
			}
			System.out.println();
		}
	}

	public Grafo getG() {
		return g;
	}

	public void setG(Grafo g) {
		this.g = g;
	}

	public long[] getSolucion() {
		return solucion;
	}

	public long getTiempoEjecucion() {
		return tiempoEjecucion;
	}

}

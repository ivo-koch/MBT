package mbt.modelo.arboles;

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

	private IloNumVar[][][] x;

	private IloNumVar[][] y;

	private IloNumVar[][] yDist;

	private IloNumVar[][] w;

	private IloNumVar[] z;

	private IloNumVar[] t;

	private int maxT = 0;

	private long[] solucion;

	public Modelo(Grafo g, int v0) {

		this.g = g;
		this.maxT = g.getVertices() - 1;
		this.v0 = v0;
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
		y = new IloNumVar[g.getVertices()][g.getVertices()];
		yDist = new IloNumVar[g.getVertices()][g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j)) {
					y[i][j] = cplex.boolVar();
					y[j][i] = cplex.boolVar();
					yDist[i][j] = cplex.boolVar();
					yDist[j][i] = cplex.boolVar();
				}

		// variable t
		t = new IloNumVar[g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			t[i] = cplex.numVar(0, maxT);

		// variable z
		z = new IloNumVar[g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			z[i] = cplex.boolVar();

		// variable w
		w = new IloNumVar[g.getVertices()][g.getVertices()];
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j) {
				w[i][j] = cplex.boolVar();
				w[j][i] = cplex.boolVar();
			}

		// función objetivo.
		// TODO: OJO que faltan los pesos en los árboles.
		IloNumExpr fobj = cplex.linearIntExpr();
		fobj = cplex.sum(fobj, cplex.prod(1.0, t[v0]));
		for (int i = 0; i < g.getVertices(); ++i)
			fobj = cplex.sum(fobj, cplex.prod(g.getPeso(i), z[i]));

		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j)) {
					fobj = cplex.sum(fobj, cplex.prod(g.getPeso(i, j), y[i][j]));
					fobj = cplex.sum(fobj, cplex.prod(g.getPeso(i, j), y[j][i]));
				}

		cplex.addMaximize(fobj);

		// constraints
		ArrayList<IloRange> restricciones = new ArrayList<IloRange>();

		for (int i = 0; i < g.getVertices(); ++i)
			if (i != v0 && g.isArista(i, v0)) {
				IloNumExpr lhs = cplex.linearIntExpr();
				lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][v0]));
				restricciones.add(cplex.addLe(lhs, 0));
			}

		// restricción (28)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j : g.getVecinos(i))
				for (int jPrime : g.getVecinos(i))
					if (j != jPrime)
						for (int kPrima = 0; kPrima < g.getVecinos(i).size(); kPrima++) {
							for (int k = 0; k < kPrima; k++) {
								IloNumExpr lhs = cplex.linearIntExpr();
								lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));
								lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][jPrime][kPrima]));
								lhs = cplex.sum(lhs, cplex.prod(1.0, w[j][jPrime]));
								restricciones.add(cplex.addLe(lhs, 2));
							}
						}

		// restricción (29)

		// TODO: Más huevo con esta cota, man.
		double M = Math.pow(g.getVertices(), 3);
	
		for (int j = 0; j < g.getVertices(); ++j)
			for (int jPrime = 0; jPrime < g.getVertices(); ++jPrime)
				if (j != jPrime) {

					Set<Integer> Nj = new HashSet<Integer>(g.getVecinos(j));
					Set<Integer> NjPrime = new HashSet<Integer>(g.getVecinos(jPrime));

					Nj.retainAll(NjPrime);
					if (!Nj.isEmpty()) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(-1.0, t[j]));
						lhs = cplex.sum(lhs, cplex.prod(1.0, t[jPrime]));
						lhs = cplex.sum(lhs, cplex.prod(-M, w[j][jPrime]));
						restricciones.add(cplex.addLe(lhs, 0));
					}
				}

		// restricción (30)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = 0; j < g.getVertices(); ++j)
				if (i != j && g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(1.0, yDist[i][j]));
					lhs = cplex.sum(lhs, cplex.prod(-1.0, y[i][j]));
					restricciones.add(cplex.addLe(lhs, 0));
				}

		// restricción (31)
		for (int i = 0; i < g.getVertices(); ++i) {
			IloNumExpr lhs = cplex.linearIntExpr();
			for (int j : g.getVecinos(i))
				lhs = cplex.sum(lhs, cplex.prod(1.0, yDist[i][j]));

			restricciones.add(cplex.addLe(lhs, 1));
		}

		// restricción (32)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int k = 0; k < g.getVecinos(i).size(); k++) {
				IloNumExpr lhs = cplex.linearIntExpr();
				for (int j : g.getVecinos(i))
					lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

				restricciones.add(cplex.addLe(lhs, 1));
			}

		// restricción (33)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = 0; j < g.getVertices(); ++j)
				if (i != j && g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));

					for (int k = 0; k < g.getVecinos(i).size(); k++)
						lhs = cplex.sum(lhs, cplex.prod(-(k + 1), x[i][j][k]));

					lhs = cplex.sum(lhs, cplex.prod(-1.0, t[j]));

					lhs = cplex.sum(lhs, cplex.prod(M, yDist[i][j]));

					restricciones.add(cplex.addLe(lhs, M));
				}

		// restricción (34)
		for (int i = 0; i < g.getVertices(); ++i) {

			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, cplex.prod(1.0, t[i]));

			for (int j : g.getVecinos(i))
				lhs = cplex.sum(lhs, cplex.prod(-M, yDist[i][j]));

			restricciones.add(cplex.addLe(lhs, 0));
		}

		// restricción (35)
		for (int i = 0; i < g.getVertices(); ++i)
			if (i != v0)
				for (int j = 0; j < g.getVertices(); ++j)
					if (i != j && g.isArista(i, j)) {
						IloNumExpr lhs = cplex.linearIntExpr();
						lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][j]));

						for (int v : g.getVecinos(i))
							lhs = cplex.sum(lhs, cplex.prod(-1.0, y[v][i]));

						restricciones.add(cplex.addLe(lhs, 0));
					}

		// restricción (36)
		for (int j = 0; j < g.getVertices(); ++j) {

			IloNumExpr lhs = cplex.linearIntExpr();

			lhs = cplex.sum(lhs, cplex.prod(-1.0, z[j]));

			for (int i : g.getVecinos(j))
				lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][j]));

			restricciones.add(cplex.addEq(lhs, 0));
		}

		// restricción (37)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = i + 1; j < g.getVertices(); ++j)
				if (g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(1.0, y[i][j]));
					lhs = cplex.sum(lhs, cplex.prod(1.0, y[j][i]));

					restricciones.add(cplex.addLe(lhs, 1));
				}

		// restricción (38)
		for (int j = 0; j < g.getVertices(); ++j)
			for (int jPrime = j + 1; jPrime < g.getVertices(); ++jPrime) {

				Set<Integer> Nj = new HashSet<Integer>(g.getVecinos(j));
				Set<Integer> NjPrime = new HashSet<Integer>(g.getVecinos(jPrime));

				Nj.retainAll(NjPrime);

				if (!Nj.isEmpty()) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(1.0, w[j][jPrime]));
					lhs = cplex.sum(lhs, cplex.prod(1.0, w[jPrime][j]));
					restricciones.add(cplex.addLe(lhs, 1));
				}
			}

		// restricción (39)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int j = 0; j < g.getVertices(); ++j)
				if (i != j && g.isArista(i, j)) {
					IloNumExpr lhs = cplex.linearIntExpr();
					lhs = cplex.sum(lhs, cplex.prod(-1.0, y[i][j]));
					for (int k = 0; k < g.getVecinos(i).size(); k++)
						lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k]));

					restricciones.add(cplex.addEq(lhs, 0));
				}

		// restricción (40)
		for (int i = 0; i < g.getVertices(); ++i)
			for (int k = 0; k < g.getVecinos(i).size() - 1; k++) {
				IloNumExpr lhs = cplex.linearIntExpr();
				for (int j : g.getVecinos(i))
					lhs = cplex.sum(lhs, cplex.prod(1.0, x[i][j][k + 1]));

				for (int j : g.getVecinos(i))
					lhs = cplex.sum(lhs, cplex.prod(-1.0, x[i][j][k]));

				restricciones.add(cplex.addLe(lhs, 0));
			}

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

		boolean ok = cplex.solve();
		long end = System.currentTimeMillis();

		System.out.println("Gap " + cplex.getMIPRelativeGap());
		System.out.println("Mejor sol entera " + cplex.getObjValue());
		System.out.println("Status " + cplex.getCplexStatus());
		System.out.println("Best bound " + cplex.getBestObjValue());
		System.out.print(baos.toString());

		if (ok) {

			for (int v = 0; v < g.getVertices(); v++) {
				for (int w = 0; w < g.getVertices(); w++) {
					if (v != w && g.isArista(v, w) && cplex.getValue(y[v][w]) > 0.99)
						System.out.println("y[" + v + ", " + w + "]");
				}
			}

			for (int v = 0; v < g.getVertices(); v++) {
				for (int w = 0; w < g.getVertices(); w++) {
					if (v != w && g.isArista(v, w) && cplex.getValue(yDist[v][w]) > 0.99)
						System.out.println("yDist[" + v + ", " + w + "]");
				}
			}

			for (int v = 0; v < g.getVertices(); v++)
				for (int w = v + 1; w < g.getVertices(); w++)
					try {
						if (cplex.getValue(this.w[v][w]) > 0.99)
							System.out.println("w[" + v + ", " + w + "]");
					} catch (Exception e) {
					}

			solucion = new long[g.getVertices()];
			double[][] t_e = new double[g.getVertices()][g.getVertices()];

			for (int v = 0; v < g.getVertices(); v++) {
				solucion[v] = Math.round(cplex.getValue(this.t[v]));
				System.out.println("t[" + v + "] = " + solucion[v] + " (" + cplex.getValue(this.t[v]) + ")");
				for (int w : g.getVecinos(v)) {
					try {
						if (cplex.getValue(y[v][w]) == 1)
							for (int k = 0; k < g.getVecinos(v).size(); k++)
								if (cplex.getValue(x[v][w][k]) > 0.99) {
									t_e[v][w] = k;
									System.out.println("t_e[" + v + "," + w + "] = " + k);
								}
					} catch (Exception e) {
					}
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

}

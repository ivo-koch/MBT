package mbt.modelo.caminos;

import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

import util.Grafo;

public class Modelo {

	private Grafo g;

	private Set<Integer> V0;

	private IloNumVar[][][] y;

	private int max = 0;

	private int[] solucion;

	public Modelo(Grafo g, Set<Integer> V0, int max) {

		//this.g = g.aumentar(2);

		int s = this.g.getVertices() - 2;
		int t = this.g.getVertices() - 1;

		for (int v0 : V0)
			this.g.setArista(s, v0);

		for (int v = 0; v < this.g.getVertices() - 2; v++)
			this.g.setArista(v, t);

		// TODO: ojo que falta boletear las aristas com ambos extremos en V0

		this.max = max;
		this.V0 = V0;
	}

	public void solve(OutputStream baos) throws Exception {

		// create CPLEX optimizer/modeler
		IloCplex cplex = new IloCplex();

		// build model
		IloNumVar[][][][] var = new IloNumVar[1][][][];

		IloRange[][] rng = new IloRange[1][];

		int s = g.getVertices() - 2;
		int t = g.getVertices() - 1;

		y = new IloNumVar[V0.size()][g.getVertices()][g.getVertices()];

		for (int v0 : V0) {
			y[v0] = new IloNumVar[g.getVertices()][g.getVertices()];
			for (int i = 0; i < g.getVertices(); ++i)
				for (int j = i + 1; j < g.getVertices(); ++j)
					if (g.isArista(i, j)) {
						if (i != t && j != s)
							y[v0][i][j] = cplex.boolVar();
						if (i != s && j != t)
							y[v0][j][i] = cplex.boolVar();
					}
		}

		// variables y
		var[0] = y;

		IloNumVar cant = cplex.numVar(0, max);

		cplex.addMinimize(cant);

		// constraints
		ArrayList<IloRange> restricciones = new ArrayList<IloRange>();

		// las aristas de un camino que entran a v tienen que ser las mismas que
		// las que salen.
		for (int v = 0; v < g.getVertices(); v++)
			if (v != s && v != t)
				for (int v0 : V0) {
					IloNumExpr lhs = cplex.linearIntExpr();
					for (int w : g.getVecinos(v)) {
						if (w != s)
							lhs = cplex.sum(lhs, cplex.prod(1, y[v0][v][w]));
						if (w != t)
							lhs = cplex.sum(lhs, cplex.prod(-1.0, y[v0][w][v]));
					}
					restricciones.add(cplex.addEq(lhs, 0, "Rest3 para vértice:" + v0));
				}

		// paso a lo sumo una vez por cada arista.

		for (int v = 0; v < g.getVertices() - 1; v++)
			for (int w = v + 1; w < g.getVertices(); w++)
				if (v != w && g.isArista(v, w))
					for (int v0 : V0) {
						IloNumExpr lhs = cplex.linearIntExpr();
						if (w != s)
							lhs = cplex.sum(lhs, cplex.prod(1, y[v0][v][w]));
						if (w != t)
							lhs = cplex.sum(lhs, cplex.prod(1.0, y[v0][w][v]));
						restricciones.add(cplex.addLe(lhs, 1, "Rest3 para vértice:" + v0));
					}

		// dado un vértice, puede tener solamente dos aristas de cualquier
		// camino que pasen por él
		for (int v = 0; v < g.getVertices(); v++)
			if (v != s && v != t) {
				IloNumExpr lhs = cplex.linearIntExpr();
				for (int v0 : V0) {
					for (int w : g.getVecinos(v)) {
						if (w != s)
							lhs = cplex.sum(lhs, cplex.prod(1, y[v0][v][w]));
						if (w != t)
							lhs = cplex.sum(lhs, cplex.prod(1, y[v0][w][v]));
					}
				}
				restricciones.add(cplex.addEq(lhs, 2, "Rest3 para vértice:" + v));
			}

		// la longitud de cualquier camino es menor o igual que cant.
		for (int v0 : V0) {
			IloNumExpr lhs = cplex.linearIntExpr();
			lhs = cplex.sum(lhs, cplex.prod(-1, cant));
			for (int v = 0; v < g.getVertices(); v++)
				for (int w : g.getVecinos(v))
					if (v != s && v != t)
						if (w != s)
							lhs = cplex.sum(lhs, cplex.prod(1, y[v0][v][w]));

			restricciones.add(cplex.addLe(lhs, 0, "Rest3 para vértice:" + v0));
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

			for (int v0 : V0) {
				System.out.println("v0:" + v0);
				for (int v = 0; v < g.getVertices(); v++)
					for (int w : g.getVecinos(v)) {
						try {
							if (cplex.getValue(y[v0][v][w]) == 1)
								System.out.print("v" + v + "->v" + w + " ");
						} catch (Exception e) {
						}
					}
				System.out.println();
			}

			// // System.out.print(GlobalStatistics.getInstance());
			// solucion = new int[g.getVertices()];
			//
			// for (int v = 0; v < g.getVertices(); v++)
			// for (int w : g.getVecinos(v))
			// for (int v0 : V0)
			// if (cplex.getValue(y[v0][v][w]) == 1)
			// solucion[w] = v0;
			//
			// for (int v0 : V0)
			// solucion[v0] = v0;
		}
	}

	public Grafo getG() {
		return g;
	}

	public void setG(Grafo g) {
		this.g = g;
	}

	public int[] getSolucion() {
		return solucion;
	}

}

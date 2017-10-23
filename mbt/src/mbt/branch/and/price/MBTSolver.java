package mbt.branch.and.price;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.io.SimpleBAPLogger;
import org.jorlib.frameworks.columnGeneration.io.SimpleDebugger;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;
import org.jorlib.frameworks.columnGeneration.util.Configuration;

import util.Grafo;
import util.GraphUtils;

public class MBTSolver {

	// private final Grafo grafo;

	private MBTBranchAndPrice bap;
	private MBTMaster master;
	private DataModel dataModel;
	private MBTPricingProblem pricingProblem;

	public MBTSolver(Grafo grafo, Set<Integer> V0) {

		Properties properties = new Properties();
		properties.setProperty("EXPORT_MODEL", "false");
		Configuration.readFromFile(properties);

		// el data model con los datos de V0 y el grafo
		dataModel = new DataModel(grafo, V0);

		// el problema de pricing (sólo sirve para las variables duales)
		pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");

		// // El solver para el problema de pricing.
		// ExactPricingProblemSolverMultipleV0 exactPricingSolver = new
		// ExactPricingProblemSolverMultipleV0(dataModel,
		// pricingProblem);

		// El Master.
		master = new MBTMaster(dataModel, pricingProblem);

		// Define which solvers to use for the pricing problem
		List<Class<? extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem>>> solvers = Collections
				.singletonList(ExactPricingProblemSolverMultipleV0.class);

		// Define Branch creators
		List<? extends AbstractBranchCreator<DataModel, MBTColumn, MBTPricingProblem>> branchCreators = Collections
				.singletonList(new MBTBranchCreator(dataModel, pricingProblem));

		// Create a Branch-and-Price instance, and provide the initial solution as a
		// warm-start
		bap = new MBTBranchAndPrice(dataModel, master, pricingProblem, solvers, branchCreators, 0,
				dataModel.getGrafo().getVertices() - 1);

		// OPTIONAL: Attach a debugger
		new SimpleDebugger(bap, true);

		// OPTIONAL: Attach a logger to the Branch-and-Price procedure.
		new SimpleBAPLogger(bap, new File("output.log"));
	}

	public MBTSolution solve() {

		// Optional: Get an initial solution
		List<MBTColumn> initSolution = this.getInitialSolution(dataModel, pricingProblem);
		int costo = 0;
		for (MBTColumn col : initSolution)
			costo = Math.max(costo, col.getArbol().calcularCosto());

		bap.warmStart(-costo, initSolution);

		// Solve the Graph Coloring problem through Branch-and-Price
		bap.runBranchAndPrice(System.currentTimeMillis() + 8000000L);
		master.printSolution();
		MBTSolution mbtSolution = new MBTSolution(-bap.getObjective(), bap.getTotalNrIterations(),
				bap.getNumberOfProcessedNodes(), bap.getMasterSolveTime(), bap.getPricingSolveTime(), bap.isOptimal(),
				bap.getSolution());

		mbtSolution.print();

		// Clean up:
		bap.close(); // Close master and pricing problems

		return mbtSolution;
	}

	public static void main(String[] args) throws IOException {
		// Grafo g = new Grafo(9);
		//
		// g.setArista(0, 1);
		// g.setArista(0, 2);
		// g.setArista(0, 3);
		// g.setArista(0, 4);
		// g.setArista(2, 5);
		// g.setArista(4, 6);
		// g.setArista(4, 7);
		// g.setArista(8, 7);

		Set<Integer> V0 = new TreeSet<Integer>();

		V0.add(0);
		V0.add(10);

		Grafo g = GraphUtils.loadFromTxt("Rand12_0.3");

		// GraphUtils.saveToTxt("Rand12_0.3", g);
		new MBTSolver(g, V0);
	}

	// ------------------ Helper methods -----------------

	/**
	 * Calculate a feasible graph coloring using a greedy algorithm.
	 * 
	 * @param pricingProblem
	 *            Pricing problem
	 * @return Feasible coloring.
	 */
	public List<MBTColumn> getInitialSolution(DataModel dataModel, MBTPricingProblem pricingProblem) {
		List<MBTColumn> solInicial = new ArrayList<MBTColumn>();

		// y ahora hacemos BFS desde ese V0.
		Arbol bfs = dataModel.getGrafo().bfs(dataModel.getV0());

		for (int v : bfs.getHijos(bfs.getRoot())) {
			Arbol nuevo = Arbol.Builder.buildSubarbol(bfs, v);
			solInicial.add(new MBTColumn(pricingProblem, true, "generateInitialFeasibleSolution", nuevo));
		}

		return solInicial;
	}

}

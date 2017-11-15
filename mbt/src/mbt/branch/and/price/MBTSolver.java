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
	public MBTSolver(Grafo grafo, Set<Integer> V0) {

		Properties properties = new Properties();
		properties.setProperty("EXPORT_MODEL", "true");
		Configuration.readFromFile(properties);

		// el data model con los datos de V0 y el grafo
		DataModel dataModel = new DataModel(grafo, V0);

		// el problema de pricing (sólo sirve para las variables duales)
		MBTPricingProblem pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");

		// // El solver para el problema de pricing.
		// ExactPricingProblemSolverMultipleV0 exactPricingSolver = new
		// ExactPricingProblemSolverMultipleV0(dataModel,
		// pricingProblem);

		// El Master.
		MBTMaster master = new MBTMaster(dataModel, pricingProblem);

		// Define which solvers to use for the pricing problem
		// List<Class<? extends AbstractPricingProblemSolver<DataModel, MBTColumn,
		// MBTPricingProblem>>> solvers = Collections
		// .singletonList(ExactPricingProblemSolverMultipleV0.class);

		List<Class<? extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem>>> solvers = Collections
				.singletonList(ExactPricingProblemSolverMultipleV0.class);

//		// Optional: Get an initial solution
//		List<MBTColumn> initSolution = this.getInitialSolution(dataModel, pricingProblem);
//		int costo = 0;
//		for (MBTColumn col : initSolution)
//			costo = Math.max(costo, col.getArbol().calcularCosto());

		// Define Branch creators
		List<? extends AbstractBranchCreator<DataModel, MBTColumn, MBTPricingProblem>> branchCreators = Collections
				.singletonList(new MBTBranchCreator(dataModel, pricingProblem));

		// Create a Branch-and-Price instance, and provide the initial solution as a
		// warm-start
		MBTBranchAndPrice bap = new MBTBranchAndPrice(dataModel, master, pricingProblem, solvers, branchCreators,
				-(dataModel.getGrafo().getVertices() - 1), 0);

		//bap.warmStart(-costo, initSolution);

		// OPTIONAL: Attach a debugger
		new SimpleDebugger(bap, true);

		// OPTIONAL: Attach a logger to the Branch-and-Price procedure.
		new SimpleBAPLogger(bap, new File("output.log"));

		// Solve the Graph Coloring problem through Branch-and-Price
		bap.runBranchAndPrice(System.currentTimeMillis() + 8000000L);
		master.printSolution();
		MBTSolution mbtSolution = new MBTSolution(bap.getObjective(), bap.getTotalNrIterations(),
				bap.getNumberOfProcessedNodes(), bap.getMasterSolveTime(), bap.getPricingSolveTime(), bap.isOptimal(),
				bap.getSolution());

		mbtSolution.print();

		// Clean up:
		bap.close(); // Close master and pricing problems

		// return mbtSolution;
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
		V0.add(4);
		V0.add(8);
		V0.add(7);

		//// V0.add(4);
		// V0.add(8);
		// V0.add(7);

		// Grafo g = GraphUtils.loadFromTxt("./testInst/G_10_0.6");
		Grafo g = GraphUtils.loadFromTxt("./testInst/G_10_0.6");

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
			solInicial.add(new MBTColumn(pricingProblem, false, "generateInitialFeasibleSolution", nuevo));
		}

		return solInicial;
	}

}

package mbt.branch.and.price.test;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import mbt.branch.and.price.DataModel;
import mbt.branch.and.price.ExactPricingProblemSolver;
import mbt.branch.and.price.MBTPricingProblem;
import util.Grafo;

public class TestExactPricingProblemSolver {

	@Test
	public void testModelo() {
		
		DataModel dataModel = new DataModel();
		
		
		Grafo g = new Grafo(10);
		Set<Integer> V0 = new HashSet<Integer>();
		
		
		MBTPricingProblem pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");
		
		double[] dualCosts = new double[V0.size() + g.getVertices()];
		
		
		pricingProblem.initPricingProblem(dualCosts);
		ExactPricingProblemSolver exactPricingSolver = new ExactPricingProblemSolver(dataModel, pricingProblem);
		
		exactPricingSolver.setObjective();
		
		
	}

}

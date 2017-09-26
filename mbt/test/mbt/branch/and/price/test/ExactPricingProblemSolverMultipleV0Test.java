package mbt.branch.and.price.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.junit.Test;

import mbt.branch.and.price.Arbol;
import mbt.branch.and.price.DataModel;
import mbt.branch.and.price.ExactPricingProblemSolverMultipleV0;
import mbt.branch.and.price.MBTColumn;
import mbt.branch.and.price.MBTPricingProblem;
import util.Grafo;

public class ExactPricingProblemSolverMultipleV0Test {

	@Test
	public void testModelo() throws TimeLimitExceededException {
		
		
		Grafo g = new Grafo(11);
		
		//un árbol que parte de 0
		g.setArista(0, 1);
		g.setArista(1, 2);
		g.setArista(2, 3);
		g.setArista(2, 4);
		
		//otro que parte de 5		
		g.setArista(6, 5);
		
		//otro que parte de 7
		g.setArista(7, 8);		
		g.setArista(7, 9);
		g.setArista(7, 10);
		
		
		Set<Integer> V0 = new HashSet<Integer>();
		
		V0.add(0);
		V0.add(7);
		V0.add(5);
		DataModel dataModel = new DataModel(g, V0);
		
		
		MBTPricingProblem pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");
				
		double[] dualCosts = new double[V0.size() + g.getVertices()];
		
		//ajusto los pesos de forma que el árbol seleccionado resulte T7
		
		dualCosts[0] = 10;
		dualCosts[1] = 10;
		dualCosts[2] = 0.1;
						
		for (int i = 0; i< g.getVertices(); i++)
			dualCosts[i + V0.size()] = -1;
		
		pricingProblem.initPricingProblem(dualCosts);
		ExactPricingProblemSolverMultipleV0 exactPricingSolver = new ExactPricingProblemSolverMultipleV0(dataModel, pricingProblem);
		
		exactPricingSolver.setObjective();
		
		List<MBTColumn> columnas = exactPricingSolver.generateNewColumns();
		Arbol solucion = columnas.get(0).getArbol();
		
		
		assertEquals(7l, solucion.getRoot());
		assertTrue(solucion.contains(8));
		assertTrue(solucion.contains(9));
		assertTrue(solucion.contains(10));
		assertEquals(3l, solucion.getInternalNodes().size());		
		assertEquals(3l, solucion.getCosto(), 0.0001);
	}

}

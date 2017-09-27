package mbt.branch.and.price.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.junit.Test;

import mbt.branch.and.price.Arbol;
import mbt.branch.and.price.DataModel;
import mbt.branch.and.price.ExactPricingProblemSolverMultipleV0;
import mbt.branch.and.price.MBTBranchingDecision;
import mbt.branch.and.price.MBTColumn;
import mbt.branch.and.price.MBTPricingProblem;
import util.Grafo;

public class ExactPricingProblemSolverMultipleV0Test {

	//@Test
	public void testModelo() throws TimeLimitExceededException {

		Grafo g = new Grafo(11);

		// un árbol que parte de 0
		g.setArista(0, 1);
		g.setArista(1, 2);
		g.setArista(2, 3);
		g.setArista(2, 4);

		// otro que parte de 5
		g.setArista(6, 5);

		// otro que parte de 7
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

		// ajusto los pesos de forma que el árbol seleccionado resulte T7

		dualCosts[0] = 10; // costo de v0
		dualCosts[1] = 10; // costo de v5
		dualCosts[2] = 0.1; // costo de v7 (recordar que siempre cuando se recorra el v0 devuelve los
							// vértices en orden ascendente)

		for (int i = 0; i < g.getVertices(); i++)
			dualCosts[i + V0.size()] = -1;

		pricingProblem.initPricingProblem(dualCosts);
		ExactPricingProblemSolverMultipleV0 exactPricingSolver = new ExactPricingProblemSolverMultipleV0(dataModel,
				pricingProblem);

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

	//@Test
	public void testCalculoCosto() throws TimeLimitExceededException {

		Grafo g = new Grafo(14);

		// un único árbol que parte de 1
		g.setArista(1, 2);
		g.setArista(1, 3);
		g.setArista(1, 4);
		g.setArista(1, 5);
		g.setArista(5, 9);
		g.setArista(4, 6);
		g.setArista(4, 7);
		g.setArista(4, 8);
		g.setArista(3, 13);
		g.setArista(10, 13);
		g.setArista(11, 13);
		g.setArista(12, 13);
		g.setArista(11, 0);

		Set<Integer> V0 = new HashSet<Integer>();

		V0.add(1);

		DataModel dataModel = new DataModel(g, V0);

		MBTPricingProblem pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");

		double[] dualCosts = new double[V0.size() + g.getVertices()];

		// ajusto los pesos de forma que el árbol seleccionado resulte todo el grafo

		dualCosts[0] = 0.01;	

		for (int i = 0; i < g.getVertices(); i++)
			dualCosts[i + V0.size()] = -1;

		pricingProblem.initPricingProblem(dualCosts);
		ExactPricingProblemSolverMultipleV0 exactPricingSolver = new ExactPricingProblemSolverMultipleV0(dataModel,
				pricingProblem);

		exactPricingSolver.setObjective();

		List<MBTColumn> columnas = exactPricingSolver.generateNewColumns();
		Arbol solucion = columnas.get(0).getArbol();

		assertEquals(1l, solucion.getRoot());
		for (int i = 0; i < 14; i++)
			assertTrue(solucion.contains(i));

		assertEquals(14l, solucion.getInternalNodes().size() + 1);
		assertEquals(solucion.calcularCosto(), solucion.getCosto(), 0.0001);

		System.out.println(solucion.getCosto());
	}

	@Test
	public void testBranchingPerformed() throws TimeLimitExceededException {

		Grafo g = new Grafo(9);

		g.setArista(0, 1);
		g.setArista(0, 2);
		g.setArista(0, 3);
		g.setArista(0, 4);
		g.setArista(2, 5);
		g.setArista(4, 6);
		g.setArista(4, 7);
		g.setArista(8, 7);

		Set<Integer> V0 = new HashSet<Integer>();

		V0.add(0);
		V0.add(8);

		DataModel dataModel = new DataModel(g, V0);

		MBTPricingProblem pricingProblem = new MBTPricingProblem(dataModel, "testPricingProblem");

		double[] dualCosts = new double[V0.size() + g.getVertices()];

		// ajusto los pesos de forma que el árbol seleccionado resulte T0
		dualCosts[0] = 0.01;
		dualCosts[1] = 100;

		for (int i = 0; i < g.getVertices(); i++)
			dualCosts[i + V0.size()] = -1;

		pricingProblem.initPricingProblem(dualCosts);
		ExactPricingProblemSolverMultipleV0 exactPricingSolver = new ExactPricingProblemSolverMultipleV0(dataModel,
				pricingProblem);

		exactPricingSolver.setObjective();

		List<MBTColumn> columnas = exactPricingSolver.generateNewColumns();
		Arbol solucion = columnas.get(0).getArbol();

		assertEquals(0l, solucion.getRoot());
		for (int i = 0; i < 9; i++)
			if (i != 8)
				assertTrue(solucion.contains(i));

		assertEquals(8l, solucion.getInternalNodes().size() + 1);
		assertEquals(solucion.calcularCosto(), solucion.getCosto(), 0.0001);
		assertEquals(-7 + 0.01 * 4, solucion.getValorFuncionObjetivo(), 0.0001);

		////// ahora hacemos el branching por 0, 4
		MBTBranchingDecision bd = new MBTBranchingDecision(g.getArista(0, 4));
		exactPricingSolver.branchingDecisionPerformed(bd);

		// quiere decir que ahora 4 tiene que estar en V0, y el offset de 4 y 0 es 1, y
		// todos los demás 0
		assertTrue(dataModel.getV0().contains(4));
		for (int i = 0; i < 9; i++)
			if (i == 0 || i == 4)
				assertEquals(1l, dataModel.getOffset()[i]);
			else
				assertEquals(0l, dataModel.getOffset()[i]);

		// si resuelvo el modelo, qué carajo debería pasar?

		// ajusto los pesos de forma que gana el que tiene más vértices (debería ser el que sale de v0)
		dualCosts[0] = 0.01;
		dualCosts[1] = 0.01;
		dualCosts[2] = 0.01;

		for (int i = 0; i < g.getVertices(); i++)
			dualCosts[i + V0.size()] = -1;

		pricingProblem.initPricingProblem(dualCosts);
		
		exactPricingSolver.setObjective();

		//resolvemos otra vez
		Arbol solucionLuegoDelBranch = exactPricingSolver.generateNewColumns().get(0).getArbol();
		
		//y nos fijamos qué resultó de resolver el problema brancheado.
		assertEquals(4l, solucionLuegoDelBranch.getInternalNodes().size());
		assertEquals(0l, solucionLuegoDelBranch.getRoot());
		assertTrue(solucionLuegoDelBranch.contains(1));
		assertTrue(solucionLuegoDelBranch.contains(2));
		assertTrue(solucionLuegoDelBranch.contains(3));
		assertTrue(solucionLuegoDelBranch.contains(5));
		assertEquals(solucionLuegoDelBranch.calcularCosto(), solucionLuegoDelBranch.getCosto(), 0.0001);
		
		assertEquals(-4 + 0.01 * 4, solucionLuegoDelBranch.getValorFuncionObjetivo(), 0.0001);

	}

}

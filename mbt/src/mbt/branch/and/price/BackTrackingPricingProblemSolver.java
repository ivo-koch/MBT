package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.javatuples.Pair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.io.TimeLimitExceededException;
import org.jorlib.frameworks.columnGeneration.pricing.AbstractPricingProblemSolver;

import util.Grafo.AristaDirigida;

/***
 * Implementaci√≥n de soluci√≥n exacta para el problema de pricing para un V0 de
 * varios v√©rtices.
 */
public final class BackTrackingPricingProblemSolver
		extends AbstractPricingProblemSolver<DataModel, MBTColumn, MBTPricingProblem> {

	/** Mantenemos ac√° las variables de la funci√≥n objetivo */
	double[] duals;

	/***
	 * Crea un nuevo solver de pricing.
	 * 
	 * @param dataModel
	 * @param pricingProblem
	 */
	public BackTrackingPricingProblemSolver(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
		this.name = "BackTrackingPricingProblemSolver";
	}


	/**
	 * M√©todo principal que resuelve el problema de pricing.
	 * 
	 * @return List of columns (independent sets) with negative reduced cost.
	 * @throws TimeLimitExceededException
	 *             TimeLimitExceededException
	 */
	@Override
	public List<MBTColumn> generateNewColumns() throws TimeLimitExceededException {
		List<MBTColumn> newPatterns = new ArrayList<>();

		logger.debug("Resolviendo backtracking...");
		Estadisticas.llamadasBacktracking++;
		int i = 0;
		int n = this.dataModel.getGrafo().getVertices();

		for (int v0 : this.dataModel.getV0()) {
			double coeficienteDeV0 = duals[i++];
			Arbol T = new Arbol(n, v0);
			LinkedList<Arista> candidatas = aristasSalientes(T, v0);
			
			if (continuarArbol(T, candidatas, coeficienteDeV0))
			{
				logger.debug("Agregando columna desde v" + v0 + " (obj: " + 
						T.valorFuncionObjetivo(coeficienteDeV0, duals, dataModel) + ")");
				
				
				newPatterns.add(new MBTColumn(pricingProblem, false, "BacktrackingPricingProblemSolver", T));
				Estadisticas.columnasBacktracking++;
			}
		}

		return newPatterns;
	}

	/**
	 * Arma el conjunto con las posibles aristas a agregar a partir de un vÈrtice del ·rbol 
	 * @param T
	 * @param v
	 * @return
	 */
	private LinkedList<Arista> aristasSalientes(Arbol T, int v) 
	{
		LinkedList<Arista> ret = new LinkedList<Arista>();
		for (int w : dataModel.getGrafo().getVecinos(v))
		{
			if (!T.contains(w) && !dataModel.getV0().contains(w))
				ret.add(new Arista(v, w));
		}
		
		return ret;
	}
	
	/**
	 * Este es el algoritmo de backtracking que resuelve el pricing. Recibe un ·rbol y la lista de 
	 * aristas candidatas 
	 * @param T
	 * @param candidatas
	 * @param coefV0
	 * @return
	 */
	private boolean continuarArbol(Arbol T, LinkedList<Arista> candidatas, double coefV0) 
	{
		if (columnaValida(T, coefV0))
			return true;
		
		// Tomo la primera de las candidatas que no estÈ ya en el arbol y la saco de la lista
		Arista uv = null;
		while (!candidatas.isEmpty())
		{
			Arista a = candidatas.pop();
			
			if (!T.contains(a.v))
			{
				uv = a;
				break;
			}
		}
			
		if (uv == null) // no encontrÈ candidata
			return false;

		
		// Pruebo a ver si encuentro algo agregando el nuevo vÈrtice		
		int viejo = uv.u;
		int nuevo = uv.v;
		T.addVertex(nuevo, viejo);
		LinkedList<Arista> vecinosNuevos = aristasSalientes(T, nuevo);
		LinkedList<Arista> nuevasCandidatas = agregarCandidatas(candidatas, vecinosNuevos);
		if (continuarArbol(T, nuevasCandidatas, coefV0))
			return true;

		// Si no sirviÛ agregar esa arista, sigo probando con las dem·s candidatas
		T.removeVertex(nuevo);
		if (continuarArbol(T, candidatas, coefV0))
			return true;

		return false;
	}


	/**
	 * Indica si el arbol sirve como una columna a agregar al primal
	 * @param T
	 * @param coefV0
	 * @return
	 */
	private boolean columnaValida(Arbol T, double coefV0) 
	{
		double fobj = T.valorFuncionObjetivo(coefV0, duals, dataModel);
		return fobj < -config.PRECISION;
	}


	/**
	 * Fusiona las dos listas de candidatos en orden
	 * @param candidatas
	 * @param vecinosNuevos
	 * @return
	 */
	private LinkedList<Arista> agregarCandidatas(LinkedList<Arista> candidatas, LinkedList<Arista> vecinosNuevos) 
	{
		// TODO: Insertar en orden seg˙n profit o lo que sea
		LinkedList<Arista> ret = new LinkedList<Arista>(candidatas);
		ret.addAll(vecinosNuevos);
		return ret;
	}



	
	
	
	
	


	/**
	 * Actualizamos la funci√≥n objetivo del problema con la nueva soluci√≥n dual que
	 * viene del master.
	 */
	@Override
	public void setObjective() {
		duals = pricingProblem.dualCosts;
	}

	/**
	 * Cerrar el problema de pricing.
	 */
	@Override
	public void close() {

	}

	/**
	 * Aplicamos esta decisi√≥n de branching al problema
	 * 
	 * 
	 * @param bd
	 *            BranchingDecision
	 */
	@Override
	public void branchingDecisionPerformed(BranchingDecision bd) {

	}

	/**
	 * Volvemos atr√°s la decisi√≥n de branchear (cuando hacemos el backtracking)
	 * 
	 * 
	 * @param bd
	 *            BranchingDecision
	 */
	@Override
	public void branchingDecisionReversed(BranchingDecision bd) {
	}

	class Arista 
	{
		public int u; // nodo en el ·rbol
		public int v; // nodo fuera del ·rbol

		public Arista(int u, int v) 
		{
			this.u = u;
			this.v = v;
		}
	}

}

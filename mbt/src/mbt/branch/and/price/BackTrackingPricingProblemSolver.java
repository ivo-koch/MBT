package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
		
		double fullProfit = 0.0;		
		for (int j = 0; j < dataModel.getGrafo().getVertices(); ++j)
			fullProfit += profit(j);
		
		int i = 0;
		int n = this.dataModel.getGrafo().getVertices();

		for (int v0 : this.dataModel.getV0()) 
		{
			double coeficienteDeV0 = duals[i++];
			Arbol T = new Arbol(n, v0);
			Candidatas candidatas = aristasSalientes(T, v0);			
			
			if (continuarArbol(T, candidatas, coeficienteDeV0, fullProfit - profit(v0)))
			{
				logger.debug("Agregando columna desde v" + v0 + " (obj: " + 
						T.valorFuncionObjetivo(coeficienteDeV0, duals, dataModel) + ")");
				
				
				newPatterns.add(new MBTColumn(pricingProblem, false, "BacktrackingPricingProblemSolver", T));
				Estadisticas.columnasBacktracking++;
				
				// TODO: Ver si conviene cortar ac· o no..
				break;
			}
		}

		return newPatterns;
	}

	private double profit(int v) 
	{
		return duals[dataModel.getV0().size() + v];
	}
	
	/**
	 * Arma el conjunto con las posibles aristas a agregar a partir de un vÈrtice del ·rbol 
	 * @param T
	 * @param v
	 * @return
	 */
	private Candidatas aristasSalientes(Arbol T, int v) 
	{
		Candidatas ret = new Candidatas(duals);
		for (int w : dataModel.getGrafo().getVecinos(v))
		{
			if (!T.contains(w) && !dataModel.getV0().contains(w))
				ret.agregar(new Arista(v, w));
		}
		
		return ret;
	}
	
	/**
	 * Este es el algoritmo de backtracking que resuelve el pricing. Recibe un ·rbol y la lista de 
	 * aristas candidatas 
	 * @param T
	 * @param candidatas
	 * @param coefV0
	 * @param profitRestante 
	 * @return
	 */
	private boolean continuarArbol(Arbol T, Candidatas candidatas, double coefV0, double profitRestante) 
	{
		if (columnaValida(T, coefV0))
			return true;
		
		// TODO: Agregar m·s podas 
		
		// Poda 1: Si con los candidatos que quedan no llegamos, ya fue
		if (T.valorFuncionObjetivo(coefV0, duals, dataModel) + profitRestante > 0)
			return false;
		
		// Tomo la primera de las candidatas que no estÈ ya en el arbol y la saco de la lista
		Arista uv = proximaArista(T, candidatas);
		if (uv == null) // no encontrÈ candidata
			return false;

		
		// Pruebo a ver si encuentro algo agregando el nuevo vÈrtice		
		int viejo = uv.u;
		int nuevo = uv.v;
		T.addVertex(nuevo, viejo);
		Candidatas vecinosNuevos = aristasSalientes(T, nuevo);
		Candidatas nuevasCandidatas = agregarCandidatas(candidatas, vecinosNuevos);
		if (continuarArbol(T, nuevasCandidatas, coefV0, profitRestante - profit(nuevo)))
			return true;

		// Si no sirviÛ agregar esa arista, sigo probando con las dem·s candidatas
		T.removeVertex(nuevo);
		if (continuarArbol(T, candidatas, coefV0, profitRestante))
			return true;

		return false;
	}


	private Arista proximaArista(Arbol T, Candidatas candidatas) 
	{
		Arista uv = null;
		while (!candidatas.vacia())
		{
			Arista a = candidatas.pop();
			
			if (!T.contains(a.v))
			{
				uv = a;
				break;
			}
		}
		return uv;
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
	private Candidatas agregarCandidatas(Candidatas candidatas, Candidatas vecinosNuevos) 
	{
		Candidatas ret = new Candidatas(candidatas);
		ret.agregarTodas(vecinosNuevos);
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
	
	class Candidatas 
	{
		private LinkedList<Arista> aristas;
		private double[] duals = null;
		static final boolean ORDENAR = true;

		
		public Candidatas(double[] duals) 
		{
			aristas = new LinkedList<Arista>();
			this.duals = duals;
		}

		public Candidatas(Candidatas candidatas) 
		{
			aristas = new LinkedList<Arista>(candidatas.aristas);
			duals = candidatas.duals;
		}

		public void agregar(Arista arista) 
		{			
			if (ORDENAR)
				agregarDespuesDe(arista, aristas.listIterator());
			else
				aristas.addLast(arista);
		}

		private void agregarDespuesDe(Arista arista, ListIterator<Arista> iter) 
		{
			double prof = profit(arista.v);
			while (iter.hasNext())
			{
				// Note: m·s chico es mejor!
				if (prof < profit(iter.next().v))
				{
					iter.previous(); // Lo avancÈ para consultarle... :-(
					iter.add(arista);
					break;
				}
			}

			if (!iter.hasNext()) // No lo agreguÈ... va al final
				aristas.addLast(arista);
		}

		public boolean vacia() 
		{
			return aristas.isEmpty();
		}

		public Arista pop() 
		{
			Arista a = aristas.pop();
			return a;
		}

		private double profit(int v) 
		{
			return duals[dataModel.getV0().size() + v];
		}

		public void agregarTodas(Candidatas cand) 
		{
			if (ORDENAR)
			{
				// Hago el siguiente HORROR para que el agregado de todas sea 
				// en tiempo lineal (aprovechando el orden)
				
				LinkedList<Arista> nuevas = new LinkedList<Arista>();			
				ListIterator<Arista> iter1 = aristas.listIterator();
				ListIterator<Arista> iter2 = cand.aristas.listIterator();
				while (iter1.hasNext() && iter2.hasNext())
				{
					Arista a1 = iter1.next();
					Arista a2 = iter2.next();
	
					if (profit(a1.v) < profit(a2.v))
					{
						nuevas.addLast(a1);
						iter2.previous(); // lo retrocedo poque no lo usÈ
					}
					else
					{
						nuevas.addLast(a2);
						iter1.previous(); // lo retrocedo poque no lo usÈ
					}
				}
				
				while (iter1.hasNext())
				{
					nuevas.addLast(iter1.next());
				}
				
				while (iter2.hasNext())
				{
					nuevas.addLast(iter2.next());
				}
	
				aristas = nuevas;
			}
			else
			{
				aristas.addAll(cand.aristas);
			}
		}
	}

}

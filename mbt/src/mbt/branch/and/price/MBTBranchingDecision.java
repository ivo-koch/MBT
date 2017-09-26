package mbt.branch.and.price;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

import util.Grafo.AristaDirigida;

/**
 * Una decisión de branching para nosotros es: Vamos del vértice origen (que debe estar en V0) de la
 * arista al destino.
 */
public final class MBTBranchingDecision implements BranchingDecision<DataModel, MBTColumn> {

	/***la arista por la cual brancheamos ***/
	private final AristaDirigida arista;

	public MBTBranchingDecision(AristaDirigida arista) {		
		this.arista = arista;
	}

	public AristaDirigida getArista() {
		return arista;
	}

	/**
	 * Determina si una columna es compatible con el branching actual. Es compatible
	 * si el árbol que nos pasan NO tiene el vértice destino (el v2 de la arista de
	 * la variable de instancia), que ahora pasaría a estar en V0
	 * 
	 * 
	 * @param column
	 *            column
	 * @return true
	 */
	@Override
	public boolean columnIsCompatibleWithBranchingDecision(MBTColumn column) {
		return !column.getArbol().contains(this.arista.getV2());
	}

	/**
	 * Determina si la desigualdad parámetro permanece válida Importa sólo para el
	 * branch and price and cut.
	 * 
	 * @param inequality
	 *            inequality
	 * @return true
	 */
	@Override
	public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
		return true;
	}

	@Override
	public String toString() {
		return "Branching " + this.arista.getV1() + "->" + this.arista.getV2();
	}
}

package mbt.branch.and.price;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

import util.Grafo.AristaDirigida;

/**
 * Una decisión de branching para nosotros es:
 * Vamos del vértice origen de la arista al destino.
 * Entonces el offset del origen va a ser la variable offsetOrigen
 * y el offset del destino va a ser offsetDestino.
 */
public final class MBTBranchingDecision implements BranchingDecision<DataModel, Arbol> {

	private final AristaDirigida arista;
	//private final int offsetOrigen;
	//private final int offsetDestino;
	

	public MBTBranchingDecision(AristaDirigida arista) {
		this.arista = arista;
		//this.offsetOrigen = offsetOrigen;
		//this.offsetDestino = offsetDestino;
	}

	public AristaDirigida getArista() {
		return arista;
	}
//
//	public int getOffsetOrigen() {
//		return offsetOrigen;
//	}
//
//	public int getOffsetDestino() {
//		return offsetDestino;
//	}

	/**
	 * Determina si una columna es compatible con el branching actual.
	 * Es compatible si el árbol que nos pasan NO tiene el vértice destino, 
	 * porque quiere decir que salió desde otro v0.
	 * 
	 * @param column
	 *            column
	 * @return true
	 */
	@Override
	public boolean columnIsCompatibleWithBranchingDecision(Arbol column) {
		return !column.getVertices().contains(this.arista.getV2());
	}

	/**
	 * Determine whether the given inequality remains feasible for the child node
	 * 
	 * @param inequality
	 *            inequality
	 * @return true
	 */
	@Override
	public boolean inEqualityIsCompatibleWithBranchingDecision(AbstractInequality inequality) {
		return true; // Cuts are not added in this example
	}

	@Override
	public String toString() {
		return "Branching " + this.arista.getV1() + "->" + this.arista.getV2();
	}
}

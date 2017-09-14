package mbt.branch.and.price;

import org.jgrapht.util.VertexPair;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.branchingDecisions.BranchingDecision;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;

import util.Grafo.AristaDirigida;

/**
 * Ensure that two vertices are assigned the same color
 * 
 * @author Joris Kinable
 * @version 29-6-2016
 */
public final class MBTBranchingDecision implements BranchingDecision<DataModel, Arbol> {

	private final AristaDirigida arista;
	private final int offsetOrigen;
	private final int offsetDestino;

	public final Offset nuevoOffsetDestino = null;

	public MBTBranchingDecision(AristaDirigida arista, int offsetOrigen, int offsetDestino) {
		this.arista = arista;
		this.offsetOrigen = offsetOrigen;
		this.offsetDestino = offsetDestino;
	}

	public AristaDirigida getArista() {
		return arista;
	}

	public int getOffsetOrigen() {
		return offsetOrigen;
	}

	public int getOffsetDestino() {
		return offsetDestino;
	}

	/**
	 * Determine whether the given column remains feasible for the child node
	 * 
	 * @param column
	 *            column
	 * @return true if the column is compliant with the branching decision
	 */
	@Override
	public boolean columnIsCompatibleWithBranchingDecision(Arbol column) {
		//return !(column.vertices.contains(vertexPair.getFirst()) ^ column.vertices.contains(vertexPair.getSecond()));
		return true;
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
		return "Samecolor "; //+ vertexPair;
	}
}

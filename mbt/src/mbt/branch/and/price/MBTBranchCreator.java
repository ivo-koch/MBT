package mbt.branch.and.price;

import java.util.ArrayList;
import java.util.List;

import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;

import util.Grafo.AristaDirigida;

/**
 * Clase responsable de crear los branches.
 * 
 */
public final class MBTBranchCreator extends AbstractBranchCreator<DataModel, Arbol, MBTPricingProblem> {

	public MBTBranchCreator(DataModel dataModel, MBTPricingProblem pricingProblem) {
		super(dataModel, pricingProblem);
	}

	/**
	 * Podemos hacer un branching si hay un v fuera de V0.
	 */
	@Override
	protected boolean canPerformBranching(List<Arbol> solution) {

		// decimos que hay un v fuera de V0 así.
		return dataModel.getV0().size() < dataModel.getGrafo().getVertices();
	}

	/**
	 * Creamos los branches
	 * 
	 * 
	 * @param parentNode
	 *            Nodo fraccionario a partir del cual brancheamos.
	 * 
	 * @return Lista de hijos para el branch.
	 */
	@Override
	protected List<BAPNode<DataModel, Arbol>> getBranches(BAPNode<DataModel, Arbol> parentNode) {

		List<BAPNode<DataModel, Arbol>> branches = new ArrayList<BAPNode<DataModel, Arbol>>();

		// Buscamos el árbol fraccionario.
		// Para eso, la única forma que tenemos de verlo desde acá es viendo los árboles
		// que salgan del
		// mismo v0
		// en este getSolution nos da todas las columnas distintas de 0.
		boolean[] v0YaEncontrado = new boolean[dataModel.getGrafo().getVertices()];
		int v0ConMultiplesArboles = -1;
		int offsetV0 = -1;
		for (Arbol arbol : parentNode.getSolution()) {
			if (v0YaEncontrado[arbol.getRoot()]) {
				v0ConMultiplesArboles = arbol.getRoot();
				offsetV0 = this.dataModel.getOffset()[arbol.getRoot()];
				break;
			}
			v0YaEncontrado[arbol.getRoot()] = true;
		}

		if (v0ConMultiplesArboles == -1 || offsetV0 == -1)
			throw new RuntimeException("Error, no encontramos un árbol fraccionario con más de un V0");

		// Creamos un branch para cada arista desde v0, ajustando los offsets de acuerdo
		// al que tenía v0.
		for (AristaDirigida arista : dataModel.getGrafo().getAristasIncidentes(v0ConMultiplesArboles))
			if (!this.dataModel.getV0().contains(arista.getV2())) {
				MBTBranchingDecision bd = new MBTBranchingDecision(arista);
				BAPNode<DataModel, Arbol> node = this.createBranch(parentNode, bd, parentNode.getSolution(),
						parentNode.getInequalities());
				branches.add(node);
			}

		return branches;
	}
}
